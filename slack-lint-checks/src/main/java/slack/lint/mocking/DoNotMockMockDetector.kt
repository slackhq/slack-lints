// Copyright (C) 2021 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint.mocking

import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Severity
import com.intellij.psi.PsiClass
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UElement
import org.jetbrains.uast.toUElementOfType
import slack.lint.util.MetadataJavaEvaluator
import slack.lint.util.sourceImplementation

/** A [AbstractMockDetector] that checks for mocking `@DoNotMock`-annotated classes. */
class DoNotMockMockDetector : AbstractMockDetector() {
  companion object {
    val ISSUE: Issue =
      Issue.create(
        "DoNotMock",
        "<Never used>", // We always compute the brief description.
        """
        Do not mock classes annotated with `@DoNotMock`, as they are explicitly asking not to be \
        mocked in favor of better options (test fakes, etc). These types should define \
        explanations/alternatives in their annotation.
      """,
        Category.CORRECTNESS,
        6,
        Severity.ERROR,
        sourceImplementation<DoNotMockMockDetector>()
      )

    private const val FQCN_SLACK_DNM = "slack.lint.annotations.DoNotMock"
    private const val FQCN_EP_DNM = "com.google.errorprone.annotations.DoNotMock"
  }

  override fun checkType(
    context: JavaContext,
    evaluator: MetadataJavaEvaluator,
    mockedType: PsiClass
  ): Reason? {
    val uMockedType = mockedType.toUElementOfType<UClass>() ?: return null
    val doNotMockAnnotation =
      uMockedType.findAnnotation(FQCN_SLACK_DNM)
        ?: uMockedType.findAnnotation(FQCN_EP_DNM) ?: return null

    val messagePrefix = "Do not mock ${mockedType.name}"
    val suffix = doNotMockAnnotation.findAttributeValue("value")?.evaluate() as String?
    val message =
      if (suffix == null) {
        messagePrefix
      } else {
        "$messagePrefix: $suffix"
      }
    return Reason(mockedType, message)
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
