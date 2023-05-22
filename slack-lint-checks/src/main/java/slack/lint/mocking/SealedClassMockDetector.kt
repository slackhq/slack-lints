// Copyright (C) 2021 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint.mocking

import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Severity
import com.intellij.psi.PsiClass
import org.jetbrains.uast.UElement
import slack.lint.util.sourceImplementation

/** A [AbstractMockDetector] that checks for mocking Kotlin sealed classes. */
class SealedClassMockDetector : AbstractMockDetector() {
  companion object {
    val ISSUE: Issue =
      Issue.create(
        "DoNotMockSealedClass",
        "sealed classes have a restricted type hierarchy, use a subtype instead",
        """
        sealed classes have a restricted type hierarchy, so creating new unrestricted mocks \
        will break runtime expectations. Use a subtype instead.
      """,
        Category.CORRECTNESS,
        6,
        Severity.ERROR,
        sourceImplementation<SealedClassMockDetector>()
      )
  }

  override val annotations: Set<String> = emptySet()

  override fun checkType(context: JavaContext, mockedType: PsiClass): Reason? {
    // Check permitsList to cover Java 17 sealed types too
    return if (context.evaluator.isSealed(mockedType) || mockedType.permitsList != null) {
      Reason(mockedType, "sealed classes have a restricted type hierarchy, use a subtype instead.")
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
