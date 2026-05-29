// Copyright (C) 2025 Slack Technologies, LLC
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
import org.jetbrains.uast.UThrowExpression
import org.jetbrains.uast.getParentOfType
import slack.lint.util.OptionLoadingDetector
import slack.lint.util.StringSetLintOption
import slack.lint.util.sourceImplementation

private val DEFAULT_EXCEPTION_TYPES =
  listOf(
    "ArrayIndexOutOfBoundsException",
    "Exception",
    "IllegalArgumentException",
    "IllegalMonitorStateException",
    "IllegalStateException",
    "IndexOutOfBoundsException",
    "NullPointerException",
    "RuntimeException",
    "Throwable",
  )

class ThrowingExceptionsWithoutMessageDetector(
  private val exceptionTypesOption: StringSetLintOption = StringSetLintOption(EXCEPTION_TYPES)
) : OptionLoadingDetector(exceptionTypesOption), SourceCodeScanner {

  override fun getApplicableUastTypes(): List<Class<out UElement>> =
    listOf(UCallExpression::class.java)

  override fun createUastHandler(context: JavaContext): UElementHandler {
    return object : UElementHandler() {
      override fun visitCallExpression(node: UCallExpression) {
        if (node.kind != org.jetbrains.uast.UastCallKind.CONSTRUCTOR_CALL) return
        val throwExpression = node.getParentOfType<UThrowExpression>() ?: return
        if (throwExpression.thrownExpression != node) return

        val method = node.resolve() ?: return
        val className = method.containingClass?.name ?: return
        val types = exceptionTypesOption.value.ifEmpty { DEFAULT_EXCEPTION_TYPES.toSet() }
        if (className !in types) return

        if (node.valueArguments.isEmpty()) {
          context.report(
            ISSUE,
            node,
            context.getLocation(node),
            "`$className` thrown without a message or cause argument.",
          )
        }
      }
    }
  }

  companion object {
    private val EXCEPTION_TYPES =
      StringOption(
        "exception-types",
        "Comma-separated list of exception type simple names to check.",
        DEFAULT_EXCEPTION_TYPES.joinToString(","),
        "Exceptions of these types must include a message or cause when thrown.",
      )

    val ISSUE =
      Issue.create(
          id = "ThrowingExceptionsWithoutMessageOrCause",
          briefDescription = "Exception thrown without a message or cause",
          explanation =
            "Exceptions should always include a descriptive message or a cause " +
              "to aid in debugging. Throwing without arguments makes it harder to diagnose issues.",
          category = Category.CORRECTNESS,
          priority = 6,
          severity = Severity.WARNING,
          implementation = sourceImplementation<ThrowingExceptionsWithoutMessageDetector>(),
        )
        .setOptions(listOf(EXCEPTION_TYPES))
  }
}
