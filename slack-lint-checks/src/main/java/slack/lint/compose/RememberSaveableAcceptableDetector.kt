// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint.compose

import com.android.tools.lint.checks.DataFlowAnalyzer
import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.android.tools.lint.detector.api.TextFormat
import com.android.tools.lint.detector.api.asCall
import com.intellij.psi.PsiArrayType
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiParameter
import com.intellij.psi.PsiPrimitiveType
import com.intellij.psi.PsiType
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.UReturnExpression
import org.jetbrains.uast.UastEmptyExpression
import org.jetbrains.uast.skipParenthesizedExprDown
import org.jetbrains.uast.tryResolve
import org.jetbrains.uast.tryResolveNamed
import org.jetbrains.uast.unwrapReferenceNameElement
import org.jetbrains.uast.visitor.AbstractUastVisitor
import slack.lint.util.Name
import slack.lint.util.Package
import slack.lint.util.asClass
import slack.lint.util.implements
import slack.lint.util.isBoxedPrimitive
import slack.lint.util.isInPackageName
import slack.lint.util.sourceImplementation

private const val REMEMBER_SAVEABLE_METHOD_NAME = "rememberSaveable"

private val ComposeRuntimePackageName = Package("androidx.compose.runtime")
private val ComposeSaveablePackageName = Package("androidx.compose.runtime.saveable")
private val SaverFQN = Name(ComposeSaveablePackageName, "Saver").javaFqn
private val AUTO_SAVER = UastEmptyExpression(null)

// todo
//  - Collections (listOf, mapOf, etc) checks
//  - ArrayList check?
//  Think the savers likely go in a different detector
//  - MapSaver check
//  - ListSaver check
class RememberSaveableAcceptableDetector : Detector(), SourceCodeScanner {

  override fun getApplicableUastTypes(): List<Class<out UElement>> =
    listOf(UCallExpression::class.java)

  override fun createUastHandler(context: JavaContext): UElementHandler =
    RememberSaveableElementHandler(context)

  companion object {
    const val MESSAGE = "remember"
    const val ISSUE_ID = "RememberSaveableTypeMustBeAcceptable"
    const val BRIEF_DESCRIPTION = "Brief description"
    const val EXPLANATION = """Full explanation"""
    val ISSUE: Issue =
      Issue.create(
        ISSUE_ID,
        BRIEF_DESCRIPTION,
        EXPLANATION,
        Category.CORRECTNESS,
        10,
        Severity.ERROR,
        sourceImplementation<RememberSaveableAcceptableDetector>(),
      )
  }
}

private class RememberSaveableElementHandler(val context: JavaContext) : UElementHandler() {
  override fun visitCallExpression(node: UCallExpression) {
    if (node.methodName != REMEMBER_SAVEABLE_METHOD_NAME) return
    val evaluator = context.evaluator
    val method = node.resolve()
    val returnType = node.returnType
    if (
      method != null && returnType != null && method.isInPackageName(ComposeSaveablePackageName)
    ) {
      val arguments = evaluator.computeArgumentMapping(node, method)
      val saver = arguments.getSaver()
      if (saver == AUTO_SAVER) {
        visitAutoSaver(node, returnType, arguments)
      } else {
        visitCustomSaver(node, saver)
      }
    }
  }

