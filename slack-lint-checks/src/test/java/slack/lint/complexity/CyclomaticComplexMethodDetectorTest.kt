// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint.complexity

import com.android.tools.lint.checks.infrastructure.TestMode
import org.junit.Test
import slack.lint.BaseSlackLintTest

class CyclomaticComplexMethodDetectorTest : BaseSlackLintTest() {
  override fun getDetector() = CyclomaticComplexMethodDetector()

  override fun getIssues() = listOf(CyclomaticComplexMethodDetector.ISSUE)

  // IF_TO_WHEN rewrites if/else chains into when expressions; because simple when entries are
  // ignored by default (unlike if branches), the two forms intentionally yield different
  // complexity, so the cross-mode consistency check does not apply.
  override val skipTestModes =
    arrayOf(TestMode.WHITESPACE, TestMode.SUPPRESSIBLE, TestMode.IF_TO_WHEN)

  @Test
  fun `clean - simple function`() {
    lint()
      .files(
        kotlin(
            """
          fun example(x: Int): Int {
            if (x > 0) return 1
            return 0
          }
          """
          )
          .indented()
      )
      .run()
      .expectClean()
  }

  @Test
  fun `error - too many decision points`() {
    lint()
      .files(
        kotlin(
            """
          fun example(x: Int) {
            if (x == 1) println("1")
            if (x == 2) println("2")
            if (x == 3) println("3")
            if (x == 4) println("4")
            if (x == 5) println("5")
            if (x == 6) println("6")
            if (x == 7) println("7")
            if (x == 8) println("8")
            if (x == 9) println("9")
            if (x == 10) println("10")
            if (x == 11) println("11")
            if (x == 12) println("12")
            if (x == 13) println("13")
            if (x == 14) println("14")
            if (x == 15) println("15")
          }
          """
          )
          .indented()
      )
      .run()
      .expectContains("Function has a cyclomatic complexity of 16, exceeding the limit of 15")
  }

  @Test
  fun `clean - higher threshold configured`() {
    lint()
      .configureOption("threshold", "20")
      .files(
        kotlin(
            """
          fun example(x: Int) {
            if (x == 1) println("1")
            if (x == 2) println("2")
            if (x == 3) println("3")
            if (x == 4) println("4")
            if (x == 5) println("5")
            if (x == 6) println("6")
            if (x == 7) println("7")
            if (x == 8) println("8")
            if (x == 9) println("9")
            if (x == 10) println("10")
            if (x == 11) println("11")
            if (x == 12) println("12")
            if (x == 13) println("13")
            if (x == 14) println("14")
            if (x == 15) println("15")
          }
          """
          )
          .indented()
      )
      .run()
      .expectClean()
  }

  @Test
  fun `clean - simple when entries ignored by default`() {
    // Two when expressions (so the single-when exemption does not apply) with only
    // single-expression entries. By default these simple entries add no complexity.
    lint()
      .configureOption("threshold", "5")
      .files(
        kotlin(
            """
          fun example(x: Int, y: Int) {
            when (x) {
              1 -> println("a")
              2 -> println("b")
              3 -> println("c")
              else -> println("d")
            }
            when (y) {
              1 -> println("e")
              2 -> println("f")
              3 -> println("g")
              else -> println("h")
            }
          }
          """
          )
          .indented()
      )
      .run()
      .expectClean()
  }

  @Test
  fun `error - simple when entries counted when option disabled`() {
    lint()
      .configureOption("threshold", "5")
      .configureOption("ignore-simple-when-entries", "false")
      .files(
        kotlin(
            """
          fun example(x: Int, y: Int) {
            when (x) {
              1 -> println("a")
              2 -> println("b")
              3 -> println("c")
              else -> println("d")
            }
            when (y) {
              1 -> println("e")
              2 -> println("f")
              3 -> println("g")
              else -> println("h")
            }
          }
          """
          )
          .indented()
      )
      .run()
      .expectContains("exceeding the limit of 5")
  }

  @Test
  fun `error - elvis operators add complexity`() {
    lint()
      .configureOption("threshold", "3")
      .files(
        kotlin(
            """
          fun example(a: Int?, b: Int?, c: Int?, d: Int?): Int {
            val w = a ?: 0
            val x = b ?: 0
            val y = c ?: 0
            val z = d ?: 0
            return w + x + y + z
          }
          """
          )
          .indented()
      )
      .run()
      .expectContains("exceeding the limit of 3")
  }

