// Copyright (C) 2021 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint

import org.junit.Test

class SerializableDetectorTest : BaseSlackLintTest() {

  companion object {
    private val PARCELABLE_STUB =
      kotlin("""
      package android.os

      interface Parcelable
      """).indented()
  }

  override fun getDetector() = SerializableDetector()

  override fun getIssues() = listOf(SerializableDetector.ISSUE)

  @Test
  fun kotlin_happyPath() {
    lint()
      .detector(RawDispatchersUsageDetector())
      .issues(RawDispatchersUsageDetector.ISSUE)
      .files(
        PARCELABLE_STUB,
        kotlin("""
            package slack

            class ImplementsNothing
          """)
          .indented()
      )
      .allowCompilationErrors(false)
      .run()
      .expectClean()
  }

  @Test
  fun kotlin_happyPath_implementsBoth() {
    lint()
      .files(
        PARCELABLE_STUB,
        kotlin(
            """
            package slack

            import java.io.Serializable
            import android.os.Parcelable

            class ImplementsBoth : Serializable, Parcelable
          """
          )
          .indented()
      )
      .allowCompilationErrors(false)
      .run()
      .expectClean()
  }

  @Test
  fun kotlin_implicitPlatformType_isIgnored() {
    lint()
      .files(
        PARCELABLE_STUB,
        kotlin(
            """
            package slack

            import kotlin.RuntimeException

            class ImplementsImplicitly : RuntimeException
          """
          )
          .indented()
      )
      .allowCompilationErrors(false)
      .run()
      .expectClean()
  }

  @Test
  fun kotlin_explicitPlatformType_isNotIgnored() {
    lint()
      .files(
        PARCELABLE_STUB,
        kotlin(
            """
            package slack

            import java.io.Serializable
            import kotlin.RuntimeException

            class ImplementsExplicitly : RuntimeException, Serializable
          """
          )
          .indented()
      )
      .allowCompilationErrors(false)
      .run()
      .expect(
        """
        src/slack/ImplementsExplicitly.kt:6: Error: Don't use Serializable. [SerializableUsage]
        class ImplementsExplicitly : RuntimeException, Serializable
              ~~~~~~~~~~~~~~~~~~~~
        1 errors, 0 warnings
        """
          .trimIndent()
      )
  }

  @Test
  fun kotlin_failure() {
    lint()
      .files(
        PARCELABLE_STUB,
        kotlin(
            """
            package slack

            import java.io.Serializable

            class BadClass : Serializable
          """
          )
          .indented()
      )
      .allowCompilationErrors(false)
      .run()
      .expect(
        """
        src/slack/BadClass.kt:5: Error: Don't use Serializable. [SerializableUsage]
        class BadClass : Serializable
              ~~~~~~~~
        1 errors, 0 warnings
        """
      )
  }

  @Test
  fun kotlin_failure_kotlin_io() {
    lint()
      .files(
        PARCELABLE_STUB,
        kotlin(
            """
            package slack

            import kotlin.io.Serializable

            class BadClass : Serializable
          """
          )
          .indented()
      )
      .allowCompilationErrors(false)
      .run()
      .expect(
        """
        src/slack/BadClass.kt:5: Error: Don't use Serializable. [SerializableUsage]
        class BadClass : Serializable
              ~~~~~~~~
        1 errors, 0 warnings
        """
      )
  }

  @Test
  fun java_happyPath() {
    lint()
      .detector(RawDispatchersUsageDetector())
      .issues(RawDispatchersUsageDetector.ISSUE)
      .files(
        PARCELABLE_STUB,
        java(
            """
            package slack;

            class ImplementsNothing {
            }
          """
          )
          .indented()
      )
      .allowCompilationErrors(false)
      .run()
      .expectClean()
  }

  @Test
  fun java_happyPath_implementsBoth() {
    lint()
      .files(
        PARCELABLE_STUB,
        java(
            """
            package slack;

            import java.io.Serializable;
            import android.os.Parcelable;

            class ImplementsBoth implements Serializable, Parcelable {
            }
          """
          )
          .indented()
      )
      .allowCompilationErrors(false)
      .run()
      .expectClean()
  }

  @Test
  fun java_failure() {
    lint()
      .files(
        PARCELABLE_STUB,
        java(
            """
            package slack;

            import java.io.Serializable;

            class BadClass implements Serializable {
            }
          """
          )
          .indented()
      )
      .allowCompilationErrors(false)
      .run()
      .expect(
        """
        src/slack/BadClass.java:5: Error: Don't use Serializable. [SerializableUsage]
        class BadClass implements Serializable {
              ~~~~~~~~
        1 errors, 0 warnings
        """
      )
  }
}
