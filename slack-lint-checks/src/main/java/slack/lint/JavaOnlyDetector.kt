// Copyright (C) 2020 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.LintFix
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.android.tools.lint.detector.api.UastLintUtils
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassType
import org.jetbrains.uast.UAnnotation
import org.jetbrains.uast.UAnonymousClass
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UCallableReferenceExpression
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.ULambdaExpression
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.UReturnExpression
import org.jetbrains.uast.getContainingUClass
import org.jetbrains.uast.getContainingUMethod
import org.jetbrains.uast.kotlin.isKotlin
import org.jetbrains.uast.toUElementOfType
import slack.lint.util.sourceImplementation

/**
 * Logic adapted from the analogous KotlinOnlyChecker in Error-Prone.
 *
 * Consuming repos should create and use `@KotlinOnly` and `@JavaOnly` annotations from the
 * `slack-lint-annotations` artifact. We would normally like to consume these via properties
 * defining them, but lint APIs only allow reading APIs from project-local gradle.properties and not
 * root properties files.
 *
 * Copied recipe from https://github.com/uber/lint-checks
 */
class JavaOnlyDetector : Detector(), SourceCodeScanner {
  companion object {
    private const val KOTLIN_ONLY = "slack.lint.annotations.KotlinOnly"
    private const val JAVA_ONLY = "slack.lint.annotations.JavaOnly"
    private const val ISSUE_ID = "JavaOnlyDetector"
    private const val MESSAGE_LINT_ERROR_TITLE = "Using @JavaOnly elements in Kotlin code."
    private const val MESSAGE_LINT_ERROR_EXPLANATION = "This should not be called from Kotlin code"
    @JvmField
    val ISSUE =
      Issue.create(
        ISSUE_ID,
        MESSAGE_LINT_ERROR_TITLE,
        MESSAGE_LINT_ERROR_EXPLANATION,
        Category.INTEROPERABILITY_KOTLIN,
        6,
        Severity.ERROR,
        sourceImplementation<JavaOnlyDetector>(),
      )

    private fun anonymousTypeString(psiClass: PsiClass, type: String): String {
      return "Cannot create $type instances of @JavaOnly-annotated type ${UastLintUtils.getClassName(psiClass)} (in ${psiClass.containingFile.name}) " +
        "in Kotlin. Make a concrete class instead."
    }
  }

