// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.android.tools.lint.detector.api.TextFormat
import com.intellij.psi.PsiCapturedWildcardType
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiPrimitiveType
import com.intellij.psi.PsiType
import com.intellij.psi.PsiWildcardType
import com.intellij.psi.util.PsiTypesUtil
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.uast.UCallExpression
import slack.lint.moshi.MoshiLintUtil.hasMoshiAnnotation
import slack.lint.util.sourceImplementation

/**
 * A detector that checks if a type passed into the JsonInflater.inflate/deflate methods follows
 * Moshi's requirements for serialization/deserialization.
 *
 * `JsonInflater` is a JSON serialization indirection we have internally at Slack.
 */
class JsonInflaterMoshiCompatibilityDetector : Detector(), SourceCodeScanner {

  override fun getApplicableUastTypes() = listOf(UCallExpression::class.java)

  override fun createUastHandler(context: JavaContext): UElementHandler {
    return object : UElementHandler() {
      override fun visitCallExpression(node: UCallExpression) {
        val method = node.resolve() ?: return

        if (isJsonInflaterInflateOrDeflate(node)) {
          when (method.name) {
            "inflate" -> validateInflateReturnType(context, node)
            "deflate" -> validateDeflateArguments(context, node)
          }
        }
      }
    }
  }

  private fun isJsonInflaterInflateOrDeflate(node: UCallExpression): Boolean {
    // Get the method being called
    val method = node.resolve() ?: return false

    // Check if it's from JsonInflater class
    val containingClass = method.containingClass?.qualifiedName ?: return false
    if (containingClass != FQCN_JSON_INFLATER) return false

    // Check if it's an inflate or deflate method
    return method.name == "inflate" || method.name == "deflate"
  }

  private fun validateInflateReturnType(context: JavaContext, node: UCallExpression) {
    // Get the return type of the inflate call
    val returnType = node.getExpressionType() ?: return

    // Skip checking primitive types and String
    if (isJsonPrimitive(returnType)) return

    // Validate if the class is Moshi-compatible
    validateClassForMoshiCompatibility(context, node, returnType)
  }

  private fun isJsonPrimitive(type: PsiType): Boolean {
    val isString =
      if (type is PsiClassType) {
        type.resolve()?.qualifiedName == FQCN_JAVA_STRING
      } else {
        false
      }
    return type is PsiPrimitiveType || isString
  }

  private fun validateDeflateArguments(context: JavaContext, node: UCallExpression) {
    val method = node.resolve() ?: return

    val parameters = method.parameterList.parameters
    val valueParamIndex = parameters.indexOfFirst { it.name == "value" }
    if (valueParamIndex == -1) return

    val valueArg = node.getArgumentForParameter(valueParamIndex) ?: return
    val valueArgType = valueArg.getExpressionType() ?: return

    validateClassForMoshiCompatibility(context, node, valueArgType)
  }

  private fun validateClassForMoshiCompatibility(
    context: JavaContext,
    node: UCallExpression,
    typeToValidate: PsiType,
  ) {
    val modelClasses = extractModelClasses(typeToValidate)

    if (modelClasses.any { !isMoshiCompatible(it) }) {
      context.report(
        issue = ISSUE,
        location = context.getLocation(node),
        message = ISSUE.getBriefDescription(TextFormat.TEXT),
      )
    }
  }

  private fun extractModelClasses(type: PsiType): List<PsiClass> {
    val result = mutableListOf<PsiClass>()

    fun processType(t: PsiType) {
      val unwrapped =
        when (t) {
          is PsiWildcardType -> t.bound ?: return
          is PsiCapturedWildcardType -> t.wildcard.bound ?: return
          else -> t
        }

      val psiClass = PsiTypesUtil.getPsiClass(unwrapped)
      if (psiClass != null) {
        result.add(psiClass)
      }

      if (unwrapped is PsiClassType) {
        for (param in unwrapped.parameters) {
          processType(param)
        }
      }
    }

    processType(type)
    return result
  }

  private fun isMoshiCompatible(psiClass: PsiClass): Boolean {
    if (isPrimitiveType(psiClass)) return true

    if (isCollectionType(psiClass)) return true

    if (isAbstractOrNonPublicClass(psiClass)) return false

    if (psiClass.isInterface && !isSealedInterface(psiClass)) return false

    return psiClass.hasMoshiAnnotation()
  }

  private fun isCollectionType(psiClass: PsiClass): Boolean {
    val qualifiedName = psiClass.qualifiedName ?: return false
    return qualifiedName in listOf(FQCN_LIST, FQCN_SET, FQCN_MAP, FQCN_COLLECTION)
  }

  private fun isPrimitiveType(psiClass: PsiClass): Boolean {
    val qualifiedName = psiClass.qualifiedName ?: return false
    return qualifiedName in
      listOf(
        FQCN_JAVA_STRING,
        FQCN_JAVA_BOOLEAN,
        FQCN_JAVA_BYTE,
        FQCN_JAVA_CHARACTER,
        FQCN_JAVA_SHORT,
        FQCN_JAVA_INTEGER,
        FQCN_JAVA_LONG,
        FQCN_JAVA_FLOAT,
        FQCN_JAVA_DOUBLE,
      )
  }

  private fun isAbstractOrNonPublicClass(psiClass: PsiClass): Boolean {
    return !psiClass.isInterface &&
      (psiClass.hasModifierProperty(PsiModifier.ABSTRACT) ||
        !psiClass.hasModifierProperty(PsiModifier.PUBLIC))
  }

  private fun isSealedInterface(psiClass: PsiClass): Boolean {
    if (!psiClass.isInterface) return false

    // For Kotlin classes, check using Kotlin PSI
    if (psiClass is KtLightClass) {
      val ktClass = psiClass.kotlinOrigin
      return ktClass?.hasModifier(KtTokens.SEALED_KEYWORD) == true
    }

    // Fallback for Java classes
    return psiClass.hasModifierProperty(PsiModifier.SEALED)
  }

  companion object {
    // Fully qualified class names for relevant annotations and types
    private const val FQCN_JSON_INFLATER = "slack.commons.json.JsonInflater"
    private const val FQCN_JAVA_STRING = "java.lang.String"
    private const val FQCN_JAVA_BOOLEAN = "java.lang.Boolean"
    private const val FQCN_JAVA_BYTE = "java.lang.Byte"
    private const val FQCN_JAVA_CHARACTER = "java.lang.Character"
    private const val FQCN_JAVA_SHORT = "java.lang.Short"
    private const val FQCN_JAVA_INTEGER = "java.lang.Integer"
    private const val FQCN_JAVA_LONG = "java.lang.Long"
    private const val FQCN_JAVA_FLOAT = "java.lang.Float"
    private const val FQCN_JAVA_DOUBLE = "java.lang.Double"
    private const val FQCN_LIST = "java.util.List"
    private const val FQCN_SET = "java.util.Set"
    private const val FQCN_MAP = "java.util.Map"
    private const val FQCN_COLLECTION = "java.util.Collection"

    val ISSUE =
      Issue.create(
        id = "JsonInflaterMoshiIncompatibleType",
        "Using JsonInflater.inflate/deflate with a Moshi-incompatible type.",
        """
          Classes used with JsonInflater.inflate/deflate must be annotated with @JsonClass or @AdaptedBy to make it \
          compatible with Moshi. Additionally, it cannot be an abstract class or an interface.
        """,
        Category.CORRECTNESS,
        6,
        Severity.ERROR,
        implementation = sourceImplementation<JsonInflaterMoshiCompatibilityDetector>(),
      )
  }
}
