// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint.exceptions

import com.android.tools.lint.checks.infrastructure.TestMode
import org.junit.Test
import slack.lint.BaseSlackLintTest

class TooGenericExceptionThrownDetectorTest : BaseSlackLintTest() {
  override fun getDetector() = TooGenericExceptionThrownDetector()

  override fun getIssues() = listOf(TooGenericExceptionThrownDetector.ISSUE)

  override val skipTestModes =
    arrayOf(
      TestMode.WHITESPACE,
      TestMode.SUPPRESSIBLE,
      TestMode.PARENTHESIZED,
      TestMode.FULLY_QUALIFIED,
    )

  @Test
  fun `clean - specific exception thrown`() {
    lint()
      .files(
        kotlin(
            """
          fun example() {
            throw IllegalArgumentException("bad input")
          }
          """
          )
          .indented()
      )
      .run()
      .expectClean()
  }

  @Test
  fun `error - generic exception thrown`() {
    lint()
      .files(
        kotlin(
            """
          fun example() {
            throw RuntimeException("oops")
          }
          """
          )
          .indented()
      )
      .run()
      .expectContains("Thrown exception RuntimeException is too generic")
  }

  @Test
  fun `error - Exception thrown`() {
    lint()
      .files(
        kotlin(
            """
          fun example() {
            throw Exception("oops")
          }
          """
          )
          .indented()
      )
      .run()
      .expectContains("Thrown exception Exception is too generic")
  }

  @Test
  fun `error - NullPointerException thrown`() {
    lint()
      .files(
        kotlin(
            """
          fun example() {
            throw NullPointerException("npe")
          }
          """
          )
          .indented()
      )
      .run()
      .expectContains("Thrown exception NullPointerException is too generic")
  }
}
