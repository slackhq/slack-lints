// Copyright (C) 2020 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint

import org.junit.Test

class RedactedUsageDetectorTest : BaseSlackLintTest() {

  companion object {
    private val REDACTED_STUB =
      kotlin(
        """
        package slack.annotations

        annotation class Redacted
        annotation class AnotherRedacted
        annotation class AnotherAnnotation
        """
          .trimIndent()
      )
  }

  override fun getDetector() = RedactedUsageDetector()

  override fun getIssues() = RedactedUsageDetector.ISSUES.toList()

  @Test
  fun smokeTest() {
    lint()
      .files(
        REDACTED_STUB,
        kotlin(
            """
            package test.pkg

            import slack.annotations.Redacted
            import slack.annotations.AnotherRedacted
            import slack.annotations.AnotherAnnotation

            @Redacted
            data class RedactedClass(val value: String)

            data class RedactedProps(@Redacted val value: String)
          """
          )
          .indented(),
        java(
            """
            package test.pkg;

            import slack.annotations.Redacted;
            import slack.annotations.AnotherAnnotation;

            @Redacted
            class RedactedClass {
              @Redacted
              int value;
              @AnotherAnnotation
              int value2;

              @Redacted
              public int getValue() {
                return value;
              }

              @AnotherAnnotation
              public int getValue2() {
                return value2;
              }

              @AnotherAnnotation
              static class AnotherInner {

              }
            }
          """
          )
          .indented(),
        java(
            """
            package test.pkg;

            import slack.annotations.Redacted;
            import slack.annotations.AnotherRedacted;

            @AnotherRedacted
            class AnotherRedactedClass {
              @AnotherRedacted
              int value;

              @AnotherRedacted
              public int getValue() {
                return value;
              }
            }
          """
          )
          .indented(),
      )
      .allowCompilationErrors(false)
      .allowClassNameClashes(true)
      .run()
      .expect(
        """
          src/test/pkg/AnotherRedactedClass.java:6: Error: @Redacted is only supported in Kotlin classes! [RedactedInJavaUsage]
          @AnotherRedacted
          ~~~~~~~~~~~~~~~~
          src/test/pkg/AnotherRedactedClass.java:8: Error: @Redacted is only supported in Kotlin classes! [RedactedInJavaUsage]
            @AnotherRedacted
            ~~~~~~~~~~~~~~~~
          src/test/pkg/AnotherRedactedClass.java:11: Error: @Redacted is only supported in Kotlin classes! [RedactedInJavaUsage]
            @AnotherRedacted
            ~~~~~~~~~~~~~~~~
          src/test/pkg/RedactedClass.java:6: Error: @Redacted is only supported in Kotlin classes! [RedactedInJavaUsage]
          @Redacted
          ~~~~~~~~~
          src/test/pkg/RedactedClass.java:8: Error: @Redacted is only supported in Kotlin classes! [RedactedInJavaUsage]
            @Redacted
            ~~~~~~~~~
          src/test/pkg/RedactedClass.java:13: Error: @Redacted is only supported in Kotlin classes! [RedactedInJavaUsage]
            @Redacted
            ~~~~~~~~~
          6 errors, 0 warnings
        """
          .trimIndent()
      )
  }
}
