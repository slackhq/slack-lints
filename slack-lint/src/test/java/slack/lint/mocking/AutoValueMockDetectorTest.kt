/*
 * Copyright (C) 2021 Slack Technologies, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package slack.lint.mocking

import org.junit.Test
import slack.lint.BaseSlackLintTest

class AutoValueMockDetectorTest : BaseSlackLintTest() {

  private val autoValueAnnotationClass = kotlin(
    """
        package com.google.auto.value

        annotation class AutoValue {

          annotation class Builder
        }
      """
  )

  private val testClass = java(
    """
      package slack.test;

      import com.google.auto.value.AutoValue;

      @AutoValue
      public abstract class TestClass {
        public static Builder builder() {
          return null;
        }
        @AutoValue.Builder
        public abstract class Builder {
          abstract TestClass build();
        }
      }
    """
  ).indented()

  override fun getDetector() = AutoValueMockDetector()
  override fun getIssues() = listOf(AutoValueMockDetector.ISSUE)

  @Test
  fun kotlinTests() {
    val source = kotlin(
      "test/test/slack/test/TestClass.kt",
      """
          package slack.test

          import org.mockito.Mock
          import org.mockito.Spy
          import slack.test.mockito.mock

          class MyTests {
            @Mock lateinit var fieldMock: TestClass
            @Spy lateinit var fieldSpy: TestClass
            @Mock lateinit var fieldBuilderMock: TestClass.Builder
            @Spy lateinit var fieldBuilderSpy: TestClass.Builder

            fun example() {
              val localMock1 = org.mockito.Mockito.mock(TestClass::class.java)
              val localSpy1 = org.mockito.Mockito.spy(localMock1)
              val localMock2 = mock<TestClass>()
              val classRef = TestClass::class.java
              val localMock3 = org.mockito.Mockito.mock(classRef)

              val builderLocalMock1 = org.mockito.Mockito.mock(TestClass.Builder::class.java)
              val builderLocalSpy1 = org.mockito.Mockito.spy(builderLocalMock1)
              val builderLocalMock2 = mock<TestClass.Builder>()
              val builderClassRef = TestClass.Builder::class.java
              val builderLocalMock3 = org.mockito.Mockito.mock(classRef)
              val fake = TestClass.builder().build()
            }
          }
        """
    ).indented()

    lint()
      .files(*mockFileStubs(), autoValueAnnotationClass, testClass, source)
      .allowCompilationErrors() // Until AGP 7.1.0 https://groups.google.com/g/lint-dev/c/BigCO8sMhKU
      .run()
      .expect(
        """
          test/test/slack/test/TestClass.kt:8: Error: Mocked type is annotated with non-mockable annotation com.google.auto.value.AutoValue. [DoNotMockAutoValue]
            @Mock lateinit var fieldMock: TestClass
            ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
          test/test/slack/test/TestClass.kt:9: Error: Mocked type is annotated with non-mockable annotation com.google.auto.value.AutoValue. [DoNotMockAutoValue]
            @Spy lateinit var fieldSpy: TestClass
            ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
          test/test/slack/test/TestClass.kt:10: Error: Mocked type is annotated with non-mockable annotation com.google.auto.value.AutoValue.Builder. [DoNotMockAutoValue]
            @Mock lateinit var fieldBuilderMock: TestClass.Builder
            ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
          test/test/slack/test/TestClass.kt:11: Error: Mocked type is annotated with non-mockable annotation com.google.auto.value.AutoValue.Builder. [DoNotMockAutoValue]
            @Spy lateinit var fieldBuilderSpy: TestClass.Builder
            ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
          test/test/slack/test/TestClass.kt:14: Error: Mocked type is annotated with non-mockable annotation com.google.auto.value.AutoValue. [DoNotMockAutoValue]
              val localMock1 = org.mockito.Mockito.mock(TestClass::class.java)
                               ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
          test/test/slack/test/TestClass.kt:15: Error: Mocked type is annotated with non-mockable annotation com.google.auto.value.AutoValue. [DoNotMockAutoValue]
              val localSpy1 = org.mockito.Mockito.spy(localMock1)
                              ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
          test/test/slack/test/TestClass.kt:16: Error: Mocked type is annotated with non-mockable annotation com.google.auto.value.AutoValue. [DoNotMockAutoValue]
              val localMock2 = mock<TestClass>()
                               ~~~~~~~~~~~~~~~~~
          test/test/slack/test/TestClass.kt:18: Error: Mocked type is annotated with non-mockable annotation com.google.auto.value.AutoValue. [DoNotMockAutoValue]
              val localMock3 = org.mockito.Mockito.mock(classRef)
                               ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
          test/test/slack/test/TestClass.kt:20: Error: Mocked type is annotated with non-mockable annotation com.google.auto.value.AutoValue.Builder. [DoNotMockAutoValue]
              val builderLocalMock1 = org.mockito.Mockito.mock(TestClass.Builder::class.java)
                                      ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
          test/test/slack/test/TestClass.kt:21: Error: Mocked type is annotated with non-mockable annotation com.google.auto.value.AutoValue.Builder. [DoNotMockAutoValue]
              val builderLocalSpy1 = org.mockito.Mockito.spy(builderLocalMock1)
                                     ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
          test/test/slack/test/TestClass.kt:22: Error: Mocked type is annotated with non-mockable annotation com.google.auto.value.AutoValue.Builder. [DoNotMockAutoValue]
              val builderLocalMock2 = mock<TestClass.Builder>()
                                      ~~~~~~~~~~~~~~~~~~~~~~~~~
          test/test/slack/test/TestClass.kt:24: Error: Mocked type is annotated with non-mockable annotation com.google.auto.value.AutoValue. [DoNotMockAutoValue]
              val builderLocalMock3 = org.mockito.Mockito.mock(classRef)
                                      ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
          12 errors, 0 warnings
        """.trimIndent()
      )
  }

  @Test
  fun javaTests() {
    val source = java(
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
            @Mock TestClass.Builder fieldMock;
            @Spy TestClass.Builder fieldSpy;

            public void example() {
              TestClass localMock = mock(TestClass.class);
              TestClass localSpy = spy(localMock);
              Class<TestClass> classRef = TestClass.class;
              TestClass localMock2 = mock(classRef);

              TestClass.Builder builderLocalMock = mock(TestClass.Builder.class);
              TestClass.Builder builderLocalSpy = spy(builderLocalMock);
              Class<TestClass.Builder> builderClassRef = TestClass.Builder.class;
              TestClass.Builder builderLocalMock2 = mock(builderClassRef);
              TestClass fake = TestClass.builder().build();
            }
          }
        """
    ).indented()

    lint()
      .files(*mockFileStubs(), autoValueAnnotationClass, testClass, source)
      .run()
      .expect(
        """
          test/test/slack/test/TestClass.java:9: Error: Mocked type is annotated with non-mockable annotation com.google.auto.value.AutoValue. [DoNotMockAutoValue]
            @Mock TestClass fieldMock;
            ~~~~~~~~~~~~~~~~~~~~~~~~~~
          test/test/slack/test/TestClass.java:10: Error: Mocked type is annotated with non-mockable annotation com.google.auto.value.AutoValue. [DoNotMockAutoValue]
            @Spy TestClass fieldSpy;
            ~~~~~~~~~~~~~~~~~~~~~~~~
          test/test/slack/test/TestClass.java:11: Error: Mocked type is annotated with non-mockable annotation com.google.auto.value.AutoValue.Builder. [DoNotMockAutoValue]
            @Mock TestClass.Builder fieldMock;
            ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
          test/test/slack/test/TestClass.java:12: Error: Mocked type is annotated with non-mockable annotation com.google.auto.value.AutoValue.Builder. [DoNotMockAutoValue]
            @Spy TestClass.Builder fieldSpy;
            ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
          test/test/slack/test/TestClass.java:15: Error: Mocked type is annotated with non-mockable annotation com.google.auto.value.AutoValue. [DoNotMockAutoValue]
              TestClass localMock = mock(TestClass.class);
                                    ~~~~~~~~~~~~~~~~~~~~~
          test/test/slack/test/TestClass.java:16: Error: Mocked type is annotated with non-mockable annotation com.google.auto.value.AutoValue. [DoNotMockAutoValue]
              TestClass localSpy = spy(localMock);
                                   ~~~~~~~~~~~~~~
          test/test/slack/test/TestClass.java:18: Error: Mocked type is annotated with non-mockable annotation com.google.auto.value.AutoValue. [DoNotMockAutoValue]
              TestClass localMock2 = mock(classRef);
                                     ~~~~~~~~~~~~~~
          test/test/slack/test/TestClass.java:20: Error: Mocked type is annotated with non-mockable annotation com.google.auto.value.AutoValue.Builder. [DoNotMockAutoValue]
              TestClass.Builder builderLocalMock = mock(TestClass.Builder.class);
                                                   ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
          test/test/slack/test/TestClass.java:21: Error: Mocked type is annotated with non-mockable annotation com.google.auto.value.AutoValue.Builder. [DoNotMockAutoValue]
              TestClass.Builder builderLocalSpy = spy(builderLocalMock);
                                                  ~~~~~~~~~~~~~~~~~~~~~
          test/test/slack/test/TestClass.java:23: Error: Mocked type is annotated with non-mockable annotation com.google.auto.value.AutoValue.Builder. [DoNotMockAutoValue]
              TestClass.Builder builderLocalMock2 = mock(builderClassRef);
                                                    ~~~~~~~~~~~~~~~~~~~~~
          10 errors, 0 warnings
        """.trimIndent()
      )
  }
}
