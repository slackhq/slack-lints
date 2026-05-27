// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint.detekt

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.android.tools.lint.detector.api.StringOption
import org.jetbrains.uast.UAnnotation
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UField
import org.jetbrains.uast.ULiteralExpression
import org.jetbrains.uast.ULocalVariable
import org.jetbrains.uast.getParentOfType
import slack.lint.util.OptionLoadingDetector
import slack.lint.util.StringSetLintOption
import slack.lint.util.hasAnyAnnotation
import slack.lint.util.sourceImplementation

private val ALLOWED_NUMBERS = setOf(-1.0, 0.0, 1.0, 2.0)

class MagicNumberDetector(
  private val ignoreAnnotatedOption: StringSetLintOption = StringSetLintOption(IGNORE_ANNOTATED)
) : OptionLoadingDetector(ignoreAnnotatedOption), SourceCodeScanner {

  override fun getApplicableUastTypes(): List<Class<out UElement>> =
    listOf(ULiteralExpression::class.java)

  override fun createUastHandler(context: JavaContext): UElementHandler {
    if (context.isTestSource) return UElementHandler.NONE
    return object : UElementHandler() {
      override fun visitLiteralExpression(node: ULiteralExpression) {
        val value = node.value
        val numericValue =
          when (value) {
            is Int -> value.toDouble()
            is Long -> value.toDouble()
            is Float -> value.toDouble()
            is Double -> value
            else -> return
          }
        if (numericValue in ALLOWED_NUMBERS) return

        // Ignore if part of a constant declaration
        val field = node.getParentOfType<UField>()
        if (field != null) {
          val sourcePsi = field.sourcePsi
          if (sourcePsi != null && sourcePsi.text.contains("const ")) return
        }

        // Ignore if in a local val assignment (named constant)
        val localVar = node.getParentOfType<ULocalVariable>()
        if (localVar != null) return

        // Ignore if in an annotation
        val annotation = node.getParentOfType<UAnnotation>()
        if (annotation != null) return

        // Check ignoreAnnotated on containing function/class
        val containingMethod = node.getParentOfType<org.jetbrains.uast.UMethod>()
        if (
          containingMethod != null && containingMethod.hasAnyAnnotation(ignoreAnnotatedOption.value)
        )
          return
        val containingClass = node.getParentOfType<org.jetbrains.uast.UClass>()
        if (
          containingClass != null && containingClass.hasAnyAnnotation(ignoreAnnotatedOption.value)
        )
          return

        context.report(
          ISSUE,
          node,
          context.getLocation(node),
          "Magic number `$value`. Extract to a named constant.",
        )
      }
    }
  }

  companion object {
    private val IGNORE_ANNOTATED =
      StringOption(
        "ignore-annotated",
        "Comma-separated list of annotation simple names to ignore.",
        "Preview",
        "Numeric literals in functions/classes annotated with these are excluded.",
      )

    val ISSUE =
      Issue.create(
          id = "MagicNumber",
          briefDescription = "Magic number used without a named constant",
          explanation =
            "Using unnamed numeric literals makes code harder to understand and maintain. " +
              "Extract magic numbers into named constants with descriptive names.",
          category = Category.CORRECTNESS,
          priority = 4,
          severity = Severity.WARNING,
          implementation = sourceImplementation<MagicNumberDetector>(shouldRunOnTestSources = false),
        )
        .setOptions(listOf(IGNORE_ANNOTATED))
  }
}
