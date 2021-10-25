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

import com.android.tools.lint.client.api.LintClient
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.Severity
import slack.lint.util.Priorities
import slack.lint.util.sourceImplementation

/**
 * Raises a warning whenever we use deprecated methods or classes. Generally used for keeping track of health score.
 *
 */
class DeprecatedAnnotationDetector : AnnotatedClassOrMethodUsageDetector() {

  override val annotationNames = listOf(DEPRECATED_ANNOTATION_NAME_KOTLIN, DEPRECATED_ANNOTATION_NAME_JAVA)
  override val issue = ISSUE_DEPRECATED_CALL

  // Only enable on CLI
  override val isEnabled: Boolean
    get() = !LintClient.isStudio

  companion object {
    private const val DEPRECATED_ANNOTATION_NAME_JAVA = "java.lang.Deprecated"
    private const val DEPRECATED_ANNOTATION_NAME_KOTLIN = "kotlin.Deprecated"

    private fun Implementation.toIssue(): Issue {
      return Issue.create(
        "DeprecatedCall",
        "This class or method is deprecated; consider using an alternative.",
        "Using deprecated classes is not advised; please consider using an alternative.",
        Category.CORRECTNESS,
        Priorities.NORMAL,
        Severity.WARNING,
        this
      )
    }

    val ISSUE_DEPRECATED_CALL = sourceImplementation<DeprecatedAnnotationDetector>().toIssue()
  }
}
