// Copyright (C) 2021 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint

import com.android.tools.lint.checks.AbstractAnnotationDetector
import com.android.tools.lint.detector.api.AnnotationUsageType
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.android.tools.lint.detector.api.TextFormat
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.UAnnotation
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
    return type == AnnotationUsageType.METHOD_CALL ||
      type == AnnotationUsageType.METHOD_CALL_CLASS ||
      type == AnnotationUsageType.METHOD_CALL_PARAMETER
  }

  override fun visitAnnotationUsage(
    context: JavaContext,
    usage: UElement,
    type: AnnotationUsageType,
    annotation: UAnnotation,
    qualifiedName: String,
    method: PsiMethod?,
    referenced: PsiElement?,
    annotations: List<UAnnotation>,
    allMemberAnnotations: List<UAnnotation>,
    allClassAnnotations: List<UAnnotation>,
    allPackageAnnotations: List<UAnnotation>
  ) {
    if (isEnabled && applicableAnnotations().contains(qualifiedName)) {
      val issueToReport = issue
      val location = context.getLocation(usage)
      report(
        context,
        issueToReport,
        usage,
        location,
        issueToReport.getBriefDescription(TextFormat.TEXT),
        null
      )
    }
  }
}
