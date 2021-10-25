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

class NonKotlinPairDetectorTest : BaseSlackLintTest() {
  override fun getDetector(): Detector = NonKotlinPairDetector()

  override fun getIssues() = NonKotlinPairDetector.issues.toList()

  @Test
  fun `Java - AndroidX Pair create shows warning`() {
    lint()
      .files(
        ANDROIDX_PAIR_STUB,
        java(
          """
                  package slack.test;

                  import androidx.core.util.Pair;

                  public class TestClass {

                    public void doStuff() {
                      new Integer(5);
                      new String("TestString").toString();
                      Pair pair = Pair.create("first", "second");
                      pair.first.toString();
                    }
                  }
                """
        ).indented()
      )
      .issues(*NonKotlinPairDetector.issues)
      .run()
      .expect(
        """
              src/slack/test/TestClass.java:10: Warning: Use Kotlin's kotlin.Pair instead of other Pair types from other libraries like AndroidX and Slack commons [KotlinPairNotCreated]
                  Pair pair = Pair.create("first", "second");
                              ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
              0 errors, 1 warnings
            """
      )
  }

  @Test
  fun `Java - AndroidX Pair constructor create shows warning`() {
    lint()
      .files(
        ANDROIDX_PAIR_STUB,
        java(
          """
                  package slack.test;

                  import androidx.core.util.Pair;

                  public class TestClass {

                    public void doStuff() {
                      new Integer(5);
                      new String("TestString").toString();
                      new Pair<>("first", "second").first.toString();
                    }
                  }
                """
        ).indented()
      )
      .issues(*NonKotlinPairDetector.issues)
      .run()
      .expect(
        """
              src/slack/test/TestClass.java:10: Warning: Use Kotlin's kotlin.Pair instead of other Pair types from other libraries like AndroidX and Slack commons [KotlinPairNotCreated]
                  new Pair<>("first", "second").first.toString();
                  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
              0 errors, 1 warnings
            """
      )
  }

  @Test
  fun `Java - Slack commons Pair constructor create shows warning`() {
    lint()
      .files(
        SLACK_COMMONS_PAIR,
        java(
          """
                  package slack.test;

                  import slack.commons.Pair;

                  public class TestClass {

                    public void doStuff() {
                      new Integer(5);
                      new String("TestString").toString();
                      new Pair<>("first", "second").getFirst();
                    }
                  }
                """
        ).indented()
      )
      .issues(*NonKotlinPairDetector.issues)
      .run()
      .expect(
        """
              src/slack/test/TestClass.java:10: Warning: Use Kotlin's kotlin.Pair instead of other Pair types from other libraries like AndroidX and Slack commons [KotlinPairNotCreated]
                  new Pair<>("first", "second").getFirst();
                  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
              0 errors, 1 warnings
            """
      )
  }

  @Test
  fun `Java - Full qualified Slack commons Pair constructor create shows warning`() {
    lint()
      .files(
        SLACK_COMMONS_PAIR,
        java(
          """
                  package slack.test;

                  public class TestClass {

                    public void doStuff() {
                      new Integer(5);
                      new String("TestString").toString();
                      new slack.commons.Pair<>("first", "second").getFirst();
                    }
                  }
                """
        ).indented()
      )
      .issues(*NonKotlinPairDetector.issues)
      .run()
      .expect(
        """
              src/slack/test/TestClass.java:8: Warning: Use Kotlin's kotlin.Pair instead of other Pair types from other libraries like AndroidX and Slack commons [KotlinPairNotCreated]
                  new slack.commons.Pair<>("first", "second").getFirst();
                  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
              0 errors, 1 warnings
            """
      )
  }

  @Test
  fun `Kotlin - Kotlin Pair constructor create has no warnings`() {
    lint()
      .files(
        KOTLIN_PAIR,
        kotlin(
          """
                  package slack.test

                  import kotlin.Pair

                  class TestClass {

                    fun doStuff() {
                      String("TestString").toString()
                      Integer(6)
                      val pair = Pair("first", "second")
                    }
                  }
                """
        ).indented()
      )
      .issues(*NonKotlinPairDetector.issues)
      .run()
      .expectClean()
  }

  @Test
  fun `Kotlin - AndroidX Pair create shows warning`() {
    lint()
      .files(
        ANDROIDX_PAIR_STUB,
        kotlin(
          """
                  package slack.test

                  import androidx.core.util.Pair

                  class TestClass {

                    fun doStuff() {
                      Integer(5)
                      String("TestString").toString()
                      val pair = Pair.create("first", "second")
                    }
                  }
                """
        ).indented()
      )
      .issues(*NonKotlinPairDetector.issues)
      .run()
      .expect(
        """
              src/slack/test/TestClass.kt:10: Warning: Use Kotlin's kotlin.Pair instead of other Pair types from other libraries like AndroidX and Slack commons [KotlinPairNotCreated]
                  val pair = Pair.create("first", "second")
                             ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
              0 errors, 1 warnings
            """
      )
  }

  @Test
  fun `Kotlin - AndroidX Pair constructor create shows warning`() {
    lint()
      .files(
        ANDROIDX_PAIR_STUB,
        kotlin(
          """
                  package slack.test

                  import androidx.core.util.Pair

                  class TestClass {

                    fun doStuff() {
                      Integer(5)
                      String("TestString").toString()
                      val pair = Pair("first", "second")
                    }
                  }
                """
        ).indented()
      )
      .issues(*NonKotlinPairDetector.issues)
      .run()
      .expect(
        """
              src/slack/test/TestClass.kt:10: Warning: Use Kotlin's kotlin.Pair instead of other Pair types from other libraries like AndroidX and Slack commons [KotlinPairNotCreated]
                  val pair = Pair("first", "second")
                             ~~~~~~~~~~~~~~~~~~~~~~~
              0 errors, 1 warnings
            """
      )
  }

  @Test
  fun `Kotlin - Slack commons Pair constructor create shows warning`() {
    lint()
      .files(
        SLACK_COMMONS_PAIR,
        kotlin(
          """
                  package slack.test

                  import slack.commons.Pair

                  class TestClass {

                    fun doStuff() {
                      Integer(5)
                      Integer(6)
                      String("TestString").toString()
                      val pair = Pair("first", "second")
                    }
                  }
                """
        ).indented()
      )
      .issues(*NonKotlinPairDetector.issues)
      .run()
      .expect(
        """
              src/slack/test/TestClass.kt:11: Warning: Use Kotlin's kotlin.Pair instead of other Pair types from other libraries like AndroidX and Slack commons [KotlinPairNotCreated]
                  val pair = Pair("first", "second")
                             ~~~~~~~~~~~~~~~~~~~~~~~
              0 errors, 1 warnings
            """
      )
  }

  companion object {
    private val ANDROIDX_PAIR_STUB = java(
      """
          package androidx.core.util;
          public class Pair<F, S> {
            public final F first;
            public final S second;

            public Pair(F first, S second) {
                this.first = first;
                this.second = second;
            }

            public static <A, B> Pair<A, B> create(A a, B b) {
                return null;
            }
          }
        """
    )

    private val SLACK_COMMONS_PAIR = kotlin(
      """
          package slack.commons
          data class Pair<out A, out B>(
              val first: A,
              val second: B
          )
        """
    )
  }
  private val KOTLIN_PAIR = kotlin(
    """
          package kotlin
          public data class Pair<out A, out B>(
              public val first: A,
              public val second: B
          )
        """
  )
}
