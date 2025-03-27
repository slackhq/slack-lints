// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint

import org.junit.Test

class TestParameterSiteTargetDetectorTest : BaseSlackLintTest() {
  override fun getDetector() = TestParameterSiteTargetDetector()

  override fun getIssues() = listOf(TestParameterSiteTargetDetector.ISSUE)

  @Test
  fun testParameterWithParamSiteTarget() {
    lint()
      .files(
        kotlin(
          """
          class MyTest(
              @param:com.google.testing.junit.testparameterinjector.TestParameter val myParam: String
          )
          """
        )
      )
      .run()
      .expectClean()
  }

  @Test
  fun testParameterWithoutParamSiteTarget() {
    lint()
      .files(
        kotlin(
          """
          class MyTest(
              @com.google.testing.junit.testparameterinjector.TestParameter val myParam: String
          )
          """
        )
      )
      .run()
      .expect(
        """
        src/MyTest.kt:3: Error: TestParameter annotation has the wrong site target. [TestParameterSiteTarget]
                      @com.google.testing.junit.testparameterinjector.TestParameter val myParam: String
                      ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        1 error
        """
          .trimIndent()
      )
  }

  @Test
  fun testParameterWithWrongSiteTarget() {
    lint()
      .files(
        kotlin(
          """
          class MyTest(
              @field:com.google.testing.junit.testparameterinjector.TestParameter val myParam: String
          )
          """
        )
      )
      .run()
      .expect(
        """
        src/MyTest.kt:3: Error: TestParameter annotation has the wrong site target. [TestParameterSiteTarget]
                      @field:com.google.testing.junit.testparameterinjector.TestParameter val myParam: String
                      ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        1 error
        """
          .trimIndent()
      )
  }

  @Test
  fun testNonPropertyParameter() {
    lint()
      .files(
        kotlin(
          """
          class MyTest(
              @com.google.testing.junit.testparameterinjector.TestParameter myParam: String
          )
          """
        )
      )
      .run()
      .expectClean()
  }

  @Test
  fun testRegularProperty() {
    lint()
      .files(
        kotlin(
          """
          class MyTest {
              @com.google.testing.junit.testparameterinjector.TestParameter
              val myParam: String = ""
          }
          """
        )
      )
      .run()
      .expectClean()
  }
}
