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
