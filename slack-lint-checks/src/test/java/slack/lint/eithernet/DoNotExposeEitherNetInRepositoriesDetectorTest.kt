// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint.eithernet

import com.android.tools.lint.checks.infrastructure.LintDetectorTest.kotlin
import com.android.tools.lint.checks.infrastructure.TestMode
import org.junit.Test
import slack.lint.BaseSlackLintTest

private val API_RESULT =
  kotlin("""
  package com.slack.eithernet

  interface ApiResult<out T : Any, out E : Any>
""")
    .indented()

class DoNotExposeEitherNetInRepositoriesDetectorTest : BaseSlackLintTest() {
  override fun getDetector() = DoNotExposeEitherNetInRepositoriesDetector()

  override fun getIssues() = listOf(DoNotExposeEitherNetInRepositoriesDetector.ISSUE)

  // TODO fix these
  override val skipTestModes: Array<TestMode> = arrayOf(TestMode.SUPPRESSIBLE)

  @Test
  fun javaTests() {
    lint()
      .files(
        API_RESULT,
        java(
            """
        package test;

        import com.slack.eithernet.ApiResult;

        interface MyRepository {
          // Bad

          ApiResult<String, Exception> getResult();

          // Good

          String getString();
        }
      """
          )
          .indented(),

        // Non-interface version
        java(
            """
        package test;

        import com.slack.eithernet.ApiResult;

        abstract class MyClassRepository {
          // Bad

          public abstract ApiResult<String, Exception> getResultPublic();
          public ApiResult<String, Exception> resultField = null;

          // Good

          ApiResult<String, Exception> resultFieldPackagePrivate = null;
          private final ApiResult<String, Exception> resultFieldPrivate = null;
          protected ApiResult<String, Exception> resultFieldProtected = null;
          abstract ApiResult<String, Exception> getResultPackagePrivate();
          private ApiResult<String, Exception> getResultPrivate();
          private ApiResult<String, Exception> getResultProtected();
          public abstract String getString();
        }
      """
          )
          .indented(),
      )
      .run()
      .expect(
        """
        src/test/MyClassRepository.java:8: Error: Repository APIs should not expose EitherNet types directly. [DoNotExposeEitherNetInRepositories]
          public abstract ApiResult<String, Exception> getResultPublic();
                          ~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        src/test/MyClassRepository.java:9: Error: Repository APIs should not expose EitherNet types directly. [DoNotExposeEitherNetInRepositories]
          public ApiResult<String, Exception> resultField = null;
                 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        src/test/MyRepository.java:8: Error: Repository APIs should not expose EitherNet types directly. [DoNotExposeEitherNetInRepositories]
          ApiResult<String, Exception> getResult();
          ~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        3 errors, 0 warnings
        """
          .trimIndent()
      )
  }

  @Test
  fun kotlinTests() {
    lint()
      .files(
        API_RESULT,
        kotlin(
            """
        package test

        import com.slack.eithernet.ApiResult

        interface MyRepository {
          // Bad

          fun getResult(): ApiResult<String, Exception>
          suspend fun getResultSuspended(): ApiResult<String, Exception>
          val resultVal: ApiResult<String, Exception>

          // Good

          fun getString(): String
          suspend fun getStringSuspended(): String
          val stringValue: String
        }
      """
          )
          .indented(),

        // Non-interface version
        kotlin(
            """
        package test

        import com.slack.eithernet.ApiResult

        abstract class MyClassRepository {
          // Bad

          abstract fun getResultPublic(): ApiResult<String, Exception>
          fun typeLessFunction() = getResultPublic()
          val resultProperty: ApiResult<String, Exception>? = null
          val typeLessProperty get() = resultProperty

          // Good

          internal val resultPropertyInternal: ApiResult<String, Exception>? = null
          private val resultPropertyPrivate: ApiResult<String, Exception>? = null
          protected val resultPropertyProtected: ApiResult<String, Exception>? = null
          internal abstract fun getResultInternal(): ApiResult<String, Exception>
          private fun getResultPrivate(): ApiResult<String, Exception>
          private fun getResultProtected(): ApiResult<String, Exception>
          abstract fun getString(): String
        }
      """
          )
          .indented(),
      )
      .run()
      .expect(
        """
        src/test/MyClassRepository.kt:8: Error: Repository APIs should not expose EitherNet types directly. [DoNotExposeEitherNetInRepositories]
          abstract fun getResultPublic(): ApiResult<String, Exception>
                                          ~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        src/test/MyClassRepository.kt:9: Error: Repository APIs should not expose EitherNet types directly. [DoNotExposeEitherNetInRepositories]
          fun typeLessFunction() = getResultPublic()
              ~~~~~~~~~~~~~~~~
        src/test/MyClassRepository.kt:10: Error: Repository APIs should not expose EitherNet types directly. [DoNotExposeEitherNetInRepositories]
          val resultProperty: ApiResult<String, Exception>? = null
                              ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        src/test/MyClassRepository.kt:11: Error: Repository APIs should not expose EitherNet types directly. [DoNotExposeEitherNetInRepositories]
          val typeLessProperty get() = resultProperty
              ~~~~~~~~~~~~~~~~
        src/test/MyRepository.kt:8: Error: Repository APIs should not expose EitherNet types directly. [DoNotExposeEitherNetInRepositories]
          fun getResult(): ApiResult<String, Exception>
                           ~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        src/test/MyRepository.kt:9: Error: Repository APIs should not expose EitherNet types directly. [DoNotExposeEitherNetInRepositories]
          suspend fun getResultSuspended(): ApiResult<String, Exception>
                                            ~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        6 errors, 0 warnings
        """
          .trimIndent()
      )
  }

  @Test
  fun regressionTest() {
    lint()
      .files(
        API_RESULT,
        kotlin(
            """
        package test

        import com.slack.eithernet.ApiResult

        interface StuffRepository {

          suspend fun fetchStuff(): ApiResult<String, String>

          suspend fun setStuff(
            discoverability: String
          ): ApiResult<Unit, String>
        }
      """
          )
          .indented(),
      )
      .run()
      .expect(
        """
          src/test/StuffRepository.kt:7: Error: Repository APIs should not expose EitherNet types directly. [DoNotExposeEitherNetInRepositories]
            suspend fun fetchStuff(): ApiResult<String, String>
                                      ~~~~~~~~~~~~~~~~~~~~~~~~~
          src/test/StuffRepository.kt:11: Error: Repository APIs should not expose EitherNet types directly. [DoNotExposeEitherNetInRepositories]
            ): ApiResult<Unit, String>
               ~~~~~~~~~~~~~~~~~~~~~~~
          2 errors, 0 warnings
        """
          .trimIndent()
      )
  }
}