  private fun visitAutoSaver(
    node: UCallExpression,
    returnType: PsiType,
    arguments: Map<UExpression, PsiParameter>,
  ) {
    val init = arguments.getInit()
    // If there is no init expression or if the return is an acceptable type, just return.
    if (init == null || returnType.isAcceptableType()) {
      return
    }
    // Check what is created in the init expression.
    val (allAcceptableMutableStates, customPolices) =
      returnsKnownMutableState(returnType, init, context)
    if (allAcceptableMutableStates) {
      // Found a known parcelable mutable state!
      // Report the custom policy error when using the auto saver.
      // todo Report error message "If you use a custom SnapshotMutationPolicy for your
      //  MutableState you have to write a custom Saver"
      customPolices.forEach {
        context.report(
          RememberSaveableAcceptableDetector.Companion.ISSUE,
          context.getLocation(it),
          RememberSaveableAcceptableDetector.Companion.ISSUE.getBriefDescription(TextFormat.TEXT) +
            " (Custom policy)",
        )
      }
      return
    }

    if (returnsLambdaExpression(returnType, init)) {
      // todo Report specific error language about kotlin Lambdas
      context.report(
        RememberSaveableAcceptableDetector.Companion.ISSUE,
        context.getLocation(node),
        RememberSaveableAcceptableDetector.Companion.ISSUE.getBriefDescription(TextFormat.TEXT) +
          " (LAMBDA)",
      )
    } else {
      // "The default implementation only supports types which can be stored inside the Bundle.
      // Please consider implementing a custom Saver for this class and pass it to
      // rememberSaveable()."
      context.report(
        RememberSaveableAcceptableDetector.Companion.ISSUE,
        context.getLocation(node),
        RememberSaveableAcceptableDetector.Companion.ISSUE.getBriefDescription(TextFormat.TEXT),
      )
    }
  }

  private fun visitCustomSaver(node: UCallExpression, saver: UExpression) {
    // todo Custom saver, check the return type of the save call and report an error.
    val unwrapedElement = unwrapReferenceNameElement(saver)
    val visitor = CustomSaverVisitor()
    unwrapedElement?.accept(visitor)
    if (visitor.saveableType?.isAcceptableType() == false) {
      context.report(
        RememberSaveableAcceptableDetector.Companion.ISSUE,
        context.getLocation(node),
        RememberSaveableAcceptableDetector.Companion.ISSUE.getBriefDescription(TextFormat.TEXT),
      )
    }
  }
}

private class CustomSaverVisitor : AbstractUastVisitor() {

  var saveableType: PsiType? = null

  override fun visitExpression(node: UExpression): Boolean {
    this.saveableType = node.getExpressionType()?.asClass()?.let { unwrapSaveableType(it) }
    return saveableType != null || super.visitExpression(node)
  }

  private fun unwrapSaveableType(element: PsiClassType): PsiType? {
    val resolved = element.resolve()
    return when {
      resolved == null -> null
      resolved.qualifiedName == SaverFQN -> element.parameters[1]
      else -> resolved.superTypes.firstNotNullOfOrNull { type -> unwrapSaveableType(type) }
    }
  }
}

private fun Map<UExpression, PsiParameter>.getSaver(): UExpression {
  val saver =
    firstNotNullOfOrNull { (expression, parameter) ->
        if (parameter.name == "saver") expression else null
      }
      ?.skipParenthesizedExprDown()
  val resolved = saver?.tryResolve()
  if (
    resolved is PsiMethod &&
      resolved.name == "autoSaver" &&
      resolved.isInPackageName(ComposeSaveablePackageName)
  ) {
    return AUTO_SAVER
  }
  return saver ?: AUTO_SAVER
}

private fun Map<UExpression, PsiParameter>.getInit(): UExpression? {
  val init = firstNotNullOfOrNull { (expression, parameter) ->
    if (parameter.name == "init") {
      expression
    } else null
  }
  return init
}

/**
 * The default Android `mutableStateOf` is `ParcelableSnapshotMutableState`, so check if all the
 * return statements are default `mutableStateOf` calls.
 */
private fun returnsKnownMutableState(
  returnType: PsiType,
  expression: UExpression,
  context: JavaContext,
): ReturnsKnownMutableStateResult {
  return if (returnType.isAcceptableMutableStateClass()) {
    val visitor = MutableStateOfVisitor(context)
    expression.accept(visitor)
    val allAcceptableMutableStates = visitor.checkAllReturned(expression)
    ReturnsKnownMutableStateResult(allAcceptableMutableStates, visitor.customPolices)
  } else {
    ReturnsKnownMutableStateResult(false, emptyList())
  }
}

private data class ReturnsKnownMutableStateResult(
  val allAcceptableMutableStates: Boolean,
  val customPolices: List<UCallExpression>,
)

