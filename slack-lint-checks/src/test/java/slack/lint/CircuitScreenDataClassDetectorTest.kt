// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint

import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test

class CircuitScreenDataClassDetectorTest : BaseSlackLintTest() {

  override fun getDetector(): Detector = CircuitScreenDataClassDetector()

  override fun getIssues(): List<Issue> = listOf(CircuitScreenDataClassDetector.ISSUE)

  @Test
  fun `Test CircuitScreenDataClassDetector - regular class implements Screen - fails`() {
    val testFile =
      kotlin(
          """
            package com.example.screens

            import com.slack.circuit.runtime.screen.Screen

            @Parcelize
            class HomeScreen(val userId: String) : Screen {
                data class State(message: String) : State
            }
        """
            .trimIndent()
        )
        .indented()

    lint()
      .files(circuitScreenStub, testFile)
      .run()
      .expect(
        """
        src/com/example/screens/HomeScreen.kt:6: Error: ${CircuitScreenDataClassDetector.MESSAGE} [${CircuitScreenDataClassDetector.ISSUE_ID}]
        class HomeScreen(val userId: String) : Screen {
        ~~~~~
        1 error
        """
          .trimIndent()
      )
  }

  @Test
  fun `Test CircuitScreenDataClassDetector - data class implements Screen - succeeds`() {
    val testFile =
      kotlin(
          """
            package com.example.screens

            import com.slack.circuit.runtime.screen.Screen

            data class HomeScreen(val userId: String) : Screen
        """
            .trimIndent()
        )
        .indented()

    lint().files(circuitScreenStub, testFile).run().expectClean()
  }

  @Test
  fun `Test CircuitScreenDataClassDetector - data object implements Screen - succeeds`() {
    val testFile =
      kotlin(
          """
            package com.example.screens

            import com.slack.circuit.runtime.screen.Screen

            data object SettingsScreen : Screen
        """
            .trimIndent()
        )
        .indented()

    lint().files(circuitScreenStub, testFile).run().expectClean()
  }

  @Test
  fun `Test CircuitScreenDataClassDetector - interface extends Screen - succeeds`() {
    val testFile =
      kotlin(
          """
            package com.example.screens

            import com.slack.circuit.runtime.screen.Screen

            interface CustomScreen : Screen {
              val id: String
            }
        """
            .trimIndent()
        )
        .indented()

    lint().files(circuitScreenStub, testFile).run().expectClean()
  }

  @Test
  fun `Test CircuitScreenDataClassDetector - regular class not implementing Screen - succeeds`() {
    val testFile =
      kotlin(
          """
            package com.example.screens

            class HomeViewModel(val userId: String)
        """
            .trimIndent()
        )
        .indented()

    lint().files(testFile).run().expectClean()
  }

  @Test
  fun `Test CircuitScreenDataClassDetector - class name contains 'class' keyword - only replaces declaration`() {
    val testFile =
      kotlin(
          """
            package com.example.screens

            import com.slack.circuit.runtime.screen.Screen

            class MyClassScreen(val id: String) : Screen
        """
            .trimIndent()
        )
        .indented()

    lint()
      .files(circuitScreenStub, testFile)
      .run()
      .expect(
        """
        src/com/example/screens/MyClassScreen.kt:5: Error: ${CircuitScreenDataClassDetector.MESSAGE} [${CircuitScreenDataClassDetector.ISSUE_ID}]
        class MyClassScreen(val id: String) : Screen
        ~~~~~
        1 error
        """
          .trimIndent()
      )
  }

  @Test
  fun `Test CircuitScreenDataClassDetector - regular object class implements Screen - suggests data object`() {
    val testFile =
      kotlin(
          """
            package com.example.screens

            import com.slack.circuit.runtime.screen.Screen

            object SettingsScreen : Screen
        """
            .trimIndent()
        )
        .indented()

    lint()
      .files(circuitScreenStub, testFile)
      .run()
      .expect(
        """
        src/com/example/screens/SettingsScreen.kt:5: Error: ${CircuitScreenDataClassDetector.MESSAGE} [${CircuitScreenDataClassDetector.ISSUE_ID}]
        object SettingsScreen : Screen
        ~~~~~~
        1 error
        """
          .trimIndent()
      )
  }

  @Test
  fun `Test CircuitScreenDataClassDetector - open class implements Screen - succeeds`() {
    val testFile =
      kotlin(
          """
            package com.example.screens

            import com.slack.circuit.runtime.screen.Screen

            open class ProfileScreen(val userId: String) : Screen
        """
            .trimIndent()
        )
        .indented()

    lint().files(circuitScreenStub, testFile).run().expectClean()
  }

