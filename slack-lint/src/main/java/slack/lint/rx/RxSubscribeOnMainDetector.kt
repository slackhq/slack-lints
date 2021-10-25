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
package slack.lint.rx

import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.LintFix
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.intellij.lang.java.JavaLanguage
import com.intellij.psi.PsiField
import com.intellij.psi.PsiLocalVariable
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiMethodCallExpression
import org.jetbrains.kotlin.asJava.elements.KtLightField
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.java.JavaUCallExpression
import org.jetbrains.uast.java.JavaUCompositeQualifiedExpression
import org.jetbrains.uast.kotlin.KotlinUFunctionCallExpression
import org.jetbrains.uast.kotlin.KotlinUQualifiedReferenceExpression
import org.jetbrains.uast.kotlin.KotlinUSimpleReferenceExpression
import org.jetbrains.uast.kotlin.psi.UastKotlinPsiVariable
import slack.lint.util.sourceImplementation
import kotlin.reflect.full.safeCast

/**
 * [Detector] for usages of `Observable.subscribeOn(AndroidSchedulers.mainThread())`. Typically, this is not desired
 * and instead users are looking for `observeOn(AndroidSchedulers.mainThread())`.
 */
class RxSubscribeOnMainDetector : Detector(), SourceCodeScanner {

  companion object {
    private fun Implementation.toIssue() = Issue.create(
      id = "SubscribeOnMain",
      briefDescription = "subscribeOn called with the main thread scheduler.",
      explanation = """
        Calling `subscribeOn(AndroidSchedulers.mainThread())` will cause the code ran at subscription time to be executed \
        on the main thread - that is, code above this line.
        Typically this is not actually desired, and instead you want to use observeOn(AndroidSchedulers.mainThread()) \
        which will cause the code below this line to be run on the main thread (eg the code inside your subscribe() \
        block).
      """,
      category = Category.CORRECTNESS,
      priority = 4,
      severity = Severity.ERROR,
      implementation = this
    )

    val ISSUE = sourceImplementation<RxSubscribeOnMainDetector>().toIssue()
  }

  override fun getApplicableMethodNames(): List<String> = listOf("subscribeOn")

  override fun getApplicableCallOwners() = listOf(
    "io/reactivex/rxjava3/core/Completable",
    "io/reactivex/rxjava3/core/Flowable",
    "io/reactivex/rxjava3/core/Maybe",
    "io/reactivex/rxjava3/core/Observable",
    "io/reactivex/rxjava3/core/Single"
  )

  override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
    val arg = node.valueArguments.first()
    when (arg) {
      is JavaUCompositeQualifiedExpression -> checkCall { JavaUCallExpression::class.safeCast(arg.selector) }
      is JavaUCallExpression -> checkCall { arg }
      is KotlinUQualifiedReferenceExpression -> checkCall { KotlinUFunctionCallExpression::class.safeCast(arg.selector) }
      is KotlinUFunctionCallExpression -> checkCall { arg }
      else -> checkVariable { arg }
    }.let { mainThreadFound ->
      if (mainThreadFound) {
        context.report(
          ISSUE,
          context.getCallLocation(node, includeReceiver = false, includeArguments = true),
          "This will make the code for the initial subscription (above this line) run on the main thread. " +
            "You probably want `observeOn(AndroidSchedulers.mainThread())`.",
          LintFix.create()
            .replace()
            .name("Replace with observeOn()")
            .text("subscribeOn")
            .with("observeOn")
            .build()
        )
      }
    }
  }

  /**
   * return true if the resolved [UCallExpression] has method name "mainThread" or "immediateMainThread",
   * false otherwise
   */
  private fun checkCall(fn: () -> UCallExpression?): Boolean {
    return fn()?.let { call -> "mainThread" == call.methodName || "immediateMainThread" == call.methodName } ?: false
  }

  /**
   * return true if the resolved [UExpression] was created from the "mainThread" or "immediateMainThread" methods,
   * false otherwise
   */
  private fun checkVariable(fn: () -> UExpression?): Boolean {
    return fn()?.let { exp ->
      when (exp.lang) {
        is KotlinLanguage -> checkKotlinVariable(exp)
        is JavaLanguage -> checkJavaVariable(exp)
        else -> return false
      }
    } ?: false
  }

  private fun checkKotlinVariable(exp: UExpression): Boolean {
    return when (exp) {
      is KotlinUSimpleReferenceExpression -> {
        val initializerText = when (val reference = exp.resolve()) {
          is KtLightField -> { // The variable reference is a member
            val initializer = (reference.kotlinOrigin as? KtProperty)?.initializer
            initializer?.node?.text
          }
          is UastKotlinPsiVariable -> { // The variable reference is local
            val initializer = reference.initializer
            initializer?.node?.text
          }
          else -> null
        }
        initializerText?.let { it.endsWith("mainThread()") || it.endsWith("immediateMainThread()") } ?: false
      }
      else -> false
    }
  }

  private fun checkJavaVariable(exp: UExpression): Boolean {
    val assignment = when (val variable = exp.sourcePsi?.reference?.resolve()) {
      is PsiField -> variable.initializer as? PsiMethodCallExpression
      is PsiLocalVariable -> variable.initializer as? PsiMethodCallExpression
      else -> null
    }
    val methodName = assignment?.resolveMethod()?.name
    return methodName == "mainThread" || methodName == "immediateMainThread"
  }
}
