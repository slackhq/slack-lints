// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint.naming

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.android.tools.lint.detector.api.StringOption
import com.android.tools.lint.detector.api.isKotlin
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UField
import slack.lint.util.sourceImplementation

class TopLevelPropertyNamingDetector : Detector(), SourceCodeScanner {

  override fun getApplicableUastTypes(): List<Class<out UElement>> = listOf(UField::class.java)

  override fun createUastHandler(context: JavaContext): UElementHandler {
    if (!isKotlin(context.psiFile)) return UElementHandler.NONE
    val pattern =
      Regex(CONSTANT_PATTERN.getValue(context.configuration) ?: CONSTANT_PATTERN.defaultValue!!)
    return object : UElementHandler() {
      override fun visitField(node: UField) {
        val ktProperty = node.sourcePsi as? KtProperty ?: return
        if (!ktProperty.isTopLevel) return
        if (!ktProperty.hasModifier(KtTokens.CONST_KEYWORD)) return
        val name = node.name
        if (!pattern.matches(name)) {
          context.report(
            ISSUE,
            node,
            context.getNameLocation(node),
            "Top-level constant `$name` does not match pattern `$pattern`.",
          )
        }
      }
    }
  }

  companion object {
    private val CONSTANT_PATTERN =
      StringOption(
        "constant-pattern",
        "Regex pattern for top-level constant names.",
        "[A-Z][_A-Za-z0-9]*",
        "Top-level constants must match this pattern.",
      )

    val ISSUE =
      Issue.create(
          id = "TopLevelPropertyNaming",
          briefDescription = "Top-level constant does not follow naming conventions",
          explanation =
            "Top-level `const val` properties should follow the configured naming pattern " +
              "(default: UPPER_CASE_WITH_UNDERSCORES).",
          category = Category.CORRECTNESS,
          priority = 5,
          severity = Severity.WARNING,
          implementation = sourceImplementation<TopLevelPropertyNamingDetector>(),
        )
        .setOptions(listOf(CONSTANT_PATTERN))
  }
}
