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
import org.jetbrains.uast.sourcePsiElement
import org.w3c.dom.Element
import slack.lint.retrofit.RetrofitUsageDetector
import slack.lint.rx.RxSubscribeOnMainDetector
import slack.lint.util.removeNode
import slack.lint.util.sourceImplementation
import java.util.regex.Pattern

class SpanPointMarkDangerousCheckDetector : Detector(), SourceCodeScanner {

    companion object {
        private fun Implementation.toIssue() = Issue.create(
            id = "SpanPointMarkDangerousCheck",
            briefDescription = "my desc.",
            explanation = """
        Spans flags can have priority or other bits set. Check using `currentFlag and Spanned.SPAN_POINT_MARK_MASK == desiredFlag` 
      """,
            category = Category.CORRECTNESS,
            priority = 4,
            severity = Severity.ERROR,
            implementation = this
        )

        val ISSUE = sourceImplementation<SpanPointMarkDangerousCheckDetector>().toIssue()

        private const val MARK_POINT =
            "(Spanned.)?(INCLUSIVE_INCLUSIVE|INCLUSIVE_EXCLUSIVE|EXCLUSIVE_INCLUSIVE|EXCLUSIVE_EXCLUSIVE)"
        private const val FLAG_WITHOUT_MASK = "((?!\\b+and\\s+SPAN_POINT_MARK_MASK\\b).)*"
        private const val EQUALITY_OPERATOR = "((==)|(\\!=))"
        val regex = Regex(
            "(^($FLAG_WITHOUT_MASK)\\s*$EQUALITY_OPERATOR\\s*($MARK_POINT)$)|" +
                    "(^($MARK_POINT)\\s*$EQUALITY_OPERATOR\\s*($FLAG_WITHOUT_MASK)$)"
        )
    }

    override fun getApplicableUastTypes() = listOf(UBinaryExpression::class.java)

    /**
     *
     * For next time:
     *
     * Figure out how to only match the smallest binary expression that matches for each line:
     * Currently it matches twice.
     *
     * Matches are [0 Spanned.INCLUSIVE_INCLUSIVE != spanned.getSpanFlags(Object()) || Spanned.x(), 1 null, 2 null, 3 null, 4 null, 5 null, 6 null, 7 null, 8 null, 9 null, 10 Spanned.INCLUSIVE_INCLUSIVE != spanned.getSpanFlags(Object()) || Spanned.x(), 11 Spanned.INCLUSIVE_INCLUSIVE, 12 Spanned., 13 INCLUSIVE_INCLUSIVE, 14 !=, 15 null, 16 !=, 17 spanned.getSpanFlags(Object()) || Spanned.x(), 18 )]
        Matches are [0 Spanned.INCLUSIVE_INCLUSIVE != spanned.getSpanFlags(Object()), 1 null, 2 null, 3 null, 4 null, 5 null, 6 null, 7 null, 8 null, 9 null, 10 Spanned.INCLUSIVE_INCLUSIVE != spanned.getSpanFlags(Object()), 11 Spanned.INCLUSIVE_INCLUSIVE, 12 Spanned., 13 INCLUSIVE_INCLUSIVE, 14 !=, 15 null, 16 !=, 17 spanned.getSpanFlags(Object()), 18 )]

     */

    override fun createUastHandler(context: JavaContext): UElementHandler {
        System.out.println("regex" + regex.toString())
        return object : UElementHandler() {
            override fun visitBinaryExpression(node: UBinaryExpression) {
                val text = node.sourcePsi?.text ?: return
                val match = regex.find(text) ?: return
                System.out.println("Matches are " + match.groups.mapIndexed { i, group -> "$i ${group?.value}" })
                val markPoint = (match.groups[7] ?: match.groups[11])!!.value
                val flagWithoutMask = (match.groups[2] ?: match.groups[10])!!.value

                /*
                Matches are [0 spanned.getSpanFlags(Object()) != Spanned.INCLUSIVE_INCLUSIVE, 1 spanned.getSpanFlags(Object()) != Spanned.INCLUSIVE_INCLUSIVE, 2 spanned.getSpanFlags(Object()) , 3  , 4 !=, 5 null, 6 !=, 7 Spanned.INCLUSIVE_INCLUSIVE, 8 Spanned., 9 INCLUSIVE_INCLUSIVE, 10 null, 11 null, 12 null, 13 null, 14 null, 15 null, 16 null, 17 null, 18 null]
                 */
                val equality = (match.groups[5] ?: match.groups[6] ?: match.groups[15] ?: match.groups[16])!!.value
                context.report(
                    ISSUE,
                    context.getLocation(node),
                    "Do not check against $markPoint directly. " +
                            "Instead mask flag with Spanned.SPAN_POINT_MARK_MASK to only check MARK_POINT flags.",
                    LintFix.create()
                        .replace()
                        .name("Use bitwise mask")
                        .text(match.groups[0]!!.value)
                        .with("($flagWithoutMask) and Spanned.SPAN_POINT_MARK_MASK $equality $markPoint")
                        .build()
                )
            }
        }
    }

}