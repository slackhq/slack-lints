// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint.exceptions

import com.android.tools.lint.checks.infrastructure.TestMode
import org.junit.Test
import slack.lint.BaseSlackLintTest

class TooGenericExceptionCaughtDetectorTest : BaseSlackLintTest() {
  override fun getDetector() = TooGenericExceptionCaughtDetector()

  override fun getIssues() = listOf(TooGenericExceptionCaughtDetector.ISSUE)

  override val skipTestModes = arrayOf(TestMode.WHITESPACE, TestMode.SUPPRESSIBLE)

  @Test
  fun `clean - specific exception caught`() {
    lint()
      .files(
        kotlin(
            """
          fun example() {
            try {
              doSomething()
            } catch (e: IllegalArgumentException) {
              println(e.message)
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
  fun `error - generic exception caught`() {
    lint()
      .files(
        kotlin(
            """
          fun example() {
            try {
              doSomething()
            } catch (e: Exception) {
              println(e.message)
            }
          }
          fun doSomething() {}
          """
          )
          .indented()
      )
      .run()
      .expectContains("Caught exception type Exception is too generic")
  }

  @Test
  fun `error - Throwable caught`() {
    lint()
      .files(
        kotlin(
            """
          fun example() {
            try {
              doSomething()
            } catch (e: Throwable) {
              println(e.message)
            }
          }
          fun doSomething() {}
          """
          )
          .indented()
      )
      .run()
      .expectContains("Caught exception type Throwable is too generic")
  }

  @Test
  fun `clean - exception named ignored`() {
    lint()
      .files(
        kotlin(
            """
          fun example() {
            try {
              doSomething()
            } catch (ignored: Exception) {
              println("skip")
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
  fun `clean - exception named expected`() {
    lint()
      .files(
        kotlin(
            """
          fun example() {
            try {
              doSomething()
            } catch (expectedFailure: Throwable) {
              println("skip")
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
  fun `error - every default generic type is reported`() {
    lint()
      .files(
        kotlin(
            """
          fun example() {
            try { doSomething() } catch (a: ArrayIndexOutOfBoundsException) { println(a) }
            try { doSomething() } catch (b: Error) { println(b) }
            try { doSomething() } catch (c: Exception) { println(c) }
            try { doSomething() } catch (d: IllegalMonitorStateException) { println(d) }
            try { doSomething() } catch (e: IndexOutOfBoundsException) { println(e) }
            try { doSomething() } catch (g: NullPointerException) { println(g) }
            try { doSomething() } catch (h: RuntimeException) { println(h) }
            try { doSomething() } catch (i: Throwable) { println(i) }
          }

          fun doSomething() {}
          """
          )
          .indented()
      )
      .run()
      .expectWarningCount(8)
  }

  @Test
  fun `error - fully qualified generic type`() {
    lint()
      .files(
        kotlin(
            """
          fun example() {
            try {
              doSomething()
            } catch (e: java.lang.Exception) {
              println(e)
            }
          }

          fun doSomething() {}
          """
          )
          .indented()
      )
      .run()
      .expectContains("Caught exception type Exception is too generic")
  }

  @Test
  fun `clean - custom subclass of a generic type`() {
    lint()
      .files(
        kotlin(
            """
          class MyException : RuntimeException()

          fun example() {
            try {
              doSomething()
            } catch (e: MyException) {
              println(e)
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
  fun `error - name only containing the allowed pattern`() {
    lint()
      .files(
        kotlin(
            """
          fun example() {
            try {
              doSomething()
            } catch (pleaseIgnoreThis: Exception) {
              println("skip")
            }
          }

          fun doSomething() {}
          """
          )
          .indented()
      )
      .run()
      .expectContains("Caught exception type Exception is too generic")
  }

  @Test
  fun `clean - type removed from the configured list`() {
    lint()
      .files(
        kotlin(
            """
          fun example() {
            try {
              doSomething()
            } catch (e: Exception) {
              println(e)
            }
          }

          fun doSomething() {}
          """
          )
          .indented()
      )
      .configureOption(TooGenericExceptionCaughtDetector.EXCEPTION_TYPES, "SomeOtherException")
      .run()
      .expectClean()
  }

  @Test
  fun `clean - configured allowed exception name`() {
    lint()
      .files(
        kotlin(
            """
          fun example() {
            try {
              doSomething()
            } catch (myIgnore: Exception) {
              println("skip")
            }
          }

          fun doSomething() {}
          """
          )
          .indented()
      )
      .configureOption(TooGenericExceptionCaughtDetector.ALLOWED_EXCEPTION_NAME, "myIgnore")
      .run()
      .expectClean()
  }

  @Test
  fun `clean - exception named underscore`() {
    lint()
      .files(
        kotlin(
            """
          fun example() {
            try {
              doSomething()
            } catch (_: Exception) {
              println("skip")
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
}
