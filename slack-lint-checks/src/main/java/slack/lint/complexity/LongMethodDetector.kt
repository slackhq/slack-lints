// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint.complexity

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.IntOption
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.kdoc.psi.api.KDoc
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UMethod
import slack.lint.util.IntLintOption
import slack.lint.util.OptionLoadingDetector
import slack.lint.util.sourceImplementation

class LongMethodDetector(private val thresholdOption: IntLintOption = IntLintOption(THRESHOLD)) :
  OptionLoadingDetector(thresholdOption), SourceCodeScanner {

  override fun getApplicableUastTypes(): List<Class<out UElement>> = listOf(UMethod::class.java)

  override fun createUastHandler(context: JavaContext): UElementHandler {
    return object : UElementHandler() {
      override fun visitMethod(node: UMethod) {
        val function = node.sourcePsi as? KtNamedFunction ?: return
        val lineCount = countLines(function)
        if (lineCount > thresholdOption.value) {
          context.report(
            ISSUE,
            node,
            context.getNameLocation(node),
            "Function is too long ($lineCount lines), exceeding the limit of ${thresholdOption.value}.",
          )
        }
      }
    }
  }

  /**
   * Counts the lines of code in [function]: distinct physical lines containing a non-whitespace,
   * non-comment token. Top-level functions are measured by their body; nested functions are
   * measured whole. The net line count of every nested function is subtracted so their lines aren't
   * attributed to the enclosing function.
   */
  private fun countLines(function: KtNamedFunction): Int {
    val body = function.bodyBlockExpression ?: function.bodyExpression ?: return 0
    val fileText = function.containingFile?.text ?: return 0
    val newlineOffsets = newlineOffsets(fileText)

    val isNested = PsiTreeUtil.getParentOfType(function, KtNamedFunction::class.java) != null
    val measured: PsiElement = if (isNested) function else body
    return distinctCodeLines(measured, newlineOffsets) -
      nestedFunctionLines(function, newlineOffsets)
  }

  /**
   * Sum of the net line counts of every nested function, where a function's net count is its own
   * whole-function line count minus the net counts of the functions nested within it.
   */
  private fun netLines(function: KtNamedFunction, newlineOffsets: IntArray): Int =
    distinctCodeLines(function, newlineOffsets) - nestedFunctionLines(function, newlineOffsets)

  private fun nestedFunctionLines(function: PsiElement, newlineOffsets: IntArray): Int =
    PsiTreeUtil.findChildrenOfType(function, KtNamedFunction::class.java).sumOf {
      netLines(it, newlineOffsets)
    }

  private fun distinctCodeLines(element: PsiElement, newlineOffsets: IntArray): Int {
    val lines = HashSet<Int>()
    collectCodeLines(element, newlineOffsets, lines)
    return lines.size
  }

  private fun collectCodeLines(element: PsiElement, newlineOffsets: IntArray, lines: HashSet<Int>) {
    // Skip whitespace and comment/KDoc subtrees entirely so they don't count toward length.
    if (element is PsiWhiteSpace || element is PsiComment || element is KDoc) return
    var child = element.firstChild
    if (child == null) {
      lines.add(lineIndex(element.textRange.startOffset, newlineOffsets))
      return
    }
    while (child != null) {
      collectCodeLines(child, newlineOffsets, lines)
      child = child.nextSibling
    }
  }

  /** Returns the 0-based line index for [offset] via binary search over [newlineOffsets]. */
  private fun lineIndex(offset: Int, newlineOffsets: IntArray): Int {
    var lo = 0
    var hi = newlineOffsets.size
    while (lo < hi) {
      val mid = (lo + hi) ushr 1
      if (newlineOffsets[mid] < offset) lo = mid + 1 else hi = mid
    }
    return lo
  }

  private fun newlineOffsets(text: String): IntArray {
    val offsets = ArrayList<Int>()
    text.forEachIndexed { index, c -> if (c == '\n') offsets.add(index) }
    return offsets.toIntArray()
  }

  companion object {
    private val THRESHOLD =
      IntOption("threshold", "Maximum number of lines allowed in a function.", 120)

    val ISSUE =
      Issue.create(
          id = "LongMethod",
          briefDescription = "Function is too long",
          explanation =
            "Long functions are harder to understand, test, and maintain. " +
              "Consider extracting parts of the function into smaller, well-named functions.",
          category = Category.CORRECTNESS,
          priority = 5,
          severity = Severity.WARNING,
          implementation = sourceImplementation<LongMethodDetector>(),
        )
        .setOptions(listOf(THRESHOLD))
  }
}
