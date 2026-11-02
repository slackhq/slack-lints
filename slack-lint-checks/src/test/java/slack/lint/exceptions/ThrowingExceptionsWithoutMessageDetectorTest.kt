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
  fun `clean - exception thrown with a cause`() {
    lint()
      .files(
        kotlin(
            """
          fun example(cause: Throwable) {
            throw IllegalStateException(cause)
          }
          """
          )
          .indented()
      )
      .run()
      .expectClean()
  }

  @Test
  fun `clean - message passed as a named argument`() {
    lint()
      .files(
        kotlin(
            """
          fun example() {
            throw IllegalStateException(message = "something broke")
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
      .expectContains("IllegalStateException is created without a message or cause")
  }

  @Test
  fun `error - IOException thrown without a message`() {
    lint()
      .files(
        kotlin(
            """
          import java.io.IOException

          fun example() {
            throw IOException()
          }
          """
          )
          .indented()
      )
      .run()
      .expectContains("IOException is created without a message or cause")
  }

  @Test
  fun `error - exception constructed without being thrown`() {
    lint()
      .files(
        kotlin(
            """
          fun example() {
            val error = IllegalArgumentException()
            println(error)
          }
          """
          )
          .indented()
      )
      .run()
      .expectContains("IllegalArgumentException is created without a message or cause")
  }

  @Test
  fun `error - argument-less exception nested as an argument`() {
    lint()
      .files(
        kotlin(
            """
          fun example() {
            throw IllegalStateException(IllegalArgumentException())
          }
          """
          )
          .indented()
      )
      .run()
      .expectWarningCount(1)
      .expectContains("IllegalArgumentException is created without a message or cause")
  }

  @Test
  fun `clean - exception type not in the list`() {
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

  @Test
  fun `clean - subclass calling a listed supertype constructor`() {
    lint()
      .files(
        kotlin(
            """
          class CustomException : IllegalStateException()

          class WithBody : RuntimeException() {
            fun describe() = "custom"
          }
          """
          )
          .indented()
      )
      .run()
      .expectClean()
  }

  @Test
  fun `clean - function whose name matches an exception ignoring case`() {
    lint()
      .files(
        kotlin(
            """
          fun illegalArgumentException() {
            // no-op
          }

          fun example() {
            illegalArgumentException()
          }
          """
          )
          .indented()
      )
      .run()
      .expectClean()
  }

  @Test
  fun `clean - type removed from the configured list`() {
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
      .configureOption(ThrowingExceptionsWithoutMessageDetector.EXCEPTION_TYPES, "Throwable")
      .run()
      .expectClean()
  }

  @Test
  fun `error - type added to the configured list`() {
    lint()
      .files(
        kotlin(
            """
          fun example() {
            throw UnsupportedOperationException()
          }
          """
          )
          .indented()
      )
      .configureOption(
        ThrowingExceptionsWithoutMessageDetector.EXCEPTION_TYPES,
        "UnsupportedOperationException",
      )
      .run()
      .expectContains("UnsupportedOperationException is created without a message or cause")
  }
}
