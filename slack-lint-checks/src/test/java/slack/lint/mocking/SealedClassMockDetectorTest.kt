// Copyright (C) 2021 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint.mocking

import com.android.tools.lint.checks.infrastructure.TestMode
import com.intellij.pom.java.LanguageLevel
import org.junit.Test
import slack.lint.BaseSlackLintTest

class SealedClassMockDetectorTest : BaseSlackLintTest() {

  private val testClass =
    kotlin(
        """
      package slack.test

      sealed class TestClass
    """
      )
      .indented()

  private val javaTestClass =
    java(
        """
      package slack.test;

      sealed interface TestJavaClass permits TestJavaClass.Subtype1 {
        interface Subtype1 {

        }
      }
    """
      )
      .indented()

  private val testClasses = arrayOf(testClass, javaTestClass)

  override fun getDetector() = MockDetector()

  override fun getIssues() = MockDetector.ISSUES.toList()

  override val skipTestModes: Array<TestMode>
    // I don't understand the point of this mode
    get() = arrayOf(TestMode.SUPPRESSIBLE)

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

            @Mock lateinit var fieldMock2: TestJavaClass
            @Spy lateinit var fieldSpy2: TestJavaClass
            fun example2() {
              val localMock1 = org.mockito.Mockito.mock(TestJavaClass::class.java)
              val localSpy1 = org.mockito.Mockito.spy(localMock1)
              val localMock2 = mock<TestJavaClass>()
              val classRef = TestJavaClass::class.java
              val localMock3 = org.mockito.Mockito.mock(classRef)

              val dynamicMock = mock<TestJavaClass> {

              }
              val assigned: TestJavaClass = mock()
              val fake = TestJavaClass("this is fine")
            }
          }
        """,
        )
        .indented()

    lint()
      .javaLanguageLevel(LanguageLevel.JDK_17)
      .files(*mockFileStubs(), *testClasses, source)
      .run()
      .expect(
        """
          test/test/slack/test/TestClass.kt:8: Error: 'slack.test.TestClass' is a sealed type and has a restricted type hierarchy, use a subtype instead. [DoNotMockSealedClass]
            @Mock lateinit var fieldMock: TestClass
            ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
          test/test/slack/test/TestClass.kt:9: Error: 'slack.test.TestClass' is a sealed type and has a restricted type hierarchy, use a subtype instead. [DoNotMockSealedClass]
            @Spy lateinit var fieldSpy: TestClass
            ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
          test/test/slack/test/TestClass.kt:12: Error: 'slack.test.TestClass' is a sealed type and has a restricted type hierarchy, use a subtype instead. [DoNotMockSealedClass]
              val localMock1 = org.mockito.Mockito.mock(TestClass::class.java)
                               ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
          test/test/slack/test/TestClass.kt:13: Error: 'slack.test.TestClass' is a sealed type and has a restricted type hierarchy, use a subtype instead. [DoNotMockSealedClass]
              val localSpy1 = org.mockito.Mockito.spy(localMock1)
                              ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
          test/test/slack/test/TestClass.kt:14: Error: 'slack.test.TestClass' is a sealed type and has a restricted type hierarchy, use a subtype instead. [DoNotMockSealedClass]
              val localMock2 = mock<TestClass>()
                               ~~~~~~~~~~~~~~~~~
          test/test/slack/test/TestClass.kt:16: Error: 'slack.test.TestClass' is a sealed type and has a restricted type hierarchy, use a subtype instead. [DoNotMockSealedClass]
              val localMock3 = org.mockito.Mockito.mock(classRef)
                               ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
          test/test/slack/test/TestClass.kt:18: Error: 'slack.test.TestClass' is a sealed type and has a restricted type hierarchy, use a subtype instead. [DoNotMockSealedClass]
              val dynamicMock = mock<TestClass> {
                                ^
          test/test/slack/test/TestClass.kt:21: Error: 'slack.test.TestClass' is a sealed type and has a restricted type hierarchy, use a subtype instead. [DoNotMockSealedClass]
              val assigned: TestClass = mock()
                                        ~~~~~~
          test/test/slack/test/TestClass.kt:25: Error: 'slack.test.TestJavaClass' is a sealed type and has a restricted type hierarchy, use a subtype instead. [DoNotMockSealedClass]
            @Mock lateinit var fieldMock2: TestJavaClass
            ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
          test/test/slack/test/TestClass.kt:26: Error: 'slack.test.TestJavaClass' is a sealed type and has a restricted type hierarchy, use a subtype instead. [DoNotMockSealedClass]
            @Spy lateinit var fieldSpy2: TestJavaClass
            ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
          test/test/slack/test/TestClass.kt:28: Error: 'slack.test.TestJavaClass' is a sealed type and has a restricted type hierarchy, use a subtype instead. [DoNotMockSealedClass]
              val localMock1 = org.mockito.Mockito.mock(TestJavaClass::class.java)
                               ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
          test/test/slack/test/TestClass.kt:29: Error: 'slack.test.TestJavaClass' is a sealed type and has a restricted type hierarchy, use a subtype instead. [DoNotMockSealedClass]
              val localSpy1 = org.mockito.Mockito.spy(localMock1)
                              ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
          test/test/slack/test/TestClass.kt:30: Error: 'slack.test.TestJavaClass' is a sealed type and has a restricted type hierarchy, use a subtype instead. [DoNotMockSealedClass]
              val localMock2 = mock<TestJavaClass>()
                               ~~~~~~~~~~~~~~~~~~~~~
          test/test/slack/test/TestClass.kt:32: Error: 'slack.test.TestJavaClass' is a sealed type and has a restricted type hierarchy, use a subtype instead. [DoNotMockSealedClass]
              val localMock3 = org.mockito.Mockito.mock(classRef)
                               ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
          test/test/slack/test/TestClass.kt:34: Error: 'slack.test.TestJavaClass' is a sealed type and has a restricted type hierarchy, use a subtype instead. [DoNotMockSealedClass]
              val dynamicMock = mock<TestJavaClass> {
                                ^
          test/test/slack/test/TestClass.kt:37: Error: 'slack.test.TestJavaClass' is a sealed type and has a restricted type hierarchy, use a subtype instead. [DoNotMockSealedClass]
              val assigned: TestJavaClass = mock()
                                            ~~~~~~
          16 errors, 0 warnings
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

            @Mock TestJavaClass fieldMock2;
            @Spy TestJavaClass fieldSpy2;
            public void example2() {
              TestClass localMock = mock(TestJavaClass.class);
              TestJavaClass localSpy = spy(localMock);
              Class<TestJavaClass> classRef = TestJavaClass.class;
              TestJavaClass localMock2 = mock(classRef);
              TestJavaClass fake = new TestJavaClass("this is fine");
            }
          }
        """,
        )
        .indented()

    lint()
      .javaLanguageLevel(LanguageLevel.JDK_17)
      .files(*mockFileStubs(), *testClasses, source)
      .run()
      .expect(
        """
          test/test/slack/test/TestClass.java:9: Error: 'slack.test.TestClass' is a sealed type and has a restricted type hierarchy, use a subtype instead. [DoNotMockSealedClass]
            @Mock TestClass fieldMock;
            ~~~~~~~~~~~~~~~~~~~~~~~~~~
          test/test/slack/test/TestClass.java:10: Error: 'slack.test.TestClass' is a sealed type and has a restricted type hierarchy, use a subtype instead. [DoNotMockSealedClass]
            @Spy TestClass fieldSpy;
            ~~~~~~~~~~~~~~~~~~~~~~~~
          test/test/slack/test/TestClass.java:13: Error: 'slack.test.TestClass' is a sealed type and has a restricted type hierarchy, use a subtype instead. [DoNotMockSealedClass]
              TestClass localMock = mock(TestClass.class);
                                    ~~~~~~~~~~~~~~~~~~~~~
          test/test/slack/test/TestClass.java:14: Error: 'slack.test.TestClass' is a sealed type and has a restricted type hierarchy, use a subtype instead. [DoNotMockSealedClass]
              TestClass localSpy = spy(localMock);
                                   ~~~~~~~~~~~~~~
          test/test/slack/test/TestClass.java:16: Error: 'slack.test.TestClass' is a sealed type and has a restricted type hierarchy, use a subtype instead. [DoNotMockSealedClass]
              TestClass localMock2 = mock(classRef);
                                     ~~~~~~~~~~~~~~
          test/test/slack/test/TestClass.java:20: Error: 'slack.test.TestJavaClass' is a sealed type and has a restricted type hierarchy, use a subtype instead. [DoNotMockSealedClass]
            @Mock TestJavaClass fieldMock2;
            ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
          test/test/slack/test/TestClass.java:21: Error: 'slack.test.TestJavaClass' is a sealed type and has a restricted type hierarchy, use a subtype instead. [DoNotMockSealedClass]
            @Spy TestJavaClass fieldSpy2;
            ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
          test/test/slack/test/TestClass.java:23: Error: 'slack.test.TestJavaClass' is a sealed type and has a restricted type hierarchy, use a subtype instead. [DoNotMockSealedClass]
              TestClass localMock = mock(TestJavaClass.class);
                                    ~~~~~~~~~~~~~~~~~~~~~~~~~
          test/test/slack/test/TestClass.java:24: Error: 'slack.test.TestClass' is a sealed type and has a restricted type hierarchy, use a subtype instead. [DoNotMockSealedClass]
              TestJavaClass localSpy = spy(localMock);
                                       ~~~~~~~~~~~~~~
          test/test/slack/test/TestClass.java:26: Error: 'slack.test.TestJavaClass' is a sealed type and has a restricted type hierarchy, use a subtype instead. [DoNotMockSealedClass]
              TestJavaClass localMock2 = mock(classRef);
                                         ~~~~~~~~~~~~~~
          10 errors, 0 warnings
        """
          .trimIndent()
      )
  }
}
