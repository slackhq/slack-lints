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
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UFile
import slack.lint.util.IntLintOption
import slack.lint.util.OptionLoadingDetector
import slack.lint.util.StringSetLintOption
import slack.lint.util.hasAnyAnnotation
import slack.lint.util.sourceImplementation

class TooManyFunctionsDetector(
  private val inFilesOption: IntLintOption = IntLintOption(THRESHOLD_IN_FILES),
  private val inClassesOption: IntLintOption = IntLintOption(THRESHOLD_IN_CLASSES),
  private val inInterfacesOption: IntLintOption = IntLintOption(THRESHOLD_IN_INTERFACES),
  private val inObjectsOption: IntLintOption = IntLintOption(THRESHOLD_IN_OBJECTS),
  private val inEnumsOption: IntLintOption = IntLintOption(THRESHOLD_IN_ENUMS),
  private val ignoreAnnotatedOption: StringSetLintOption = StringSetLintOption(IGNORE_ANNOTATED),
) :
  OptionLoadingDetector(
    inFilesOption,
    inClassesOption,
    inInterfacesOption,
    inObjectsOption,
    inEnumsOption,
    ignoreAnnotatedOption,
  ),
  SourceCodeScanner {

  override fun getApplicableUastTypes(): List<Class<out UElement>> =
    listOf(UClass::class.java, UFile::class.java)

  override fun createUastHandler(context: JavaContext): UElementHandler {
    return object : UElementHandler() {
      override fun visitClass(node: UClass) {
        val declaration = node.sourcePsi as? KtClassOrObject ?: return
        if (node.hasAnyAnnotation(ignoreAnnotatedOption.value)) return

        val (subject, threshold) =
          when {
            declaration is KtClass && declaration.isInterface() ->
              "Interface" to inInterfacesOption.value
            declaration is KtClass && declaration.isEnum() -> "Enum class" to inEnumsOption.value
            declaration is KtObjectDeclaration -> "Object" to inObjectsOption.value
            else -> "Class" to inClassesOption.value
          }

        val count = declaration.functionCount()
        if (count >= threshold) {
          // An anonymous object has no name to report.
          val subjectName = declaration.name?.let { "$subject `$it`" } ?: "Anonymous object"
          context.report(
            ISSUE,
            context.getNameLocation(node),
            "$subjectName has $count functions, the threshold is $threshold",
          )
        }
      }

      override fun visitFile(node: UFile) {
        val file = node.sourcePsi as? KtFile ?: return
        val threshold = inFilesOption.value
        val count = file.declarations.count { it is KtNamedFunction }
        if (count >= threshold) {
          context.report(
            ISSUE,
            Location.create(context.file),
            "File `${context.file.name}` has $count top-level functions, the threshold is $threshold",
          )
        }
      }

      // Only functions declared directly in the body count; a nested class's own functions are
      // reported against that class instead.
      private fun KtClassOrObject.functionCount(): Int =
        body?.declarations?.count { it is KtNamedFunction } ?: 0
    }
  }

  companion object {
    internal val THRESHOLD_IN_FILES =
      IntOption("threshold-in-files", "Number of top-level functions required to report.", 11)

    internal val THRESHOLD_IN_CLASSES =
      IntOption("threshold-in-classes", "Number of functions in a class required to report.", 11)

    internal val THRESHOLD_IN_INTERFACES =
      IntOption(
        "threshold-in-interfaces",
        "Number of functions in an interface required to report.",
        11,
      )

    internal val THRESHOLD_IN_OBJECTS =
      IntOption("threshold-in-objects", "Number of functions in an object required to report.", 11)

    internal val THRESHOLD_IN_ENUMS =
      IntOption(
        "threshold-in-enums",
        "Number of functions in an enum class required to report.",
        11,
      )

    internal val IGNORE_ANNOTATED =
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
        .setOptions(
          listOf(
            THRESHOLD_IN_FILES,
            THRESHOLD_IN_CLASSES,
            THRESHOLD_IN_INTERFACES,
            THRESHOLD_IN_OBJECTS,
            THRESHOLD_IN_ENUMS,
            IGNORE_ANNOTATED,
          )
        )
  }
}
