// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint

import org.junit.Test

class AlwaysNullReadOnlyVariableDetectorTest : BaseSlackLintTest() {
  override fun getDetector() = AlwaysNullReadOnlyVariableDetector()

  override fun getIssues() = AlwaysNullReadOnlyVariableDetector.ISSUES.toList()

  @Test
  fun `initializing a read-only variable with null shows warnings`() {
    lint()
      .files(
        kotlin(
            """
            package foo

            class Test {
                val str: String? = null

                fun method() {
                    val strInFunction: String? = null
                }
            }
            """
          )
          .indented()
      )
      .allowDuplicates()
      .run()
      .expect(
        """
        src/foo/Test.kt:4: Warning: Avoid initializing read-only variable with null in Kotlin [AvoidNullInitForReadOnlyVariables]
            val str: String? = null
                               ~~~~
        src/foo/Test.kt:7: Warning: Avoid initializing read-only variable with null in Kotlin [AvoidNullInitForReadOnlyVariables]
                val strInFunction: String? = null
                                             ~~~~
        0 errors, 2 warnings
        """
          .trimIndent()
      )
  }

  @Test
  fun `returning null in custom getter shows warnings`() {
    lint()
      .files(
        kotlin(
            """
            package foo

            class Test {
                val str1: String?
                    get() = null

                val str2: String?
                    get() {
                        return null
                    }

                val str3: String? = null
                    get() = field
            }
            """
          )
          .indented()
      )
      .allowDuplicates()
      .run()
      .expect(
        """
        src/foo/Test.kt:12: Warning: Avoid initializing read-only variable with null in Kotlin [AvoidNullInitForReadOnlyVariables]
            val str3: String? = null
                                ~~~~
        src/foo/Test.kt:5: Warning: Avoid returning null in getter for read-only properties in Kotlin [AvoidReturningNullInGetter]
                get() = null
                        ~~~~
        src/foo/Test.kt:9: Warning: Avoid returning null in getter for read-only properties in Kotlin [AvoidReturningNullInGetter]
                    return null
                           ~~~~
        0 errors, 3 warnings
        """
          .trimIndent()
      )
  }

  @Test
  fun `initializing a read-write variable with null doesn't show warnings`() {
    lint()
      .files(
        kotlin(
            """
            package foo

            class Test {
                var str: String? = null

                fun method() {
                    var str: String = null
                }
            }
            """
          )
          .indented()
      )
      .allowDuplicates()
      .run()
      .expectClean()
  }

  @Test
  fun `initializing a read-only variable with not null doesn't show warnings`() {
    lint()
      .files(
        kotlin(
            """
            package foo

            class Test {
                val str: String? = "str"

                fun method() {
                    val str: String? = "str"
                }
            }
            """
          )
          .indented()
      )
      .allowDuplicates()
      .run()
      .expectClean()
  }

  @Test
  fun `initializing a read-write variable with not null doesn't show warnings`() {
    lint()
      .files(
        kotlin(
            """
            package foo

            class Test {
                var str: String? = "str"

                fun method() {
                    var str: String? = "str"
                }
            }
            """
          )
          .indented()
      )
      .allowDuplicates()
      .run()
      .expectClean()
  }

  @Test
  fun `returning not null in custom getter doesn't show warnings`() {
    lint()
      .files(
        kotlin(
            """
            package foo

            class Test {
                val str1: String?
                    get() = "str"

                val str2: String?
                    get() {
                        return "str"
                    }
            }
            """
          )
          .indented()
      )
      .allowDuplicates()
      .run()
      .expectClean()
  }
}
