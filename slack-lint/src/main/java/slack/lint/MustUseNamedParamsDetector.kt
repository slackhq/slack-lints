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
package slack.lint

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.android.tools.lint.detector.api.TextFormat
import com.android.tools.lint.detector.api.isJava
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.psi.KtValueArgumentList
import org.jetbrains.kotlin.psi.KtValueArgumentName
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import org.jetbrains.uast.UCallExpression
import slack.lint.util.sourceImplementation

class MustUseNamedParamsDetector : Detector(), SourceCodeScanner {

  override fun getApplicableUastTypes() = listOf(UCallExpression::class.java)

  override fun createUastHandler(context: JavaContext): UElementHandler {
    return object : UElementHandler() {
      override fun visitCallExpression(node: UCallExpression) {
        val method = node.resolve() ?: return

        // Java doesn't have named parameters.
        if (isJava(method.language)) return

        if (method.hasAnnotation("slack.lint.annotations.MustUseNamedParams")) {
          val areAllNamed = node.sourcePsi!!
            .getChildOfType<KtValueArgumentList>()!!
            .children.filterIsInstance<KtValueArgument>()
            .all { it.getChildOfType<KtValueArgumentName>() != null }

          if (!areAllNamed) {
            context.report(
              ISSUE,
              node,
              context.getLocation(node),
              ISSUE.getBriefDescription(TextFormat.TEXT)
            )
          }
        }
      }
    }
  }

  companion object {
    val ISSUE: Issue = Issue.create(
      "MustUseNamedParams",
      "Calls to @MustUseNamedParams-annotated methods must name all parameters.",
      "Calls to @MustUseNamedParams-annotated methods must name all parameters.",
      Category.CORRECTNESS,
      9,
      Severity.ERROR,
      sourceImplementation<MustUseNamedParamsDetector>()
    )
  }
}
