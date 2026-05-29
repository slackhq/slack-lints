// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint.exceptions

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import org.jetbrains.uast.UCatchClause
import org.jetbrains.uast.UElement
import org.jetbrains.uast.USimpleNameReferenceExpression
import org.jetbrains.uast.visitor.AbstractUastVisitor
import slack.lint.util.sourceImplementation

class SwallowedExceptionDetector : Detector(), SourceCodeScanner {

  override fun getApplicableUastTypes(): List<Class<out UElement>> =
    listOf(UCatchClause::class.java)

  override fun createUastHandler(context: JavaContext): UElementHandler {
    return object : UElementHandler() {
      override fun visitCatchClause(node: UCatchClause) {
        val params = node.parameters
        if (params.isEmpty()) return
        val paramName = params.first().name
        if (paramName == "_") return

        var isUsed = false
        node.body.accept(
          object : AbstractUastVisitor() {
            override fun visitSimpleNameReferenceExpression(
              node: USimpleNameReferenceExpression
            ): Boolean {
              if (node.identifier == paramName) {
                isUsed = true
              }
              return isUsed
            }
          }
        )
        if (!isUsed) {
          context.report(
            ISSUE,
            node as UElement,
            context.getLocation(params.first() as UElement),
            "Exception `$paramName` is caught but never used. Either use it or rename to `_`.",
          )
        }
      }
    }
  }

  companion object {
    val ISSUE =
      Issue.create(
        id = "SwallowedException",
        briefDescription = "Caught exception is not used",
        explanation =
          "An exception is caught but never referenced in the catch block. " +
            "This may hide bugs. Either log/rethrow the exception or rename the parameter to `_`.",
        category = Category.CORRECTNESS,
        priority = 6,
        severity = Severity.WARNING,
        implementation = sourceImplementation<SwallowedExceptionDetector>(),
      )
  }
}
