// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.LintFix
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.android.tools.lint.detector.api.TextFormat
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.psiUtil.isPropertyParameter
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.UParameter
import org.jetbrains.uast.kotlin.KotlinUAnnotation
import org.jetbrains.uast.kotlin.isKotlin
import org.jetbrains.uast.toUElement
import slack.lint.util.sourceImplementation

/**
 * Detector that checks for `@TestParameter` annotations on parameter properties and ensures they
 * have param: site targets.
 */
class TestParameterFieldDetector : Detector(), SourceCodeScanner {

  override fun getApplicableUastTypes(): List<Class<out UElement>> {
    return listOf(UMethod::class.java)
  }

  override fun createUastHandler(context: JavaContext): UElementHandler? {
    // Only check Kotlin files
    if (!isKotlin(context.uastFile?.lang)) return null

    return object : UElementHandler() {
      override fun visitMethod(node: UMethod) {
        if (!node.isConstructor) return

        // Check parameters
        for (parameter in node.uastParameters) {
          checkParameter(parameter)
        }
      }

      private fun checkParameter(parameter: UParameter) {
        val sourcePsi = parameter.sourcePsi as? KtParameter ?: return

        // Only check property parameters
        if (!sourcePsi.isPropertyParameter()) return

        // Find @TestParameter annotations
        for (annotationEntry in sourcePsi.annotationEntries) {
          val qualifiedName = (annotationEntry.toUElement() as? KotlinUAnnotation)?.qualifiedName

          // Check if this is a TestParameter annotation
          if (
            qualifiedName == TEST_PARAMETER_ANNOTATION ||
              annotationEntry.shortName?.asString() == "TestParameter"
          ) {

            // Check if the annotation has a param: site target
            if (annotationEntry.useSiteTarget?.text != "param") {
              // Get the full text of the annotation
              val annotationText = annotationEntry.text

              // Create the replacement text by adding or replacing the site target
              val replacementText =
                if (annotationEntry.useSiteTarget != null) {
                  // Replace existing site target with param:
                  annotationText.replace("${annotationEntry.useSiteTarget!!.text}:", "param:")
                } else {
                  // Add param: site target
                  annotationText.replace("@", "@param:")
                }

              context.report(
                ISSUE,
                context.getLocation(annotationEntry),
                ISSUE.getBriefDescription(TextFormat.TEXT),
                LintFix.create().replace().text(annotationText).with(replacementText).build(),
              )
            }
          }
        }
      }
    }
  }

  companion object {
    private const val TEST_PARAMETER_ANNOTATION =
      "com.google.testing.junit.testparameterinjector.TestParameter"

    val ISSUE =
      Issue.create(
        id = "TestParameterSiteTarget",
        briefDescription =
          "`TestParameter` annotation has the wrong site target.",
        explanation =
          """
        `TestParameter` annotations on parameter properties must have `param:` site targets.\
        \
        For example:\
        ```kotlin\
        class MyTest(\
          @param:TestParameter val myParam: String\
        )\
        ```\
        \
        For more information, see: https://github.com/google/TestParameterInjector/issues/49
      """,
        category = Category.CORRECTNESS,
        priority = 5,
        severity = Severity.ERROR,
        implementation = sourceImplementation<TestParameterFieldDetector>(),
      )
  }
}
