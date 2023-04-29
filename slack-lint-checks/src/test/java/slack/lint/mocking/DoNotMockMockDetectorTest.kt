// Copyright (C) 2021 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint.mocking

import org.junit.Test
import slack.lint.BaseSlackLintTest

class DoNotMockMockDetectorTest : BaseSlackLintTest() {

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

  private val testClass =
    kotlin(
        """
      package slack.test

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

      @com.google.errorprone.annotations.DoNotMock
      interface TestClass4 {
        fun fake(): TestClass4? = null
      }
    """
      )
      .indented()

  override fun getDetector() = DoNotMockMockDetector()

  override fun getIssues() = listOf(DoNotMockMockDetector.ISSUE)

  @Test
  fun kotlinTests() {
    val source =
      kotlin(
          "test/test/slack/test/TestClass.kt",
          """
          package slack.test

          import org.mockito.Mock

          class MyTests {
            @Mock lateinit var mock1: TestClass
            @Mock lateinit var mock2: TestClass2
            @Mock lateinit var mock3: TestClass3
            @Mock lateinit var mock4: TestClass4
          }
        """
        )
        .indented()

    lint()
      .files(*mockFileStubs(), slackDoNotMock, epDoNotMock, testClass, source)
      .run()
      .expect(
        """
          test/test/slack/test/TestClass.kt:6: Error: Do not mock TestClass: Use fake() [DoNotMock]
            @Mock lateinit var mock1: TestClass
            ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
          test/test/slack/test/TestClass.kt:7: Error: Do not mock TestClass2: Use fake() [DoNotMock]
            @Mock lateinit var mock2: TestClass2
            ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
          test/test/slack/test/TestClass.kt:8: Error: Do not mock TestClass3: BECAUSE REASONS [DoNotMock]
            @Mock lateinit var mock3: TestClass3
            ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
          test/test/slack/test/TestClass.kt:9: Error: Do not mock TestClass4: BECAUSE REASONS [DoNotMock]
            @Mock lateinit var mock4: TestClass4
            ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
          4 errors, 0 warnings
        """
          .trimIndent()
      )
  }

  @Test
  fun javaTests() {
    val source =
      java(
          "test/test/slack/test/TestClass.java",
          """
          package slack.test;

          import org.mockito.Mock;
          import org.mockito.Spy;
          import static org.mockito.Mockito.mock;
          import static org.mockito.Mockito.spy;

          class MyTests {
            @Mock TestClass mock;
            @Mock TestClass2 mock2;
            @Mock TestClass3 mock3;
            @Mock TestClass4 mock4;

            public void example() {
            }
          }
        """
        )
        .indented()

    lint()
      .files(*mockFileStubs(), slackDoNotMock, epDoNotMock, testClass, source)
      .run()
      .expect(
        """
          test/test/slack/test/TestClass.java:9: Error: Do not mock TestClass: Use fake() [DoNotMock]
            @Mock TestClass mock;
            ~~~~~~~~~~~~~~~~~~~~~
          test/test/slack/test/TestClass.java:10: Error: Do not mock TestClass2: Use fake() [DoNotMock]
            @Mock TestClass2 mock2;
            ~~~~~~~~~~~~~~~~~~~~~~~
          test/test/slack/test/TestClass.java:11: Error: Do not mock TestClass3: BECAUSE REASONS [DoNotMock]
            @Mock TestClass3 mock3;
            ~~~~~~~~~~~~~~~~~~~~~~~
          test/test/slack/test/TestClass.java:12: Error: Do not mock TestClass4: BECAUSE REASONS [DoNotMock]
            @Mock TestClass4 mock4;
            ~~~~~~~~~~~~~~~~~~~~~~~
          4 errors, 0 warnings
        """
          .trimIndent()
      )
  }
}
