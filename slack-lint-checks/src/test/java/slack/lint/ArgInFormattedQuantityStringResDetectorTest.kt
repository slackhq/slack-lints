// Copyright (C) 2021 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint

import com.android.tools.lint.checks.infrastructure.TestMode
import org.junit.Test

class ArgInFormattedQuantityStringResDetectorTest : BaseSlackLintTest() {

  override val skipTestModes: Array<TestMode> = arrayOf(TestMode.PARENTHESIZED)

  override fun getDetector() = ArgInFormattedQuantityStringResDetector()

  override fun getIssues() = ArgInFormattedQuantityStringResDetector.issues.toList()

  @Test
  fun getQuantityStringWithNoLocalizedFormatTest() {
    lint()
      .files(
        java(
          "" +
            "package com.slack.lint;\n" +
            "\n" +
            "import android.content.res.Resources;\n" +
            "\n" +
            "public class Foo {\n" +
            "  public void bar(Resources res) {\n" +
            "    String s = res.getQuantityString(0, 3, 3, \"asdf\");\n" +
            "  }\n" +
            "}\n"
        )
      )
      .run()
      .expect(
        ("src/com/slack/lint/Foo.java:7: Warning: This may require a localized count modifier. If so, use LocalizationUtils.getFormattedCount(). Consult #plz-localization if you are unsure. [ArgInFormattedQuantityStringRes]\n" +
          "    String s = res.getQuantityString(0, 3, 3, \"asdf\");\n" +
          "                                           ~\n" +
          "src/com/slack/lint/Foo.java:7: Warning: This may require a localized count modifier. If so, use LocalizationUtils.getFormattedCount(). Consult #plz-localization if you are unsure. [ArgInFormattedQuantityStringRes]\n" +
          "    String s = res.getQuantityString(0, 3, 3, \"asdf\");\n" +
          "                                              ~~~~~~\n" +
          "0 errors, 2 warnings\n")
      )
  }

  @Test
  fun getQuantityStringWithNoLocalizedFormatTestKotlin() {
    lint()
      .files(
        kotlin(
          "" +
            "package com.slack.lint\n" +
            "\n" +
            "import android.content.res.Resources\n" +
            "\n" +
            "class Foo {\n" +
            "  fun bar(res: Resources) {\n" +
            "    val s = res.getQuantityString(0, 3, 3, \"asdf\")\n" +
            "  }\n" +
            "}"
        )
      )
      .run()
      .expect(
        """
          src/com/slack/lint/Foo.kt:7: Warning: This may require a localized count modifier. If so, use LocalizationUtils.getFormattedCount(). Consult #plz-localization if you are unsure. [ArgInFormattedQuantityStringRes]
              val s = res.getQuantityString(0, 3, 3, "asdf")
                                                  ~
          src/com/slack/lint/Foo.kt:7: Warning: This may require a localized count modifier. If so, use LocalizationUtils.getFormattedCount(). Consult #plz-localization if you are unsure. [ArgInFormattedQuantityStringRes]
              val s = res.getQuantityString(0, 3, 3, "asdf")
                                                     ~~~~~~
          0 errors, 2 warnings
        """.trimIndent()
      )
  }

  @Test
  fun getQuantityStringWithLocalizedFormatTest() {
    lint()
      .files(
        java(
          ("" +
            "package com.slack.lint;\n" +
            "import static com.slack.lint.Foo.LocalizationUtils.getFormattedCount;\n" +
            "\n" +
            "import android.content.res.Resources;\n" +
            "\n" +
            "public class Foo {\n" +
            "  public void bar(Resources res) {\n" +
            "    String s = res.getQuantityString(0, 3, getFormattedCount(), 3);\n" +
            "  }\n" +
            "  public static class LocalizationUtils {\n" +
            "   public static String getFormattedCount() { return \"\"; }\n" +
            "  }" +
            "}\n")
        )
      )
      .run()
      .expect(
        "src/com/slack/lint/Foo.java:8: Warning: This may require a localized count modifier. If so, use LocalizationUtils.getFormattedCount(). Consult #plz-localization if you are unsure. [ArgInFormattedQuantityStringRes]\n" +
          "    String s = res.getQuantityString(0, 3, getFormattedCount(), 3);\n" +
          "                                                                ~\n" +
          "0 errors, 1 warnings\n"
      )
  }

  @Test
  fun getQuantityStringWithLocalizedFormatTestKotlin() {
    lint()
      .files(
        kotlin(
          "" +
            "package com.slack.lint\n" +
            "\n" +
            "import android.content.res.Resources\n" +
            "import com.slack.lint.Foo.LocalizationUtils.Companion.getFormattedCount\n" +
            "\n" +
            "class Foo {\n" +
            "  fun bar(res: Resources) {\n" +
            "    val s = res.getQuantityString(0, 3, getFormattedCount(), 3)\n" +
            "  }\n" +
            "\n" +
            "  class LocalizationUtils {\n" +
            "    companion object {\n" +
            "      fun getFormattedCount(): String {\n" +
            "        return \"\"\n" +
            "      }\n" +
            "    }\n" +
            "  }\n" +
            "}\n"
        )
      )
      .run()
      .expect(
        "src/com/slack/lint/Foo.kt:8: Warning: This may require a localized count modifier. If so, use LocalizationUtils.getFormattedCount(). Consult #plz-localization if you are unsure. [ArgInFormattedQuantityStringRes]\n" +
          "    val s = res.getQuantityString(0, 3, getFormattedCount(), 3)\n" +
          "                                                             ~\n" +
          "0 errors, 1 warnings\n"
      )
  }

