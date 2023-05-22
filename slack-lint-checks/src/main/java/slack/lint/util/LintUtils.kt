// Copyright (C) 2021 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint.util

import com.android.tools.lint.client.api.Configuration
import com.android.tools.lint.client.api.JavaEvaluator
import com.android.tools.lint.client.api.TYPE_BOOLEAN_WRAPPER
import com.android.tools.lint.client.api.TYPE_BYTE_WRAPPER
import com.android.tools.lint.client.api.TYPE_CHARACTER_WRAPPER
import com.android.tools.lint.client.api.TYPE_DOUBLE_WRAPPER
import com.android.tools.lint.client.api.TYPE_FLOAT_WRAPPER
import com.android.tools.lint.client.api.TYPE_INTEGER_WRAPPER
import com.android.tools.lint.client.api.TYPE_LONG_WRAPPER
import com.android.tools.lint.client.api.TYPE_SHORT_WRAPPER
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.LintFix
import com.android.tools.lint.detector.api.ResourceXmlDetector
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.android.tools.lint.detector.api.StringOption
import com.android.tools.lint.detector.api.UastLintUtils
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiModifierListOwner
import com.intellij.psi.PsiType
import com.intellij.psi.PsiWildcardType
import java.util.EnumSet
import org.jetbrains.kotlin.asJava.classes.KtLightClassForFacade
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.UQualifiedReferenceExpression
import org.jetbrains.uast.UReferenceExpression
import org.jetbrains.uast.USimpleNameReferenceExpression
import org.jetbrains.uast.kotlin.KotlinUClass
import org.jetbrains.uast.tryResolve

/**
 * @param qualifiedName the qualified name of the desired interface type
 * @param nameFilter an optional name filter, used to check when to stop searching up the type
 *
 * ```
 *                   hierarchy. This is useful if you want to only check direct implementers in
 *                   certain packages. Called with a fully qualified class name; return false if
 *                   you want to stop searching up the type tree, true to continue.
 * ```
 */
internal fun PsiClass.implements(
  qualifiedName: String,
  nameFilter: (String) -> Boolean = { true }
): Boolean {
  val fqcn = this.qualifiedName ?: return false
  if (fqcn == qualifiedName) {
    // Found a match
    return true
  }

  if (!nameFilter(fqcn)) {
    // Don't proceed further
    return false
  }

  return this.superTypes.filterNotNull().any { classType ->
    classType.resolve()?.implements(qualifiedName, nameFilter) ?: false
  }
}

/**
 * @return whether or not the [this] is a Kotlin `companion object` type.
 * @see [isKotlinObject]
 */
internal fun UClass.isCompanionObject(evaluator: JavaEvaluator): Boolean {
  if (this is KotlinUClass && sourcePsi is KtObjectDeclaration && name == "Companion") {
    // best effort until we can update to lint tools 26.6.x. See below
    return true
  }

  return evaluator.hasModifier(this, KtTokens.COMPANION_KEYWORD)
}

/**
 * @return whether or not [this] is a Kotlin `object` type.
 * @see [isCompanionObject]
 */
internal fun UClass.isKotlinObject() = this is KotlinUClass && sourcePsi is KtObjectDeclaration

internal fun UClass.isKotlinTopLevelFacadeClass() =
  this is KotlinUClass && javaPsi is KtLightClassForFacade

/** @return whether or not [owner] is a Kotlin `value` class. */
internal fun JavaEvaluator.isValueClass(owner: PsiModifierListOwner?): Boolean {
  return hasModifier(owner, KtTokens.VALUE_KEYWORD)
}

internal fun UClass.isInnerClass(evaluator: JavaEvaluator): Boolean {
  // If it has no containing class, it's top-level and therefore not inner
  containingClass ?: return false

  // If it's static (i.e. in Java), it's not an inner class
  if (isStatic) return false

  // If it's Kotlin and "inner", then it's definitely an inner class
  if (this is KotlinUClass && evaluator.hasModifier(this, KtTokens.INNER_KEYWORD)) return true

  // We could check the containing class's innerClasses to look for a match here, but we've
  // logically ruled
  // out this possibility above
  return false
}

@Suppress("UnstableApiUsage", "SpreadOperator")
internal inline fun <reified T> sourceImplementation(
  shouldRunOnTestSources: Boolean = true
): Implementation where T : Detector, T : SourceCodeScanner {
  // We use the overloaded constructor that takes a varargs of `Scope` as the last param.
  // This is to enable on-the-fly IDE checks. We are telling lint to run on both
  // JAVA and TEST_SOURCES in the `scope` parameter but by providing the `analysisScopes`
  // params, we're indicating that this check can run on either JAVA or TEST_SOURCES and
  // doesn't require both of them together.
  // From discussion on lint-dev https://groups.google.com/d/msg/lint-dev/ULQMzW1ZlP0/1dG4Vj3-AQAJ
  // This was supposed to be fixed in AS 3.4 but still required as recently as 3.6-alpha10.
  return if (shouldRunOnTestSources) {
    Implementation(
      T::class.java,
      EnumSet.of(Scope.JAVA_FILE, Scope.TEST_SOURCES),
      EnumSet.of(Scope.JAVA_FILE),
      EnumSet.of(Scope.TEST_SOURCES)
    )
  } else {
    Implementation(T::class.java, EnumSet.of(Scope.JAVA_FILE))
  }
}

@Suppress("UnstableApiUsage", "SpreadOperator")
internal inline fun <reified T : ResourceXmlDetector> resourcesImplementation(): Implementation {
  return Implementation(T::class.java, Scope.RESOURCE_FILE_SCOPE)
}

