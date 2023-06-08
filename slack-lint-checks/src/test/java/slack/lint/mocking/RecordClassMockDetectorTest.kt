// Copyright (C) 2021 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint.mocking

import org.junit.Test
import slack.lint.BaseSlackLintTest

class RecordClassMockDetectorTest : BaseSlackLintTest() {

  private val testClass =
    kotlin(
        """
      package slack.test

      import kotlin.jvm.JvmRecord

      @JvmRecord
      data class TestClass(val foo: String)
    """
      )
      .indented()

  private val testJavaClass =
    java(
        "test/slack/test/TestJavaClass.java",
        """
      package slack.test;

      record TestJavaClass(String foo) {

      }
    """
      )
      .indented()

  private val testClasses =
    arrayOf(
      testClass,
      // TODO test once lint supports java record
      //  https://issuetracker.google.com/issues/283693337
      //            testJavaClass
    )

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
            }

              // TODO uncomment once above Java record support is fixed
//            @Mock lateinit var fieldMock2: TestJavaClass
//            @Spy lateinit var fieldSpy2: TestJavaClass
//
//            fun example2() {
//              val localMock1 = org.mockito.Mockito.mock(TestJavaClass::class.java)
//              val localSpy1 = org.mockito.Mockito.spy(localMock1)
//              val localMock2 = mock<TestJavaClass>()
//              val classRef = TestJavaClass::class.java
//              val localMock3 = org.mockito.Mockito.mock(classRef)
//
//              val dynamicMock = mock<TestJavaClass> {
//
//              }
//              val assigned: TestJavaClass = mock()
//              val fake = TestJavaClass("this is fine")
//            }
          }
        """
        )
        .indented()

    lint()
      .files(*mockFileStubs(), *testClasses, source)
      .run()
      .expect(
        """
          test/test/slack/test/TestClass.kt:8: Error: 'slack.test.TestClass' is a record class, so mocking it should not be necessary [DoNotMockRecordClass]
                      @Mock lateinit var fieldMock: TestClass
                      ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
          test/test/slack/test/TestClass.kt:9: Error: 'slack.test.TestClass' is a record class, so mocking it should not be necessary [DoNotMockRecordClass]
                      @Spy lateinit var fieldSpy: TestClass
                      ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
          test/test/slack/test/TestClass.kt:12: Error: 'slack.test.TestClass' is a record class, so mocking it should not be necessary [DoNotMockRecordClass]
                        val localMock1 = org.mockito.Mockito.mock(TestClass::class.java)
                                         ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
          test/test/slack/test/TestClass.kt:13: Error: 'slack.test.TestClass' is a record class, so mocking it should not be necessary [DoNotMockRecordClass]
                        val localSpy1 = org.mockito.Mockito.spy(localMock1)
                                        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
          test/test/slack/test/TestClass.kt:14: Error: 'slack.test.TestClass' is a record class, so mocking it should not be necessary [DoNotMockRecordClass]
                        val localMock2 = mock<TestClass>()
                                         ~~~~~~~~~~~~~~~~~
          test/test/slack/test/TestClass.kt:16: Error: 'slack.test.TestClass' is a record class, so mocking it should not be necessary [DoNotMockRecordClass]
                        val localMock3 = org.mockito.Mockito.mock(classRef)
                                         ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
          test/test/slack/test/TestClass.kt:18: Error: 'slack.test.TestClass' is a record class, so mocking it should not be necessary [DoNotMockRecordClass]
                        val dynamicMock = mock<TestClass> {
                                          ^
          test/test/slack/test/TestClass.kt:21: Error: 'slack.test.TestClass' is a record class, so mocking it should not be necessary [DoNotMockRecordClass]
                        val assigned: TestClass = mock()
                                                  ~~~~~~
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

              // TODO uncomment once above Java record support is fixed
//            @Mock TestJavaClass fieldMock2;
//            @Spy TestJavaClass fieldSpy2;
//
//            public void example2() {
//              TestJavaClass localMock = mock(TestJavaClass.class);
//              TestJavaClass localSpy = spy(localMock);
//              Class<TestJavaClass> classRef = TestJavaClass.class;
//              TestJavaClass localMock2 = mock(classRef);
//              TestJavaClass fake = new TestJavaClass("this is fine");
//            }
          }
        """
        )
        .indented()

    lint()
      .files(*mockFileStubs(), *testClasses, source)
      .run()
      .expect(
        """
          test/test/slack/test/TestClass.java:9: Error: 'slack.test.TestClass' is a record class, so mocking it should not be necessary [DoNotMockRecordClass]
                      @Mock TestClass fieldMock;
                      ~~~~~~~~~~~~~~~~~~~~~~~~~~
          test/test/slack/test/TestClass.java:10: Error: 'slack.test.TestClass' is a record class, so mocking it should not be necessary [DoNotMockRecordClass]
                      @Spy TestClass fieldSpy;
                      ~~~~~~~~~~~~~~~~~~~~~~~~
          test/test/slack/test/TestClass.java:13: Error: 'slack.test.TestClass' is a record class, so mocking it should not be necessary [DoNotMockRecordClass]
                        TestClass localMock = mock(TestClass.class);
                                              ~~~~~~~~~~~~~~~~~~~~~
          test/test/slack/test/TestClass.java:14: Error: 'slack.test.TestClass' is a record class, so mocking it should not be necessary [DoNotMockRecordClass]
                        TestClass localSpy = spy(localMock);
                                             ~~~~~~~~~~~~~~
          test/test/slack/test/TestClass.java:16: Error: 'slack.test.TestClass' is a record class, so mocking it should not be necessary [DoNotMockRecordClass]
                        TestClass localMock2 = mock(classRef);
                                               ~~~~~~~~~~~~~~
          5 errors, 0 warnings
        """
          .trimIndent()
      )
  }
}
