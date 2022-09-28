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

import com.android.tools.lint.checks.infrastructure.TestLintTask
import com.android.tools.lint.detector.api.Detector
import org.junit.Test
import slack.lint.BaseSlackLintTest

class MissingResourceImportAliasDetectorTest : BaseSlackLintTest() {

  override fun getDetector(): Detector = MissingResourceImportAliasDetector()

  override fun getIssues() = listOf(MissingResourceImportAliasDetector.ISSUE)

  override fun lint(): TestLintTask {
    return super.lint()
      .configureOption(
        ImportAliasesLoader.IMPORT_ALIASES,
        "slack.l10n.R as L10nR, slack.uikit.resources.R as SlackKitR, slack.uikit.R as UiKitR"
      )
  }

  @Test
  fun `test success`() {
    lint()
      .files(
        kotlin(
          """
          package slack.pkg.subpackage

          import slack.l10n.R as L10nR
          import slack.pkg.R

          class MyClass {

             init {
                  val appName = getString(L10R.string.app_name)
              }

           }
          """
        )
          .indented()
      )
      .run()
      .expectClean()
  }

  @Test
  fun `test failure no references`() {
    lint()
      .files(
        kotlin(
          """
          package slack.pkg.subpackage

          import slack.l10n.R

          class MyClass
          """
        )
          .indented()
      )
      .run()
      .expect(
        """
        src/slack/pkg/subpackage/MyClass.kt:3: Error: Use an import alias for R classes from other modules [MissingResourceImportAlias]
        import slack.l10n.R
        ~~~~~~~~~~~~~~~~~~~
        1 errors, 0 warnings
        """.trimIndent()
      )
      .expectFixDiffs(
        """
        Autofix for src/slack/pkg/subpackage/MyClass.kt line 3: Add import alias:
        @@ -3 +3
        - import slack.l10n.R
        + import slack.l10n.R as L10nR
        """.trimIndent()
      )
  }

  @Test
  fun `test failure one reference`() {
    lint()
      .files(
        kotlin(
          """
          package slack.pkg.subpackage

          import slack.l10n.R

          class MyClass {

             init {
                  val appName = getString(R.string.app_name)
              }

           }
          """
        )
          .indented()
      )
      .run()
      .expect(
        """
        src/slack/pkg/subpackage/MyClass.kt:3: Error: Use an import alias for R classes from other modules [MissingResourceImportAlias]
        import slack.l10n.R
        ~~~~~~~~~~~~~~~~~~~
        1 errors, 0 warnings
        """.trimIndent()
      )
      .expectFixDiffs(
        """
        Autofix for src/slack/pkg/subpackage/MyClass.kt line 3: Add import alias:
        @@ -3 +3
        - import slack.l10n.R
        + import slack.l10n.R as L10nR
        @@ -8 +8
        -         val appName = getString(R.string.app_name)
        +         val appName = getString(L10nR.string.app_name)
        """.trimIndent()
      )
  }

  @Test
  fun `test failure multiple references`() {
    lint()
      .files(
        kotlin(
          """
          package slack.pkg.subpackage

          import slack.l10n.R

          class MyClass {

             init {
                  val appName = getString(R.string.app_name) + "-" + getString(R.string.suffix)
                  getColor(R.color.transparent).let { println(it) }
              }

           }
          """
        )
          .indented()
      )
      .run()
      .expect(
        """
        src/slack/pkg/subpackage/MyClass.kt:3: Error: Use an import alias for R classes from other modules [MissingResourceImportAlias]
        import slack.l10n.R
        ~~~~~~~~~~~~~~~~~~~
        1 errors, 0 warnings
        """.trimIndent()
      )
      .expectFixDiffs(
        """
        Autofix for src/slack/pkg/subpackage/MyClass.kt line 3: Add import alias:
        @@ -3 +3
        - import slack.l10n.R
        + import slack.l10n.R as L10nR
        @@ -8 +8
        -         val appName = getString(R.string.app_name) + "-" + getString(R.string.suffix)
        -         getColor(R.color.transparent).let { println(it) }
        +         val appName = getString(L10nR.string.app_name) + "-" + getString(L10nR.string.suffix)
        +         getColor(L10nR.color.transparent).let { println(it) }
        """.trimIndent()
      )
  }

  @Test
  fun `test failure VERSION_CODES R reference`() {
    lint()
      .files(
        kotlin(
          """
          package slack.pkg.subpackage

          import slack.l10n.R

          class MyClass {

             init {
                  val appName = getString(R.string.app_name) + "-" + getString(R.string.suffix)
                  if (isAtLeastApi(Build.VERSION_CODES.R)) {
                        getColor(R.color.transparent).let { println(it) }
                  }
              }

           }
          """
        )
          .indented()
      )
      .run()
      .expect(
        """
        src/slack/pkg/subpackage/MyClass.kt:3: Error: Use an import alias for R classes from other modules [MissingResourceImportAlias]
        import slack.l10n.R
        ~~~~~~~~~~~~~~~~~~~
        1 errors, 0 warnings
        """.trimIndent()
      )
      .expectFixDiffs(
        """
        Autofix for src/slack/pkg/subpackage/MyClass.kt line 3: Add import alias:
        @@ -3 +3
        - import slack.l10n.R
        + import slack.l10n.R as L10nR
        @@ -8 +8
        -         val appName = getString(R.string.app_name) + "-" + getString(R.string.suffix)
        +         val appName = getString(L10nR.string.app_name) + "-" + getString(L10nR.string.suffix)
        @@ -10 +10
        -               getColor(R.color.transparent).let { println(it) }
        +               getColor(L10nR.color.transparent).let { println(it) }
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

          import slack.pkg.subpkg.R

          class MyClass {

             init {
                  val appName = getString(R.string.app_name)
              }

           }
          """
        )
          .indented()
      )
      .run()
      .expect(
        """
        src/slack/pkg/subpackage/MyClass.kt:3: Error: Use an import alias for R classes from other modules [MissingResourceImportAlias]
        import slack.pkg.subpkg.R
        ~~~~~~~~~~~~~~~~~~~~~~~~~
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

          import slack.pkg.subpkg.R;

          class MyClass {

             MyClass() {
                  String appName = getString(R.string.app_name);
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
