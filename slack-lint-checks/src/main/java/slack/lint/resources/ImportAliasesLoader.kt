// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint.resources

import com.android.tools.lint.detector.api.Context
import com.android.tools.lint.detector.api.StringOption

object ImportAliasesLoader {

  internal val IMPORT_ALIASES =
    StringOption(
      "import-aliases",
      "A comma-separated list of package name and their import aliases.",
      null,
      "This property should define a comma-separated list of package name and their import aliases" +
        " in the format: packageName as importAlias",
    )

  /** Loads the import aliases from the [IMPORT_ALIASES] option. */
  fun loadImportAliases(context: Context): Map<String, String> {
    return IMPORT_ALIASES.getValue(context.configuration)
      ?.splitToSequence(",")
      .orEmpty()
      .map(String::trim)
      .filter(String::isNotBlank)
      .map {
        val (packageName, alias) = it.split(" as ")
        packageName.trim() to alias.trim()
      }
      .toMap()
  }
}
