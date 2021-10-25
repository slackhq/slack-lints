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
    val ISSUE: Issue = Issue.create(
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

  override fun getApplicableUastTypes() =
    listOf(UClass::class.java)

  override fun createUastHandler(context: JavaContext): UElementHandler {
    return object : UElementHandler() {
      override fun visitClass(node: UClass) {
        node.findAnnotation(FQCN_EP_DNM)?.let { epDoNotMock ->
          val replacement = if ("@$FQCN_EP_DNM" in epDoNotMock.sourcePsi!!.text) {
            "@$FQCN_EP_DNM"
          } else {
            "@DoNotMock"
          }
          context.report(
            ISSUE,
            context.getLocation(epDoNotMock),
            ISSUE.getBriefDescription(TEXT),
            quickfixData = fix()
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
