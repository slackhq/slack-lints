// Copyright (C) 2021 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint.retrofit

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.LintFix
import com.android.tools.lint.detector.api.Location
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.intellij.psi.PsiTypes
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.sourcePsiElement
import slack.lint.util.removeNode
import slack.lint.util.safeReturnType
import slack.lint.util.sourceImplementation

/**
 * A simple detector that validates basic Retrofit usage.
 * - Retrofit endpoints must be annotated with a retrofit method API unless they're an extension
 *   function or private.
 * - `@FormUrlEncoded` must use `@POST`, `@PUT`, or `@PATCH`.
 * - `@Body` parameter requires `@POST`, `@PUT`, or `@PATCH`.
 * - `@Field` parameters require it to be annotated with `@FormUrlEncoded`.
 * - Must return something other than [Unit].
 */
class RetrofitUsageDetector : Detector(), SourceCodeScanner {

  override fun getApplicableUastTypes() = listOf(UMethod::class.java)

  override fun createUastHandler(context: JavaContext): UElementHandler {
    return object : UElementHandler() {
      override fun visitMethod(node: UMethod) {
        val httpAnnotation =
          HTTP_ANNOTATIONS.firstNotNullOfOrNull { node.findAnnotation(it) } ?: return

        val returnType = node.safeReturnType(context)
        val isVoidOrUnitReturnType =
          returnType == null ||
            returnType == PsiTypes.voidType() ||
            returnType.canonicalText == "kotlin.Unit"
        if (isVoidOrUnitReturnType) {
          val allowsUnitResult = node.hasAnnotation("slack.lint.annotations.AllowUnitResult")
          val isSuspend = context.evaluator.isSuspend(node)
          if (!(isSuspend && allowsUnitResult)) {
            node.report(
              "Retrofit endpoints should return something other than Unit/void.",
              context.getNameLocation(node),
            )
          }
        }

        val httpAnnotationFqnc = httpAnnotation.qualifiedName ?: return
        val isBodyMethod = httpAnnotationFqnc in HTTP_BODY_ANNOTATIONS
        val annotationsByFqcn = node.uAnnotations.associateBy { it.qualifiedName }

        val isFormUrlEncoded = FQCN_FORM_ENCODED in annotationsByFqcn

        if (isFormUrlEncoded && !isBodyMethod) {
          node.report("@FormUrlEncoded requires @PUT, @POST, or @PATCH.")
          return
        }

        val isMultipart = FQCN_MULTIPART in annotationsByFqcn
        if (isMultipart && !isBodyMethod) {
          node.report("@Multipart requires @PUT, @POST, or @PATCH.")
          return
        }

        val hasPath =
          (httpAnnotation.findDeclaredAttributeValue("value")?.evaluate() as? String)?.isNotBlank()
            ?: false

        var hasBodyParam = false
        var hasFieldParams = false
        var hasPartParams = false
        var hasUrlParam = false

        for (parameter in node.uastParameters) {
          if (parameter.hasAnnotation(FQCN_BODY)) {
            if (!isBodyMethod) {
              httpAnnotation.report("@Body param requires @PUT, @POST, or @PATCH.")
            } else if (hasBodyParam) {
              parameter.report("Duplicate @Body param!.")
            } else {
              hasBodyParam = true
            }
          } else if (
            parameter.hasAnnotation(FQCN_FIELD) || parameter.hasAnnotation(FQCN_FIELD_MAP)
          ) {
            hasFieldParams = true
            if (!isFormUrlEncoded) {
              val currentText = node.text
              node.report(
                "@Field(Map) param requires @FormUrlEncoded.",
                quickFixData =
                  LintFix.create()
                    .replace()
                    .text(currentText)
                    .with("@$FQCN_FORM_ENCODED\n$currentText")
                    .autoFix()
                    .build(),
              )
            }
          } else if (parameter.hasAnnotation(FQCN_URL)) {
            if (hasPath) {
              httpAnnotation.report("@Url param should be used with an empty path.")
            } else {
              hasUrlParam = true
            }
          } else if (parameter.hasAnnotation(FQCN_PART)) {
            if (!isBodyMethod) {
              httpAnnotation.report("@Part param requires @PUT, @POST, or @PATCH.")
            } else {
              hasPartParams = true
            }
          }
        }

        if (isFormUrlEncoded) {
          if (!hasFieldParams) {
            val annotation = annotationsByFqcn.getValue(FQCN_FORM_ENCODED)
            annotation.report(
              "@FormUrlEncoded but has no @Field(Map) parameters.",
              quickFixData = LintFix.create().removeNode(context, annotation.sourcePsiElement!!),
            )
          }
        } else if (isMultipart) {
          if (hasBodyParam || hasFieldParams) {
            httpAnnotation.report("@Multipart methods should only contain @Part parameters.")
          } else if (!hasPartParams) {
            httpAnnotation.report("@Multipart methods should contain at least one @Part parameter.")
          }
        } else if (isBodyMethod && !hasBodyParam && !hasFieldParams && !hasPartParams) {
          httpAnnotation.report("This annotation requires an `@Body` parameter.")
        }
        if (!hasPath && !hasUrlParam) {
          httpAnnotation.report("Http path is empty but has no @Url parameter.")
        }
      }

      private fun UElement.report(
        briefDescription: String,
        location: Location = context.getLocation(this),
        quickFixData: LintFix? = null,
      ) {
        context.report(ISSUE, location, briefDescription, quickfixData = quickFixData)
      }
    }
  }

  companion object {
    private val HTTP_ANNOTATIONS =
      setOf(
        "retrofit2.http.DELETE",
        "retrofit2.http.GET",
        "retrofit2.http.HEAD",
        "retrofit2.http.OPTIONS",
        "retrofit2.http.PATCH",
        "retrofit2.http.POST",
        "retrofit2.http.PUT",
      )
    private val HTTP_BODY_ANNOTATIONS =
      setOf("retrofit2.http.PATCH", "retrofit2.http.POST", "retrofit2.http.PUT")
    private const val FQCN_FORM_ENCODED = "retrofit2.http.FormUrlEncoded"
    private const val FQCN_MULTIPART = "retrofit2.http.Multipart"
    private const val FQCN_FIELD = "retrofit2.http.Field"
    private const val FQCN_PART = "retrofit2.http.Part"
    private const val FQCN_FIELD_MAP = "retrofit2.http.FieldMap"
    private const val FQCN_BODY = "retrofit2.http.Body"
    private const val FQCN_URL = "retrofit2.http.Url"

    val ISSUE: Issue =
      Issue.create(
        "RetrofitUsage",
        "This is replaced by the caller.",
        "This linter reports various common configuration issues with Retrofit.",
        Category.CORRECTNESS,
        10,
        Severity.ERROR,
        sourceImplementation<RetrofitUsageDetector>(),
      )
  }
}
