// Copyright (C) 2021 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint.mocking

import org.junit.Test
import slack.lint.BaseSlackLintTest

class PlatformTypeMockDetectorTest : BaseSlackLintTest() {

  private val stubs =
    arrayOf(
      kotlin(
        """
              package androidx.collection

              class ArrayMap<K, V>
          """
          .trimIndent()
      )
    )

  override fun getDetector() = MockDetector()

  override fun getIssues() = MockDetector.ISSUES.toList()

  @Test
  fun kotlinTests() {
    val source =
      kotlin(
          "test/test/slack/test/TestClass.kt",
          """
          package slack.test

          import slack.test.mockito.mock
          import java.lang.Runnable
          import kotlin.io.FileTreeWalk
          import android.graphics.Typeface
          import androidx.collection.ArrayMap

          class MyTests {
            fun example() {
              // java.
              mock<Comparable<String>>()
              mock<Runnable>()
              // kotlin.
              mock<FileTreeWalk>()
              mock<Lazy<String>>()
              // android.
              mock<Typeface>()
              // androidx.
              mock<ArrayMap<String, String>>()
            }
          }
        """,
        )
        .indented()

    lint()
      .files(*mockFileStubs(), *stubs, source)
      .run()
      .expect(
        """
          test/test/slack/test/TestClass.kt:12: Error: platform type 'java.lang.Comparable' should not be mocked [DoNotMockPlatformTypes]
              mock<Comparable<String>>()
              ~~~~~~~~~~~~~~~~~~~~~~~~~~
          test/test/slack/test/TestClass.kt:13: Error: platform type 'java.lang.Runnable' should not be mocked [DoNotMockPlatformTypes]
              mock<Runnable>()
              ~~~~~~~~~~~~~~~~
          test/test/slack/test/TestClass.kt:15: Error: platform type 'kotlin.io.FileTreeWalk' should not be mocked [DoNotMockPlatformTypes]
              mock<FileTreeWalk>()
              ~~~~~~~~~~~~~~~~~~~~
          test/test/slack/test/TestClass.kt:16: Error: platform type 'kotlin.Lazy' should not be mocked [DoNotMockPlatformTypes]
              mock<Lazy<String>>()
              ~~~~~~~~~~~~~~~~~~~~
          test/test/slack/test/TestClass.kt:18: Error: platform type 'android.graphics.Typeface' should not be mocked [DoNotMockPlatformTypes]
              mock<Typeface>()
              ~~~~~~~~~~~~~~~~
          test/test/slack/test/TestClass.kt:20: Error: platform type 'androidx.collection.ArrayMap' should not be mocked [DoNotMockPlatformTypes]
              mock<ArrayMap<String, String>>()
              ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
          6 errors, 0 warnings
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

          import static org.mockito.Mockito.mock;
          import java.lang.Runnable;
          import kotlin.Lazy;
          import kotlin.io.FileTreeWalk;
          import android.graphics.Typeface;
          import androidx.collection.ArrayMap;

          class MyTests {
            public void example() {
              // java.
              mock(Comparable.class);
              mock(Runnable.class);
              // kotlin.
              mock(FileTreeWalk.class);
              mock(Lazy.class);
              // android.
              mock(Typeface.class);
              // androidx.
              mock(ArrayMap.class);
            }
          }
        """,
        )
        .indented()

    lint()
      .files(*mockFileStubs(), *stubs, source)
      .run()
      .expect(
        """
          test/test/slack/test/TestClass.java:13: Error: platform type 'java.lang.Comparable' should not be mocked [DoNotMockPlatformTypes]
              mock(Comparable.class);
              ~~~~~~~~~~~~~~~~~~~~~~
          test/test/slack/test/TestClass.java:14: Error: platform type 'java.lang.Runnable' should not be mocked [DoNotMockPlatformTypes]
              mock(Runnable.class);
              ~~~~~~~~~~~~~~~~~~~~
          test/test/slack/test/TestClass.java:16: Error: platform type 'kotlin.io.FileTreeWalk' should not be mocked [DoNotMockPlatformTypes]
              mock(FileTreeWalk.class);
              ~~~~~~~~~~~~~~~~~~~~~~~~
          test/test/slack/test/TestClass.java:17: Error: platform type 'kotlin.Lazy' should not be mocked [DoNotMockPlatformTypes]
              mock(Lazy.class);
              ~~~~~~~~~~~~~~~~
          test/test/slack/test/TestClass.java:19: Error: platform type 'android.graphics.Typeface' should not be mocked [DoNotMockPlatformTypes]
              mock(Typeface.class);
              ~~~~~~~~~~~~~~~~~~~~
          test/test/slack/test/TestClass.java:21: Error: platform type 'androidx.collection.ArrayMap' should not be mocked [DoNotMockPlatformTypes]
              mock(ArrayMap.class);
              ~~~~~~~~~~~~~~~~~~~~
          6 errors, 0 warnings
        """
          .trimIndent()
      )
  }
}
