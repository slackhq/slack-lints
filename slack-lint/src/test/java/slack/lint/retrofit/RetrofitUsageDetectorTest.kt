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
package slack.lint.retrofit

import com.android.tools.lint.detector.api.Detector
import org.junit.Test
import slack.lint.BaseSlackLintTest

class RetrofitUsageDetectorTest : BaseSlackLintTest() {

  private val retrofit2Jar = retrofit2Jar()

  override fun getDetector(): Detector = RetrofitUsageDetector()

  override fun getIssues() = listOf(RetrofitUsageDetector.ISSUE)

  @Test
  fun formEncoding() {
    lint()
      .files(
        retrofit2Jar,
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
        ).indented()
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
        """.trimIndent()
      )
      .expectFixDiffs(
        """
        Fix for src/test/Example.kt line 14: Remove '@FormUrlEncoded':
        @@ -14 +14
        -   @FormUrlEncoded
        +
        Fix for src/test/Example.kt line 18: Replace with @retrofit2.http.FormUrlEncoded...:
        @@ -17 +17
        -   @POST("/")
        +   @retrofit2.http.FormUrlEncoded
        + @POST("/")
        """.trimIndent()
      )
  }

  @Test
  fun bodies() {
    lint()
      .files(
        retrofit2Jar,
        kotlin(
          """
            package test

            import retrofit2.http.Body
            import retrofit2.http.GET
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
            }
          """
        ).indented()
      )
      .run()
      .expect(
        """
        src/test/Example.kt:8: Error: @Body param requires @PUT, @POST, or @PATCH. [RetrofitUsage]
          @GET("/")
          ~~~~~~~~~
        src/test/Example.kt:11: Error: This annotation requires an @Body parameter. [RetrofitUsage]
          @POST("/")
          ~~~~~~~~~~
        src/test/Example.kt:15: Error: Duplicate @Body param!. [RetrofitUsage]
          fun doubleBody(@Body input: String, @Body input2: String): String
                                              ~~~~~~~~~~~~~~~~~~~~
        3 errors, 0 warnings
        """.trimIndent()
      )
  }

  @Test
  fun unitReturn() {
    lint()
      .files(
        retrofit2Jar,
        kotlin(
          """
            package test

            import retrofit2.http.GET

            interface Example {
              @GET("/")
              fun unitMethod()

              @GET("/")
              fun unitMethodExplicit(): Unit
            }
          """
        ).indented()
      )
      .run()
      .expect(
        """
        src/test/Example.kt:7: Error: Retrofit endpoints should return something other than Unit/void. [RetrofitUsage]
          fun unitMethod()
              ~~~~~~~~~~
        src/test/Example.kt:10: Error: Retrofit endpoints should return something other than Unit/void. [RetrofitUsage]
          fun unitMethodExplicit(): Unit
              ~~~~~~~~~~~~~~~~~~
        2 errors, 0 warnings
        """.trimIndent()
      )
  }
}
