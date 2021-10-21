/*
 * Copyright (C) 2021 Slack Technologies, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package slack.lint

import com.android.tools.lint.client.api.TYPE_BOOLEAN
import com.android.tools.lint.client.api.TYPE_BYTE
import com.android.tools.lint.client.api.TYPE_CHAR
import com.android.tools.lint.client.api.TYPE_DOUBLE
import com.android.tools.lint.client.api.TYPE_FLOAT
import com.android.tools.lint.client.api.TYPE_INT
import com.android.tools.lint.client.api.TYPE_LONG
import com.android.tools.lint.client.api.TYPE_OBJECT
import com.android.tools.lint.client.api.TYPE_SHORT
import com.android.tools.lint.client.api.TYPE_STRING
import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.LintFix
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.android.tools.lint.detector.api.TextFormat
import com.android.tools.lint.detector.api.getPrimitiveType
import com.android.tools.lint.detector.api.isKotlin
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.UTypeReferenceExpression
import org.jetbrains.uast.kotlin.KotlinReceiverUParameter
import slack.lint.DaggerKotlinIssuesDetector.Companion.ISSUE_BINDS_CAN_BE_EXTENSION_FUNCTION
import java.util.EnumSet

/**
 * This is a simple lint check to catch common Dagger+Kotlin usage issues.
 *
 * - [ISSUE_BINDS_CAN_BE_EXTENSION_FUNCTION] covers `@Binds` functions where the target binding can be a an extension
 * function receiver instead.
 */
@Suppress("UnstableApiUsage")
class DaggerKotlinIssuesDetector : Detector(), SourceCodeScanner {

  companion object {
    // We use the overloaded constructor that takes a varargs of `Scope` as the last param.
    // This is to enable on-the-fly IDE checks. We are telling lint to run on both
    // JAVA and TEST_SOURCES in the `scope` parameter but by providing the `analysisScopes`
    // params, we're indicating that this check can run on either JAVA or TEST_SOURCES and
    // doesn't require both of them together.
    // From discussion on lint-dev https://groups.google.com/d/msg/lint-dev/ULQMzW1ZlP0/1dG4Vj3-AQAJ
    // This was supposed to be fixed in AS 3.4 but still required as recently as 3.6-alpha10.
    private val SCOPES = Implementation(
      DaggerKotlinIssuesDetector::class.java,
      EnumSet.of(Scope.JAVA_FILE, Scope.TEST_SOURCES),
      EnumSet.of(Scope.JAVA_FILE),
      EnumSet.of(Scope.TEST_SOURCES)
    )

    private val ISSUE_BINDS_CAN_BE_EXTENSION_FUNCTION: Issue = Issue.create(
      "BindsCanBeExtensionFunction",
      "@Binds-annotated functions can be extension functions.",
      "@Binds-annotated functions can be extension functions to simplify code readability.",
      Category.USABILITY,
      6,
      Severity.INFORMATIONAL,
      SCOPES
    )

    private val JB_NULLABILITY_ANNOTATIONS = setOf(
      "org.jetbrains.annotations.Nullable",
      "org.jetbrains.annotations.NotNull"
    )

    private const val BINDS_ANNOTATION = "dagger.Binds"

    val issues: Array<Issue> = arrayOf(
      ISSUE_BINDS_CAN_BE_EXTENSION_FUNCTION
    )

    /**
     * Tries to convert a type to an idiomatic kotlin type. This is a little annoying as UAST gives us the JVM type,
     * so kotlin intrinsics are... tricky. Collections and generics will still get whacked at the moment, but this tries
     * to cover the common cases at least.
     *
     * See https://groups.google.com/forum/#!topic/lint-dev/5KLM_YbFQlo
     */
    private fun UTypeReferenceExpression.toKotlinPrimitiveSourceString(): String {
      val qualifiedName = getQualifiedName() ?: asSourceString()
      return when (getPrimitiveType(qualifiedName) ?: qualifiedName) {
        TYPE_INT -> "Int"
        TYPE_LONG -> "Long"
        TYPE_CHAR -> "Char"
        TYPE_FLOAT -> "Float"
        TYPE_DOUBLE -> "Double"
        TYPE_BOOLEAN -> "Boolean"
        TYPE_SHORT -> "Short"
        TYPE_BYTE -> "Byte"
        TYPE_STRING -> "String"
        TYPE_OBJECT -> "Any"
        else -> qualifiedName
      }
    }
  }

  override fun getApplicableUastTypes(): List<Class<out UElement>> {
    return listOf(UMethod::class.java)
  }

  override fun createUastHandler(context: JavaContext): UElementHandler? {
    if (!isKotlin(context.psiFile)) {
      // This is only relevant for Kotlin files.
      return null
    }
    return object : UElementHandler() {
      override fun visitMethod(node: UMethod) {
        if (!node.isConstructor && node.hasAnnotation(BINDS_ANNOTATION)) {
          val firstParam = node.uastParameters[0]
          if (firstParam !is KotlinReceiverUParameter) {
            val annotations = firstParam.uAnnotations
              .filterNot { it.qualifiedName in JB_NULLABILITY_ANNOTATIONS }
              .takeIf { it.isNotEmpty() }
              ?.joinToString(" ", postfix = " ") {
                "@receiver:${it.qualifiedName}"
              }
              .orEmpty()
            val type = firstParam.typeReference!!.toKotlinPrimitiveSourceString()
            context.report(
              ISSUE_BINDS_CAN_BE_EXTENSION_FUNCTION,
              context.getLocation(node),
              ISSUE_BINDS_CAN_BE_EXTENSION_FUNCTION.getBriefDescription(TextFormat.TEXT),
              LintFix.create()
                .name("Convert to extension function")
                .replace()
                .pattern("(${node.name}\\((.*?)\\))")
                .with("$annotations$type.${node.name}()")
                .autoFix()
                .build()
            )
          }
        }
      }
    }
  }
}
