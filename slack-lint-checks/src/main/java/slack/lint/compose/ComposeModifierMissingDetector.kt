// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint.compose

import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.TextFormat
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.psiUtil.isPublic
import slack.lint.compose.util.definedInInterface
import slack.lint.compose.util.emitsContent
import slack.lint.compose.util.isOverride
import slack.lint.compose.util.isPreview
import slack.lint.compose.util.modifierParameter
import slack.lint.compose.util.returnsValue
import slack.lint.util.Priorities
import slack.lint.util.sourceImplementation

class ComposeModifierMissingDetector : ComposableFunctionDetector() {

  companion object {

    val ISSUE =
      Issue.create(
          id = "ComposeModifierMissing",
          briefDescription =
            "This @Composable function emits content but doesn't have a modifier parameter.",
          explanation =
            """
              This @Composable function emits content but doesn't have a modifier parameter.\
              \
              See https://twitter.github.io/compose-rules/rules/#when-should-i-expose-modifier-parameters for more information.
            """
              .trimIndent(),
          category = Category.PRODUCTIVITY,
          priority = Priorities.NORMAL,
          severity = Severity.ERROR,
          implementation = sourceImplementation<ComposeModifierMissingDetector>()
        )
        .setOptions(listOf(PROVIDED_CONTENT_EMITTERS))
  }

  override fun visitComposable(context: JavaContext, function: KtFunction) {
    // We want to find all composable functions that:
    //  - are public
    //  - emit content
    //  - are not overridden or part of an interface
    //  - are not a @Preview composable
    if (
      !function.isPublic ||
        function.returnsValue ||
        function.isOverride ||
        function.definedInInterface ||
        function.isPreview
    ) {
      return
    }

    // If there is a modifier param, we bail
    if (function.modifierParameter != null) return

    // In case we didn't find any `modifier` parameters, we check if it emits content and report the
    // error if so.
    if (function.emitsContent(providedContentEmitters())) {
      context.report(
        ISSUE,
        function,
        context.getNameLocation(function),
        ISSUE.getBriefDescription(TextFormat.TEXT)
      )
    }
  }
}
