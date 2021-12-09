package slack.lint.text

import org.intellij.lang.annotations.Language
import org.junit.Assert.*
import org.junit.Test
import slack.lint.BaseSlackLintTest
import slack.lint.rx.RxSubscribeOnMainDetector
import slack.lint.rx.rxJavaJar3

/**
 * Tests [SpanPointMarkDangerousCheckDetector].
 */
class SpanPointMarkDangerousCheckDetectorTest : BaseSlackLintTest() {

    override fun getDetector() = SpanPointMarkDangerousCheckDetector()
    override fun getIssues() = listOf(SpanPointMarkDangerousCheckDetector.ISSUE)

    private val androidTextStubs = kotlin(
        """
        package android.text

        object Spanned {
            const val INCLUSIVE_INCLUSIVE = 1
            const val INCLUSIVE_EXCLUSIVE = 2
            const val EXCLUSIVE_INCLUSIVE = 3
            const val EXCLUSIVE_EXCLUSIVE = 4
            const val SPAN_POINT_MARK_MASK = 0xFF
        }
      """
    ).indented()

    @Test
    fun `visitBinaryExpression - given conforming expression - has clean report`() {
        @Language("kotlin")
        val testFile = kotlin(
            """
                package slack.text
        
              import android.text.Spanned
        
              class MyClass {
                  fun doCheckCorrectly(spanned: Spanned): Boolean {
                    return spanned.getSpanFlags(Object()) and Spanned.SPAN_POINT_MARK_MASK == Spanned.INCLUSIVE_INCLUSIVE
                  }
              }
            """
        ).indented()
        lint()
            .files(androidTextStubs, testFile)
            .issues(SpanPointMarkDangerousCheckDetector.ISSUE)
            .run()
            .expectClean()
    }

    @Test
    fun `visitBinaryExpression - given violating expression with INCLUSIVE_INCLUSIVE - creates error and fix`() {
        testViolatingExpression("Spanned.INCLUSIVE_INCLUSIVE")
    }

    @Test
    fun `visitBinaryExpression - given violating expression with INCLUSIVE_EXCLUSIVE - creates error and fix`() {
        testViolatingExpression("Spanned.INCLUSIVE_EXCLUSIVE")
    }

    @Test
    fun `visitBinaryExpression - given violating expression with EXCLUSIVE_INCLUSIVE - creates error and fix`() {
        testViolatingExpression("Spanned.EXCLUSIVE_INCLUSIVE")
    }

    @Test
    fun `visitBinaryExpression - given violating expression with EXCLUSIVE_EXCLUSIVE - creates error and fix`() {
        testViolatingExpression("Spanned.EXCLUSIVE_EXCLUSIVE")
    }

    private fun testViolatingExpression(markPoint: String) {
        @Language("kotlin")
        val testFile = kotlin(
            """
                package slack.text
        
              import android.text.Spanned
        
              class MyClass {
                  fun doCheckIncorrectly(spanned: Spanned): Boolean {
                    return spanned.getSpanFlags(Object()) == $markPoint || Spanned.x()
                  }
              }
            """
        ).indented()
        lint()
            .files(androidTextStubs, testFile)
            .issues(SpanPointMarkDangerousCheckDetector.ISSUE)
            .run()
            .expect(
                """
                src/slack/text/MyClass.kt:7: Error: Do not check against $markPoint directly. Instead mask flag with Spanned.SPAN_POINT_MARK_MASK to only check MARK_POINT flags. [SpanPointMarkDangerousCheck]
                      return spanned.getSpanFlags(Object()) == $markPoint || Spanned.x()
                             ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                1 errors, 0 warnings
                """.trimIndent()
            )
            .expectFixDiffs(
                """
                Fix for src/slack/text/MyClass.kt line 7: Use bitwise mask:
                @@ -7 +7
                -       return spanned.getSpanFlags(Object()) == $markPoint || Spanned.x()
                +       return ((spanned.getSpanFlags(Object())) and android.text.Spanned.SPAN_POINT_MARK_MASK) == $markPoint || Spanned.x()
                """.trimIndent()
            )
    }

}
