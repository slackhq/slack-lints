// Copyright (C) 2021 Slack Technologies, LLC
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
import com.intellij.lang.jvm.JvmClassKind
import com.intellij.psi.PsiTypes
import com.intellij.psi.util.childrenOfType
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.uast.UAnnotated
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.kotlin.KotlinReceiverUParameter
import org.jetbrains.uast.kotlin.KotlinUMethod
import slack.lint.util.sourceImplementation

/** This is a simple lint check to catch common Dagger+Kotlin usage issues. */
class DaggerIssuesDetector : Detector(), SourceCodeScanner {

  companion object {
    private val ISSUE_BINDS_MUST_BE_IN_MODULE: Issue =
      Issue.create(
        "BindsMustBeInModule",
        "@Binds function must be in `@Module`-annotated classes.",
        "@Binds function must be in `@Module`-annotated classes.",
        Category.CORRECTNESS,
        6,
        Severity.ERROR,
        sourceImplementation<DaggerIssuesDetector>()
      )

    private val ISSUE_BINDS_TYPE_MISMATCH: Issue =
      Issue.create(
        "BindsTypeMismatch",
        "@Binds function parameters must be type-assignable to their return types.",
        "@Binds function parameters must be type-assignable to their return types.",
        Category.CORRECTNESS,
        6,
        Severity.ERROR,
        sourceImplementation<DaggerIssuesDetector>()
      )

    private val ISSUE_RETURN_TYPE: Issue =
      Issue.create(
        "BindingReturnType",
        "@Binds/@Provides functions must have a return type. Cannot be void or Unit.",
        "@Binds/@Provides functions must have a return type. Cannot be void or Unit.",
        Category.CORRECTNESS,
        6,
        Severity.ERROR,
        sourceImplementation<DaggerIssuesDetector>()
      )

    private val ISSUE_RECEIVER_PARAMETER: Issue =
      Issue.create(
        "BindingReceiverParameter",
        "@Binds/@Provides functions cannot be extension functions.",
        "@Binds/@Provides functions cannot be extension functions. Move the receiver type to a parameter via IDE inspection (option+enter and convert to parameter).",
        Category.CORRECTNESS,
        6,
        Severity.ERROR,
        sourceImplementation<DaggerIssuesDetector>()
      )

    private val ISSUE_BINDS_WRONG_PARAMETER_COUNT: Issue =
      Issue.create(
        "BindsWrongParameterCount",
        "@Binds functions require a single parameter as an input to bind.",
        "@Binds functions require a single parameter as an input to bind.",
        Category.CORRECTNESS,
        6,
        Severity.ERROR,
        sourceImplementation<DaggerIssuesDetector>()
      )

    private val ISSUE_BINDS_MUST_BE_ABSTRACT: Issue =
      Issue.create(
        "BindsMustBeAbstract",
        "@Binds functions must be abstract and cannot have function bodies.",
        "@Binds functions must be abstract and cannot have function bodies.",
        Category.CORRECTNESS,
        6,
        Severity.ERROR,
        sourceImplementation<DaggerIssuesDetector>()
      )

    private val ISSUE_PROVIDES_CANNOT_BE_ABSTRACT: Issue =
      Issue.create(
        "ProvidesMustNotBeAbstract",
        "@Provides functions cannot be abstract.",
        "@Provides functions cannot be abstract.",
        Category.CORRECTNESS,
        6,
        Severity.ERROR,
        sourceImplementation<DaggerIssuesDetector>()
      )

    private val ISSUE_BINDS_REDUNDANT: Issue =
      Issue.create(
        "RedundantBinds",
        "@Binds functions should return a different type (including annotations) than the input type.",
        "@Binds functions should return a different type (including annotations) than the input type.",
        Category.CORRECTNESS,
        6,
        Severity.ERROR,
        sourceImplementation<DaggerIssuesDetector>()
      )

    private const val BINDS_ANNOTATION = "dagger.Binds"
    private const val PROVIDES_ANNOTATION = "dagger.Provides"

    val ISSUES: Array<Issue> =
      arrayOf(
        ISSUE_BINDS_TYPE_MISMATCH,
        ISSUE_RETURN_TYPE,
        ISSUE_RECEIVER_PARAMETER,
        ISSUE_BINDS_WRONG_PARAMETER_COUNT,
        ISSUE_BINDS_MUST_BE_ABSTRACT,
        ISSUE_BINDS_REDUNDANT,
        ISSUE_BINDS_MUST_BE_IN_MODULE,
        ISSUE_PROVIDES_CANNOT_BE_ABSTRACT,
      )
  }

  override fun getApplicableUastTypes() = listOf(UMethod::class.java)

