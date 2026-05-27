// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint.detekt

import com.android.tools.lint.checks.infrastructure.TestMode
import org.junit.Test
import slack.lint.BaseSlackLintTest

class FunctionNamingDetectorTest : BaseSlackLintTest() {
  override fun getDetector() = FunctionNamingDetector()

  override fun getIssues() = listOf(FunctionNamingDetector.ISSUE)

  override val skipTestModes = arrayOf(TestMode.WHITESPACE, TestMode.SUPPRESSIBLE)

  @Test
  fun `clean - camelCase function`() {
    lint()
      .files(
        kotlin(
          """
          fun myFunction() {}
          fun anotherOne() {}
          """
            .trimIndent()
        )
      )
      .run()
      .expectClean()
  }

  @Test
  fun `error - PascalCase function`() {
    lint()
      .files(
        kotlin(
          """
          fun MyFunction() {}
          """
            .trimIndent()
        )
      )
      .run()
      .expectContains("Function name")
      .expectContains("MyFunction")
      .expectContains("should start with a lowercase letter")
  }

  @Test
  fun `clean - Composable function`() {
    lint()
      .files(
        kotlin(
          """
          annotation class Composable

          @Composable
          fun MyScreen() {}
          """
            .trimIndent()
        )
      )
      .run()
      .expectClean()
  }

  @Test
  fun `clean - Test function`() {
    lint()
      .files(
        kotlin(
          """
          annotation class Test

          @Test
          fun MyTest_does_something() {}
          """
            .trimIndent()
        )
      )
      .run()
      .expectClean()
  }
}
