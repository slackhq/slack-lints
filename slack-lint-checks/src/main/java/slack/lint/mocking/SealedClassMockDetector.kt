// Copyright (C) 2021 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint.mocking

import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Severity
import com.intellij.psi.PsiClass
import com.intellij.psi.impl.light.LightElement
import slack.lint.util.MetadataJavaEvaluator
import slack.lint.util.sourceImplementation

/** A [MockDetector.TypeChecker] that checks for mocking Kotlin sealed classes. */
object SealedClassMockDetector : MockDetector.TypeChecker {
  override val issue: Issue =
    Issue.create(
      "DoNotMockSealedClass",
      "sealed classes have a restricted type hierarchy, use a subtype instead",
      """
      sealed classes have a restricted type hierarchy, so creating new unrestricted mocks \
      will break runtime expectations. Mockito also cannot mock sealed classes in Java 17+. \
      Use a subtype instead.
    """,
      Category.CORRECTNESS,
      6,
      Severity.ERROR,
      sourceImplementation<MockDetector>(),
    )

  override val annotations: Set<String> = emptySet()

  override fun checkType(
    context: JavaContext,
    evaluator: MetadataJavaEvaluator,
    mockedType: PsiClass,
  ): MockDetector.Reason? {
    // Check permitsList to cover Java 17 sealed types too
    return if (evaluator.isSealed(mockedType) || mockedType.hasPermitsList()) {
      MockDetector.Reason(
        mockedType,
        "'${mockedType.qualifiedName}' is a sealed type and has a restricted type hierarchy, use a subtype instead.",
      )
    } else {
      null
    }
  }

  private fun PsiClass.hasPermitsList(): Boolean {
    return if (this is LightElement) {
      // TODO https://issuetracker.google.com/issues/428697839
      false
    } else {
      permitsList != null
    }
  }
}
