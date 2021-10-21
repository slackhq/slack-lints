/*
 * Copyright (C) 2021 Slack Technologies, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package slack.lint

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.android.tools.lint.detector.api.TextFormat
import com.android.tools.lint.detector.api.UastLintUtils.Companion.tryResolveUDeclaration
import com.android.tools.lint.detector.api.isJava
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassType
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UCallableReferenceExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.UQualifiedReferenceExpression
import java.util.EnumSet

/**
 * This is a [Detector] for detecting direct usages of Kotlin coroutines'
 * [kotlinx.coroutines.Dispatchers] properties, which we want to prevent in favor of our
 * `SlackDispatchers` abstraction.
 */
class RawDispatchersUsageDetector : Detector(), SourceCodeScanner {

  companion object {
    private val SCOPES = Implementation(
      RawDispatchersUsageDetector::class.java,
      EnumSet.of(Scope.JAVA_FILE)
    )

    val ISSUE: Issue = Issue.create(
      "RawDispatchersUse",
      "Use SlackDispatchers.",
      """
        Direct use of `Dispatchers.*` APIs are discouraged as they are difficult to test. Prefer using \
        `SlackDispatchers`.
      """,
      Category.CORRECTNESS,
      6,
      Severity.ERROR,
      SCOPES
    )

    private const val DISPATCHERS_CLASS = "kotlinx.coroutines.Dispatchers"
    private val PROPERTY_GETTERS = setOf(
      "getDefault",
      "getIO",
      "getMain",
      "getUnconfined",
      "Default",
      "IO",
      "Main",
      "Unconfined"
    )
  }

  override fun getApplicableUastTypes(): List<Class<out UElement>> = listOf(
    UCallExpression::class.java,
    UCallableReferenceExpression::class.java,
    UQualifiedReferenceExpression::class.java
  )

  override fun createUastHandler(context: JavaContext): UElementHandler? {
    // Only applicable on Kotlin files
    if (isJava(context.psiFile)) return null

    fun report(node: UElement) {
      context.report(
        ISSUE,
        context.getLocation(node),
        ISSUE.getBriefDescription(TextFormat.TEXT)
      )
    }

    fun String?.isDispatcherGetter(): Boolean {
      return this in PROPERTY_GETTERS
    }

    fun PsiClass?.isDispatchersClass(): Boolean {
      if (this == null) return false
      return qualifiedName == DISPATCHERS_CLASS
    }

    return object : UElementHandler() {
      // Awkward but this is how to look for property getter calls
      override fun visitQualifiedReferenceExpression(node: UQualifiedReferenceExpression) {
        val expressionType = node.receiver.getExpressionType() ?: return
        if (expressionType is PsiClassType && expressionType.resolve().isDispatchersClass()) {
          val uDecl = node.selector.tryResolveUDeclaration() ?: return
          if (uDecl is UMethod && uDecl.name.isDispatcherGetter()) {
            report(node)
          }
        }
      }

      override fun visitCallExpression(node: UCallExpression) {
        if (node.methodName.isDispatcherGetter()) {
          val resolved = node.resolve() ?: return
          if (resolved.containingClass.isDispatchersClass()) {
            report(node)
          }
        }
      }

      override fun visitCallableReferenceExpression(node: UCallableReferenceExpression) {
        if (node.callableName.isDispatcherGetter()) {
          val qualifierType = node.qualifierType ?: return
          if (qualifierType is PsiClassType &&
            qualifierType.resolve().isDispatchersClass()
          )
            report(node)
        }
      }
    }
  }
}
