/*
 * Copyright (C) 2020 Slack Technologies, LLC
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
package slack.lint

import org.junit.Test

class RedactedUsageDetectorTest : BaseSlackLintTest() {

  companion object {
    private val REDACTED_STUB = kotlin(
      """
      package slack.annotations

      annotation class Redacted
      annotation class AnotherRedacted
      annotation class AnotherAnnotation
      """.trimIndent()
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
        ).indented(),
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
        ).indented(),
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
        ).indented()
      )
      .allowCompilationErrors(false)
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
        """.trimIndent()
      )
  }
}
