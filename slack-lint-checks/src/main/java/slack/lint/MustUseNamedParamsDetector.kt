// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.android.tools.lint.detector.api.TextFormat
import com.android.tools.lint.detector.api.isJava
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.psi.KtValueArgumentList
import org.jetbrains.kotlin.psi.KtValueArgumentName
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import org.jetbrains.uast.UCallExpression
import slack.lint.util.sourceImplementation

class MustUseNamedParamsDetector : Detector(), SourceCodeScanner {

  override fun getApplicableUastTypes() = listOf(UCallExpression::class.java)

  override fun createUastHandler(context: JavaContext): UElementHandler {
    return object : UElementHandler() {
      override fun visitCallExpression(node: UCallExpression) {
        val method = node.resolve() ?: return

        // Java doesn't have named parameters.
        if (isJava(method.language)) return

        if (method.hasAnnotation("slack.lint.annotations.MustUseNamedParams")) {
          val areAllNamed =
            node.sourcePsi!!
              .getChildOfType<KtValueArgumentList>()!!
              .children
              .filterIsInstance<KtValueArgument>()
              .all { it.getChildOfType<KtValueArgumentName>() != null }

          if (!areAllNamed) {
            context.report(
              ISSUE,
              node,
              context.getLocation(node),
              ISSUE.getBriefDescription(TextFormat.TEXT),
            )
          }
        }
      }
    }
  }

  companion object {
    val ISSUE: Issue =
      Issue.create(
        "MustUseNamedParams",
        "Calls to @MustUseNamedParams-annotated methods must name all parameters.",
        "Calls to @MustUseNamedParams-annotated methods must name all parameters.",
        Category.CORRECTNESS,
        9,
        Severity.ERROR,
        sourceImplementation<MustUseNamedParamsDetector>(),
      )
  }
}
