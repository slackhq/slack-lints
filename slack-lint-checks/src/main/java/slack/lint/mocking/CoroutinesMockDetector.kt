// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint.mocking

import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Severity
import com.intellij.psi.PsiClass
import org.jetbrains.uast.UClass
import org.jetbrains.uast.kotlin.isKotlin
import org.jetbrains.uast.toUElementOfType
import slack.lint.util.MetadataJavaEvaluator
import slack.lint.util.sourceImplementation

/**
 * A [MockDetector.TypeChecker] that checks for mocking non-final types containing coroutine APIs.
 */
object CoroutinesMockDetector : MockDetector.TypeChecker {

  override val issue: Issue =
    Issue.create(
      "DoNotMockCoroutines",
      "coroutines should not be mocked",
      """
      Mockito has absolutely no concept of Kotlin coroutines. This means that if you mock a type that has suspend \
      functions or Flow-returning functions, you _will_ get strange and incomprehensible runtime failures \
      in tests that try to use them. Please use real instances or write fakes instead.
    """,
      Category.CORRECTNESS,
      6,
      Severity.ERROR,
      sourceImplementation<MockDetector>(),
    )

  override fun checkType(
    context: JavaContext,
    evaluator: MetadataJavaEvaluator,
    mockedType: PsiClass,
  ): MockDetector.Reason? {
    // Check if the type is an interface, and if so check it for suspend functions or Flow-returning
    // functions.
    if (mockedType.isInterface && isKotlin(mockedType.language)) {
      mockedType.toUElementOfType<UClass>()?.let { uClass ->
        for (method in uClass.methods) {
          if (evaluator.isSuspend(method)) {
            return MockDetector.Reason(
              mockedType,
              "Do not mock ${mockedType.name} as it has a suspend function '${method.name}'",
            )
          } else if (
            evaluator.extendsClass(
              evaluator.getTypeClass(method.returnType),
              "kotlinx.coroutines.flow.Flow",
            )
          ) {
            return MockDetector.Reason(
              mockedType,
              "Do not mock ${mockedType.name} as it has a Flow-returning function '${method.name}'",
            )
          }
        }
      }
    }
    return null
  }
}
