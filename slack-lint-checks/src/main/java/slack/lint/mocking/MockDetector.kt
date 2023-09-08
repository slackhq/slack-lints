// Copyright (C) 2021 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint.mocking

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.android.tools.lint.detector.api.StringOption
import com.android.tools.lint.detector.api.isJava
import com.android.tools.lint.detector.api.isKotlin
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassType
import com.intellij.util.containers.map2Array
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.uast.UAnnotated
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UClassLiteralExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UField
import org.jetbrains.uast.UReferenceExpression
import org.jetbrains.uast.UastCallKind
import slack.lint.mocking.MockDetector.Companion.TYPE_CHECKERS
import slack.lint.mocking.MockDetector.TypeChecker
import slack.lint.util.MetadataJavaEvaluator
import slack.lint.util.OptionLoadingDetector
import slack.lint.util.StringSetLintOption

/**
 * A detector for detecting different kinds of mocking behavior. Implementations of [TypeChecker]
 * can indicate annotated types that should be reported via [TypeChecker.checkType] or
 * [TypeChecker.annotations] for more dynamic control.
 *
 * New [TypeChecker] implementations should be added to [TYPE_CHECKERS] to run in this.
 */
class MockDetector
constructor(
  private val mockAnnotations: StringSetLintOption = StringSetLintOption(MOCK_ANNOTATIONS),
  private val mockClasses: StringSetLintOption = StringSetLintOption(MOCK_CLASSES),
  private val mockMethods: StringSetLintOption = StringSetLintOption(MOCK_METHODS),
) : OptionLoadingDetector(mockAnnotations, mockClasses, mockMethods), SourceCodeScanner {
  companion object {

    internal val MOCK_ANNOTATIONS =
      StringOption(
        "mock-annotations",
        "A comma-separated list of mock annotations.",
        "org.mockito.Mock,org.mockito.Spy",
        "This property should define comma-separated list of mock annotation class names (FQCN)."
      )

    internal val MOCK_CLASSES =
      StringOption(
        "mock-classes",
        "A comma-separated list of mock classes.",
        "org.mockito.Mockito,slack.test.mockito.MockitoHelpers,slack.test.mockito.MockitoHelpersKt",
        "This property should define comma-separated list of mock class names (FQCN)."
      )

    internal val MOCK_METHODS =
      StringOption(
        "mock-methods",
        "A comma-separated list of mock methods.",
        "mock,spy",
        "This property should define comma-separated list of mock method names."
      )

    private val TYPE_CHECKERS =
      listOf(
        // Loosely defined in the order of most likely to be hit
        PlatformTypeMockDetector,
        DataClassMockDetector,
        DoNotMockMockDetector,
        SealedClassMockDetector,
        AutoValueMockDetector,
        ObjectClassMockDetector,
        RecordClassMockDetector,
      )
    private val OPTIONS = listOf(MOCK_ANNOTATIONS, MOCK_CLASSES, MOCK_METHODS)
    val ISSUES = TYPE_CHECKERS.map2Array { it.issue.setOptions(OPTIONS) }
  }

  override fun getApplicableUastTypes() = listOf(UCallExpression::class.java, UField::class.java)

  override fun createUastHandler(context: JavaContext): UElementHandler? {
    if (!context.isTestSource) return null

    val checkers =
      TYPE_CHECKERS.filter { context.isEnabled(it.issue) }
        .ifEmpty {
          return null
        }
    val slackEvaluator = MetadataJavaEvaluator(context.file.name, context.evaluator)
    return object : UElementHandler() {

      // Checks for mock()/spy() calls
      override fun visitCallExpression(node: UCallExpression) {
        // We only want method calls
        if (node.kind != UastCallKind.METHOD_CALL) return

        // Check our known mock methods
        if (node.methodName in mockMethods.value) {
          val resolvedClass = node.resolve()?.containingClass?.qualifiedName
          // Now resolve the mocked type
          var argumentType: PsiClass? = null
          val expressionType = node.getExpressionType()
          if (expressionType != null) {
            argumentType = slackEvaluator.getTypeClass(expressionType)
          } else if (node.typeArgumentCount == 1) {
            // We can read the type here for the fun <reified T> mock() helpers
            argumentType = slackEvaluator.getTypeClass(node.typeArguments[0])
          } else if (resolvedClass in mockClasses.value && node.valueArgumentCount != 0) {
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
          }

          argumentType?.let { checkMock(node, argumentType) }
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
        return mockAnnotations.value.any { node.findAnnotation(it) != null }
      }

      private fun checkMock(node: UElement, type: PsiClass) {
        for (checker in checkers) {
          val reason = checker.checkType(context, slackEvaluator, type)
          if (reason != null) {
            context.report(checker.issue, context.getLocation(node), reason.reason)
            continue
          }
          val disallowedAnnotation = checker.annotations.find { type.hasAnnotation(it) } ?: continue
          context.report(
            checker.issue,
            context.getLocation(node),
            "Mocked type is annotated with non-mockable annotation $disallowedAnnotation."
          )
          return
        }
      }
    }
  }

  interface TypeChecker {
    val issue: Issue

    /** Set of annotation FQCNs that should not be mocked */
    val annotations: Set<String>
      get() = emptySet()

    fun checkType(
      context: JavaContext,
      evaluator: MetadataJavaEvaluator,
      mockedType: PsiClass
    ): Reason? {
      return null
    }
  }

  /**
   * @property type a [PsiClass] object representing the class that should not be mocked.
   * @property reason The reason this class should not be mocked, which may be as simple as "it is
   *   annotated to forbid mocking" but may also provide a suggested workaround.
   */
  data class Reason(val type: PsiClass, val reason: String)
}
