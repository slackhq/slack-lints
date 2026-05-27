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
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.visitor.AbstractUastVisitor
import slack.lint.util.IntLintOption
import slack.lint.util.OptionLoadingDetector
import slack.lint.util.sourceImplementation

class LongMethodDetector(
  private val thresholdOption: IntLintOption = IntLintOption(THRESHOLD),
) : OptionLoadingDetector(thresholdOption), SourceCodeScanner {

  override fun getApplicableUastTypes(): List<Class<out UElement>> = listOf(UMethod::class.java)

  override fun createUastHandler(context: JavaContext): UElementHandler {
    return object : UElementHandler() {
      override fun visitMethod(node: UMethod) {
        val body = node.uastBody ?: return
        val lineCount = countLines(context, body)
        if (lineCount > thresholdOption.value) {
          context.report(
            ISSUE,
            node,
            context.getNameLocation(node),
            "Function body has $lineCount lines, exceeding the limit of ${thresholdOption.value}.",
          )
        }
      }
    }
  }

  private fun countLines(context: JavaContext, body: UExpression): Int {
    val sourcePsi = body.sourcePsi ?: return 0
    val text = sourcePsi.text
    return text.count { it == '\n' }
  }

  companion object {
    private val THRESHOLD =
      IntOption("threshold", "Maximum number of lines allowed in a function body.", 120)

    val ISSUE =
      Issue.create(
        id = "LongMethod",
        briefDescription = "Function body is too long",
        explanation =
          "Long functions are harder to understand, test, and maintain. " +
            "Consider extracting parts of the function into smaller, well-named functions.",
        category = Category.CORRECTNESS,
        priority = 5,
        severity = Severity.WARNING,
        implementation = sourceImplementation<LongMethodDetector>(),
      )
        .setOptions(listOf(THRESHOLD))
  }
}
