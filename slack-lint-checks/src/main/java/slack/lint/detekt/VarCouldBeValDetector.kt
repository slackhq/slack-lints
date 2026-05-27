// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint.detekt

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.LintFix
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.android.tools.lint.detector.api.StringOption
import com.android.tools.lint.detector.api.isKotlin
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UField
import org.jetbrains.uast.USimpleNameReferenceExpression
import org.jetbrains.uast.UBinaryExpression
import org.jetbrains.uast.UastBinaryOperator
import org.jetbrains.uast.getParentOfType
import org.jetbrains.uast.visitor.AbstractUastVisitor
import slack.lint.util.OptionLoadingDetector
import slack.lint.util.StringSetLintOption
import slack.lint.util.hasAnyAnnotation
import slack.lint.util.sourceImplementation

class VarCouldBeValDetector(
  private val ignoreAnnotatedOption: StringSetLintOption = StringSetLintOption(IGNORE_ANNOTATED),
) : OptionLoadingDetector(ignoreAnnotatedOption), SourceCodeScanner {

  override fun getApplicableUastTypes(): List<Class<out UElement>> = listOf(UField::class.java)

  override fun createUastHandler(context: JavaContext): UElementHandler {
    if (!isKotlin(context.psiFile)) return UElementHandler.NONE
    return object : UElementHandler() {
      override fun visitField(node: UField) {
        val ktProperty = node.sourcePsi as? KtProperty ?: return
        if (!ktProperty.isVar) return
        if (ktProperty.setter != null) return
        if (node.hasAnyAnnotation(ignoreAnnotatedOption.value)) return

        // Check if property is reassigned anywhere in the containing class
        val containingClass = node.getParentOfType<UClass>() ?: return
        val name = node.name
        var isReassigned = false

        for (method in containingClass.methods.toList()) {
          method.uastBody?.accept(
            object : AbstractUastVisitor() {
              override fun visitBinaryExpression(node: UBinaryExpression): Boolean {
                if (node.operator is UastBinaryOperator.AssignOperator) {
                  val leftText = node.leftOperand
                  if (leftText is USimpleNameReferenceExpression && leftText.identifier == name) {
                    isReassigned = true
                  }
                }
                return isReassigned
              }
            }
          )
          if (isReassigned) break
        }

        if (!isReassigned) {
          context.report(
            ISSUE,
            node,
            context.getNameLocation(node),
            "Property `$name` is never reassigned and could be a `val`.",
            fix().replace().text("var").with("val").build(),
          )
        }
      }
    }
  }

  companion object {
    private val IGNORE_ANNOTATED =
      StringOption(
        "ignore-annotated",
        "Comma-separated list of annotation simple names to ignore.",
        "Inject,Mock,Captor",
        "Properties annotated with these annotations are excluded.",
      )

    val ISSUE =
      Issue.create(
        id = "VarCouldBeVal",
        briefDescription = "Property declared as `var` but never reassigned",
        explanation =
          "Properties that are never reassigned should be declared as `val` " +
            "to communicate immutability and prevent accidental mutation.",
        category = Category.CORRECTNESS,
        priority = 5,
        severity = Severity.WARNING,
        implementation = sourceImplementation<VarCouldBeValDetector>(),
      )
        .setOptions(listOf(IGNORE_ANNOTATED))
  }
}
