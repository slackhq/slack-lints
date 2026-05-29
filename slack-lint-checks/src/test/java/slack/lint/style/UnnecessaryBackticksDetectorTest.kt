// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint.style

import com.android.tools.lint.checks.infrastructure.TestMode
import org.junit.Test
import slack.lint.BaseSlackLintTest

class UnnecessaryBackticksDetectorTest : BaseSlackLintTest() {
  override fun getDetector() = UnnecessaryBackticksDetector()

  override fun getIssues() = listOf(UnnecessaryBackticksDetector.ISSUE)

  override val skipTestModes = arrayOf(TestMode.WHITESPACE, TestMode.SUPPRESSIBLE)

  @Test
  fun `clean - necessary backticks for keyword`() {
    lint()
      .files(
        kotlin(
          """
          val `class` = "test"
          """
        )
        .indented()
      )
      .run()
      .expectClean()
  }

  @Test
  fun `clean - no backticks`() {
    lint()
      .files(
        kotlin(
          """
          val normalName = "test"
          """
        )
        .indented()
      )
      .run()
      .expectClean()
  }

  @Test
  fun `error - unnecessary backticks on function`() {
    lint()
      .files(
        kotlin(
          """
          fun `myFunction`() {}
          """
        )
        .indented()
      )
      .run()
      .expectContains("Unnecessary backticks around")
      .expectContains("myFunction")
  }
}
