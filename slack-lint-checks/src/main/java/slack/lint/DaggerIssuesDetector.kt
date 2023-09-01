// Copyright (C) 2021 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.android.tools.lint.detector.api.TextFormat
import com.android.tools.lint.detector.api.isKotlin
import com.intellij.lang.jvm.JvmClassKind
import java.util.EnumSet
import org.jetbrains.uast.UAnnotated
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.kotlin.KotlinReceiverUParameter

/** This is a simple lint check to catch common Dagger+Kotlin usage issues. */
class DaggerIssuesDetector : Detector(), SourceCodeScanner {

  companion object {
    // We use the overloaded constructor that takes a varargs of `Scope` as the last param.
    // This is to enable on-the-fly IDE checks. We are telling lint to run on both
    // JAVA and TEST_SOURCES in the `scope` parameter but by providing the `analysisScopes`
    // params, we're indicating that this check can run on either JAVA or TEST_SOURCES and
    // doesn't require both of them together.
    // From discussion on lint-dev https://groups.google.com/d/msg/lint-dev/ULQMzW1ZlP0/1dG4Vj3-AQAJ
    // This was supposed to be fixed in AS 3.4 but still required as recently as 3.6-alpha10.
    private val SCOPES =
      Implementation(
        DaggerIssuesDetector::class.java,
        EnumSet.of(Scope.JAVA_FILE, Scope.TEST_SOURCES),
        EnumSet.of(Scope.JAVA_FILE),
        EnumSet.of(Scope.TEST_SOURCES)
      )

    private val ISSUE_BINDS_TYPE_MISMATCH: Issue =
      Issue.create(
        "BindsTypeMismatch",
        "@Binds function parameters must be type-assignable to their return types.",
        "@Binds function parameters must be type-assignable to their return types.",
        Category.CORRECTNESS,
        6,
        Severity.ERROR,
        SCOPES
      )

    private val ISSUE_BINDS_RETURN_TYPE: Issue =
      Issue.create(
        "BindsReturnType",
        "@Binds functions must have a return type. Cannot be void or Unit.",
        "@Binds functions must have a return type. Cannot be void or Unit.",
        Category.CORRECTNESS,
        6,
        Severity.ERROR,
        SCOPES
      )

    private val ISSUE_BINDS_RECEIVER_PARAMETER: Issue =
      Issue.create(
        "BindsReceiverParameter",
        "@Binds functions cannot be extension functions.",
        "@Binds functions cannot be extension functions. Move the receiver type to a parameter via IDE inspection (option+enter and convert to property).",
        Category.CORRECTNESS,
        6,
        Severity.ERROR,
        SCOPES
      )

    private val ISSUE_BINDS_WRONG_PARAMETER_COUNT: Issue =
      Issue.create(
        "BindsWrongParameterCount",
        "@Binds functions require a single parameter as an input to bind.",
        "@Binds functions require a single parameter as an input to bind.",
        Category.CORRECTNESS,
        6,
        Severity.ERROR,
        SCOPES
      )

    private val ISSUE_BINDS_MUST_BE_ABSTRACT: Issue =
      Issue.create(
        "BindsMustBeAbstract",
        "@Binds functions must be abstract and cannot have function bodies.",
        "@Binds functions must be abstract and cannot have function bodies.",
        Category.CORRECTNESS,
        6,
        Severity.ERROR,
        SCOPES
      )

    private val ISSUE_BINDS_REDUNDANT: Issue =
      Issue.create(
        "RedundantBinds",
        "@Binds functions should return a different type (including annotations) than the input type.",
        "@Binds functions should return a different type (including annotations) than the input type.",
        Category.CORRECTNESS,
        6,
        Severity.ERROR,
        SCOPES
      )

    private const val BINDS_ANNOTATION = "dagger.Binds"

    val ISSUES: Array<Issue> =
      arrayOf(
        ISSUE_BINDS_TYPE_MISMATCH,
        ISSUE_BINDS_RETURN_TYPE,
        ISSUE_BINDS_RECEIVER_PARAMETER,
        ISSUE_BINDS_WRONG_PARAMETER_COUNT,
        ISSUE_BINDS_MUST_BE_ABSTRACT,
        ISSUE_BINDS_REDUNDANT,
      )
  }

  override fun getApplicableUastTypes(): List<Class<out UElement>> {
    return listOf(UMethod::class.java)
  }

  override fun createUastHandler(context: JavaContext): UElementHandler? {
    if (!isKotlin(context.psiFile)) {
      // This is only relevant for Kotlin files.
      return null
    }
    return object : UElementHandler() {
      override fun visitMethod(node: UMethod) {
        if (!node.isConstructor && node.hasAnnotation(BINDS_ANNOTATION)) {
          val containingClass = node.containingClass
          if (containingClass != null) {
            when {
              containingClass.isInterface -> {
                // Cannot have a default impl in interfaces
                if (node.uastBody != null) {
                  context.report(
                    ISSUE_BINDS_MUST_BE_ABSTRACT,
                    context.getLocation(node),
                    ISSUE_BINDS_MUST_BE_ABSTRACT.getBriefDescription(TextFormat.TEXT),
                  )
                  return
                }
              }
              containingClass.classKind == JvmClassKind.CLASS -> {
                // Check abstract
                if (!context.evaluator.isAbstract(node)) {
                  context.report(
                    ISSUE_BINDS_MUST_BE_ABSTRACT,
                    context.getLocation(node),
                    ISSUE_BINDS_MUST_BE_ABSTRACT.getBriefDescription(TextFormat.TEXT),
                  )
                  return
                }
              }
            }
          }

          if (node.uastParameters.size != 1) {
            context.report(
              ISSUE_BINDS_WRONG_PARAMETER_COUNT,
              context.getLocation(node),
              ISSUE_BINDS_WRONG_PARAMETER_COUNT.getBriefDescription(TextFormat.TEXT)
            )
            return
          }

          val firstParam = node.uastParameters[0]
          if (firstParam is KotlinReceiverUParameter) {
            context.report(
              ISSUE_BINDS_RECEIVER_PARAMETER,
              context.getLocation(firstParam.node),
              ISSUE_BINDS_RECEIVER_PARAMETER.getBriefDescription(TextFormat.TEXT),
            )
            return
          }
          val instanceType = firstParam.type

          val returnType =
            node.returnType?.takeUnless {
              context.evaluator.getTypeClass(it)?.qualifiedName == "kotlin.Unit"
            }
              ?: run {
                // Report missing return type
                context.report(
                  ISSUE_BINDS_RETURN_TYPE,
                  context.getLocation(node),
                  ISSUE_BINDS_RETURN_TYPE.getBriefDescription(TextFormat.TEXT),
                )
                return
              }

          if (instanceType == returnType) {
            // Check that they have different annotations, otherwise it's redundant
            if (firstParam.qualifiers() != node.qualifiers()) {
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

  private fun UAnnotated.qualifiers() =
    uAnnotations
      .asSequence()
      .filter { it.resolve()?.hasAnnotation("javax.inject.Qualifier") == true }
      .toSet()
}
