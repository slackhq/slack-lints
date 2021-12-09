package slack.lint.text

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.LintFix
import com.android.tools.lint.detector.api.Location
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.android.tools.lint.detector.api.UastLintUtils
import com.android.tools.lint.detector.api.XmlContext
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.PsiType
import org.jetbrains.uast.UBinaryExpression
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.UReferenceExpression
import org.jetbrains.uast.UastBinaryOperator
import org.jetbrains.uast.sourcePsiElement
import org.jetbrains.uast.tryResolve
import org.w3c.dom.Element
import slack.lint.retrofit.RetrofitUsageDetector
import slack.lint.rx.RxSubscribeOnMainDetector
import slack.lint.util.removeNode
import slack.lint.util.sourceImplementation
import java.util.regex.Pattern

/**
 * Checks for SpanPointMarkDangerousCheck. See [ISSUE].
 */
class SpanPointMarkDangerousCheckDetector : Detector(), SourceCodeScanner {

    companion object {
        private fun Implementation.toIssue() = Issue.create(
            id = "SpanPointMarkDangerousCheck",
            briefDescription = "my desc.",
            explanation = """
                Spans flags can have priority or other bits set. Check using `currentFlag and Spanned.SPAN_POINT_MARK_MASK == desiredFlag` 
              """.trimIndent(),
            category = Category.CORRECTNESS,
            priority = 4,
            severity = Severity.ERROR,
            implementation = this
        )

        val ISSUE = sourceImplementation<SpanPointMarkDangerousCheckDetector>().toIssue()
    }

    override fun getApplicableUastTypes() = listOf(UBinaryExpression::class.java)

    override fun createUastHandler(context: JavaContext): UElementHandler {
        return ReportingHandler(context)
    }

    /**
     * Reports violations of SpanPointMarkDangerousCheck.
     */
    private class ReportingHandler(private val context: JavaContext) : UElementHandler() {
        companion object{
            private const val SPANNED_CLASS = "android.text.Spanned"
            private val markPointFields = setOf(
                "$SPANNED_CLASS.INCLUSIVE_INCLUSIVE",
                "$SPANNED_CLASS.INCLUSIVE_EXCLUSIVE",
                "$SPANNED_CLASS.EXCLUSIVE_INCLUSIVE",
                "$SPANNED_CLASS.EXCLUSIVE_EXCLUSIVE",
            )
            private const val MASK_CLASS = "$SPANNED_CLASS.SPAN_POINT_MARK_MASK"
        }
        override fun visitBinaryExpression(node: UBinaryExpression) {
            if (node.operator is UastBinaryOperator.ComparisonOperator) {
                checkExpressions(node, node.leftOperand, node.rightOperand)
                checkExpressions(node, node.rightOperand, node.leftOperand)
            }
        }

        fun checkExpressions(node: UBinaryExpression, markPointCheck: UExpression, maskCheck: UExpression) {
            if (matchesMarkPoint(markPointCheck) && !matchesMask(maskCheck)) {
                context.report(
                    ISSUE,
                    context.getLocation(node),
                    "Do not check against ${markPointCheck.sourcePsi?.text} directly. " +
                            "Instead mask flag with Spanned.SPAN_POINT_MARK_MASK to only check MARK_POINT flags.",
                    LintFix.create()
                        .replace()
                        .name("Use bitwise mask")
                        .text(maskCheck.sourcePsi?.text)
                        .with("((${maskCheck.sourcePsi?.text}) and $MASK_CLASS)")
                        .build()
                )
            }
        }

        fun matchesMarkPoint(expression: UExpression): Boolean {
            return markPointFields.contains(getQualifiedName(expression))
        }

        fun matchesMask(expression: UExpression): Boolean {
            return if (expression is UBinaryExpression) {
                getQualifiedName(expression.leftOperand) == MASK_CLASS || getQualifiedName(expression.rightOperand) == MASK_CLASS
            } else {
                false
            }
        }

        fun getQualifiedName(expression: UExpression): String? {
            return (expression as? UReferenceExpression)?.referenceNameElement?.uastParent?.tryResolve()?.let {
                UastLintUtils.getQualifiedName(it)
            }
        }
    }
}