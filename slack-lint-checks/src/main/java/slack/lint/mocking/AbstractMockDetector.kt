// Copyright (C) 2021 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint.mocking

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.android.tools.lint.detector.api.isJava
import com.android.tools.lint.detector.api.isKotlin
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassType
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.uast.UAnnotated
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UClassLiteralExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UField
import org.jetbrains.uast.UReferenceExpression
import org.jetbrains.uast.UVariable
import org.jetbrains.uast.getParentOfType
import slack.lint.util.MetadataJavaEvaluator

/**
 * A base detector class for detecting different kinds of mocking behavior. Subclasses can indicate
 * annotated types that should be reported or implement [checkType] for more dynamic control.
 */
abstract class AbstractMockDetector : Detector(), SourceCodeScanner {
  companion object {
    val MOCK_ANNOTATIONS = setOf("org.mockito.Mock", "org.mockito.Spy")
    val MOCK_CLASSES =
      setOf(
        "org.mockito.Mockito",
        "slack.test.mockito.MockitoHelpers",
        "slack.test.mockito.MockitoHelpersKt"
      )
    val MOCK_METHODS = setOf("mock", "spy")
  }

  /** Set of annotation FQCNs that should not be mocked */
  open val annotations: Set<String> = emptySet()

  open fun checkType(
          context: JavaContext,
          evaluator: MetadataJavaEvaluator,
          mockedType: PsiClass
  ): Reason? {
    return null
  }

  abstract fun report(
    context: JavaContext,
    mockedType: PsiClass,
    mockNode: UElement,
    reason: Reason
  )

  override fun getApplicableUastTypes() = listOf(UCallExpression::class.java, UField::class.java)

  override fun createUastHandler(context: JavaContext): UElementHandler {
    val slackEvaluator = MetadataJavaEvaluator(context.file.name, context.evaluator)
    return object : UElementHandler() {

      // Checks for mock()/spy() calls
      override fun visitCallExpression(node: UCallExpression) {
        if (node.methodName in MOCK_METHODS) {
          var nodeToReport: UElement = node
          val resolvedClass = node.resolve()?.containingClass?.qualifiedName
          // Now resolve the mocked type
          var argumentType: PsiClass? = null
          if (node.typeArgumentCount == 1) {
            // We can read the type here for the fun <reified T> mock() helpers
            argumentType = slackEvaluator.getTypeClass(node.typeArguments[0])
          } else if (resolvedClass in MOCK_CLASSES && node.valueArgumentCount != 0) {
            when (val firstArg = node.valueArguments[0]) {
              is UClassLiteralExpression -> {
                // It's Foo.class, we can just use it directly
                argumentType = slackEvaluator.getTypeClass(firstArg.type)
              }
              is UReferenceExpression -> {
                val type = firstArg.getExpressionType()
                if (node.methodName == "spy") {
                  // spy takes an instance, so take the type at face value
                  argumentType = slackEvaluator.getTypeClass(type)
                } else if (type is PsiClassType && type.parameterCount == 1) {
                  // If it's a Class and not a "spy" method, assume it's the mock type
                  val classGeneric = type.parameters[0]
                  argumentType = slackEvaluator.getTypeClass(classGeneric)
                }
              }
            }
          } else {
            // Last ditch effort - see if we are assigning to a variable and can get its type
            // Covers cases like `val dynamicMock: TestClass = mock()`
            val variable = node.getParentOfType<UVariable>() ?: return
            nodeToReport = variable
            argumentType = slackEvaluator.getTypeClass(variable.type)
          }

          argumentType?.let { checkMock(nodeToReport, argumentType) }
        }
      }

      // Checks properties and fields, usually annotated with @Mock/@Spy
      override fun visitField(node: UField) {
        if (isKotlin(node)) {
          val sourcePsi = node.sourcePsi ?: return
          if (sourcePsi is KtProperty && isMockAnnotated(node)) {
            val type = slackEvaluator.getTypeClass(node.type) ?: return
            checkMock(node, type)
            return
          }
        } else if (isJava(node) && isMockAnnotated(node)) {
          val type = slackEvaluator.getTypeClass(node.type) ?: return
          checkMock(node, type)
          return
        }
      }

      private fun isMockAnnotated(node: UAnnotated): Boolean {
        return MOCK_ANNOTATIONS.any { node.findAnnotation(it) != null }
      }

      private fun checkMock(node: UElement, type: PsiClass) {
        val reason = checkType(context, slackEvaluator, type)
        if (reason != null) {
          report(context, type, node, reason)
          return
        }
        val disallowedAnnotation = annotations.find { type.hasAnnotation(it) } ?: return
        report(
          context,
          type,
          node,
          Reason(
            type,
            "Mocked type is annotated with non-mockable annotation $disallowedAnnotation."
          )
        )
      }
    }
  }

  /**
   * @property type a [PsiClass] object representing the class that should not be mocked.
   * @property reason The reason this class should not be mocked, which may be as simple as "it is
   *   annotated to forbid mocking" but may also provide a suggested workaround.
   */
  data class Reason(val type: PsiClass, val reason: String)
}
