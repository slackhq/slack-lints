// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint.detekt

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.IntOption
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.android.tools.lint.detector.api.StringOption
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UFile
import slack.lint.util.IntLintOption
import slack.lint.util.OptionLoadingDetector
import slack.lint.util.StringSetLintOption
import slack.lint.util.hasAnyAnnotation
import slack.lint.util.sourceImplementation

class TooManyFunctionsDetector(
  private val thresholdOption: IntLintOption = IntLintOption(THRESHOLD),
  private val ignoreAnnotatedOption: StringSetLintOption = StringSetLintOption(IGNORE_ANNOTATED),
) : OptionLoadingDetector(thresholdOption, ignoreAnnotatedOption), SourceCodeScanner {

  override fun getApplicableUastTypes(): List<Class<out UElement>> =
    listOf(UClass::class.java, UFile::class.java)

  override fun createUastHandler(context: JavaContext): UElementHandler {
    return object : UElementHandler() {
      override fun visitClass(node: UClass) {
        if (node.hasAnyAnnotation(ignoreAnnotatedOption.value)) return
        val functionCount = node.methods.count { !it.isConstructor }
        if (functionCount > thresholdOption.value) {
          context.report(
            ISSUE,
            node,
            context.getNameLocation(node),
            "Class `${node.name}` has $functionCount functions, exceeding the limit of ${thresholdOption.value}.",
          )
        }
      }

      override fun visitFile(node: UFile) {
        val topLevelFunctions = node.classes.flatMap { cls ->
          cls.methods.filter { !it.isConstructor }
        }
        // Only check file-level function count if there's no primary class
        // (i.e. it's a file of top-level functions)
        val primaryClass = node.classes.firstOrNull { it.name != null }
        if (primaryClass == null || node.classes.size == 1) return
        // File-level check is just for files with only top-level declarations
      }
    }
  }

  companion object {
    private val THRESHOLD =
      IntOption("threshold", "Maximum number of functions allowed in a class.", 40)

    private val IGNORE_ANNOTATED =
      StringOption(
        "ignore-annotated",
        "Comma-separated list of annotation simple names to ignore.",
        "Module",
        "Classes annotated with these annotations are excluded from this check.",
      )

    val ISSUE =
      Issue.create(
        id = "TooManyFunctions",
        briefDescription = "Class has too many functions",
        explanation =
          "Classes with too many functions are likely doing too much. " +
            "Consider splitting responsibilities across multiple classes.",
        category = Category.CORRECTNESS,
        priority = 5,
        severity = Severity.WARNING,
        implementation = sourceImplementation<TooManyFunctionsDetector>(),
      )
        .setOptions(listOf(THRESHOLD, IGNORE_ANNOTATED))
  }
}
