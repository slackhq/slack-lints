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
import java.io.File
import java.io.StringReader
import java.util.Properties

object ImportAliasesLoader {
  const val PROPERTY_FILE = "gradle.properties"
  const val IMPORT_ALIASES_PROPERTY = "nonTransitiveRClass.importAliases"

  private const val TOP_LEVEL_FILENAME = "settings.gradle"

  // Caches the import aliases to improve performance.
  var importAliases = emptyMap<String, String>()
    private set

  /**
   * Loads the import aliases from the root [PROPERTY_FILE].
   * If it can't find it, it defaults to the local [PROPERTY_FILE].
   */
  fun loadImportAliases(context: Context) {
    // Project#getPropertyFiles returns the current module's PROPERTY_FILE file.
    // To avoid having to define IMPORT_ALIASES_PROPERTY in every module's PROPERTY_FILE,
    // this loads it from the root folder's PROPERTY_FILE.

    if (importAliases.isEmpty()) {
      val root: File? = findRoot(context)

      File(root, PROPERTY_FILE).run {
        if (this.exists()) {
          val aliasesProperty = getProperty(context, IMPORT_ALIASES_PROPERTY)
          if (aliasesProperty != null)
            loadFromProperty(aliasesProperty)
          else
            loadFromLocalPropertyFile(context)
        } else {
          loadFromLocalPropertyFile(context)
        }
      }
    }
  }

  /**
   * Finds the root folder containing the [TOP_LEVEL_FILENAME] file.
   */
  private fun findRoot(context: Context): File? {
    var file: File? = context.project.dir
    while (file != null && file.listFiles { _, name ->
      name.contains(TOP_LEVEL_FILENAME)
    }?.isEmpty() == true
    ) {
      file = file.parentFile
    }
    return file
  }

  private fun File.getProperty(context: Context, key: String): String? {
    val content = StringReader(context.client.readFile(this).toString())
    val props = Properties()
    props.load(content)
    return props.getProperty(key)
  }

  private fun loadFromProperty(aliasesProperty: String?) {
    aliasesProperty?.splitToSequence(",")
      .orEmpty()
      .map(String::trim)
      .filter(String::isNotBlank)
      .map {
        val (packageName, alias) = it.split(" as ")
        packageName.trim() to alias.trim()
      }.toMap().let { importAliases = it }
  }

  private fun loadFromLocalPropertyFile(context: Context) {
    context.project.propertyFiles.find { it.name == PROPERTY_FILE }?.run {
      loadFromProperty(getProperty(context, IMPORT_ALIASES_PROPERTY))
    }
  }
}
