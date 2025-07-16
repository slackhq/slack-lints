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
import com.android.tools.lint.detector.api.getUMethod
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.KtPrimaryConstructor
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UElement
import slack.lint.util.implements
import slack.lint.util.sourceImplementation

class CircuitScreenDataClassDetector : Detector(), SourceCodeScanner {
  override fun getApplicableUastTypes(): List<Class<out UElement>> = listOf(UClass::class.java)

  override fun createUastHandler(context: JavaContext): UElementHandler {
    return object : UElementHandler() {

      override fun visitClass(node: UClass) {
        val sourceNode = node.sourcePsi as? KtClassOrObject ?: return

        val isInterface = node.isInterface
        // Open classes cannot be "data" classes
        val isOpen = sourceNode.hasModifier(KtTokens.OPEN_KEYWORD)
        // Screens must be parcelable and inner classes cannot be parcelable
        val isInner = sourceNode.hasModifier(KtTokens.INNER_KEYWORD)
        // Cannot have abstract data class / object
        val isAbstract = sourceNode.hasModifier(KtTokens.ABSTRACT_KEYWORD)
        // Cannot have sealed data class / object
        val isSealed = sourceNode.hasModifier(KtTokens.SEALED_KEYWORD)
        // Cannot have companion data object
        val isCompanionObject = sourceNode.hasModifier(KtTokens.COMPANION_KEYWORD)

        val isApplicableClass =
          !isOpen && !isCompanionObject && !isInterface && !isInner && !isAbstract && !isSealed

        if (isApplicableClass && node.implements(QUALIFIED_CIRCUIT_SCREEN)) {
          val isDataClass = sourceNode.isData()

          if (!isDataClass) {
            val hasProperties =
              !node.constructors
                .asSequence()
                .mapNotNull { it.getUMethod() }
                .firstOrNull { it.sourcePsi is KtPrimaryConstructor }
                ?.uastParameters
                .isNullOrEmpty()
            val classKeyword =
              when (sourceNode) {
                is KtClass -> sourceNode.getClassKeyword()
                is KtObjectDeclaration -> sourceNode.getObjectKeyword() ?: return
                else -> return
              }
            val isObject = classKeyword?.node?.elementType == KtTokens.OBJECT_KEYWORD
            val originalKeyword = if (isObject) KtTokens.OBJECT_KEYWORD else KtTokens.CLASS_KEYWORD
            val replacement =
              if (hasProperties) "${KtTokens.DATA_KEYWORD} ${KtTokens.CLASS_KEYWORD}"
              else "${KtTokens.DATA_KEYWORD} ${KtTokens.OBJECT_KEYWORD}"
            val keywordLocation = context.getLocation(classKeyword)
            val quickFix =
              fix()
                .replace()
                .name("Replace with $replacement")
                .range(keywordLocation)
                .text(originalKeyword.value)
                .with(replacement)
                .reformat(true)
                .build()

            // update location to the keyword
            context.report(ISSUE, keywordLocation, MESSAGE, quickFix)
          }
        }
      }
    }
  }

  companion object {
    const val QUALIFIED_CIRCUIT_SCREEN = "com.slack.circuit.runtime.screen.Screen"
    const val MESSAGE =
      "Circuit Screen implementations should be data classes or data objects, not regular classes."
    const val ISSUE_ID = "CircuitScreenShouldBeDataClass"
    const val BRIEF_DESCRIPTION = "Circuit Screen should be a data class or data object"
    const val EXPLANATION =
      """Circuit Screen implementations should be data classes or data objects to ensure proper
equality, hashCode, and toString implementations. Regular classes can cause issues with
screen comparison and navigation."""

    val ISSUE: Issue =
      Issue.create(
        ISSUE_ID,
        BRIEF_DESCRIPTION,
        EXPLANATION,
        Category.CORRECTNESS,
        8,
        Severity.ERROR,
        sourceImplementation<CircuitScreenDataClassDetector>(),
      )
  }
}
