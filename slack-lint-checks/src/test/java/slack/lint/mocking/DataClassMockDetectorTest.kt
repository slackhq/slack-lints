// Copyright (C) 2021 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint.mocking

import org.junit.Test
import slack.lint.BaseSlackLintTest

class DataClassMockDetectorTest : BaseSlackLintTest() {

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

    lint()
      .files(*mockFileStubs(), testClass, source)
      // https://issuetracker.google.com/issues/283693338
      .allowCompilationErrors()
      .run()
      .expect(
        """
          test/test/slack/test/TestClass.kt:8: Error: data classes represent pure value classes, so mocking them should not be necessary [DoNotMockDataClass]
            @Mock lateinit var fieldMock: TestClass
            ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
          test/test/slack/test/TestClass.kt:9: Error: data classes represent pure value classes, so mocking them should not be necessary [DoNotMockDataClass]
            @Spy lateinit var fieldSpy: TestClass
            ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
          test/test/slack/test/TestClass.kt:12: Error: data classes represent pure value classes, so mocking them should not be necessary [DoNotMockDataClass]
              val localMock1 = org.mockito.Mockito.mock(TestClass::class.java)
                               ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
          test/test/slack/test/TestClass.kt:13: Error: data classes represent pure value classes, so mocking them should not be necessary [DoNotMockDataClass]
              val localSpy1 = org.mockito.Mockito.spy(localMock1)
                              ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
          test/test/slack/test/TestClass.kt:14: Error: data classes represent pure value classes, so mocking them should not be necessary [DoNotMockDataClass]
              val localMock2 = mock<TestClass>()
                               ~~~~~~~~~~~~~~~~~
          test/test/slack/test/TestClass.kt:16: Error: data classes represent pure value classes, so mocking them should not be necessary [DoNotMockDataClass]
              val localMock3 = org.mockito.Mockito.mock(classRef)
                               ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
          test/test/slack/test/TestClass.kt:18: Error: data classes represent pure value classes, so mocking them should not be necessary [DoNotMockDataClass]
              val dynamicMock = mock<TestClass> {
                                ^
          test/test/slack/test/TestClass.kt:21: Error: data classes represent pure value classes, so mocking them should not be necessary [DoNotMockDataClass]
              val assigned: TestClass = mock()
                                        ~~~~~~
          test/test/slack/test/TestClass.kt:31: Error: data classes represent pure value classes, so mocking them should not be necessary [DoNotMockDataClass]
                listOf(mock())
                       ~~~~~~
          test/test/slack/test/TestClass.kt:35: Error: data classes represent pure value classes, so mocking them should not be necessary [DoNotMockDataClass]
                list = listOf(mock())
                              ~~~~~~
          test/test/slack/test/TestClass.kt:27: Warning: platform type 'java.util.List' should not be mocked [DoNotMockPlatformTypes]
                mock()
                ~~~~~~
          10 errors, 1 warnings
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

    lint()
      .files(*mockFileStubs(), testClass, source)
      .run()
      .expect(
        """
          test/test/slack/test/TestClass.java:9: Error: data classes represent pure value classes, so mocking them should not be necessary [DoNotMockDataClass]
            @Mock TestClass fieldMock;
            ~~~~~~~~~~~~~~~~~~~~~~~~~~
          test/test/slack/test/TestClass.java:10: Error: data classes represent pure value classes, so mocking them should not be necessary [DoNotMockDataClass]
            @Spy TestClass fieldSpy;
            ~~~~~~~~~~~~~~~~~~~~~~~~
          test/test/slack/test/TestClass.java:13: Error: data classes represent pure value classes, so mocking them should not be necessary [DoNotMockDataClass]
              TestClass localMock = mock(TestClass.class);
                                    ~~~~~~~~~~~~~~~~~~~~~
          test/test/slack/test/TestClass.java:14: Error: data classes represent pure value classes, so mocking them should not be necessary [DoNotMockDataClass]
              TestClass localSpy = spy(localMock);
                                   ~~~~~~~~~~~~~~
          test/test/slack/test/TestClass.java:16: Error: data classes represent pure value classes, so mocking them should not be necessary [DoNotMockDataClass]
              TestClass localMock2 = mock(classRef);
                                     ~~~~~~~~~~~~~~
          5 errors, 0 warnings
        """
          .trimIndent()
      )
  }
}
