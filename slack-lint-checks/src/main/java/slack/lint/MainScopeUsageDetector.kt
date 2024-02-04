// Copyright (C) 2021 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.LintFix
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.android.tools.lint.detector.api.TextFormat
import com.android.tools.lint.detector.api.isJava
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassType
import java.util.EnumSet
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UCallableReferenceExpression
import org.jetbrains.uast.UElement

/**
 * This is a [Detector] for detecting direct usages of Kotlin coroutines'
 * [kotlinx.coroutines.MainScope] helper function, as we want folks to use our
 * `slack.foundation.coroutines.android.MainScope` alternative.
 */
class MainScopeUsageDetector : Detector(), SourceCodeScanner {

  companion object {
    private val SCOPES =
      Implementation(MainScopeUsageDetector::class.java, EnumSet.of(Scope.JAVA_FILE))

    val ISSUE: Issue =
      Issue.create(
        "MainScopeUsage",
        "Use slack.foundation.coroutines.android.MainScope.",
        """
        Prefer using Slack's internal `MainScope` function, which supports `SlackDispatchers` and uses \
        Dispatchers.Main.immediate under the hood.
      """,
        Category.CORRECTNESS,
        6,
        Severity.ERROR,
        SCOPES,
      )

    private const val COROUTINE_SCOPE_CLASS = "kotlinx.coroutines.CoroutineScopeKt"
    private const val MAIN_SCOPE_FUNCTION = "MainScope"
  }

  override fun getApplicableUastTypes(): List<Class<out UElement>> =
    listOf(UCallExpression::class.java, UCallableReferenceExpression::class.java)

  override fun createUastHandler(context: JavaContext): UElementHandler? {
    // Only applicable on Kotlin files
    if (isJava(context.psiFile)) return null

    fun report(node: UElement) {
      context.report(
        ISSUE,
        context.getLocation(node),
        ISSUE.getBriefDescription(TextFormat.TEXT),
        LintFix.create()
          .replace()
          .name("Use slack.foundation.coroutines.android.MainScope")
          .text("MainScope(")
          .with("slack.foundation.coroutines.android.MainScope(")
          .autoFix()
          .build(),
      )
    }

    fun String?.isMainScope(): Boolean {
      return this == MAIN_SCOPE_FUNCTION
    }

    fun PsiClass?.isCoroutineScopeClass(): Boolean {
      if (this == null) return false
      return qualifiedName == COROUTINE_SCOPE_CLASS
    }

    return object : UElementHandler() {
      override fun visitCallExpression(node: UCallExpression) {
        if (node.methodName.isMainScope()) {
          val resolved = node.resolve() ?: return
          if (resolved.containingClass.isCoroutineScopeClass()) {
            report(node)
          }
        }
      }

      override fun visitCallableReferenceExpression(node: UCallableReferenceExpression) {
        if (node.callableName.isMainScope()) {
          val qualifierType = node.qualifierType ?: return
          if (qualifierType is PsiClassType && qualifierType.resolve().isCoroutineScopeClass())
            report(node)
        }
      }
    }
  }
}
