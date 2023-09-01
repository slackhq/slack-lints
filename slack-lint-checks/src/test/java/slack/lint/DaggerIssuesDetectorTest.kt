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

  override val skipTestModes: Array<TestMode> = arrayOf(TestMode.WHITESPACE)

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
                  import javax.inject.Inject
                  import javax.inject.Qualifier
                  import kotlin.jvm.JvmStatic
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
              
        """
          .trimIndent()
      )
  }
}
