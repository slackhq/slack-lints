// Copyright (C) 2021 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.android.tools.lint.detector.api.TextFormat
import com.android.tools.lint.detector.api.isKotlin
import org.jetbrains.uast.UAnnotated
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UField
import org.jetbrains.uast.UMethod
import slack.lint.util.sourceImplementation

/**
 * A simple detector that ensures that `@Redacted` annotations are only used in Kotlin files (Java
 * is unsupported).
 */
// TODO check for toString() impls in Kotlin classes using Redacted? i.e. it's an error to have both
// TODO check that redacted classes are data classes
class RedactedUsageDetector : Detector(), SourceCodeScanner {

  override fun getApplicableUastTypes(): List<Class<out UElement>> {
    return listOf(UClass::class.java, UMethod::class.java, UField::class.java)
  }

  override fun createUastHandler(context: JavaContext): UElementHandler? {
    // Redacted can only be used in Kotlin files, so this check only checks in Java files
    if (isKotlin(context.psiFile)) return null

    return object : UElementHandler() {
      override fun visitClass(node: UClass) = node.checkRedacted()

      override fun visitField(node: UField) = node.checkRedacted()

      override fun visitMethod(node: UMethod) = node.checkRedacted()

      fun UAnnotated.checkRedacted() {
        uAnnotations
          .find { it.qualifiedName?.contains(NAME_REDACTED, ignoreCase = true) == true }
          ?.let { redactedAnnotation ->
            context.report(
              JAVA_USAGE,
              context.getLocation(redactedAnnotation),
              JAVA_USAGE.getBriefDescription(TextFormat.TEXT),
              quickfixData = null
            )
          }
      }
    }
  }

  companion object {
    // We check simple name only rather than any specific redacted annotation
    private const val NAME_REDACTED = "redacted"

    private val JAVA_USAGE: Issue =
      Issue.create(
        "RedactedInJavaUsage",
        "@Redacted is only supported in Kotlin classes!",
        "@Redacted is only supported in Kotlin classes!",
        Category.CORRECTNESS,
        9,
        Severity.ERROR,
        sourceImplementation<RedactedUsageDetector>()
      )

    val ISSUES = arrayOf(JAVA_USAGE)
  }
}
