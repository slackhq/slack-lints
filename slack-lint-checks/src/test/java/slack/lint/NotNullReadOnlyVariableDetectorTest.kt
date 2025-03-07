package slack.lint

import com.android.tools.lint.checks.infrastructure.TestMode
import org.junit.Test

class NotNullReadOnlyVariableDetectorTest : BaseSlackLintTest() {
  override fun getDetector() = NotNullReadOnlyVariableDetector()

  override fun getIssues() = listOf(NotNullReadOnlyVariableDetector.ISSUE)

  @Test
  fun testDocumentationExample() {
    lint()
      .files(
        kotlin(
          """
                            package foo
                
                            val nullableKtString: String? = null

                            class Test {
                                val nullableString: String? = null
                                val nullableFloat: Float? = null
                             
                                fun method() {
                                    val nullableLocalString: String? = null 
                                }

                                companion object {
                                    val nullableCompanionString: String? = null
                                }
                            }
                        """
        )
      )
      .skipTestModes(TestMode.JVM_OVERLOADS)
      .allowDuplicates()
      .run()
      .expect(
        """
                    src/foo/Test.kt:4: Warning: Avoid initializing read-only variable with null [AvoidNullInitializationForReadOnlyVariables]
                                                val nullableKtString: String? = null
                                                                                ~~~~
                    src/foo/Test.kt:7: Warning: Avoid initializing read-only variable with null [AvoidNullInitializationForReadOnlyVariables]
                                                    val nullableString: String? = null
                                                                                  ~~~~
                    src/foo/Test.kt:8: Warning: Avoid initializing read-only variable with null [AvoidNullInitializationForReadOnlyVariables]
                                                    val nullableFloat: Float? = null
                                                                                ~~~~
                    src/foo/Test.kt:11: Warning: Avoid initializing read-only variable with null [AvoidNullInitializationForReadOnlyVariables]
                                                        val nullableLocalString: String? = null 
                                                                                           ~~~~
                    src/foo/Test.kt:15: Warning: Avoid initializing read-only variable with null [AvoidNullInitializationForReadOnlyVariables]
                                                        val nullableCompanionString: String? = null
                                                                                               ~~~~
                    0 errors, 5 warnings
                """

          .trimIndent()
      )
  }
}