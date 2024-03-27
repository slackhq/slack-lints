// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint.eithernet

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Location
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.android.tools.lint.detector.api.isJava
import com.intellij.psi.PsiModifierListOwner
import com.intellij.psi.PsiType
import com.intellij.psi.util.PsiTypesUtil
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtModifierListOwner
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.psiUtil.visibilityModifierTypeOrDefault
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UField
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.getContainingUClass
import org.jetbrains.uast.java.isJava
import slack.lint.util.safeReturnType
import slack.lint.util.sourceImplementation

private const val EITHERNET_PACKAGE = "com.slack.eithernet"

/** Reports an error when returning EitherNet types directly in public repository APIs. */
class DoNotExposeEitherNetInRepositoriesDetector : Detector(), SourceCodeScanner {

  override fun getApplicableUastTypes(): List<Class<out UElement>> =
    listOf(UMethod::class.java, UField::class.java)

  override fun createUastHandler(context: JavaContext) =
    object : UElementHandler() {
      val isJava = isJava(context.uastFile?.lang)

      override fun visitMethod(node: UMethod) {
        if (node.sourcePsi is KtProperty) return // Handled by visitField
        check(
          node.isPublic,
          { node.isRepositoryMember },
          { node.safeReturnType(context).isEitherNetType },
          { context.getLocation(node.returnTypeReference ?: node) },
        )
      }

      override fun visitField(node: UField) {
        check(
          node.isPublic,
          { node.isRepositoryMember },
          { node.type.isEitherNetType },
          { context.getLocation(node.typeReference ?: node) },
        )
      }

      private val UElement.isPublic: Boolean
        get() {
          if (isJava && this is PsiModifierListOwner) {
            if (modifierList?.hasExplicitModifier("public") == true) return true
          }
          if (sourcePsi is KtModifierListOwner) {
            val ktModifierListOwner = sourcePsi as KtModifierListOwner
            val visibility = ktModifierListOwner.visibilityModifierTypeOrDefault()
            if (visibility == KtTokens.PUBLIC_KEYWORD) return true
          }
          return getContainingUClass()?.isInterface == true
        }

      private fun check(
        isPublic: Boolean,
        isRepositoryMember: () -> Boolean,
        isEitherNetType: () -> Boolean,
        location: () -> Location,
      ) {
        if (!isPublic) return
        if (!isRepositoryMember()) return
        if (isEitherNetType()) {
          context.report(
            issue = ISSUE,
            location = location(),
            message = "Repository APIs should not expose EitherNet types directly.",
          )
        }
      }

      private val UElement.isRepositoryMember: Boolean
        get() {
          val containingClass = getContainingUClass() ?: return false
          return containingClass.name?.endsWith("Repository") == true
        }

      private val PsiType?.isEitherNetType: Boolean
        get() {
          return PsiTypesUtil.getPsiClass(this)?.qualifiedName?.startsWith(EITHERNET_PACKAGE) ==
            true
        }
    }

  companion object {
    private fun Implementation.toIssue(): Issue {
      return Issue.create(
        id = "DoNotExposeEitherNetInRepositories",
        briefDescription = "Repository APIs should not expose EitherNet types directly.",
        explanation =
          "EitherNet (and networking in general) should be an implementation detail of the repository layer.",
        category = Category.CORRECTNESS,
        priority = 0,
        severity = Severity.ERROR,
        implementation = this,
      )
    }

    val ISSUE: Issue = sourceImplementation<DoNotExposeEitherNetInRepositoriesDetector>().toIssue()
  }
}
