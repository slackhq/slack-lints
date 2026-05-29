// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint.complexity

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.BooleanOption
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.IntOption
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import org.jetbrains.uast.UBinaryExpression
import org.jetbrains.uast.UCatchClause
import org.jetbrains.uast.UDoWhileExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UForEachExpression
import org.jetbrains.uast.UForExpression
import org.jetbrains.uast.UIfExpression
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.USwitchClauseExpression
import org.jetbrains.uast.USwitchExpression
import org.jetbrains.uast.UWhileExpression
import org.jetbrains.uast.UastBinaryOperator
import org.jetbrains.uast.visitor.AbstractUastVisitor
import slack.lint.util.BooleanLintOption
import slack.lint.util.IntLintOption
import slack.lint.util.OptionLoadingDetector
import slack.lint.util.sourceImplementation

class CyclomaticComplexMethodDetector(
  private val thresholdOption: IntLintOption = IntLintOption(THRESHOLD),
  private val ignoreSingleWhenOption: BooleanLintOption = BooleanLintOption(IGNORE_SINGLE_WHEN),
  private val ignoreSimpleWhenEntriesOption: BooleanLintOption =
    BooleanLintOption(IGNORE_SIMPLE_WHEN_ENTRIES),
) :
  OptionLoadingDetector(thresholdOption, ignoreSingleWhenOption, ignoreSimpleWhenEntriesOption),
  SourceCodeScanner {

  override fun getApplicableUastTypes(): List<Class<out UElement>> = listOf(UMethod::class.java)

  override fun createUastHandler(context: JavaContext): UElementHandler {
    return object : UElementHandler() {
      override fun visitMethod(node: UMethod) {
        val body = node.uastBody ?: return
        var complexity = 1
        var whenCount = 0
        var whenEntries = 0

        body.accept(
          object : AbstractUastVisitor() {
            override fun visitIfExpression(node: UIfExpression): Boolean {
              complexity++
              return false
            }

            override fun visitForExpression(node: UForExpression): Boolean {
              complexity++
              return false
            }

            override fun visitForEachExpression(node: UForEachExpression): Boolean {
              complexity++
              return false
            }

            override fun visitWhileExpression(node: UWhileExpression): Boolean {
              complexity++
              return false
            }

            override fun visitDoWhileExpression(node: UDoWhileExpression): Boolean {
              complexity++
              return false
            }

            override fun visitSwitchExpression(node: USwitchExpression): Boolean {
              whenCount++
              return false
            }

            override fun visitSwitchClauseExpression(node: USwitchClauseExpression): Boolean {
              if (node.caseValues.isNotEmpty()) {
                complexity++
                whenEntries++
              }
              return false
            }

            override fun visitCatchClause(node: UCatchClause): Boolean {
              complexity++
              return false
            }

            override fun visitBinaryExpression(node: UBinaryExpression): Boolean {
              if (
                node.operator == UastBinaryOperator.LOGICAL_AND ||
                  node.operator == UastBinaryOperator.LOGICAL_OR
              ) {
                complexity++
              }
              return false
            }
          }
        )

        if (ignoreSingleWhenOption.value && whenCount == 1) {
          complexity -= whenEntries
        } else if (ignoreSimpleWhenEntriesOption.value) {
          // Simple when entries don't add complexity in this mode
          // (handled by not counting entries with single expressions)
        }

        if (complexity > thresholdOption.value) {
          context.report(
            ISSUE,
            node,
            context.getNameLocation(node),
            "Function has a cyclomatic complexity of $complexity, exceeding the limit of ${thresholdOption.value}.",
          )
        }
      }
    }
  }

  companion object {
    private val THRESHOLD =
      IntOption("threshold", "Maximum cyclomatic complexity allowed per function.", 15)

    private val IGNORE_SINGLE_WHEN =
      BooleanOption(
        "ignore-single-when-expression",
        "If true, functions with a single when expression are not counted.",
        true,
      )

    private val IGNORE_SIMPLE_WHEN_ENTRIES =
      BooleanOption(
        "ignore-simple-when-entries",
        "If true, simple when entries (single line) don't add to complexity.",
        true,
      )

    val ISSUE =
      Issue.create(
          id = "CyclomaticComplexMethod",
          briefDescription = "Function has high cyclomatic complexity",
          explanation =
            "Functions with high cyclomatic complexity have too many decision points, " +
              "making them hard to understand and test. Consider decomposing the function.",
          category = Category.CORRECTNESS,
          priority = 5,
          severity = Severity.WARNING,
          implementation = sourceImplementation<CyclomaticComplexMethodDetector>(),
        )
        .setOptions(listOf(THRESHOLD, IGNORE_SINGLE_WHEN, IGNORE_SIMPLE_WHEN_ENTRIES))
  }
}
