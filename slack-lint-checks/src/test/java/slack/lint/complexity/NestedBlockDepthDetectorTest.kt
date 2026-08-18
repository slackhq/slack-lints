// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint.complexity

import com.android.tools.lint.checks.infrastructure.TestMode
import org.junit.Test
import slack.lint.BaseSlackLintTest

class NestedBlockDepthDetectorTest : BaseSlackLintTest() {
  override fun getDetector() = NestedBlockDepthDetector()

  override fun getIssues() = listOf(NestedBlockDepthDetector.ISSUE)

  // BODY_REMOVAL strips an `if` body's braces, which legitimately changes the nesting shape.
  override val skipTestModes =
    arrayOf(
      TestMode.WHITESPACE,
      TestMode.SUPPRESSIBLE,
      TestMode.IF_TO_WHEN,
      TestMode.BODY_REMOVAL,
    )

  @Test
  fun `clean - acceptable nesting`() {
    lint()
      .files(
        kotlin(
            """
          fun f() {
            if (true) {
              if (true) {
                if (true) {
                }
              }
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
  fun `error - too deeply nested`() {
    lint()
      .files(
        kotlin(
            """
          fun f() {
            if (true) {
              if (true) {
                if (true) {
                  if (true) {
                  }
                }
              }
            }
          }
          """
          )
          .indented()
      )
      .run()
      .expectWarningCount(1)
      .expectContains("Function is nested to a depth of 4, the threshold is 4")
  }

  @Test
  fun `clean - else if does not add depth`() {
    lint()
      .files(
        kotlin(
            """
          fun f() {
            if (true) {
              if (true) {
              } else if (true) {
                if (true) {
                }
              }
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
  fun `error - nesting inside else if branch`() {
    lint()
      .files(
        kotlin(
            """
          fun f() {
            if (true) {
              if (true) {
              } else if (true) {
                if (true) {
                  if (true) {
                  }
                }
              }
            }
          }
          """
          )
          .indented()
      )
      .run()
      .expectWarningCount(1)
  }

  @Test
  fun `error - lambda calls nest`() {
    lint()
      .files(
        kotlin(
            """
          fun f() {
            if (true) {
              5.run {
                this.let {
                  listOf(1, 2, 3).forEach { println(it) }
                }
              }
            }
          }
          """
          )
          .indented()
      )
      .run()
      .expectWarningCount(1)
  }

  @Test
  fun `error - local function is checked`() {
    lint()
      .files(
        kotlin(
            """
          fun outer() {
            fun inner(x: Int) {
              if (x > 0) {
                while (true) {
                  for (i in 0..1) {
                    when (i) {
                      1 -> println(i)
                    }
                  }
                }
              }
            }
          }
          """
          )
          .indented()
      )
      .run()
      .expectContains("Function is nested to a depth of 4")
  }

  @Test
  fun `error - member of anonymous object reported once each`() {
    lint()
      .files(
        kotlin(
            """
          interface Foo

          fun outer() {
            val obj = object : Foo {
              fun helper() {
                if (true) {
                  while (true) {
                    for (i in 0..1) {
                      when (i) {
                        1 -> println(i)
                      }
                    }
                  }
                }
              }
            }
          }
          """
          )
          .indented()
      )
      .run()
      // `outer` counts the nesting in its subtree; `helper` is visited on its own. Neither is
      // reported twice.
      .expectWarningCount(2)
  }

  @Test
  fun `clean - nesting inside a non-scope lambda does not count`() {
    lint()
      .files(
        kotlin(
            """
          fun helper(block: () -> Unit) {}

          fun f() {
            helper {
              if (true) {
                if (true) {
                  if (true) {
                    if (true) {
                    }
                  }
                }
              }
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
  fun `clean - class nesting does not count`() {
    lint()
      .files(
        kotlin(
            """
          class Outer {
            class Middle {
              class Inner {
                fun f() {
                  if (true) {
                  }
                }
              }
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
  fun `clean - higher threshold configured`() {
    lint()
      .configureOption(NestedBlockDepthDetector.THRESHOLD, 8)
      .files(
        kotlin(
            """
          fun f() {
            if (true) {
              if (true) {
                if (true) {
                  if (true) {
                  }
                }
              }
            }
          }
          """
          )
          .indented()
      )
      .run()
      .expectClean()
  }
}
