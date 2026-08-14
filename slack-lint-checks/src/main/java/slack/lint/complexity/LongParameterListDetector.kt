// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint.complexity

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.BooleanOption
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.IntOption
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Location
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.android.tools.lint.detector.api.StringOption
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtConstructor
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtModifierListOwner
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UMethod
import slack.lint.util.BooleanLintOption
import slack.lint.util.IntLintOption
import slack.lint.util.OptionLoadingDetector
import slack.lint.util.StringSetLintOption
import slack.lint.util.hasAnyAnnotation
import slack.lint.util.sourceImplementation

class LongParameterListDetector(
  private val functionThresholdOption: IntLintOption = IntLintOption(FUNCTION_THRESHOLD),
  private val constructorThresholdOption: IntLintOption = IntLintOption(CONSTRUCTOR_THRESHOLD),
  private val ignoreDataClassesOption: BooleanLintOption = BooleanLintOption(IGNORE_DATA_CLASSES),
  private val ignoreAnnotatedOption: StringSetLintOption = StringSetLintOption(IGNORE_ANNOTATED),
) :
  OptionLoadingDetector(
    functionThresholdOption,
    constructorThresholdOption,
    ignoreDataClassesOption,
    ignoreAnnotatedOption,
  ),
  SourceCodeScanner {

  override fun getApplicableUastTypes(): List<Class<out UElement>> = listOf(UMethod::class.java)

  override fun createUastHandler(context: JavaContext): UElementHandler {
    return object : UElementHandler() {
      override fun visitMethod(node: UMethod) {
        val source = node.sourcePsi
        val isConstructor =
          source is KtConstructor<*> || (source is KtClassOrObject && node.isConstructor)
        if (isConstructor) {
          if (!ignoresOwningDataClass(source)) {
            report(
              node.hasAnyAnnotation(ignoreAnnotatedOption.value),
              source,
              context.getNameLocation(node),
              isConstructor = true,
            )
          }
          return
        }
        if (source !is KtNamedFunction) return
        report(
          node.hasAnyAnnotation(ignoreAnnotatedOption.value),
          source,
          context.getNameLocation(node),
          isConstructor = false,
        )

        // Lint never visits local functions, so check them from the enclosing declaration. They
        // have no light-class method, so annotations and location come from the source instead.
        reportLocalFunctions(source)
      }

      /**
       * Reports local functions declared directly in [scope], recursing into each one. Stops at any
       * declaration lint visits on its own, so a class member nested in here isn't reported twice.
       */
      private fun reportLocalFunctions(scope: KtElement) {
        scope.children.forEach { child ->
          when {
            child is KtNamedFunction && child.isLocal -> {
              report(
                child.hasAnyIgnoredAnnotation(),
                child,
                context.getNameLocation(child),
                isConstructor = false,
              )
              reportLocalFunctions(child)
            }
            child is KtClassOrObject -> Unit
            child is KtElement -> reportLocalFunctions(child)
          }
        }
      }

      private fun report(
        ignoredByAnnotation: Boolean,
        source: PsiElement?,
        location: Location,
        isConstructor: Boolean,
      ) {
        if (ignoredByAnnotation) return
        if ((source as? KtModifierListOwner)?.hasModifier(KtTokens.OVERRIDE_KEYWORD) == true) return

        val threshold =
          if (isConstructor) constructorThresholdOption.value else functionThresholdOption.value
        // Count declared value parameters, not UAST parameters: the latter also includes the
        // extension receiver and a suspend function's continuation.
        val paramCount = valueParameterCount(source) ?: return
        if (paramCount >= threshold) {
          val subject = if (isConstructor) "Constructor" else "Function"
          context.report(
            ISSUE,
            location,
            "$subject has $paramCount parameters, the threshold is $threshold",
          )
        }
      }

      private fun KtNamedFunction.hasAnyIgnoredAnnotation(): Boolean {
        val ignored = ignoreAnnotatedOption.value
        if (ignored.isEmpty()) return false
        return annotationEntries.any {
          it.shortName?.asString() in ignored ||
            it.typeReference?.text?.substringAfterLast('.') in ignored
        }
      }

      private fun valueParameterCount(source: PsiElement?): Int? =
        when (source) {
          is KtFunction -> source.valueParameterList?.parameters?.size
          is KtClassOrObject -> source.primaryConstructor?.valueParameters?.size
          else -> null
        }

      private fun ignoresOwningDataClass(source: PsiElement?): Boolean {
        if (!ignoreDataClassesOption.value) return false
        val owner =
          when (source) {
            is KtConstructor<*> -> source.getContainingClassOrObject()
            is KtClassOrObject -> source
            else -> null
          }
        return (owner as? KtClass)?.hasModifier(KtTokens.DATA_KEYWORD) == true
      }
    }
  }

  companion object {
    internal val FUNCTION_THRESHOLD =
      IntOption("function-threshold", "Number of function parameters required to report.", 6)

    internal val CONSTRUCTOR_THRESHOLD =
      IntOption("constructor-threshold", "Number of constructor parameters required to report.", 7)

    internal val IGNORE_DATA_CLASSES =
      BooleanOption(
        "ignore-data-classes",
        "Ignore long constructor parameter lists on data classes.",
        true,
      )

    internal val IGNORE_ANNOTATED =
      StringOption(
        "ignore-annotated",
        "Comma-separated list of annotation simple names to ignore.",
        "Inject,Provides,AssistedInject,Composable",
        "Functions annotated with these annotations are excluded from this check.",
      )

    val ISSUE =
      Issue.create(
          id = "LongParameterList",
          briefDescription = "Function has too many parameters",
          explanation =
            "Functions with many parameters are hard to call correctly and suggest " +
              "the function is doing too much. Consider using a data class or builder pattern.",
          category = Category.CORRECTNESS,
          priority = 5,
          severity = Severity.WARNING,
          implementation = sourceImplementation<LongParameterListDetector>(),
        )
        .setOptions(
          listOf(FUNCTION_THRESHOLD, CONSTRUCTOR_THRESHOLD, IGNORE_DATA_CLASSES, IGNORE_ANNOTATED)
        )
  }
}
