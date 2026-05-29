// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint.exceptions

import com.android.tools.lint.checks.infrastructure.TestMode
import org.junit.Test
import slack.lint.BaseSlackLintTest

class SwallowedExceptionDetectorTest : BaseSlackLintTest() {
  override fun getDetector() = SwallowedExceptionDetector()

  override fun getIssues() = listOf(SwallowedExceptionDetector.ISSUE)

  override val skipTestModes = arrayOf(TestMode.WHITESPACE, TestMode.SUPPRESSIBLE)

  @Test
  fun `clean - exception is used`() {
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
            .trimIndent()
        )
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
              println("failed")
            }
          }
          fun doSomething() {}
          """
            .trimIndent()
        )
      )
      .run()
      .expectClean()
  }

  @Test
  fun `error - exception is swallowed`() {
    lint()
      .files(
        kotlin(
          """
          fun example() {
            try {
              doSomething()
            } catch (e: Exception) {
              println("failed")
            }
          }
          fun doSomething() {}
          """
            .trimIndent()
        )
      )
      .run()
      .expectContains("Exception")
      .expectContains("is caught but never used")
  }
}
