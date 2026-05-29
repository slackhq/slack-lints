// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint.style

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.IntOption
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import org.jetbrains.uast.UElement
import org.jetbrains.uast.ULiteralExpression
import slack.lint.util.IntLintOption
import slack.lint.util.OptionLoadingDetector
import slack.lint.util.sourceImplementation

class UnderscoresInNumericLiteralsDetector(
  private val acceptableLengthOption: IntLintOption = IntLintOption(ACCEPTABLE_LENGTH)
) : OptionLoadingDetector(acceptableLengthOption), SourceCodeScanner {

  override fun getApplicableUastTypes(): List<Class<out UElement>> =
    listOf(ULiteralExpression::class.java)

  override fun createUastHandler(context: JavaContext): UElementHandler {
    return object : UElementHandler() {
      override fun visitLiteralExpression(node: ULiteralExpression) {
        val sourcePsi = node.sourcePsi ?: return
        val text = sourcePsi.text
        if (!isNumericLiteral(text)) return
        val digits = extractDigitPart(text)
        if (digits.length >= acceptableLengthOption.value && !digits.contains('_')) {
          context.report(
            ISSUE,
            node,
            context.getLocation(node),
            "Numeric literal `$text` should use underscores for readability (length >= ${acceptableLengthOption.value}).",
          )
        }
      }
    }
  }

  private fun isNumericLiteral(text: String): Boolean {
    if (text.isEmpty()) return false
    val first = text[0]
    return first.isDigit() || (first == '-' && text.length > 1 && text[1].isDigit())
  }

  private fun extractDigitPart(text: String): String {
    val stripped = text.removeSuffix("L").removeSuffix("l").removeSuffix("f").removeSuffix("F")
    if (
      stripped.startsWith("0x", ignoreCase = true) || stripped.startsWith("0b", ignoreCase = true)
    )
      return ""
    val dotIndex = stripped.indexOf('.')
    return if (dotIndex >= 0) {
      val intPart = stripped.substring(0, dotIndex).removePrefix("-")
      intPart
    } else {
      stripped.removePrefix("-")
    }
  }

  companion object {
    private val ACCEPTABLE_LENGTH =
      IntOption("acceptable-length", "Minimum number of digits before underscores are required.", 5)

    val ISSUE =
      Issue.create(
          id = "UnderscoresInNumericLiterals",
          briefDescription = "Long numeric literal without underscores",
          explanation =
            "Long numeric literals are more readable with underscores grouping digits " +
              "(e.g. `1_000_000` instead of `1000000`).",
          category = Category.USABILITY,
          priority = 3,
          severity = Severity.WARNING,
          implementation = sourceImplementation<UnderscoresInNumericLiteralsDetector>(),
        )
        .setOptions(listOf(ACCEPTABLE_LENGTH))
  }
}
