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

import com.android.tools.lint.detector.api.Context
import com.android.tools.lint.detector.api.StringOption

object ImportAliasesLoader {

  internal val IMPORT_ALIASES =
    StringOption(
      "import-aliases",
      "A comma-separated list of package name and their import aliases.",
      null,
      "This property should define a comma-separated list of package name and their import aliases" +
        " in the format: packageName as importAlias"
    )

  /**
   * Loads the import aliases from the [IMPORT_ALIASES] option.
   */
  fun loadImportAliases(context: Context): Map<String, String> {
    return IMPORT_ALIASES.getValue(context.configuration)
      ?.splitToSequence(",")
      .orEmpty()
      .map(String::trim)
      .filter(String::isNotBlank)
      .map {
        val (packageName, alias) = it.split(" as ")
        packageName.trim() to alias.trim()
      }.toMap()
  }
}
