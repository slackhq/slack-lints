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

/** A [MockDetector.TypeChecker] that checks for mocking platform types. */
object PlatformTypeMockDetector : MockDetector.TypeChecker {
  private val platforms =
    setOf("android", "androidx", "java", "javax", "kotlin", "kotlinx", "scala")

  override val issue: Issue =
    Issue.create(
      "DoNotMockPlatformTypes",
      "platform types should not be mocked",
      """
      Platform types (i.e. classes in java.*, android.*, etc) should not be mocked. \
      Use a real instance or fake instead. Mocking platform types like these can lead \
      to surprising test failures due to mocks that actually behave differently than \
      real instances, especially when passed into real implementations that use them \
      and expect them to behave in a certain way. If using Robolectric, consider using \
      its shadow APIs.
    """,
      Category.CORRECTNESS,
      6,
      Severity.WARNING,
      sourceImplementation<MockDetector>()
    )

  override fun checkType(
    context: JavaContext,
    evaluator: MetadataJavaEvaluator,
    mockedType: PsiClass
  ): MockDetector.Reason? {
    val name = mockedType.qualifiedName ?: return null
    val isPlatformType = name.substringBefore('.') in platforms
    return if (isPlatformType) {
      MockDetector.Reason(mockedType, "platform type '$name' should not be mocked")
    } else {
      null
    }
  }
}
