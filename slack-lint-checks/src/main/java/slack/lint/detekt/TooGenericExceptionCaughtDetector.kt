// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint.detekt

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.android.tools.lint.detector.api.StringOption
import org.jetbrains.uast.UCatchClause
import org.jetbrains.uast.UElement
import slack.lint.util.OptionLoadingDetector
import slack.lint.util.StringSetLintOption
import slack.lint.util.sourceImplementation

private val DEFAULT_GENERIC_EXCEPTIONS =
  listOf(
    "ArrayIndexOutOfBoundsException",
    "Error",
    "Exception",
    "IllegalMonitorStateException",
    "IndexOutOfBoundsException",
    "NullPointerException",
    "RuntimeException",
    "Throwable",
  )

class TooGenericExceptionCaughtDetector(
  private val exceptionTypesOption: StringSetLintOption = StringSetLintOption(EXCEPTION_TYPES),
) : OptionLoadingDetector(exceptionTypesOption), SourceCodeScanner {

  override fun getApplicableUastTypes(): List<Class<out UElement>> =
    listOf(UCatchClause::class.java)

  override fun createUastHandler(context: JavaContext): UElementHandler {
    return object : UElementHandler() {
      override fun visitCatchClause(node: UCatchClause) {
        val types = exceptionTypesOption.value.ifEmpty { DEFAULT_GENERIC_EXCEPTIONS.toSet() }
        for (typeRef in node.typeReferences) {
          val typeName = typeRef.type.canonicalText.substringAfterLast('.')
          if (typeName in types) {
            context.report(
              ISSUE,
              node,
              context.getLocation(typeRef),
              "Caught exception type `$typeName` is too generic. Catch a more specific exception.",
            )
          }
        }
      }
    }
  }

  companion object {
    private val EXCEPTION_TYPES =
      StringOption(
        "exception-types",
        "Comma-separated list of generic exception simple names.",
        DEFAULT_GENERIC_EXCEPTIONS.joinToString(","),
        "Catching these exceptions is flagged as too generic.",
      )

    val ISSUE =
      Issue.create(
        id = "TooGenericExceptionCaught",
        briefDescription = "Caught exception type is too generic",
        explanation =
          "Catching overly generic exceptions like `Exception` or `Throwable` " +
            "can hide bugs and make error handling brittle. Catch more specific exception types.",
        category = Category.CORRECTNESS,
        priority = 6,
        severity = Severity.WARNING,
        implementation = sourceImplementation<TooGenericExceptionCaughtDetector>(),
      )
        .setOptions(listOf(EXCEPTION_TYPES))
  }
}
