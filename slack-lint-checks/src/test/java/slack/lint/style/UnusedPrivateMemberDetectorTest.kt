// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint.style

import com.android.tools.lint.checks.infrastructure.TestMode
import org.junit.Test
import slack.lint.BaseSlackLintTest

class UnusedPrivateMemberDetectorTest : BaseSlackLintTest() {
  override fun getDetector() = UnusedPrivateMemberDetector()

  override fun getIssues() = listOf(UnusedPrivateMemberDetector.ISSUE)

  override val skipTestModes = arrayOf(TestMode.WHITESPACE, TestMode.SUPPRESSIBLE)

  @Test
  fun `clean - private field is read`() {
    lint()
      .files(
        kotlin(
            """
          class Example {
            private val limit = 5

            fun read(): Int {
              return limit
            }
          }
          """
          )
          .indented()
      )
      .run()
      .expectClean()
  }

  @Test
  fun `clean - private function is called`() {
    lint()
      .files(
        kotlin(
            """
          class Example {
            private fun helper() {}

            fun doWork() {
              helper()
            }
          }
          """
          )
          .indented()
      )
      .run()
      .expectClean()
  }

  @Test
  fun `error - unused private function`() {
    lint()
      .files(
        kotlin(
            """
          class Example {
            private fun helper() {}

            fun doWork() {}
          }
          """
          )
          .indented()
      )
      .run()
      .expectContains("Private member helper is never used")
  }

  @Test
  fun `error - unused private field`() {
    lint()
      .files(
        kotlin(
            """
          class Example {
            private val unused = 5

            fun doWork() {}
          }
          """
          )
          .indented()
      )
      .run()
      .expectContains("Private member unused is never used")
  }
}
