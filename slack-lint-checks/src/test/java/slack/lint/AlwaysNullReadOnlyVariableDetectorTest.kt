// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint

import com.android.tools.lint.checks.infrastructure.TestMode
import org.junit.Test

class AlwaysNullReadOnlyVariableDetectorTest : BaseSlackLintTest() {
    override fun getDetector() = AlwaysNullReadOnlyVariableDetector()

    override fun getIssues() = listOf(AlwaysNullReadOnlyVariableDetector.ISSUE)

    @Test
    fun `initializing a read-only variable with null shows warnings`() {
    lint()
      .files(
        kotlin(
            """
            package foo
    
            class Test {
                val str: String? = null
    
                fun method() {
                    val strInFunction: String? = null
                }
            }
            """
        ).indented()
      )
      .skipTestModes(TestMode.JVM_OVERLOADS)
      .allowDuplicates()
      .run()
      .expect(
        """
        src/foo/Test.kt:4: Warning: Avoid initializing read-only variable with null [AvoidNullInitializationForReadOnlyVariables]
            val str: String? = null
                               ~~~~
        src/foo/Test.kt:7: Warning: Avoid initializing read-only variable with null [AvoidNullInitializationForReadOnlyVariables]
                val strInFunction: String? = null
                                             ~~~~
        0 errors, 2 warnings
        """
          .trimIndent()
      )
  }
}
