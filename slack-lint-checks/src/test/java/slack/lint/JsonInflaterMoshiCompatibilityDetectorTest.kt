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
  private val jsonClassStub = java("""
    package com.squareup.moshi;
    
    import java.lang.annotation.Retention;
    import java.lang.annotation.RetentionPolicy;
    
    @Retention(RetentionPolicy.RUNTIME)
    public @interface JsonClass {
        boolean generateAdapter();
    }
  """
  )

  private val jsonStub = java("""
    package com.squareup.moshi;
    
    import java.lang.annotation.Retention;
    import java.lang.annotation.RetentionPolicy;
    
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Json {
        String name();
    }
  """
  )

  private val adaptedByStub = kotlin("""
    package dev.zacsweers.moshix.adapters
    
    import java.lang.annotation.Retention
    import java.lang.annotation.RetentionPolicy
    
    @Retention(RetentionPolicy.RUNTIME)
    public annotation class AdaptedBy(val adapter: KClass<*>, val nullSafe: Boolean = true)
  """
  )

  private val jsonInflaterStub = kotlin("""
    package slack.commons.json

    interface JsonInflater {
      fun <T : Any> inflate(jsonData: String, clazz: Class<T>): T
    
      fun <T : Any> deflate(value: T, clazz: Class<T>): String
    
      fun deflate(value: Any, type: Type): String
    }
  """
  )


  @Test
  fun testDataClassJsonClassTrue() {
    lint().files(
      jsonClassStub,
      jsonStub,
      jsonInflaterStub,
      kotlin("""
        package test
        
        import com.squareup.moshi.JsonClass
        
        @JsonClass(generateAdapter = true)
        data class ValidModel(
            val id: String,
            val name: String,
            val count: Int
        )
        
        fun useJsonInflater(jsonInflater: slack.commons.json.JsonInflater) {
            val model = jsonInflater.inflate("{}", ValidModel::class.java)
            val json = jsonInflater.deflate(model)
        }
      """
      )
    )
      .run()
      .expectClean()
  }

  @Test
  fun testDataClassAdaptedBy() {
    lint().files(
      jsonClassStub,
      jsonStub,
      jsonInflaterStub,
      adaptedByStub,
      kotlin("""
        package test
        
        import dev.zacsweers.moshix.adapters.AdaptedBy
        
        @AdaptedBy(String::class)
        data class ValidModel(
            val id: String,
            val name: String,
            val count: Int
        )
        
        fun useJsonInflater(jsonInflater: slack.commons.json.JsonInflater) {
            val model = jsonInflater.inflate("{}", ValidModel::class.java)
            val json = jsonInflater.deflate(model)
        }
      """
      )
    )
      .run()
      .expectClean()
  }

  @Test
  fun testDataClassJsonClassFalse() {
    lint().files(
      jsonClassStub,
      jsonStub,
      jsonInflaterStub,
      kotlin("""
        package test
        
        import com.squareup.moshi.JsonClass
        
        @JsonClass(generateAdapter = false)
        data class ValidModel(
            val id: String,
            val name: String,
            val count: Int
        )
        
        fun useJsonInflater(jsonInflater: slack.commons.json.JsonInflater) {
            val model = jsonInflater.inflate("{}", ValidModel::class.java)
            val json = jsonInflater.deflate(model)
        }
      """
      )
    )
      .run()
      .expect("""
        src/test/ValidModel.kt:14: Error: Using JsonInflater.inflate/deflate with a Moshi-incompatible type. [JsonInflaterMoshiCompatibility:MoshiIncompatibleType]
                    val model = jsonInflater.inflate("{}", ValidModel::class.java)
                                ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        src/test/ValidModel.kt:15: Error: Using JsonInflater.inflate/deflate with a Moshi-incompatible type. [JsonInflaterMoshiCompatibility:MoshiIncompatibleType]
                    val json = jsonInflater.deflate(model)
                               ~~~~~~~~~~~~~~~~~~~~~~~~~~~
        2 errors
      """
      )
  }

  @Test
  fun testMissingJsonClassAnnotation() {
    lint().files(
      jsonClassStub,
      jsonStub,
      jsonInflaterStub,
      kotlin("""
        package test
        
        data class InvalidModel(
            val id: String,
            val name: String,
            val count: Int
        )
        
        fun useJsonInflater(jsonInflater: slack.commons.json.JsonInflater) {
            val model = jsonInflater.inflate("{}", InvalidModel::class.java)
            val json = jsonInflater.deflate(model)
        }
      """)
    ).run()
      .expect("""
        src/test/InvalidModel.kt:11: Error: Using JsonInflater.inflate/deflate with a Moshi-incompatible type. [JsonInflaterMoshiCompatibility:MoshiIncompatibleType]
                    val model = jsonInflater.inflate("{}", InvalidModel::class.java)
                                ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        src/test/InvalidModel.kt:12: Error: Using JsonInflater.inflate/deflate with a Moshi-incompatible type. [JsonInflaterMoshiCompatibility:MoshiIncompatibleType]
                    val json = jsonInflater.deflate(model)
                               ~~~~~~~~~~~~~~~~~~~~~~~~~~~
        2 errors
      """
      )
  }

  @Test
  fun testNonDataClass() {
    lint().files(
      jsonClassStub,
      jsonStub,
      jsonInflaterStub,
      kotlin("""
        package test

        import com.squareup.moshi.JsonClass

        @JsonClass(generateAdapter = true)
        class InvalidModel(
            val id: String,
            val name: String,
            val count: Int
        )

        fun useJsonInflater(jsonInflater: slack.commons.json.JsonInflater) {
            val model = jsonInflater.inflate("{}", InvalidModel::class.java)
            val json = jsonInflater.deflate(model)
        }
      """)
    )
      .run()
      .expectClean()
  }
  @Test
  fun testAbstractClass() {
    lint().files(
      jsonClassStub,
      jsonStub,
      jsonInflaterStub,
      kotlin("""
        package test

        import com.squareup.moshi.JsonClass

        @JsonClass(generateAdapter = true)
        abstract class InvalidModel(
            val id: String,
            val name: String,
            val count: Int
        )

        fun useJsonInflater(jsonInflater: slack.commons.json.JsonInflater) {
            val model = jsonInflater.inflate("{}", InvalidModel::class.java)
            val json = jsonInflater.deflate(model)
        }
      """)
    )
      .run()
      .expect("""
        src/test/InvalidModel.kt:14: Error: Using JsonInflater.inflate/deflate with a Moshi-incompatible type. [JsonInflaterMoshiCompatibility:MoshiIncompatibleType]
                    val model = jsonInflater.inflate("{}", InvalidModel::class.java)
                                ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        src/test/InvalidModel.kt:15: Error: Using JsonInflater.inflate/deflate with a Moshi-incompatible type. [JsonInflaterMoshiCompatibility:MoshiIncompatibleType]
                    val json = jsonInflater.deflate(model)
                               ~~~~~~~~~~~~~~~~~~~~~~~~~~~
        2 errors
      """
      )
  }
}