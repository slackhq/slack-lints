// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint.style

import com.android.tools.lint.checks.infrastructure.TestMode
import org.junit.Test
import slack.lint.BaseSlackLintTest

class UnnecessaryAbstractClassDetectorTest : BaseSlackLintTest() {
  override fun getDetector() = UnnecessaryAbstractClassDetector()

  override fun getIssues() = listOf(UnnecessaryAbstractClassDetector.ISSUE)

  override val skipTestModes = arrayOf(TestMode.WHITESPACE, TestMode.SUPPRESSIBLE)

  @Test
  fun `clean - abstract class with abstract member`() {
    lint()
      .files(
        kotlin(
            """
          abstract class Example {
            abstract fun doWork()
          }
          """
          )
          .indented()
      )
      .run()
      .expectClean()
  }

  @Test
  fun `clean - abstract class with abstract property`() {
    lint()
      .files(
        kotlin(
            """
          abstract class Example {
            abstract val name: String
          }
          """
          )
          .indented()
      )
      .run()
      .expectClean()
  }

  @Test
  fun `clean - concrete class`() {
    lint()
      .files(
        kotlin(
            """
          class Example {
            fun doWork() {}
          }
          """
          )
          .indented()
      )
      .run()
      .expectClean()
  }

  @Test
  fun `error - abstract class without abstract members`() {
    lint()
      .files(
        kotlin(
            """
          abstract class Example {
            fun doWork() {}
          }
          """
          )
          .indented()
      )
      .run()
      .expectContains("Abstract class Example has no abstract members")
  }

  @Test
  fun `clean - ignored annotated abstract class`() {
    lint()
      .configureOption("ignore-annotated", "Deprecated")
      .files(
        kotlin(
            """
          @Deprecated("legacy")
          abstract class Example {
            fun doWork() {}
          }
          """
          )
          .indented()
      )
      .run()
      .expectClean()
  }
}
