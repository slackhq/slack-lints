// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint.complexity

import com.android.tools.lint.checks.infrastructure.TestMode
import org.junit.Test
import slack.lint.BaseSlackLintTest

class TooManyFunctionsDetectorTest : BaseSlackLintTest() {
  override fun getDetector() = TooManyFunctionsDetector()

  override fun getIssues() = listOf(TooManyFunctionsDetector.ISSUE)

  override val skipTestModes =
    arrayOf(TestMode.WHITESPACE, TestMode.SUPPRESSIBLE, TestMode.JVM_OVERLOADS)

  private fun lintWithThresholds(threshold: Int = 1) =
    lint()
      .configureOption(TooManyFunctionsDetector.THRESHOLD_IN_FILES, threshold)
      .configureOption(TooManyFunctionsDetector.THRESHOLD_IN_CLASSES, threshold)
      .configureOption(TooManyFunctionsDetector.THRESHOLD_IN_INTERFACES, threshold)
      .configureOption(TooManyFunctionsDetector.THRESHOLD_IN_OBJECTS, threshold)
      .configureOption(TooManyFunctionsDetector.THRESHOLD_IN_ENUMS, threshold)

  @Test
  fun `error - function in class`() {
    lintWithThresholds()
      .files(
        kotlin(
            """
          class Example {
            fun a() = Unit
          }
          """
          )
          .indented()
      )
      .run()
      .expectContains("Class Example has 1 functions, the threshold is 1")
  }

  @Test
  fun `error - function in object`() {
    lintWithThresholds()
      .files(
        kotlin(
            """
          object Example {
            fun a() = Unit
          }
          """
          )
          .indented()
      )
      .run()
      .expectContains("Object Example has 1 functions")
  }

  @Test
  fun `error - function in interface`() {
    lintWithThresholds()
      .files(
        kotlin(
            """
          interface Example {
            fun a()
          }
          """
          )
          .indented()
      )
      .run()
      .expectContains("Interface Example has 1 functions")
  }

  @Test
  fun `error - function in enum`() {
    lintWithThresholds()
      .files(
        kotlin(
            """
          enum class Example {
            A;
            fun a() = Unit
          }
          """
          )
          .indented()
      )
      .run()
      .expectContains("Enum class Example has 1 functions")
  }

  @Test
  fun `error - top level function in file`() {
    lintWithThresholds()
      .files(kotlin("fun f() = Unit").indented())
      .run()
      .expectContains("has 1 top-level functions, the threshold is 1")
  }

  @Test
  fun `error - counts only top level functions in file`() {
    lintWithThresholds(3)
      .files(
        kotlin(
            """
          fun f1() = Unit
          class C
          object O
          fun f2() = Unit
          interface I
          enum class E
          fun f3() = Unit
          """
          )
          .indented()
      )
      .run()
      .expectContains("has 3 top-level functions")
  }

  @Test
  fun `error - nested class counted separately`() {
    lintWithThresholds()
      .files(
        kotlin(
            """
          class A {
            class B {
              fun a() = Unit
            }
          }
          """
          )
          .indented()
      )
      .run()
      .expectWarningCount(1)
      .expectContains("Class B has 1 functions")
  }

  @Test
  fun `error - anonymous object has no name to report`() {
    lintWithThresholds()
      .files(
        kotlin(
            """
          interface Foo

          val x = object : Foo {
            fun a() = Unit
          }
          """
          )
          .indented()
      )
      .run()
      .expectContains("Anonymous object has 1 functions, the threshold is 1")
  }

  @Test
  fun `error - companion object counted separately`() {
    lintWithThresholds()
      .files(
        kotlin(
            """
          class Example {
            companion object {
              fun a() = Unit
            }
          }
          """
          )
          .indented()
      )
      .run()
      .expectWarningCount(1)
      .expectContains("Object Companion has 1 functions")
  }

  @Test
  fun `clean - properties are not functions`() {
    lintWithThresholds(3)
      .files(
        kotlin(
            """
          class Example {
            val a: Int = 1
            var b: Int = 2
            fun one() = Unit
            fun two() = Unit
          }
          """
          )
          .indented()
      )
      .run()
      .expectClean()
  }

  @Test
  fun `clean - data class synthetics are not functions`() {
    lintWithThresholds(3)
      .files(
        kotlin(
            """
          data class Example(val a: Int, val b: Int) {
            fun one() = Unit
          }
          """
          )
          .indented()
      )
      .run()
      .expectClean()
  }

  @Test
  fun `clean - ignoreAnnotated class`() {
    lintWithThresholds()
      .files(
        kotlin(
            """
          annotation class Module

          @Module
          class Example {
            fun a() = Unit
          }
          """
          )
          .indented()
      )
      .run()
      .expectClean()
  }

  @Test
  fun `clean - below threshold`() {
    lintWithThresholds(3)
      .files(
        kotlin(
            """
          class Example {
            fun a() = Unit
            fun b() = Unit
          }
          """
          )
          .indented()
      )
      .run()
      .expectClean()
  }
}
