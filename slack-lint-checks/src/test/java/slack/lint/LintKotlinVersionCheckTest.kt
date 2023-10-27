// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import org.jetbrains.uast.UMethod
import org.junit.Test
import slack.lint.util.sourceImplementation

/**
 * Lint chases Kotlin versions differently than what we declare our build with. This test is just
 * here to catch those for our own awareness and should be updated whenever lint updates its own.
 */
class LintKotlinVersionCheckTest : BaseSlackLintTest() {

  override fun getDetector(): Detector = KotlinVersionDetector()

  override fun getIssues() = listOf(KotlinVersionDetector.ISSUE)

  @Test
  fun check() {
    lint()
      .files(
        kotlin(
            """
            package test

            fun main() {
              println("Hello, world!")
            }
          """
          )
          .indented()
      )
      .run()
      .expect(
        """
          src/test/test.kt:3: Error: Kotlin version matched expected one [KotlinVersion]
          fun main() {
          ^
          1 errors, 0 warnings
        """
          .trimIndent()
      )
  }

  class KotlinVersionDetector : Detector(), SourceCodeScanner {
    override fun getApplicableUastTypes() = listOf(UMethod::class.java)

    override fun createUastHandler(context: JavaContext): UElementHandler {
      return object : UElementHandler() {
        override fun visitMethod(node: UMethod) {
          if (KotlinVersion.CURRENT == EXPECTED_VERSION) {
            // Report something anyway to ensure our lint was correctly picked up at least
            context.report(
              ISSUE,
              node,
              context.getLocation(node),
              "Kotlin version matched expected one"
            )
          } else {
            context.report(
              ISSUE,
              node,
              context.getLocation(node),
              "Kotlin version was ${KotlinVersion.CURRENT}, expected $EXPECTED_VERSION"
            )
          }
        }
      }
    }

    companion object {
      private val EXPECTED_VERSION = KotlinVersion(1, 9, 0)
      val ISSUE =
        Issue.create(
          "KotlinVersion",
          "Kotlin version",
          "Kotlin version",
          sourceImplementation<KotlinVersionDetector>(),
          severity = Severity.ERROR,
        )
    }
  }
}
