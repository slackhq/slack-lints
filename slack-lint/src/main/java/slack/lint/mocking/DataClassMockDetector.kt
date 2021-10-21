/*
 * Copyright (C) 2021 Slack Technologies, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package slack.lint.mocking

import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Severity
import com.intellij.psi.PsiClass
import org.jetbrains.uast.UElement
import slack.lint.util.sourceImplementation

/** A [AbstractMockDetector] that checks for mocking Kotlin data classes. */
class DataClassMockDetector : AbstractMockDetector() {
  companion object {
    val ISSUE: Issue = Issue.create(
      "DoNotMockDataClass",
      "data classes represent pure data classes, so mocking them should not be necessary.",
      """
        data classes represent pure data classes, so mocking them should not be necessary. \
        Construct a real instance of the class instead.
      """,
      Category.CORRECTNESS,
      6,
      Severity.ERROR,
      sourceImplementation<DataClassMockDetector>()
    )
  }

  override val annotations: Set<String> = emptySet()

  override fun checkType(context: JavaContext, mockedType: PsiClass): Reason? {
    return if (context.evaluator.isData(mockedType)) {
      Reason(
        mockedType,
        "data classes represent pure value classes, so mocking them should not be necessary"
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
    context.report(
      ISSUE,
      context.getLocation(mockNode),
      reason.reason
    )
  }
}
