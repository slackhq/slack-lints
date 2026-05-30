// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint.naming

import com.android.tools.lint.checks.infrastructure.TestMode
import org.junit.Test
import slack.lint.BaseSlackLintTest

class TopLevelPropertyNamingDetectorTest : BaseSlackLintTest() {
  override fun getDetector() = TopLevelPropertyNamingDetector()

  override fun getIssues() = listOf(TopLevelPropertyNamingDetector.ISSUE)

  override val skipTestModes = arrayOf(TestMode.WHITESPACE, TestMode.SUPPRESSIBLE)

  @Test
  fun `clean - upper case top-level constant`() {
    lint()
      .files(
        kotlin(
            """
          const val MAX_COUNT = 10
          """
          )
          .indented()
      )
      .run()
      .expectClean()
  }

  @Test
  fun `error - lowercase top-level constant`() {
    lint()
      .files(
        kotlin(
            """
          const val maxCount = 10
          """
          )
          .indented()
      )
      .run()
      .expectContains("Top-level constant maxCount does not match pattern")
  }

  @Test
  fun `clean - non-const top-level property`() {
    lint()
      .files(
        kotlin(
            """
          val maxCount = 10
          """
          )
          .indented()
      )
      .run()
      .expectClean()
  }

  @Test
  fun `clean - custom pattern configured`() {
    lint()
      .configureOption("constant-pattern", "[a-z][A-Za-z0-9]*")
      .files(
        kotlin(
            """
          const val maxCount = 10
          """
          )
          .indented()
      )
      .run()
      .expectClean()
  }
}
