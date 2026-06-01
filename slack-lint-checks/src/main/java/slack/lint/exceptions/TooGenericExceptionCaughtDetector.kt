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
import org.jetbrains.kotlin.psi.KtCatchClause
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

private const val DEFAULT_ALLOWED_EXCEPTION_NAME = "_|(ignore|expected).*"

class TooGenericExceptionCaughtDetector(
  private val exceptionTypesOption: StringSetLintOption = StringSetLintOption(EXCEPTION_TYPES)
) : OptionLoadingDetector(exceptionTypesOption), SourceCodeScanner {

  override fun getApplicableUastTypes(): List<Class<out UElement>> =
    listOf(UCatchClause::class.java)

  override fun createUastHandler(context: JavaContext): UElementHandler {
    // Read as a whole string, not a set: a regex may legitimately contain a comma.
    val allowedNamePattern =
      Regex(
        ALLOWED_EXCEPTION_NAME.getValue(context.configuration) ?: DEFAULT_ALLOWED_EXCEPTION_NAME
      )
    return object : UElementHandler() {
      override fun visitCatchClause(node: UCatchClause) {
        // A name like `_`, `ignored`, or `expected` says the throw was deliberately dropped.
        val paramName = (node.sourcePsi as? KtCatchClause)?.catchParameter?.name
        if (paramName != null && allowedNamePattern.matches(paramName)) return

        for (typeRef in node.typeReferences) {
          val typeName = typeRef.type.canonicalText.substringAfterLast('.')
          if (typeName in exceptionTypesOption.value) {
            context.report(
              ISSUE,
              node,
              context.getLocation(typeRef),
              "Caught exception type $typeName is too generic, so catch a more specific one",
            )
          }
        }
      }
    }
  }

  companion object {
    internal val EXCEPTION_TYPES =
      StringOption(
        "exception-types",
        "Comma-separated list of generic exception simple names.",
        DEFAULT_GENERIC_EXCEPTIONS.joinToString(","),
        "Catching these exceptions is flagged as too generic.",
      )

    internal val ALLOWED_EXCEPTION_NAME =
      StringOption(
        "allowed-exception-name-regex",
        "Regex for catch parameter names that are exempt from this check.",
        DEFAULT_ALLOWED_EXCEPTION_NAME,
        "Catch blocks whose parameter name matches this pattern are allowed.",
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
        .setOptions(listOf(EXCEPTION_TYPES, ALLOWED_EXCEPTION_NAME))
  }
}
