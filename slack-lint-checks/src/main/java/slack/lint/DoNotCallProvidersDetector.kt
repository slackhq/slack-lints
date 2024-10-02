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
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UClass
import org.jetbrains.uast.getParentOfType
import slack.lint.util.sourceImplementation

class DoNotCallProvidersDetector : Detector(), SourceCodeScanner {

  companion object {
    private val SCOPES =
      sourceImplementation<DoNotCallProvidersDetector>(shouldRunOnTestSources = false)

    val ISSUE: Issue =
      Issue.create(
        "DoNotCallProviders",
        "Dagger provider methods should not be called directly by user code.",
        """
          Dagger provider methods should not be called directly by user code. These are intended solely for use \
          by Dagger-generated code and it is programmer error to call them from user code.
      """,
        Category.CORRECTNESS,
        6,
        Severity.ERROR,
        SCOPES,
      )

    private val PROVIDER_ANNOTATIONS =
      setOf("dagger.Binds", "dagger.Provides", "dagger.producers.Produces")
    private val GENERATED_ANNOTATIONS =
      setOf("javax.annotation.Generated", "javax.annotation.processing.Generated")
  }

  override fun getApplicableUastTypes() = listOf(UCallExpression::class.java)

  override fun createUastHandler(context: JavaContext) =
    object : UElementHandler() {
      override fun visitCallExpression(node: UCallExpression) {
        val enclosingClass = node.getParentOfType<UClass>() ?: return
        if (GENERATED_ANNOTATIONS.any(enclosingClass::hasAnnotation)) return
        val method = node.resolve() ?: return
        if (PROVIDER_ANNOTATIONS.any(method::hasAnnotation)) {
          context.report(
            ISSUE,
            context.getLocation(node),
            ISSUE.getBriefDescription(TextFormat.TEXT),
          )
        }
      }
    }
}
