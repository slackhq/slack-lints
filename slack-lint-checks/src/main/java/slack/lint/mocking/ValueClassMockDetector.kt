// Copyright (C) 2021 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint.mocking

import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Severity
import com.intellij.psi.PsiClass
import org.jetbrains.uast.UElement
import slack.lint.util.SlackJavaEvaluator
import slack.lint.util.isValueClass
import slack.lint.util.sourceImplementation

/** A [AbstractMockDetector] that checks for mocking Kotlin value classes. */
class ValueClassMockDetector : AbstractMockDetector() {
  companion object {
    val ISSUE: Issue =
      Issue.create(
        "DoNotMockValueClass",
        "value classes represent inlined types, so mocking them should not be necessary.",
        """
        value classes represent inlined types, so mocking them should not be necessary. \
        Construct a real instance of the class instead.
      """,
        Category.CORRECTNESS,
        6,
        Severity.ERROR,
        sourceImplementation<ValueClassMockDetector>()
      )
  }

  override val annotations: Set<String> = emptySet()

  override fun checkType(
    context: JavaContext,
    evaluator: SlackJavaEvaluator,
    mockedType: PsiClass
  ): Reason? {
    return if (evaluator.isValueClass(mockedType)) {
      Reason(
        mockedType,
        "value classes represent inlined types, so mocking them should not be necessary"
      )
    } else {
      null
    }
  }

  override fun report(
    context: JavaContext,
    mockedType: PsiClass,
    mockNode: UElement,
    reason: Reason
  ) {
    context.report(ISSUE, context.getLocation(mockNode), reason.reason)
  }
}
