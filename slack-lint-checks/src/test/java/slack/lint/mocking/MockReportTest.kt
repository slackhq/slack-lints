// Copyright (C) 2021 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint.mocking

import com.google.common.truth.Truth.assertThat
import kotlin.io.path.exists
import kotlin.io.path.readText
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import slack.lint.BaseSlackLintTest
import slack.lint.mocking.MockDetector.Companion.MOCK_REPORT

class MockReportTest : BaseSlackLintTest() {

  @Rule @JvmField val tmpFolder = TemporaryFolder()

  private val testClass =
    kotlin(
        """
      package slack.test

      data class TestClass(val foo: String, val list: List<TestClass> = emptyList())
    """
      )
      .indented()

  override fun getDetector() = MockDetector()

  override fun getIssues() = MockDetector.ISSUES.toList()

  @Test
  fun kotlinTests() {
    val source =
      kotlin(
          "test/test/slack/test/TestClass.kt",
          """
          package slack.test

          import org.mockito.Mock
          import org.mockito.Spy
          import slack.test.mockito.mock

          class MyTests {
            @Mock lateinit var fieldMock: TestClass
            @Spy lateinit var fieldSpy: TestClass

            fun example() {
              val localMock1 = org.mockito.Mockito.mock(TestClass::class.java)
              val localSpy1 = org.mockito.Mockito.spy(localMock1)
              val localMock2 = mock<TestClass>()
              val classRef = TestClass::class.java
              val localMock3 = org.mockito.Mockito.mock(classRef)

              val dynamicMock = mock<TestClass> {

              }
              val assigned: TestClass = mock()
              val fake = TestClass("this is fine")

              // Extra tests for location reporting
              val unnecessaryMockedValues = TestClass(
                "This is fine",
                mock()
              )
              val unnecessaryNestedMockedValues = TestClass(
                "This is fine",
                listOf(mock())
              )
              val withNamedArgs = TestClass(
                foo = "This is fine",
                list = listOf(mock())
              )
            }
          }
        """
        )
        .indented()

    val task =
      lint()
        .rootDirectory(tmpFolder.root)
        .files(*mockFileStubs(), testClass, source)
        .configureOption(MOCK_REPORT, true)

    task.run()

    val reports = tmpFolder.root.toPath().resolve("default/app/${MockDetector.MOCK_REPORT_PATH}")
    assertThat(reports.exists()).isTrue()
    assertThat(reports.readText())
      .isEqualTo(
        """
        java.util.List
        slack.test.TestClass
        slack.test.TestClass
        slack.test.TestClass
        slack.test.TestClass
        slack.test.TestClass
        slack.test.TestClass
        slack.test.TestClass
        slack.test.TestClass
        slack.test.TestClass
        slack.test.TestClass
      """
          .trimIndent()
      )
  }

  @Test
  fun javaTests() {
    val source =
      java(
          "test/test/slack/test/TestClass.java",
          """
          package slack.test;

          import org.mockito.Mock;
          import org.mockito.Spy;
          import static org.mockito.Mockito.mock;
          import static org.mockito.Mockito.spy;

          class MyTests {
            @Mock TestClass fieldMock;
            @Spy TestClass fieldSpy;

            public void example() {
              TestClass localMock = mock(TestClass.class);
              TestClass localSpy = spy(localMock);
              Class<TestClass> classRef = TestClass.class;
              TestClass localMock2 = mock(classRef);
              TestClass fake = new TestClass("this is fine");
            }
          }
        """
        )
        .indented()

    val task =
      lint()
        .rootDirectory(tmpFolder.root)
        .files(*mockFileStubs(), testClass, source)
        .configureOption(MOCK_REPORT, true)

    task.run()

    val reports = tmpFolder.root.toPath().resolve("default/app/${MockDetector.MOCK_REPORT_PATH}")
    assertThat(reports.exists()).isTrue()
    assertThat(reports.readText())
      .isEqualTo(
        """
        slack.test.TestClass
        slack.test.TestClass
        slack.test.TestClass
        slack.test.TestClass
        slack.test.TestClass
      """
          .trimIndent()
      )
  }
}
