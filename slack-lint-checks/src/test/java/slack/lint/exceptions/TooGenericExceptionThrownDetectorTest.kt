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
  fun `error - every default generic type is reported`() {
    lint()
      .files(
        kotlin(
            """
          fun throwsError() { throw Error() }

          fun throwsException() { throw Exception() }

          fun throwsRuntimeException() { throw RuntimeException() }

          fun throwsThrowable() { throw Throwable() }
          """
          )
          .indented()
      )
      .run()
      .expectWarningCount(4)
  }

  @Test
  fun `error - generic exception thrown with a message`() {
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
      .expectContains("RuntimeException is too generic to throw")
  }

  @Test
  fun `clean - NullPointerException is not treated as generic`() {
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
      .expectClean()
  }

  @Test
  fun `clean - generic exception constructed without being thrown`() {
    lint()
      .files(
        kotlin(
            """
          fun example() {
            val error = Exception("built but not thrown")
            println(error)
          }
          """
          )
          .indented()
      )
      .run()
      .expectClean()
  }

  @Test
  fun `clean - generic exception passed as an argument`() {
    lint()
      .files(
        kotlin(
            """
          fun example() {
            throw IllegalStateException(Exception("cause"))
          }
          """
          )
          .indented()
      )
      .run()
      .expectClean()
  }

  @Test
  fun `clean - rethrowing a caught exception`() {
    lint()
      .files(
        kotlin(
            """
          fun example() {
            try {
              doSomething()
            } catch (e: Exception) {
              throw e
            }
          }

          fun doSomething() {}
          """
          )
          .indented()
      )
      .run()
      .expectClean()
  }

  @Test
  fun `clean - throwing the result of a factory call`() {
    lint()
      .files(
        kotlin(
            """
          fun makeException(): Exception = IllegalStateException("specific")

          fun example() {
            throw makeException()
          }
          """
          )
          .indented()
      )
      .run()
      .expectClean()
  }

  @Test
  fun `clean - custom subclass of a generic type`() {
    lint()
      .files(
        kotlin(
            """
          class MyException : RuntimeException()

          fun example() {
            throw MyException()
          }
          """
          )
          .indented()
      )
      .run()
      .expectClean()
  }

  @Test
  fun `error - fully qualified generic type`() {
    lint()
      .files(
        kotlin(
            """
          fun example() {
            throw java.lang.RuntimeException("oops")
          }
          """
          )
          .indented()
      )
      .run()
      .expectContains("RuntimeException is too generic to throw")
  }

  @Test
  fun `error - thrown from a local function`() {
    lint()
      .files(
        kotlin(
            """
          fun example() {
            fun local() {
              throw Exception("from a local function")
            }
            local()
          }
          """
          )
          .indented()
      )
      .run()
      .expectContains("Exception is too generic to throw")
  }

  @Test
  fun `clean - type removed from the configured list`() {
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
      .configureOption(TooGenericExceptionThrownDetector.EXCEPTION_TYPES, "SomeOtherException")
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
            throw UnsupportedOperationException("nope")
          }
          """
          )
          .indented()
      )
      .configureOption(
        TooGenericExceptionThrownDetector.EXCEPTION_TYPES,
        "UnsupportedOperationException",
      )
      .run()
      .expectContains("UnsupportedOperationException is too generic to throw")
  }
}
