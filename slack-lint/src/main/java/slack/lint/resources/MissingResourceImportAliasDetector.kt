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
import org.jetbrains.uast.getQualifiedParentOrThis
import slack.lint.resources.ImportAliasesLoader.IMPORT_ALIASES
import slack.lint.resources.model.RootIssueData
import slack.lint.util.sourceImplementation

/** Reports an error when an R class, other than the local module's, is imported without an import alias. */
class MissingResourceImportAliasDetector : Detector(), SourceCodeScanner {

  private val fixes = mutableListOf<LintFix>()
  private var rootIssueData: RootIssueData? = null

  private lateinit var importAliases: Map<String, String>

  override fun beforeCheckRootProject(context: Context) {
    super.beforeCheckRootProject(context)

    importAliases = ImportAliasesLoader.loadImportAliases(context)
  }

  override fun afterCheckFile(context: Context) {
    // Collect all the fixes and apply them to one issue on the import to avoid adding the import alias with a fix
    // and leaving the R references still referencing the non-aliased R or vice versa.
    rootIssueData?.let {
      context.report(
        ISSUE,
        it.nameLocation,
        "Use an import alias for R classes from other modules",
        quickfixData =
        fix()
          .name("Add import alias")
          // Apply the fixes in reverse so that the ranges/locations don't change.
          .composite(*fixes.reversed().toTypedArray())
          .autoFix()
      )
      rootIssueData = null
      fixes.clear()
    }
  }

  override fun getApplicableUastTypes(): List<Class<out UElement>> =
    listOf(UImportStatement::class.java, USimpleNameReferenceExpression::class.java)

  override fun createUastHandler(context: JavaContext): UElementHandler {
    return object : UElementHandler() {

      override fun visitImportStatement(node: UImportStatement) {
        // Import alias is a Kotlin feature.
        val importDirective = node.sourcePsi as? KtImportDirective ?: return

        if (importDirective.importedName?.identifier == "R" && importDirective.aliasName == null) {
          val packageName = context.project.`package` ?: return

          val importedFqName = importDirective.importedFqName
          val parentImportedFqName = importedFqName?.parent()?.asString()

          val isLocalResourceImport = parentImportedFqName?.let { it == packageName } ?: true
          if (!isLocalResourceImport) {
            val importedFqNameString = requireNotNull(importedFqName).asString()
            val alias = importAliases[importedFqNameString]

            if (alias != null) {
              rootIssueData = RootIssueData(alias = alias, nameLocation = context.getNameLocation(importDirective))

              fixes.add(createImportLintFix(importDirective, importedFqNameString, alias))
            } else {
              context.report(ISSUE, context.getNameLocation(importDirective), "Use an import alias for R classes from other modules")
            }
          }
        }
      }

      private fun createImportLintFix(importDirective: KtImportDirective, importedFqNameString: String, alias: String?): LintFix {
        return fix()
          .replace()
          .range(context.getLocation(importDirective))
          .text(importedFqNameString)
          .with("$importedFqNameString as $alias")
          .build()
      }

      override fun visitSimpleNameReferenceExpression(node: USimpleNameReferenceExpression) {
        rootIssueData?.alias?.let {
          if (node.asSourceString() == "R" &&
            // Make sure node is its own parent to safeguard against cases like Build.VERSION_CODES.R.
            node.getQualifiedParentOrThis() == node
          ) {
            fixes.add(createReferenceLintFix(node, it))
          }
        }
      }

      private fun createReferenceLintFix(node: USimpleNameReferenceExpression, alias: String): LintFix {
        return fix().replace().range(context.getLocation(node)).with(alias).build()
      }
    }
  }

  companion object {
    val ISSUE: Issue =
      Issue.create(
        "MissingResourceImportAlias",
        "Missing import alias for R class.",
        """
          Only the local module's R class is allowed to be imported without an alias. \
          Add an import alias for this. For example, import slack.l10n.R as L10nR
          """,
        Category.CORRECTNESS,
        6,
        Severity.ERROR,
        sourceImplementation<MissingResourceImportAliasDetector>()
      ).setOptions(listOf(IMPORT_ALIASES))
  }
}
