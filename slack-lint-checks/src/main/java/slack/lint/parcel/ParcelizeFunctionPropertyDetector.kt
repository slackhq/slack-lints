// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint.parcel

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.android.tools.lint.detector.api.TextFormat
import com.android.tools.lint.detector.api.getUMethod
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiType
import org.jetbrains.kotlin.psi.KtPrimaryConstructor
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UElement
import org.jetbrains.uast.kotlin.isKotlin
import slack.lint.parcel.ParcelizeFunctionPropertyDetector.Companion.ISSUE
import slack.lint.util.sourceImplementation

/** @see ISSUE */
class ParcelizeFunctionPropertyDetector : Detector(), SourceCodeScanner {

  override fun getApplicableUastTypes(): List<Class<out UElement>> {
    return listOf(UClass::class.java)
  }

  override fun createUastHandler(context: JavaContext): UElementHandler? {
    // Parcelize can only be used in Kotlin files, so this check only checks in Kotlin files
    if (!isKotlin(context.uastFile?.lang)) return null

    return object : UElementHandler() {
      override fun visitClass(node: UClass) {
        if (!node.hasAnnotation(PARCELIZE)) return

        // Primary constructor is required, but Parcelize's inspections will catch this
        val primaryConstructor =
          node.constructors
            .asSequence()
            .mapNotNull { it.getUMethod() }
            .firstOrNull { it.sourcePsi is KtPrimaryConstructor } ?: return

        // Now check properties
        for (parameter in primaryConstructor.uastParameters) {
          if (parameter.type.isFunctionType && !parameter.hasAnnotation(IGNORED_ON_PARCEL)) {
            context.report(
              ISSUE,
              context.getLocation(parameter.typeReference),
              ISSUE.getExplanation(TextFormat.TEXT),
            )
          }
        }
      }

      private val PsiType.isFunctionType: Boolean
        get() =
          this is PsiClassType &&
            resolve()?.qualifiedName?.startsWith("kotlin.jvm.functions.Function") == true
    }
  }

  companion object {
    private const val PARCELIZE_PACKAGE = "kotlinx.parcelize"
    private const val PARCELIZE = "$PARCELIZE_PACKAGE.Parcelize"
    private const val IGNORED_ON_PARCEL = "$PARCELIZE_PACKAGE.IgnoredOnParcel"

    internal val ISSUE: Issue =
      Issue.create(
        "ParcelizeFunctionProperty",
        "Function type properties are not parcelable",
        "While technically (and surprisingly) supported by Parcelize, function types " +
          "should not be used in Parcelize classes. There are only limited conditions where it " +
          "will work and it's usually a sign that you're modeling your data wrong.",
        Category.CORRECTNESS,
        9,
        Severity.ERROR,
        sourceImplementation<ParcelizeFunctionPropertyDetector>(),
      )
  }
}
