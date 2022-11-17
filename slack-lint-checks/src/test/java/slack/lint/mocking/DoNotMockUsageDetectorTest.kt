// Copyright (C) 2021 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint.mocking

import org.junit.Test
import slack.lint.BaseSlackLintTest

class DoNotMockUsageDetectorTest : BaseSlackLintTest() {

  private val slackDoNotMock =
    kotlin(
        """
      package slack.lint.annotations

      annotation class DoNotMock(val value: String = "BECAUSE REASONS")
    """
      )
      .indented()

  private val epDoNotMock =
    kotlin(
        """
      package com.google.errorprone.annotations

      annotation class DoNotMock(val value: String = "BECAUSE REASONS")
    """
      )
      .indented()

  override fun getDetector() = ErrorProneDoNotMockDetector()
  override fun getIssues() = listOf(ErrorProneDoNotMockDetector.ISSUE)

  @Test
  fun kotlinTests() {
    val source =
      kotlin(
          """
      package slack.test

      import com.google.errorprone.annotations.DoNotMock

      @slack.lint.annotations.DoNotMock("Use fake()")
      interface TestClass {
        fun fake(): TestClass? = null
      }

      @com.google.errorprone.annotations.DoNotMock("Use fake()")
      interface TestClass2 {
        fun fake(): TestClass2? = null
      }

      @slack.lint.annotations.DoNotMock
      interface TestClass3 {
        fun fake(): TestClass3? = null
      }

      @DoNotMock
      interface TestClass4 {
        fun fake(): TestClass4? = null
      }
    """
        )
        .indented()

    lint()
      .files(*mockFileStubs(), slackDoNotMock, epDoNotMock, source)
      .run()
      .expect(
        """
          src/slack/test/TestClass.kt:10: Error: Use Slack's internal @DoNotMock annotation. [ErrorProneDoNotMockUsage]
          @com.google.errorprone.annotations.DoNotMock("Use fake()")
          ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
          src/slack/test/TestClass.kt:20: Error: Use Slack's internal @DoNotMock annotation. [ErrorProneDoNotMockUsage]
          @DoNotMock
          ~~~~~~~~~~
          2 errors, 0 warnings
        """
          .trimIndent()
      )
      .expectFixDiffs(
        """
          Fix for src/slack/test/TestClass.kt line 10: Replace with slack.lint.annotations.DoNotMock:
          @@ -10 +10
          - @com.google.errorprone.annotations.DoNotMock("Use fake()")
          + @slack.lint.annotations.DoNotMock("Use fake()")
          Fix for src/slack/test/TestClass.kt line 20: Replace with slack.lint.annotations.DoNotMock:
          @@ -20 +20
          - @DoNotMock
          + @slack.lint.annotations.DoNotMock
        """
          .trimIndent()
      )
  }

  @Test
  fun javaTests() {
    val source =
      java(
          """
      package slack.test;

      import com.google.errorprone.annotations.DoNotMock;

      @slack.lint.annotations.DoNotMock("Use fake()")
      interface TestClass {
      }

      @com.google.errorprone.annotations.DoNotMock("Use fake()")
      interface TestClass2 {
      }

      @slack.lint.annotations.DoNotMock
      interface TestClass3 {
      }

      @DoNotMock
      interface TestClass4 {
      }
    """
        )
        .indented()

    lint()
      .files(*mockFileStubs(), slackDoNotMock, epDoNotMock, source)
      .run()
      .expect(
        """
          src/slack/test/TestClass.java:9: Error: Use Slack's internal @DoNotMock annotation. [ErrorProneDoNotMockUsage]
          @com.google.errorprone.annotations.DoNotMock("Use fake()")
          ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
          src/slack/test/TestClass.java:17: Error: Use Slack's internal @DoNotMock annotation. [ErrorProneDoNotMockUsage]
          @DoNotMock
          ~~~~~~~~~~
          2 errors, 0 warnings
        """
          .trimIndent()
      )
      .expectFixDiffs(
        """
          Fix for src/slack/test/TestClass.java line 9: Replace with slack.lint.annotations.DoNotMock:
          @@ -9 +9
          - @com.google.errorprone.annotations.DoNotMock("Use fake()")
          + @slack.lint.annotations.DoNotMock("Use fake()")
          Fix for src/slack/test/TestClass.java line 17: Replace with slack.lint.annotations.DoNotMock:
          @@ -17 +17
          - @DoNotMock
          + @slack.lint.annotations.DoNotMock
        """
          .trimIndent()
      )
  }
}
