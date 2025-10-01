// Copyright (C) 2021 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint.retrofit

import com.android.tools.lint.checks.infrastructure.TestFile
import com.android.tools.lint.detector.api.Detector
import org.junit.Test
import slack.lint.BaseSlackLintTest

class RetrofitUsageDetectorTest : BaseSlackLintTest() {

  private companion object {
    val allowUnitResult: TestFile =
      kotlin(
          """
        package slack.lint.annotations

        @Target(AnnotationTarget.FUNCTION)
        @Retention(AnnotationRetention.SOURCE)
        annotation class AllowUnitResult
      """
        )
        .indented()
  }

  private val retrofit3Jar = retrofit3Jar()

  override fun getDetector(): Detector = RetrofitUsageDetector()

  override fun getIssues() = listOf(RetrofitUsageDetector.ISSUE)

  @Test
  fun formEncoding() {
    lint()
      .files(
        retrofit3Jar,
        kotlin(
            """
            package test

            import retrofit2.http.Field
            import retrofit2.http.FormUrlEncoded
            import retrofit2.http.GET
            import retrofit2.http.POST

            interface Example {
              @GET("/")
              @FormUrlEncoded
              fun wrongMethod(): String

              @POST("/")
              @FormUrlEncoded
              fun missingFieldParams(): String

              @POST("/")
              fun missingAnnotation(@Field("hi") input: String): String

              @FormUrlEncoded
              @POST("/")
              fun correct(@Field("hi") input: String): String
            }
          """
          )
          .indented(),
      )
      .run()
      .expect(
        """
        src/test/Example.kt:11: Error: @FormUrlEncoded requires @PUT, @POST, or @PATCH. [RetrofitUsage]
          fun wrongMethod(): String
              ~~~~~~~~~~~
        src/test/Example.kt:14: Error: @FormUrlEncoded but has no @Field(Map) parameters. [RetrofitUsage]
          @FormUrlEncoded
          ~~~~~~~~~~~~~~~
        src/test/Example.kt:18: Error: @Field(Map) param requires @FormUrlEncoded. [RetrofitUsage]
          fun missingAnnotation(@Field("hi") input: String): String
              ~~~~~~~~~~~~~~~~~
        3 errors, 0 warnings
        """
          .trimIndent()
      )
      .expectFixDiffs(
        """
        Autofix for src/test/Example.kt line 14: Remove '@FormUrlEncoded':
        @@ -14 +14
        -   @FormUrlEncoded
        Autofix for src/test/Example.kt line 18: Replace with @retrofit2.http.FormUrlEncoded...:
        @@ -17 +17
        -   @POST("/")
        +   @retrofit2.http.FormUrlEncoded
        + @POST("/")
        """
          .trimIndent()
      )
  }

  @Test
  fun bodies() {
    lint()
      .files(
        retrofit3Jar,
        kotlin(
            """
            package test

            import retrofit2.http.Body
            import retrofit2.http.GET
            import retrofit2.http.Multipart
            import retrofit2.http.Part
            import retrofit2.http.POST

            interface Example {
              @GET("/")
              fun wrongMethod(@Body body : String): String

              @POST("/")
              fun missingBody(): String

              @POST("/")
              fun doubleBody(@Body input: String, @Body input2: String): String

              @POST("/")
              fun correct(@Body input: String): String

              @Multipart
              @POST("/")
              fun multipartCorrect(@Part input: String): String

              @Multipart
              @GET("/")
              fun multipartBadMethod(@Part input: String): String

              @Multipart
              @POST("/")
              fun multipartBadParameterType(@Body input: String): String

              @Multipart
              @POST("/")
              fun multipartMissingPartParameter(): String
            }
          """
          )
          .indented(),
      )
      .run()
      .expect(
        """
        src/test/Example.kt:10: Error: @Body param requires @PUT, @POST, or @PATCH. [RetrofitUsage]
          @GET("/")
          ~~~~~~~~~
        src/test/Example.kt:13: Error: This annotation requires an @Body parameter. [RetrofitUsage]
          @POST("/")
          ~~~~~~~~~~
        src/test/Example.kt:17: Error: Duplicate @Body param!. [RetrofitUsage]
          fun doubleBody(@Body input: String, @Body input2: String): String
                                              ~~~~~~~~~~~~~~~~~~~~
        src/test/Example.kt:28: Error: @Multipart requires @PUT, @POST, or @PATCH. [RetrofitUsage]
          fun multipartBadMethod(@Part input: String): String
              ~~~~~~~~~~~~~~~~~~
        src/test/Example.kt:31: Error: @Multipart methods should only contain @Part parameters. [RetrofitUsage]
          @POST("/")
          ~~~~~~~~~~
        src/test/Example.kt:35: Error: @Multipart methods should contain at least one @Part parameter. [RetrofitUsage]
          @POST("/")
          ~~~~~~~~~~
        6 errors, 0 warnings
        """
          .trimIndent()
      )
  }

  @Test
  fun unitReturn() {
    lint()
      .files(
        retrofit3Jar,
        allowUnitResult,
        kotlin(
            """
            package test

            import retrofit2.http.GET
            import slack.lint.annotations.AllowUnitResult

            interface Example {
              @GET("/")
              fun unitMethod()

              @GET("/")
              suspend fun suspendUnitMethod()

              @GET("/")
              fun unitMethodExplicit(): Unit

              suspend fun suspendUnitMethodExplicit(): Unit

              @AllowUnitResult
              @PUT("/")
              suspend fun suspendUnitMethodAllowUnitResult()

              @AllowUnitResult
              @DELETE("/")
              suspend fun suspendUnitMethodExplicitAllowUnitResult(): Unit
            }
          """
          )
          .indented(),
      )
      .run()
      .expect(
        """
        src/test/Example.kt:8: Error: Retrofit endpoints should return something other than Unit/void. [RetrofitUsage]
          fun unitMethod()
              ~~~~~~~~~~
        src/test/Example.kt:11: Error: Retrofit endpoints should return something other than Unit/void. [RetrofitUsage]
          suspend fun suspendUnitMethod()
                      ~~~~~~~~~~~~~~~~~
        src/test/Example.kt:14: Error: Retrofit endpoints should return something other than Unit/void. [RetrofitUsage]
          fun unitMethodExplicit(): Unit
              ~~~~~~~~~~~~~~~~~~
        3 errors
        """
          .trimIndent()
      )
  }
}
