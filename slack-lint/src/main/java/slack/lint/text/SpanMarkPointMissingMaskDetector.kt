// Copyright (C) 2021 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint.text

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.LintFix
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.android.tools.lint.detector.api.UastLintUtils
import org.jetbrains.uast.UBinaryExpression
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.UReferenceExpression
import org.jetbrains.uast.UastBinaryOperator
import org.jetbrains.uast.tryResolve
import slack.lint.text.SpanMarkPointMissingMaskDetector.Companion.ISSUE
import slack.lint.util.resolveQualifiedNameOrNull
import slack.lint.util.sourceImplementation

/** Checks for SpanMarkPointMissingMask. See [ISSUE]. */
class SpanMarkPointMissingMaskDetector : Detector(), SourceCodeScanner {

  companion object {
    val ISSUE =
      Issue.create(
        id = "SpanMarkPointMissingMask",
        briefDescription =
          "Check that Span flags use the bitwise mask SPAN_POINT_MARK_MASK when being compared to.",
        explanation =
          """
        Spans flags can have priority or other bits set. \
        Ensure that Span flags are checked using \
        `currentFlag and Spanned.SPAN_POINT_MARK_MASK == desiredFlag` \
        rather than just `currentFlag == desiredFlag`
      """,
        category = Category.CORRECTNESS,
        priority = 4,
        severity = Severity.ERROR,
        implementation = sourceImplementation<SpanMarkPointMissingMaskDetector>()
      )
  }

  override fun getApplicableUastTypes() = listOf(UBinaryExpression::class.java)

  override fun createUastHandler(context: JavaContext): UElementHandler = ReportingHandler(context)
}

private const val SPANNED_CLASS = "android.text.Spanned"
private val MARK_POINT_FIELDS =
  setOf(
    "$SPANNED_CLASS.SPAN_INCLUSIVE_INCLUSIVE",
    "$SPANNED_CLASS.SPAN_INCLUSIVE_EXCLUSIVE",
    "$SPANNED_CLASS.SPAN_EXCLUSIVE_INCLUSIVE",
    "$SPANNED_CLASS.SPAN_EXCLUSIVE_EXCLUSIVE",
  )
private const val MASK_CLASS = "$SPANNED_CLASS.SPAN_POINT_MARK_MASK"

/** Reports violations of SpanMarkPointMissingMask. */
private class ReportingHandler(private val context: JavaContext) : UElementHandler() {
  override fun visitBinaryExpression(node: UBinaryExpression) {
    if (
      node.operator == UastBinaryOperator.EQUALS ||
        node.operator == UastBinaryOperator.NOT_EQUALS ||
        node.operator == UastBinaryOperator.IDENTITY_EQUALS ||
        node.operator == UastBinaryOperator.IDENTITY_NOT_EQUALS
    ) {
      checkExpressions(node, node.leftOperand, node.rightOperand)
      checkExpressions(node, node.rightOperand, node.leftOperand)
    }
  }

  private fun checkExpressions(
    node: UBinaryExpression,
    markPointCheck: UExpression,
    maskCheck: UExpression
  ) {
    if (markPointCheck.isMarkPointFieldName() && !maskCheck.isMaskClass()) {
      context.report(
        ISSUE,
        context.getLocation(node),
        """
          Do not check against ${markPointCheck.sourcePsi?.text} directly. \
          Instead mask flag with Spanned.SPAN_POINT_MARK_MASK to only check MARK_POINT flags.
        """
          .trimIndent(),
        LintFix.create()
          .replace()
          .name("Use bitwise mask")
          .text(maskCheck.sourcePsi?.text)
          .with("((${maskCheck.sourcePsi?.text}) and $MASK_CLASS)")
          .build()
      )
    }
  }
}

private fun UExpression.isMarkPointFieldName(): Boolean =
  this.getQualifiedName() in MARK_POINT_FIELDS

private fun UExpression.getQualifiedName(): String? {
  return (this as? UReferenceExpression)
    ?.referenceNameElement
    ?.uastParent
    ?.tryResolve()
    ?.let(UastLintUtils::getQualifiedName)
}

private fun UExpression.isMaskClass(): Boolean {
  return if (this is UBinaryExpression) {
    this.leftOperand.resolveQualifiedNameOrNull() == MASK_CLASS ||
      this.rightOperand.resolveQualifiedNameOrNull() == MASK_CLASS
  } else {
    false
  }
}
