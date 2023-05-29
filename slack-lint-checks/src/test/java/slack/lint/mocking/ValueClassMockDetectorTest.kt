// Copyright (C) 2021 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint.mocking

import org.junit.Ignore
import org.junit.Test
import slack.lint.BaseSlackLintTest

@Ignore("https://issuetracker.google.com/issues/283715187")
class ValueClassMockDetectorTest : BaseSlackLintTest() {

  private val testClass =
    kotlin(
        """
      package slack.test

      import kotlin.jvm.JvmInline

      @JvmInline
      value class TestClass(val foo: String)
    """
      )
      .indented()

  override fun getDetector() = ValueClassMockDetector()

  override fun getIssues() = listOf(ValueClassMockDetector.ISSUE)

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
//              val localMock1 = org.mockito.Mockito.mock(TestClass::class.java)
              val localSpy1 = org.mockito.Mockito.spy(1u)
//              val localMock2 = mock<TestClass>()
//              val classRef = TestClass::class.java
//              val localMock3 = org.mockito.Mockito.mock(classRef)

//              val dynamicMock = mock<TestClass> {
//
//              }
//              val assigned: TestClass = mock()
//              val fake = TestClass("this is fine")
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
          test/test/slack/test/TestClass.kt:8: Error: value classes represent inlined types, so mocking them should not be necessary [DoNotMockValueClass]
            @Mock lateinit var fieldMock: TestClass
            ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
          test/test/slack/test/TestClass.kt:9: Error: value classes represent inlined types, so mocking them should not be necessary [DoNotMockValueClass]
            @Spy lateinit var fieldSpy: TestClass
            ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
          test/test/slack/test/TestClass.kt:12: Error: value classes represent inlined types, so mocking them should not be necessary [DoNotMockValueClass]
              val localMock1 = org.mockito.Mockito.mock(TestClass::class.java)
                               ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
          test/test/slack/test/TestClass.kt:13: Error: value classes represent inlined types, so mocking them should not be necessary [DoNotMockValueClass]
              val localSpy1 = org.mockito.Mockito.spy(localMock1)
                              ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
          test/test/slack/test/TestClass.kt:14: Error: value classes represent inlined types, so mocking them should not be necessary [DoNotMockValueClass]
              val localMock2 = mock<TestClass>()
                               ~~~~~~~~~~~~~~~~~
          test/test/slack/test/TestClass.kt:16: Error: value classes represent inlined types, so mocking them should not be necessary [DoNotMockValueClass]
              val localMock3 = org.mockito.Mockito.mock(classRef)
                               ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
          test/test/slack/test/TestClass.kt:18: Error: value classes represent inlined types, so mocking them should not be necessary [DoNotMockValueClass]
              val dynamicMock = mock<TestClass> {
                                ^
          test/test/slack/test/TestClass.kt:21: Error: value classes represent inlined types, so mocking them should not be necessary [DoNotMockValueClass]
              val assigned: TestClass = mock()
              ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
          8 errors, 0 warnings
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
          test/test/slack/test/TestClass.java:9: Error: value classes represent inlined types, so mocking them should not be necessary [DoNotMockValueClass]
            @Mock TestClass fieldMock;
            ~~~~~~~~~~~~~~~~~~~~~~~~~~
          test/test/slack/test/TestClass.java:10: Error: value classes represent inlined types, so mocking them should not be necessary [DoNotMockValueClass]
            @Spy TestClass fieldSpy;
            ~~~~~~~~~~~~~~~~~~~~~~~~
          test/test/slack/test/TestClass.java:13: Error: value classes represent inlined types, so mocking them should not be necessary [DoNotMockValueClass]
              TestClass localMock = mock(TestClass.class);
                                    ~~~~~~~~~~~~~~~~~~~~~
          test/test/slack/test/TestClass.java:14: Error: value classes represent inlined types, so mocking them should not be necessary [DoNotMockValueClass]
              TestClass localSpy = spy(localMock);
                                   ~~~~~~~~~~~~~~
          test/test/slack/test/TestClass.java:16: Error: value classes represent inlined types, so mocking them should not be necessary [DoNotMockValueClass]
              TestClass localMock2 = mock(classRef);
                                     ~~~~~~~~~~~~~~
          5 errors, 0 warnings
        """
          .trimIndent()
      )
  }
}
