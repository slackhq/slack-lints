// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint.mocking

import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Severity
import com.intellij.psi.PsiClass
import slack.lint.util.MetadataJavaEvaluator
import slack.lint.util.sourceImplementation

/** A [MockDetector.TypeChecker] that checks for any mocking. */
object AnyMockDetector : MockDetector.TypeChecker {
  override val issue: Issue =
    Issue.create(
      "DoNotMockAnything",
      "Do not add new mocks.",
      """
        Mocking is almost always unnecessary and will make your tests more brittle. Use real instances \
        (if appropriate) or test fakes instead. This lint is a catch-all for mocking, and has been \
        enabled in this project to help prevent new mocking from being added.
      """,
      Category.CORRECTNESS,
      6,
      // Off by default though
      Severity.ERROR,
      sourceImplementation<MockDetector>(),
    ).setEnabledByDefault(false)

  override fun checkType(
    context: JavaContext,
    evaluator: MetadataJavaEvaluator,
    mockedType: PsiClass,
  ): MockDetector.Reason = MockDetector.Reason(mockedType, "Do not add new mocks.")
}
