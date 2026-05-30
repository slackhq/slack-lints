// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint.style

import com.android.tools.lint.checks.infrastructure.TestMode
import org.junit.Test
import slack.lint.BaseSlackLintTest

class MagicNumberDetectorTest : BaseSlackLintTest() {
  override fun getDetector() = MagicNumberDetector()

  override fun getIssues() = listOf(MagicNumberDetector.ISSUE)

  override val skipTestModes = arrayOf(TestMode.WHITESPACE, TestMode.SUPPRESSIBLE)

  @Test
  fun `clean - allowed numbers`() {
    lint()
      .files(
        kotlin(
            "src/main/java/example/Example.kt",
            """
          package example

          fun example() {
            consume(0)
            consume(1)
            consume(2)
          }
          fun consume(value: Int) {}
          """,
          )
          .indented()
      )
      .run()
      .expectClean()
  }

  @Test
  fun `clean - extracted to named constant`() {
    lint()
      .files(
        kotlin(
            "src/main/java/example/Example.kt",
            """
          package example

          const val TIMEOUT = 100

          fun example() {
            consume(TIMEOUT)
          }
          fun consume(value: Int) {}
          """,
          )
          .indented()
      )
      .run()
      .expectClean()
  }

  @Test
  fun `clean - non-const property initializer`() {
    // A literal in a property declaration is itself the named value; flagging it would be
    // inconsistent with how local vals are treated.
    lint()
      .files(
        kotlin(
            "src/main/java/example/Example.kt",
            """
          package example

          class Config {
            val timeout = 100
          }
          """,
          )
          .indented()
      )
      .run()
      .expectClean()
  }

  @Test
  fun `clean - local val initializer`() {
    lint()
      .files(
        kotlin(
            "src/main/java/example/Example.kt",
            """
          package example

          fun example() {
            val timeout = 100
            consume(timeout)
          }
          fun consume(value: Int) {}
          """,
          )
          .indented()
      )
      .run()
      .expectClean()
  }

  @Test
  fun `error - magic number in expression`() {
    lint()
      .files(
        kotlin(
            "src/main/java/example/Example.kt",
            """
          package example

          fun example() {
            consume(100)
          }
          fun consume(value: Int) {}
          """,
          )
          .indented()
      )
      .run()
      .expectContains("Magic number 100. Extract to a named constant.")
  }
}
