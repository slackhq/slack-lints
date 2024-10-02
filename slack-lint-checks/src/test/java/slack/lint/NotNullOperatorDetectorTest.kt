// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint

import kotlin.text.trimIndent
import org.junit.Test

class NotNullOperatorDetectorTest : BaseSlackLintTest() {
  override fun getDetector() = NotNullOperatorDetector()

  override fun getIssues() = listOf(NotNullOperatorDetector.ISSUE)

  @Test
  fun testDocumentationExample() {
    lint()
      .files(
        kotlin(
          """
            package foo

            class Test {
              fun doNothing(t: String?): Boolean {
                t/* this is legal */!!.length
                return t!!.length == 1
              }
            }
          """
        )
      )
      .run()
      .expect(
        """
          src/foo/Test.kt:6: Warning: Avoid using the !! operator [AvoidUsingNotNullOperator]
                          t/* this is legal */!!.length
                                              ~~
          src/foo/Test.kt:7: Warning: Avoid using the !! operator [AvoidUsingNotNullOperator]
                          return t!!.length == 1
                                  ~~
          0 errors, 2 warnings
        """
          .trimIndent()
      )
  }

  @Test
  fun `test clean`() {
    lint()
      .files(
        kotlin(
          """
            package foo

            class Test {
              fun doNothing(t: String?): Boolean {
                // Should not match despite the !! string in it
                "!!".length++
                return t?.length == 1
              }
            }
          """
        )
      )
      .run()
      .expectClean()
  }
}
