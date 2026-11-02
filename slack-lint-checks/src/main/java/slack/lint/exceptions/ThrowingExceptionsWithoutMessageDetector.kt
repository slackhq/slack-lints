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
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UastCallKind
import slack.lint.util.OptionLoadingDetector
import slack.lint.util.StringSetLintOption
import slack.lint.util.sourceImplementation

class ThrowingExceptionsWithoutMessageDetector(
  private val exceptionTypesOption: StringSetLintOption = StringSetLintOption(EXCEPTION_TYPES)
) : OptionLoadingDetector(exceptionTypesOption), SourceCodeScanner {

  override fun getApplicableUastTypes(): List<Class<out UElement>> =
    listOf(UCallExpression::class.java)

  override fun createUastHandler(context: JavaContext): UElementHandler {
    return object : UElementHandler() {
      override fun visitCallExpression(node: UCallExpression) {
        if (node.kind != UastCallKind.CONSTRUCTOR_CALL) return
        if (node.valueArguments.isNotEmpty()) return
        // A supertype call like `class Foo : Exception()` is also a constructor call, but a
        // subclass declaring its own message is fine.
        if (node.sourcePsi !is KtCallExpression) return

        val className = node.resolve()?.containingClass?.name ?: return
        if (className !in exceptionTypesOption.value) return

        context.report(
          ISSUE,
          node,
          context.getLocation(node),
          "$className is created without a message or cause",
        )
      }
    }
  }

  companion object {
    private val DEFAULT_EXCEPTION_TYPES =
      listOf(
        "ArrayIndexOutOfBoundsException",
        "Exception",
        "IllegalArgumentException",
        "IllegalMonitorStateException",
        "IllegalStateException",
        "IndexOutOfBoundsException",
        "IOException",
        "NullPointerException",
        "RuntimeException",
        "Throwable",
      )

    internal val EXCEPTION_TYPES =
      StringOption(
        "exception-types",
        "Comma-separated list of exception type simple names to check.",
        DEFAULT_EXCEPTION_TYPES.joinToString(","),
        "Exceptions of these types must include a message or cause when created.",
      )

    val ISSUE =
      Issue.create(
          id = "ThrowingExceptionsWithoutMessageOrCause",
          briefDescription = "Exception created without a message or cause",
          explanation =
            "Calling an exception's no-argument constructor leaves whoever reads the crash " +
              "with nothing to work with. Pass a message describing what went wrong, or the " +
              "underlying cause.",
          category = Category.CORRECTNESS,
          priority = 6,
          severity = Severity.WARNING,
          implementation = sourceImplementation<ThrowingExceptionsWithoutMessageDetector>(),
        )
        .setOptions(listOf(EXCEPTION_TYPES))
  }
}
