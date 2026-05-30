// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint.style

import com.android.tools.lint.checks.infrastructure.TestMode
import org.junit.Test
import slack.lint.BaseSlackLintTest

class FunctionOnlyReturningConstantDetectorTest : BaseSlackLintTest() {
  override fun getDetector() = FunctionOnlyReturningConstantDetector()

  override fun getIssues() = listOf(FunctionOnlyReturningConstantDetector.ISSUE)

  override val skipTestModes = arrayOf(TestMode.WHITESPACE, TestMode.SUPPRESSIBLE)

  @Test
  fun `clean - function with logic`() {
    lint()
      .files(
        kotlin(
            """
          fun example(x: Int): Int {
            return x + 1
          }
          """
          )
          .indented()
      )
      .run()
      .expectClean()
  }

  @Test
  fun `error - block body returning a constant`() {
    lint()
      .files(
        kotlin(
            """
          fun example(): Int {
            return 42
          }
          """
          )
          .indented()
      )
      .run()
      .expectContains("Function example only returns a constant")
  }

  @Test
  fun `error - expression body returning a constant`() {
    lint()
      .files(
        kotlin(
            """
          fun example(): Int = 42
          """
          )
          .indented()
      )
      .run()
      .expectContains("Function example only returns a constant")
  }

  @Test
  fun `clean - excluded function name`() {
    lint()
      .files(
        kotlin(
            """
          fun describeContents(): Int = 0
          """
          )
          .indented()
      )
      .run()
      .expectClean()
  }
}
