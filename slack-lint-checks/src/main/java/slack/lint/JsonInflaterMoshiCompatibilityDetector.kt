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
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiPrimitiveType
import com.intellij.psi.PsiType
import org.jetbrains.uast.UCallExpression
import slack.lint.util.sourceImplementation

/**
 * A detector that checks if a type passed into the JsonInflater.inflate/deflate methods follows
 * Moshi's requirements for serialization/deserialization.
 */
class JsonInflaterMoshiCompatibilityDetector : Detector(), SourceCodeScanner {

  override fun getApplicableUastTypes() = listOf(UCallExpression::class.java)

  override fun createUastHandler(context: JavaContext): UElementHandler {
    return object : UElementHandler() {
      override fun visitCallExpression(node: UCallExpression) {
        if (isJsonInflaterInflateOrDeflate(node)) {
          when (node.methodName) {
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
    if (isPrimitiveOrString(returnType)) return

    // Get the class for the return type
    val returnClass = context.evaluator.getTypeClass(returnType) ?: return

    // Validate if the class is Moshi-compatible
    validateClassForMoshiCompatibility(context, node, returnClass)
  }

  private fun isPrimitiveOrString(type: PsiType): Boolean {
    return type is PsiPrimitiveType || type.canonicalText == "java.lang.String"
  }

  private fun validateDeflateArguments(context: JavaContext, node: UCallExpression) {
    val method = node.resolve() ?: return
    val parameters = method.parameterList.parameters
    val arguments = node.valueArguments

    for ((index, parameter) in parameters.withIndex()) {
      if (parameter.name == "value" && index < arguments.size) {
        val objectArgument = arguments[index]
        // Get the type of the object being deflated
        val objectType = objectArgument.getExpressionType() ?: return
        val objectClass = context.evaluator.getTypeClass(objectType) ?: return

        validateClassForMoshiCompatibility(context, node, objectClass)
      }
    }
  }

  private fun validateClassForMoshiCompatibility(
    context: JavaContext,
    node: UCallExpression,
    classToValidate: PsiClass,
  ) {
    if (!isMoshiCompatible(classToValidate)) {
      context.report(
        issue = ISSUE_JSON_INFLATER_WITH_MOSHI_INCOMPATIBLE_TYPE,
        location = context.getLocation(node),
        message =
          ISSUE_JSON_INFLATER_WITH_MOSHI_INCOMPATIBLE_TYPE.getBriefDescription(TextFormat.TEXT),
      )
    }
  }

  private fun isMoshiCompatible(psiClass: PsiClass): Boolean {
    if (!isInstantiable(psiClass)) return false

    return hasPublicNoArgConstructor(psiClass) ||
      hasJsonClassGenerateAdapter(psiClass) ||
      hasAdaptedBy(psiClass)
  }

  private fun isInstantiable(psiClass: PsiClass): Boolean {
    return !psiClass.isInterface &&
      !psiClass.hasModifierProperty(PsiModifier.ABSTRACT) &&
      !psiClass.isEnum &&
      psiClass.hasModifierProperty(PsiModifier.PUBLIC)
  }

  private fun hasPublicNoArgConstructor(psiClass: PsiClass): Boolean {
    return psiClass.constructors.any { constructor ->
      constructor.parameterList.parametersCount == 0 &&
        constructor.hasModifierProperty(PsiModifier.PUBLIC)
    }
  }

  private fun hasJsonClassGenerateAdapter(psiClass: PsiClass): Boolean {
    return psiClass.annotations.any { annotation ->
      annotation.qualifiedName == FQCN_JSON_CLASS &&
        annotation.findDeclaredAttributeValue("generateAdapter")?.text == "true"
    }
  }

  private fun hasAdaptedBy(psiClass: PsiClass): Boolean {
    return psiClass.annotations.any { annotation -> annotation.qualifiedName == FQCN_ADAPTED_BY }
  }

  companion object {
    // Fully qualified class names for relevant annotations and types
    private const val FQCN_JSON_INFLATER = "slack.commons.json.JsonInflater"
    private const val FQCN_JSON_CLASS = "com.squareup.moshi.JsonClass"
    private const val FQCN_ADAPTED_BY = "dev.zacsweers.moshix.adapters.AdaptedBy"

    // Issue definitions
    private val ISSUE_JSON_INFLATER_WITH_MOSHI_INCOMPATIBLE_TYPE =
      Issue.create(
        id = "JsonInflaterMoshiCompatibility:MoshiIncompatibleType",
        "Using JsonInflater.inflate/deflate with a Moshi-incompatible type.",
        """
          Classes used with JsonInflater.inflate/deflate must be annotated with @JsonClass or @AdaptedBy to make it \
          compatible with Moshi. Additionally, it cannot be an abstract class or an interface.
        """
          .trimIndent(),
        Category.CORRECTNESS,
        6,
        Severity.ERROR,
        implementation = sourceImplementation<JsonInflaterMoshiCompatibilityDetector>(),
      )

    fun issues(): List<Issue> = listOf(ISSUE_JSON_INFLATER_WITH_MOSHI_INCOMPATIBLE_TYPE)
  }
}
