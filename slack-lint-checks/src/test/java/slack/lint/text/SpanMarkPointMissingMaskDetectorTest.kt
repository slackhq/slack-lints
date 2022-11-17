// Copyright (C) 2021 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint.text

import com.android.tools.lint.checks.infrastructure.TestMode
import org.junit.Test
import slack.lint.BaseSlackLintTest

/** Tests [SpanMarkPointMissingMaskDetector]. */
class SpanMarkPointMissingMaskDetectorTest : BaseSlackLintTest() {

  override val skipTestModes: Array<TestMode> = arrayOf(TestMode.PARENTHESIZED)
  override fun getDetector() = SpanMarkPointMissingMaskDetector()
  override fun getIssues() = listOf(SpanMarkPointMissingMaskDetector.ISSUE)

  private val androidTextStubs =
    kotlin(
        """
        package android.text

        object Spanned {
            const val SPAN_INCLUSIVE_INCLUSIVE = 1
            const val SPAN_INCLUSIVE_EXCLUSIVE = 2
            const val SPAN_EXCLUSIVE_INCLUSIVE = 3
            const val SPAN_EXCLUSIVE_EXCLUSIVE = 4
            const val SPAN_POINT_MARK_MASK = 0xFF
        }
      """
      )
      .indented()

  @Test
  fun `conforming expression - has clean report`() {
    val testFile =
      kotlin(
          """
              package slack.text

              import android.text.Spanned

              class MyClass {
                  fun doCheckCorrectly(spanned: Spanned): Boolean {
                    return spanned.getSpanFlags(Object()) and Spanned.SPAN_POINT_MARK_MASK == Spanned.SPAN_INCLUSIVE_INCLUSIVE
                  }
              }
            """
        )
        .indented()
    lint()
      .files(androidTextStubs, testFile)
      .issues(SpanMarkPointMissingMaskDetector.ISSUE)
      .run()
      .expectClean()
  }

  @Test
  fun `violating expression with INCLUSIVE_INCLUSIVE on left - creates error and fix`() {
    testViolatingExpressionLeft("Spanned.SPAN_INCLUSIVE_INCLUSIVE")
  }

  @Test
  fun `violating expression with INCLUSIVE_EXCLUSIVE on left - creates error and fix`() {
    testViolatingExpressionLeft("Spanned.SPAN_INCLUSIVE_EXCLUSIVE")
  }

  @Test
  fun `violating expression with EXCLUSIVE_INCLUSIVE on left - creates error and fix`() {
    testViolatingExpressionLeft("Spanned.SPAN_EXCLUSIVE_INCLUSIVE")
  }

  @Test
  fun `violating expression with EXCLUSIVE_EXCLUSIVE on left - creates error and fix`() {
    testViolatingExpressionLeft("Spanned.SPAN_EXCLUSIVE_EXCLUSIVE")
  }

  private fun testViolatingExpressionLeft(markPoint: String) {
    val testFile =
      kotlin(
          """
              package slack.text

              import android.text.Spanned

              class MyClass {
                  fun doCheckIncorrectly(spanned: Spanned): Boolean {
                    return spanned.getSpanFlags(Object()) == $markPoint || Spanned.x()
                  }
              }
            """
        )
        .indented()
    lint()
      .files(androidTextStubs, testFile)
      .issues(SpanMarkPointMissingMaskDetector.ISSUE)
      .run()
      .expect(
        """
          src/slack/text/MyClass.kt:7: Error: Do not check against $markPoint directly. Instead mask flag with Spanned.SPAN_POINT_MARK_MASK to only check MARK_POINT flags. [SpanMarkPointMissingMask]
                return spanned.getSpanFlags(Object()) == $markPoint || Spanned.x()
                       ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
          1 errors, 0 warnings
        """
          .trimIndent()
      )
      .expectFixDiffs(
        """
          Fix for src/slack/text/MyClass.kt line 7: Use bitwise mask:
          @@ -7 +7
          -       return spanned.getSpanFlags(Object()) == $markPoint || Spanned.x()
          +       return ((spanned.getSpanFlags(Object())) and android.text.Spanned.SPAN_POINT_MARK_MASK) == $markPoint || Spanned.x()
        """
          .trimIndent()
      )
  }

  @Test
  fun `violating expression with INCLUSIVE_INCLUSIVE on right - creates error and fix`() {
    testViolatingExpressionRight("SPAN_INCLUSIVE_INCLUSIVE")
  }

