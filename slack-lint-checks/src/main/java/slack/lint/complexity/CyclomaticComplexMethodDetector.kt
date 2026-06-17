// Copyright (C) 2026 Slack Technologies, LLC
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
import com.android.tools.lint.detector.api.StringOption
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtObjectLiteralExpression
import org.jetbrains.kotlin.psi.KtWhenEntry
import org.jetbrains.kotlin.psi.psiUtil.getCallNameExpression
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.uast.UBinaryExpression
import org.jetbrains.uast.UBreakExpression
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UCatchClause
import org.jetbrains.uast.UContinueExpression
import org.jetbrains.uast.UDoWhileExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.UForEachExpression
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
import slack.lint.util.StringSetLintOption
import slack.lint.util.sourceImplementation

class CyclomaticComplexMethodDetector(
  private val thresholdOption: IntLintOption = IntLintOption(THRESHOLD),
  private val ignoreSingleWhenOption: BooleanLintOption = BooleanLintOption(IGNORE_SINGLE_WHEN),
  private val ignoreSimpleWhenEntriesOption: BooleanLintOption =
    BooleanLintOption(IGNORE_SIMPLE_WHEN_ENTRIES),
  private val ignoreNestingFunctionsOption: BooleanLintOption =
    BooleanLintOption(IGNORE_NESTING_FUNCTIONS),
  private val nestingFunctionsOption: StringSetLintOption = StringSetLintOption(NESTING_FUNCTIONS),
) :
  OptionLoadingDetector(
    thresholdOption,
    ignoreSingleWhenOption,
    ignoreSimpleWhenEntriesOption,
    ignoreNestingFunctionsOption,
    nestingFunctionsOption,
  ),
  SourceCodeScanner {

  override fun getApplicableUastTypes(): List<Class<out UElement>> = listOf(UMethod::class.java)

  override fun createUastHandler(context: JavaContext): UElementHandler {
    return object : UElementHandler() {
      override fun visitMethod(node: UMethod) {
        // Methods inside an object literal are excluded from complexity.
        if (node.sourcePsi?.getStrictParentOfType<KtObjectLiteralExpression>() != null) return
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
              // The `else` branch has no case values and never adds complexity.
              if (node.caseValues.isEmpty()) return false
              // When ignoring simple when entries, only entries with a block body (e.g.
              // `1 -> { ... }`) add complexity; single-expression entries are considered trivial.
              if (ignoreSimpleWhenEntriesOption.value && !node.hasBlockBody()) return false
              complexity++
              whenEntries++
              return false
            }

            override fun visitCatchClause(node: UCatchClause): Boolean {
              complexity++
              return false
            }

            override fun visitBreakExpression(node: UBreakExpression): Boolean {
              complexity++
              return false
            }

            override fun visitContinueExpression(node: UContinueExpression): Boolean {
              complexity++
              return false
            }

            override fun visitCallExpression(node: UCallExpression): Boolean {
              // Nesting functions (e.g. `forEach`, `let`, `run`) introduce a nested control scope.
              if (!ignoreNestingFunctionsOption.value && node.isNestingFunctionWithLambda()) {
                complexity++
              }
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

            override fun visitExpression(node: UExpression): Boolean {
              // The elvis operator (`?:`) introduces a branch but UAST models it as a dedicated
              // expression rather than a UBinaryExpression, so detect it via its source PSI.
              val ktBinary = node.sourcePsi as? KtBinaryExpression
              if (ktBinary?.operationToken == KtTokens.ELVIS) {
                complexity++
              }
              return false
            }
          }
        )

        if (ignoreSingleWhenOption.value && whenCount == 1) {
          complexity -= whenEntries
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

  /** True if this when entry's body is a block (`-> { ... }`) rather than a single expression. */
  private fun USwitchClauseExpression.hasBlockBody(): Boolean {
    return (sourcePsi as? KtWhenEntry)?.expression is KtBlockExpression
  }

  /** True if this is a call to a configured nesting function passing a lambda with a body. */
  private fun UCallExpression.isNestingFunctionWithLambda(): Boolean {
    val ktCall = sourcePsi as? KtCallExpression ?: return false
    if (ktCall.getCallNameExpression()?.text !in nestingFunctionsOption.value) return false
    val lambda = ktCall.lambdaArguments.firstOrNull()?.getLambdaExpression()
    return lambda?.bodyExpression != null
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

    private val IGNORE_NESTING_FUNCTIONS =
      BooleanOption(
        "ignore-nesting-functions",
        "If true, calls to nesting functions (e.g. run, let, forEach) don't add to complexity.",
        false,
      )

    private val NESTING_FUNCTIONS =
      StringOption(
        "nesting-functions",
        "A comma-separated list of function names that introduce a nested control-flow scope.",
        "run,let,apply,with,also,use,forEach,isNotNull,ifNull",
        "A comma-separated list of function names that introduce a nested control-flow scope. " +
          "Calls to these with a lambda body add to a function's cyclomatic complexity.",
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
        .setOptions(
          listOf(
            THRESHOLD,
            IGNORE_SINGLE_WHEN,
            IGNORE_SIMPLE_WHEN_ENTRIES,
            IGNORE_NESTING_FUNCTIONS,
            NESTING_FUNCTIONS,
          )
        )
  }
}
