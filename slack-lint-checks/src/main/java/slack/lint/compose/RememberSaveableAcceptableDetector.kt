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
import org.jetbrains.uast.visitor.AbstractUastVisitor
import slack.lint.util.Package
import slack.lint.util.implements
import slack.lint.util.isBoxedPrimitive
import slack.lint.util.isInPackageName
import slack.lint.util.sourceImplementation

private const val REMEMBER_SAVEABLE_METHOD_NAME = "rememberSaveable"

private val ComposeRuntimePackageName = Package("androidx.compose.runtime")
private val RememberSaveablePackageName = Package("androidx.compose.runtime.saveable")
private val AUTO_SAVER = UastEmptyExpression(null)

class RememberSaveableAcceptableDetector : Detector(), SourceCodeScanner {

  override fun getApplicableUastTypes(): List<Class<out UElement>> =
    listOf(UCallExpression::class.java)

  override fun createUastHandler(context: JavaContext) =
    object : UElementHandler() {
      @Suppress("ReturnCount")
      override fun visitCallExpression(node: UCallExpression) {
        if (node.methodName != REMEMBER_SAVEABLE_METHOD_NAME) return
        val evaluator = context.evaluator
        val method = node.resolve()
        val returnType = node.returnType
        if (
          method == null ||
            returnType == null ||
            !method.isInPackageName(RememberSaveablePackageName)
        ) {
          return
        }

        val arguments = evaluator.computeArgumentMapping(node, method)
        val saver = arguments.getSaver()
        // With an auto saver check the return type.
        if (saver == AUTO_SAVER && returnType.isAcceptableType()) {
          return
        }
        // If there is no init expression just return.
        val init = arguments.getInit() ?: return
        // Check whats created in the init expression.
        if (returnsKnownMutableState(returnType, init, context)) {
          // Found a known parcelable mutable state.
          return
        }
        if (returnsLambdaExpression(returnType, init)) {
          // todo Report specific error language about kotlin Lambdas
          context.report(
            ISSUE,
            context.getLocation(node),
            ISSUE.getBriefDescription(TextFormat.TEXT) + " (LAMBDA)",
          )
          return
        }
        context.report(ISSUE, context.getLocation(node), ISSUE.getBriefDescription(TextFormat.TEXT))
      }
    }

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

private fun Map<UExpression, PsiParameter>.getSaver(): UExpression {
  val saver = firstNotNullOfOrNull { (expression, parameter) ->
    if (parameter.name == "saver") expression else null
  }
  val resolved = saver?.tryResolve()
  if (
    resolved is PsiMethod &&
      resolved.name == "autoSaver" &&
      resolved.isInPackageName(RememberSaveablePackageName)
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
): Boolean {
  return if (returnType.isAcceptableMutableStateClass()) {
    val visitor = MutableStateOfVisitor(context)
    expression.accept(visitor)
    val allReturned =
      visitor.mutableStateOfs.isNotEmpty() &&
        visitor.mutableStateOfs.all { tracked ->
          val returnsTracker = ReturnsTracker(tracked)
          expression.accept(returnsTracker)
          returnsTracker.returned
        }
    allReturned
  } else false
}

private fun PsiType?.isAcceptableMutableStateClass(): Boolean {
  val psiClassType = asClass()
  val isMutableState =
    psiClassType
      ?.resolve()
      ?.implements("${ComposeRuntimePackageName.javaPackageName}.MutableState") == true
  return isMutableState && psiClassType.parameters.all { it.isAcceptableType() }
}

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
      val policyIsAcceptable = hasAcceptablePolicy(call, context)
      typeIsAcceptable && policyIsAcceptable
    } else {
      // Known mutable[Primitive]StateOf()
      true
    }
  } else false
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
        if (parameter.name == "policy") expression.skipParenthesizedExprDown() else null
      }
      ?.tryResolveNamed() ?: return true // If no policy is specified, assume its the default

  return policyArgument is PsiMethod &&
    policyArgument.isInPackageName(ComposeRuntimePackageName) &&
    policyArgument.name in AcceptablePolicyMethods
}

