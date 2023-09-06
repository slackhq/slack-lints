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

  @Test
  fun `binds type mismatches`() {
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

                  @Module
                  interface MyModule {
                    @Binds fun validBind(real: Int): Number
                    @Binds fun validBind(real: Boolean): Comparable<Boolean>
                    @Binds fun invalidBind(real: Long): String
                    @Binds fun invalidBind(real: Long): Comparable<Boolean>
                  }
                """
          )
          .indented()
      )
      .allowCompilationErrors(false)
      .run()
      .expect(
        """
        src/foo/MyModule.kt:10: Error: @Binds function parameters must be type-assignable to their return types. [BindsTypeMismatch]
          @Binds fun invalidBind(real: Long): String
          ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        src/foo/MyModule.kt:11: Error: @Binds function parameters must be type-assignable to their return types. [BindsTypeMismatch]
          @Binds fun invalidBind(real: Long): Comparable<Boolean>
          ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        2 errors, 0 warnings
        """
          .trimIndent()
      )
  }

  @Test
  fun `redundant types`() {
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
                    @MyQualifier @Binds fun validBind(real: Boolean): Boolean
                    @Binds fun validBind(@MyQualifier real: Boolean): Boolean
                    @Binds fun invalidBind(real: Long): Long
                    @Binds fun invalidBind(real: Long): Long
                  }
                """
          )
          .indented()
      )
      .allowCompilationErrors(false)
      .run()
      .expect(
        """
        src/foo/MyQualifier.kt:13: Error: @Binds functions should return a different type (including annotations) than the input type. [RedundantBinds]
          @Binds fun invalidBind(real: Long): Long
          ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        src/foo/MyQualifier.kt:14: Error: @Binds functions should return a different type (including annotations) than the input type. [RedundantBinds]
          @Binds fun invalidBind(real: Long): Long
          ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        2 errors, 0 warnings
        """
          .trimIndent()
      )
  }

  @Test
  fun `binds type invalid return`() {
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
                    @Binds fun invalidBind(@MyQualifier real: Unit)
                    @Binds fun invalidBind(@MyQualifier real: Unit): Unit
                  }
                """
          )
          .indented()
      )
      .allowCompilationErrors(false)
      .run()
      .expect(
        """
        src/foo/MyQualifier.kt:11: Error: @Binds functions must have a return type. Cannot be void or Unit. [BindsReturnType]
          @Binds fun invalidBind(@MyQualifier real: Unit)
          ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        src/foo/MyQualifier.kt:12: Error: @Binds functions must have a return type. Cannot be void or Unit. [BindsReturnType]
          @Binds fun invalidBind(@MyQualifier real: Unit): Unit
          ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        2 errors, 0 warnings
        """
          .trimIndent()
      )
  }

  @Test
  fun `binds param counts`() {
    lint()
      .files(
        javaxInjectStubs,
        daggerStubs,
        kotlin(
            """
                  package foo
                  import dagger.Binds
                  import dagger.Module

                  @Module
                  interface MyModule {
                    @Binds fun validBind(real: Int): Number
                    @Binds fun invalidBind(real: Int, second: Int): Number
                    @Binds fun invalidBind(): Number
                  }
                """
          )
          .indented()
      )
      .allowCompilationErrors(false)
      .run()
      .expect(
        """
        src/foo/MyModule.kt:8: Error: @Binds functions require a single parameter as an input to bind. [BindsWrongParameterCount]
          @Binds fun invalidBind(real: Int, second: Int): Number
                                ~~~~~~~~~~~~~~~~~~~~~~~~
        src/foo/MyModule.kt:9: Error: @Binds functions require a single parameter as an input to bind. [BindsWrongParameterCount]
          @Binds fun invalidBind(): Number
          ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        2 errors, 0 warnings
        """
          .trimIndent()
      )
  }

  @Test
  fun `binds must be abstract`() {
    lint()
      .files(
        javaxInjectStubs,
        daggerStubs,
        kotlin(
            """
                  package foo
                  import dagger.Binds
                  import dagger.Module

                  @Module
                  interface MyModule {
                    @Binds fun validBind(real: Int): Number
                    @Binds fun invalidBind(real: Int): Number { return real }
                    @Binds fun invalidBind(real: Int): Number = real
                  }

                  @Module
                  abstract class MyModule2 {
                    @Binds abstract fun validBind(real: Int): Number
                    @Binds fun invalidBind(real: Int): Number { return real }
                    @Binds fun invalidBind(real: Int): Number = real
                  }
                """
          )
          .indented()
      )
      .allowCompilationErrors(false)
      .run()
      .expect(
        """
        src/foo/MyModule.kt:8: Error: @Binds functions must be abstract and cannot have function bodies. [BindsMustBeAbstract]
          @Binds fun invalidBind(real: Int): Number { return real }
                                                    ~~~~~~~~~~~~~~~
        src/foo/MyModule.kt:9: Error: @Binds functions must be abstract and cannot have function bodies. [BindsMustBeAbstract]
          @Binds fun invalidBind(real: Int): Number = real
          ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        src/foo/MyModule.kt:15: Error: @Binds functions must be abstract and cannot have function bodies. [BindsMustBeAbstract]
          @Binds fun invalidBind(real: Int): Number { return real }
          ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        src/foo/MyModule.kt:16: Error: @Binds functions must be abstract and cannot have function bodies. [BindsMustBeAbstract]
          @Binds fun invalidBind(real: Int): Number = real
          ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        4 errors, 0 warnings
        """
          .trimIndent()
      )
  }

  @Test
  fun `binds must be in a module`() {
    lint()
      .files(
        javaxInjectStubs,
        daggerStubs,
        kotlin(
            """
                  package foo
                  import dagger.Binds

                  interface MyModule {
                    @Binds fun validBind(real: Int): Number
                  }

                  abstract class MyModule2 {
                    @Binds abstract fun validBind(real: Int): Number
                  }
                """
          )
          .indented()
      )
      .allowCompilationErrors(false)
      .run()
      .expect(
        """
        src/foo/MyModule.kt:5: Error: @Binds function must be in @Module-annotated classes. [BindsMustBeInModule]
          @Binds fun validBind(real: Int): Number
          ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        src/foo/MyModule.kt:9: Error: @Binds function must be in @Module-annotated classes. [BindsMustBeInModule]
          @Binds abstract fun validBind(real: Int): Number
          ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        2 errors, 0 warnings
        """
          .trimIndent()
      )
  }
}
