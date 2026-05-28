// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint.detekt

import com.android.tools.lint.checks.infrastructure.TestMode
import org.junit.Test
import slack.lint.BaseSlackLintTest

class LongParameterListDetectorTest : BaseSlackLintTest() {
  override fun getDetector() = LongParameterListDetector()

  override fun getIssues() = listOf(LongParameterListDetector.ISSUE)

  override val skipTestModes =
    arrayOf(TestMode.WHITESPACE, TestMode.SUPPRESSIBLE, TestMode.JVM_OVERLOADS)

  @Test
  fun `clean - function with acceptable parameters`() {
    lint()
      .files(
        kotlin(
          """
          fun example(a: Int, b: Int, c: Int, d: Int, e: Int, f: Int, g: Int) {}
          """
            .trimIndent()
        )
      )
      .run()
      .expectClean()
  }

  @Test
  fun `error - function with too many parameters`() {
    lint()
      .files(
        kotlin(
          """
          fun example(a: Int, b: Int, c: Int, d: Int, e: Int, f: Int, g: Int, h: Int) {}
          """
            .trimIndent()
        )
      )
      .run()
      .expectContains("Function has 8 parameters, exceeding the limit of 7")
  }

  @Test
  fun `clean - ignoreAnnotated function`() {
    lint()
      .files(
        kotlin(
          """
          annotation class Inject

          @Inject
          fun example(a: Int, b: Int, c: Int, d: Int, e: Int, f: Int, g: Int, h: Int) {}
          """
            .trimIndent()
        )
      )
      .run()
      .expectClean()
  }

  @Test
  fun `clean - higher threshold configured`() {
    lint()
      .configureOption(LongParameterListDetector.FUNCTION_THRESHOLD, 10)
      .files(
        kotlin(
          """
          fun example(a: Int, b: Int, c: Int, d: Int, e: Int, f: Int, g: Int, h: Int) {}
          """
            .trimIndent()
        )
      )
      .run()
      .expectClean()
  }

  @Test
  fun `clean - custom ignoreAnnotated configured`() {
    lint()
      .configureOption(LongParameterListDetector.IGNORE_ANNOTATED, "MyCustomAnnotation")
      .files(
        kotlin(
          """
          annotation class MyCustomAnnotation

          @MyCustomAnnotation
          fun example(a: Int, b: Int, c: Int, d: Int, e: Int, f: Int, g: Int, h: Int) {}
          """
            .trimIndent()
        )
      )
      .run()
      .expectClean()
  }
}
