// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint.compose

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.JavaContext.Companion.getMethodName
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
import org.jetbrains.kotlin.asJava.elements.KtLightMember
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.uast.UBlockExpression
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.ULambdaExpression
import org.jetbrains.uast.UReturnExpression
import org.jetbrains.uast.kotlin.KotlinUImplicitReturnExpression
import org.jetbrains.uast.tryResolve
import slack.lint.util.Package
import slack.lint.util.implements
import slack.lint.util.isBoxedPrimitive
import slack.lint.util.isInPackageName
import slack.lint.util.sourceImplementation

private val RememberSaveablePackageName = Package("androidx.compose.runtime.saveable")
private const val RememberSaveableMethodName = "rememberSaveable"

// todo Rewrite this so its checking this instead
// Android only source set
// rememberSaveable with autoSaver -> Check value type is in "AcceptableClasses"
// rememberSaveable with custom saver -> Check Saver.save is in in "AcceptableClasses"

class RememberSaveableAcceptableDetector : Detector(), SourceCodeScanner {

  override fun getApplicableUastTypes(): List<Class<out UElement>> =
    listOf(UCallExpression::class.java)

  override fun createUastHandler(context: JavaContext) =
    object : UElementHandler() {
      override fun visitCallExpression(node: UCallExpression) {
        if (node.methodName != RememberSaveableMethodName) return
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

        // todo Check for custom saver or included saver, null is autoSaver
        val saver = arguments.getSaver()
        val initReturnType = arguments.getInit()
        // Auto saver checks
        if (saver == null && returnType.isAcceptableType()) {
          return
        }
        val source = saver?.sourcePsi
        if (saver != null && source != null) {
          val resolve = context.evaluator.resolve(source)
          saver.getExpressionType()
          getMethodName(saver)
        }
        context.report(ISSUE, context.getLocation(node), ISSUE.getBriefDescription(TextFormat.TEXT))
      }
    }

  companion object {
    const val MESSAGE = "remember"
    const val ISSUE_ID = "RememberSaveableTypeMustBeAcceptable"
    const val BRIEF_DESCRIPTION = "todo"
    const val EXPLANATION = """todo"""

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

private fun Map<UExpression, PsiParameter>.getSaver(): UExpression? {
  val saver = firstNotNullOfOrNull { (expression, parameter) ->
    if (parameter.name == "saver") {
      expression
    } else null
  }
  val resolved = saver?.tryResolve()
  if (
    resolved is PsiMethod &&
      resolved.name == "autoSaver" &&
      resolved.isInPackageName(RememberSaveablePackageName)
  ) {
    return null
  }
  return saver
}

private fun Map<UExpression, PsiParameter>.getInit(): UExpression? {
  val init = firstNotNullOfOrNull { (expression, parameter) ->
    if (parameter.name == "init") {
      expression
    } else null
  }
  resolveLambdaType(init)
  //  return valueArguments.filterIsInstance<ValueArgument>().find { arg ->
  //    arg.getArgumentName()?.referenceExpression?.getReferencedName() == "init"
  //  }
  return init
}

private fun resolveLambdaType(init: UExpression?) {
  if (init is ULambdaExpression) {
    val body = init.body
    when (body) {
      is UBlockExpression -> {
        val returnTypes = mutableListOf<UExpression>()
        for (expresssion in body.expressions) {
          if (expresssion is UReturnExpression) {
            val returnExpression = expresssion.returnExpression
            if (returnExpression is UCallExpression) {
              val resolved = returnExpression.resolve()
              if (resolved is KtLightMethod) {
                resolved.body
              }
            }
            // todo Handle objects
          }
        }
      }
    }
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

fun PsiClass.isAcceptableClassType(): Boolean {
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

// From
// https://github.com/androidx/androidx/blob/989d1e676252c69e1b9b2e0639c3dba039e7ac99/compose/ui/ui/src/androidMain/kotlin/androidx/compose/ui/platform/DisposableSaveableStateRegistry.android.kt#L151
/**
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
  arrayOf(
    "java.io.Serializable",
    "android.os.Parcelable",
    "java.lang.String",
    "android.util.SparseArray",
    "android.os.Binder",
    "android.util.Size",
    "android.util.SizeF",
  )
