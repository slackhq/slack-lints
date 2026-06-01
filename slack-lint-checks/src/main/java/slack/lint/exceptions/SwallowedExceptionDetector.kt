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
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtCatchClause
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtThrowExpression
import org.jetbrains.kotlin.psi.psiUtil.anyDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.psi.psiUtil.findDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedExpressionForReceiverOrThis
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.uast.UCatchClause
import org.jetbrains.uast.UElement
import slack.lint.util.OptionLoadingDetector
import slack.lint.util.StringSetLintOption
import slack.lint.util.sourceImplementation

class SwallowedExceptionDetector(
  private val ignoredTypesOption: StringSetLintOption = StringSetLintOption(IGNORED_EXCEPTION_TYPES)
) : OptionLoadingDetector(ignoredTypesOption), SourceCodeScanner {

  override fun getApplicableUastTypes(): List<Class<out UElement>> =
    listOf(UCatchClause::class.java)

  override fun createUastHandler(context: JavaContext): UElementHandler {
    // Read as a whole string, not a set: a regex may legitimately contain a comma.
    val allowedNameRegex =
      Regex(
        ALLOWED_EXCEPTION_NAME.getValue(context.configuration) ?: DEFAULT_ALLOWED_EXCEPTION_NAME
      )
    return object : UElementHandler() {
      override fun visitCatchClause(node: UCatchClause) {
        val catchClause = node.sourcePsi as? KtCatchClause ?: return
        val catchParameter = catchClause.catchParameter ?: return
        val ignoredTypes = ignoredTypesOption.value

        val exceptionType = catchParameter.typeReference?.text
        if (ignoredTypes.any { exceptionType?.contains(it, ignoreCase = true) == true }) return
        if (allowedNameRegex.matches(catchParameter.nameIdentifier?.text.orEmpty())) return
        if (!isSwallowedOrUnused(catchClause, ignoredTypes)) return

        context.report(
          ISSUE,
          node,
          context.getLocation(catchParameter),
          "The caught exception is swallowed, so the original exception could be lost",
        )
      }
    }
  }

  private fun isSwallowedOrUnused(catchClause: KtCatchClause, ignoredTypes: Set<String>): Boolean =
    isUnused(catchClause, ignoredTypes) || isSwallowed(catchClause)

  private fun isUnused(catchClause: KtCatchClause, ignoredTypes: Set<String>): Boolean {
    val parameterName = catchClause.catchParameter?.name
    val catchBody = catchClause.catchBody ?: return true
    return !catchBody.anyDescendantOfType<KtNameReferenceExpression> {
      it.text in ignoredTypes || it.text == parameterName
    }
  }

  /** The exception is referenced, but only to pull values off it, so the original is dropped. */
  private fun isSwallowed(catchClause: KtCatchClause): Boolean {
    val parameterName = catchClause.catchParameter?.name
    val catchBody = catchClause.catchBody ?: return false
    return catchBody.anyDescendantOfType<KtThrowExpression> { throwExpression ->
      val references = throwExpression.parameterReferences(parameterName, catchBody)
      references.isNotEmpty() &&
        references.all { it is KtDotQualifiedExpression && it.parent !is KtThrowExpression }
    }
  }

  private fun KtThrowExpression.parameterReferences(
    parameterName: String?,
    catchBody: KtExpression,
  ): List<KtExpression> {
    val referencesInVariables = mutableMapOf<String, KtExpression>()
    return thrownExpression
      ?.collectDescendantsOfType<KtNameReferenceExpression>()
      ?.mapNotNull { reference ->
        val referenceText = reference.text
        if (referenceText == parameterName) {
          reference.getQualifiedExpressionForReceiverOrThis()
        } else {
          referencesInVariables[referenceText]
            ?: reference.findReferenceInVariable(parameterName, referenceText, catchBody)?.also {
              referencesInVariables[referenceText] = it
            }
        }
      }
      .orEmpty()
  }

  /** Follows `val message = e.message` so assigning to a local first is still a swallow. */
  private fun KtExpression.findReferenceInVariable(
    referenceName: String?,
    variableName: String,
    catchBody: KtExpression,
  ): KtExpression? {
    var block = getStrictParentOfType<KtBlockExpression>() ?: return null
    while (true) {
      val reference =
        block
          .findDescendantOfType<KtProperty> { it.name == variableName }
          ?.let { property ->
            val initializer = property.initializer
            if (initializer is KtDotQualifiedExpression) {
              initializer.takeIf { it.receiverExpression.text == referenceName }
            } else {
              initializer.takeIf { it?.text == referenceName }
            }
          }
      if (reference != null) return reference
      if (block === catchBody) return null
      block = block.getStrictParentOfType<KtBlockExpression>() ?: return null
    }
  }

  companion object {
    private val DEFAULT_IGNORED_EXCEPTION_TYPES =
      listOf(
        "InterruptedException",
        "MalformedURLException",
        "NumberFormatException",
        "ParseException",
      )

    private const val DEFAULT_ALLOWED_EXCEPTION_NAME = "_|(ignore|expected).*"

    internal val IGNORED_EXCEPTION_TYPES =
      StringOption(
        "ignored-exception-types",
        "Comma-separated list of exception simple names that may be caught without being used.",
        DEFAULT_IGNORED_EXCEPTION_TYPES.joinToString(","),
        "Catching these exceptions without using them is not flagged.",
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
          id = "SwallowedException",
          briefDescription = "Caught exception is swallowed",
          explanation =
            "An exception is caught but neither used nor passed as the cause of a newly " +
              "thrown exception. This loses the original stack trace and can hide bugs. " +
              "Log the exception, rethrow it, or pass it as a cause.",
          category = Category.CORRECTNESS,
          priority = 6,
          severity = Severity.WARNING,
          implementation = sourceImplementation<SwallowedExceptionDetector>(),
        )
        .setOptions(listOf(IGNORED_EXCEPTION_TYPES, ALLOWED_EXCEPTION_NAME))
  }
}
