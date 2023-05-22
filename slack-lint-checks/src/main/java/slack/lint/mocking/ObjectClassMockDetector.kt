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
import slack.lint.util.isObject
import slack.lint.util.sourceImplementation

/** A [AbstractMockDetector] that checks for mocking Kotlin object classes. */
class ObjectClassMockDetector : AbstractMockDetector() {
  companion object {
    val ISSUE: Issue =
      Issue.create(
        "DoNotMockObjectClass",
        "object classes are singletons, so mocking them should not be necessary",
        """
        object classes are global singletons, so mocking them should not be necessary. \
        Use the object instance instead.
      """,
        Category.CORRECTNESS,
        6,
        Severity.ERROR,
        sourceImplementation<ObjectClassMockDetector>()
      )
  }

  override val annotations: Set<String> = emptySet()

  override fun checkType(context: JavaContext, mockedType: PsiClass): Reason? {
    return if (mockedType.toUElementOfType<UClass>()?.isObject == true) {
      Reason(mockedType, "object classes are singletons, so mocking them should not be necessary")
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
