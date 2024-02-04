// Copyright (C) 2020 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint

import com.android.tools.lint.checks.infrastructure.TestMode
import org.junit.Test

class MainScopeUsageDetectorTest : BaseSlackLintTest() {

  companion object {
    private val COROUTINE_SCOPE_STUB =
      kotlin(
        "test/kotlinx/coroutines/CoroutineScope.kt",
        // language=kotlin
        """
        package kotlinx.coroutines

        fun MainScope() {

        }
      """
          .trimIndent(),
      )
  }

  override val skipTestModes: Array<TestMode> = arrayOf(TestMode.WHITESPACE)

  override fun getDetector() = MainScopeUsageDetector()

  override fun getIssues() = listOf(MainScopeUsageDetector.ISSUE)

  @Test
  fun simple() {
    lint()
      .files(
        COROUTINE_SCOPE_STUB,
        kotlin(
            """
          package test.pkg

          import kotlinx.coroutines.MainScope

          fun example() {
            val scope = MainScope()
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
        src/test/pkg/test.kt:6: Error: Use slack.foundation.coroutines.android.MainScope. [MainScopeUsage]
          val scope = MainScope()
                      ~~~~~~~~~~~
        1 errors, 0 warnings
        """
          .trimIndent()
      )
      .expectFixDiffs(
        """
        Fix for src/test/pkg/test.kt line 6: Use slack.foundation.coroutines.android.MainScope:
        @@ -6 +6
        -   val scope = MainScope()
        +   val scope = slack.foundation.coroutines.android.MainScope()
        """
          .trimIndent()
      )
  }

  // Usage in tests are fine
  @Test
  fun testsAreFine() {
    lint()
      .files(
        COROUTINE_SCOPE_STUB,
        kotlin(
            "test/test/pkg/Test.kt",
            """
            package test.pkg

            import kotlinx.coroutines.MainScope

            fun example() {
              val scope = MainScope()
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
