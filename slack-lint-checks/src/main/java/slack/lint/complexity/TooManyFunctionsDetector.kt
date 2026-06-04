// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint.complexity

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.IntOption
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Location
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.android.tools.lint.detector.api.StringOption
import org.jetbrains.kotlin.psi.KtClassOrObject
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
        // Kotlin top-level functions are wrapped in a synthetic facade class (e.g. `FooKt`) with no
        // backing KtClassOrObject. visitFile handles those, so skip them here to avoid reporting a
        // misleading synthetic class name.
        if (node.sourcePsi !is KtClassOrObject) return
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
        // Count top-level functions, which UAST exposes as members of synthetic facade classes
        // backed by the KtFile. Members of real classes declared in the file are reported by
        // visitClass instead.
        val topLevelFunctionCount =
          node.classes
            .filter { it.sourcePsi !is KtClassOrObject }
            .sumOf { facade -> facade.methods.count { !it.isConstructor } }
        if (topLevelFunctionCount > thresholdOption.value) {
          context.report(
            ISSUE,
            node,
            Location.create(context.file),
            "File `${context.file.name}` has $topLevelFunctionCount top-level functions, exceeding the limit of ${thresholdOption.value}.",
          )
        }
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
