// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint.naming

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
          )
          .indented()
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
          )
          .indented()
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
          )
          .indented()
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
          )
          .indented()
      )
      .run()
      .expectClean()
  }
}
