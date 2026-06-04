// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint.style

import com.android.tools.lint.checks.infrastructure.TestMode
import org.junit.Test
import slack.lint.BaseSlackLintTest

class VarCouldBeValDetectorTest : BaseSlackLintTest() {
  override fun getDetector() = VarCouldBeValDetector()

  override fun getIssues() = listOf(VarCouldBeValDetector.ISSUE)

  override val skipTestModes = arrayOf(TestMode.WHITESPACE, TestMode.SUPPRESSIBLE)

  @Test
  fun `clean - var is reassigned`() {
    lint()
      .files(
        kotlin(
            """
          class Example {
            private var count = 0

            fun increment() {
              count = count + 1
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
  fun `clean - var reassigned through this`() {
    lint()
      .files(
        kotlin(
            """
          class Example {
            private var count = 0

            fun increment() {
              this.count = this.count + 1
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
  fun `clean - already a val`() {
    lint()
      .files(
        kotlin(
            """
          class Example {
            private val count = 0
          }
          """
          )
          .indented()
      )
      .run()
      .expectClean()
  }

  @Test
  fun `error - var never reassigned`() {
    lint()
      .files(
        kotlin(
            """
          class Example {
            private var count = 0

            fun read(): Int {
              return count
            }
          }
          """
          )
          .indented()
      )
      .run()
      .expectContains("Property count is never reassigned and could be a val")
  }
}
