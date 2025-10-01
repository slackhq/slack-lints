// Copyright (C) 2020 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint

import org.junit.Test

class RawDispatchersUsageDetectorTest : BaseSlackLintTest() {

  companion object {
    // Stub of dispatchers.
    // Use a combination of string constants and getters to ensure coverage for both
    // Also add an extension function to check we don't try to lint those.
    // language=kotlin
    private val DISPATCHERS_STUB =
      kotlin(
        """
        package kotlinx.coroutines

        object Dispatchers {
            @JvmStatic
            val Default: String get() = error()

            @JvmStatic
            val Main: String get() = error()

            @JvmStatic
            val Unconfined: String = ""

            @JvmStatic
            val IO: String = ""
        }

        fun Dispatchers.someExtension() {

        }
        """
          .trimIndent()
      )
  }

  override fun getDetector() = RawDispatchersUsageDetector()

  override fun getIssues() = listOf(RawDispatchersUsageDetector.ISSUE)

  @Test
  fun simple() {
    lint()
      .files(
        DISPATCHERS_STUB,
        kotlin(
            """
              package test.pkg

              import kotlinx.coroutines.Dispatchers

              fun example() {
                Dispatchers.IO
                Dispatchers.Default
                Dispatchers.Unconfined
                Dispatchers.Main
                Dispatchers.someExtension()
                Dispatchers::IO
                Dispatchers::Default
                Dispatchers::Unconfined
                Dispatchers::Main
              }
            """
              .trimIndent()
          )
          .indented(),
      )
      .allowCompilationErrors(false)
      .run()
      .expect(
        """
          src/test/pkg/test.kt:6: Error: Use SlackDispatchers. [RawDispatchersUse]
            Dispatchers.IO
            ~~~~~~~~~~~~~~
          src/test/pkg/test.kt:7: Error: Use SlackDispatchers. [RawDispatchersUse]
            Dispatchers.Default
            ~~~~~~~~~~~~~~~~~~~
          src/test/pkg/test.kt:8: Error: Use SlackDispatchers. [RawDispatchersUse]
            Dispatchers.Unconfined
            ~~~~~~~~~~~~~~~~~~~~~~
          src/test/pkg/test.kt:9: Error: Use SlackDispatchers. [RawDispatchersUse]
            Dispatchers.Main
            ~~~~~~~~~~~~~~~~
          src/test/pkg/test.kt:11: Error: Use SlackDispatchers. [RawDispatchersUse]
            Dispatchers::IO
            ~~~~~~~~~~~~~~~
          src/test/pkg/test.kt:12: Error: Use SlackDispatchers. [RawDispatchersUse]
            Dispatchers::Default
            ~~~~~~~~~~~~~~~~~~~~
          src/test/pkg/test.kt:13: Error: Use SlackDispatchers. [RawDispatchersUse]
            Dispatchers::Unconfined
            ~~~~~~~~~~~~~~~~~~~~~~~
          src/test/pkg/test.kt:14: Error: Use SlackDispatchers. [RawDispatchersUse]
            Dispatchers::Main
            ~~~~~~~~~~~~~~~~~
          8 errors, 0 warnings
        """
          .trimIndent()
      )
  }

  // Usage in tests are fine
  @Test
  fun testsAreFine() {
    lint()
      .files(
        DISPATCHERS_STUB,
        kotlin(
            "test/test/pkg/Test.kt",
            """
              package test.pkg

              import kotlinx.coroutines.Dispatchers

              fun example() {
                Dispatchers.IO
                Dispatchers.Default
                Dispatchers.Unconfined
                Dispatchers.Main
                Dispatchers.someExtension()
                Dispatchers::IO
                Dispatchers::Default
                Dispatchers::Unconfined
                Dispatchers::Main
              }
            """
              .trimIndent(),
          )
          .indented(),
      )
      .allowCompilationErrors(false)
      .run()
      .expectClean()
  }
}
