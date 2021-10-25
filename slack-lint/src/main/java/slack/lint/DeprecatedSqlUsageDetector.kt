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
package slack.lint

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import org.jetbrains.uast.UCallExpression
import slack.lint.util.sourceImplementation

/**
 * Raises a warning on direct sqlite database usage and encourages [SqlDelight](https://cashapp.github.io/sqldelight/)
 * usage.
 */
@Suppress("UnstableApiUsage")
class DeprecatedSqlUsageDetector : Detector(), SourceCodeScanner {

  override fun getApplicableUastTypes() = listOf(UCallExpression::class.java)

  override fun createUastHandler(context: JavaContext) = object : UElementHandler() {
    override fun visitCallExpression(node: UCallExpression) {
      if (APPLICABLE_RECEIVER_TYPES.contains(node.receiverType?.canonicalText) &&
        APPLICABLE_CALL_NAMES.contains(node.methodIdentifier?.name)
      ) {
        context.report(
          issue = ISSUE,
          location = context.getLocation(node),
          message = "All SQL querying should be performed using `SqlDelight`"
        )
      }
    }
  }

  companion object {
    private fun Implementation.toIssue(): Issue {
      return Issue.create(
        id = "DeprecatedSqlUsage",
        briefDescription = "Use SqlDelight!",
        explanation = "Safer, faster, etc",
        category = Category.CORRECTNESS,
        priority = 0,
        severity = Severity.WARNING,
        implementation = this
      )
    }

    val ISSUE: Issue = sourceImplementation<DeprecatedSqlUsageDetector>().toIssue()

    private val APPLICABLE_CALL_NAMES = listOf("query", "insert", "update", "delete", "execSQL")
    private val APPLICABLE_RECEIVER_TYPES =
      listOf("android.database.sqlite.SQLiteDatabase", "androidx.sqlite.db.SupportSQLiteDatabase")
  }
}
