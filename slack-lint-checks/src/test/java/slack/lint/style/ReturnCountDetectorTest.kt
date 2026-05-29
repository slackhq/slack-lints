// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint.style

import com.android.tools.lint.checks.infrastructure.TestMode
import org.junit.Test
import slack.lint.BaseSlackLintTest

class ReturnCountDetectorTest : BaseSlackLintTest() {
  override fun getDetector() = ReturnCountDetector()

  override fun getIssues() = listOf(ReturnCountDetector.ISSUE)

  override val skipTestModes = arrayOf(TestMode.WHITESPACE, TestMode.SUPPRESSIBLE)

  @Test
  fun `clean - function with few returns`() {
    lint()
      .files(
        kotlin(
          """
          fun example(x: Int): String {
            if (x > 0) return "positive"
            if (x < 0) return "negative"
            return "zero"
          }
          """
            .trimIndent()
        )
      )
      .run()
      .expectClean()
  }

  @Test
  fun `error - function with too many returns`() {
    lint()
      .files(
        kotlin(
          """
          fun example(x: Int): String {
            if (x == 1) return "one"
            if (x == 2) return "two"
            if (x == 3) return "three"
            if (x == 4) return "four"
            return "other"
          }
          """
            .trimIndent()
        )
      )
      .run()
      .expect(
        """
        src/test.kt:1: Warning: Function has 5 return statements, exceeding the limit of 4. [ReturnCount]
        fun example(x: Int): String {
            ~~~~~~~
        0 errors, 1 warnings
        """
          .trimIndent()
      )
  }

  @Test
  fun `clean - higher threshold configured`() {
    lint()
      .configureOption(ReturnCountDetector.MAX_RETURNS, 6)
      .files(
        kotlin(
          """
          fun example(x: Int): String {
            if (x == 1) return "one"
            if (x == 2) return "two"
            if (x == 3) return "three"
            if (x == 4) return "four"
            return "other"
          }
          """
            .trimIndent()
        )
      )
      .run()
      .expectClean()
  }
}