/** Removes a given [node] as a fix. */
internal fun LintFix.Builder.removeNode(
  context: JavaContext,
  node: PsiElement,
  name: String? = null,
  autoFix: Boolean = true,
  text: String = node.text
): LintFix {
  val fixName = name ?: "Remove '$text'"
  return replace()
    .name(fixName)
    .range(context.getLocation(node))
    .shortenNames()
    .text(text)
    .with("")
    .apply {
      if (autoFix) {
        autoFix()
      }
    }
    .build()
}

internal fun String.snakeToCamel(): String {
  return buildString {
    var capNext = false
    var letterSeen = false
    for (c in this@snakeToCamel) {
      if (c == '_' || c == '-') {
        capNext = letterSeen
        continue
      } else {
        letterSeen = true
        if (capNext) {
          append(c.uppercaseChar())
          capNext = false
        } else {
          append(c)
        }
      }
    }
  }
}

@Suppress("ComplexCondition")
internal fun String.toScreamingSnakeCase(): String {
  return buildString {
    var prevWasLower = false
    var hasPendingUnderScore = false
    var letterSeen = false
    for (c in this@toScreamingSnakeCase) {
      if (c == '_') {
        if (letterSeen) {
          hasPendingUnderScore = true
        }
        continue
      } else if (c == '-' || c == '.' || c == ':' || c == '/') {
        // Wild west characters in enum member names
        // TODO maybe we should report these
        if (letterSeen) {
          hasPendingUnderScore = true
        }
      } else {
        letterSeen = true
        if (hasPendingUnderScore) {
          append('_')
        }
        hasPendingUnderScore = false
        if (c.isUpperCase()) {
          if (prevWasLower) {
            prevWasLower = false
            append('_')
          }
          append(c)
        } else {
          prevWasLower = true
          append(c.uppercaseChar())
        }
      }
    }
  }
}

/** List of platform types that Moshi's reflective adapter refuses. From ClassJsonAdapter. */
private val PLATFORM_PACKAGES =
  setOf("android", "androidx", "java", "javax", "kotlin", "kotlinx", "scala")

private val BOXED_PRIMITIVES =
  setOf(
    TYPE_INTEGER_WRAPPER,
    TYPE_BOOLEAN_WRAPPER,
    TYPE_BYTE_WRAPPER,
    TYPE_SHORT_WRAPPER,
    TYPE_LONG_WRAPPER,
    TYPE_DOUBLE_WRAPPER,
    TYPE_FLOAT_WRAPPER,
    TYPE_CHARACTER_WRAPPER
  )

internal fun PsiClass.isBoxedPrimitive(): Boolean {
  val fqcn = qualifiedName ?: return false
  return fqcn in BOXED_PRIMITIVES
}

internal fun PsiClass.isString(): Boolean {
  val fqcn = qualifiedName ?: return false
  return fqcn == "java.lang.String"
}

internal fun PsiClass.isObjectOrAny(): Boolean {
  val fqcn = qualifiedName ?: return false
  return fqcn == "java.lang.Object"
}

internal fun PsiClass.isPlatformType(): Boolean {
  val fqcn = qualifiedName ?: return false
  val firstPackagePart = fqcn.substringBefore(".")
  return firstPackagePart in PLATFORM_PACKAGES
}

/**
 * Given reference expressions, try to unwrap the simple name expression (useful if the reference is
 * always the same type, like an enum).
 *
 * `Foo.BAR` -> BAR `BAR` -> BAR
 */
internal fun UExpression.unwrapSimpleNameReferenceExpression(): USimpleNameReferenceExpression {
  return when (this) {
    is USimpleNameReferenceExpression -> this
    is UQualifiedReferenceExpression -> this.selector.unwrapSimpleNameReferenceExpression()
    else -> error("Unrecognized reference expression type $this")
  }
}

/**
 * Returns the fully qualified name of the expression, or null if unknown.
 *
 * For example, given:
 * ```
 * import org.x.Clazz.CONSTANT
 * ...
 *     if (aVar == CONSTANT)
 *                 ^^^^^^^^
 * ```
 *
 * The qualified name of the underlined expression will be "org.x.Clazz.CONSTANT".
 */
internal fun UExpression.resolveQualifiedNameOrNull(): String? {
  return (this as? UReferenceExpression)?.referenceNameElement?.uastParent?.tryResolve()?.let {
    UastLintUtils.getQualifiedName(it)
  }
}

/**
 * Collects the return type of this [UMethod] in a suspend-safe way.
 *
 * For coroutines, the suspend methods return context rather than the source-declared return type,
 * which is encoded in a continuation parameter at the end of the parameter list.
 *
 * For example, the following snippet:
 * ```
 * suspend fun foo(): String
 * ```
 *
 * Will appear like so to lint:
 * ```
 * Object foo(Continuation<? super String> continuation)
 * ```
 */
internal fun UMethod.safeReturnType(context: JavaContext): PsiType? {
  if (language == KotlinLanguage.INSTANCE && context.evaluator.isSuspend(this)) {
    val classReference = parameterList.parameters.lastOrNull()?.type as? PsiClassType ?: return null
    val wildcard = classReference.parameters.singleOrNull() as? PsiWildcardType ?: return null
    return wildcard.bound
  } else {
    return returnType
  }
}

/** Loads a [StringOption] as a [delimiter]-delimited [Set] of strings. */
internal fun StringOption.loadAsSet(
  configuration: Configuration,
  delimiter: String = ","
): Set<String> {
  return getValue(configuration)
    ?.splitToSequence(delimiter)
    .orEmpty()
    .map(String::trim)
    .filter(String::isNotBlank)
    .toSet()
}
