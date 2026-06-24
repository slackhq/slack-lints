// Copyright (C) 2021 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint.mocking

import com.android.tools.lint.client.api.JavaEvaluator
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Severity
import com.intellij.psi.PsiClass
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UElement
import org.jetbrains.uast.toUElementOfType
import slack.lint.util.isValueClass
import slack.lint.util.sourceImplementation

/** A [MockDetector.TypeChecker] that checks for mocking Kotlin value classes. */
// TODO not currently enabled because of https://issuetracker.google.com/issues/283715187
object ValueClassMockDetector : MockDetector.TypeChecker {
  override val issue: Issue =
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
      sourceImplementation<MockDetector>(),
    )

  override val annotations: Set<String> = emptySet()

  override fun checkType(
    context: JavaContext,
    useSiteElement: UElement,
    evaluator: JavaEvaluator,
    mockedType: PsiClass,
  ): MockDetector.Reason? {
    val uMockedType = mockedType.toUElementOfType<UClass>() ?: return null
    return if (uMockedType.isValueClass(evaluator, useSiteElement.sourcePsi as? KtElement)) {
      MockDetector.Reason(
        mockedType,
        "'${mockedType.qualifiedName}' is a value class using inlined types, so mocking it should not be necessary",
      )
    } else {
      null
    }
  }
}
