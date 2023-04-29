// Copyright (C) 2021 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint

import com.android.tools.lint.checks.infrastructure.TestMode
import com.android.tools.lint.detector.api.Detector
import org.junit.Test

class GuavaPreconditionsDetectorTest : BaseSlackLintTest() {

  override val skipTestModes: Array<TestMode> = arrayOf(TestMode.WHITESPACE)

  override fun getDetector(): Detector = GuavaPreconditionsDetector()

  override fun getIssues() = GuavaPreconditionsDetector.issues.toList()

  private val guavaPreconditionsStub =
    java(
        """
      package com.google.common.base;

      public final class Preconditions {
          public static void checkState(boolean expression) {}
          public static void checkArgument(boolean expression) {}
          public static <T extends Object> T checkNotNull(T reference) {}
          public static int checkElementIndex(int index, int size) { return 0; }
      }
    """
      )
      .indented()

  private val slackPreconditionsStub =
    kotlin(
        """
      @file:JvmName("JavaPreconditions")

      package slack.commons
      fun check(condition: Boolean) {}
      fun require(condition: Boolean) {}
      fun checkNotNull(condition: Boolean) {}
      fun <T> checkNotNull(value: T): T {}
    """
          .trimIndent()
      )
      .indented()

  @Test
  fun `Java - Using Guava Preconditions with static reference will show warnings`() {
    lint()
      .files(
        guavaPreconditionsStub,
        java(
            """
            package foo;

            import com.google.common.base.Preconditions;

            public class Foo {

              boolean isTrue = Preconditions.checkState(1 == 1);

              void act() {
                Preconditions.checkState(1 == 1);
                Preconditions.checkArgument(1 == 1);
                Preconditions.checkNotNull("Hello");
                Preconditions.checkElementIndex(0, 1);
              }
            }
          """
          )
          .indented()
      )
      .issues(*GuavaPreconditionsDetector.issues)
      .run()
      .expect(
        """
          src/foo/Foo.java:7: Error: Use Slack's JavaPreconditions instead of Guava's Preconditions checks [GuavaChecksUsed]
            boolean isTrue = Preconditions.checkState(1 == 1);
                                           ~~~~~~~~~~
          src/foo/Foo.java:10: Error: Use Slack's JavaPreconditions instead of Guava's Preconditions checks [GuavaChecksUsed]
              Preconditions.checkState(1 == 1);
                            ~~~~~~~~~~
          src/foo/Foo.java:11: Error: Use Slack's JavaPreconditions instead of Guava's Preconditions checks [GuavaChecksUsed]
              Preconditions.checkArgument(1 == 1);
                            ~~~~~~~~~~~~~
          src/foo/Foo.java:12: Error: Use Slack's JavaPreconditions instead of Guava's Preconditions checks [GuavaChecksUsed]
              Preconditions.checkNotNull("Hello");
                            ~~~~~~~~~~~~
          src/foo/Foo.java:13: Error: Use Slack's JavaPreconditions instead of Guava's Preconditions checks [GuavaChecksUsed]
              Preconditions.checkElementIndex(0, 1);
                            ~~~~~~~~~~~~~~~~~
          5 errors, 0 warnings
        """
          .trimIndent()
      )
      .expectFixDiffs(
        """
          Fix for src/foo/Foo.java line 7: Use Slack's JavaPreconditions checks:
          @@ -7 +7
          -   boolean isTrue = Preconditions.checkState(1 == 1);
          +   boolean isTrue = slack.commons.JavaPreconditions.checkState(1 == 1);
          Fix for src/foo/Foo.java line 10: Use Slack's JavaPreconditions checks:
          @@ -10 +10
          -     Preconditions.checkState(1 == 1);
          +     slack.commons.JavaPreconditions.checkState(1 == 1);
          Fix for src/foo/Foo.java line 11: Use Slack's JavaPreconditions checks:
          @@ -11 +11
          -     Preconditions.checkArgument(1 == 1);
          +     slack.commons.JavaPreconditions.checkArgument(1 == 1);
          Fix for src/foo/Foo.java line 12: Use Slack's JavaPreconditions checks:
          @@ -12 +12
          -     Preconditions.checkNotNull("Hello");
          +     slack.commons.JavaPreconditions.checkNotNull("Hello");
          Fix for src/foo/Foo.java line 13: Use Slack's JavaPreconditions checks:
          @@ -13 +13
          -     Preconditions.checkElementIndex(0, 1);
          +     slack.commons.JavaPreconditions.checkElementIndex(0, 1);
        """
          .trimIndent()
      )
  }

