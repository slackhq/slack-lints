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
import com.android.tools.lint.detector.api.AnnotationUsageType
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.UastLintUtils
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.UAnnotation
import org.jetbrains.uast.UElement
import slack.lint.util.Priorities
import slack.lint.util.sourceImplementation

private const val DEPRECATED_ANNOTATION_NAME_JAVA = "java.lang.Deprecated"
private const val DEPRECATED_ANNOTATION_NAME_KOTLIN = "kotlin.Deprecated"
private const val BRIEF_DESCRIPTION_PREFIX_DEFAULT = "This class or method"
private const val BRIEF_DESCRIPTION_SUFFIX = " is deprecated; consider using an alternative."

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
            val messagePrefix = referenced?.let(UastLintUtils.Companion::getQualifiedName)
                    ?: BRIEF_DESCRIPTION_PREFIX_DEFAULT
            report(
                    context,
                    issueToReport,
                    usage,
                    location,
                    messagePrefix + BRIEF_DESCRIPTION_SUFFIX,
                    null
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
                    this
            )
        }

        val ISSUE_DEPRECATED_CALL = sourceImplementation<DeprecatedAnnotationDetector>().toIssue()
    }
}
