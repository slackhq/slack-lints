// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint.naming

import com.android.tools.lint.checks.infrastructure.TestMode
import org.junit.Test
import slack.lint.BaseSlackLintTest

class InvalidPackageDeclarationDetectorTest : BaseSlackLintTest() {
  override fun getDetector() = InvalidPackageDeclarationDetector()

  override fun getIssues() = listOf(InvalidPackageDeclarationDetector.ISSUE)

  override val skipTestModes = arrayOf(TestMode.WHITESPACE, TestMode.SUPPRESSIBLE)

  @Test
  fun `clean - package matches directory structure`() {
    lint()
      .files(
        kotlin(
            "src/main/java/com/example/foo/Example.kt",
            """
          package com.example.foo

          class Example
          """,
          )
          .indented()
      )
      .run()
      .expectClean()
  }

  @Test
  fun `error - package does not match directory structure`() {
    lint()
      .files(
        kotlin(
            "src/main/java/com/example/foo/Example.kt",
            """
          package com.example.wrong

          class Example
          """,
          )
          .indented()
      )
      .run()
      .expectContains(
        "Package declaration com.example.wrong does not match directory structure. Expected com.example.foo."
      )
  }
}
