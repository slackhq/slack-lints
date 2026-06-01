// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint.style

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.android.tools.lint.detector.api.StringOption
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.uast.UBlockExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.ULiteralExpression
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.UReturnExpression
import org.jetbrains.uast.getContainingUClass
import slack.lint.util.OptionLoadingDetector
import slack.lint.util.StringSetLintOption
import slack.lint.util.hasAnyAnnotation
import slack.lint.util.sourceImplementation

class FunctionOnlyReturningConstantDetector(
  private val ignoreAnnotatedOption: StringSetLintOption = StringSetLintOption(IGNORE_ANNOTATED),
  private val excludedFunctionsOption: StringSetLintOption = StringSetLintOption(EXCLUDED_FUNCTIONS),
) : OptionLoadingDetector(ignoreAnnotatedOption, excludedFunctionsOption), SourceCodeScanner {

  override fun getApplicableUastTypes(): List<Class<out UElement>> = listOf(UMethod::class.java)

  override fun createUastHandler(context: JavaContext): UElementHandler {
    return object : UElementHandler() {
      override fun visitMethod(node: UMethod) {
        if (node.isConstructor) return
        if (node.hasAnyAnnotation(ignoreAnnotatedOption.value)) return
        if (node.name in excludedFunctionsOption.value) return
        // Overridable functions (open/abstract/override, or interface members) cannot simply be
        // replaced with a const val, so they are excluded to match detekt's
        // ignoreOverridableFunction default.
        if (node.isOverridable(context)) return

        val body = node.uastBody
        val returnExpr =
          when (body) {
            is UReturnExpression -> body
            is UBlockExpression -> {
              val expressions = body.expressions
              if (expressions.size == 1) expressions.first() as? UReturnExpression else null
            }
            is ULiteralExpression -> {
              // Expression body returning a literal
              null
            }
            else -> null
          }

        // Check expression body (single expression functions)
        val isConstantReturn =
          if (returnExpr != null) {
            returnExpr.returnExpression is ULiteralExpression
          } else {
            body is ULiteralExpression
          }

        if (isConstantReturn) {
          context.report(
            ISSUE,
            node,
            context.getNameLocation(node),
            "Function `${node.name}` only returns a constant. Consider using a `const val` instead.",
          )
        }
      }
    }
  }

  /**
   * True if this function can be overridden (declared `open`, `abstract`, or `override`) or is an
   * interface member, in which case it cannot be replaced with a `const val`.
   */
  private fun UMethod.isOverridable(context: JavaContext): Boolean {
    if (context.evaluator.isAbstract(this)) return true
    val ktFunction = sourcePsi as? KtNamedFunction
    if (ktFunction != null) {
      if (
        ktFunction.hasModifier(KtTokens.OPEN_KEYWORD) ||
          ktFunction.hasModifier(KtTokens.OVERRIDE_KEYWORD) ||
          ktFunction.hasModifier(KtTokens.ABSTRACT_KEYWORD)
      ) {
        return true
      }
    }
    return getContainingUClass()?.isInterface == true
  }

  companion object {
    private val IGNORE_ANNOTATED =
      StringOption(
        "ignore-annotated",
        "Comma-separated list of annotation simple names to ignore.",
        "Provides",
        "Functions annotated with these annotations are excluded.",
      )

    private val EXCLUDED_FUNCTIONS =
      StringOption(
        "excluded-functions",
        "Comma-separated list of function names to exclude.",
        "describeContents",
        "Functions with these names are excluded from this check.",
      )

    val ISSUE =
      Issue.create(
          id = "FunctionOnlyReturningConstant",
          briefDescription = "Function only returns a constant value",
          explanation =
            "Functions that only return a constant value should be replaced with " +
              "a `const val` property for better performance and clarity.",
          category = Category.CORRECTNESS,
          priority = 4,
          severity = Severity.WARNING,
          implementation = sourceImplementation<FunctionOnlyReturningConstantDetector>(),
        )
        .setOptions(listOf(IGNORE_ANNOTATED, EXCLUDED_FUNCTIONS))
  }
}
