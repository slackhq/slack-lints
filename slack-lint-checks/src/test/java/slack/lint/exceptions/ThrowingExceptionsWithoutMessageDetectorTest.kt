// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint.exceptions

import com.android.tools.lint.checks.infrastructure.TestMode
import org.junit.Test
import slack.lint.BaseSlackLintTest

class ThrowingExceptionsWithoutMessageDetectorTest : BaseSlackLintTest() {
  override fun getDetector() = ThrowingExceptionsWithoutMessageDetector()

  override fun getIssues() = listOf(ThrowingExceptionsWithoutMessageDetector.ISSUE)

  override val skipTestModes =
    arrayOf(
      TestMode.WHITESPACE,
      TestMode.SUPPRESSIBLE,
      TestMode.PARENTHESIZED,
      TestMode.FULLY_QUALIFIED,
    )

  @Test
  fun `clean - exception thrown with a message`() {
    lint()
      .files(
        kotlin(
            """
          fun example() {
            throw IllegalStateException("something broke")
          }
          """
          )
          .indented()
      )
      .run()
      .expectClean()
  }

  @Test
  fun `error - exception thrown without a message`() {
    lint()
      .files(
        kotlin(
            """
          fun example() {
            throw IllegalStateException()
          }
          """
          )
          .indented()
      )
      .run()
      .expectContains("IllegalStateException thrown without a message or cause argument")
  }

  @Test
  fun `clean - non-listed exception thrown without a message`() {
    lint()
      .files(
        kotlin(
            """
          class CustomException : Exception()

          fun example() {
            throw CustomException()
          }
          """
          )
          .indented()
      )
      .run()
      .expectClean()
  }
}