private fun PsiType?.isAcceptableMutableStateClass(): Boolean {
  val psiClassType = asClass()
  val isMutableState =
    psiClassType
      ?.resolve()
      ?.implements("${ComposeRuntimePackageName.javaPackageName}.MutableState") == true
  return isMutableState && psiClassType.parameters.all { it.isAcceptableType() }
}

/**
 * Visitor that tracks `mutableStateOf` function calls within an expression to determine if all such
 * calls are returned from their containing expressions.
 *
 * This visitor is used to verify that when `rememberSaveable` contains `mutableStateOf` calls,
 * those calls are actually being returned (and thus can be properly saved/restored by the
 * auto-saver mechanism).
 *
 * @param context The JavaContext for the current lint analysis
 */
private class MutableStateOfVisitor(private val context: JavaContext) : AbstractUastVisitor() {

  /** List of all `mutableStateOf` function calls found during traversal */
  val mutableStateOfs = mutableListOf<UCallExpression>()
  val customPolices = mutableListOf<UCallExpression>()

  /**
   * Visits call expressions and tracks those that are known mutable state functions.
   *
   * @param node The call expression to examine
   * @return true if the node is a known mutable state function, false otherwise
   */
  override fun visitCallExpression(node: UCallExpression): Boolean =
    if (isKnownMutableStateFunction(node, context)) {
      mutableStateOfs.add(node)
      true
    } else false

  /**
   * Checks if all tracked `mutableStateOf` calls are returned from their containing expressions.
   *
   * @return true if all tracked calls are returned, false otherwise
   */
  fun checkAllReturned(expression: UExpression): Boolean {
    return mutableStateOfs.isNotEmpty() &&
      mutableStateOfs.all { tracked ->
        val visitor = ReturnsTracker(tracked)
        expression.accept(visitor)
        visitor.returned
      }
  }

  /**
   * Check if the call is one of the acceptable mutable state functions, like `mutableStateOf` or
   * `mutableFloatStateOf`.
   */
  private fun isKnownMutableStateFunction(node: UElement, context: JavaContext): Boolean {
    val resolved = node.tryResolve()
    val isKnownMutableState =
      resolved is PsiMethod &&
        resolved.isInPackageName(ComposeRuntimePackageName) &&
        resolved.name in AcceptableMutableStateMethods
    return if (isKnownMutableState) {
      // Check the type of mutableStateOf()
      if (resolved.name == "mutableStateOf") {
        val call = node.asCall()
        val mutableClass = call?.returnType?.asClass()
        val typeIsAcceptable = mutableClass?.parameters?.all { it.isAcceptableType() } == true
        // Side effect to track custom policies
        if (!hasAcceptablePolicy(call, context) && call != null) {
          customPolices += call
        }
        typeIsAcceptable
      } else {
        // Known mutable[Primitive]StateOf()
        true
      }
    } else false
  }

  private class ReturnsTracker(tracked: UElement, var returned: Boolean = false) :
    DataFlowAnalyzer(listOf(tracked)) {
    override fun returns(expression: UReturnExpression) {
      returned = true
    }
  }
}

/**
 * Checks if the mutableStateOf call uses an acceptable SnapshotMutationPolicy. Acceptable policies
 * are: neverEqualPolicy, structuralEqualityPolicy, referentialEqualityPolicy
 */
private fun hasAcceptablePolicy(call: UCallExpression?, context: JavaContext): Boolean {
  val method = call?.resolve() ?: return true // Default policy is acceptable
  val arguments = context.evaluator.computeArgumentMapping(call, method)

  // Find the policy argument by parameter name
  val policyArgument =
    arguments
      .firstNotNullOfOrNull { (expression, parameter) ->
        if (parameter.name == "policy") expression else null
      }
      ?.skipParenthesizedExprDown()
      ?.tryResolveNamed()

  // Check if the policy is acceptable
  val isAcceptablePolicy =
    policyArgument is PsiMethod &&
      policyArgument.isInPackageName(ComposeRuntimePackageName) &&
      policyArgument.name in AcceptablePolicyMethods

  // If no policy is specified, assume its the default
  return policyArgument == null || isAcceptablePolicy
}

