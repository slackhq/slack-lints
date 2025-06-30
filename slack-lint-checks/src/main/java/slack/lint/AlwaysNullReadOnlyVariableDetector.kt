// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.android.tools.lint.detector.api.TextFormat
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtReturnExpression
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.psi.psiUtil.isNull
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.ULiteralExpression
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.UVariable
import org.jetbrains.uast.kotlin.isKotlin
import slack.lint.util.sourceImplementation

class AlwaysNullReadOnlyVariableDetector : Detector(), SourceCodeScanner {
  override fun getApplicableUastTypes() =
    listOf(UVariable::class.java, UCallExpression::class.java, UMethod::class.java)

  override fun createUastHandler(context: JavaContext): UElementHandler? {
    if (!isKotlin(context.uastFile?.lang)) return null

    return object : UElementHandler() {

      private fun isNullInitializedForReadOnlyVariable(node: UVariable): Boolean {
        val uastInitializer = node.uastInitializer ?: return false
        val isNullInitialized = uastInitializer is ULiteralExpression && uastInitializer.isNull
        if (!isNullInitialized) return false
        val sourcePsi = node.sourcePsi
        val isReadOnlyVariable =
          sourcePsi is KtProperty &&
            !sourcePsi.isVar &&
            !sourcePsi.hasModifier(KtTokens.OPEN_KEYWORD)
        return isReadOnlyVariable
      }

      override fun visitVariable(node: UVariable) {
        if (isNullInitializedForReadOnlyVariable(node)) {
          context.report(
            ISSUE_ALWAYS_INITIALIZE_NULL,
            context.getLocation(node.uastInitializer),
            ISSUE_ALWAYS_INITIALIZE_NULL.getBriefDescription(TextFormat.TEXT),
          )
        }
      }

      override fun visitMethod(node: UMethod) {
        val sourcePsi = node.sourcePsi?.parent as? KtProperty ?: return
        val getter = sourcePsi.getter ?: return

        val isReadOnlyVariable = !sourcePsi.isVar
        if (isReadOnlyVariable) {

          // get() = null
          val bodyExpression = getter.bodyExpression ?: return

          if (bodyExpression.isNull()) {
            context.report(
              ISSUE_ALWAYS_RETURN_NULL_IN_GETTER,
              context.getLocation(bodyExpression),
              ISSUE_ALWAYS_RETURN_NULL_IN_GETTER.getBriefDescription(TextFormat.TEXT),
            )
          }

          // get() { return null }
          val returnExpression = bodyExpression.collectDescendantsOfType<KtReturnExpression>()
          returnExpression.forEach { expression ->
            val returnedExpression = expression.returnedExpression ?: return@forEach
            if (returnedExpression.isNull()) {
              context.report(
                ISSUE_ALWAYS_RETURN_NULL_IN_GETTER,
                context.getLocation(returnedExpression),
                ISSUE_ALWAYS_RETURN_NULL_IN_GETTER.getBriefDescription(TextFormat.TEXT),
              )
            }
          }
        }
      }
    }
  }

  companion object {
    val ISSUE_ALWAYS_INITIALIZE_NULL: Issue =
      Issue.create(
        "AvoidNullInitForReadOnlyVariables",
        "Avoid initializing read-only variable with null in Kotlin",
        """
          Avoid unnecessary `null` initialization for read-only variables, as they can never be reassigned. \
          Assigning null explicitly does not provide any real benefit and may mislead readers into thinking the value could change later. \
          If the variable needs to be modified later, it's better to use `var` instead of `val`, or consider using `lateinit var` if it is guaranteed to be initialized before use.
        """,
        Category.CORRECTNESS,
        6,
        Severity.WARNING,
        sourceImplementation<AlwaysNullReadOnlyVariableDetector>(),
      )

    val ISSUE_ALWAYS_RETURN_NULL_IN_GETTER: Issue =
      Issue.create(
        "AvoidReturningNullInGetter",
        "Avoid returning null in getter for read-only properties in Kotlin",
        """
          Avoid defining a getter that always returns `null` for a read-only (`val`) property. \
        Since `val` properties cannot be reassigned, having a getter that consistently returns `null` serves no real purpose \
        and may cause confusion.

        If the value needs to be dynamically computed, ensure the getter returns a meaningful result. \
        Otherwise, consider using a function (`fun`) instead of a property.
        """,
        Category.CORRECTNESS,
        6,
        Severity.WARNING,
        sourceImplementation<AlwaysNullReadOnlyVariableDetector>(),
      )

    val ISSUES: List<Issue> =
      listOf(ISSUE_ALWAYS_INITIALIZE_NULL, ISSUE_ALWAYS_RETURN_NULL_IN_GETTER)
  }
}
