/*
 * Copyright (C) 2022 Slack Technologies, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package slack.lint.resources

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Context
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.LintFix
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UImportStatement
import org.jetbrains.uast.USimpleNameReferenceExpression
import slack.lint.resources.model.IMPORT_ALIASES
import slack.lint.resources.model.RootIssueData
import slack.lint.util.sourceImplementation

/** Reports an error when an R class is imported using the wrong import alias. */
class WrongResourceImportAliasDetector : Detector(), SourceCodeScanner {

  private val fixes = mutableListOf<LintFix>()
  private var rootIssueData: RootIssueData? = null

  override fun afterCheckFile(context: Context) {
    rootIssueData?.let {
      context.report(
        ISSUE,
        it.nameLocation,
        "Please use ${it.alias} as an import alias here",
        quickfixData = fix()
          .name("Replace import alias")
          // Apply the fixes in reverse so that the ranges/locations don't change.
          .composite(*fixes.reversed().toTypedArray())
          .autoFix()
      )

      reset()
    }
  }

  private fun reset() {
    rootIssueData = null
    fixes.clear()
  }

  override fun getApplicableUastTypes(): List<Class<out UElement>> = listOf(UImportStatement::class.java, USimpleNameReferenceExpression::class.java)

  override fun createUastHandler(context: JavaContext): UElementHandler {
    return object : UElementHandler() {
      private var wrongAlias: String? = null

      override fun visitImportStatement(node: UImportStatement) {
        // Import alias is a Kotlin feature.
        val importDirective = node.sourcePsi as? KtImportDirective ?: return

        // In case of multiple wrong aliases, only fix the first one.
        if (wrongAlias != null) return

        val importedFqNameString = importDirective.importedFqName?.asString() ?: return
        if (importedFqNameString.endsWith(".R") && importDirective.aliasName != null) {
          IMPORT_ALIASES[importedFqNameString]?.let { alias ->
            val aliasName = importDirective.aliasName
            if (alias != aliasName) {
              this.wrongAlias = aliasName
              this@WrongResourceImportAliasDetector.rootIssueData =
                RootIssueData(
                  alias = alias,
                  nameLocation = context.getNameLocation(importDirective)
                )

              fixes.add(createImportLintFix(node, importedFqNameString, aliasName, alias))
            }
          }
        }
      }

      private fun createImportLintFix(
        node: UImportStatement,
        importedFqNameString: String,
        aliasName: String?,
        alias: String
      ): LintFix {
        return fix()
          .replace()
          .range(context.getLocation(node))
          .text("$importedFqNameString as $aliasName")
          .with("$importedFqNameString as $alias")
          .build()
      }

      override fun visitSimpleNameReferenceExpression(node: USimpleNameReferenceExpression) {
        wrongAlias?.let {
          if (node.asSourceString() == it) {
            fixes.add(createReferenceLintFix(node))
          }
        }
      }

      private fun createReferenceLintFix(node: USimpleNameReferenceExpression): LintFix {
        return fix()
          .replace()
          .range(context.getLocation(node))
          .with(rootIssueData?.alias)
          .build()
      }
    }
  }

  companion object {

    val ISSUE: Issue =
      Issue.create(
        "WrongResourceImportAlias",
        "Wrong import alias for this R class.",
        "R class import aliases should be consistent across the codebase. For example: \n" +
          "import slack.l10n.R as L10nR\n" +
          "import slack.uikit.R as UiKitR",
        Category.CORRECTNESS,
        6,
        Severity.ERROR,
        sourceImplementation<WrongResourceImportAliasDetector>()
      )
  }
}