  @Test
  fun `Java - Using Guava Preconditions with fully qualfied references will show warnings`() {
    lint()
      .files(
        guavaPreconditionsStub,
        java(
            """
            package foo;

            public class Foo {

              boolean isTrue = com.google.common.base.Preconditions.checkState(1 == 1);

              void act() {
                com.google.common.base.Preconditions.checkState(1 == 1);
                com.google.common.base.Preconditions.checkArgument(1 == 1);
                com.google.common.base.Preconditions.checkNotNull("Hello");
                com.google.common.base.Preconditions.checkElementIndex(0, 1);
              }
            }
          """
          )
          .indented()
      )
      .issues(*GuavaPreconditionsDetector.issues)
      .run()
      .expect(
        """
          src/foo/Foo.java:5: Error: Use Slack's JavaPreconditions instead of Guava's Preconditions checks [GuavaChecksUsed]
            boolean isTrue = com.google.common.base.Preconditions.checkState(1 == 1);
                                                                  ~~~~~~~~~~
          src/foo/Foo.java:8: Error: Use Slack's JavaPreconditions instead of Guava's Preconditions checks [GuavaChecksUsed]
              com.google.common.base.Preconditions.checkState(1 == 1);
                                                   ~~~~~~~~~~
          src/foo/Foo.java:9: Error: Use Slack's JavaPreconditions instead of Guava's Preconditions checks [GuavaChecksUsed]
              com.google.common.base.Preconditions.checkArgument(1 == 1);
                                                   ~~~~~~~~~~~~~
          src/foo/Foo.java:10: Error: Use Slack's JavaPreconditions instead of Guava's Preconditions checks [GuavaChecksUsed]
              com.google.common.base.Preconditions.checkNotNull("Hello");
                                                   ~~~~~~~~~~~~
          src/foo/Foo.java:11: Error: Use Slack's JavaPreconditions instead of Guava's Preconditions checks [GuavaChecksUsed]
              com.google.common.base.Preconditions.checkElementIndex(0, 1);
                                                   ~~~~~~~~~~~~~~~~~
          5 errors, 0 warnings
        """
          .trimIndent()
      )
      .expectFixDiffs(
        """
          Fix for src/foo/Foo.java line 5: Use Slack's JavaPreconditions checks:
          @@ -5 +5
          -   boolean isTrue = com.google.common.base.Preconditions.checkState(1 == 1);
          +   boolean isTrue = slack.commons.JavaPreconditions.checkState(1 == 1);
          Fix for src/foo/Foo.java line 8: Use Slack's JavaPreconditions checks:
          @@ -8 +8
          -     com.google.common.base.Preconditions.checkState(1 == 1);
          +     slack.commons.JavaPreconditions.checkState(1 == 1);
          Fix for src/foo/Foo.java line 9: Use Slack's JavaPreconditions checks:
          @@ -9 +9
          -     com.google.common.base.Preconditions.checkArgument(1 == 1);
          +     slack.commons.JavaPreconditions.checkArgument(1 == 1);
          Fix for src/foo/Foo.java line 10: Use Slack's JavaPreconditions checks:
          @@ -10 +10
          -     com.google.common.base.Preconditions.checkNotNull("Hello");
          +     slack.commons.JavaPreconditions.checkNotNull("Hello");
          Fix for src/foo/Foo.java line 11: Use Slack's JavaPreconditions checks:
          @@ -11 +11
          -     com.google.common.base.Preconditions.checkElementIndex(0, 1);
          +     slack.commons.JavaPreconditions.checkElementIndex(0, 1);
        """
          .trimIndent()
      )
  }

