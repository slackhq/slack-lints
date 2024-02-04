// Copyright (C) 2021 Slack Technologies, LLC
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
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UFile
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.getContainingUFile
import org.jetbrains.uast.resolveToUElement
import org.jetbrains.uast.toUElementOfType
import slack.lint.util.sourceImplementation

class RestrictCallsToDetector : Detector(), SourceCodeScanner {

  companion object {
    val ISSUE: Issue =
      Issue.create(
        "RestrictCallsTo",
        "Methods annotated with @RestrictedCallsTo should only be called from the specified scope.",
        """
          This method is intended to only be called from the specified scope despite it being \
          public. This could be due to its use in an interface or similar. Overrides are still \
          ok.
      """,
        Category.CORRECTNESS,
        6,
        Severity.ERROR,
        sourceImplementation<RestrictCallsToDetector>(),
      )

    private const val RESTRICT_CALLS_TO_ANNOTATION = "slack.lint.annotations.RestrictCallsTo"
  }

  override fun getApplicableUastTypes() = listOf(UCallExpression::class.java)

  override fun createUastHandler(context: JavaContext) =
    object : UElementHandler() {

      @Suppress("UNUSED_VARIABLE") // Toe-hold for now until we support other scopes
      override fun visitCallExpression(node: UCallExpression) {
        val method = node.resolveToUElement() as? UMethod ?: return

        val (restrictCallsTo, annotatedMethod) =
          method
            .superMethodSequence()
            .mapNotNull { superMethod ->
              superMethod.findAnnotation(RESTRICT_CALLS_TO_ANNOTATION)?.let { it to superMethod }
            }
            .firstOrNull() ?: return

        val containingFile = annotatedMethod.getContainingUFile() ?: return
        val callingFile = node.getContainingUFile() ?: return
        if (!callingFile.isSameAs(containingFile)) {
          context.report(
            ISSUE,
            node,
            context.getLocation(node),
            ISSUE.getBriefDescription(TextFormat.TEXT),
          )
        }
      }

      private fun UMethod.superMethodSequence(): Sequence<UMethod> {
        return generateSequence(this) {
          if (context.evaluator.isOverride(it)) {
            // Note this doesn't try to check multiple interfaces, but we can fix that in the future
            // if it matters
            it.findSuperMethods()[0].toUElementOfType()
          } else {
            null
          }
        }
      }
    }

  // UFile isn't inherently comparable, so package and simple name are close enough for us.
  private fun UFile.isSameAs(other: UFile): Boolean {
    return this.packageName == other.packageName && this.sourcePsi.name == other.sourcePsi.name
  }
}