  @Test
  fun `violating expression with INCLUSIVE_EXCLUSIVE on right - creates error and fix`() {
    testViolatingExpressionRight("SPAN_INCLUSIVE_EXCLUSIVE")
  }

  @Test
  fun `violating expression with EXCLUSIVE_INCLUSIVE on right - creates error and fix`() {
    testViolatingExpressionRight("SPAN_EXCLUSIVE_INCLUSIVE")
  }

  @Test
  fun `violating expression with EXCLUSIVE_EXCLUSIVE on right - creates error and fix`() {
    testViolatingExpressionRight("SPAN_EXCLUSIVE_EXCLUSIVE")
  }

  private fun testViolatingExpressionRight(markPoint: String) {
    val testFile =
      kotlin(
          """
              package slack.text

              import android.text.Spanned.$markPoint

              class MyClass {
                  fun doCheckIncorrectly(spanned: Spanned): Boolean {
                    return $markPoint == spanned.getSpanFlags(Object()) || isBoolean1() && isBoolean2()
                  }
              }
            """
        )
        .indented()
    lint()
      .files(androidTextStubs, testFile)
      .issues(SpanMarkPointMissingMaskDetector.ISSUE)
      .run()
      .expect(
        """
          src/slack/text/MyClass.kt:7: Error: Do not check against $markPoint directly. Instead mask flag with Spanned.SPAN_POINT_MARK_MASK to only check MARK_POINT flags. [SpanMarkPointMissingMask]
                return $markPoint == spanned.getSpanFlags(Object()) || isBoolean1() && isBoolean2()
                       ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
          1 errors, 0 warnings
        """
          .trimIndent()
      )
      .expectFixDiffs(
        """
          Fix for src/slack/text/MyClass.kt line 7: Use bitwise mask:
          @@ -7 +7
          -       return $markPoint == spanned.getSpanFlags(Object()) || isBoolean1() && isBoolean2()
          +       return $markPoint == ((spanned.getSpanFlags(Object())) and android.text.Spanned.SPAN_POINT_MARK_MASK) || isBoolean1() && isBoolean2()
        """
          .trimIndent()
      )
  }

  @Test
  fun `violating expression with fully qualified - creates error and fix`() {
    val markPoint = "android.text.Spanned.SPAN_INCLUSIVE_INCLUSIVE"
    val testFile =
      kotlin(
          """
              package slack.text

              class MyClass {
                  fun doCheckIncorrectly(spanned: Spanned): Boolean {
                    return $markPoint == spanned.getSpanFlags(Object()) || isBoolean1() && isBoolean2()
                  }
              }
            """
        )
        .indented()
    lint()
      .files(androidTextStubs, testFile)
      .issues(SpanMarkPointMissingMaskDetector.ISSUE)
      .run()
      .expect(
        """
          src/slack/text/MyClass.kt:5: Error: Do not check against android.text.Spanned.SPAN_INCLUSIVE_INCLUSIVE directly. Instead mask flag with Spanned.SPAN_POINT_MARK_MASK to only check MARK_POINT flags. [SpanMarkPointMissingMask]
                return $markPoint == spanned.getSpanFlags(Object()) || isBoolean1() && isBoolean2()
                       ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
          1 errors, 0 warnings
        """
          .trimIndent()
      )
      .expectFixDiffs(
        """
          Fix for src/slack/text/MyClass.kt line 5: Use bitwise mask:
          @@ -5 +5
          -       return $markPoint == spanned.getSpanFlags(Object()) || isBoolean1() && isBoolean2()
          +       return $markPoint == ((spanned.getSpanFlags(Object())) and android.text.Spanned.SPAN_POINT_MARK_MASK) || isBoolean1() && isBoolean2()
        """
          .trimIndent()
      )
  }