  @Test
  fun `Java - Using Guava Preconditions with static imports will show warnings`() {
    lint()
      .files(
        guavaPreconditionsStub,
        java(
            """
            package foo;

            import static com.google.common.base.Preconditions.checkState;
            import static com.google.common.base.Preconditions.checkArgument;
            import static com.google.common.base.Preconditions.checkNotNull;
            import static com.google.common.base.Preconditions.checkElementIndex;

            public class Foo {

              private boolean isTrue = checkState(1 == 1);

              void act() {
                checkState(1 == 1);
                checkArgument(1 == 1);
                checkNotNull("Hello");
                checkElementIndex(0, 1);
              }
            }
          """
          )
          .indented()
      )
      .issues(*GuavaPreconditionsDetector.issues)
      .run()
      .expect(
        """
          src/foo/Foo.java:10: Error: Use Slack's JavaPreconditions instead of Guava's Preconditions checks [GuavaChecksUsed]
            private boolean isTrue = checkState(1 == 1);
                                     ~~~~~~~~~~
          src/foo/Foo.java:13: Error: Use Slack's JavaPreconditions instead of Guava's Preconditions checks [GuavaChecksUsed]
              checkState(1 == 1);
              ~~~~~~~~~~
          src/foo/Foo.java:14: Error: Use Slack's JavaPreconditions instead of Guava's Preconditions checks [GuavaChecksUsed]
              checkArgument(1 == 1);
              ~~~~~~~~~~~~~
          src/foo/Foo.java:15: Error: Use Slack's JavaPreconditions instead of Guava's Preconditions checks [GuavaChecksUsed]
              checkNotNull("Hello");
              ~~~~~~~~~~~~
          src/foo/Foo.java:16: Error: Use Slack's JavaPreconditions instead of Guava's Preconditions checks [GuavaChecksUsed]
              checkElementIndex(0, 1);
              ~~~~~~~~~~~~~~~~~
          5 errors, 0 warnings
        """
          .trimIndent()
      )
      .expectFixDiffs(
        """
          Fix for src/foo/Foo.java line 10: Use Slack's JavaPreconditions checks:
          @@ -10 +10
          -   private boolean isTrue = checkState(1 == 1);
          +   private boolean isTrue = slack.commons.JavaPreconditions.checkState(1 == 1);
          Fix for src/foo/Foo.java line 13: Use Slack's JavaPreconditions checks:
          @@ -13 +13
          -     checkState(1 == 1);
          +     slack.commons.JavaPreconditions.checkState(1 == 1);
          Fix for src/foo/Foo.java line 14: Use Slack's JavaPreconditions checks:
          @@ -14 +14
          -     checkArgument(1 == 1);
          +     slack.commons.JavaPreconditions.checkArgument(1 == 1);
          Fix for src/foo/Foo.java line 15: Use Slack's JavaPreconditions checks:
          @@ -15 +15
          -     checkNotNull("Hello");
          +     slack.commons.JavaPreconditions.checkNotNull("Hello");
          Fix for src/foo/Foo.java line 16: Use Slack's JavaPreconditions checks:
          @@ -16 +16
          -     checkElementIndex(0, 1);
          +     slack.commons.JavaPreconditions.checkElementIndex(0, 1);
        """
          .trimIndent()
      )
  }

  @Test
  fun `Java - Using Slack Preconditions with static reference will be clean`() {
    lint()
      .files(
        slackPreconditionsStub,
        java(
            """
            package foo;

            import slack.commons.JavaPreconditions;

            public class Foo {

              boolean isTrue = JavaPreconditions.check(1 == 1);

              void act() {
                JavaPreconditions.check(1 == 1);
                JavaPreconditions.require(1 == 1);
                JavaPreconditions.checkNotNull("Hello");
              }
            }
          """
          )
          .indented()
      )
      .issues(*GuavaPreconditionsDetector.issues)
      .run()
      .expectClean()
  }

  @Test
  fun `Java - Using Slack Preconditions with static imports will be clean`() {
    lint()
      .files(
        slackPreconditionsStub,
        java(
            """
            package foo;

            import static slack.commons.JavaPreconditions.check;
            import static slack.commons.JavaPreconditions.checkNotNull;
            import static slack.commons.JavaPreconditions.require;

            public class Foo {

              private boolean isTrue = check(1 == 1);

              void act() {
                check(1 == 1);
                checkNotNull("Hello");
                require(true);
              }
            }
          """
          )
          .indented()
      )
      .issues(*GuavaPreconditionsDetector.issues)
      .allowCompilationErrors() // Until AGP 7.1.0
      // https://groups.google.com/g/lint-dev/c/BigCO8sMhKU
      .run()
      .expectClean()
  }

