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
import org.jetbrains.uast.UCatchClause
import org.jetbrains.uast.UElement
import org.jetbrains.uast.USimpleNameReferenceExpression
import org.jetbrains.uast.UThrowExpression
import org.jetbrains.uast.visitor.AbstractUastVisitor
import slack.lint.util.OptionLoadingDetector
import slack.lint.util.StringSetLintOption
import slack.lint.util.sourceImplementation

private val DEFAULT_IGNORED_EXCEPTION_TYPES =
  listOf("InterruptedException", "MalformedURLException", "NumberFormatException", "ParseException")

class SwallowedExceptionDetector(
  private val ignoredTypesOption: StringSetLintOption = StringSetLintOption(IGNORED_EXCEPTION_TYPES)
) : OptionLoadingDetector(ignoredTypesOption), SourceCodeScanner {

  override fun getApplicableUastTypes(): List<Class<out UElement>> =
    listOf(UCatchClause::class.java)

  override fun createUastHandler(context: JavaContext): UElementHandler {
    return object : UElementHandler() {
      override fun visitCatchClause(node: UCatchClause) {
        val params = node.parameters
        if (params.isEmpty()) return
        val paramName = params.first().name
        if (paramName == "_") return

        // Allow catching exceptions whose type is in the ignored list (e.g. InterruptedException),
        // matching detekt's ignoredExceptionTypes default.
        val ignoredTypes =
          ignoredTypesOption.value.ifEmpty { DEFAULT_IGNORED_EXCEPTION_TYPES.toSet() }
        val caughtTypes = node.typeReferences.map { it.type.canonicalText.substringAfterLast('.') }
        if (caughtTypes.any { it in ignoredTypes }) return

        var isUsed = false
        var rethrows = false
        node.body.accept(
          object : AbstractUastVisitor() {
            override fun visitSimpleNameReferenceExpression(
              node: USimpleNameReferenceExpression
            ): Boolean {
              if (node.identifier == paramName) {
                isUsed = true
              }
              return false
            }

            override fun visitThrowExpression(node: UThrowExpression): Boolean {
              // A catch block that throws (any exception) is handling, not swallowing, the error.
              rethrows = true
              return false
            }
          }
        )
        if (!isUsed && !rethrows) {
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
    private val IGNORED_EXCEPTION_TYPES =
      StringOption(
        "ignored-exception-types",
        "Comma-separated list of exception simple names that may be caught without being used.",
        DEFAULT_IGNORED_EXCEPTION_TYPES.joinToString(","),
        "Catching these exceptions without using them is not flagged.",
      )

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
        .setOptions(listOf(IGNORED_EXCEPTION_TYPES))
  }
}