private class MutableStateOfVisitor(private val context: JavaContext) : AbstractUastVisitor() {

  val mutableStateOfs = mutableListOf<UCallExpression>()

  override fun visitCallExpression(node: UCallExpression): Boolean =
    if (isKnownMutableStateFunction(node, context)) {
      mutableStateOfs.add(node)
      true
    } else false
}

private class ReturnsTracker(tracked: UElement, var returned: Boolean = false) :
  DataFlowAnalyzer(listOf(tracked)) {
  override fun returns(expression: UReturnExpression) {
    returned = true
  }
}

/**
 * > Lambdas in Kotlin implement Serializable, but will crash if you really try to save them. We
 * > check for both Function and Serializable (see kotlin.jvm.internal.Lambda) to support custom
 * > user defined classes implementing Function interface.
 * - From:
 *   https://github.com/androidx/androidx/blob/989d1e676252c69e1b9b2e0639c3dba039e7ac99/compose/ui/ui/src/androidMain/kotlin/androidx/compose/ui/platform/DisposableSaveableStateRegistry.android.kt#L122-L124
 */
private fun returnsLambdaExpression(returnType: PsiType, expression: UExpression): Boolean {
  val isFunction = returnType.asClass()?.resolve()?.implements("kotlin.Function") == true

  // Find all the function returns and see if any are lambda expressions
  val visitor = ReturnsLambdaVisitor()
  expression.accept(visitor)

  // Return true if the return type is a function AND we found lambda returns
  return isFunction && visitor.returnedLambda
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

private fun PsiType?.asClass(): PsiClassType? = this as? PsiClassType

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

// From
// https://github.com/androidx/androidx/blob/989d1e676252c69e1b9b2e0639c3dba039e7ac99/compose/ui/ui/src/androidMain/kotlin/androidx/compose/ui/platform/DisposableSaveableStateRegistry.android.kt#L108
/*
/** Checks that [value] can be stored inside [Bundle]. */
private fun canBeSavedToBundle(value: Any): Boolean {
    // SnapshotMutableStateImpl is Parcelable, but we do extra checks
    if (value is SnapshotMutableState<*>) {
        if (
            value.policy === neverEqualPolicy<Any?>() ||
                value.policy === structuralEqualityPolicy<Any?>() ||
                value.policy === referentialEqualityPolicy<Any?>()
        ) {
            val stateValue = value.value
            return if (stateValue == null) true else canBeSavedToBundle(stateValue)
        } else {
            return false
        }
    }
    // lambdas in Kotlin implement Serializable, but will crash if you really try to save them.
    // we check for both Function and Serializable (see kotlin.jvm.internal.Lambda) to support
    // custom user defined classes implementing Function interface.
    if (value is Function<*> && value is Serializable) {
        return false
    }
    for (cl in AcceptableClasses) {
        if (cl.isInstance(value)) {
            return true
        }
    }
    return false
}
 */

/*
 * From: https://github.com/androidx/androidx/blob/989d1e676252c69e1b9b2e0639c3dba039e7ac99/compose/ui/ui/src/androidMain/kotlin/androidx/compose/ui/platform/DisposableSaveableStateRegistry.android.kt#L151
 *
 * Contains Classes which can be stored inside [Bundle].
 *
 * Some of the classes are not added separately because:
 *
 * This classes implement Serializable:
 * - Arrays (DoubleArray, BooleanArray, IntArray, LongArray, ByteArray, FloatArray, ShortArray,
 *   CharArray, Array<Parcelable, Array<String>)
 * - ArrayList
 * - Primitives (Boolean, Int, Long, Double, Float, Byte, Short, Char) will be boxed when casted to
 *   Any, and all the boxed classes implements Serializable. This class implements Parcelable:
 * - Bundle
 *
 * Note: it is simplified copy of the array from SavedStateHandle (lifecycle-viewmodel-savedstate).
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

private val AcceptablePolicyMethods =
  setOf("neverEqualPolicy", "structuralEqualityPolicy", "referentialEqualityPolicy")
