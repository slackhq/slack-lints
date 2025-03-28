// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Location
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.intellij.psi.PsiNewExpression
import com.intellij.psi.PsiReferenceParameterList
import com.intellij.psi.PsiTypeElement
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtNullableType
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UVariable
import org.jetbrains.uast.getContainingUClass
import org.jetbrains.uast.resolveToUElement
import org.jetbrains.uast.util.isConstructorCall
import slack.lint.util.sourceImplementation

class NullableConcurrentHashMapDetector : Detector(), SourceCodeScanner {

  override fun getApplicableUastTypes() = listOf(UCallExpression::class.java, UVariable::class.java)

  override fun createUastHandler(context: JavaContext) =
    object : UElementHandler() {
      override fun visitVariable(node: UVariable) {
        if (node.typeReference?.getQualifiedName() != CONCURRENT_HASH_MAP) return

        // Check if key type is nullable
        node.check(0, "key")

        // Check if value type is nullable
        node.check(1, "value")
      }

      private fun UVariable.check(typeArgIndex: Int, name: String) {
        val location =
          when (val typeRefPsi = typeReference?.sourcePsi) {
            is KtTypeReference -> {
              typeRefPsi.typeElement
                ?.typeArgumentsAsTypes
                ?.takeIf { it.size == 2 }
                ?.getOrNull(typeArgIndex)
                ?.typeElement
                ?.takeIf { it is KtNullableType }
                ?.let(context::getLocation)
            }
            is PsiTypeElement -> {
              typeRefPsi.innermostComponentReferenceElement
                ?.getChildOfType<PsiReferenceParameterList>()
                ?.takeIf { it.typeParameterElements.size == 2 }
                ?.obtainIfNullable(typeArgIndex)
            }
            else -> null
          }

        if (location != null) {
          context.report(ISSUE, location, "ConcurrentHashMap should not use nullable $name types")
        }
      }

      override fun visitCallExpression(node: UCallExpression) {
        // Check if it's a constructor call
        if (!node.isConstructorCall()) return

        // Get the class being constructed
        val uClass = node.resolveToUElement()?.getContainingUClass() ?: return

        // Check if it's a ConcurrentHashMap
        if (uClass.qualifiedName != CONCURRENT_HASH_MAP) return

        // Check if key type is nullable
        node.check(0, "key")

        // Check if value type is nullable
        node.check(1, "value")
      }

      private fun UCallExpression.check(typeArgIndex: Int, name: String) {
        val location =
          when (val sourcePsi = sourcePsi) {
            is KtCallExpression -> {
              val typeElement =
                sourcePsi.typeArguments.getOrNull(typeArgIndex)?.typeReference?.typeElement
                  ?: return

              val isNullable = typeElement is KtNullableType
              if (isNullable) {
                context.getLocation(typeElement)
              } else {
                null
              }
            }
            is PsiNewExpression -> {
              sourcePsi.classReference
                ?.getChildOfType<PsiReferenceParameterList>()
                ?.takeIf { it.typeParameterElements.size == 2 }
                ?.obtainIfNullable(typeArgIndex)
            }
            else -> null
          }

        if (location != null) {
          context.report(ISSUE, location, "ConcurrentHashMap should not use nullable $name types")
        }
      }

      private fun PsiReferenceParameterList.obtainIfNullable(typeArgIndex: Int): Location? {
        val typeArg = typeParameterElements.getOrNull(typeArgIndex) ?: return null
        val isNullable =
          typeArg.type.annotations.any { it.qualifiedName?.endsWith(".Nullable") == true }

        return if (isNullable) {
          context.getLocation(typeArg)
        } else {
          null
        }
      }
    }

  companion object {
    private const val CONCURRENT_HASH_MAP = "java.util.concurrent.ConcurrentHashMap"

    val ISSUE =
      Issue.create(
        id = "NullableConcurrentHashMap",
        briefDescription = "ConcurrentHashMap should not use nullable types",
        explanation =
          """
        ConcurrentHashMap does not support null keys or values. \
        Use non-nullable types for both keys and values when creating a ConcurrentHashMap.
      """,
        category = Category.CORRECTNESS,
        priority = 5,
        severity = Severity.ERROR,
        implementation = sourceImplementation<NullableConcurrentHashMapDetector>(),
      )
  }
}
