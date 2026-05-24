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

        // Verify the trailing lambda is a `ProducerScope` receiver lambda (the coroutines rx
        // factory shape). We inspect the resolved factory's `block` parameter type rather than the
        // lambda's own receiver type: the latter resolves to an error type whenever the factory's
        // type parameter can't be inferred from the lambda body (e.g. a lambda that never calls
        // send()), which is exactly the case this detector needs to flag.
        val blockParamType = node.resolve()?.parameterList?.parameters?.lastOrNull()?.type ?: return
        if (PROVIDER_SCOPE_FQN !in blockParamType.canonicalText) return

        var sendCalled = false

        val visitor =
          object : DataFlowAnalyzer(emptySet()) {
            override fun visitCallExpression(node: UCallExpression): Boolean {
              // If we find a nested factory method, return immediately and stop traversing this
              // code path.
              // Note: this factory will be validated by the UElementHandler above
              if (node.methodName in functionToIssue) return true

              if (node.hasProviderScopeReceiver() && node.methodName in REQUIRE_ONE_OF) {
                sendCalled = true
              }

              return false
            }
          }

        lambdaExpression.accept(visitor)

        if (!sendCalled) {
          context.report(
            issue,
            context.getLocation(node),
            "${node.methodName} does not call `send()` or `trySend()`",
          )
        }
      }
    }
  }

  private fun UCallExpression.hasProviderScopeReceiver(): Boolean =
    receiverType?.canonicalText?.startsWith(PROVIDER_SCOPE_FQN) == true

  internal companion object {
    private val REQUIRE_ONE_OF = setOf("send", "trySend")
    private const val PROVIDER_SCOPE_FQN = "kotlinx.coroutines.channels.ProducerScope"

    private val ISSUE_RX_OBSERVABLE_DOES_NOT_EMIT =
      Issue.create(
        id = "RxObservableDoesNotEmit",
        briefDescription = "RxObservable should call `send()` or `trySend()`",
        explanation =
          "If the rxObservable trailing lambda does not call `send()` or `trySend()`, the returned Observable will never emit!",
        category = Category.CORRECTNESS,
        priority = 2,
        severity = Severity.INFORMATIONAL,
        implementation = sourceImplementation<RxObservableEmitDetector>(),
      )

    private val ISSUE_RX_FLOWABLE_DOES_NOT_EMIT =
      Issue.create(
        id = "RxFlowableDoesNotEmit",
        briefDescription = "RxFlowable should call `send()` or `trySend()`",
        explanation =
          "If the rxFlowable trailing lambda does not call `send()` or `trySend()`, the returned Flowable will never emit!",
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
