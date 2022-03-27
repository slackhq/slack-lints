/*
 * Copyright (C) 2021 Slack Technologies, LLC
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
@file:Suppress("UnstableApiUsage")

package slack.lint

import com.android.tools.lint.checks.infrastructure.TestMode
import org.junit.Test

class DaggerKotlinIssuesDetectorTest : BaseSlackLintTest() {

  private companion object {
    private val javaxInjectStubs = kotlin(
      """
        package javax.inject

        annotation class Inject
        annotation class Qualifier
      """
    ).indented()

    private val daggerStubs = kotlin(
      """
        package dagger

        annotation class Binds
        annotation class Provides
        annotation class Module
      """
    ).indented()
  }

  override val skipTestModes: Array<TestMode> = arrayOf(TestMode.WHITESPACE)
  override fun getDetector() = DaggerKotlinIssuesDetector()
  override fun getIssues() = DaggerKotlinIssuesDetector.issues.toList()

  @Test
  fun `binds can be extension functions`() {
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
                    @Binds fun bind(number: Int): Number
                    @Binds fun bind(number: Long): Number
                    @Binds fun bind(number: Double): Number
                    @Binds fun bind(number: Float): Number
                    @Binds fun bind(number: Short): Number
                    @Binds fun bind(number: Byte): Number
                    @Binds fun bind(number: Char): Comparable<Char>
                    @Binds fun bind(number: String): Comparable<String>
                    @Binds fun bind(@MyQualifier number: Boolean): Comparable<Boolean>
                  }
                """
        ).indented()
      )
      .allowCompilationErrors(false)
      .run()
      .expect(
        """
              src/foo/MyQualifier.kt:13: Information: @Binds-annotated functions can be extension functions. [BindsCanBeExtensionFunction]
                @Binds fun bind(number: Int): Number
                ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
              src/foo/MyQualifier.kt:14: Information: @Binds-annotated functions can be extension functions. [BindsCanBeExtensionFunction]
                @Binds fun bind(number: Long): Number
                ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
              src/foo/MyQualifier.kt:15: Information: @Binds-annotated functions can be extension functions. [BindsCanBeExtensionFunction]
                @Binds fun bind(number: Double): Number
                ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
              src/foo/MyQualifier.kt:16: Information: @Binds-annotated functions can be extension functions. [BindsCanBeExtensionFunction]
                @Binds fun bind(number: Float): Number
                ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
              src/foo/MyQualifier.kt:17: Information: @Binds-annotated functions can be extension functions. [BindsCanBeExtensionFunction]
                @Binds fun bind(number: Short): Number
                ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
              src/foo/MyQualifier.kt:18: Information: @Binds-annotated functions can be extension functions. [BindsCanBeExtensionFunction]
                @Binds fun bind(number: Byte): Number
                ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
              src/foo/MyQualifier.kt:19: Information: @Binds-annotated functions can be extension functions. [BindsCanBeExtensionFunction]
                @Binds fun bind(number: Char): Comparable<Char>
                ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
              src/foo/MyQualifier.kt:20: Information: @Binds-annotated functions can be extension functions. [BindsCanBeExtensionFunction]
                @Binds fun bind(number: String): Comparable<String>
                ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
              src/foo/MyQualifier.kt:21: Information: @Binds-annotated functions can be extension functions. [BindsCanBeExtensionFunction]
                @Binds fun bind(@MyQualifier number: Boolean): Comparable<Boolean>
                ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
              0 errors, 0 warnings
        """.trimIndent()
      )
      .expectFixDiffs(
        """
              Fix for src/foo/MyQualifier.kt line 13: Convert to extension function:
              @@ -13 +13
              -   @Binds fun bind(number: Int): Number
              +   @Binds fun Int.bind(): Number
              Fix for src/foo/MyQualifier.kt line 14: Convert to extension function:
              @@ -14 +14
              -   @Binds fun bind(number: Long): Number
              +   @Binds fun Long.bind(): Number
              Fix for src/foo/MyQualifier.kt line 15: Convert to extension function:
              @@ -15 +15
              -   @Binds fun bind(number: Double): Number
              +   @Binds fun Double.bind(): Number
              Fix for src/foo/MyQualifier.kt line 16: Convert to extension function:
              @@ -16 +16
              -   @Binds fun bind(number: Float): Number
              +   @Binds fun Float.bind(): Number
              Fix for src/foo/MyQualifier.kt line 17: Convert to extension function:
              @@ -17 +17
              -   @Binds fun bind(number: Short): Number
              +   @Binds fun Short.bind(): Number
              Fix for src/foo/MyQualifier.kt line 18: Convert to extension function:
              @@ -18 +18
              -   @Binds fun bind(number: Byte): Number
              +   @Binds fun Byte.bind(): Number
              Fix for src/foo/MyQualifier.kt line 19: Convert to extension function:
              @@ -19 +19
              -   @Binds fun bind(number: Char): Comparable<Char>
              +   @Binds fun Char.bind(): Comparable<Char>
              Fix for src/foo/MyQualifier.kt line 20: Convert to extension function:
              @@ -20 +20
              -   @Binds fun bind(number: String): Comparable<String>
              +   @Binds fun String.bind(): Comparable<String>
              Fix for src/foo/MyQualifier.kt line 21: Convert to extension function:
              @@ -21 +21
              -   @Binds fun bind(@MyQualifier number: Boolean): Comparable<Boolean>
              +   @Binds fun @receiver:foo.MyQualifier Boolean.bind(): Comparable<Boolean>
        """.trimIndent()
      )
  }
}
