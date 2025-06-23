// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint

import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class JsonInflaterMoshiCompatibilityDetectorTest : LintDetectorTest() {

  override fun getDetector(): Detector {
    return JsonInflaterMoshiCompatibilityDetector()
  }

  override fun getIssues(): List<Issue> {
    return JsonInflaterMoshiCompatibilityDetector.issues()
  }

  // Stubs for required annotations and classes
  private val jsonClassStub =
    java(
      """
    package com.squareup.moshi;

    import java.lang.annotation.Retention;
    import java.lang.annotation.RetentionPolicy;

    @Retention(RetentionPolicy.RUNTIME)
    public @interface JsonClass {
        boolean generateAdapter();
    }
  """
    )

  private val jsonStub =
    java(
      """
    package com.squareup.moshi;

    import java.lang.annotation.Retention;
    import java.lang.annotation.RetentionPolicy;

    @Retention(RetentionPolicy.RUNTIME)
    public @interface Json {
        String name();
    }
  """
    )

  private val adaptedByStub =
    kotlin(
      """
    package dev.zacsweers.moshix.adapters

    import java.lang.annotation.Retention
    import java.lang.annotation.RetentionPolicy

    @Retention(RetentionPolicy.RUNTIME)
    public annotation class AdaptedBy(val adapter: KClass<*>, val nullSafe: Boolean = true)
  """
    )

  private val jsonInflaterStub =
    kotlin(
      """
    package slack.commons.json

    import java.lang.reflect.Type

    interface JsonInflater {
      fun <T : Any> inflate(jsonData: String, typeOfT: Type): T

      fun <T : Any> inflate(jsonData: String, clazz: Class<T>): T

      fun <T : Any> deflate(value: T, clazz: Class<T>): String

      fun deflate(value: Any, type: Type): String
    }
  """
    )

  private val parameterizedTypeStub =
    kotlin(
      """
            package com.squareup.moshi

            import java.lang.reflect.ParameterizedType
            import java.lang.reflect.Type

            class StubParameterizedType(
                private val rawType: Type,
                private val typeArguments: Array<Type>,
                private val ownerType: Type? = null
            ) : ParameterizedType {
                override fun getActualTypeArguments(): Array<Type> = typeArguments
                override fun getRawType(): Type = rawType
                override fun getOwnerType(): Type? = ownerType
            }
        """
        .trimIndent()
    )

  @Test
  fun testDocumentationExample() {
    testMissingJsonClassAnnotation()
  }

  @Test
  fun testDataClassJsonClassTrue() {
    lint()
      .files(
        jsonClassStub,
        jsonStub,
        jsonInflaterStub,
        kotlin(
          """
        package test

        import com.squareup.moshi.JsonClass
        import slack.commons.json.JsonInflater

        @JsonClass(generateAdapter = true)
        data class ValidModel(
            val id: String,
            val name: String,
            val count: Int
        )

        fun useJsonInflater(jsonInflater: JsonInflater) {
            val model = jsonInflater.inflate("{}", ValidModel::class.java)
            val json = jsonInflater.deflate(model, ValidModel::class.java)
        }
      """
        ),
      )
      .run()
      .expectClean()
  }

  @Test
  fun testDataClassJsonClassFalse() {
    lint()
      .files(
        jsonClassStub,
        jsonStub,
        jsonInflaterStub,
        kotlin(
          """
        package test

        import com.squareup.moshi.JsonClass
        import slack.commons.json.JsonInflater

        @JsonClass(generateAdapter = false)
        data class ValidModel(
            val id: String,
            val name: String,
            val count: Int
        )

        fun useJsonInflater(jsonInflater: JsonInflater) {
            val model = jsonInflater.inflate("{}", ValidModel::class.java)
            val json = jsonInflater.deflate(model)
        }
      """
        ),
      )
      .run()
      // We only check for the existence of @JsonClass in the detector as we get more granular in
      // MoshiUsageDetector.
      .expectClean()
  }

  @Test
  fun testDataClassJsonClassEmpty() {
    lint()
      .files(
        jsonClassStub,
        jsonStub,
        jsonInflaterStub,
        kotlin(
          """
        package test

        import com.squareup.moshi.JsonClass
        import slack.commons.json.JsonInflater

        @JsonClass()
        data class ValidModel(
            val id: String,
            val name: String,
            val count: Int
        )

        fun useJsonInflater(jsonInflater: JsonInflater) {
            val model = jsonInflater.inflate("{}", ValidModel::class.java)
            val json = jsonInflater.deflate(model, ValidModel::class.java)
        }
      """
        ),
      )
      .run()
      .expectClean()
  }

  @Test
  fun testDataClassAdaptedBy() {
    lint()
      .files(
        jsonClassStub,
        jsonStub,
        jsonInflaterStub,
        adaptedByStub,
        kotlin(
          """
        package test

        import dev.zacsweers.moshix.adapters.AdaptedBy
        import slack.commons.json.JsonInflater

        @AdaptedBy(String::class)
        data class ValidModel(
            val id: String,
            val name: String,
            val count: Int
        )

        fun useJsonInflater(jsonInflater: JsonInflater) {
            val model = jsonInflater.inflate("{}", ValidModel::class.java)
            val json = jsonInflater.deflate(model, ValidModel::class.java)
        }
      """
        ),
      )
      .run()
      .expectClean()
  }

  @Test
  fun testValidDataClassInList() {
    lint()
      .files(
        jsonClassStub,
        jsonStub,
        jsonInflaterStub,
        adaptedByStub,
        parameterizedTypeStub,
        kotlin(
          """
        package test

        import com.squareup.moshi.JsonClass
        import com.squareup.moshi.StubParameterizedType
        import slack.commons.json.JsonInflater

        @JsonClass(generateAdapter = true)
        data class ValidModel(
            val id: String,
            val name: String,
            val count: Int
        )

        fun useJsonInflater(jsonInflater: JsonInflater) {
            val type = StubParameterizedType(
                List::class.java,
                arrayOf(ValidModel::class.java)
            )
            val model = jsonInflater.inflate<List<ValidModel>>("{}", type)
            val json = jsonInflater.deflate(model, type)
        }
      """
        ),
      )
      .run()
      .expectClean()
  }

  @Test
  fun testInvalidDataClassInList() {
    lint()
      .files(
        jsonClassStub,
        jsonStub,
        jsonInflaterStub,
        adaptedByStub,
        parameterizedTypeStub,
        kotlin(
          """
        package test

        import com.squareup.moshi.JsonClass
        import com.squareup.moshi.StubParameterizedType
        import slack.commons.json.JsonInflater

        data class InvalidModel(
            val id: String,
            val name: String,
            val count: Int
        )

        fun useJsonInflater(jsonInflater: JsonInflater) {
            val type = StubParameterizedType(
                List::class.java,
                arrayOf(InvalidModel::class.java)
            )
            val model = jsonInflater.inflate<List<InvalidModel>>("{}", type)
            val json = jsonInflater.deflate(model, type)
        }
      """
        ),
      )
      .run()
      .expect(
        """
                src/test/InvalidModel.kt:19: Error: Using JsonInflater.inflate/deflate with a Moshi-incompatible type. [JsonInflaterMoshiIncompatibleType]
                            val model = jsonInflater.inflate<List<InvalidModel>>("{}", type)
                                        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                src/test/InvalidModel.kt:20: Error: Using JsonInflater.inflate/deflate with a Moshi-incompatible type. [JsonInflaterMoshiIncompatibleType]
                            val json = jsonInflater.deflate(model, type)
                                       ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                2 errors
              """
      )
  }

  @Test
  fun testMissingJsonClassAnnotation() {
    lint()
      .files(
        jsonClassStub,
        jsonStub,
        jsonInflaterStub,
        kotlin(
          """
        package test

        import slack.commons.json.JsonInflater

        data class InvalidModel(
            val id: String,
            val name: String,
            val count: Int
        )

        fun useJsonInflater(jsonInflater: JsonInflater) {
            val model = jsonInflater.inflate("{}", InvalidModel::class.java)
            val json = jsonInflater.deflate(model, InvalidModel::class.java)
        }
      """
        ),
      )
      .run()
      .expect(
        """
        src/test/InvalidModel.kt:13: Error: Using JsonInflater.inflate/deflate with a Moshi-incompatible type. [JsonInflaterMoshiIncompatibleType]
                    val model = jsonInflater.inflate("{}", InvalidModel::class.java)
                                ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        src/test/InvalidModel.kt:14: Error: Using JsonInflater.inflate/deflate with a Moshi-incompatible type. [JsonInflaterMoshiIncompatibleType]
                    val json = jsonInflater.deflate(model, InvalidModel::class.java)
                               ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        2 errors
      """
      )
  }

  @Test
  fun testNonDataClass() {
    lint()
      .files(
        jsonClassStub,
        jsonStub,
        jsonInflaterStub,
        kotlin(
          """
        package test

        import com.squareup.moshi.JsonClass
        import slack.commons.json.JsonInflater

        @JsonClass(generateAdapter = true)
        class InvalidModel(
            val id: String,
            val name: String,
            val count: Int
        )

        fun useJsonInflater(jsonInflater: JsonInflater) {
            val model = jsonInflater.inflate("{}", InvalidModel::class.java)
            val json = jsonInflater.deflate(model, InvalidModel::class.java)
        }
      """
        ),
      )
      .run()
      .expectClean()
  }

  @Test
  fun testAbstractClass() {
    lint()
      .files(
        jsonClassStub,
        jsonStub,
        jsonInflaterStub,
        kotlin(
          """
        package test

        import com.squareup.moshi.JsonClass
        import slack.commons.json.JsonInflater

        @JsonClass(generateAdapter = true)
        abstract class InvalidModel(
            val id: String,
            val name: String,
            val count: Int
        )

        fun useJsonInflater(jsonInflater: JsonInflater) {
            val model = jsonInflater.inflate("{}", InvalidModel::class.java)
            val json = jsonInflater.deflate(model, InvalidModel::class.java)
        }
      """
        ),
      )
      .run()
      .expect(
        """
        src/test/InvalidModel.kt:15: Error: Using JsonInflater.inflate/deflate with a Moshi-incompatible type. [JsonInflaterMoshiIncompatibleType]
                    val model = jsonInflater.inflate("{}", InvalidModel::class.java)
                                ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        src/test/InvalidModel.kt:16: Error: Using JsonInflater.inflate/deflate with a Moshi-incompatible type. [JsonInflaterMoshiIncompatibleType]
                    val json = jsonInflater.deflate(model, InvalidModel::class.java)
                               ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        2 errors
      """
      )
  }
}