  @Test
  fun getQuantityStringWithOutsideAssignmentLocalizedFormatTest() {
    lint()
      .files(
        java(
          ("" +
            "package com.slack.lint;\n" +
            "import static com.slack.lint.Foo.LocalizationUtils.getFormattedCount;\n" +
            "\n" +
            "import android.content.res.Resources;\n" +
            "\n" +
            "public class Foo {\n" +
            "  public void bar(Resources res) {\n" +
            "    final String a = getFormattedCount();\n" +
            "    String s = res.getQuantityString(0, 3, a, 3);\n" +
            "  }\n" +
            "  public static class LocalizationUtils {\n" +
            "   public static String getFormattedCount() { return \"\"; }\n" +
            "  }\n" +
            "}\n")
        )
      )
      .run()
      .expect(
        "src/com/slack/lint/Foo.java:9: Warning: This may require a localized count modifier. If so, use LocalizationUtils.getFormattedCount(). Consult #plz-localization if you are unsure. [ArgInFormattedQuantityStringRes]\n" +
          "    String s = res.getQuantityString(0, 3, a, 3);\n" +
          "                                              ~\n" +
          "0 errors, 1 warnings\n"
      )
  }

  @Test
  fun getQuantityStringWithOutsideAssignmentLocalizedFormatTestKotlin() {
    lint()
      .files(
        kotlin(
          "" +
            "package com.slack.lint\n" +
            "\n" +
            "import android.content.res.Resources\n" +
            "import com.slack.lint.Foo.LocalizationUtils.Companion.getFormattedCount\n" +
            "\n" +
            "class Foo {\n" +
            "  fun bar(res: Resources) {\n" +
            "    val s = res.getQuantityString(0, 3, getFormattedCount(), \"asdf\")\n" +
            "  }\n" +
            "\n" +
            "  class LocalizationUtils {\n" +
            "    companion object {\n" +
            "      fun getFormattedCount(): String {\n" +
            "        return \"\"\n" +
            "      }\n" +
            "    }\n" +
            "  }\n" +
            "}"
        )
      )
      .run()
      .expect(
        """
          src/com/slack/lint/Foo.kt:8: Warning: This may require a localized count modifier. If so, use LocalizationUtils.getFormattedCount(). Consult #plz-localization if you are unsure. [ArgInFormattedQuantityStringRes]
              val s = res.getQuantityString(0, 3, getFormattedCount(), "asdf")
                                                                       ~~~~~~
          0 errors, 1 warnings
        """.trimIndent()
      )
  }

  @Test
  fun getQuantityStringWithFullyQualifiedMethodName() {
    lint()
      .files(
        kotlin(
          "" +
            "package com.slack.lint\n" +
            "\n" +
            "import android.content.res.Resources\n" +
            "\n" +
            "class Foo {\n" +
            "  fun bar(res: Resources) {\n" +
            "    val s = res.getQuantityString(0, 3, LocalizationUtils.getFormattedCount(), \"asdf\")\n" +
            "  }\n" +
            "  \n" +
            "  class LocalizationUtils {\n" +
            "    companion object {\n" +
            "      fun getFormattedCount(): String {\n" +
            "        return \"\"\n" +
            "      }\n" +
            "    }\n" +
            "  }\n" +
            "}"
        )
      )
      .run()
      .expect(
        """
          src/com/slack/lint/Foo.kt:7: Warning: This may require a localized count modifier. If so, use LocalizationUtils.getFormattedCount(). Consult #plz-localization if you are unsure. [ArgInFormattedQuantityStringRes]
              val s = res.getQuantityString(0, 3, LocalizationUtils.getFormattedCount(), "asdf")
                                                                                         ~~~~~~
          0 errors, 1 warnings
        """.trimIndent()
      )
  }

  @Test
  fun getQuantityStringNoFormatArgsTest() {
    lint()
      .files(
        java(
          "" +
            "package com.slack.lint;\n" +
            "\n" +
            "import android.content.res.Resources;\n" +
            "\n" +
            "public class Foo {\n" +
            "  public void bar(Resources res) {\n" +
            "    String s = res.getQuantityString(0, 3);\n" +
            "  }\n" +
            "}\n"
        )
      )
      .run()
      .expectClean()
  }

  @Test
  fun getQuantityStringNoFormatArgsTestKotlin() {
    lint()
      .files(
        kotlin(
          "" +
            "package com.slack.lint\n" +
            "\n" +
            "import android.content.res.Resources\n" +
            "\n" +
            "class Foo {\n" +
            "  fun bar(res: Resources) {\n" +
            "    val s = res.getQuantityString(0, 3)\n" +
            "  }\n" +
            "}"
        )
      )
      .run()
      .expectClean()
  }
}
