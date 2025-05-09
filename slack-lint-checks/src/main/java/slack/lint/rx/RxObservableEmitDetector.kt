// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint.rx

import com.android.tools.lint.checks.DataFlowAnalyzer
import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.ULambdaExpression
import slack.lint.util.sourceImplementation

class RxObservableEmitDetector : Detector(), SourceCodeScanner {
  override fun getApplicableUastTypes() = listOf(UCallExpression::class.java)

  override fun createUastHandler(context: JavaContext): UElementHandler {
    return object : UElementHandler() {
      override fun visitCallExpression(node: UCallExpression) {
        val issue = functionToIssue[node.methodName] ?: return
        val lambdaExpression =
          requireNotNull(node.valueArguments.lastOrNull() as? ULambdaExpression) {
            "Missing trailing lambda!"
          }

        var sendCalled = false

        val visitor =
          object : DataFlowAnalyzer(emptySet()) {
            override fun visitCallExpression(node: UCallExpression): Boolean {
              if (node.methodIdentifier?.name in REQUIRE_ONE_OF) {
                sendCalled = true
              }
              return super.visitCallExpression(node)
            }
          }

        lambdaExpression.accept(visitor)

        if (sendCalled) return

        context.report(
          issue,
          context.getLocation(node),
          "${node.methodName} does not call send() or trySend()",
        )
      }
    }
  }

  internal companion object {
    private val REQUIRE_ONE_OF = setOf("send", "trySend")

    private val ISSUE_RX_OBSERVABLE_DOES_NOT_EMIT =
      Issue.create(
        id = "RxObservableDoesNotEmit",
        briefDescription = "rxObservable should call send() or trySend()",
        explanation =
          "If the rxObservable trailing lambda does not call send() or trySend(), the returned Observable will never emit!",
        category = Category.CORRECTNESS,
        priority = 2,
        severity = Severity.INFORMATIONAL,
        implementation = sourceImplementation<RxObservableEmitDetector>(),
      )

    private val ISSUE_RX_FLOWABLE_DOES_NOT_EMIT =
      Issue.create(
        id = "RxFlowableDoesNotEmit",
        briefDescription = "rxFlowable should call send() or trySend()",
        explanation =
          "If the rxFlowable trailing lambda does not call send() or trySend(), the returned Flowable will never emit!",
        category = Category.CORRECTNESS,
        priority = 2,
        severity = Severity.INFORMATIONAL,
        implementation = sourceImplementation<RxObservableEmitDetector>(),
      )

    val issues = listOf(ISSUE_RX_OBSERVABLE_DOES_NOT_EMIT, ISSUE_RX_FLOWABLE_DOES_NOT_EMIT)

    private val functionToIssue =
      mapOf(
        "rxObservable" to ISSUE_RX_OBSERVABLE_DOES_NOT_EMIT,
        "rxFlowable" to ISSUE_RX_FLOWABLE_DOES_NOT_EMIT,
      )
  }
}
