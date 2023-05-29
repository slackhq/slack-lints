// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint.util

import com.android.tools.lint.client.api.JavaEvaluator
import com.android.tools.lint.model.LintModelDependencies
import com.intellij.lang.jvm.JvmClassKind
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiArrayInitializerMemberValue
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiCompiledElement
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiModifierListOwner
import com.intellij.psi.PsiPackage
import com.intellij.psi.PsiType
import java.util.Optional
import java.util.concurrent.ConcurrentHashMap
import kotlin.jvm.optionals.getOrNull
import kotlinx.metadata.Flag
import kotlinx.metadata.KmClass
import kotlinx.metadata.jvm.KotlinClassMetadata
import kotlinx.metadata.jvm.Metadata as MetadataWithNullableArgs
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.uast.UAnnotated
import org.jetbrains.uast.UAnnotation
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.findContaining
import org.jetbrains.uast.toUElementOfType

/**
 * A delegating [JavaEvaluator] that implements more comprehensive checks for Kotlin classes via
 * metadata annotations.
 *
 * This is important because, when `checkDependencies` is set to false, Lint detectors cannot see
 * Kotlin language features in externally-compiled elements. This means that constructs like `data
 * classes` or similar are not visible. Using kotlinx-metadata, we can parse the [Metadata]
 * annotations on the containing classes and read these language features from them.
 */
@Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
class MetadataJavaEvaluator(private val file: String, private val delegate: JavaEvaluator) :
  JavaEvaluator() {

  private companion object {
    // Not an exhaustive list, but at least the ones we look at currently
    private val KOTLIN_METADATA_TOKENS =
      mapOf(
        KtTokens.DATA_KEYWORD to TokenData(Flag.Class.IS_DATA),
        KtTokens.SEALED_KEYWORD to
          TokenData(
            Flag.IS_SEALED,
            applicableClassKinds = setOf(JvmClassKind.CLASS, JvmClassKind.INTERFACE)
          ),
        KtTokens.OBJECT_KEYWORD to TokenData(Flag.Class.IS_OBJECT),
        KtTokens.COMPANION_KEYWORD to TokenData(Flag.Class.IS_COMPANION_OBJECT),
        KtTokens.VALUE_KEYWORD to TokenData(Flag.Class.IS_VALUE),
      )
  }

  private data class TokenData(
    val flag: Flag,
    val applicableClassKinds: Set<JvmClassKind> = setOf(JvmClassKind.CLASS)
  )

  /** Flag to disable as needed. */
  private val checkMetadata = System.getProperty("slack.lint.checkMetadata", "true").toBoolean()
  private val cachedClasses = ConcurrentHashMap<String, Optional<KmClass>>()

  // region Delegating functions
  override val dependencies: LintModelDependencies?
    get() = delegate.dependencies

  override fun extendsClass(cls: PsiClass?, className: String, strict: Boolean): Boolean =
    delegate.extendsClass(cls, className, strict)

  override fun findAnnotation(
    listOwner: PsiModifierListOwner?,
    vararg annotationNames: String
  ): PsiAnnotation? = delegate.findAnnotation(listOwner, *annotationNames)

  override fun findAnnotationInHierarchy(
    listOwner: PsiModifierListOwner,
    vararg annotationNames: String
  ): PsiAnnotation? = delegate.findAnnotationInHierarchy(listOwner, *annotationNames)

  override fun findClass(qualifiedName: String): PsiClass? = delegate.findClass(qualifiedName)

  override fun findJarPath(element: PsiElement): String? = delegate.findJarPath(element)

  override fun findJarPath(element: UElement): String? = delegate.findJarPath(element)

  override fun getAllAnnotations(
    owner: PsiModifierListOwner,
    inHierarchy: Boolean
  ): Array<PsiAnnotation> = delegate.getAllAnnotations(owner, inHierarchy)

  override fun getAllAnnotations(owner: UAnnotated, inHierarchy: Boolean): List<UAnnotation> =
    delegate.getAllAnnotations(owner, inHierarchy)

  override fun getAnnotation(
    listOwner: PsiModifierListOwner?,
    vararg annotationNames: String
  ): UAnnotation? = delegate.getAnnotation(listOwner, *annotationNames)

  override fun getAnnotationInHierarchy(
    listOwner: PsiModifierListOwner,
    vararg annotationNames: String
  ): UAnnotation? = delegate.getAnnotationInHierarchy(listOwner, *annotationNames)

  override fun getAnnotations(
    owner: PsiModifierListOwner?,
    inHierarchy: Boolean,
    parent: UElement?
  ): List<UAnnotation> = delegate.getAnnotations(owner, inHierarchy, parent)

  override fun getClassType(psiClass: PsiClass?): PsiClassType? = delegate.getClassType(psiClass)

  override fun getPackage(node: PsiElement): PsiPackage? = delegate.getPackage(node)

  override fun getPackage(node: UElement): PsiPackage? = delegate.getPackage(node)

  override fun getTypeClass(psiType: PsiType?): PsiClass? = delegate.getTypeClass(psiType)

  override fun implementsInterface(cls: PsiClass, interfaceName: String, strict: Boolean): Boolean =
    delegate.implementsInterface(cls, interfaceName, strict)
  // endregion

  /** Deep isObject check that checks if the given [cls] */
  fun isObject(cls: PsiClass?): Boolean {
    if (cls == null) return false

    cls.toUElementOfType<UClass>()?.let { uClass ->
      if (uClass.sourcePsi is KtObjectDeclaration) {
        return true
      } else if (canCheckMetadata(cls)) {
        val (flag, applicableClassKinds) = KOTLIN_METADATA_TOKENS.getValue(KtTokens.OBJECT_KEYWORD)
        if (uClass.classKind in applicableClassKinds) {
          uClass.getOrParseMetadata()?.let { kmClass ->
            return flag(kmClass.flags)
          }
        }
      }
    }
    return false
  }

  private fun canCheckMetadata(element: PsiElement): Boolean {
    return checkMetadata && element is PsiCompiledElement
  }

  override fun hasModifier(owner: PsiModifierListOwner?, keyword: KtModifierKeywordToken): Boolean {
    val superValue = super.hasModifier(owner, keyword)
    // If it's not a compiled element or not a PsiClass, trust the super value and move on
    if (owner !is PsiClass || !canCheckMetadata(owner)) {
      return superValue
    }

    // We're working with an externally compiled element and it's a PsiClass, so we can do more
    // thorough checks here.
    KOTLIN_METADATA_TOKENS[keyword]?.let { (flag, applicableClassKinds) ->
      owner.findContaining(UClass::class.java)?.let { cls ->
        // Only parse if the target class kind is applicable to the token we're checking. For
        // example - when checking `data` tokens, they're not applicable to interfaces or enums.
        if (cls.classKind in applicableClassKinds) {
          cls.getOrParseMetadata()?.let { kmClass ->
            return flag(kmClass.flags)
          }
        }
      }
    }

    return superValue
  }

  private fun UAnnotated.getOrParseMetadata(): KmClass? {
    val cls =
      when (this) {
        is UClass -> this
        else -> return null // Only classes are supported right now
      }
    return cachedClasses
      // Don't use getOrPut. Kotlin's extension may still invoke the body and we don't want that
      .computeIfAbsent(qualifiedName!!) { key ->
        val annotation =
          cls.findAnnotation("kotlin.Metadata") ?: return@computeIfAbsent Optional.empty()
        val (durationMillis, metadata) =
          measureTimeMillisWithResult { annotation.parseMetadata(key) }
        slackLintLog("Took ${durationMillis}ms to parse metadata for $key.")
        Optional.ofNullable(metadata)
      }
      .getOrNull()
  }

  private fun UAnnotation.parseMetadata(classNameHint: String): KmClass? {
    return when (val parsedMetadata = KotlinClassMetadata.read(toMetadataAnnotation())) {
      is KotlinClassMetadata.Class -> {
        parsedMetadata.toKmClass().also {
          slackLintLog("Loaded KmClass for $classNameHint from file $file")
        }
      }
      else -> {
        if (parsedMetadata == null) {
          // Extremely weird case, log this specifically
          slackLintLog("Could not load metadata for $classNameHint from file $file")
        } else {
          slackLintLog(
            """
              Could not load KmClass for $classNameHint from file $file.
              Metadata was $parsedMetadata
            """
              .trimIndent()
          )
        }
        null
      }
    }
  }

  private fun UAnnotation.toMetadataAnnotation(): Metadata {
    return MetadataWithNullableArgs(
      kind = findAttributeValue("k")?.parseIntMember(),
      metadataVersion = findAttributeValue("mv")?.parseIntArray(),
      data1 = findAttributeValue("d1")?.parseStringArray(),
      data2 = findAttributeValue("d2")?.parseStringArray(),
      extraString = findAttributeValue("xs")?.parseStringMember(),
      packageName = findAttributeValue("pn")?.parseStringMember(),
      extraInt = findAttributeValue("xi")?.parseIntMember(),
    )
  }

  private val PsiLiteralExpression.intValue: Int
    get() = stringValue.toInt()

  private val PsiLiteralExpression.stringValue: String
    get() = value.toString()

  private fun UExpression.parseIntMember() = (sourcePsi as PsiLiteralExpression).intValue

  private fun UExpression.parseStringMember() = (sourcePsi as PsiLiteralExpression).stringValue

  private fun UExpression.parseStringArray() =
    (sourcePsi as PsiArrayInitializerMemberValue).initializers.mapArray { value ->
      (value as PsiLiteralExpression).stringValue
    }

  private fun UExpression.parseIntArray(): IntArray {
    val initializers = (sourcePsi as PsiArrayInitializerMemberValue).initializers
    return IntArray(initializers.size) { index ->
      (initializers[index] as PsiLiteralExpression).intValue
    }
  }
}
