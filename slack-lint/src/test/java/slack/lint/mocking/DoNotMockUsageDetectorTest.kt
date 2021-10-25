/*
 * Copyright (C) 2021 Slack Technologies, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package slack.lint.mocking

import org.junit.Test
import slack.lint.BaseSlackLintTest

class DoNotMockUsageDetectorTest : BaseSlackLintTest() {

  private val slackDoNotMock = kotlin(
    """
      package slack.lint.annotations

      annotation class DoNotMock(val value: String = "BECAUSE REASONS")
    """
  ).indented()

  private val epDoNotMock = kotlin(
    """
      package com.google.errorprone.annotations

      annotation class DoNotMock(val value: String = "BECAUSE REASONS")
    """
  ).indented()

  override fun getDetector() = ErrorProneDoNotMockDetector()
  override fun getIssues() = listOf(ErrorProneDoNotMockDetector.ISSUE)

  @Test
  fun kotlinTests() {
    val source = kotlin(
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
    ).indented()

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
        """.trimIndent()
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
        """.trimIndent()
      )
  }

  @Test
  fun javaTests() {
    val source = java(
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
    ).indented()

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
        """.trimIndent()
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
        """.trimIndent()
      )
  }
}
