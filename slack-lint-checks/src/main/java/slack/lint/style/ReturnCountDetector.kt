// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint.style

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.IntOption
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import org.jetbrains.kotlin.psi.KtReturnExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.UReturnExpression
import org.jetbrains.uast.visitor.AbstractUastVisitor
import slack.lint.util.IntLintOption
import slack.lint.util.OptionLoadingDetector
import slack.lint.util.sourceImplementation

class ReturnCountDetector(private val maxOption: IntLintOption = IntLintOption(MAX_RETURNS)) :
  OptionLoadingDetector(maxOption), SourceCodeScanner {

  override fun getApplicableUastTypes(): List<Class<out UElement>> = listOf(UMethod::class.java)

  override fun createUastHandler(context: JavaContext): UElementHandler {
    return object : UElementHandler() {
      override fun visitMethod(node: UMethod) {
        var returnCount = 0
        node.accept(
          object : AbstractUastVisitor() {
            override fun visitReturnExpression(node: UReturnExpression): Boolean {
              // Labeled returns (e.g. `return@forEach`) exit a lambda, not the function, so they
              // are excluded to match detekt's excludeReturnFromLambda default.
              if ((node.sourcePsi as? KtReturnExpression)?.getTargetLabel() == null) {
                returnCount++
              }
              return super.visitReturnExpression(node)
            }
          }
        )
        if (returnCount > maxOption.value) {
          context.report(
            ISSUE,
            node,
            context.getNameLocation(node),
            "Function has $returnCount return statements, exceeding the limit of ${maxOption.value}.",
          )
        }
      }
    }
  }

  companion object {
    internal val MAX_RETURNS =
      IntOption("max-returns", "Maximum number of return statements allowed per function.", 4)

    val ISSUE =
      Issue.create(
          id = "ReturnCount",
          briefDescription = "Function has too many return statements",
          explanation =
            "Functions with many return statements are harder to follow. " +
              "Consider refactoring to reduce the number of exit points.",
          category = Category.CORRECTNESS,
          priority = 5,
          severity = Severity.WARNING,
          implementation = sourceImplementation<ReturnCountDetector>(),
        )
        .setOptions(listOf(MAX_RETURNS))
  }
}
