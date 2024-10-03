// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import kotlin.jvm.java
import org.jetbrains.uast.UPostfixExpression
import org.jetbrains.uast.kotlin.isKotlin
import slack.lint.util.sourceImplementation

class NotNullOperatorDetector : Detector(), SourceCodeScanner {
  override fun getApplicableUastTypes() = listOf(UPostfixExpression::class.java)

  override fun createUastHandler(context: JavaContext): UElementHandler? {
    if (!isKotlin(context.uastFile?.lang)) return null

    return object : UElementHandler() {
      override fun visitPostfixExpression(node: UPostfixExpression) {
        if (node.operator.text == "!!") {
          // Report a warning for the usage of `!!`
          context.report(
            ISSUE,
            node,
            context.getLocation(node.operatorIdentifier ?: node),
            "Avoid using the `!!` operator",
          )
        }
      }
    }
  }

  companion object {
    val ISSUE: Issue =
      Issue.create(
        "AvoidUsingNotNullOperator",
        "Avoid using the !! operator in Kotlin",
        """
        The `!!` operator is a not-null assertion in Kotlin that will lead to a \
        `NullPointerException` if the value is null. It's better to use safe \
        null-handling mechanisms like `?.`, `?:`, `?.let`, etc.
        """,
        Category.CORRECTNESS,
        6,
        Severity.WARNING,
        sourceImplementation<NotNullOperatorDetector>(),
      )
  }
}
