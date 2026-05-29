// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint.style

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.android.tools.lint.detector.api.StringOption
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UElement
import slack.lint.util.OptionLoadingDetector
import slack.lint.util.StringSetLintOption
import slack.lint.util.hasAnyAnnotation
import slack.lint.util.sourceImplementation

class UnnecessaryAbstractClassDetector(
  private val ignoreAnnotatedOption: StringSetLintOption = StringSetLintOption(IGNORE_ANNOTATED)
) : OptionLoadingDetector(ignoreAnnotatedOption), SourceCodeScanner {

  override fun getApplicableUastTypes(): List<Class<out UElement>> = listOf(UClass::class.java)

  override fun createUastHandler(context: JavaContext): UElementHandler {
    return object : UElementHandler() {
      override fun visitClass(node: UClass) {
        if (!node.isAbstract()) return
        if (node.hasAnyAnnotation(ignoreAnnotatedOption.value)) return

        val hasAbstractMembers = node.methods.any { method -> context.evaluator.isAbstract(method) }
        if (!hasAbstractMembers) {
          context.report(
            ISSUE,
            node,
            context.getNameLocation(node),
            "Abstract class `${node.name}` has no abstract members. Remove the `abstract` modifier or add abstract members.",
          )
        }
      }
    }
  }

  private fun UClass.isAbstract(): Boolean {
    return javaPsi.modifierList?.hasModifierProperty("abstract") == true
  }

  companion object {
    private val IGNORE_ANNOTATED =
      StringOption(
        "ignore-annotated",
        "Comma-separated list of annotation simple names to ignore.",
        "Module",
        "Abstract classes annotated with these are excluded (e.g. Dagger modules).",
      )

    val ISSUE =
      Issue.create(
          id = "UnnecessaryAbstractClass",
          briefDescription = "Abstract class has no abstract members",
          explanation =
            "An abstract class without abstract members doesn't need to be abstract. " +
              "Consider making it a concrete class or an interface, or add abstract members.",
          category = Category.CORRECTNESS,
          priority = 5,
          severity = Severity.WARNING,
          implementation = sourceImplementation<UnnecessaryAbstractClassDetector>(),
        )
        .setOptions(listOf(IGNORE_ANNOTATED))
  }
}
