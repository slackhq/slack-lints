// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint

import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test

class MustUseNamedParamsDetectorTest : BaseSlackLintTest() {
  private companion object {
    private val mustUseNamedParams =
      kotlin(
          """
      package slack.lint.annotations

      /**
       * Callers to this function must named all parameters.
       */
      @Target(AnnotationTarget.FUNCTION)
      @Retention(AnnotationRetention.RUNTIME)
      annotation class MustUseNamedParams
      """
        )
        .indented()
  }

  override fun getDetector(): Detector = MustUseNamedParamsDetector()

  override fun getIssues(): List<Issue> = listOf(MustUseNamedParamsDetector.ISSUE)

  @Test
  fun simpleTest() {
    lint()
      .files(
        mustUseNamedParams,
        kotlin(
            """
          package foo

          import slack.lint.annotations.MustUseNamedParams

          class TestFile {
            @MustUseNamedParams
            fun methodWithAnnotation(name: String) {
              // Do nothing.
            }

            fun methodWithoutAnnotation(name: String) {
              // Do nothing.
            }

            fun useMethod() {
              methodWithAnnotation("Zac")
              methodWithAnnotation(name = "Sean")

              methodWithoutAnnotation("Yifan")
              methodWithoutAnnotation(name = "Sean2")
            }
          }
        """
          )
          .indented(),
      )
      .allowCompilationErrors(false)
      .run()
      .expect(
        """
          src/foo/TestFile.kt:16: Error: Calls to @MustUseNamedParams-annotated methods must name all parameters. [MustUseNamedParams]
              methodWithAnnotation("Zac")
              ~~~~~~~~~~~~~~~~~~~~~~~~~~~
          1 errors, 0 warnings
        """
          .trimIndent()
      )
  }
}
