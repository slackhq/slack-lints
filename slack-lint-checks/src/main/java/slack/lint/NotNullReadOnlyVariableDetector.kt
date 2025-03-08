// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.ULiteralExpression
import org.jetbrains.uast.ULocalVariable
import org.jetbrains.uast.UVariable
import org.jetbrains.uast.kotlin.isKotlin
import slack.lint.util.sourceImplementation

class NotNullReadOnlyVariableDetector : Detector(), SourceCodeScanner {
  override fun getApplicableUastTypes() = listOf(ULocalVariable::class.java, UVariable::class.java)

  override fun createUastHandler(context: JavaContext): UElementHandler? {
    if (!isKotlin(context.uastFile?.lang)) return null

    return object : UElementHandler() {

      fun isNullInitializedForReadOnlyVariable(node: UVariable): Boolean {
        val uastInitializer = node.uastInitializer

        val isNullInitialized = uastInitializer is ULiteralExpression && uastInitializer.isNull
        val isReadOnlyVariable = !node.isWritable

        return isNullInitialized && isReadOnlyVariable
      }

      fun report(node: UExpression?) {
        context.report(
          ISSUE,
          context.getLocation(node),
          "Avoid initializing read-only variable with null",
        )
      }

      override fun visitLocalVariable(node: ULocalVariable) {
        if (isNullInitializedForReadOnlyVariable(node)) {
          report(node.uastInitializer)
        }
      }

      override fun visitVariable(node: UVariable) {
        if (isNullInitializedForReadOnlyVariable(node)) {
          report(node.uastInitializer)
        }
      }
    }
  }

  companion object {
    val ISSUE: Issue =
      Issue.create(
        "AvoidNullInitializationForReadOnlyVariables",
        "Avoid initializing read-only variable with null in Kotlin",
        """
                    Avoid unnecessary `null` initialization for read-only variables, as they can never be reassigned. \
                    Assigning null explicitly does not provide any real benefit and may mislead readers into thinking the value could change later. \
                    If the variable needs to be modified later, it's better to use `var` instead of `val`, or consider using `lateinit var` if it is guaranteed to be initialized before use.
                    """,
        Category.CORRECTNESS,
        6,
        Severity.WARNING,
        sourceImplementation<NotNullReadOnlyVariableDetector>(),
      )
  }
}
