// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint.exceptions

import com.android.tools.lint.checks.infrastructure.TestMode
import org.junit.Test
import slack.lint.BaseSlackLintTest

class SwallowedExceptionDetectorTest : BaseSlackLintTest() {
  override fun getDetector() = SwallowedExceptionDetector()

  override fun getIssues() = listOf(SwallowedExceptionDetector.ISSUE)

  // Exception types are matched by name, so an alias or extra parens hides them.
  override val skipTestModes =
    arrayOf(
      TestMode.WHITESPACE,
      TestMode.SUPPRESSIBLE,
      TestMode.TYPE_ALIAS,
      TestMode.PARENTHESIZED,
    )

  @Test
  fun `clean - exception is logged`() {
    lint()
      .files(
        kotlin(
            """
          fun example() {
            try {
              doSomething()
            } catch (e: Exception) {
              println(e.message)
            }
          }

          fun doSomething() {}
          """
          )
          .indented()
      )
      .run()
      .expectClean()
  }

  @Test
  fun `clean - exception is passed as the cause`() {
    lint()
      .files(
        kotlin(
            """
          fun example() {
            try {
              doSomething()
            } catch (e: Exception) {
              throw IllegalArgumentException(e)
            }
          }

          fun doSomething() {}
          """
          )
          .indented()
      )
      .run()
      .expectClean()
  }

  @Test
  fun `clean - exception is passed as both message and cause`() {
    lint()
      .files(
        kotlin(
            """
          fun example() {
            try {
              doSomething()
            } catch (e: IllegalStateException) {
              throw IllegalArgumentException(e.message, e)
            }
          }

          fun doSomething() {}
          """
          )
          .indented()
      )
      .run()
      .expectClean()
  }

  @Test
  fun `clean - exception used as a receiver and the result is thrown`() {
    lint()
      .files(
        kotlin(
            """
          fun Exception.transformException(): Exception = this

          fun example() {
            try {
              doSomething()
            } catch (e: Exception) {
              throw e.transformException()
            }
          }

          fun doSomething() {}
          """
          )
          .indented()
      )
      .run()
      .expectClean()
  }

  @Test
  fun `error - exception is unused`() {
    lint()
      .files(
        kotlin(
            """
          fun example() {
            try {
              doSomething()
            } catch (e: Exception) {
              println("failed")
            }
          }

          fun doSomething() {}
          """
          )
          .indented()
      )
      .run()
      .expectContains("The caught exception is swallowed")
  }

  @Test
  fun `error - only the message is passed to a new exception`() {
    lint()
      .files(
        kotlin(
            """
          fun example() {
            try {
              doSomething()
            } catch (e: IllegalStateException) {
              throw IllegalArgumentException(e.message)
            }
          }

          fun doSomething() {}
          """
          )
          .indented()
      )
      .run()
      .expectContains("The caught exception is swallowed")
  }

  @Test
  fun `error - a brand new exception is thrown instead`() {
    lint()
      .files(
        kotlin(
            """
          fun example() {
            try {
              doSomething()
            } catch (e: Exception) {
              throw IllegalArgumentException()
            }
          }

          fun doSomething() {}
          """
          )
          .indented()
      )
      .run()
      .expectContains("The caught exception is swallowed")
  }

  @Test
  fun `error - message is routed through a local variable`() {
    lint()
      .files(
        kotlin(
            """
          fun example() {
            try {
              doSomething()
            } catch (e: IllegalStateException) {
              val message = e.message
              throw IllegalArgumentException(message)
            }
          }

          fun doSomething() {}
          """
          )
          .indented()
      )
      .run()
      .expectContains("The caught exception is swallowed")
  }

  @Test
  fun `error - message is routed through a variable declared in an outer block`() {
    lint()
      .files(
        kotlin(
            """
          fun example(condition: Boolean) {
            try {
              doSomething()
            } catch (e: IllegalStateException) {
              val message = e.message
              if (condition) {
                throw IllegalArgumentException(message)
              }
            }
          }

          fun doSomething() {}
          """
          )
          .indented()
      )
      .run()
      .expectContains("The caught exception is swallowed")
  }

  @Test
  fun `clean - one of several throws passes the cause`() {
    lint()
      .files(
        kotlin(
            """
          fun example(condition: Boolean) {
            try {
              doSomething()
            } catch (e: IllegalStateException) {
              if (condition) {
                throw IllegalArgumentException(e)
              }
              throw IllegalArgumentException(e)
            }
          }

          fun doSomething() {}
          """
          )
          .indented()
      )
      .run()
      .expectClean()
  }