  override fun createUastHandler(context: JavaContext): UElementHandler? {
    // We only run this on Kotlin files, the ErrorProne analogue handles Java files. Can revisit
    // if we get lint in the IDE or otherwise unify
    if (!isKotlin(context.uastFile?.lang)) return null

    return object : UElementHandler() {
      override fun visitClass(node: UClass) {
        val hasJavaOnly = context.evaluator.getAnnotation(node, JAVA_ONLY) != null
        val hasKotlinOnly = context.evaluator.getAnnotation(node, KOTLIN_ONLY) != null
        if (hasJavaOnly && hasKotlinOnly) {
          context.report(
            ISSUE,
            context.getLocation(node.sourcePsi!!),
            "Cannot annotate types with both `@KotlinOnly` and `@JavaOnly`",
          )
          return
        }
        if (hasJavaOnly || hasKotlinOnly) {
          return
        }
        if (node is UAnonymousClass) {
          if (node.uastParent.isReturnExpression() && node.isEnclosedInJavaOnlyMethod()) {
            return
          }
          node.baseClassType.resolve()?.let { psiClass ->
            context.evaluator.getAnnotation(psiClass, JAVA_ONLY)?.run {
              val message = anonymousTypeString(psiClass, "anonymous")
              context.report(ISSUE, context.getLocation(node.sourcePsi!!), message)
            }
          }
          return
        }
        val reportData =
          checkMissingSubclass(node, KOTLIN_ONLY, "KotlinOnly")
            ?: checkMissingSubclass(node, JAVA_ONLY, "JavaOnly")
            ?: return
        context.report(
          ISSUE,
          context.getLocation(node.sourcePsi!!),
          reportData.first,
          reportData.second,
        )
      }

      private fun checkMissingSubclass(
        node: UClass,
        targetAnnotation: String,
        targetAnnotationSimpleName: String,
      ): Pair<String, LintFix>? {
        return listOfNotNull(node.javaPsi.superClass, *node.interfaces)
          .mapNotNull { psiClass ->
            context.evaluator.getAnnotation(psiClass, targetAnnotation)?.run {
              val message =
                "Type subclasses/implements ${UastLintUtils.getClassName(psiClass)} in ${psiClass.containingFile.name} which is annotated @$targetAnnotationSimpleName, it should also be annotated."
              val source = node.text
              return@mapNotNull message to
                fix()
                  .replace()
                  .name("Add @$targetAnnotationSimpleName")
                  .range(context.getLocation(node.sourcePsi!!))
                  .shortenNames()
                  .text(source)
                  .with("@$targetAnnotation $source")
                  .autoFix()
                  .build()
            }
          }
          .firstOrNull()
      }

      override fun visitLambdaExpression(node: ULambdaExpression) {
        if (node.isReturnExpression() && node.isEnclosedInJavaOnlyMethod()) {
          return
        }
        node.functionalInterfaceType?.let { type ->
          if (type is PsiClassType) {
            type.resolve()?.let { psiClass ->
              context.evaluator.getAnnotation(psiClass, JAVA_ONLY)?.let {
                val message = anonymousTypeString(psiClass, "lambda")
                context.report(ISSUE, context.getLocation(node.sourcePsi!!), message)
                return
              }
              val functionalMethod = psiClass.methods.firstOrNull() ?: return
              functionalMethod.toUElementOfType<UMethod>()?.isAnnotationPresent()?.let {
                node.report(it, "expressed as a lambda in Kotlin")
              }
            }
          }
        }
      }

      override fun visitMethod(node: UMethod) {
        val hasJavaOnly = context.evaluator.getAnnotation(node, JAVA_ONLY) != null
        val hasKotlinOnly = context.evaluator.getAnnotation(node, KOTLIN_ONLY) != null
        if (hasJavaOnly && hasKotlinOnly) {
          context.report(
            ISSUE,
            context.getLocation(node.sourcePsi!!),
            "Cannot annotate functions with both `@KotlinOnly` and `@JavaOnly`",
          )
          return
        }
        if (hasJavaOnly || hasKotlinOnly) {
          return
        }
        val reportData =
          checkMissingOverride(node, KOTLIN_ONLY, "KotlinOnly")
            ?: checkMissingOverride(node, JAVA_ONLY, "JavaOnly")
            ?: return
        context.report(ISSUE, context.getLocation(node), reportData.first, reportData.second)
      }

      private fun checkMissingOverride(
        node: UMethod,
        targetAnnotation: String,
        targetAnnotationSimpleName: String,
      ): Pair<String, LintFix>? {
        return context.evaluator.getSuperMethod(node)?.let { method ->
          context.evaluator.getAnnotation(method, targetAnnotation)?.run {
            val message =
              "Function overrides ${method.name} in ${
                UastLintUtils.getClassName(
                  method.containingClass!!
                )
              } which is annotated @$targetAnnotationSimpleName, it should also be annotated."
            val modifier = node.modifierList.children.joinToString(separator = " ") { it.text }
            return@let message to
              fix()
                .replace()
                .name("Add @$targetAnnotationSimpleName")
                .range(context.getLocation(node))
                .shortenNames()
                .text(modifier)
                .with("@$targetAnnotation $modifier")
                .autoFix()
                .build()
          }
        }
      }

      override fun visitCallExpression(node: UCallExpression) {
        node.resolve().toUElementOfType<UMethod>()?.isAnnotationPresent()?.let { node.report(it) }
      }

      override fun visitCallableReferenceExpression(node: UCallableReferenceExpression) {
        node.resolve().toUElementOfType<UMethod>()?.isAnnotationPresent()?.let { node.report(it) }
      }

      private fun UExpression.report(
        javaOnlyMessage: String?,
        callString: String = "called from Kotlin",
      ) {
        val message = StringBuilder("This method should not be $callString")
        if (javaOnlyMessage.isNullOrBlank()) {
          message.append(", see its documentation for details.")
        } else {
          message.append(": $javaOnlyMessage")
        }
        context.report(ISSUE, context.getLocation(this), message.toString())
      }

      private fun UElement?.isReturnExpression(): Boolean =
        this != null && uastParent is UReturnExpression

      private fun UElement.isEnclosedInJavaOnlyMethod(): Boolean {
        return getContainingUMethod()?.isAnnotationPresent() != null
      }

      private fun UMethod.isAnnotationPresent(): String? {
        findAnnotation(JAVA_ONLY)?.let {
          return it.extractValue()
        }
        getContainingUClass()?.findAnnotation(JAVA_ONLY)?.let {
          return it.extractValue()
        }
        context.evaluator.getPackage(this)?.let { pkg ->
          context.evaluator.getAnnotation(pkg, KOTLIN_ONLY)?.let {
            return it.extractValue()
          }
        }
        return null
      }

      private fun UAnnotation.extractValue(): String {
        return UastLintUtils.getAnnotationStringValue(this, "reason").orEmpty()
      }
    }
  }

  override fun getApplicableUastTypes(): List<Class<out UElement>> {
    return listOf(
      UMethod::class.java,
      UCallExpression::class.java,
      UCallableReferenceExpression::class.java,
      ULambdaExpression::class.java,
      UClass::class.java,
    )
  }
}
