/*
 * Copyright (C) 2022 Slack Technologies, LLC
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
package slack.lint.resources

import com.android.tools.lint.detector.api.Detector
import org.junit.Test
import slack.lint.BaseSlackLintTest

class FullyQualifiedResourceDetectorTest : BaseSlackLintTest() {

  override fun getDetector(): Detector = FullyQualifiedResourceDetector()

  override fun getIssues() = listOf(FullyQualifiedResourceDetector.ISSUE)

  @Test
  fun `test success`() {
    lint()
      .files(
        kotlin(
          """
          package slack.pkg.subpackage

          import slack.l10n.R as L10nR

          class MyClass {

             init {
                  val appName = getString(L10R.string.app_name)
              }

           }
          """
        ).indented()
      )
      .run()
      .expectClean()
  }

  @Test
  fun `test failure no imports`() {
    lint()
      .files(
        kotlin(
          """
          package slack.pkg.subpackage

          class MyClass {

             init {
                  val appName = getString(slack.l10n.R.string.app_name)
              }

           }
          """
        )
          .indented()
      )
      .run()
      .expect(
        """
        src/slack/pkg/subpackage/MyClass.kt:6: Error: Use L10nR as an import alias instead [FullyQualifiedResource]
                val appName = getString(slack.l10n.R.string.app_name)
                                        ~~~~~~~~~~~~
        1 errors, 0 warnings
        """.trimIndent()
      )
      .expectFixDiffs(
        """
        Autofix for src/slack/pkg/subpackage/MyClass.kt line 6: Replace import alias:
        @@ -3 +3
        + import slack.l10n.R as L10nR
        +
        @@ -6 +8
        -         val appName = getString(slack.l10n.R.string.app_name)
        +         val appName = getString(L10nR.string.app_name)
        """.trimIndent()
      )
  }

  @Test
  fun `test failure no import`() {
    lint()
      .files(
        kotlin(
          """
          package slack.pkg.subpackage

          import slack.pkg.R

          class MyClass {

             init {
                  val appName = getString(slack.l10n.R.string.app_name)
              }

           }
          """
        )
          .indented()
      )
      .run()
      .expect(
        """
        src/slack/pkg/subpackage/MyClass.kt:8: Error: Use L10nR as an import alias instead [FullyQualifiedResource]
                val appName = getString(slack.l10n.R.string.app_name)
                                        ~~~~~~~~~~~~
        1 errors, 0 warnings
        """.trimIndent()
      )
      .expectFixDiffs(
        """
        Autofix for src/slack/pkg/subpackage/MyClass.kt line 8: Replace import alias:
        @@ -4 +4
        + import slack.l10n.R as L10nR
        @@ -8 +9
        -         val appName = getString(slack.l10n.R.string.app_name)
        +         val appName = getString(L10nR.string.app_name)
        """.trimIndent()
      )
  }

  @Test
  fun `test failure import`() {
    lint()
      .files(
        kotlin(
          """
          package slack.pkg.subpackage

          import slack.pkg.R
          import slack.l10n.R as L10nR

          class MyClass {

             init {
                  val appName = getString(slack.l10n.R.string.app_name)
              }

           }
          """
        )
          .indented()
      )
      .run()
      .expect(
        """
        src/slack/pkg/subpackage/MyClass.kt:9: Error: Use L10nR as an import alias instead [FullyQualifiedResource]
                val appName = getString(slack.l10n.R.string.app_name)
                                        ~~~~~~~~~~~~
        1 errors, 0 warnings
        """.trimIndent()
      )
      .expectFixDiffs(
        """
        Autofix for src/slack/pkg/subpackage/MyClass.kt line 9: Replace import alias:
        @@ -9 +9
        -         val appName = getString(slack.l10n.R.string.app_name)
        +         val appName = getString(L10nR.string.app_name)
        """.trimIndent()
      )
  }

  @Test
  fun `test failure import without alias`() {
    lint()
      .files(
        kotlin(
          """
          package slack.pkg.subpackage

          import slack.l10n.R

          class MyClass {

             init {
                  val appName = getString(slack.l10n.R.string.app_name)
              }

           }
          """
        )
          .indented()
      )
      .run()
      .expect(
        """
        src/slack/pkg/subpackage/MyClass.kt:8: Error: Use L10nR as an import alias instead [FullyQualifiedResource]
                val appName = getString(slack.l10n.R.string.app_name)
                                        ~~~~~~~~~~~~
        1 errors, 0 warnings
        """.trimIndent()
      )
      .expectFixDiffs(
        """
        Autofix for src/slack/pkg/subpackage/MyClass.kt line 8: Replace import alias:
        @@ -4 +4
        + import slack.l10n.R as L10nR
        @@ -8 +9
        -         val appName = getString(slack.l10n.R.string.app_name)
        +         val appName = getString(L10nR.string.app_name)
        """.trimIndent()
      )
  }

  @Test
  fun `test failure import wrong alias`() {
    lint()
      .files(
        kotlin(
          """
          package slack.pkg.subpackage

          import slack.l10n.R as L10R

          class MyClass {

             init {
                  val appName = getString(slack.l10n.R.string.app_name)
              }

           }
          """
        )
          .indented()
      )
      .run()
      .expect(
        """
        src/slack/pkg/subpackage/MyClass.kt:8: Error: Use L10nR as an import alias instead [FullyQualifiedResource]
                val appName = getString(slack.l10n.R.string.app_name)
                                        ~~~~~~~~~~~~
        1 errors, 0 warnings
        """.trimIndent()
      )
      .expectFixDiffs(
        """
        Autofix for src/slack/pkg/subpackage/MyClass.kt line 8: Replace import alias:
        @@ -4 +4
        + import slack.l10n.R as L10nR
        @@ -8 +9
        -         val appName = getString(slack.l10n.R.string.app_name)
        +         val appName = getString(L10nR.string.app_name)
        """.trimIndent()
      )
  }

  @Test
  fun `test failure no fix`() {
    lint()
      .files(
        kotlin(
          """
          package slack.pkg.subpackage

          import slack.l10n.R as L10R

          class MyClass {

             init {
                  val appName = getString(slack.pkg.R.string.app_name)
              }

           }
          """
        )
          .indented()
      )
      .run()
      .expect(
        """
        src/slack/pkg/subpackage/MyClass.kt:8: Error: Use an import alias instead [FullyQualifiedResource]
                val appName = getString(slack.pkg.R.string.app_name)
                                        ~~~~~~~~~~~
        1 errors, 0 warnings
        """.trimIndent()
      )
      .expectFixDiffs("")
  }

  @Test
  fun `test java no-op`() {
    lint()
      .files(
        java(
          """
          package slack.pkg.subpackage;

          class MyClass {
            MyClass(){
            String appName = getString(slack.l10n.R.string.app_name);
           }

           }
          """
        )
          .indented()
      )
      .run()
      .expectClean()
  }
}
