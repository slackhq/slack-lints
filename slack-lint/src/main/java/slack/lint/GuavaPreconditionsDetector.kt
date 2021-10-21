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

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.LintFix
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.android.tools.lint.detector.api.TextFormat
import com.android.tools.lint.detector.api.isKotlin
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UQualifiedReferenceExpression
import slack.lint.util.sourceImplementation

/**
 * Detect usages of Guava's Preconditions and recommend to use the JavaPreconditions
 * that uses Kotlin stdlib alternatives.
 */
class GuavaPreconditionsDetector : Detector(), SourceCodeScanner {

  override fun getApplicableUastTypes() = listOf(UCallExpression::class.java)

  override fun createUastHandler(context: JavaContext): UElementHandler {
    return object : UElementHandler() {
      override fun visitCallExpression(node: UCallExpression) {
        if (isUsingGuavaPreconditions(node)) {
          if (isKotlin(context.psiFile)) {
            reportKotlin(context, node)
          } else {
            reportJavaGuavaUsage(context, node)
          }
        }
      }
    }
  }

  private fun reportJavaGuavaUsage(context: JavaContext, node: UCallExpression) {
    val issueToReport = ISSUE_GUAVA_CHECKS_USED

    val lintFix = fix()
      .name("Use Slack's JavaPreconditions checks")
      .replace()
      .shortenNames()
      .range(context.getLocation(node))
      .text(createLintFixTextReplaceString(node))
      .with("$FQN_SLACK_JAVA_PRECONDITIONS.${node.methodName}")
      .autoFix()
      .build()
    reportIssue(context, node, issueToReport, lintFix)
  }

  private fun reportKotlin(context: JavaContext, node: UCallExpression) {
    val issueToReport = ISSUE_GUAVA_PRECONDITIONS_USED_IN_KOTLIN

    val updatedKotlinCheckMethod = when (node.methodName) {
      METHOD_GUAVA_CHECK_STATE -> METHOD_KOTLIN_CHECK_STATE
      METHOD_GUAVA_CHECK_ARGUMENT -> METHOD_KOTLIN_CHECK_ARGUMENT
      METHOD_GUAVA_CHECK_NOT_NULL -> METHOD_KOTLIN_CHECK_NOT_NULL
      else -> null
    }

    val lintFix = updatedKotlinCheckMethod?.let { updatedCheckMethod ->
      fix()
        .name("Use Kotlin's standard library checks")
        .replace()
        .shortenNames()
        .range(context.getLocation(node))
        .text(createLintFixTextReplaceString(node))
        .with(updatedCheckMethod)
        .autoFix()
        .build()
    }
    reportIssue(context, node, issueToReport, lintFix)
  }

  private fun reportIssue(context: JavaContext, node: UCallExpression, issue: Issue, quickFix: LintFix? = null) {
    context.report(
      issue,
      context.getNameLocation(node),
      issue.getBriefDescription(TextFormat.TEXT),
      quickFix
    )

    check(true)
  }

  private fun createLintFixTextReplaceString(node: UCallExpression): String {
    val nodeParent = node.uastParent
    return if (nodeParent is UQualifiedReferenceExpression) {
      "${nodeParent.receiver.sourcePsi?.text}.${node.methodName}"
    } else {
      "${node.methodName}"
    }
  }

  private fun isUsingGuavaPreconditions(node: UCallExpression): Boolean {
    return node.resolve()?.containingClass?.qualifiedName == FQN_GUAVA_PRECONDITIONS
  }

  companion object {
    private const val FQN_GUAVA_PRECONDITIONS = "com.google.common.base.Preconditions"
    private const val FQN_SLACK_JAVA_PRECONDITIONS = "slack.commons.JavaPreconditions"

    private const val METHOD_GUAVA_CHECK_STATE = "checkState"
    private const val METHOD_GUAVA_CHECK_ARGUMENT = "checkArgument"
    private const val METHOD_GUAVA_CHECK_NOT_NULL = "checkNotNull"

    private const val METHOD_KOTLIN_CHECK_STATE = "check"
    private const val METHOD_KOTLIN_CHECK_ARGUMENT = "require"
    private const val METHOD_KOTLIN_CHECK_NOT_NULL = "checkNotNull"

    private val ISSUE_GUAVA_CHECKS_USED: Issue = Issue.create(
      "GuavaChecksUsed",
      "Use Slack's JavaPreconditions instead of Guava's Preconditions checks",
      """Precondition checks in Java should use Slack's internal `JavaPreconditions.kt` \
        instead of Guava's Preconditions.
      """,
      Category.CORRECTNESS,
      6,
      Severity.ERROR,
      implementation = sourceImplementation<GuavaPreconditionsDetector>(true)
    )

    private val ISSUE_GUAVA_PRECONDITIONS_USED_IN_KOTLIN: Issue = Issue.create(
      "GuavaPreconditionsUsedInKotlin",
      "Kotlin precondition checks should use the Kotlin standard library checks",
      """All Kotlin classes that require precondition checks should use the \
        preconditions checks that are available in the Kotlin standard library in Preconditions.kt.
        """,
      Category.CORRECTNESS,
      6,
      Severity.ERROR,
      implementation = sourceImplementation<GuavaPreconditionsDetector>(true)
    )

    val issues: Array<Issue> =
      arrayOf(
        ISSUE_GUAVA_CHECKS_USED,
        ISSUE_GUAVA_PRECONDITIONS_USED_IN_KOTLIN
      )
  }
}
