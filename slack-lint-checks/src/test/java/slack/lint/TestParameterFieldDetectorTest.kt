package slack.lint

import org.junit.Test

class TestParameterFieldDetectorTest : BaseSlackLintTest() {
  override fun getDetector() = TestParameterFieldDetector()

  override fun getIssues() = listOf(TestParameterFieldDetector.ISSUE)

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
        src/MyTest.kt:3: Error: TestParameter annotations on parameter properties must have param: site targets. See https://github.com/google/TestParameterInjector/issues/49 [TestParameterSiteTarget]
                      @com.google.testing.junit.testparameterinjector.TestParameter val myParam: String
                      ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        1 error
        """.trimIndent()
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
        src/MyTest.kt:3: Error: TestParameter annotations on parameter properties must have param: site targets. See https://github.com/google/TestParameterInjector/issues/49 [TestParameterSiteTarget]
                      @field:com.google.testing.junit.testparameterinjector.TestParameter val myParam: String
                      ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        1 error
        """.trimIndent()
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