  @Test
  fun `Test CircuitScreenDataClassDetector - companion object implements Screen - fails`() {
    val testFile =
      kotlin(
          """
            package com.example.screens

            import com.slack.circuit.runtime.screen.Screen

            class SomeClass {
                companion object NavScreen : Screen
            }
        """
            .trimIndent()
        )
        .indented()

    lint().files(circuitScreenStub, testFile).run().expectClean()
  }

  @Test
  fun `Test CircuitScreenDataClassDetector - nested class implements Screen - fails`() {
    val testFile =
      kotlin(
          """
            package com.example.screens

            import com.slack.circuit.runtime.screen.Screen

            class OuterClass {
                class NestedScreen(val id: String) : Screen
            }
        """
            .trimIndent()
        )
        .indented()

    lint()
      .files(circuitScreenStub, testFile)
      .run()
      .expect(
        """
        src/com/example/screens/OuterClass.kt:6: Error: ${CircuitScreenDataClassDetector.MESSAGE} [${CircuitScreenDataClassDetector.ISSUE_ID}]
            class NestedScreen(val id: String) : Screen
            ~~~~~
        1 error
        """
          .trimIndent()
      )
  }

  @Test
  fun `Test CircuitScreenDataClassDetector - abstract class implements Screen - succeeds`() {
    val testFile =
      kotlin(
          """
            package com.example.screens

            import com.slack.circuit.runtime.screen.Screen

            abstract class BaseScreen : Screen {
                abstract val screenId: String
            }
        """
            .trimIndent()
        )
        .indented()

    lint().files(circuitScreenStub, testFile).run().expectClean()
  }

  @Test
  fun `Test CircuitScreenDataClassDetector - sealed class implements Screen - fails`() {
    val testFile =
      kotlin(
          """
            package com.example.screens

            import com.slack.circuit.runtime.screen.Screen

            sealed class NavigationScreen : Screen {
                data class Home(val userId: String) : NavigationScreen()
                data object Settings : NavigationScreen()
            }
        """
            .trimIndent()
        )
        .indented()

    lint()
      .files(circuitScreenStub, testFile)
      .run()
      .expectClean()
  }

  @Test
  fun `Test CircuitScreenDataClassDetector - inner class implements Screen - succeeds`() {
    val testFile =
      kotlin(
          """
            package com.example.screens

            import com.slack.circuit.runtime.screen.Screen

            class OuterClass {
                class InnerScreen(val id: String) : Screen
            }
        """
            .trimIndent()
        )
        .indented()

    lint()
      .files(circuitScreenStub, testFile)
      .run()
      .expect(
        "src/com/example/screens/OuterClass.kt:6: Error: Circuit Screen implementations should be data classes or data objects, not regular classes. [CircuitScreenShouldBeDataClass]\n" +
          "    class InnerScreen(val id: String) : Screen\n" +
          "    ~~~~~\n" +
          "1 error"
      )
  }

  @Test
  fun `Test CircuitScreenDataClassDetector - class with no constructor parameters - suggests data object`() {
    val testFile =
      kotlin(
          """
            package com.example.screens

            import com.slack.circuit.runtime.screen.Screen

            class EmptyScreen : Screen
        """
            .trimIndent()
        )
        .indented()

    lint()
      .files(circuitScreenStub, testFile)
      .run()
      .expect(
        """
        src/com/example/screens/EmptyScreen.kt:5: Error: ${CircuitScreenDataClassDetector.MESSAGE} [${CircuitScreenDataClassDetector.ISSUE_ID}]
        class EmptyScreen : Screen
        ~~~~~
        1 error
        """
          .trimIndent()
      )
  }

  @Test
  fun `Test CircuitScreenDataClassDetector - class with empty constructor - suggests data object`() {
    val testFile =
      kotlin(
          """
            package com.example.screens

            import com.slack.circuit.runtime.screen.Screen

            class EmptyScreen() : Screen
        """
            .trimIndent()
        )
        .indented()

    lint()
      .files(circuitScreenStub, testFile)
      .run()
      .expect(
        """
        src/com/example/screens/EmptyScreen.kt:5: Error: ${CircuitScreenDataClassDetector.MESSAGE} [${CircuitScreenDataClassDetector.ISSUE_ID}]
        class EmptyScreen() : Screen
        ~~~~~
        1 error
        """
          .trimIndent()
      )
  }

  private val circuitScreenStub =
    kotlin(
        """
            package com.slack.circuit.runtime.screen

            import android.os.Parcelable

            interface Screen : Parcelable
        """
          .trimIndent()
      )
      .indented()
}
