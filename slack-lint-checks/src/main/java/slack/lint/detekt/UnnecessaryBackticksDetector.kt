// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint.detekt

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.LintFix
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.android.tools.lint.detector.api.isKotlin
import org.jetbrains.uast.UDeclaration
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UField
import org.jetbrains.uast.ULocalVariable
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.UParameter
import slack.lint.util.sourceImplementation

private val KOTLIN_KEYWORDS =
  setOf(
    "as",
    "break",
    "class",
    "continue",
    "do",
    "else",
    "false",
    "for",
    "fun",
    "if",
    "in",
    "interface",
    "is",
    "null",
    "object",
    "package",
    "return",
    "super",
    "this",
    "throw",
    "true",
    "try",
    "typealias",
    "typeof",
    "val",
    "var",
    "when",
    "while",
  )

private val IDENTIFIER_REGEX = Regex("^[a-zA-Z_][a-zA-Z0-9_]*$")

class UnnecessaryBackticksDetector : Detector(), SourceCodeScanner {

  override fun getApplicableUastTypes(): List<Class<out UElement>> =
    listOf(UMethod::class.java, UField::class.java, ULocalVariable::class.java)

  override fun createUastHandler(context: JavaContext): UElementHandler {
    if (!isKotlin(context.psiFile)) return UElementHandler.NONE
    return object : UElementHandler() {
      override fun visitMethod(node: UMethod) = checkDeclaration(context, node)

      override fun visitField(node: UField) = checkDeclaration(context, node)

      override fun visitLocalVariable(node: ULocalVariable) {
        val sourcePsi = node.sourcePsi ?: return
        val text = sourcePsi.text
        val nameStart = text.indexOf('`')
        if (nameStart == -1) return
        val nameEnd = text.indexOf('`', nameStart + 1)
        if (nameEnd == -1) return
        val backtickName = text.substring(nameStart + 1, nameEnd)
        if (isBacktickUnnecessary(backtickName)) {
          context.report(
            ISSUE,
            node as UElement,
            context.getNameLocation(node),
            "Unnecessary backticks around `$backtickName`.",
            createFix(backtickName),
          )
        }
      }
    }
  }

  private fun checkDeclaration(context: JavaContext, node: UDeclaration) {
    val sourcePsi = node.sourcePsi ?: return
    val text = sourcePsi.text
    val nameStart = text.indexOf('`')
    if (nameStart == -1) return
    val nameEnd = text.indexOf('`', nameStart + 1)
    if (nameEnd == -1) return
    val backtickName = text.substring(nameStart + 1, nameEnd)
    if (isBacktickUnnecessary(backtickName)) {
      context.report(
        ISSUE,
        node as UElement,
        context.getNameLocation(node),
        "Unnecessary backticks around `$backtickName`.",
        createFix(backtickName),
      )
    }
  }

  private fun isBacktickUnnecessary(name: String): Boolean {
    if (name in KOTLIN_KEYWORDS) return false
    return IDENTIFIER_REGEX.matches(name)
  }

  private fun createFix(name: String): LintFix {
    return fix().replace().text("`$name`").with(name).build()
  }

  companion object {
    val ISSUE =
      Issue.create(
        id = "UnnecessaryBackticks",
        briefDescription = "Unnecessary backticks around identifier",
        explanation =
          "Backticks are only needed for identifiers that are Kotlin keywords " +
            "or contain special characters. Using them on regular identifiers is unnecessary noise.",
        category = Category.CORRECTNESS,
        priority = 3,
        severity = Severity.WARNING,
        implementation = sourceImplementation<UnnecessaryBackticksDetector>(),
      )
  }
}
