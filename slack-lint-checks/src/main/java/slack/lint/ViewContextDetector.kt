// Copyright (C) 2021 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint

import com.android.tools.lint.detector.api.Category.Companion.CORRECTNESS
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.android.tools.lint.detector.api.TextFormat
import com.intellij.psi.PsiMethod
import com.intellij.psi.impl.compiled.ClsClassImpl
import org.jetbrains.uast.UBinaryExpressionWithType
import org.jetbrains.uast.UCallExpression
import slack.lint.util.sourceImplementation

/**
 * This [Detector] scans Java files for calls to methods named "getContext". It then checks if the
 * object being called is an `android.view.View` and if they are casting the returned
 * `android.content.Context` to `android.app.Activity`. If so, an [Issue] with id
 * `CastingViewContextToActivity` is reported.
 */
@Suppress("UnstableApiUsage")
class ViewContextDetector : Detector(), SourceCodeScanner {
  override fun getApplicableMethodNames(): List<String> {
    return listOf("getContext")
  }

  override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
    if (
      method.parent is ClsClassImpl &&
        "android.view.View" == (method.parent as ClsClassImpl).qualifiedName
    ) {
      // Check if the parent expression is a cast expression
      var parent = node.uastParent
      if (parent != null) {
        // Check if it's called as 'view.getContext()'
        if (parent.uastParent is UBinaryExpressionWithType) {
          parent = parent.uastParent
        }
        if (parent is UBinaryExpressionWithType) {
          // See if it is being cast to android.App.Activity
          if ("android.app.Activity" == parent.type.canonicalText) {
            // report an issue
            context.report(
              issue = ISSUE_VIEW_CONTEXT_CAST,
              scope = node,
              location = context.getLocation(parent),
              message = ISSUE_VIEW_CONTEXT_CAST.getBriefDescription(TextFormat.TEXT)
            )
          }
        }
      }
    }
  }

  companion object {
    val ISSUE_VIEW_CONTEXT_CAST: Issue =
      Issue.create(
        "CastingViewContextToActivity",
        "Unsafe cast of `Context` to `Activity`",
        """`View.getContext()` is not guaranteed to return an `Activity` and can often \
        return a `ContextWrapper` instead resulting in a `ClassCastException`. Instead, use \
        `UiUtils.getActivityFromView()`.
        """,
        CORRECTNESS,
        9,
        Severity.ERROR,
        sourceImplementation<ViewContextDetector>()
      )

    val issues: List<Issue>
      get() = listOf(ISSUE_VIEW_CONTEXT_CAST)
  }
}
