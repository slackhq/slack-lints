package slack.lint.text

import org.intellij.lang.annotations.Language
import org.junit.Assert.*
import org.junit.Test
import slack.lint.BaseSlackLintTest
import slack.lint.rx.RxSubscribeOnMainDetector
import slack.lint.rx.rxJavaJar3

class SpanPointMarkDangerousCheckDetectorTest : BaseSlackLintTest() {

    override fun getDetector() = SpanPointMarkDangerousCheckDetector()
    override fun getIssues() = listOf(SpanPointMarkDangerousCheckDetector.ISSUE)

    @Test
    fun `visitBinaryExpression - given violating file with equality and flag on right - creates error and fix`() {
        @Language("kotlin")
        val testFile = kotlin(
            """
                package slack.text
        
              import android.text.Spanned
        
              class MyClass {
                  fun doCheck(spanned: Spanned): Boolean {
                    return spanned.getSpanFlags(Object()) == Spanned.INCLUSIVE_INCLUSIVE || Spanned.x()
                  }
              }
            """
        ).indented()
        lint()
            .files(testFile)
            .issues(SpanPointMarkDangerousCheckDetector.ISSUE)
            .run()
            .expect(
                """
                src/slack/text/MyClass.kt:7: Error: Do not check against Spanned.INCLUSIVE_INCLUSIVE directly. Instead mask flag with Spanned.SPAN_POINT_MARK_MASK to only check MARK_POINT flags. [SpanPointMarkDangerousCheck]
                      return spanned.getSpanFlags(Object()) == Spanned.INCLUSIVE_INCLUSIVE || Spanned.x()
                             ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                1 errors, 0 warnings
                """.trimIndent()
            )
            .expectFixDiffs(
                """
                Fix for src/slack/text/MyClass.kt line 7: Use bitwise mask:
                @@ -7 +7
                -       return spanned.getSpanFlags(Object()) == Spanned.INCLUSIVE_INCLUSIVE || Spanned.x()
                +       return (spanned.getSpanFlags(Object()) ) and Spanned.SPAN_POINT_MARK_MASK == Spanned.INCLUSIVE_INCLUSIVE || Spanned.x()
                """.trimIndent()
            )
    }
    @Test
    fun `visitBinaryExpression - given violating file with equality and flag on left - creates error and fix`() {
        @Language("kotlin")
        val testFile = kotlin(
            """
                package slack.text
        
              import android.text.Spanned
        
              class MyClass {
                  fun doCheck(spanned: Spanned): Boolean {
                    return Spanned.INCLUSIVE_INCLUSIVE == spanned.getSpanFlags(Object()) || Spanned.x()
                  }
              }
            """
        ).indented()
        lint()
            .files(testFile)
            .issues(SpanPointMarkDangerousCheckDetector.ISSUE)
            .run()
            .expect(
                """
                src/slack/text/MyClass.kt:7: Error: Do not check against Spanned.INCLUSIVE_INCLUSIVE directly. Instead mask flag with Spanned.SPAN_POINT_MARK_MASK to only check MARK_POINT flags. [SpanPointMarkDangerousCheck]
                      return Spanned.INCLUSIVE_INCLUSIVE == spanned.getSpanFlags(Object()) || Spanned.x()
                             ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                1 errors, 0 warnings
                """.trimIndent()
            )
            .expectFixDiffs(
                """
                Fix for src/slack/text/MyClass.kt line 7: Use bitwise mask:
                @@ -7 +7
                -       return spanned.getSpanFlags(Object()) == Spanned.INCLUSIVE_INCLUSIVE || Spanned.x()
                +       return (spanned.getSpanFlags(Object()) ) and Spanned.SPAN_POINT_MARK_MASK == Spanned.INCLUSIVE_INCLUSIVE || Spanned.x()
                """.trimIndent()
            )
    }

    @Test
    fun `visitBinaryExpression - given violating file with inequality and flag on right - creates error and fix`() {
        @Language("kotlin")
        val testFile = kotlin(
            """
                package slack.text
        
              import android.text.Spanned
        
              class MyClass {
                  fun doCheck(spanned: Spanned): Boolean {
                    return spanned.getSpanFlags(Object()) != Spanned.INCLUSIVE_INCLUSIVE || Spanned.x()
                  }
              }
            """
        ).indented()
        lint()
            .files(testFile)
            .issues(SpanPointMarkDangerousCheckDetector.ISSUE)
            .run()
            .expect(
                """
                src/slack/text/MyClass.kt:7: Error: Do not check against Spanned.INCLUSIVE_INCLUSIVE directly. Instead mask flag with Spanned.SPAN_POINT_MARK_MASK to only check MARK_POINT flags. [SpanPointMarkDangerousCheck]
                      return spanned.getSpanFlags(Object()) != Spanned.INCLUSIVE_INCLUSIVE || Spanned.x()
                             ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                1 errors, 0 warnings
                """.trimIndent()
            )
            .expectFixDiffs(
                """
                Fix for src/slack/text/MyClass.kt line 7: Use bitwise mask:
                @@ -7 +7
                -       return spanned.getSpanFlags(Object()) != Spanned.INCLUSIVE_INCLUSIVE || Spanned.x()
                +       return (spanned.getSpanFlags(Object()) ) and Spanned.SPAN_POINT_MARK_MASK != Spanned.INCLUSIVE_INCLUSIVE || Spanned.x()
                """.trimIndent()
            )
    }

    @Test
    fun `visitBinaryExpression - given violating file with inequality and flag on left - creates error and fix`() {
        @Language("kotlin")
        val testFile = kotlin(
            """
                package slack.text
        
              import android.text.Spanned
        
              class MyClass {
                  fun doCheck(spanned: Spanned): Boolean {
                    return Spanned.INCLUSIVE_INCLUSIVE != spanned.getSpanFlags(Object()) || Spanned.x()
                  }
              }
            """
        ).indented()
        lint()
            .files(testFile)
            .issues(SpanPointMarkDangerousCheckDetector.ISSUE)
            .run()
            .expect(
                """
                src/slack/text/MyClass.kt:7: Error: Do not check against Spanned.INCLUSIVE_INCLUSIVE directly. Instead mask flag with Spanned.SPAN_POINT_MARK_MASK to only check MARK_POINT flags. [SpanPointMarkDangerousCheck]
                      return spanned.getSpanFlags(Object()) == Spanned.INCLUSIVE_INCLUSIVE || Spanned.x()
                             ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                1 errors, 0 warnings
                """.trimIndent()
            )
            .expectFixDiffs(
                """
                Fix for src/slack/text/MyClass.kt line 7: Use bitwise mask:
                @@ -7 +7
                -       return spanned.getSpanFlags(Object()) == Spanned.INCLUSIVE_INCLUSIVE || Spanned.x()
                +       return (spanned.getSpanFlags(Object()) ) and Spanned.SPAN_POINT_MARK_MASK == Spanned.INCLUSIVE_INCLUSIVE || Spanned.x()
                """.trimIndent()
            )
    }
}
