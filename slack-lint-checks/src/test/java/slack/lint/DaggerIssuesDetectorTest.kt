// Copyright (C) 2021 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
@file:Suppress("UnstableApiUsage")

package slack.lint

import com.android.tools.lint.checks.infrastructure.TestMode
import org.junit.Test

class DaggerIssuesDetectorTest : BaseSlackLintTest() {

  private companion object {
    private val javaxInjectStubs =
      kotlin(
          """
        package javax.inject

        annotation class Inject
        annotation class Qualifier
      """
        )
        .indented()

    private val daggerStubs =
      kotlin(
          """
        package dagger

        annotation class Binds
        annotation class Provides
        annotation class Module
      """
        )
        .indented()
  }

  override val skipTestModes: Array<TestMode> = arrayOf(TestMode.WHITESPACE, TestMode.SUPPRESSIBLE)

  override fun getDetector() = DaggerIssuesDetector()

  override fun getIssues() = DaggerIssuesDetector.ISSUES.toList()

  @Test
  fun `binds cannot be extension functions`() {
    lint()
      .files(
        javaxInjectStubs,
        daggerStubs,
        kotlin(
            """
                  package foo
                  import javax.inject.Qualifier
                  import dagger.Binds
                  import dagger.Module

                  @Qualifier
                  annotation class MyQualifier

                  @Module
                  interface MyModule {
                    @Binds fun Int.bind(): Number
                    @Binds fun Long.bind(): Number
                    @Binds fun Double.bind(): Number
                    @Binds fun Float.bind(): Number
                    @Binds fun Short.bind(): Number
                    @Binds fun Byte.bind(): Number
                    @Binds fun Char.bind(): Comparable<Char>
                    @Binds fun String.bind(): Comparable<String>
                    @Binds fun @receiver:MyQualifier Boolean.bind(): Comparable<Boolean>
                  }
                """
          )
          .indented()
      )
      .allowCompilationErrors(false)
      .run()
      .expect(
        """
        src/foo/MyQualifier.kt:11: Error: @Binds functions cannot be extension functions. [BindsReceiverParameter]
          @Binds fun Int.bind(): Number
                     ~~~
        src/foo/MyQualifier.kt:12: Error: @Binds functions cannot be extension functions. [BindsReceiverParameter]
          @Binds fun Long.bind(): Number
                     ~~~~
        src/foo/MyQualifier.kt:13: Error: @Binds functions cannot be extension functions. [BindsReceiverParameter]
          @Binds fun Double.bind(): Number
                     ~~~~~~
        src/foo/MyQualifier.kt:14: Error: @Binds functions cannot be extension functions. [BindsReceiverParameter]
          @Binds fun Float.bind(): Number
                     ~~~~~
        src/foo/MyQualifier.kt:15: Error: @Binds functions cannot be extension functions. [BindsReceiverParameter]
          @Binds fun Short.bind(): Number
                     ~~~~~
        src/foo/MyQualifier.kt:16: Error: @Binds functions cannot be extension functions. [BindsReceiverParameter]
          @Binds fun Byte.bind(): Number
                     ~~~~
        src/foo/MyQualifier.kt:17: Error: @Binds functions cannot be extension functions. [BindsReceiverParameter]
          @Binds fun Char.bind(): Comparable<Char>
                     ~~~~
        src/foo/MyQualifier.kt:18: Error: @Binds functions cannot be extension functions. [BindsReceiverParameter]
          @Binds fun String.bind(): Comparable<String>
                     ~~~~~~
        src/foo/MyQualifier.kt:19: Error: @Binds functions cannot be extension functions. [BindsReceiverParameter]
          @Binds fun @receiver:MyQualifier Boolean.bind(): Comparable<Boolean>
                     ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        9 errors, 0 warnings
        """
          .trimIndent()
      )
  }
}
