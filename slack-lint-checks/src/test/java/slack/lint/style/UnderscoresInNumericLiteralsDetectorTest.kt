// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint.style

import com.android.tools.lint.checks.infrastructure.TestMode
import org.junit.Test
import slack.lint.BaseSlackLintTest

class UnderscoresInNumericLiteralsDetectorTest : BaseSlackLintTest() {
  override fun getDetector() = UnderscoresInNumericLiteralsDetector()

  override fun getIssues() = listOf(UnderscoresInNumericLiteralsDetector.ISSUE)

  override val skipTestModes = arrayOf(TestMode.WHITESPACE, TestMode.SUPPRESSIBLE)

  @Test
  fun `clean - short literal`() {
    lint()
      .files(
        kotlin(
            """
          val value = 1000
          """
          )
          .indented()
      )
      .run()
      .expectClean()
  }

  @Test
  fun `clean - long literal with underscores`() {
    lint()
      .files(
        kotlin(
            """
          val value = 1_000_000
          """
          )
          .indented()
      )
      .run()
      .expectClean()
  }

  @Test
  fun `error - long literal without underscores`() {
    lint()
      .files(
        kotlin(
            """
          val value = 1000000
          """
          )
          .indented()
      )
      .run()
      .expectContains("Numeric literal 1000000 should use underscores for readability")
  }

  @Test
  fun `clean - higher acceptable length configured`() {
    lint()
      .configureOption("acceptable-length", "8")
      .files(
        kotlin(
            """
          val value = 1000000
          """
          )
          .indented()
      )
      .run()
      .expectClean()
  }
}
