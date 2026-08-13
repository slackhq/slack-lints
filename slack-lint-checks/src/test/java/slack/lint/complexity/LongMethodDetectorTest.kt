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
      // Body spans 6 distinct code lines: the `{`, four println statements, and the `}`.
      .expectContains("Function is too long (6 lines), exceeding the limit of 3")
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

  @Test
  fun `clean - blank lines and comments are not counted`() {
    // Body has 6 physical lines between braces but only 4 lines carry code tokens
    // (`{`, two printlns, `}`). A raw newline count would flag this; counting code lines does not.
    lint()
      .configureOption("threshold", "4")
      .files(
        kotlin(
            """
          fun example() {
            // a comment
            println("1")

            println("2")
          }
          """
          )
          .indented()
      )
      .run()
      .expectClean()
  }

  @Test
  fun `clean - nested function lines are subtracted from the enclosing function`() {
    // outer's body spans 8 code lines, but inner's 4 lines are subtracted, leaving 4.
    lint()
      .configureOption("threshold", "5")
      .files(
        kotlin(
            """
          fun outer() {
            println("a")
            fun inner() {
              println("b")
              println("c")
            }
            println("d")
          }
          """
          )
          .indented()
      )
      .run()
      .expectClean()
  }

  @Test
  fun `clean - deeply nested function lines are subtracted at every level`() {
    // outer's body spans 11 code lines; subtracting the nested functions leaves 4. A broken
    // subtraction (e.g. counting the whole body) would flag this at threshold 5.
    lint()
      .configureOption("threshold", "5")
      .files(
        kotlin(
            """
          fun outer() {
            println("a")
            fun middle() {
              println("b")
              fun inner() {
                println("c")
              }
              println("d")
            }
            println("e")
          }
          """
          )
          .indented()
      )
      .run()
      .expectClean()
  }

  @Test
  fun `error - expression body functions are measured`() {
    lint()
      .configureOption("threshold", "2")
      .files(
        kotlin(
            """
          fun example() =
            listOf(1, 2, 3)
              .map { it * 2 }
              .filter { it > 2 }
          """
          )
          .indented()
      )
      .run()
      .expectContains("exceeding the limit of 2")
  }
}
