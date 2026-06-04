// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint.complexity

import com.android.tools.lint.checks.infrastructure.TestMode
import org.junit.Test
import slack.lint.BaseSlackLintTest

class LongMethodDetectorTest : BaseSlackLintTest() {
  override fun getDetector() = LongMethodDetector()

  override fun getIssues() = listOf(LongMethodDetector.ISSUE)

  override val skipTestModes = arrayOf(TestMode.WHITESPACE, TestMode.SUPPRESSIBLE)

  @Test
  fun `clean - short function under default threshold`() {
    lint()
      .files(
        kotlin(
            """
          fun example() {
            println("1")
            println("2")
            println("3")
          }
          """
          )
          .indented()
      )
      .run()
      .expectClean()
  }

  @Test
  fun `error - function body exceeds configured threshold`() {
    lint()
      .configureOption("threshold", "3")
      .files(
        kotlin(
            """
          fun example() {
            println("1")
            println("2")
            println("3")
            println("4")
          }
          """
          )
          .indented()
      )
      .run()
      .expectContains("exceeding the limit of 3")
  }

  @Test
  fun `clean - long body allowed when threshold raised`() {
    lint()
      .configureOption("threshold", "10")
      .files(
        kotlin(
            """
          fun example() {
            println("1")
            println("2")
            println("3")
            println("4")
          }
          """
          )
          .indented()
      )
      .run()
      .expectClean()
  }
}