  @Test
  fun `error - control flow constructs each add complexity`() {
    lint()
      .configureOption("threshold", "5")
      .files(
        kotlin(
            """
          fun example(items: List<Int>, n: Int): Int {
            var sum = 0
            for (i in items) {
              sum += i
            }
            items.forEach { sum += it }
            var i = 0
            while (i < n) {
              i++
            }
            do {
              i--
            } while (i > 0)
            try {
              sum += 1
            } catch (e: Exception) {
              sum += 2
            }
            return sum
          }
          """
          )
          .indented()
      )
      .run()
      .expectContains("Function has a cyclomatic complexity of 6, exceeding the limit of 5")
  }

  @Test
  fun `error - boolean operators each add complexity`() {
    lint()
      .configureOption("threshold", "4")
      .files(
        kotlin(
            """
          fun example(a: Boolean, b: Boolean, c: Boolean, d: Boolean): Boolean {
            return (a && b) || (c && d) || a
          }
          """
          )
          .indented()
      )
      .run()
      .expectContains("Function has a cyclomatic complexity of 5, exceeding the limit of 4")
  }

  @Test
  fun `error - break and continue each add complexity`() {
    lint()
      .configureOption("threshold", "5")
      .files(
        kotlin(
            """
          fun example(items: List<Int>) {
            for (i in items) {
              if (i < 0) continue
              if (i > 100) break
              println(i)
            }
          }
          """
          )
          .indented()
      )
      .run()
      .expectContains("Function has a cyclomatic complexity of 6, exceeding the limit of 5")
  }

  @Test
  fun `error - nesting function calls add complexity`() {
    lint()
      .configureOption("threshold", "3")
      .files(
        kotlin(
            """
          fun example(items: List<Int>, value: Int?) {
            items.forEach { println(it) }
            value?.let { println(it) }
            run { println("done") }
          }
          """
          )
          .indented()
      )
      .run()
      .expectContains("Function has a cyclomatic complexity of 4, exceeding the limit of 3")
  }

  @Test
  fun `error - control flow inside a nesting function lambda still counts`() {
    lint()
      .configureOption("threshold", "2")
      .files(
        kotlin(
            """
          fun example(items: List<Int>) {
            items.forEach {
              if (it > 0) println(it)
            }
          }
          """
          )
          .indented()
      )
      .run()
      .expectContains("Function has a cyclomatic complexity of 3, exceeding the limit of 2")
  }

  @Test
  fun `clean - object literal method is not reported on its own`() {
    // A method declared inside an object literal is dispatched independently by lint but excluded
    // from complexity, so it is never reported standalone even though its own body has enough
    // decision points to exceed the threshold of 1.
    lint()
      .configureOption("threshold", "1")
      .files(
        kotlin(
            """
          fun interface Predicate {
            fun test(x: Int): Boolean
          }

          fun makePredicate(): Predicate {
            return object : Predicate {
              override fun test(x: Int): Boolean {
                if (x > 0) return true
                if (x < -10) return true
                return false
              }
            }
          }
          """
          )
          .indented()
      )
      .run()
      // The override is never reported standalone; only makePredicate is.
      .expectContains("complexity of 3")
      .expectContains("fun makePredicate")
  }

  @Test
  fun `clean - nesting functions ignored when option disabled`() {
    lint()
      .configureOption("threshold", "3")
      .configureOption("ignore-nesting-functions", "true")
      .files(
        kotlin(
            """
          fun example(items: List<Int>, value: Int?) {
            items.forEach { println(it) }
            value?.let { println(it) }
            run { println("done") }
          }
          """
          )
          .indented()
      )
      .run()
      .expectClean()
  }

  @Test
  fun `error - block-bodied when entries still count`() {
    // Block-bodied entries are not "simple" and add complexity even with the default option on.
    lint()
      .configureOption("threshold", "5")
      .files(
        kotlin(
            """
          fun example(x: Int, y: Int) {
            when (x) {
              1 -> { println("a") }
              2 -> { println("b") }
              3 -> { println("c") }
              else -> { println("d") }
            }
            when (y) {
              1 -> { println("e") }
              2 -> { println("f") }
              3 -> { println("g") }
              else -> { println("h") }
            }
          }
          """
          )
          .indented()
      )
      .run()
      .expectContains("exceeding the limit of 5")
  }
}