  @Test
  fun `error - each swallowing catch in a chain is reported`() {
    lint()
      .files(
        kotlin(
            """
          fun example() {
            try {
              doSomething()
            } catch (e: IllegalStateException) {
              throw IllegalArgumentException(e.message)
            } catch (f: Exception) {
              throw Exception(IllegalArgumentException(f.toString()))
            }
          }

          fun doSomething() {}
          """
          )
          .indented()
      )
      .run()
      .expectWarningCount(2)
  }

  @Test
  fun `error - nested catch is reported independently`() {
    lint()
      .files(
        kotlin(
            """
          fun example() {
            try {
              doSomething()
            } catch (e: IllegalStateException) {
              try {
                doSomething()
              } catch (nested: Exception) {
                throw IllegalArgumentException()
              }
              throw IllegalArgumentException(e)
            }
          }

          fun doSomething() {}
          """
          )
          .indented()
      )
      .run()
      .expectWarningCount(1)
  }

  @Test
  fun `clean - parameter name matches the allowed name pattern`() {
    lint()
      .files(
        kotlin(
            """
          fun example() {
            try {
              doSomething()
            } catch (ignored: Exception) {
              println("nothing to do")
            }
          }

          fun doSomething() {}
          """
          )
          .indented()
      )
      .run()
      .expectClean()
  }

  @Test
  fun `clean - exception named underscore`() {
    lint()
      .files(
        kotlin(
            """
          fun example() {
            try {
              doSomething()
            } catch (_: Exception) {
              println("failed")
            }
          }

          fun doSomething() {}
          """
          )
          .indented()
      )
      .run()
      .expectClean()
  }

  @Test
  fun `clean - ignored exception type in the catch clause`() {
    lint()
      .files(
        kotlin(
            """
          fun example() {
            try {
              doSomething()
            } catch (e: NumberFormatException) {
              println("default")
            }
          }

          fun doSomething() {}
          """
          )
          .indented()
      )
      .run()
      .expectClean()
  }

  @Test
  fun `clean - ignored exception type thrown from the catch body`() {
    lint()
      .files(
        kotlin(
            """
          fun example() {
            try {
              doSomething()
            } catch (e: Exception) {
              throw NumberFormatException("bad input")
            }
          }

          fun doSomething() {}
          """
          )
          .indented()
      )
      .run()
      .expectClean()
  }

  @Test
  fun `clean - configured ignored exception type`() {
    lint()
      .files(
        kotlin(
            """
          fun example() {
            try {
              doSomething()
            } catch (e: IllegalArgumentException) {
              println("ignored")
            }
          }

          fun doSomething() {}
          """
          )
          .indented()
      )
      .configureOption(
        SwallowedExceptionDetector.IGNORED_EXCEPTION_TYPES,
        "IllegalArgumentException",
      )
      .run()
      .expectClean()
  }

  @Test
  fun `error - type missing from the configured ignore list`() {
    lint()
      .files(
        kotlin(
            """
          fun example() {
            try {
              doSomething()
            } catch (e: Exception) {
              println("swallowed")
            }
          }

          fun doSomething() {}
          """
          )
          .indented()
      )
      .configureOption(
        SwallowedExceptionDetector.IGNORED_EXCEPTION_TYPES,
        "IllegalArgumentException",
      )
      .run()
      .expectContains("The caught exception is swallowed")
  }

  @Test
  fun `clean - configured allowed exception name`() {
    lint()
      .files(
        kotlin(
            """
          fun example() {
            try {
              doSomething()
            } catch (myIgnore: Exception) {
              println("nothing to do")
            }
          }

          fun doSomething() {}
          """
          )
          .indented()
      )
      .configureOption(SwallowedExceptionDetector.ALLOWED_EXCEPTION_NAME, "myIgnore")
      .run()
      .expectClean()
  }

  @Test
  fun `error - catch with an empty body`() {
    lint()
      .files(
        kotlin(
            """
          fun example() {
            try {
              doSomething()
            } catch (e: Exception) {
            }
          }

          fun doSomething() {}
          """
          )
          .indented()
      )
      .run()
      .expectContains("The caught exception is swallowed")
  }
}
