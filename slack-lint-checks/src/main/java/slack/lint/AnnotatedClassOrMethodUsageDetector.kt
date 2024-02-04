// Copyright (C) 2021 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint

import com.android.tools.lint.checks.AbstractAnnotationDetector
import com.android.tools.lint.detector.api.AnnotationInfo
import com.android.tools.lint.detector.api.AnnotationUsageInfo
import com.android.tools.lint.detector.api.AnnotationUsageType
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.android.tools.lint.detector.api.TextFormat
import org.jetbrains.uast.UElement

/**
 * Raises a warning whenever we use deprecated methods or classes. Generally used for keeping track
 * of health score.
 */
abstract class AnnotatedClassOrMethodUsageDetector :
  AbstractAnnotationDetector(), SourceCodeScanner {

  abstract val annotationNames: List<String>
  abstract val issue: Issue
  open val isEnabled: Boolean = true

  override fun applicableAnnotations(): List<String> =
    if (isEnabled) annotationNames else emptyList()

  override fun isApplicableAnnotationUsage(type: AnnotationUsageType): Boolean {
    // If it's not enabled, no need to scan further
    if (!isEnabled) return false
    @Suppress("DEPRECATION") // METHOD_CALL_CLASS doesn't have a replacement
    return type == AnnotationUsageType.METHOD_CALL ||
      type == AnnotationUsageType.METHOD_CALL_CLASS ||
      type == AnnotationUsageType.METHOD_CALL_PARAMETER
  }

  override fun visitAnnotationUsage(
    context: JavaContext,
    element: UElement,
    annotationInfo: AnnotationInfo,
    usageInfo: AnnotationUsageInfo,
  ) {
    if (isEnabled && applicableAnnotations().contains(annotationInfo.qualifiedName)) {
      val issueToReport = issue
      val location = context.getLocation(element)
      report(
        context,
        issueToReport,
        element,
        location,
        issueToReport.getBriefDescription(TextFormat.TEXT),
        null,
      )
    }
  }
}
