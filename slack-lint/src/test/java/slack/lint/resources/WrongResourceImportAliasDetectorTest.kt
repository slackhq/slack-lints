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

import com.android.tools.lint.checks.infrastructure.TestFile
import com.android.tools.lint.detector.api.Detector
import org.junit.Test
import slack.lint.BaseSlackLintTest

class WrongResourceImportAliasDetectorTest : BaseSlackLintTest() {

  private fun propertiesFile(): TestFile.PropertyTestFile = projectProperties().apply {
    property(
      ImportAliasesLoader.IMPORT_ALIASES_PROPERTY,
      "slack.l10n.R as L10nR, slack.uikit.resources.R as SlackKitR, slack.uikit.R as UiKitR"
    )
    to(ImportAliasesLoader.PROPERTY_FILE)
  }

  override fun getDetector(): Detector = WrongResourceImportAliasDetector()

  override fun getIssues() = listOf(WrongResourceImportAliasDetector.ISSUE)

  @Test
  fun `test success`() {
    lint()
      .files(
        propertiesFile(),
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
        propertiesFile(),
        kotlin(
          """
          package slack.pkg.subpackage

          import slack.l10n.R as L10R

          class MyClass
          """
        )
          .indented()
      )
      .run()
      .expect(
        """
        src/slack/pkg/subpackage/MyClass.kt:3: Error: Use L10nR as an import alias here [WrongResourceImportAlias]
        import slack.l10n.R as L10R
        ~~~~~~~~~~~~~~~~~~~~~~~~~~~
        1 errors, 0 warnings
        """.trimIndent()
      )
      .expectFixDiffs(
        """
        Autofix for src/slack/pkg/subpackage/MyClass.kt line 3: Replace import alias:
        @@ -3 +3
        - import slack.l10n.R as L10R
        + import slack.l10n.R as L10nR
        """.trimIndent()
      )
  }

  @Test
  fun `test failure one reference`() {
    lint()
      .files(
        propertiesFile(),
        kotlin(
          """
          package slack.pkg.subpackage

          import slack.uikit.resources.R as SlackKitR
          import slack.l10n.R as L10R

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
      .expect(
        """
        src/slack/pkg/subpackage/MyClass.kt:4: Error: Use L10nR as an import alias here [WrongResourceImportAlias]
        import slack.l10n.R as L10R
        ~~~~~~~~~~~~~~~~~~~~~~~~~~~
        1 errors, 0 warnings
        """.trimIndent()
      )
      .expectFixDiffs(
        """
        Autofix for src/slack/pkg/subpackage/MyClass.kt line 4: Replace import alias:
        @@ -4 +4
        - import slack.l10n.R as L10R
        + import slack.l10n.R as L10nR
        @@ -9 +9
        -         val appName = getString(L10R.string.app_name)
        +         val appName = getString(L10nR.string.app_name)
        """.trimIndent()
      )
  }

  @Test
  fun `test failure multiple references`() {
    lint()
      .files(
        propertiesFile(),
        kotlin(
          """
          package slack.pkg.subpackage

          import slack.l10n.R as L10R

          class MyClass {

             init {
                  val appName = getString(L10R.string.app_name) + "-" + getString(L10R.string.suffix)
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
        src/slack/pkg/subpackage/MyClass.kt:3: Error: Use L10nR as an import alias here [WrongResourceImportAlias]
        import slack.l10n.R as L10R
        ~~~~~~~~~~~~~~~~~~~~~~~~~~~
        1 errors, 0 warnings
        """.trimIndent()
      )
      .expectFixDiffs(
        """
        Autofix for src/slack/pkg/subpackage/MyClass.kt line 3: Replace import alias:
        @@ -3 +3
        - import slack.l10n.R as L10R
        + import slack.l10n.R as L10nR
        @@ -8 +8
        -         val appName = getString(L10R.string.app_name) + "-" + getString(L10R.string.suffix)
        +         val appName = getString(L10nR.string.app_name) + "-" + getString(L10nR.string.suffix)
        """.trimIndent()
      )
  }

  @Test
  fun `test failure multiple wrong imports`() {
    lint()
      .files(
        propertiesFile(),
        kotlin(
          """
          package slack.pkg.subpackage

          import slack.uikit.resources.R as SKR
          import slack.l10n.R as L10R

          class MyClass {

             init {
                  getColor(SKR.color.transparent).let { println(it) }
                  val appName = getString(L10R.string.app_name) + "-" + getString(L10R.string.suffix)
              }

           }
          """
        )
          .indented()
      )
      .run()
      .expect(
        """
        src/slack/pkg/subpackage/MyClass.kt:3: Error: Use SlackKitR as an import alias here [WrongResourceImportAlias]
        import slack.uikit.resources.R as SKR
        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        1 errors, 0 warnings
        """.trimIndent()
      )
      .expectFixDiffs(
        """
        Autofix for src/slack/pkg/subpackage/MyClass.kt line 3: Replace import alias:
        @@ -3 +3
        - import slack.uikit.resources.R as SKR
        + import slack.uikit.resources.R as SlackKitR
        @@ -9 +9
        -         getColor(SKR.color.transparent).let { println(it) }
        +         getColor(SlackKitR.color.transparent).let { println(it) }
        """.trimIndent()
      )
  }

  @Test
  fun `test no fix`() {
    lint()
      .files(
        propertiesFile(),
        kotlin(
          """
          package slack.pkg.subpackage

          import slack.pkg.subpkg.R as SubPkgR

          class MyClass {

             init {
                  val appName = getString(SubPkgR.string.app_name)
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
  fun `test java no-op`() {
    lint()
      .files(
        propertiesFile(),
        java(
          """
          package slack.pkg.subpackage;

          import slack.l10n.R;

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
