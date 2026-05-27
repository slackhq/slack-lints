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
import com.android.tools.lint.detector.api.isKotlin
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UMethod
import slack.lint.util.OptionLoadingDetector
import slack.lint.util.StringSetLintOption
import slack.lint.util.hasAnyAnnotation
import slack.lint.util.sourceImplementation

private val FUNCTION_NAME_PATTERN = Regex("^[a-z][a-zA-Z0-9]*$")

class FunctionNamingDetector(
  private val ignoreAnnotatedOption: StringSetLintOption = StringSetLintOption(IGNORE_ANNOTATED)
) : OptionLoadingDetector(ignoreAnnotatedOption), SourceCodeScanner {

  override fun getApplicableUastTypes(): List<Class<out UElement>> = listOf(UMethod::class.java)

  override fun createUastHandler(context: JavaContext): UElementHandler {
    if (!isKotlin(context.psiFile)) return UElementHandler.NONE
    return object : UElementHandler() {
      override fun visitMethod(node: UMethod) {
        if (node.isConstructor) return
        val name = node.name
        if (name.startsWith("`")) return
        if (FUNCTION_NAME_PATTERN.matches(name)) return
        if (node.hasAnyAnnotation(ignoreAnnotatedOption.value)) return
        context.report(
          ISSUE,
          node,
          context.getNameLocation(node),
          "Function name `$name` should start with a lowercase letter and use camelCase.",
        )
      }
    }
  }

  companion object {
    private val IGNORE_ANNOTATED =
      StringOption(
        "ignore-annotated",
        "Comma-separated list of annotation simple names to ignore.",
        "Composable,Test",
        "Functions annotated with these annotations are excluded from the naming check.",
      )

    val ISSUE =
      Issue.create(
          id = "FunctionNaming",
          briefDescription = "Function name does not follow naming conventions",
          explanation =
            "Function names should start with a lowercase letter and use camelCase. " +
              "Exceptions can be configured via the ignore-annotated option.",
          category = Category.CORRECTNESS,
          priority = 5,
          severity = Severity.WARNING,
          implementation = sourceImplementation<FunctionNamingDetector>(),
        )
        .setOptions(listOf(IGNORE_ANNOTATED))
  }
}
