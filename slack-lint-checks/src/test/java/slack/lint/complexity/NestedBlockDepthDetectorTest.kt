// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint.complexity

import com.android.tools.lint.checks.infrastructure.TestMode
import org.junit.Test
import slack.lint.BaseSlackLintTest

class NestedBlockDepthDetectorTest : BaseSlackLintTest() {
  override fun getDetector() = NestedBlockDepthDetector()

  override fun getIssues() = listOf(NestedBlockDepthDetector.ISSUE)

  override val skipTestModes = arrayOf(TestMode.WHITESPACE, TestMode.SUPPRESSIBLE)

  @Test
  fun `clean - acceptable nesting`() {
    lint()
      .files(
        kotlin(
          """
          fun example(x: Int) {
            if (x > 0) {
              if (x > 1) {
                println("deep enough")
              }
            }
          }
          """
            .trimIndent()
        )
      )
      .run()
      .expectClean()
  }

  @Test
  fun `error - too deeply nested`() {
    lint()
      .files(
        kotlin(
          """
          fun example(x: Int) {
            if (x > 0) {
              if (x > 1) {
                if (x > 2) {
                  if (x > 3) {
                    if (x > 4) {
                      if (x > 5) {
                        if (x > 6) {
                          println("too deep")
                        }
                      }
                    }
                  }
                }
              }
            }
          }
          """
            .trimIndent()
        )
      )
      .run()
      .expectContains("Function has a nested block depth of 7, exceeding the limit of 6")
  }

  @Test
  fun `clean - higher threshold configured`() {
    lint()
      .configureOption(NestedBlockDepthDetector.THRESHOLD, 8)
      .files(
        kotlin(
          """
          fun example(x: Int) {
            if (x > 0) {
              if (x > 1) {
                if (x > 2) {
                  if (x > 3) {
                    if (x > 4) {
                      if (x > 5) {
                        if (x > 6) {
                          println("too deep")
                        }
                      }
                    }
                  }
                }
              }
            }
          }
          """
            .trimIndent()
        )
      )
      .run()
      .expectClean()
  }
}
