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
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.android.tools.lint.detector.api.TextFormat
import com.intellij.psi.PsiClass
import com.intellij.psi.util.InheritanceUtil
import org.jetbrains.uast.UClass
import org.jetbrains.uast.kotlin.KotlinUClassLiteralExpression
import slack.lint.util.sourceImplementation

/**
 * A simple detector that ensures that...
 * - Classes annotated with `@InjectWith` implement `LoggedInUserProvider` if the target scope is
 * `UserScope` or `OrgScope`.
 * - Classes annotated with `@InjectWith` implement `AnvilInjectable`.
 */
class InjectWithUsageDetector : Detector(), SourceCodeScanner {

  override fun getApplicableUastTypes() = listOf(UClass::class.java)

  override fun createUastHandler(context: JavaContext): UElementHandler {
    return object : UElementHandler() {
      override fun visitClass(node: UClass) {
        node.findAnnotation(FQCN_INJECT_WITH)?.let { injectWith ->
          val implementsAnvilInjectable = InheritanceUtil.isInheritor(
            node,
            true,
            FQCN_ANVIL_INJECTABLE
          )
          if (!implementsAnvilInjectable) {
            context.report(
              ANVIL_INJECTABLE_ISSUE,
              context.getNameLocation(node),
              ANVIL_INJECTABLE_ISSUE.getBriefDescription(TextFormat.TEXT),
              quickfixData = null
            )
          }

          val scopeAttribute = (
            injectWith.findAttributeValue(
              "scope"
            ) as? KotlinUClassLiteralExpression
            ) ?: return

          val scopeType = scopeAttribute.type ?: return
          val scopeClass = context.evaluator.getTypeClass(scopeType) ?: return

          if (needsToImplementLoggedInUserProvider(scopeClass)) {
            val implementsLoggedInUserProvider = InheritanceUtil.isInheritor(
              node,
              true,
              FQCN_LOGGED_IN_USER_PROVIDER
            )
            if (!implementsLoggedInUserProvider) {
              context.report(
                LOGGED_IN_USER_PROVIDER_ISSUE,
                context.getNameLocation(node),
                LOGGED_IN_USER_PROVIDER_ISSUE.getBriefDescription(TextFormat.TEXT),
                quickfixData = null
              )
            }
          }
        }
      }
    }
  }

  companion object {
    private const val FQCN_INJECT_WITH = "slack.anvil.injection.InjectWith"
    private const val FQCN_ANVIL_INJECTABLE = "slack.anvil.injection.AnvilInjectable"
    private const val FQCN_ORG_SCOPE = "slack.di.OrgScope"
    private const val FQCN_USER_SCOPE = "slack.di.UserScope"
    private const val FQCN_LOGGED_IN_USER_PROVIDER = "slack.foundation.auth.LoggedInUserProvider"

    private val LOGGED_IN_USER_PROVIDER_ISSUE: Issue = Issue.create(
      "InjectWithScopeRequiredLoggedInUserProvider",
      "@InjectWith-annotated classes must implement LoggedInUserProvider (or extend something that does) if they target UserScope or OrgScope.",
      "`@InjectWith`-annotated classes must implement LoggedInUserProvider (or extend something that does) if they target UserScope or OrgScope.",
      Category.CORRECTNESS,
      9,
      Severity.ERROR,
      sourceImplementation<InjectWithUsageDetector>()
    )

    private val ANVIL_INJECTABLE_ISSUE: Issue = Issue.create(
      "InjectWithTypeMustImplementAnvilInjectable",
      "@InjectWith-annotated classes must implement AnvilInjectable (or extend something that does).",
      "`@InjectWith`-annotated classes must implement AnvilInjectable (or extend something that does).",
      Category.CORRECTNESS,
      9,
      Severity.ERROR,
      sourceImplementation<InjectWithUsageDetector>()
    )

    val ISSUES = arrayOf(LOGGED_IN_USER_PROVIDER_ISSUE, ANVIL_INJECTABLE_ISSUE)

    private val LOGGED_IN_SCOPES = setOf(
      FQCN_ORG_SCOPE, FQCN_USER_SCOPE
    )

    private fun needsToImplementLoggedInUserProvider(scope: PsiClass): Boolean {
      return scope.qualifiedName in LOGGED_IN_SCOPES
    }
  }
}
