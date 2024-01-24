// Copyright (C) 2020 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint

import org.junit.Test

class InjectWithUsageDetectorTest : BaseSlackLintTest() {

  companion object {
    private val FOUNDATION_DI_STUBS =
      kotlin(
        """
      package slack.foundation.auth

      interface LoggedInUserProvider
      """
          .trimIndent()
      )
    private val DI_STUBS =
      kotlin(
        """
      package slack.di

      abstract class AppScope private constructor()
      abstract class UserScope private constructor()
      abstract class OrgScope private constructor()
      """
          .trimIndent()
      )
    private val INJECTION_STUBS =
      kotlin(
        """
      package slack.anvil.injection

      annotation class InjectWith(val scope: KClass<*>)

      // Not a complete match to the real impl but close enough for testing
      interface AnvilInjectable<MapKeyType : Annotation, InjectableType : Any>
      """
          .trimIndent()
      )
  }

  override fun getDetector() = InjectWithUsageDetector()

  override fun getIssues() = InjectWithUsageDetector.ISSUES.toList()

  @Test
  fun smokeTest() {
    lint()
      .files(
        FOUNDATION_DI_STUBS,
        DI_STUBS,
        INJECTION_STUBS,
        kotlin(
            """
            package test.pkg

            import slack.anvil.injection.InjectWith
            import slack.anvil.injection.AnvilInjectable
            import slack.di.AppScope
            import slack.di.UserScope
            import slack.di.OrgScope
            import slack.foundation.auth.LoggedInUserProvider

            annotation class BaseInjectableAnnotation

            abstract class BaseInjectable : AnvilInjectable<BaseInjectableAnnotation, BaseInjectable>

            @InjectWith(AppScope::class)
            class AppActivityCorrect : BaseInjectable()

            @InjectWith(UserScope::class)
            class UserActivityCorrect : BaseInjectable(), LoggedInUserProvider

            @InjectWith(OrgScope::class)
            class OrgActivityCorrect : BaseInjectable(), LoggedInUserProvider

            @InjectWith(UserScope::class)
            class UserActivityWrong : BaseInjectable()

            @InjectWith(OrgScope::class)
            class OrgActivityWrong : BaseInjectable()

            @InjectWith(AppScope::class)
            class MissingAnvilInjectable
          """
          )
          .indented(),
      )
      .allowCompilationErrors(false)
      .run()
      .expect(
        """
          src/test/pkg/BaseInjectableAnnotation.kt:24: Error: @InjectWith-annotated classes must implement LoggedInUserProvider (or extend something that does) if they target UserScope or OrgScope. [InjectWithScopeRequiredLoggedInUserProvider]
          class UserActivityWrong : BaseInjectable()
                ~~~~~~~~~~~~~~~~~
          src/test/pkg/BaseInjectableAnnotation.kt:27: Error: @InjectWith-annotated classes must implement LoggedInUserProvider (or extend something that does) if they target UserScope or OrgScope. [InjectWithScopeRequiredLoggedInUserProvider]
          class OrgActivityWrong : BaseInjectable()
                ~~~~~~~~~~~~~~~~
          src/test/pkg/BaseInjectableAnnotation.kt:30: Error: @InjectWith-annotated classes must implement AnvilInjectable (or extend something that does). [InjectWithTypeMustImplementAnvilInjectable]
          class MissingAnvilInjectable
                ~~~~~~~~~~~~~~~~~~~~~~
          3 errors, 0 warnings
        """
          .trimIndent()
      )
  }
}