/**
 * > Lambdas in Kotlin implement Serializable, but will crash if you really try to save them. We
 * > check for both Function and Serializable (see kotlin.jvm.internal.Lambda) to support custom
 * > user defined classes implementing Function interface.
 *
 * https://github.com/androidx/androidx/blob/989d1e676252c69e1b9b2e0639c3dba039e7ac99/compose/ui/ui/src/androidMain/kotlin/androidx/compose/ui/platform/DisposableSaveableStateRegistry.android.kt#L122-L124
 */
private fun returnsLambdaExpression(returnType: PsiType, expression: UExpression): Boolean {
  val isFunction = returnType.asClass()?.resolve()?.implements("kotlin.Function") == true
  return if (isFunction) {
    val visitor = ReturnsLambdaVisitor()
    expression.accept(visitor)
    visitor.returnedLambda
  } else false
}

private class ReturnsLambdaVisitor(var returnedLambda: Boolean = false) : AbstractUastVisitor() {

  override fun visitReturnExpression(node: UReturnExpression): Boolean {
    val returnValue = node.returnExpression
    if (returnValue != null && isLambdaExpression(returnValue)) {
      returnedLambda = true
    }
    return super.visitReturnExpression(node)
  }

  private fun isLambdaExpression(expression: UExpression): Boolean {
    // Check if this is a lambda expression by looking at the expression type
    val expressionType = expression.getExpressionType()
    return expressionType?.asClass()?.resolve()?.implements("kotlin.Function") == true
  }
}

private fun PsiType.isAcceptableType(): Boolean {
  return when (this) {
    is PsiPrimitiveType -> true
    is PsiArrayType -> componentType.isAcceptableType()
    is PsiClassType -> {
      val resolved = resolve() ?: return true // Can't resolve class type treat as acceptable?
      resolved.isAcceptableClassType() && parameters.all { it.isAcceptableType() }
    }
    else -> false
  }
}

private fun PsiClass.isAcceptableClassType(): Boolean {
  return isBoxedPrimitive() || AcceptableClasses.any { implements(it) }
}

/**
 * Based on this set used by `canBeSavedToBundle()`:
 * ```
 * private val AcceptableClasses =
 *     arrayOf(
 *         Serializable::class.java,
 *         Parcelable::class.java,
 *         String::class.java,
 *         SparseArray::class.java,
 *         Binder::class.java,
 *         Size::class.java,
 *         SizeF::class.java,
 *     )
 * ```
 *
 * https://github.com/androidx/androidx/blob/989d1e676252c69e1b9b2e0639c3dba039e7ac99/compose/ui/ui/src/androidMain/kotlin/androidx/compose/ui/platform/DisposableSaveableStateRegistry.android.kt#L151-L160
 */
private val AcceptableClasses =
  setOf(
    "java.io.Serializable",
    "android.os.Parcelable",
    "java.lang.String",
    "android.util.SparseArray",
    "android.os.Binder",
    "android.util.Size",
    "android.util.SizeF",
  )

private val AcceptableMutableStateMethods =
  setOf(
    "mutableStateOf",
    "mutableIntStateOf",
    "mutableFloatStateOf",
    "mutableDoubleStateOf",
    "mutableLongStateOf",
  )

/**
 * Based on this check performed in `canBeSavedToBundle()`:
 * ```
 *   if (
 *     value.policy === neverEqualPolicy<Any?>() ||
 *       value.policy === structuralEqualityPolicy<Any?>() ||
 *       value.policy === referentialEqualityPolicy<Any?>()
 *   ) {
 * ```
 *
 * https://github.com/androidx/androidx/blob/989d1e676252c69e1b9b2e0639c3dba039e7ac99/compose/ui/ui/src/androidMain/kotlin/androidx/compose/ui/platform/DisposableSaveableStateRegistry.android.kt#L112-L114
 */
private val AcceptablePolicyMethods =
  setOf("neverEqualPolicy", "structuralEqualityPolicy", "referentialEqualityPolicy")
