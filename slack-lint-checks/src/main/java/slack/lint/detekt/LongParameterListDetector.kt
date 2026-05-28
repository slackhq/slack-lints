// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint.detekt

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.IntOption
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.android.tools.lint.detector.api.StringOption
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UMethod
import slack.lint.util.IntLintOption
import slack.lint.util.OptionLoadingDetector
import slack.lint.util.StringSetLintOption
import slack.lint.util.hasAnyAnnotation
import slack.lint.util.sourceImplementation

class LongParameterListDetector(
  private val thresholdOption: IntLintOption = IntLintOption(FUNCTION_THRESHOLD),
  private val ignoreAnnotatedOption: StringSetLintOption = StringSetLintOption(IGNORE_ANNOTATED),
) : OptionLoadingDetector(thresholdOption, ignoreAnnotatedOption), SourceCodeScanner {

  override fun getApplicableUastTypes(): List<Class<out UElement>> = listOf(UMethod::class.java)

  override fun createUastHandler(context: JavaContext): UElementHandler {
    return object : UElementHandler() {
      override fun visitMethod(node: UMethod) {
        if (node.hasAnyAnnotation(ignoreAnnotatedOption.value)) return
        val paramCount = node.uastParameters.size
        if (paramCount > thresholdOption.value) {
          context.report(
            ISSUE,
            node,
            context.getNameLocation(node),
            "Function has $paramCount parameters, exceeding the limit of ${thresholdOption.value}.",
          )
        }
      }
    }
  }

  companion object {
    internal val FUNCTION_THRESHOLD =
      IntOption("function-threshold", "Maximum number of parameters allowed.", 7)

    internal val IGNORE_ANNOTATED =
      StringOption(
        "ignore-annotated",
        "Comma-separated list of annotation simple names to ignore.",
        "Inject,Provides,AssistedInject,Composable",
        "Functions annotated with these annotations are excluded from this check.",
      )

    val ISSUE =
      Issue.create(
          id = "LongParameterList",
          briefDescription = "Function has too many parameters",
          explanation =
            "Functions with many parameters are hard to call correctly and suggest " +
              "the function is doing too much. Consider using a data class or builder pattern.",
          category = Category.CORRECTNESS,
          priority = 5,
          severity = Severity.WARNING,
          implementation = sourceImplementation<LongParameterListDetector>(),
        )
        .setOptions(listOf(FUNCTION_THRESHOLD, IGNORE_ANNOTATED))
  }
}
