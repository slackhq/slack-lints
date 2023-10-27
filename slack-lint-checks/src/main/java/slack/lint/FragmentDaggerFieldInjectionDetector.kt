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
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UField
import slack.lint.util.implements
import slack.lint.util.sourceImplementation

/**
 * Detects that Fragments should use constructor injection in order to obtain references to its
 * dependencies.
 */
class FragmentDaggerFieldInjectionDetector : Detector(), SourceCodeScanner {

  override fun getApplicableUastTypes() = listOf(UField::class.java)

  override fun createUastHandler(context: JavaContext): UElementHandler {
    return object : UElementHandler() {

      override fun visitField(node: UField) {
        if (node.isStatic || node.findAnnotation(FQN_JAVAX_INJECT) == null) return

        val nodeParent = node.uastParent
        if (nodeParent !is UClass || nodeParent.isInterface) return

        if (!nodeParent.isFragment()) return

        val issueToReport =
          if (nodeParent.hasConstructorInjection()) {
            ISSUE_FRAGMENT_CONSTRUCTOR_INJECTION_AVAILABLE
          } else {
            ISSUE_FRAGMENT_FIELD_INJECTION_USED
          }

        context.report(
          issueToReport,
          context.getLocation(node),
          issueToReport.getBriefDescription(TextFormat.TEXT)
        )
      }
    }
  }

  private fun UClass.isFragment() = implements("androidx.fragment.app.Fragment")

  private fun UClass.hasConstructorInjection() =
    constructors.any {
      it.hasAnnotation(FQN_JAVAX_INJECT) || it.hasAnnotation(FQN_DAGGER_ASSISTED_INJECT)
    }

  companion object {
    private const val FQN_JAVAX_INJECT = "javax.inject.Inject"
    private const val FQN_DAGGER_ASSISTED_INJECT = "dagger.assisted.AssistedInject"

    private val ISSUE_FRAGMENT_CONSTRUCTOR_INJECTION_AVAILABLE: Issue =
      Issue.create(
        id = "FragmentConstructorInjection",
        briefDescription =
          "Fragment dependencies should be injected using constructor injections only.",
        explanation =
          """
        This Fragment has been set up to inject its dependencies through the constructor. \
        This dependency should be declared in the constructor where dagger will handle the \
        injection at runtime.
      """,
        category = Category.CORRECTNESS,
        priority = 6,
        severity = Severity.ERROR,
        implementation = sourceImplementation<FragmentDaggerFieldInjectionDetector>()
      )

    private val ISSUE_FRAGMENT_FIELD_INJECTION_USED: Issue =
      Issue.create(
        id = "FragmentFieldInjection",
        briefDescription =
          "Fragment dependencies should be injected using the Fragment's constructor.",
        explanation =
          """
        This dependency should be injected by dagger via the constructor. Add this field's type \
        into the parameter list of the Fragment's constructor. This constructor should be annotated \
        with either `@AssistedInject` or `@Inject`. Annotate with `@AssistedInject` if this \
        Fragment requires runtime arguments via a `Bundle`. Annotate with `@Inject` if this \
        Fragment does not require any runtime arguments. If this is an abstract class, the \
        constructor does not need to be annotated with `@Inject` or `@AssistedInject`.
      """,
        category = Category.CORRECTNESS,
        priority = 6,
        severity = Severity.ERROR,
        implementation = sourceImplementation<FragmentDaggerFieldInjectionDetector>()
      )

    val issues =
      listOf(ISSUE_FRAGMENT_CONSTRUCTOR_INJECTION_AVAILABLE, ISSUE_FRAGMENT_FIELD_INJECTION_USED)
  }
}
