// Copyright (C) 2021 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint

import com.android.tools.lint.client.api.LintClient
import com.android.tools.lint.detector.api.AnnotationInfo
import com.android.tools.lint.detector.api.AnnotationUsageInfo
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.UastLintUtils
import org.jetbrains.uast.UElement
import slack.lint.util.Priorities
import slack.lint.util.sourceImplementation

private const val DEPRECATED_ANNOTATION_NAME_JAVA = "java.lang.Deprecated"
private const val DEPRECATED_ANNOTATION_NAME_KOTLIN = "kotlin.Deprecated"
private const val BRIEF_DESCRIPTION_PREFIX_DEFAULT = "This class or method"
private const val BRIEF_DESCRIPTION_SUFFIX = " is deprecated; consider using an alternative."

/**
 * Raises a warning whenever we use deprecated methods or classes. Generally used for keeping track
 * of health score.
 */
class DeprecatedAnnotationDetector : AnnotatedClassOrMethodUsageDetector() {

  override val annotationNames =
    listOf(DEPRECATED_ANNOTATION_NAME_KOTLIN, DEPRECATED_ANNOTATION_NAME_JAVA)
  override val issue = ISSUE_DEPRECATED_CALL

  // Only enable on CLI
  override val isEnabled: Boolean
    get() = !LintClient.isStudio

  override fun visitAnnotationUsage(
    context: JavaContext,
    element: UElement,
    annotationInfo: AnnotationInfo,
    usageInfo: AnnotationUsageInfo,
  ) {
    if (isEnabled && applicableAnnotations().contains(annotationInfo.qualifiedName)) {
      val issueToReport = issue
      val location = context.getLocation(element)
      val messagePrefix =
        usageInfo.referenced?.let(UastLintUtils.Companion::getQualifiedName)
          ?: BRIEF_DESCRIPTION_PREFIX_DEFAULT
      report(
        context,
        issueToReport,
        element,
        location,
        messagePrefix + BRIEF_DESCRIPTION_SUFFIX,
        null,
      )
    }
  }

  companion object {
    private fun Implementation.toIssue(): Issue {
      return Issue.create(
        "DeprecatedCall",
        BRIEF_DESCRIPTION_PREFIX_DEFAULT + BRIEF_DESCRIPTION_SUFFIX,
        "Using deprecated classes is not advised; please consider using an alternative.",
        Category.CORRECTNESS,
        Priorities.NORMAL,
        Severity.WARNING,
        this,
      )
    }

    val ISSUE_DEPRECATED_CALL = sourceImplementation<DeprecatedAnnotationDetector>().toIssue()
  }
}
