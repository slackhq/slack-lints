// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint.exceptions

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.android.tools.lint.detector.api.StringOption
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UQualifiedReferenceExpression
import org.jetbrains.uast.UThrowExpression
import org.jetbrains.uast.UastCallKind
import org.jetbrains.uast.getParentOfType
import slack.lint.util.OptionLoadingDetector
import slack.lint.util.StringSetLintOption
import slack.lint.util.sourceImplementation

private val DEFAULT_GENERIC_EXCEPTIONS =
  listOf("Error", "Exception", "RuntimeException", "Throwable")

class TooGenericExceptionThrownDetector(
  private val exceptionTypesOption: StringSetLintOption = StringSetLintOption(EXCEPTION_TYPES)
) : OptionLoadingDetector(exceptionTypesOption), SourceCodeScanner {

  override fun getApplicableUastTypes(): List<Class<out UElement>> =
    listOf(UCallExpression::class.java)

  override fun createUastHandler(context: JavaContext): UElementHandler {
    return object : UElementHandler() {
      override fun visitCallExpression(node: UCallExpression) {
        if (node.kind != UastCallKind.CONSTRUCTOR_CALL) return
        // Only the exception being thrown counts, not one built to pass as an argument.
        val throwExpression = node.getParentOfType<UThrowExpression>() ?: return
        if (!throwExpression.throws(node)) return

        val className = node.resolve()?.containingClass?.name ?: return
        if (className in exceptionTypesOption.value) {
          context.report(
            ISSUE,
            node,
            context.getLocation(node),
            "$className is too generic to throw, so throw a more specific exception",
          )
        }
      }
    }
  }

  /**
   * Whether [call] is the exception this throws. A qualified name like `java.lang.Exception()` is
   * wrapped in a reference expression, so the call is its selector rather than the thrown
   * expression itself.
   */
  private fun UThrowExpression.throws(call: UCallExpression): Boolean {
    val thrown = thrownExpression
    return thrown === call || (thrown as? UQualifiedReferenceExpression)?.selector === call
  }

  companion object {
    internal val EXCEPTION_TYPES =
      StringOption(
        "exception-types",
        "Comma-separated list of generic exception simple names that should not be thrown.",
        DEFAULT_GENERIC_EXCEPTIONS.joinToString(","),
        "Throwing these exceptions is flagged as too generic.",
      )

    val ISSUE =
      Issue.create(
          id = "TooGenericExceptionThrown",
          briefDescription = "Thrown exception type is too generic",
          explanation =
            "Throwing overly generic exceptions like `Exception` or `Throwable` " +
              "makes it impossible for callers to handle specific error conditions. " +
              "Throw a more specific exception type.",
          category = Category.CORRECTNESS,
          priority = 6,
          severity = Severity.WARNING,
          implementation = sourceImplementation<TooGenericExceptionThrownDetector>(),
        )
        .setOptions(listOf(EXCEPTION_TYPES))
  }
}
