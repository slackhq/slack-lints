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
package slack.lint.compose

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.android.tools.lint.detector.api.TextFormat.TEXT
import com.android.tools.lint.detector.api.isJava
import org.jetbrains.uast.UBlockExpression
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.skipParenthesizedExprDown
import slack.lint.util.sourceImplementation

/**
 * Composable functions that emit a layout should always have a Modifier parameter.
 *
 * See https://chris.banes.dev/always-provide-a-modifier/ for more details.
 *
 * The "known" list of layout functions are currently defined at [COMPOSABLE_EMITTERS_SET] and
 * should be added to eagerly.
 */
class AlwaysProvideAModifier : Detector(), SourceCodeScanner {

  override fun getApplicableUastTypes() = listOf(UMethod::class.java)

  override fun createUastHandler(context: JavaContext): UElementHandler? {
    if (isJava(context.uastFile?.lang)) return null
    return object : UElementHandler() {
      override fun visitMethod(node: UMethod) {
        if (!node.isComposable) return

        // Check for modifier param. If it's already present, nothing further to do!
        if (node.uastParameters.any { context.evaluator.getTypeClass(it.type)?.qualifiedName == MODIFIER }) {
          // TODO - do we actually want to proceed and report if layout calls are not reusing this
          //  param?
          return
        }

        val topLevelExpressions = when (val body = node.uastBody) {
          null -> return
          is UBlockExpression -> body.expressions
          else -> listOf(body)
        }
        if (topLevelExpressions.isEmpty()) return
        val hasTopLevelLayout = topLevelExpressions
          .map { it.skipParenthesizedExprDown() }
          .any { expression ->
            // TODO do we care about the package name to be super sure?
            expression is UCallExpression && expression.methodName in COMPOSABLE_EMITTERS_SET
          }
        if (hasTopLevelLayout) {
          context.report(
            ISSUE,
            node,
            context.getLocation(node.parameterList),
            ISSUE.getExplanation(TEXT)
          )
        }
      }
    }
  }

  companion object {
    private const val MODIFIER = "androidx.compose.ui.Modifier"
    private val COMPOSABLE_EMITTERS_SET = setOf(
      // androidx.compose.foundation
      "BasicTextField",
      "Box",
      "Canvas",
      "ClickableText",
      "Column",
      "Icon",
      "Image",
      "Layout",
      "LazyColumn",
      "LazyRow",
      "LazyVerticalGrid",
      "Row",
      "Text",
      // android.compose.material
      "BottomDrawer",
      "Button",
      "Card",
      "Checkbox",
      "CircularProgressIndicator",
      "Divider",
      "ExposedDropdownMenuBox",
      "ExtendedFloatingActionButton",
      "FloatingActionButton",
      "IconButton",
      "IconToggleButton",
      "LeadingIconTab",
      "LinearProgressIndicator",
      "ListItem",
      "ModalBottomSheetLayout",
      "ModalDrawer",
      "NavigationRail",
      "NavigationRailItem",
      "OutlinedButton",
      "OutlinedTextField",
      "RadioButton",
      "Scaffold",
      "ScrollableTabRow",
      "Slider",
      "SnackbarHost",
      "Surface",
      "SwipeToDismiss",
      "Switch",
      "Tab",
      "TabRow",
      "TextButton",
      "TopAppBar",
      // Accompanist
      "BottomNavigation",
      "BottomNavigationContent",
      "BottomNavigationSurface",
      "FlowColumn",
      "FlowRow",
      "HorizontalPager",
      "HorizontalPagerIndicator",
      "SwipeRefresh",
      "SwipeRefreshIndicator",
      "TopAppBarContent",
      "TopAppBarSurface",
      "VerticalPager",
      "VerticalPagerIndicator",
      "WebView",
    )

    val ISSUE: Issue = Issue.create(
      "AlwaysProvideAModifier",
      "Always provide a Modifier.",
      "Composable functions that emit a layout should always have a Modifier parameter. See https://chris.banes.dev/always-provide-a-modifier/ for more details.",
      Category.CORRECTNESS,
      10,
      Severity.ERROR,
      sourceImplementation<AlwaysProvideAModifier>()
    )
  }
}
