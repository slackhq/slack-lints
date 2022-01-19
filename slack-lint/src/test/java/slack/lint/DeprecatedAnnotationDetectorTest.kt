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
package slack.lint

import com.android.tools.lint.detector.api.Detector
import org.junit.Test

class DeprecatedAnnotationDetectorTest : BaseSlackLintTest() {
  override fun getDetector(): Detector = DeprecatedAnnotationDetector()

  override fun getIssues() = listOf(DeprecatedAnnotationDetector.ISSUE_DEPRECATED_CALL)

  @Test
  fun `non-deprecated class has no warnings`() {
    lint()
      .files(
        NON_DEPRECATED_CLASS,
        java(
          """
                  package slack.test;

                  import slack.test.ThisIsNotDeprecated;

                  public class TestClass {

                    public void doStuff() {
                      new ThisIsNotDeprecated();
                    }
                  }
                """
        ).indented()
      )
      .issues(DeprecatedAnnotationDetector.ISSUE_DEPRECATED_CALL)
      .run()
      .expectClean()
  }

  @Test
  fun `deprecated class has a warning`() {
    lint()
      .files(
        DEPRECATED_CLASS,
        java(
          """
                  package slack.test;

                  import slack.test.ThisIsDeprecated;

                  public class TestClass {

                    public void doStuff() {
                      new ThisIsDeprecated();
                    }
                  }
                """
        ).indented()
      )
      .issues(DeprecatedAnnotationDetector.ISSUE_DEPRECATED_CALL)
      .run()
      .expect(
        """
          src/slack/test/TestClass.java:8: Warning: This class or method is deprecated; consider using an alternative. [DeprecatedCall]
              new ThisIsDeprecated();
              ~~~~~~~~~~~~~~~~~~~~~~
          0 errors, 1 warnings
        """.trimIndent()
      )
  }

  @Test
  fun `non-deprecated method use no warnings`() {
    lint()
      .files(
        DEPRECATED_METHOD,
        java(
          """
                  package slack.test;

                  import slack.test.ThisIsNotDeprecated;

                  public class TestClass {

                    public void doStuff() {
                      new ThisIsNotDeprecated().thisIsNotDeprecated();
                    }
                  }
                """
        ).indented()
      )
      .issues(DeprecatedAnnotationDetector.ISSUE_DEPRECATED_CALL)
      .run()
      .expectClean()
  }

  @Test
  fun `deprecated method use has a warning`() {
    lint()
      .files(
        DEPRECATED_METHOD,
        java(
          """
                  package slack.test;

                  import slack.test.ThisIsNotDeprecated;

                  public class TestClass {

                    public void doStuff() {
                      new ThisIsNotDeprecated().thisIsDeprecated();
                    }
                  }
                """
        ).indented()
      )
      .issues(DeprecatedAnnotationDetector.ISSUE_DEPRECATED_CALL)
      .run()
      .expect(
        """
          src/slack/test/TestClass.java:8: Warning: slack.test.ThisIsNotDeprecated.thisIsDeprecated is deprecated; consider using an alternative. [DeprecatedCall]
              new ThisIsNotDeprecated().thisIsDeprecated();
              ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
          0 errors, 1 warnings
        """.trimIndent()
      )
  }

  @Test
  fun `deprecated method use has a warning in kotlin`() {
    lint()
      .files(
        DEPRECATED_METHOD,
        kotlin(
          """
                  package slack.test

                  import slack.test.ThisIsNotDeprecated

                  class TestClass {

                    public fun doStuff() {
                      ThisIsNotDeprecated().thisIsDeprecated()
                    }
                  }
                """
        ).indented()
      )
      .issues(DeprecatedAnnotationDetector.ISSUE_DEPRECATED_CALL)
      .run()
      .expect(
        """
          src/slack/test/TestClass.kt:8: Warning: slack.test.ThisIsNotDeprecated.thisIsDeprecated is deprecated; consider using an alternative. [DeprecatedCall]
              ThisIsNotDeprecated().thisIsDeprecated()
              ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
          0 errors, 1 warnings
        """.trimIndent()
      )
  }

  @Test
  fun `kotlin-sourced deprecated class use has a warning in kotlin`() {
    lint()
      .files(
        DEPRECATED_CLASS_KOTLIN,
        kotlin(
          """
                  package slack.test

                  import slack.test.ThisIsDeprecated

                  class TestClass {

                    public fun doStuff() {
                      ThisIsDeprecated()
                    }
                  }
                """
        ).indented()
      )
      .issues(DeprecatedAnnotationDetector.ISSUE_DEPRECATED_CALL)
      .run()
      .expect(
        """
          src/slack/test/TestClass.kt:8: Warning: slack.test.ThisIsDeprecated.ThisIsDeprecated is deprecated; consider using an alternative. [DeprecatedCall]
              ThisIsDeprecated()
              ~~~~~~~~~~~~~~~~~~
          0 errors, 1 warnings
        """.trimIndent()
      )
  }

  @Test
  fun `kotlin-sourced deprecated method use has a warning in kotlin`() {
    lint()
      .files(
        DEPRECATED_METHOD_KOTLIN,
        kotlin(
          """
                  package slack.test

                  import slack.test.ThisIsNotDeprecated

                  class TestClass {

                    public fun doStuff() {
                      ThisIsNotDeprecated().thisIsDeprecated()
                    }
                  }
                """
        ).indented()
      )
      .issues(DeprecatedAnnotationDetector.ISSUE_DEPRECATED_CALL)
      .run()
      .expect(
        """
          src/slack/test/TestClass.kt:8: Warning: slack.test.ThisIsNotDeprecated.thisIsDeprecated is deprecated; consider using an alternative. [DeprecatedCall]
              ThisIsNotDeprecated().thisIsDeprecated()
              ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
          0 errors, 1 warnings
        """.trimIndent()
      )
  }

  private val DEPRECATED_CLASS = java(
    """
        package slack.test;

        import java.lang.Deprecated;

        @Deprecated()
        class ThisIsDeprecated {

        }
        """
  )

  private val NON_DEPRECATED_CLASS = java(
    """
        package slack.test;

        class ThisIsNotDeprecated {

        }
        """
  )

  private val DEPRECATED_METHOD = java(
    """
        package slack.test;

        import java.lang.Deprecated;

        class ThisIsNotDeprecated {
          @Deprecated()
          public void thisIsDeprecated() {}

          public void thisIsNotDeprecated() {}
        }
        """
  )

  private val DEPRECATED_CLASS_KOTLIN = kotlin(
    """
          package slack.test

          import kotlin.Deprecated

          @Deprecated
          class ThisIsDeprecated {

          }
        """
  )

  private val DEPRECATED_METHOD_KOTLIN = kotlin(
    """
          package slack.test

          import kotlin.Deprecated

          class ThisIsNotDeprecated {

            @Deprecated
            public fun thisIsDeprecated() {
            }

            public fun thisIsNotDeprecated() {}
          }
        """
  )
}