  @Test
  fun `violating expression with not equal - creates error and fix`() {
    val markPoint = "android.text.Spanned.SPAN_INCLUSIVE_INCLUSIVE"
    val testFile =
      kotlin(
          """
              package slack.text

              class MyClass {
                  fun doCheckIncorrectly(spanned: Spanned): Boolean {
                    return $markPoint != spanned.getSpanFlags(Object()) || isBoolean1() && isBoolean2()
                  }
              }
            """
        )
        .indented()
    lint()
      .files(androidTextStubs, testFile)
      .issues(SpanMarkPointMissingMaskDetector.ISSUE)
      .run()
      .expect(
        """
          src/slack/text/MyClass.kt:5: Error: Do not check against android.text.Spanned.SPAN_INCLUSIVE_INCLUSIVE directly. Instead mask flag with Spanned.SPAN_POINT_MARK_MASK to only check MARK_POINT flags. [SpanMarkPointMissingMask]
                return $markPoint != spanned.getSpanFlags(Object()) || isBoolean1() && isBoolean2()
                       ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
          1 errors, 0 warnings
        """
          .trimIndent()
      )
      .expectFixDiffs(
        """
          Fix for src/slack/text/MyClass.kt line 5: Use bitwise mask:
          @@ -5 +5
          -       return $markPoint != spanned.getSpanFlags(Object()) || isBoolean1() && isBoolean2()
          +       return $markPoint != ((spanned.getSpanFlags(Object())) and android.text.Spanned.SPAN_POINT_MARK_MASK) || isBoolean1() && isBoolean2()
        """
          .trimIndent()
      )
  }

  @Test
  fun `violating expression with identity equality- creates error and fix`() {
    val markPoint = "android.text.Spanned.SPAN_INCLUSIVE_INCLUSIVE"
    val testFile =
      kotlin(
          """
              package slack.text

              class MyClass {
                  fun doCheckIncorrectly(spanned: Spanned): Boolean {
                    return $markPoint === spanned.getSpanFlags(Object()) || isBoolean1() && isBoolean2()
                  }
              }
            """
        )
        .indented()
    lint()
      .files(androidTextStubs, testFile)
      .issues(SpanMarkPointMissingMaskDetector.ISSUE)
      .run()
      .expect(
        """
          src/slack/text/MyClass.kt:5: Error: Do not check against android.text.Spanned.SPAN_INCLUSIVE_INCLUSIVE directly. Instead mask flag with Spanned.SPAN_POINT_MARK_MASK to only check MARK_POINT flags. [SpanMarkPointMissingMask]
                return $markPoint === spanned.getSpanFlags(Object()) || isBoolean1() && isBoolean2()
                       ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
          1 errors, 0 warnings
        """
          .trimIndent()
      )
      .expectFixDiffs(
        """
          Fix for src/slack/text/MyClass.kt line 5: Use bitwise mask:
          @@ -5 +5
          -       return $markPoint === spanned.getSpanFlags(Object()) || isBoolean1() && isBoolean2()
          +       return $markPoint === ((spanned.getSpanFlags(Object())) and android.text.Spanned.SPAN_POINT_MARK_MASK) || isBoolean1() && isBoolean2()
        """
          .trimIndent()
      )
  }

  @Test
  fun `violating expression with not identity equality - creates error and fix`() {
    val markPoint = "android.text.Spanned.SPAN_INCLUSIVE_INCLUSIVE"
    val testFile =
      kotlin(
          """
              package slack.text

              class MyClass {
                  fun doCheckIncorrectly(spanned: Spanned): Boolean {
                    return $markPoint !== spanned.getSpanFlags(Object()) || isBoolean1() && isBoolean2()
                  }
              }
            """
        )
        .indented()
    lint()
      .files(androidTextStubs, testFile)
      .issues(SpanMarkPointMissingMaskDetector.ISSUE)
      .run()
      .expect(
        """
          src/slack/text/MyClass.kt:5: Error: Do not check against android.text.Spanned.SPAN_INCLUSIVE_INCLUSIVE directly. Instead mask flag with Spanned.SPAN_POINT_MARK_MASK to only check MARK_POINT flags. [SpanMarkPointMissingMask]
                return $markPoint !== spanned.getSpanFlags(Object()) || isBoolean1() && isBoolean2()
                       ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
          1 errors, 0 warnings
        """
          .trimIndent()
      )
      .expectFixDiffs(
        """
          Fix for src/slack/text/MyClass.kt line 5: Use bitwise mask:
          @@ -5 +5
          -       return $markPoint !== spanned.getSpanFlags(Object()) || isBoolean1() && isBoolean2()
          +       return $markPoint !== ((spanned.getSpanFlags(Object())) and android.text.Spanned.SPAN_POINT_MARK_MASK) || isBoolean1() && isBoolean2()
        """
          .trimIndent()
      )
  }
}