  override fun createUastHandler(context: JavaContext): UElementHandler {
    return object : UElementHandler() {
      override fun visitMethod(node: UMethod) {
        if (!node.isConstructor) {
          val isBinds = node.hasAnnotation(BINDS_ANNOTATION)
          val isProvides = node.hasAnnotation(PROVIDES_ANNOTATION)

          if (!isBinds && !isProvides) return

          val containingClass = node.containingClass
          if (containingClass != null) {
            when {
              !containingClass.hasAnnotation("dagger.Module") -> {
                context.report(
                  ISSUE_BINDS_MUST_BE_IN_MODULE,
                  context.getLocation(node),
                  ISSUE_BINDS_MUST_BE_IN_MODULE.getBriefDescription(TextFormat.TEXT),
                )
                return
              }
              isBinds && containingClass.isInterface -> {
                // Cannot have a default impl in interfaces
                if (node.uastBody != null) {
                  context.report(
                    ISSUE_BINDS_MUST_BE_ABSTRACT,
                    context.getLocation(node.uastBody),
                    ISSUE_BINDS_MUST_BE_ABSTRACT.getBriefDescription(TextFormat.TEXT),
                  )
                  return
                }
              }
              containingClass.classKind == JvmClassKind.CLASS -> {
                val isAbstract = context.evaluator.isAbstract(node)
                // Binds must be abstract
                if (isBinds && !isAbstract) {
                  context.report(
                    ISSUE_BINDS_MUST_BE_ABSTRACT,
                    context.getLocation(node),
                    ISSUE_BINDS_MUST_BE_ABSTRACT.getBriefDescription(TextFormat.TEXT),
                  )
                  return
                } else if (isProvides && isAbstract) {
                  context.report(
                    ISSUE_PROVIDES_CANNOT_BE_ABSTRACT,
                    context.getLocation(node),
                    ISSUE_PROVIDES_CANNOT_BE_ABSTRACT.getBriefDescription(TextFormat.TEXT),
                  )
                  return
                }
              }
              containingClass.classKind == JvmClassKind.INTERFACE && isProvides -> {
                context.report(
                  ISSUE_PROVIDES_CANNOT_BE_ABSTRACT,
                  context.getLocation(node),
                  ISSUE_PROVIDES_CANNOT_BE_ABSTRACT.getBriefDescription(TextFormat.TEXT),
                )
                return
              }
            }
          }

          if (isBinds) {
            if (node.uastParameters.size != 1) {
              val locationToHighlight =
                if (node.uastParameters.isEmpty()) {
                  node
                } else {
                  node.parameterList
                }
              context.report(
                ISSUE_BINDS_WRONG_PARAMETER_COUNT,
                context.getLocation(locationToHighlight),
                ISSUE_BINDS_WRONG_PARAMETER_COUNT.getBriefDescription(TextFormat.TEXT)
              )
              return
            }
          }

          val returnType =
            node.returnType?.takeUnless {
              it == PsiTypes.voidType() ||
                context.evaluator.getTypeClass(it)?.qualifiedName == "kotlin.Unit"
            }
              ?: run {
                // Report missing return type
                val nodeLocation = node.returnTypeElement ?: node
                context.report(
                  ISSUE_RETURN_TYPE,
                  context.getLocation(nodeLocation),
                  ISSUE_RETURN_TYPE.getBriefDescription(TextFormat.TEXT),
                )
                return
              }

          if (node.uastParameters.isNotEmpty()) {
            val firstParam = node.uastParameters[0]
            if (firstParam is KotlinReceiverUParameter) {
              val nodeToReport =
                (node as KotlinUMethod).sourcePsi?.childrenOfType<KtTypeReference>()?.first()
                  ?: node
              context.report(
                ISSUE_RECEIVER_PARAMETER,
                context.getLocation(nodeToReport),
                ISSUE_RECEIVER_PARAMETER.getBriefDescription(TextFormat.TEXT),
              )
              return
            }

            if (isBinds) {
              val instanceType = firstParam.type
              if (instanceType == returnType) {
                // Check that they have different annotations, otherwise it's redundant
                if (firstParam.qualifiers() == node.qualifiers()) {
                  context.report(
                    ISSUE_BINDS_REDUNDANT,
                    context.getLocation(node),
                    ISSUE_BINDS_REDUNDANT.getBriefDescription(TextFormat.TEXT),
                  )
                  return
                }
              }

              if (!returnType.isAssignableFrom(instanceType)) {
                context.report(
                  ISSUE_BINDS_TYPE_MISMATCH,
                  context.getLocation(node),
                  ISSUE_BINDS_TYPE_MISMATCH.getBriefDescription(TextFormat.TEXT),
                )
              }
            }
          }
        }
      }
    }
  }

  private fun UAnnotated.qualifiers() =
    uAnnotations
      .asSequence()
      .filter { it.resolve()?.hasAnnotation("javax.inject.Qualifier") == true }
      .toSet()
}
