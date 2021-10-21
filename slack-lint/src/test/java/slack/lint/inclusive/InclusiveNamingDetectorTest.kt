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
package slack.lint.inclusive

import com.android.tools.lint.checks.infrastructure.TestFile.PropertyTestFile
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Ignore
import org.junit.Test
import slack.lint.BaseSlackLintTest

@Suppress("UnstableApiUsage")
class InclusiveNamingDetectorTest : BaseSlackLintTest() {

  private fun propertiesFile(): PropertyTestFile = projectProperties().apply {
    property(InclusiveNamingChecker.BLOCK_LIST_PROPERTY, "fork,knife,spoon,spork")
    to(InclusiveNamingChecker.PROPERTY_FILE)
  }

  override fun getDetector(): Detector = InclusiveNamingSourceCodeScanner()
  override fun getIssues(): List<Issue> = InclusiveNamingChecker.ISSUES.toList()

  @Test
  fun kotlin() {
    // This covers the following cases:
    // - Class
    // - Parameter
    // - Property
    // - Local var
    // - Label
    // - Function
    lint()
      .files(
        propertiesFile(),
        kotlin(
          "test/ForkHandler.kt",
          """
              class Spork(val sporkBranch: String, sporkParam: String) {
                val knife = ""

                fun spoonBranch(val spoonRef: String) {
                  val localFork = ""
                  emptyList<String>()
                      .forEach spoonRefs@ {

                       }
                }
              }
            """
        ).indented()
      )
      .run()
      .expect(
        """
          test/ForkHandler.kt:1: Error: Use inclusive naming. Matched string is 'fork' in file name 'ForkHandler.kt' [InclusiveNaming]
          class Spork(val sporkBranch: String, sporkParam: String) {
          ^
          test/ForkHandler.kt:1: Error: Use inclusive naming. Matched string is 'spork' in class name 'Spork' [InclusiveNaming]
          class Spork(val sporkBranch: String, sporkParam: String) {
          ^
          test/ForkHandler.kt:1: Error: Use inclusive naming. Matched string is 'spork' in parameter name 'sporkParam' [InclusiveNaming]
          class Spork(val sporkBranch: String, sporkParam: String) {
                                               ~~~~~~~~~~~~~~~~~~
          test/ForkHandler.kt:1: Error: Use inclusive naming. Matched string is 'spork' in property name 'sporkBranch' [InclusiveNaming]
          class Spork(val sporkBranch: String, sporkParam: String) {
                      ~~~~~~~~~~~~~~~~~~~~~~~
          test/ForkHandler.kt:2: Error: Use inclusive naming. Matched string is 'knife' in property name 'knife' [InclusiveNaming]
            val knife = ""
            ~~~~~~~~~~~~~~
          test/ForkHandler.kt:4: Error: Use inclusive naming. Matched string is 'spoon' in function name 'spoonBranch' [InclusiveNaming]
            fun spoonBranch(val spoonRef: String) {
            ^
          test/ForkHandler.kt:4: Error: Use inclusive naming. Matched string is 'spoon' in parameter name 'spoonRef' [InclusiveNaming]
            fun spoonBranch(val spoonRef: String) {
                            ~~~~~~~~~~~~~~~~~~~~
          test/ForkHandler.kt:5: Error: Use inclusive naming. Matched string is 'fork' in local variable name 'localFork' [InclusiveNaming]
              val localFork = ""
              ~~~~~~~~~~~~~~~~~~
          test/ForkHandler.kt:7: Error: Use inclusive naming. Matched string is 'spoon' in label name 'spoonRefs' [InclusiveNaming]
                  .forEach spoonRefs@ {
                           ^
          9 errors, 0 warnings
        """.trimIndent()
      )
  }

  @Test
  fun java() {
    // This covers the following cases:
    // - Class
    // - Parameter
    // - Field
    // - Local var
    // - Method
    lint()
      .files(
        propertiesFile(),
        java(
          "test/ForkHandler.java",
          """
              class ForkHandler {
                String knife = "";

                void spoonBranch(String spoonRef) {
                  String localFork = "";
                }
              }
            """
        ).indented()
      )
      .run()
      .expect(
        """
          test/ForkHandler.java:1: Error: Use inclusive naming. Matched string is 'fork' in class name 'ForkHandler' [InclusiveNaming]
          class ForkHandler {
          ^
          test/ForkHandler.java:1: Error: Use inclusive naming. Matched string is 'fork' in file name 'ForkHandler.java' [InclusiveNaming]
          class ForkHandler {
          ^
          test/ForkHandler.java:2: Error: Use inclusive naming. Matched string is 'knife' in field name 'knife' [InclusiveNaming]
            String knife = "";
            ~~~~~~~~~~~~~~~~~~
          test/ForkHandler.java:4: Error: Use inclusive naming. Matched string is 'spoon' in method name 'spoonBranch' [InclusiveNaming]
            void spoonBranch(String spoonRef) {
            ^
          test/ForkHandler.java:4: Error: Use inclusive naming. Matched string is 'spoon' in parameter name 'spoonRef' [InclusiveNaming]
            void spoonBranch(String spoonRef) {
                             ~~~~~~~~~~~~~~~
          test/ForkHandler.java:5: Error: Use inclusive naming. Matched string is 'fork' in local variable name 'localFork' [InclusiveNaming]
              String localFork = "";
              ~~~~~~~~~~~~~~~~~~~~~~
          6 errors, 0 warnings
        """.trimIndent()
      )
  }

  @Ignore("Not working juuuuuust yet. Left as a toe-hold")
  @Test
  fun xml() {
    // Attr
    lint()
      .files(
        propertiesFile(),
        xml(
          "test_file.xml",
          """
              <?xml version="1.0" encoding="utf-8"?>
              <com.example.SomeView
                  xmlns:android="http://schemas.android.com/apk/res/android"
                  android:masterAttribute="testing"
                  />
            """
        ).indented()
      )
      .run()
      .expect(
        """

        """.trimIndent()
      )
  }
}