  @Test
  fun `Kotlin - Using Guava Preconditions with static reference will show warnings`() {
    lint()
      .files(
        guavaPreconditionsStub,
        kotlin(
            """
            package foo

            import com.google.common.base.Preconditions

            class Foo {

              val isTrue = Preconditions.checkState(false)

              fun act() {
                Preconditions.checkState(true)
                Preconditions.checkArgument(false)
                Preconditions.checkNotNull("Hello")
                Preconditions.checkElementIndex(0, 1)
              }
            }
          """
          )
          .indented()
      )
      .issues(*GuavaPreconditionsDetector.issues)
      .run()
      .expect(
        """
          src/foo/Foo.kt:7: Error: Kotlin precondition checks should use the Kotlin standard library checks [GuavaPreconditionsUsedInKotlin]
            val isTrue = Preconditions.checkState(false)
                                       ~~~~~~~~~~
          src/foo/Foo.kt:10: Error: Kotlin precondition checks should use the Kotlin standard library checks [GuavaPreconditionsUsedInKotlin]
              Preconditions.checkState(true)
                            ~~~~~~~~~~
          src/foo/Foo.kt:11: Error: Kotlin precondition checks should use the Kotlin standard library checks [GuavaPreconditionsUsedInKotlin]
              Preconditions.checkArgument(false)
                            ~~~~~~~~~~~~~
          src/foo/Foo.kt:12: Error: Kotlin precondition checks should use the Kotlin standard library checks [GuavaPreconditionsUsedInKotlin]
              Preconditions.checkNotNull("Hello")
                            ~~~~~~~~~~~~
          src/foo/Foo.kt:13: Error: Kotlin precondition checks should use the Kotlin standard library checks [GuavaPreconditionsUsedInKotlin]
              Preconditions.checkElementIndex(0, 1)
                            ~~~~~~~~~~~~~~~~~
          5 errors, 0 warnings
        """
          .trimIndent()
      )
      .expectFixDiffs(
        """
          Fix for src/foo/Foo.kt line 7: Use Kotlin's standard library checks:
          @@ -7 +7
          -   val isTrue = Preconditions.checkState(false)
          +   val isTrue = check(false)
          Fix for src/foo/Foo.kt line 10: Use Kotlin's standard library checks:
          @@ -10 +10
          -     Preconditions.checkState(true)
          +     check(true)
          Fix for src/foo/Foo.kt line 11: Use Kotlin's standard library checks:
          @@ -11 +11
          -     Preconditions.checkArgument(false)
          +     require(false)
          Fix for src/foo/Foo.kt line 12: Use Kotlin's standard library checks:
          @@ -12 +12
          -     Preconditions.checkNotNull("Hello")
          +     checkNotNull("Hello")
        """
          .trimIndent()
      )
  }

