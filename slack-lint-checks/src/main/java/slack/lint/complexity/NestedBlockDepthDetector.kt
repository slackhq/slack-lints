// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint.complexity

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.IntOption
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Location
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtContainerNodeForControlStructureBody
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtIfExpression
import org.jetbrains.kotlin.psi.KtLambdaArgument
import org.jetbrains.kotlin.psi.KtLoopExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.psi.KtTryExpression
import org.jetbrains.kotlin.psi.KtWhenExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UMethod
import slack.lint.util.IntLintOption
import slack.lint.util.OptionLoadingDetector
import slack.lint.util.sourceImplementation

class NestedBlockDepthDetector(
  private val thresholdOption: IntLintOption = IntLintOption(THRESHOLD)
) : OptionLoadingDetector(thresholdOption), SourceCodeScanner {

  override fun getApplicableUastTypes(): List<Class<out UElement>> = listOf(UMethod::class.java)

  override fun createUastHandler(context: JavaContext): UElementHandler {
    return object : UElementHandler() {
      override fun visitMethod(node: UMethod) {
        val function = node.sourcePsi as? KtNamedFunction ?: return
        report(function, context.getNameLocation(node))

        // Lint never visits local functions, so check them from the enclosing declaration.
        reportLocalFunctions(function)
      }

      /**
       * Reports local functions declared directly in [scope], recursing into each one. Stops at any
       * declaration lint visits on its own, so a class member nested in here isn't reported twice.
       */
      private fun reportLocalFunctions(scope: KtElement) {
        scope.children.forEach { child ->
          when {
            child is KtNamedFunction && child.isLocal -> {
              report(child, context.getNameLocation(child))
              reportLocalFunctions(child)
            }
            child is KtClassOrObject -> Unit
            child is KtElement -> reportLocalFunctions(child)
          }
        }
      }

      private fun report(function: KtNamedFunction, location: Location) {
        val threshold = thresholdOption.value
        val visitor = DepthVisitor(threshold)
        function.accept(visitor)
        if (visitor.maxDepth >= threshold) {
          context.report(
            ISSUE,
            location,
            "Function is nested to a depth of ${visitor.maxDepth}, the threshold is $threshold",
          )
        }
      }
    }
  }

  private class DepthVisitor(private val threshold: Int) : KtTreeVisitorVoid() {
    private var depth = 0
    var maxDepth = 0
      private set

    override fun visitIfExpression(expression: KtIfExpression) {
      // An `else if` is a child of the parent `if`, so only count the outermost one.
      if (expression.parent is KtContainerNodeForControlStructureBody) {
        super.visitIfExpression(expression)
      } else {
        nested { super.visitIfExpression(expression) }
      }
    }

    override fun visitLoopExpression(loopExpression: KtLoopExpression) {
      nested { super.visitLoopExpression(loopExpression) }
    }

    override fun visitWhenExpression(expression: KtWhenExpression) {
      nested { super.visitWhenExpression(expression) }
    }

    override fun visitTryExpression(expression: KtTryExpression) {
      nested { super.visitTryExpression(expression) }
    }

    override fun visitCallExpression(expression: KtCallExpression) {
      // Traversal stops at any other call, so nesting inside a lambda passed to something that
      // isn't one of these doesn't count.
      if (expression.calleeExpression?.text !in NESTING_CALLS) return
      if (expression.hasLambdaBody()) {
        nested { super.visitCallExpression(expression) }
      } else {
        super.visitCallExpression(expression)
      }
    }

    private inline fun nested(visit: () -> Unit) {
      depth++
      if (depth >= threshold && depth > maxDepth) {
        maxDepth = depth
      }
      visit()
      depth--
    }

    private fun KtCallExpression.hasLambdaBody(): Boolean =
      lambdaArguments.firstOrNull()?.hasBody() == true

    private fun KtLambdaArgument.hasBody(): Boolean = getLambdaExpression()?.bodyExpression != null
  }

  companion object {
    private val NESTING_CALLS = setOf("run", "let", "apply", "with", "use", "forEach")

    internal val THRESHOLD = IntOption("threshold", "Nesting depth required to report.", 4)

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
