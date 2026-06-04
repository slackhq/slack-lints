// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint.complexity

import com.android.tools.lint.checks.infrastructure.TestMode
import org.junit.Test
import slack.lint.BaseSlackLintTest

class TooManyFunctionsDetectorTest : BaseSlackLintTest() {
  override fun getDetector() = TooManyFunctionsDetector()

  override fun getIssues() = listOf(TooManyFunctionsDetector.ISSUE)

  override val skipTestModes =
    arrayOf(TestMode.WHITESPACE, TestMode.SUPPRESSIBLE, TestMode.JVM_OVERLOADS)

  @Test
  fun `clean - class under threshold`() {
    lint()
      .configureOption("threshold", "3")
      .files(
        kotlin(
            """
          class Example {
            fun a() {}
            fun b() {}
            fun c() {}
          }
          """
          )
          .indented()
      )
      .run()
      .expectClean()
  }

  @Test
  fun `error - class with too many functions`() {
    lint()
      .configureOption("threshold", "3")
      .files(
        kotlin(
            """
          class Example {
            fun a() {}
            fun b() {}
            fun c() {}
            fun d() {}
          }
          """
          )
          .indented()
      )
      .run()
      .expectContains("Class Example has 4 functions, exceeding the limit of 3")
  }

  @Test
  fun `clean - ignored annotated class`() {
    lint()
      .configureOption("threshold", "3")
      .files(
        kotlin(
            """
          annotation class Module

          @Module
          class Example {
            fun a() {}
            fun b() {}
            fun c() {}
            fun d() {}
          }
          """
          )
          .indented()
      )
      .run()
      .expectClean()
  }

  @Test
  fun `clean - top-level functions under threshold`() {
    lint()
      .configureOption("threshold", "3")
      .files(
        kotlin(
            """
          fun a() {}
          fun b() {}
          fun c() {}
          """
          )
          .indented()
      )
      .run()
      .expectClean()
  }

  @Test
  fun `error - too many top-level functions`() {
    lint()
      .configureOption("threshold", "3")
      .files(
        kotlin(
            "src/example/Util.kt",
            """
          package example

          fun a() {}
          fun b() {}
          fun c() {}
          fun d() {}
          """,
          )
          .indented()
      )
      .run()
      .expectContains("File Util.kt has 4 top-level functions, exceeding the limit of 3")
  }
}