  @Test
  fun `Kotlin - Using Guava Preconditions with fully qualified references will show warnings`() {
    lint()
      .files(
        guavaPreconditionsStub,
        kotlin(
            """
            package foo

            class Foo {

              val isTrue = com.google.common.base.Preconditions.checkState(false)

              fun act() {
                com.google.common.base.Preconditions.checkState(true)
                com.google.common.base.Preconditions.checkArgument(false)
                com.google.common.base.Preconditions.checkNotNull("Hello")
                com.google.common.base.Preconditions.checkElementIndex(0, 1)
              }
            }
          """
          )
          .indented()
      )
      .issues(*GuavaPreconditionsDetector.issues)
      .run()
      .expect(
        """
          src/foo/Foo.kt:5: Error: Kotlin precondition checks should use the Kotlin standard library checks [GuavaPreconditionsUsedInKotlin]
            val isTrue = com.google.common.base.Preconditions.checkState(false)
                                                              ~~~~~~~~~~
          src/foo/Foo.kt:8: Error: Kotlin precondition checks should use the Kotlin standard library checks [GuavaPreconditionsUsedInKotlin]
              com.google.common.base.Preconditions.checkState(true)
                                                   ~~~~~~~~~~
          src/foo/Foo.kt:9: Error: Kotlin precondition checks should use the Kotlin standard library checks [GuavaPreconditionsUsedInKotlin]
              com.google.common.base.Preconditions.checkArgument(false)
                                                   ~~~~~~~~~~~~~
          src/foo/Foo.kt:10: Error: Kotlin precondition checks should use the Kotlin standard library checks [GuavaPreconditionsUsedInKotlin]
              com.google.common.base.Preconditions.checkNotNull("Hello")
                                                   ~~~~~~~~~~~~
          src/foo/Foo.kt:11: Error: Kotlin precondition checks should use the Kotlin standard library checks [GuavaPreconditionsUsedInKotlin]
              com.google.common.base.Preconditions.checkElementIndex(0, 1)
                                                   ~~~~~~~~~~~~~~~~~
          5 errors, 0 warnings
        """
          .trimIndent()
      )
      .expectFixDiffs(
        """
          Fix for src/foo/Foo.kt line 5: Use Kotlin's standard library checks:
          @@ -5 +5
          -   val isTrue = com.google.common.base.Preconditions.checkState(false)
          +   val isTrue = check(false)
          Fix for src/foo/Foo.kt line 8: Use Kotlin's standard library checks:
          @@ -8 +8
          -     com.google.common.base.Preconditions.checkState(true)
          +     check(true)
          Fix for src/foo/Foo.kt line 9: Use Kotlin's standard library checks:
          @@ -9 +9
          -     com.google.common.base.Preconditions.checkArgument(false)
          +     require(false)
          Fix for src/foo/Foo.kt line 10: Use Kotlin's standard library checks:
          @@ -10 +10
          -     com.google.common.base.Preconditions.checkNotNull("Hello")
          +     checkNotNull("Hello")
        """
          .trimIndent()
      )
  }

  @Test
  fun `Kotlin - Using Guava Preconditions with static imports will show warnings`() {
    lint()
      .files(
        guavaPreconditionsStub,
        kotlin(
            """
            package foo

            import com.google.common.base.Preconditions.checkState
            import com.google.common.base.Preconditions.checkArgument
            import com.google.common.base.Preconditions.checkNotNull
            import com.google.common.base.Preconditions.checkElementIndex

            class Foo {

              val isTrue = checkState(false);

              fun act() {
                checkState(true)
                checkArgument(false)
                checkNotNull("Hello")
                checkElementIndex(0, 1)
              }
            }
          """
          )
          .indented()
      )
      .issues(*GuavaPreconditionsDetector.issues)
      .run()
      .expect(
        """
          src/foo/Foo.kt:10: Error: Kotlin precondition checks should use the Kotlin standard library checks [GuavaPreconditionsUsedInKotlin]
            val isTrue = checkState(false);
                         ~~~~~~~~~~
          src/foo/Foo.kt:13: Error: Kotlin precondition checks should use the Kotlin standard library checks [GuavaPreconditionsUsedInKotlin]
              checkState(true)
              ~~~~~~~~~~
          src/foo/Foo.kt:14: Error: Kotlin precondition checks should use the Kotlin standard library checks [GuavaPreconditionsUsedInKotlin]
              checkArgument(false)
              ~~~~~~~~~~~~~
          src/foo/Foo.kt:15: Error: Kotlin precondition checks should use the Kotlin standard library checks [GuavaPreconditionsUsedInKotlin]
              checkNotNull("Hello")
              ~~~~~~~~~~~~
          src/foo/Foo.kt:16: Error: Kotlin precondition checks should use the Kotlin standard library checks [GuavaPreconditionsUsedInKotlin]
              checkElementIndex(0, 1)
              ~~~~~~~~~~~~~~~~~
          5 errors, 0 warnings
        """
          .trimIndent()
      )
      .expectFixDiffs(
        """
          Fix for src/foo/Foo.kt line 10: Use Kotlin's standard library checks:
          @@ -10 +10
          -   val isTrue = checkState(false);
          +   val isTrue = check(false);
          Fix for src/foo/Foo.kt line 13: Use Kotlin's standard library checks:
          @@ -13 +13
          -     checkState(true)
          +     check(true)
          Fix for src/foo/Foo.kt line 14: Use Kotlin's standard library checks:
          @@ -14 +14
          -     checkArgument(false)
          +     require(false)
        """
          .trimIndent()
      )
  }
}
