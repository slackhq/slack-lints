/*
 * Copyright (C) 2021 Slack Technologies, LLC
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
@file:Suppress("UnstableApiUsage")

package slack.lint.inclusive

import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Context
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Location
import com.android.tools.lint.detector.api.Position
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.TextFormat
import com.android.tools.lint.detector.api.XmlContext
import org.jetbrains.uast.UElement
import org.w3c.dom.Node
import slack.lint.util.Priorities
import slack.lint.util.resourcesImplementation
import slack.lint.util.sourceImplementation
import java.io.StringReader
import java.util.Locale
import java.util.Properties

sealed class InclusiveNamingChecker<C : Context, N> {
  companion object {

    private val SOURCE_ISSUE = sourceImplementation<InclusiveNamingSourceCodeScanner>().toIssue()
    private val RESOURCES_ISSUE = resourcesImplementation<InclusiveNamingResourceScanner>().toIssue()

    val ISSUES: Array<Issue> = arrayOf(
      SOURCE_ISSUE,
      RESOURCES_ISSUE
    )

    private fun Implementation.toIssue(): Issue {
      return Issue.create(
        "InclusiveNaming",
        "Use inclusive naming.",
        """
          We try to use inclusive naming at Slack. Terms such as blacklist, whitelist, master, slave, etc, while maybe \
          widely used today, can be socially charged and make others feel excluded or uncomfortable.
        """.trimIndent(),
        Category.CORRECTNESS,
        Priorities.NORMAL,
        Severity.ERROR,
        this
      )
    }

    const val PROPERTY_FILE = "gradle.properties"
    const val BLOCK_LIST_PROPERTY = "inclusiveNaming.blocklist"

    /**
     * Loads a comma-separated list of blocked words from [BLOCK_LIST_PROPERTY] set in the root project's
     * `gradle.properties`.
     */
    fun loadBlocklist(context: Context): Set<String> {
      val props = Properties()
      return context.project.propertyFiles.find { it.name == PROPERTY_FILE }?.run {
        val content = StringReader(context.client.readFile(this).toString())
        props.load(content)
        props.getProperty(BLOCK_LIST_PROPERTY)
          ?.splitToSequence(",")
          .orEmpty()
          .map(String::trim)
          .filter(String::isNotBlank)
          .toSet()
      } ?: emptySet()
    }
  }

  abstract val context: C
  abstract val blocklist: Set<String>
  protected abstract val issue: Issue
  abstract fun locationFor(node: N): Location
  open fun shouldReport(node: N, location: Location, name: String, isFile: Boolean): Boolean = true

  fun check(node: N, name: String?, type: String, isFile: Boolean = false) {
    if (name == null) return
    val lowerCased = name.toLowerCase(Locale.US)
    blocklist.find { it in lowerCased }?.let { matched ->
      val location = locationFor(node)
      if (!shouldReport(node, location, name, isFile)) return
      val description = buildString {
        append(issue.getBriefDescription(TextFormat.TEXT))
        append(" Matched string is '")
        append(matched)
        append("' in ")
        append(type)
        append(" name '")
        append(name)
        append("'")
      }
      context.report(
        issue,
        location,
        description,
        null
      )
    }
  }

  class SourceCodeChecker(
    override val context: JavaContext,
    override val blocklist: Set<String>
  ) : InclusiveNamingChecker<JavaContext, UElement>() {
    /**
     * Some element types will be reported multiple times (such as property parameters). This caches reports so we
     * only report once.
     */
    private val cachedReports = mutableSetOf<CacheKey>()
    override val issue: Issue = SOURCE_ISSUE

    override fun locationFor(node: UElement): Location {
      return context.getLocation(node)
    }

    override fun shouldReport(node: UElement, location: Location, name: String, isFile: Boolean): Boolean {
      return cachedReports.add(CacheKey.fromLocation(location, isFile))
    }

    private data class CacheKey(val location: String) {
      companion object {
        fun fromLocation(location: Location, isFile: Boolean): CacheKey {
          val fileName = location.file.name
          val start: String
          val end: String
          if (isFile) {
            start = ""
            end = ""
          } else {
            start = location.start?.lineString ?: ""
            end = location.end?.lineString ?: ""
          }
          return CacheKey("$fileName-$start-$end")
        }

        private val Position.lineString: String
          get() {
            return "$line:$column"
          }
      }
    }
  }

  class XmlChecker(
    override val context: XmlContext,
    override val blocklist: Set<String>
  ) : InclusiveNamingChecker<XmlContext, Node>() {
    override val issue: Issue = RESOURCES_ISSUE
    override fun locationFor(node: Node): Location {
      return context.getLocation(node)
    }
  }
}
