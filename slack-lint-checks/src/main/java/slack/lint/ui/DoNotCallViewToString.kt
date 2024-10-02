// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint.ui

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.intellij.psi.util.InheritanceUtil
import org.jetbrains.uast.UCallExpression
import slack.lint.util.sourceImplementation

class DoNotCallViewToString : Detector(), SourceCodeScanner {
  override fun getApplicableUastTypes() = listOf(UCallExpression::class.java)

  override fun createUastHandler(context: JavaContext): UElementHandler {
    return object : UElementHandler() {
      override fun visitCallExpression(node: UCallExpression) {
        if (node.methodName != "toString") return
        val method = node.resolve() ?: return
        val containingClass = method.containingClass ?: return
        if (InheritanceUtil.isInheritor(containingClass, "android.view.View")) {
          context.report(
            ISSUE,
            node,
            context.getNameLocation(node),
            "Do not call `View.toString()`",
          )
        }
      }
    }
  }

  companion object {
    val ISSUE: Issue =
      Issue.Companion.create(
        "DoNotCallViewToString",
        "Do not use `View.toString()`",
        """
        `View.toString()` and its overrides can often print surprisingly detailed information about \
        the current view state, and has led to PII logging issues in the past.
      """,
        Category.Companion.SECURITY,
        9,
        Severity.ERROR,
        sourceImplementation<DoNotCallViewToString>(),
      )
  }
}
