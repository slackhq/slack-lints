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

import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.intellij.psi.PsiLocalVariable
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiMethodCallExpression
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.java.JavaUCallExpression
import org.jetbrains.uast.java.JavaUCompositeQualifiedExpression
import org.jetbrains.uast.kotlin.KotlinUFunctionCallExpression
import org.jetbrains.uast.kotlin.KotlinUQualifiedReferenceExpression
import org.jetbrains.uast.sourcePsiElement
import kotlin.reflect.full.safeCast

class ArgInFormattedQuantityStringResDetector : Detector(), SourceCodeScanner {

  companion object {
    val ISSUE_ARG_IN_QUANTITY_STRING_FORMAT: Issue = Issue.create(
      "ArgInFormattedQuantityStringRes",
      "Count value in formatted string resource.",
      "Some languages require modifiers to counted values in written text. Consider consulting #plz-localization " +
        "if you are unsure if this formatted string requires a special modifier. If one is required, consider using " +
        "`LocalizationUtils.getFormattedCount()`. If not, suppress this warning.",
      Category.I18N,
      6,
      Severity.WARNING,
      Implementation(ArgInFormattedQuantityStringResDetector::class.java, Scope.JAVA_FILE_SCOPE)
    )

    val issues: Array<Issue> = arrayOf(ISSUE_ARG_IN_QUANTITY_STRING_FORMAT)
  }

  override fun getApplicableMethodNames(): List<String> = listOf("getQuantityString")

  override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
    // Ignore methods that aren't in a subclass of Android "Resources"
    if (context.evaluator.isMemberInSubClassOf(method, "android.content.res.Resources", false)) {
      node.valueArguments
        .drop(2) // Ignore the first 2 arguments passed to "getQuantityString"
        .forEach { arg ->
          when (arg) {
            is JavaUCompositeQualifiedExpression -> checkCall { JavaUCallExpression::class.safeCast(arg.selector) }
            is JavaUCallExpression -> checkCall { arg }
            is KotlinUQualifiedReferenceExpression -> checkCall { KotlinUFunctionCallExpression::class.safeCast(arg.selector) }
            is KotlinUFunctionCallExpression -> checkCall { arg }
            else -> checkVariable { arg }
          }.let { countFormatFound ->
            if (!countFormatFound) {
              context.report(
                ISSUE_ARG_IN_QUANTITY_STRING_FORMAT,
                context.getLocation(arg),
                "This may require a localized count modifier. If so, use `LocalizationUtils.getFormattedCount()`. Consult #plz-localization if you are unsure."
              )
            }
          }
        }
    }

    super.visitMethodCall(context, node, method)
  }

  /**
   * return true if the resolved [UCallExpression] has method name "getFormattedCount", false otherwise
   */
  private fun checkCall(fn: () -> UCallExpression?): Boolean {
    return fn()?.let { call -> "getFormattedCount" == call.methodName } ?: false
  }

  /**
   * return true if the resolved [UExpression] was created from the "getFormattedCount" method, false otherwise
   */
  private fun checkVariable(fn: () -> UExpression?): Boolean {
    return fn()?.let { exp ->
      val variable = exp.sourcePsiElement?.reference?.resolve() as? PsiLocalVariable
      val assignment = variable?.initializer as? PsiMethodCallExpression
      return assignment?.resolveMethod()?.name == "getFormattedCount"
    } ?: false
  }
}
