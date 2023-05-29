// Copyright (C) 2021 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint.mocking

import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.isJava
import com.intellij.psi.PsiClass
import org.jetbrains.uast.UElement
import slack.lint.util.MetadataJavaEvaluator
import slack.lint.util.sourceImplementation

/** A [AbstractMockDetector] that checks for mocking record classes. */
class RecordClassMockDetector : AbstractMockDetector() {
  companion object {
    val ISSUE: Issue =
      Issue.create(
        "DoNotMockRecordClass",
        "record classes represent pure data classes, so mocking them should not be necessary.",
        """
        record classes represent pure data classes, so mocking them should not be necessary. \
        Construct a real instance of the class instead.
      """,
        Category.CORRECTNESS,
        6,
        Severity.ERROR,
        sourceImplementation<RecordClassMockDetector>()
      )
  }

  override val annotations: Set<String> = emptySet()

  override fun checkType(
          context: JavaContext,
          evaluator: MetadataJavaEvaluator,
          mockedType: PsiClass
  ): Reason? {
    val isRecord =
      if (isJava(mockedType.language)) {
        // Java
        mockedType.isRecord
      } else {
        // Kotlin
        evaluator.isData(mockedType) && mockedType.hasAnnotation("kotlin.jvm.JvmRecord")
      }
    return if (isRecord) {
      Reason(
        mockedType,
        "record classes represent pure value classes, so mocking them should not be necessary"
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
