// Copyright (C) 2021 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint.mocking

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.android.tools.lint.detector.api.TextFormat.TEXT
import org.jetbrains.uast.UClass
import slack.lint.util.sourceImplementation

/**
 * A simple checker that checks for use of error-prone's `@DoNotMock` and suggests replacing with
 * our slack.lint.annotation version.
 */
class ErrorProneDoNotMockDetector : Detector(), SourceCodeScanner {
  companion object {
    val ISSUE: Issue =
      Issue.create(
        "ErrorProneDoNotMockUsage",
        "Use Slack's internal `@DoNotMock` annotation.",
        """
      While error-prone has a `@DoNotMock` annotation, prefer to use Slack's internal one as it's \
      not specific to error-prone and won't go away in a Java-less world.
      """,
        Category.CORRECTNESS,
        6,
        Severity.ERROR,
        sourceImplementation<ErrorProneDoNotMockDetector>()
      )

    private const val FQCN_SLACK_DNM = "slack.lint.annotations.DoNotMock"
    private const val FQCN_EP_DNM = "com.google.errorprone.annotations.DoNotMock"
  }

  override fun getApplicableUastTypes() = listOf(UClass::class.java)

  override fun createUastHandler(context: JavaContext): UElementHandler {
    return object : UElementHandler() {
      override fun visitClass(node: UClass) {
        node.findAnnotation(FQCN_EP_DNM)?.let { epDoNotMock ->
          val replacement =
            if ("@$FQCN_EP_DNM" in epDoNotMock.sourcePsi!!.text) {
              "@$FQCN_EP_DNM"
            } else {
              "@DoNotMock"
            }
          context.report(
            ISSUE,
            context.getLocation(epDoNotMock),
            ISSUE.getBriefDescription(TEXT),
            quickfixData =
              fix()
                .replace()
                .name("Replace with slack.lint.annotations.DoNotMock")
                .range(context.getLocation(epDoNotMock))
                .shortenNames()
                .text(replacement)
                .with("@$FQCN_SLACK_DNM")
                .autoFix()
                .build()
          )
        }
      }
    }
  }
}
