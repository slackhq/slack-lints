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
import com.android.tools.lint.detector.api.isKotlin
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UElement
import org.jetbrains.uast.USimpleNameReferenceExpression
import org.jetbrains.uast.visitor.AbstractUastVisitor
import slack.lint.util.OptionLoadingDetector
import slack.lint.util.StringSetLintOption
import slack.lint.util.hasAnyAnnotation
import slack.lint.util.sourceImplementation

class UnusedPrivateMemberDetector(
  private val ignoreAnnotatedOption: StringSetLintOption = StringSetLintOption(IGNORE_ANNOTATED)
) : OptionLoadingDetector(ignoreAnnotatedOption), SourceCodeScanner {

  override fun getApplicableUastTypes(): List<Class<out UElement>> = listOf(UClass::class.java)

  override fun createUastHandler(context: JavaContext): UElementHandler {
    if (!isKotlin(context.psiFile)) return UElementHandler.NONE
    return object : UElementHandler() {
      override fun visitClass(node: UClass) {
        val privateMembers = mutableMapOf<String, UElement>()

        for (field in node.fields) {
          if (!context.evaluator.isPrivate(field)) continue
          if (field.hasAnyAnnotation(ignoreAnnotatedOption.value)) continue
          val name = field.name
          if (name.startsWith("_")) continue
          privateMembers[name] = field
        }

        for (method in node.methods) {
          if (method.isConstructor) continue
          if (!context.evaluator.isPrivate(method)) continue
          if (method.hasAnyAnnotation(ignoreAnnotatedOption.value)) continue
          val name = method.name
          privateMembers[name] = method
        }

        if (privateMembers.isEmpty()) return

        val usedNames = mutableSetOf<String>()
        node.accept(
          object : AbstractUastVisitor() {
            override fun visitSimpleNameReferenceExpression(
              node: USimpleNameReferenceExpression
            ): Boolean {
              usedNames.add(node.identifier)
              return false
            }
          }
        )

        for ((name, member) in privateMembers) {
          // A member declares its own name, so it will appear once in references (its declaration).
          // We check if it's used beyond its declaration by checking if usedNames contains it.
          // Since UAST reference expressions don't include declarations, this is fine.
          if (name !in usedNames) {
            context.report(
              ISSUE,
              member,
              context.getNameLocation(member),
              "Private member `$name` is never used.",
            )
          }
        }
      }
    }
  }

  companion object {
    private val IGNORE_ANNOTATED =
      StringOption(
        "ignore-annotated",
        "Comma-separated list of annotation simple names to ignore.",
        "Preview",
        "Private members annotated with these annotations are excluded.",
      )

    val ISSUE =
      Issue.create(
          id = "UnusedPrivateMember",
          briefDescription = "Private member is never used",
          explanation =
            "Unused private members add noise to the codebase. " + "Remove them or make them used.",
          category = Category.CORRECTNESS,
          priority = 5,
          severity = Severity.WARNING,
          implementation = sourceImplementation<UnusedPrivateMemberDetector>(),
        )
        .setOptions(listOf(IGNORE_ANNOTATED))
  }
}
