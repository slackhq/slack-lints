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
import com.intellij.psi.PsiClass
import com.intellij.psi.util.InheritanceUtil.isInheritor
import kotlin.jvm.java
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.getContainingUClass
import slack.lint.util.implements
import slack.lint.util.sourceImplementation

/**
 * Lint detector that ensures `ItemDecoration` does not inflate a view. If the view contains
 * `TextView`, this textual information cannot be announced to screen readers by TalkBack.
 */
class ItemDecorationViewBindingDetector : Detector(), SourceCodeScanner {
  override fun getApplicableUastTypes() = listOf(UCallExpression::class.java)

  override fun createUastHandler(context: JavaContext): UElementHandler =
    object : UElementHandler() {

      override fun visitCallExpression(node: UCallExpression) {
        val containingClass = node.getContainingUClass() ?: return
        if (!containingClass.implements(ITEM_DECORATION_CLASS_NAME)) return

        val method = node.resolve() ?: return
        val methodClass = method.containingClass

        if (
          LAYOUT_INFLATER_METHOD_NAME == method.name && methodClass?.isViewBindingClass() == true
        ) {
          context.report(ISSUE, node, context.getNameLocation(node), ISSUE_DESCRIPTION)
        }
      }
    }

  private fun PsiClass.isViewBindingClass(): Boolean {
    return LAYOUT_INFLATER_PACKAGE_NAME == this.qualifiedName ||
      isInheritor(this, VIEW_BINDING_PACKAGE_NAME)
  }

  companion object {
    private const val ISSUE_ID = "InflationInItemDecoration"
    private const val ITEM_DECORATION_CLASS_NAME =
      "androidx.recyclerview.widget.RecyclerView.ItemDecoration"
    private const val LAYOUT_INFLATER_METHOD_NAME = "inflate"
    private const val LAYOUT_INFLATER_PACKAGE_NAME = "android.view.LayoutInflater"
    private const val VIEW_BINDING_PACKAGE_NAME = "androidx.viewbinding.ViewBinding"

    private const val ISSUE_BRIEF_DESCRIPTION = "Avoid inflating a view to display text"
    private const val ISSUE_DESCRIPTION =
      """
        ViewBinding should not be used in `ItemDecoration`. If an inflated view contains \
        meaningful textual information, it will not be visible to TalkBack. This means \
        that screen reader users will not be able to know what is on the screen.
      """

    val ISSUE =
      Issue.create(
        id = ISSUE_ID,
        briefDescription = ISSUE_BRIEF_DESCRIPTION,
        explanation = ISSUE_DESCRIPTION,
        category = Category.A11Y,
        priority = 10,
        severity = Severity.WARNING,
        implementation = sourceImplementation<ItemDecorationViewBindingDetector>(),
      )
  }
}
