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
package slack.lint.compose

import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test

class AlwaysProvideAModifierTest : BaseComposeLintTest() {

  override fun getDetector(): Detector = AlwaysProvideAModifier()
  override fun getIssues(): List<Issue> = listOf(AlwaysProvideAModifier.ISSUE)

  @Test
  fun simple() {
    lint()
      .files(
        *commonFiles,
        kotlin(
          """
              package test

              import androidx.compose.runtime.Composable
              import androidx.compose.ui.Modifier

              @Composable
              fun Layout(children: @Composable () -> Unit) {
              }

              // This should error
              @Composable
              fun MissingModifier() {
                Layout {

                }
              }

              // This should not error
              @Composable
              fun MissingModifier() {
                println("Hi")
              }

              // This should not error
              @Composable
              fun HasModifier(modifier: Modifier) {
                Layout {

                }
              }
            """
        ).indented()
      )
      .run()
      .expect(
        """
          src/test/test.kt:12: Error: Composable functions that emit a layout should always have a Modifier parameter. See https://chris.banes.dev/always-provide-a-modifier/ for more details. [AlwaysProvideAModifier]
          fun MissingModifier() {
                             ~~
          1 errors, 0 warnings
        """.trimIndent()
      )
  }
}
