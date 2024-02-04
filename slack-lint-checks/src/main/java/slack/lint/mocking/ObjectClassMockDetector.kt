// Copyright (C) 2021 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint.mocking

import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Severity
import com.intellij.psi.PsiClass
import slack.lint.util.MetadataJavaEvaluator
import slack.lint.util.sourceImplementation

/** A [MockDetector.TypeChecker] that checks for mocking Kotlin object classes. */
object ObjectClassMockDetector : MockDetector.TypeChecker {
  override val issue: Issue =
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
      sourceImplementation<MockDetector>(),
    )

  override val annotations: Set<String> = emptySet()

  override fun checkType(
    context: JavaContext,
    evaluator: MetadataJavaEvaluator,
    mockedType: PsiClass,
  ): MockDetector.Reason? {
    return if (evaluator.isObject(mockedType)) {
      MockDetector.Reason(
        mockedType,
        "'${mockedType.qualifiedName}' is an object, so mocking it should not be necessary",
      )
    } else {
      null
    }
  }
}
