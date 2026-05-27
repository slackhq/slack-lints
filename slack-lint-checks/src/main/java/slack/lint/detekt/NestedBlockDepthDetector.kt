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
import org.jetbrains.uast.UDoWhileExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UForEachExpression
import org.jetbrains.uast.UForExpression
import org.jetbrains.uast.UIfExpression
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.USwitchExpression
import org.jetbrains.uast.UTryExpression
import org.jetbrains.uast.UWhileExpression
import org.jetbrains.uast.visitor.AbstractUastVisitor
import slack.lint.util.IntLintOption
import slack.lint.util.OptionLoadingDetector
import slack.lint.util.sourceImplementation

class NestedBlockDepthDetector(
  private val thresholdOption: IntLintOption = IntLintOption(THRESHOLD),
) : OptionLoadingDetector(thresholdOption), SourceCodeScanner {

  override fun getApplicableUastTypes(): List<Class<out UElement>> = listOf(UMethod::class.java)

  override fun createUastHandler(context: JavaContext): UElementHandler {
    return object : UElementHandler() {
      override fun visitMethod(node: UMethod) {
        var maxDepth = 0
        var currentDepth = 0
        node.uastBody?.accept(
          object : AbstractUastVisitor() {
            override fun visitIfExpression(node: UIfExpression): Boolean {
              currentDepth++
              maxDepth = maxOf(maxDepth, currentDepth)
              return false
            }

            override fun afterVisitIfExpression(node: UIfExpression) {
              currentDepth--
            }

            override fun visitForExpression(node: UForExpression): Boolean {
              currentDepth++
              maxDepth = maxOf(maxDepth, currentDepth)
              return false
            }

            override fun afterVisitForExpression(node: UForExpression) {
              currentDepth--
            }

            override fun visitForEachExpression(node: UForEachExpression): Boolean {
              currentDepth++
              maxDepth = maxOf(maxDepth, currentDepth)
              return false
            }

            override fun afterVisitForEachExpression(node: UForEachExpression) {
              currentDepth--
            }

            override fun visitWhileExpression(node: UWhileExpression): Boolean {
              currentDepth++
              maxDepth = maxOf(maxDepth, currentDepth)
              return false
            }

            override fun afterVisitWhileExpression(node: UWhileExpression) {
              currentDepth--
            }

            override fun visitDoWhileExpression(node: UDoWhileExpression): Boolean {
              currentDepth++
              maxDepth = maxOf(maxDepth, currentDepth)
              return false
            }

            override fun afterVisitDoWhileExpression(node: UDoWhileExpression) {
              currentDepth--
            }

            override fun visitSwitchExpression(node: USwitchExpression): Boolean {
              currentDepth++
              maxDepth = maxOf(maxDepth, currentDepth)
              return false
            }

            override fun afterVisitSwitchExpression(node: USwitchExpression) {
              currentDepth--
            }

            override fun visitTryExpression(node: UTryExpression): Boolean {
              currentDepth++
              maxDepth = maxOf(maxDepth, currentDepth)
              return false
            }

            override fun afterVisitTryExpression(node: UTryExpression) {
              currentDepth--
            }
          }
        )
        if (maxDepth > thresholdOption.value) {
          context.report(
            ISSUE,
            node,
            context.getNameLocation(node),
            "Function has a nested block depth of $maxDepth, exceeding the limit of ${thresholdOption.value}.",
          )
        }
      }
    }
  }

  companion object {
    private val THRESHOLD =
      IntOption("threshold", "Maximum nesting depth allowed in a function.", 6)

    val ISSUE =
      Issue.create(
        id = "NestedBlockDepth",
        briefDescription = "Function has too many nested blocks",
        explanation =
          "Deeply nested code blocks are difficult to read and understand. " +
            "Consider extracting nested logic into separate functions or using early returns.",
        category = Category.CORRECTNESS,
        priority = 5,
        severity = Severity.WARNING,
        implementation = sourceImplementation<NestedBlockDepthDetector>(),
      )
        .setOptions(listOf(THRESHOLD))
  }
}
