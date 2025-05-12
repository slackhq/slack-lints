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
        val lambdaExpression = node.valueArguments.lastOrNull() as? ULambdaExpression ?: return
        val producerScopeParam = lambdaExpression.parameters.firstOrNull() ?: return
        
        // Verify the parameter is a ProducerScope
        if (!producerScopeParam.type.canonicalText.startsWith(PROVIDER_SCOPE_FQN)) return

        var sendCalled = false

        val visitor =
          object : DataFlowAnalyzer(emptySet()) {
            override fun visitCallExpression(node: UCallExpression): Boolean {
              // If we find a nested factory method, return immediately and stop traversing this code path.
              // Note: this factory will be validated by the UElementHandler above
              if (node.methodName in functionToIssue) return true

              return super.visitCallExpression(node)
                .also { if (node.matchesRequiredMethod() && node.methodName in REQUIRE_ONE_OF) sendCalled = true }
            }
          }

        lambdaExpression.accept(visitor)

        if (!sendCalled) {
          context.report(
            issue,
            context.getLocation(node),
            "${node.methodName} does not call send() or trySend()",
          )
        }
      }
    }
  }

  private fun UCallExpression.matchesRequiredMethod(): Boolean =
    receiverType?.canonicalText?.startsWith(PROVIDER_SCOPE_FQN) == true

  internal companion object {
    private val REQUIRE_ONE_OF = setOf("send", "trySend")
    private const val PROVIDER_SCOPE_FQN = "kotlin.coroutines.ProducerScope"

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
