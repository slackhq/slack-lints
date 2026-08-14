// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint.complexity

import com.android.tools.lint.checks.infrastructure.TestMode
import org.junit.Test
import slack.lint.BaseSlackLintTest

class LongParameterListDetectorTest : BaseSlackLintTest() {
  override fun getDetector() = LongParameterListDetector()

  override fun getIssues() = listOf(LongParameterListDetector.ISSUE)

  override val skipTestModes =
    arrayOf(TestMode.WHITESPACE, TestMode.SUPPRESSIBLE, TestMode.JVM_OVERLOADS)

  @Test
  fun `clean - function with acceptable parameters`() {
    lint()
      .files(
        kotlin(
            """
          fun example(a: Int, b: Int, c: Int, d: Int, e: Int) {}
          """
          )
          .indented()
      )
      .run()
      .expectClean()
  }

  @Test
  fun `error - function at the threshold`() {
    lint()
      .files(
        kotlin(
            """
          fun example(a: Int, b: Int, c: Int, d: Int, e: Int, f: Int) {}
          """
          )
          .indented()
      )
      .run()
      .expectContains("Function has 6 parameters, the threshold is 6")
  }

  @Test
  fun `clean - ignoreAnnotated function`() {
    lint()
      .files(
        kotlin(
            """
          annotation class Inject

          @Inject
          fun example(a: Int, b: Int, c: Int, d: Int, e: Int, f: Int, g: Int, h: Int) {}
          """
          )
          .indented()
      )
      .run()
      .expectClean()
  }

  @Test
  fun `clean - higher threshold configured`() {
    lint()
      .configureOption(LongParameterListDetector.FUNCTION_THRESHOLD, 10)
      .files(
        kotlin(
            """
          fun example(a: Int, b: Int, c: Int, d: Int, e: Int, f: Int, g: Int, h: Int) {}
          """
          )
          .indented()
      )
      .run()
      .expectClean()
  }

  @Test
  fun `clean - extension receiver is not a value parameter`() {
    lint()
      .files(
        kotlin(
            """
          fun String.example(a: Int, b: Int, c: Int, d: Int, e: Int) {}
          """
          )
          .indented()
      )
      .run()
      .expectClean()
  }

  @Test
  fun `clean - suspend continuation is not a value parameter`() {
    lint()
      .files(
        kotlin(
            """
          suspend fun example(a: Int, b: Int, c: Int, d: Int, e: Int) {}
          """
          )
          .indented()
      )
      .run()
      .expectClean()
  }

  @Test
  fun `error - local function is checked`() {
    lint()
      .files(
        kotlin(
            """
          fun outer() {
            fun inner(a: Int, b: Int, c: Int, d: Int, e: Int, f: Int) {}
          }
          """
          )
          .indented()
      )
      .run()
      .expectWarningCount(1)
      .expectContains("Function has 6 parameters, the threshold is 6")
  }

  @Test
  fun `error - function in anonymous object reported once`() {
    lint()
      .files(
        kotlin(
            """
          interface Foo

          fun outer() {
            val obj = object : Foo {
              fun helper(a: Int, b: Int, c: Int, d: Int, e: Int, f: Int) {}
            }
          }
          """
          )
          .indented()
      )
      .run()
      .expectWarningCount(1)
  }

  @Test
  fun `error - constructor uses its own threshold`() {
    lint()
      .files(
        kotlin(
            """
          class Example(a: Int, b: Int, c: Int, d: Int, e: Int, f: Int, g: Int)
          """
          )
          .indented()
      )
      .run()
      .expectContains("Constructor has 7 parameters, the threshold is 7")
  }

  @Test
  fun `clean - constructor below its own threshold`() {
    lint()
      .files(
        kotlin(
            """
          class Example(a: Int, b: Int, c: Int, d: Int, e: Int, f: Int)
          """
          )
          .indented()
      )
      .run()
      .expectClean()
  }

  @Test
  fun `clean - data class constructor is exempt`() {
    lint()
      .files(
        kotlin(
            """
          data class Example(val a: Int, val b: Int, val c: Int, val d: Int, val e: Int, val f: Int, val g: Int)
          """
          )
          .indented()
      )
      .run()
      .expectClean()
  }

  @Test
  fun `clean - override function is exempt`() {
    lint()
      .files(
        kotlin(
            """
          open class Base {
            open fun example(a: Int, b: Int, c: Int, d: Int, e: Int, f: Int, g: Int, h: Int) {}
          }

          class Impl : Base() {
            override fun example(a: Int, b: Int, c: Int, d: Int, e: Int, f: Int, g: Int, h: Int) {}
          }
          """
          )
          .indented()
      )
      .run()
      .expectWarningCount(1)
      .expectContains("Base.kt:2")
  }

  @Test
  fun `error - data class flagged when ignoreDataClasses disabled`() {
    lint()
      .configureOption(LongParameterListDetector.IGNORE_DATA_CLASSES, false)
      .files(
        kotlin(
            """
          data class Example(val a: Int, val b: Int, val c: Int, val d: Int, val e: Int, val f: Int, val g: Int)
          """
          )
          .indented()
      )
      .run()
      .expectContains("Constructor has 7 parameters, the threshold is 7")
  }

  @Test
  fun `clean - ignoreAnnotated constructor`() {
    lint()
      .files(
        kotlin(
            """
          annotation class Inject

          class Example @Inject constructor(a: Int, b: Int, c: Int, d: Int, e: Int, f: Int, g: Int)
          """
          )
          .indented()
      )
      .run()
      .expectClean()
  }

  @Test
  fun `clean - custom ignoreAnnotated configured`() {
    lint()
      .configureOption(LongParameterListDetector.IGNORE_ANNOTATED, "MyCustomAnnotation")
      .files(
        kotlin(
            """
          annotation class MyCustomAnnotation

          @MyCustomAnnotation
          fun example(a: Int, b: Int, c: Int, d: Int, e: Int, f: Int, g: Int, h: Int) {}
          """
          )
          .indented()
      )
      .run()
      .expectClean()
  }
}
