// Copyright (C) 2021 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint

import org.junit.Test

class RestrictCallsToDetectorTest : BaseSlackLintTest() {

  private companion object {
    private val restrictCallsTo =
      kotlin(
          """
        package slack.lint.annotations
        import java.lang.annotation.Inherited

        @Inherited
        annotation class RestrictCallsTo(val scope: Int) {
          companion object {
            const val FILE = 0
          }
        }
      """
        )
        .indented()
  }

  override fun getDetector() = RestrictCallsToDetector()

  override fun getIssues() = listOf(RestrictCallsToDetector.ISSUE)

  @Test
  fun smokeTest() {
    lint()
      .files(
        restrictCallsTo,
        kotlin(
            """
          package foo

          import slack.lint.annotations.RestrictCallsTo
          import slack.lint.annotations.RestrictCallsTo.Companion.FILE

          interface MyApi {
            fun example()

            @RestrictCallsTo(FILE)
            fun annotatedExample()
          }

          class SameFile {
            fun doStuffWith(api: MyApi) {
              // This is ok
              api.example()
              api.annotatedExample()
            }
          }

          class MyApiImpl : MyApi {
            override fun example() {
              annotatedExample()
            }

            // Note this is not annotated, ensures we check up the hierarchy
            override fun annotatedExample() {
              println("Hello")
            }
          }
        """
          )
          .indented(),
        kotlin(
            """
          package foo

          class DifferentFile {
            fun doStuffWith(api: MyApi) {
              // This is ok
              api.example()
              // This is not
              api.annotatedExample()
            }
          }

          class MyApiImpl2 : MyApi {
            override fun example() {
              // Not ok
              annotatedExample()
            }

            // Still ok
            override fun annotatedExample() {
              println("Hello")
            }

            fun backdoor() {
              // Backdoors don't work either! This isn't annotated on the impl but we check the
              // original overridden type.
              MyApiImpl().annotatedExample()
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
        src/foo/DifferentFile.kt:8: Error: Methods annotated with @RestrictedCallsTo should only be called from the specified scope. [RestrictCallsTo]
            api.annotatedExample()
            ~~~~~~~~~~~~~~~~~~~~~~
        src/foo/DifferentFile.kt:15: Error: Methods annotated with @RestrictedCallsTo should only be called from the specified scope. [RestrictCallsTo]
            annotatedExample()
            ~~~~~~~~~~~~~~~~~~
        src/foo/DifferentFile.kt:26: Error: Methods annotated with @RestrictedCallsTo should only be called from the specified scope. [RestrictCallsTo]
            MyApiImpl().annotatedExample()
            ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        3 errors, 0 warnings
        """
          .trimIndent()
      )
  }
}
