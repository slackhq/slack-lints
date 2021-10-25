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
package slack.lint

import org.junit.Test

class MoshiUsageDetectorTest : BaseSlackLintTest() {

  private val keepAnnotation = java(
    """
        package androidx.annotation;

        public @interface Keep {
        }
      """
  ).indented()

  private val jsonClassAnnotation = java(
    """
        package com.squareup.moshi;

        public @interface JsonClass {
          boolean generateAdapter();
          String generator() default "";
        }
      """
  ).indented()

  private val jsonAnnotation = java(
    """
      package com.squareup.moshi;

      public @interface Json {
        String name();
      }
    """
  ).indented()

  private val jsonQualifierAnnotation = java(
    """
      package com.squareup.moshi;

      public @interface JsonQualifier {
      }
    """
  ).indented()

  private val typeLabel = kotlin(
    """
      package dev.zacsweers.moshix.sealed.annotations

      annotation class TypeLabel(val label: String, val alternateLabels: Array<String> = [])
    """
  ).indented()

  private val defaultObject = kotlin(
    """
      package dev.zacsweers.moshix.sealed.annotations

      annotation class DefaultObject
    """
  ).indented()

  private val adaptedBy = kotlin(
    """
      package dev.zacsweers.moshix.adapters

      import kotlin.reflect.KClass

      annotation class AdaptedBy(
        val adapter: KClass<*>,
        val nullSafe: Boolean = true
      )
    """
  ).indented()

  private val jsonAdapter = java(
    """
      package com.squareup.moshi;

      public class JsonAdapter<T> {
        public interface Factory {

        }
      }
    """
  ).indented()

  override fun getDetector() = MoshiUsageDetector()
  override fun getIssues() = MoshiUsageDetector.issues().toList()

  @Test
  fun simpleCorrect() {
    val source = kotlin(
      """
          package slack.model

          import com.squareup.moshi.JsonClass
          import com.squareup.moshi.Json

          @JsonClass(generateAdapter = true)
          data class TestClass(@Json(name = "bar") val foo: String)
        """
    ).indented()

    lint()
      .files(*testFiles(), source)
      .run()
      .expectClean()
  }

  @Test
  fun sealed_correct() {
    val source = kotlin(
      """
          package slack.model

          import com.squareup.moshi.JsonClass
          import dev.zacsweers.moshix.sealed.annotations.TypeLabel
          import dev.zacsweers.moshix.sealed.annotations.DefaultObject

          @JsonClass(generateAdapter = true, generator = "sealed:type")
          sealed class BaseType {
            @TypeLabel(label = "nested")
            @JsonClass(generateAdapter = true)
            data class Nested(val foo: String) : BaseType()
          }

          @TypeLabel(label = "one")
          @JsonClass(generateAdapter = true)
          data class Subtype(val foo: String) : BaseType()

          @TypeLabel(label = "two")
          object ObjectSubType : BaseType()

          @DefaultObject
          object Default : BaseType()

          // Cover for making sure listing interfaces before superclasses don't affect
          // superclass lookups
          @TypeLabel(label = "three")
          @JsonClass(generateAdapter = true)
          data class SubtypeWithInterface(val foo: String) : ARandomInterface, BaseType()

          interface ARandomInterface
        """
    ).indented()

    lint()
      .files(*testFiles(), source)
      .run()
      .expectClean()
  }

  @Test
  fun sealed_generic() {
    val source = kotlin(
      """
          package slack.model

          import com.squareup.moshi.JsonClass
          import dev.zacsweers.moshix.sealed.annotations.TypeLabel

          @JsonClass(generateAdapter = true, generator = "sealed:type")
          sealed class BaseType<T>

          @TypeLabel(label = "one")
          @JsonClass(generateAdapter = true)
          data class Subtype<T>(val foo: T) : BaseType<T>()

          // This is "ok" generics use because the subtype itself has none
          @TypeLabel(label = "two")
          @JsonClass(generateAdapter = true)
          data class SubtypeTwo(val foo: String) : BaseType<String>()
        """
    ).indented()

    lint()
      .files(*testFiles(), source)
      .run()
      .expect(
        """
        src/slack/model/BaseType.kt:11: Error: Sealed subtypes used with moshi-sealed cannot be generic. [MoshiUsageGenericSealedSubtype]
        data class Subtype<T>(val foo: T) : BaseType<T>()
                          ~~~
        1 errors, 0 warnings
        """.trimIndent()
      )
  }

  @Test
  fun sealed_missing_base_type() {
    val source = kotlin(
      """
          package slack.model

          import com.squareup.moshi.JsonClass
          import dev.zacsweers.moshix.sealed.annotations.TypeLabel
          import dev.zacsweers.moshix.sealed.annotations.DefaultObject

          @TypeLabel(label = "one")
          @JsonClass(generateAdapter = true)
          data class Subtype(val foo: String)

          @TypeLabel(label = "two")
          object ObjectSubType

          @DefaultObject
          object Default
        """
    ).indented()

    lint()
      .files(*testFiles(), source)
      .run()
      .expect(
        """
        src/slack/model/Subtype.kt:7: Error: Inappropriate @TypeLabel or @DefaultObject annotation. [MoshiUsageInappropriateTypeLabel]
        @TypeLabel(label = "one")
        ~~~~~~~~~~~~~~~~~~~~~~~~~
        src/slack/model/Subtype.kt:11: Error: Inappropriate @TypeLabel or @DefaultObject annotation. [MoshiUsageInappropriateTypeLabel]
        @TypeLabel(label = "two")
        ~~~~~~~~~~~~~~~~~~~~~~~~~
        src/slack/model/Subtype.kt:14: Error: Inappropriate @TypeLabel or @DefaultObject annotation. [MoshiUsageInappropriateTypeLabel]
        @DefaultObject
        ~~~~~~~~~~~~~~
        3 errors, 0 warnings
        """.trimIndent()
      )
      .expectFixDiffs(
        """
        Fix for src/slack/model/Subtype.kt line 7: Remove '@TypeLabel(label = "one")':
        @@ -7 +7
        - @TypeLabel(label = "one")
        +
        Fix for src/slack/model/Subtype.kt line 11: Remove '@TypeLabel(label = "two")':
        @@ -11 +11
        - @TypeLabel(label = "two")
        +
        Fix for src/slack/model/Subtype.kt line 14: Remove '@DefaultObject':
        @@ -14 +14
        - @DefaultObject
        +
        """.trimIndent()
      )
  }

  @Test
  fun sealed_double_annotation() {
    val source = kotlin(
      """
          package slack.model

          import com.squareup.moshi.JsonClass
          import dev.zacsweers.moshix.sealed.annotations.TypeLabel
          import dev.zacsweers.moshix.sealed.annotations.DefaultObject

          @JsonClass(generateAdapter = true, generator = "sealed:type")
          sealed class BaseType

          @TypeLabel(label = "one")
          @DefaultObject
          object Default : BaseType()
        """
    ).indented()

    lint()
      .files(*testFiles(), source)
      .run()
      .expect(
        """
        src/slack/model/BaseType.kt:10: Error: Only use one of @TypeLabel or @DefaultObject. [MoshiUsageDoubleTypeLabel]
        @TypeLabel(label = "one")
        ~~~~~~~~~~~~~~~~~~~~~~~~~
        src/slack/model/BaseType.kt:11: Error: Only use one of @TypeLabel or @DefaultObject. [MoshiUsageDoubleTypeLabel]
        @DefaultObject
        ~~~~~~~~~~~~~~
        2 errors, 0 warnings
        """.trimIndent()
      )
      .expectFixDiffs(
        """
        Fix for src/slack/model/BaseType.kt line 10: Remove '@TypeLabel(label = "one")':
        @@ -10 +10
        - @TypeLabel(label = "one")
        +
        Fix for src/slack/model/BaseType.kt line 11: Remove '@DefaultObject':
        @@ -11 +11
        - @DefaultObject
        +
        """.trimIndent()
      )
  }

  @Test
  fun sealed_missing_type_label() {
    val source = kotlin(
      """
          package slack.model

          import com.squareup.moshi.JsonClass

          @JsonClass(generateAdapter = true, generator = "sealed:type")
          sealed class BaseType

          @JsonClass(generateAdapter = true)
          data class Subtype(val foo: String) : BaseType()

          object ObjectSubType : BaseType()
        """
    ).indented()

    lint()
      .files(*testFiles(), source)
      .run()
      .expect(
        """
        src/slack/model/BaseType.kt:9: Error: Sealed Moshi subtypes must be annotated with @TypeLabel or @DefaultObject. [MoshiUsageMissingTypeLabel]
        data class Subtype(val foo: String) : BaseType()
                   ~~~~~~~
        src/slack/model/BaseType.kt:11: Error: Sealed Moshi subtypes must be annotated with @TypeLabel or @DefaultObject. [MoshiUsageMissingTypeLabel]
        object ObjectSubType : BaseType()
               ~~~~~~~~~~~~~
        2 errors, 0 warnings
        """.trimIndent()
      )
  }

  @Test
  fun sealed_must_be_sealed() {
    val source = kotlin(
      """
          package slack.model

          import com.squareup.moshi.JsonClass

          @JsonClass(generateAdapter = true, generator = "sealed:type")
          abstract class BaseType
        """
    ).indented()

    lint()
      .files(*testFiles(), source)
      .run()
      .expect(
        """
        src/slack/model/BaseType.kt:6: Error: Moshi-sealed can only be applied to 'sealed' types. [MoshiUsageSealedMustBeSealed]
        abstract class BaseType
                       ~~~~~~~~
        1 errors, 0 warnings
        """.trimIndent()
      )
  }

  @Test
  fun sealed_blank_type() {
    val source = kotlin(
      """
          package slack.model

          import com.squareup.moshi.JsonClass

          @JsonClass(generateAdapter = true, generator = "sealed:")
          sealed class BaseType
        """
    ).indented()

    lint()
      .files(*testFiles(), source)
      .run()
      .expect(
        """
        src/slack/model/BaseType.kt:5: Error: Moshi-sealed requires a type label specified after the 'sealed:' prefix. [MoshiUsageBlankTypeLabel]
        @JsonClass(generateAdapter = true, generator = "sealed:")
                                                        ~~~~~~~
        1 errors, 0 warnings
        """.trimIndent()
      )
  }

  @Test
  fun empty_generator() {
    val source = kotlin(
      """
          package slack.model

          import com.squareup.moshi.JsonClass

          @JsonClass(generateAdapter = true, generator = "")
          data class Example(val value: String)
        """
    ).indented()

    lint()
      .files(*testFiles(), source)
      .run()
      .expect(
        """
          src/slack/model/Example.kt:5: Error: Don't use blank JsonClass.generator values. [MoshiUsageBlankGenerator]
          @JsonClass(generateAdapter = true, generator = "")
                                                         ~~
          1 errors, 0 warnings
        """.trimIndent()
      )
  }

  @Test
  fun blank_generator() {
    val source = kotlin(
      """
          package slack.model

          import com.squareup.moshi.JsonClass

          @JsonClass(generateAdapter = true, generator = " ")
          data class Example(val value: String)
        """
    ).indented()

    lint()
      .files(*testFiles(), source)
      .run()
      .expect(
        """
        src/slack/model/Example.kt:5: Error: Don't use blank JsonClass.generator values. [MoshiUsageBlankGenerator]
        @JsonClass(generateAdapter = true, generator = " ")
                                                        ~
        1 errors, 0 warnings
        """.trimIndent()
      )
  }

  @Test
  fun private_constructor() {
    val source = kotlin(
      """
          package slack.model

          import com.squareup.moshi.JsonClass

          @JsonClass(generateAdapter = true)
          data class Example private constructor(val value: String)

          @JsonClass(generateAdapter = true)
          data class Example2 protected constructor(val value: String)
        """
    ).indented()

    lint()
      .files(*testFiles(), source)
      .run()
      .expect(
        """
        src/slack/model/Example.kt:6: Error: Constructors in Moshi classes cannot be private. [MoshiUsagePrivateConstructor]
        data class Example private constructor(val value: String)
                           ~~~~~~~
        src/slack/model/Example.kt:9: Error: Constructors in Moshi classes cannot be private. [MoshiUsagePrivateConstructor]
        data class Example2 protected constructor(val value: String)
                            ~~~~~~~~~
        2 errors, 0 warnings
        """.trimIndent()
      )
      .expectFixDiffs(
        """
        Fix for src/slack/model/Example.kt line 6: Make constructor 'internal':
        @@ -6 +6
        - data class Example private constructor(val value: String)
        + data class Example internal constructor(val value: String)
        Fix for src/slack/model/Example.kt line 9: Make constructor 'internal':
        @@ -9 +9
        - data class Example2 protected constructor(val value: String)
        @@ -10 +9
        + data class Example2 internal constructor(val value: String)
        """.trimIndent()
      )
  }

  @Test
  fun params_that_need_init() {
    val source = kotlin(
      """
          package slack.model

          import com.squareup.moshi.JsonClass
          import kotlin.jvm.Transient

          @JsonClass(generateAdapter = true)
          class Example(val value: String, nonProp: String, @Transient val transientProp: String)
        """
    ).indented()

    lint()
      .files(*testFiles(), source)
      .run()
      .expect(
        """
        src/slack/model/Example.kt:7: Error: Constructor non-property parameters in Moshi classes must have default values. [MoshiUsageParamNeedsInit]
        class Example(val value: String, nonProp: String, @Transient val transientProp: String)
                                         ~~~~~~~~~~~~~~~
        src/slack/model/Example.kt:7: Error: Transient constructor properties must have default values. [MoshiUsageTransientNeedsInit]
        class Example(val value: String, nonProp: String, @Transient val transientProp: String)
                                                          ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        src/slack/model/Example.kt:7: Error: Model classes should be immutable data classes. [MoshiUsageUseData]
        class Example(val value: String, nonProp: String, @Transient val transientProp: String)
              ~~~~~~~
        3 errors, 0 warnings
        """.trimIndent()
      )
  }

  @Test
  fun private_prop() {
    val source = kotlin(
      """
          package slack.model

          import com.squareup.moshi.JsonClass

          @JsonClass(generateAdapter = true)
          data class Example(private val value: String, protected val value2: String)
        """
    ).indented()

    lint()
      .files(*testFiles(), source)
      .run()
      .expect(
        """
        src/slack/model/Example.kt:6: Error: Constructor parameter properties in Moshi classes cannot be private. [MoshiUsagePrivateConstructorProperty]
        data class Example(private val value: String, protected val value2: String)
                           ~~~~~~~
        src/slack/model/Example.kt:6: Error: Constructor parameter properties in Moshi classes cannot be private. [MoshiUsagePrivateConstructorProperty]
        data class Example(private val value: String, protected val value2: String)
                                                      ~~~~~~~~~
        2 errors, 0 warnings
        """.trimIndent()
      )
      .expectFixDiffs(
        """
        Fix for src/slack/model/Example.kt line 6: Make value 'internal':
        @@ -6 +6
        - data class Example(private val value: String, protected val value2: String)
        @@ -7 +6
        + data class Example(internal val value: String, protected val value2: String)
        Fix for src/slack/model/Example.kt line 6: Make value2 'internal':
        @@ -6 +6
        - data class Example(private val value: String, protected val value2: String)
        @@ -7 +6
        + data class Example(private val value: String, internal val value2: String)
        """.trimIndent()
      )
  }

  @Test
  fun mutable_prop() {
    val source = kotlin(
      """
          package slack.model

          import com.squareup.moshi.JsonClass

          @JsonClass(generateAdapter = true)
          data class Example(var value: String)
        """
    ).indented()

    lint()
      .files(*testFiles(), source)
      .run()
      .expect(
        """
        src/slack/model/Example.kt:6: Warning: Moshi properties should be immutable. [MoshiUsageVarProperty]
        data class Example(var value: String)
                           ~~~
        0 errors, 1 warnings
        """.trimIndent()
      )
      .expectFixDiffs(
        """
        Fix for src/slack/model/Example.kt line 6: Make value 'val':
        @@ -6 +6
        - data class Example(var value: String)
        @@ -7 +6
        + data class Example(val value: String)
        """.trimIndent()
      )
  }

  @Test
  fun generateAdapter_should_be_true() {
    val source = kotlin(
      """
          package slack.model

          import com.squareup.moshi.JsonClass

          @JsonClass(generateAdapter = false)
          data class Example(val value: String)
        """
    ).indented()

    lint()
      .files(*testFiles(), source)
      .run()
      .expect(
        """
        src/slack/model/Example.kt:5: Error: JsonClass.generateAdapter must be true in order for Moshi code gen to run. [MoshiUsageGenerateAdapterShouldBeTrue]
        @JsonClass(generateAdapter = false)
                                     ~~~~~
        1 errors, 0 warnings
        """.trimIndent()
      )
  }

  @Test
  fun empty_json_name() {
    val source = kotlin(
      """
          package slack.model

          import com.squareup.moshi.JsonClass
          import com.squareup.moshi.Json

          @JsonClass(generateAdapter = true)
          data class Example(@Json(name = "") val value: String)
        """
    ).indented()

    lint()
      .files(*testFiles(), source)
      .run()
      .expect(
        """
          src/slack/model/Example.kt:7: Error: Don't use blank names in @Json. [MoshiUsageBlankJsonName]
          data class Example(@Json(name = "") val value: String)
                                          ~~
          1 errors, 0 warnings
        """.trimIndent()
      )
  }

  @Test
  fun blank_json_name() {
    val source = kotlin(
      """
          package slack.model

          import com.squareup.moshi.JsonClass
          import com.squareup.moshi.Json

          @JsonClass(generateAdapter = true)
          data class Example(@Json(name = " ") val value: String)
        """
    ).indented()

    lint()
      .files(*testFiles(), source)
      .run()
      .expect(
        """
          src/slack/model/Example.kt:7: Error: Don't use blank names in @Json. [MoshiUsageBlankJsonName]
          data class Example(@Json(name = " ") val value: String)
                                           ~
          1 errors, 0 warnings
        """.trimIndent()
      )
  }

  @Test
  fun jsonNameSiteTargets() {
    val source = kotlin(
      """
          package slack.model

          import com.squareup.moshi.JsonClass
          import com.squareup.moshi.Json

          @JsonClass(generateAdapter = true)
          data class Example(
            @field:Json(name = "foo") val value: String
          )
        """
    ).indented()

    lint()
      .files(*testFiles(), source)
      .run()
      .expect(
        """
          src/slack/model/Example.kt:8: Error: Use of site-targets on @Json are redundant. [MoshiUsageRedundantSiteTarget]
            @field:Json(name = "foo") val value: String
             ~~~~~
          1 errors, 0 warnings
        """.trimIndent()
      )
      .expectFixDiffs(
        """
        Fix for src/slack/model/Example.kt line 8: Remove 'field:':
        @@ -8 +8
        -   @field:Json(name = "foo") val value: String
        +   @Json(name = "foo") val value: String
        """.trimIndent()
      )
  }

  @Test
  fun jsonNameMultiple() {
    val source = kotlin(
      """
          package slack.model

          import com.squareup.moshi.JsonClass
          import com.squareup.moshi.Json

          @JsonClass(generateAdapter = true)
          data class Example(
            @Json(name = "foo") @field:Json(name = "foo") @property:Json(name = "foo") @get:Json(name = "foo") val value: String
          )
        """
    ).indented()

    lint()
      .files(*testFiles(), source)
      .run()
      .expect(
        """
          src/slack/model/Example.kt:8: Error: Use of site-targets on @Json are redundant. [MoshiUsageRedundantSiteTarget]
            @Json(name = "foo") @field:Json(name = "foo") @property:Json(name = "foo") @get:Json(name = "foo") val value: String
                                ~~~~~~~~~~~~~~~~~~~~~~~~~
          src/slack/model/Example.kt:8: Error: Use of site-targets on @Json are redundant. [MoshiUsageRedundantSiteTarget]
            @Json(name = "foo") @field:Json(name = "foo") @property:Json(name = "foo") @get:Json(name = "foo") val value: String
                                                          ~~~~~~~~~~~~~~~~~~~~~~~~~~~~
          src/slack/model/Example.kt:8: Error: Use of site-targets on @Json are redundant. [MoshiUsageRedundantSiteTarget]
            @Json(name = "foo") @field:Json(name = "foo") @property:Json(name = "foo") @get:Json(name = "foo") val value: String
                                                                                       ~~~~~~~~~~~~~~~~~~~~~~~
          3 errors, 0 warnings
        """.trimIndent()
      )
      .expectFixDiffs(
        """
        Fix for src/slack/model/Example.kt line 8: Remove '@field:Json(name = "foo")':
        @@ -8 +8
        -   @Json(name = "foo") @field:Json(name = "foo") @property:Json(name = "foo") @get:Json(name = "foo") val value: String
        +   @Json(name = "foo")  @property:Json(name = "foo") @get:Json(name = "foo") val value: String
        Fix for src/slack/model/Example.kt line 8: Remove '@property:Json(name = "foo")':
        @@ -8 +8
        -   @Json(name = "foo") @field:Json(name = "foo") @property:Json(name = "foo") @get:Json(name = "foo") val value: String
        +   @Json(name = "foo") @field:Json(name = "foo")  @get:Json(name = "foo") val value: String
        Fix for src/slack/model/Example.kt line 8: Remove '@get:Json(name = "foo")':
        @@ -8 +8
        -   @Json(name = "foo") @field:Json(name = "foo") @property:Json(name = "foo") @get:Json(name = "foo") val value: String
        +   @Json(name = "foo") @field:Json(name = "foo") @property:Json(name = "foo")  val value: String
        """.trimIndent()
      )
  }

  // Tweaked multiple site targets test, where we intentionally leave one for a secondary cleanup
  @Test
  fun jsonNameMultipleAllSiteTargets() {
    val source = kotlin(
      """
          package slack.model

          import com.squareup.moshi.JsonClass
          import com.squareup.moshi.Json

          @JsonClass(generateAdapter = true)
          data class Example(
            @field:Json(name = "foo") @property:Json(name = "foo") @get:Json(name = "foo") val value: String
          )
        """
    ).indented()

    lint()
      .files(*testFiles(), source)
      .run()
      .expect(
        """
          src/slack/model/Example.kt:8: Error: Use of site-targets on @Json are redundant. [MoshiUsageRedundantSiteTarget]
            @field:Json(name = "foo") @property:Json(name = "foo") @get:Json(name = "foo") val value: String
                                      ~~~~~~~~~~~~~~~~~~~~~~~~~~~~
          src/slack/model/Example.kt:8: Error: Use of site-targets on @Json are redundant. [MoshiUsageRedundantSiteTarget]
            @field:Json(name = "foo") @property:Json(name = "foo") @get:Json(name = "foo") val value: String
                                                                   ~~~~~~~~~~~~~~~~~~~~~~~
          2 errors, 0 warnings
        """.trimIndent()
      )
      .expectFixDiffs(
        """
        Fix for src/slack/model/Example.kt line 8: Remove '@property:Json(name = "foo")':
        @@ -8 +8
        -   @field:Json(name = "foo") @property:Json(name = "foo") @get:Json(name = "foo") val value: String
        +   @field:Json(name = "foo")  @get:Json(name = "foo") val value: String
        Fix for src/slack/model/Example.kt line 8: Remove '@get:Json(name = "foo")':
        @@ -8 +8
        -   @field:Json(name = "foo") @property:Json(name = "foo") @get:Json(name = "foo") val value: String
        +   @field:Json(name = "foo") @property:Json(name = "foo")  val value: String
        """.trimIndent()
      )
  }

  @Test
  fun snake_case_name() {
    val source = kotlin(
      """
          package slack.model

          import com.squareup.moshi.JsonClass
          import com.squareup.moshi.Json

          @JsonClass(generateAdapter = true)
          data class Example(
            val snake_case: String,
            @Json(name = "taken") val already_annotated_is_ignored: String
          )
        """
    ).indented()

    lint()
      .files(*testFiles(), source)
      .run()
      .expect(
        """
        src/slack/model/Example.kt:8: Warning: Consider using @Json(name = ...) rather than direct snake casing. [MoshiUsageSnakeCase]
          val snake_case: String,
              ~~~~~~~~~~
        0 errors, 1 warnings
        """.trimIndent()
      )
      .expectFixDiffs(
        """
        Fix for src/slack/model/Example.kt line 8: Add @Json(name = "snake_case") and rename to 'snakeCase':
        @@ -8 +8
        -   val snake_case: String,
        +   @Json(name = "snake_case") val snakeCase: String,
        """.trimIndent()
      )
  }

  @Test
  fun missing_primary() {
    val source = kotlin(
      """
          package slack.model

          import com.squareup.moshi.JsonClass

          @JsonClass(generateAdapter = true)
          class Example
        """
    ).indented()

    lint()
      .files(*testFiles(), source)
      .run()
      .expect(
        """
        src/slack/model/Example.kt:6: Error: @JsonClass-annotated types must have a primary constructor or be sealed. [MoshiUsageMissingPrimary]
        class Example
              ~~~~~~~
        src/slack/model/Example.kt:6: Error: Model classes should be immutable data classes. [MoshiUsageUseData]
        class Example
              ~~~~~~~
        2 errors, 0 warnings
        """.trimIndent()
      )
  }

  @Test
  fun missing_primary_ok_in_sealed() {
    val source = kotlin(
      """
          package slack.model

          import com.squareup.moshi.JsonClass

          @JsonClass(generateAdapter = true, generator = "sealed:type")
          sealed class Example
        """
    ).indented()

    lint()
      .files(*testFiles(), source)
      .run()
      .expectClean()
  }

  @Test
  fun unsupportedClasses() {
    val source = kotlin(
      """
          package slack.model

          import com.squareup.moshi.JsonClass

          class Example {
            @JsonClass(generateAdapter = true)
            annotation class UnsupportedAnnotation

            @JsonClass(generateAdapter = true)
            inner class UnsupportedInner

            @JsonClass(generateAdapter = true)
            abstract class UnsupportedAbstract

            @JsonClass(generateAdapter = true)
            interface UnsupportedInterface
          }
        """
    ).indented()

    lint()
      .files(*testFiles(), source)
      .run()
      .expect(
        """
        src/slack/model/Example.kt:6: Error: This type cannot be annotated with @JsonClass. [MoshiUsageUnsupportedType]
          @JsonClass(generateAdapter = true)
          ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        src/slack/model/Example.kt:9: Error: This type cannot be annotated with @JsonClass. [MoshiUsageUnsupportedType]
          @JsonClass(generateAdapter = true)
          ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        src/slack/model/Example.kt:12: Error: This type cannot be annotated with @JsonClass. [MoshiUsageUnsupportedType]
          @JsonClass(generateAdapter = true)
          ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        src/slack/model/Example.kt:15: Error: This type cannot be annotated with @JsonClass. [MoshiUsageUnsupportedType]
          @JsonClass(generateAdapter = true)
          ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        4 errors, 0 warnings
        """.trimIndent()
      )
      .expectFixDiffs(
        """
        Fix for src/slack/model/Example.kt line 6: Remove '@JsonClass(generateAdapter = true)':
        @@ -6 +6
        -   @JsonClass(generateAdapter = true)
        +
        Fix for src/slack/model/Example.kt line 9: Remove '@JsonClass(generateAdapter = true)':
        @@ -9 +9
        -   @JsonClass(generateAdapter = true)
        +
        Fix for src/slack/model/Example.kt line 12: Remove '@JsonClass(generateAdapter = true)':
        @@ -12 +12
        -   @JsonClass(generateAdapter = true)
        +
        Fix for src/slack/model/Example.kt line 15: Remove '@JsonClass(generateAdapter = true)':
        @@ -15 +15
        -   @JsonClass(generateAdapter = true)
        +
        """.trimIndent()
          // Weirdness here because spotless strips the trailing whitespace after the '+'
          .lineSequence()
          .map { line ->
            if (line == "+") {
              "+  "
            } else {
              line
            }
          }
          .joinToString("\n")
      )
  }

  @Test
  fun unsupportedClasses_okWithAdaptedBy() {
    val source = kotlin(
      """
          package slack.model

          import androidx.annotation.Keep
          import com.squareup.moshi.JsonAdapter
          import dev.zacsweers.moshix.adapters.AdaptedBy

          class Example {
            @AdaptedBy(CustomFactory::class)
            annotation class UnsupportedAnnotation

            @AdaptedBy(CustomFactory::class)
            inner class UnsupportedInner

            @AdaptedBy(CustomFactory::class)
            abstract class UnsupportedAbstract

            @AdaptedBy(CustomFactory::class)
            interface UnsupportedInterface
          }

          @Keep
          abstract class CustomFactory : JsonAdapter.Factory
        """
    ).indented()

    lint()
      .files(*testFiles(), source)
      .run()
      .expectClean()
  }

  @Test
  fun double_class_annotation() {
    val source = kotlin(
      """
          package slack.model

          import androidx.annotation.Keep
          import com.squareup.moshi.JsonAdapter
          import com.squareup.moshi.JsonClass
          import dev.zacsweers.moshix.adapters.AdaptedBy

          @JsonClass(generateAdapter = true)
          @AdaptedBy(CustomFactory::class)
          data class Example(val value: String)

          @Keep
          abstract class CustomFactory : JsonAdapter.Factory
        """
    ).indented()

    lint()
      .files(*testFiles(), source)
      .run()
      .expect(
        """
        src/slack/model/Example.kt:8: Error: Only use one of @AdaptedBy or @JsonClass. [MoshiUsageDoubleClassAnnotation]
        @JsonClass(generateAdapter = true)
        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        src/slack/model/Example.kt:9: Error: Only use one of @AdaptedBy or @JsonClass. [MoshiUsageDoubleClassAnnotation]
        @AdaptedBy(CustomFactory::class)
        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        2 errors, 0 warnings
        """.trimIndent()
      )
      .expectFixDiffs(
        """
        Fix for src/slack/model/Example.kt line 8: Remove '@JsonClass(generateAdapter = true)':
        @@ -8 +8
        - @JsonClass(generateAdapter = true)
        +
        Fix for src/slack/model/Example.kt line 9: Remove '@AdaptedBy(CustomFactory::class)':
        @@ -9 +9
        - @AdaptedBy(CustomFactory::class)
        +
        """.trimIndent()
      )
  }

  @Test
  fun valid_adapters() {
    val source = kotlin(
      """
          package slack.model

          import androidx.annotation.Keep
          import com.squareup.moshi.JsonAdapter
          import com.squareup.moshi.JsonClass
          import dev.zacsweers.moshix.adapters.AdaptedBy

          @AdaptedBy(CustomFactory::class)
          class Example1(val value: String)

          @AdaptedBy(CustomAdapter::class)
          class Example2(val value: String)

          @AdaptedBy(NotAnAdapter::class)
          class Example3

          @JsonClass(generateAdapter = true)
          data class Example3(
            @AdaptedBy(CustomAdapter::class) val value1: String,
            @AdaptedBy(NotAnAdapter::class) val value2: String
          )

          @AdaptedBy(CustomAdapterMissingKeep::class)
          class Example4

          @Keep
          abstract class CustomFactory : JsonAdapter.Factory
          @Keep
          abstract class CustomAdapter : JsonAdapter<String>()
          class NotAnAdapter
          abstract class CustomAdapterMissingKeep : JsonAdapter<String>()
        """
    ).indented()

    lint()
      .files(*testFiles(), source)
      .run()
      .expect(
        """
        src/slack/model/Example1.kt:14: Error: @AdaptedBy.adapter must be a JsonAdapter or JsonAdapter.Factory. [MoshiUsageAdaptedByRequiresAdapter]
        @AdaptedBy(NotAnAdapter::class)
                   ~~~~~~~~~~~~~~~~~~~
        src/slack/model/Example1.kt:20: Error: @AdaptedBy.adapter must be a JsonAdapter or JsonAdapter.Factory. [MoshiUsageAdaptedByRequiresAdapter]
          @AdaptedBy(NotAnAdapter::class) val value2: String
                     ~~~~~~~~~~~~~~~~~~~
        src/slack/model/Example1.kt:23: Error: Adapters targeted by @AdaptedBy must have @Keep. [MoshiUsageAdaptedByRequiresKeep]
        @AdaptedBy(CustomAdapterMissingKeep::class)
                   ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        3 errors, 0 warnings
        """.trimIndent()
      )
  }

  @Test
  fun unsupportedClasses_okWithCustomGenerator() {
    val source = kotlin(
      """
          package slack.model

          import com.squareup.moshi.JsonClass

          class Example {
            @JsonClass(generateAdapter = true, generator = "custom")
            annotation class UnsupportedAnnotation

            @JsonClass(generateAdapter = true, generator = "custom")
            inner class UnsupportedInner

            @JsonClass(generateAdapter = true, generator = "custom")
            abstract class UnsupportedAbstract

            @JsonClass(generateAdapter = true, generator = "custom")
            interface UnsupportedInterface
          }
        """
    ).indented()

    lint()
      .files(*testFiles(), source)
      .run()
      .expectClean()
  }

  @Test
  fun unsupported_visibility() {
    val source = kotlin(
      """
          package slack.model

          import com.squareup.moshi.JsonClass

          @JsonClass(generateAdapter = true)
          private data class PrivateClass(val value: String)

          open class EnclosingClass {
            @JsonClass(generateAdapter = true)
            protected data class ProtectedClass(val value: String)
          }
        """
    ).indented()

    lint()
      .files(*testFiles(), source)
      .run()
      .expect(
        """
        src/slack/model/PrivateClass.kt:6: Error: @JsonClass-annotated types must be public, package-private, or internal. [MoshiUsageClassVisibility]
        private data class PrivateClass(val value: String)
        ~~~~~~~
        src/slack/model/PrivateClass.kt:10: Error: @JsonClass-annotated types must be public, package-private, or internal. [MoshiUsageClassVisibility]
          protected data class ProtectedClass(val value: String)
          ~~~~~~~~~
        2 errors, 0 warnings
        """.trimIndent()
      )
      .expectFixDiffs(
        """
        Fix for src/slack/model/PrivateClass.kt line 6: Make 'internal':
        @@ -6 +6
        - private data class PrivateClass(val value: String)
        + internal data class PrivateClass(val value: String)
        Fix for src/slack/model/PrivateClass.kt line 10: Make 'internal':
        @@ -10 +10
        -   protected data class ProtectedClass(val value: String)
        +   internal data class ProtectedClass(val value: String)
        """.trimIndent()
      )
  }

  @Test
  fun enum_prop_suggest_moshi() {
    val source = kotlin(
      """
          package slack.model

          import com.squareup.moshi.JsonClass

          @JsonClass(generateAdapter = true)
          data class Example(val value: TestEnum)

          enum class TestEnum {
            VALUE
          }
        """
    ).indented()

    lint()
      .files(*testFiles(), source)
      .run()
      .expect(
        """
        src/slack/model/Example.kt:6: Warning: Consider making enum properties also use Moshi. [MoshiUsageEnumPropertyCouldBeMoshi]
        data class Example(val value: TestEnum)
                                      ~~~~~~~~
        0 errors, 1 warnings
        """.trimIndent()
      )
  }

  @Test
  fun enum_prop_default_unknown() {
    val source = kotlin(
      """
          package slack.model

          import com.squareup.moshi.JsonClass

          @JsonClass(generateAdapter = true)
          data class Example(
            val value1: TestEnum?,
            val value2: TestEnum? = null,
            val value3: TestEnum? = UNKNOWN,
            val value4: TestEnum = UNKNOWN,
            val value5: TestEnum = TestEnum.UNKNOWN,
          )

          @JsonClass(generateAdapter = false)
          enum class TestEnum {
            UNKNOWN, VALUE
          }
        """
    ).indented()

    lint()
      .files(*testFiles(), source)
      .run()
      .expect(
        """
        src/slack/model/Example.kt:9: Error: Suspicious default value to 'UNKNOWN' for a Moshi enum. [MoshiUsageEnumPropertyDefaultUnknown]
          val value3: TestEnum? = UNKNOWN,
                                  ~~~~~~~
        src/slack/model/Example.kt:10: Error: Suspicious default value to 'UNKNOWN' for a Moshi enum. [MoshiUsageEnumPropertyDefaultUnknown]
          val value4: TestEnum = UNKNOWN,
                                 ~~~~~~~
        src/slack/model/Example.kt:11: Error: Suspicious default value to 'UNKNOWN' for a Moshi enum. [MoshiUsageEnumPropertyDefaultUnknown]
          val value5: TestEnum = TestEnum.UNKNOWN,
                                 ~~~~~~~~~~~~~~~~
        3 errors, 0 warnings
        """.trimIndent()
      )
      .expectFixDiffs(
        """
        Fix for src/slack/model/Example.kt line 9: Remove ' = UNKNOWN':
        @@ -9 +9
        -   val value3: TestEnum? = UNKNOWN,
        +   val value3: TestEnum?,
        Fix for src/slack/model/Example.kt line 10: Remove ' = UNKNOWN':
        @@ -10 +10
        -   val value4: TestEnum = UNKNOWN,
        +   val value4: TestEnum,
        Fix for src/slack/model/Example.kt line 11: Remove ' = TestEnum.UNKNOWN':
        @@ -11 +11
        -   val value5: TestEnum = TestEnum.UNKNOWN,
        +   val value5: TestEnum,
        """.trimIndent()
      )
  }

  @Test
  fun enum_prop_already_moshi() {
    val source = kotlin(
      """
          package slack.model

          import com.squareup.moshi.JsonClass

          @JsonClass(generateAdapter = true)
          data class Example(val value: TestEnum)

          @JsonClass(generateAdapter = false)
          enum class TestEnum {
            UNKNOWN, VALUE
          }
        """
    ).indented()

    lint()
      .files(*testFiles(), source)
      .run()
      .expectClean()
  }

  @Test
  fun objects_cannot_jsonClass() {
    val source = kotlin(
      """
          package slack.model

          import com.squareup.moshi.JsonClass

          @JsonClass(generateAdapter = true)
          object Example
        """
    ).indented()

    lint()
      .files(*testFiles(), source)
      .run()
      .expect(
        """
        src/slack/model/Example.kt:5: Error: Object types cannot be annotated with @JsonClass. [MoshiUsageObject]
        @JsonClass(generateAdapter = true)
        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        1 errors, 0 warnings
        """.trimIndent()
      )
      .expectFixDiffs(
        """
        Fix for src/slack/model/Example.kt line 5: Remove '@JsonClass(generateAdapter = true)':
        @@ -5 +5
        - @JsonClass(generateAdapter = true)
        +
        """.trimIndent()
      )
  }

  @Test
  fun prefer_data_classes() {
    val source = kotlin(
      """
          package slack.model

          import com.squareup.moshi.JsonClass

          @JsonClass(generateAdapter = true)
          class Example(val value: String)
        """
    ).indented()

    lint()
      .files(*testFiles(), source)
      .run()
      .expect(
        """
        src/slack/model/Example.kt:6: Error: Model classes should be immutable data classes. [MoshiUsageUseData]
        class Example(val value: String)
              ~~~~~~~
        1 errors, 0 warnings
        """.trimIndent()
      )
  }

  @Test
  fun propertyTypes() {
    val externalType = kotlin(
      """
      package external

      class ExternalType
    """
    ).indented()
    val externalTypeAnnotated = kotlin(
      """
      package external

      import com.squareup.moshi.JsonClass

      @JsonClass(generateAdapter = true)
      data class ExternalTypeAnnotated(val value: String)
    """
    ).indented()
    val internalType = kotlin(
      """
      package slack

      class InternalType
    """
    ).indented()
    val internalTypeAnnotated = kotlin(
      """
      package slack

      import androidx.annotation.Keep
      import com.squareup.moshi.JsonClass
      import com.squareup.moshi.JsonAdapter
      import dev.zacsweers.moshix.adapters.AdaptedBy

      @JsonClass(generateAdapter = true)
      data class InternalTypeAnnotated(val value: String)

      @AdaptedBy(InternalTypeAdapter::class)
      data class InternalTypeAnnotated2(val value: String)

      @Keep
      abstract class InternalTypeAdapter : JsonAdapter.Factory
    """
    ).indented()
    val jsonQualifier = kotlin(
      """
      package com.squareup.moshi

      annotation class JsonQualifier
    """
    ).indented()
    val customQualifier = kotlin(
      """
      package test

      import com.squareup.moshi.JsonQualifier

      @JsonQualifier
      annotation class CustomQualifier
    """
    ).indented()

    val source = kotlin(
      """
          package slack.model

          import androidx.annotation.Keep
          import com.squareup.moshi.JsonAdapter
          import com.squareup.moshi.JsonClass
          import java.util.ArrayList
          import java.util.HashSet
          import java.util.HashMap
          import java.util.Date
          import external.ExternalType
          import external.ExternalTypeAnnotated
          import slack.InternalType
          import slack.InternalTypeAnnotated
          import slack.InternalTypeAnnotated2
          import dev.zacsweers.moshix.adapters.AdaptedBy
          import test.CustomQualifier

          @JsonClass(generateAdapter = true)
          data class Example(
            // collections
            val okList: List<Int>,
            val okSet: Set<Int>,
            val okCollection: Collection<Int>,
            val okMap: Map<String, String>,
            val concreteList: ArrayList<Int>,
            val concreteSet: HashSet<Int>,
            val concreteMap: HashMap<String, String>,
            // platform
            val platformType: Date,
            @AdaptedBy(DateFactory::class) val adaptedByOk: Date,
            // external
            val externalType: ExternalType,
            val externalTypeAnnotated: ExternalTypeAnnotated,
            // internal
            val internalType: InternalType,
            val internalTypeAnnotated: InternalTypeAnnotated,
            val internalTypeAnnotated2: InternalTypeAnnotated2,
            val int: Int,
            val string: String,
            val nullableString: String?,
            val any: Any,
            // Arrays
            val arrayType: Array<String>,
            val intArray: IntArray,
            val boolArray: BooleanArray,
            val complexArray: Array<List<String>>,
            val badGeneric: List<ExternalType>,
            val badGeneric2: CustomGenericType<ExternalType>,
            val badNestedGeneric: CustomGenericType<List<ExternalType>>,
            // This would normally error but since it has a custom qualifier we skip the check
            @CustomQualifier val customQualifier: Date,
            // Mutable collections
            val mutableList: MutableList<Int>,
            val mutableSet: MutableSet<Int>,
            val mutableCollection: MutableCollection<Int>,
            val mutableMap: MutableMap<String, String>
          )

          @Keep
          abstract class DateFactory : JsonAdapter.Factory

          @JsonClass(generateAdapter = true)
          data class CustomGenericType<T>(val value: T)
        """
    ).indented()

    lint()
      .files(*testFiles(), externalType, externalTypeAnnotated, internalType, internalTypeAnnotated, jsonQualifier, customQualifier, source)
      .run()
      .expect(
        """
        src/slack/model/Example.kt:43: Warning: Prefer List over Array. [MoshiUsageArray]
          val arrayType: Array<String>,
                         ~~~~~~~~~~~~~
        src/slack/model/Example.kt:44: Warning: Prefer List over Array. [MoshiUsageArray]
          val intArray: IntArray,
                        ~~~~~~~~
        src/slack/model/Example.kt:45: Warning: Prefer List over Array. [MoshiUsageArray]
          val boolArray: BooleanArray,
                         ~~~~~~~~~~~~
        src/slack/model/Example.kt:46: Warning: Prefer List over Array. [MoshiUsageArray]
          val complexArray: Array<List<String>>,
                            ~~~~~~~~~~~~~~~~~~~
        src/slack/model/Example.kt:53: Error: Use immutable collections rather than mutable versions. [MoshiUsageMutableCollections]
          val mutableList: MutableList<Int>,
                           ~~~~~~~~~~~~~~~~
        src/slack/model/Example.kt:54: Error: Use immutable collections rather than mutable versions. [MoshiUsageMutableCollections]
          val mutableSet: MutableSet<Int>,
                          ~~~~~~~~~~~~~~~
        src/slack/model/Example.kt:55: Error: Use immutable collections rather than mutable versions. [MoshiUsageMutableCollections]
          val mutableCollection: MutableCollection<Int>,
                                 ~~~~~~~~~~~~~~~~~~~~~~
        src/slack/model/Example.kt:56: Error: Use immutable collections rather than mutable versions. [MoshiUsageMutableCollections]
          val mutableMap: MutableMap<String, String>
                          ~~~~~~~~~~~~~~~~~~~~~~~~~~
        src/slack/model/Example.kt:25: Information: Concrete Collection type 'ArrayList' is not natively supported by Moshi. [MoshiUsageNonMoshiClassCollection]
          val concreteList: ArrayList<Int>,
                            ~~~~~~~~~~~~~~
        src/slack/model/Example.kt:26: Information: Concrete Collection type 'HashSet' is not natively supported by Moshi. [MoshiUsageNonMoshiClassCollection]
          val concreteSet: HashSet<Int>,
                           ~~~~~~~~~~~~
        src/slack/model/Example.kt:32: Error: External type 'ExternalType' is not natively supported by Moshi. [MoshiUsageNonMoshiClassExternal]
          val externalType: ExternalType,
                            ~~~~~~~~~~~~
        src/slack/model/Example.kt:47: Error: External type 'ExternalType' is not natively supported by Moshi. [MoshiUsageNonMoshiClassExternal]
          val badGeneric: List<ExternalType>,
                          ~~~~~~~~~~~~~~~~~~
        src/slack/model/Example.kt:48: Error: External type 'ExternalType' is not natively supported by Moshi. [MoshiUsageNonMoshiClassExternal]
          val badGeneric2: CustomGenericType<ExternalType>,
                           ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        src/slack/model/Example.kt:49: Error: External type 'ExternalType' is not natively supported by Moshi. [MoshiUsageNonMoshiClassExternal]
          val badNestedGeneric: CustomGenericType<List<ExternalType>>,
                                ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        src/slack/model/Example.kt:35: Information: Non-Moshi internal type 'InternalType' is not natively supported by Moshi. [MoshiUsageNonMoshiClassInternal]
          val internalType: InternalType,
                            ~~~~~~~~~~~~
        src/slack/model/Example.kt:27: Information: Concrete Map type 'HashMap' is not natively supported by Moshi. [MoshiUsageNonMoshiClassMap]
          val concreteMap: HashMap<String, String>,
                           ~~~~~~~~~~~~~~~~~~~~~~~
        src/slack/model/Example.kt:29: Warning: Platform type 'Date' is not natively supported by Moshi. [MoshiUsageNonMoshiClassPlatform]
          val platformType: Date,
                            ~~~~
        8 errors, 5 warnings
        """.trimIndent()
      )
      .expectFixDiffs(
        """
        Fix for src/slack/model/Example.kt line 43: Change to List<String>:
        @@ -43 +43
        -   val arrayType: Array<String>,
        +   val arrayType: List<String>,
        Fix for src/slack/model/Example.kt line 44: Change to List<Int>:
        @@ -44 +44
        -   val intArray: IntArray,
        +   val intArray: List<Int>,
        Fix for src/slack/model/Example.kt line 45: Change to List<Boolean>:
        @@ -45 +45
        -   val boolArray: BooleanArray,
        +   val boolArray: List<Boolean>,
        Fix for src/slack/model/Example.kt line 46: Change to List<List<String>>:
        @@ -46 +46
        -   val complexArray: Array<List<String>>,
        +   val complexArray: List<List<String>>,
        Fix for src/slack/model/Example.kt line 53: Change to List:
        @@ -53 +53
        -   val mutableList: MutableList<Int>,
        +   val mutableList: List<Int>,
        Fix for src/slack/model/Example.kt line 54: Change to Set:
        @@ -54 +54
        -   val mutableSet: MutableSet<Int>,
        +   val mutableSet: Set<Int>,
        Fix for src/slack/model/Example.kt line 55: Change to Collection:
        @@ -55 +55
        -   val mutableCollection: MutableCollection<Int>,
        +   val mutableCollection: Collection<Int>,
        Fix for src/slack/model/Example.kt line 56: Change to Map:
        @@ -56 +56
        -   val mutableMap: MutableMap<String, String>
        +   val mutableMap: Map<String, String>
        Fix for src/slack/model/Example.kt line 25: Change to List:
        @@ -25 +25
        -   val concreteList: ArrayList<Int>,
        +   val concreteList: List<Int>,
        Fix for src/slack/model/Example.kt line 26: Change to Set:
        @@ -26 +26
        -   val concreteSet: HashSet<Int>,
        +   val concreteSet: Set<Int>,
        Fix for src/slack/model/Example.kt line 27: Change to Map:
        @@ -27 +27
        -   val concreteMap: HashMap<String, String>,
        +   val concreteMap: Map<String, String>,
        """.trimIndent()
      )
  }

  @Test
  fun kotlin_jsonQualifierAnnotation_ok() {
    val source = kotlin(
      """
          package slack.model

          import com.squareup.moshi.JsonQualifier
          import kotlin.annotation.Retention
          import kotlin.annotation.AnnotationRetention
          import kotlin.annotation.AnnotationRetention.RUNTIME
          import kotlin.annotation.AnnotationTarget.PROPERTY
          import kotlin.annotation.AnnotationTarget
          import kotlin.annotation.AnnotationTarget.FIELD

          @JsonQualifier
          annotation class NoAnnotationsIsOk

          @Target(FIELD)
          @JsonQualifier
          annotation class NoRetentionIsOk

          @Target(AnnotationRetention.FIELD)
          @Retention(AnnotationRetention.RUNTIME)
          @JsonQualifier
          annotation class CorrectTargetAndRetention

          @Target(PROPERTY, AnnotationRetention.FIELD)
          @Retention(RUNTIME)
          @JsonQualifier
          annotation class CorrectTargetAndRetention2

          @Target([PROPERTY, FIELD])
          @Retention(RUNTIME)
          @JsonQualifier
          annotation class CorrectTargetAndRetention3

          @Target(PROPERTY)
          @JsonQualifier
          annotation class MissingTarget

          @Retention(AnnotationRetention.BINARY)
          @JsonQualifier
          annotation class WrongRetention
        """
    ).indented()

    lint()
      .files(*testFiles(), source)
      .run()
      .expect(
        """
        src/slack/model/NoAnnotationsIsOk.kt:37: Error: JsonQualifiers must have RUNTIME retention. [MoshiUsageQualifierRetention]
        @Retention(AnnotationRetention.BINARY)
        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        src/slack/model/NoAnnotationsIsOk.kt:33: Error: JsonQualifiers must include FIELD targeting. [MoshiUsageQualifierTarget]
        @Target(PROPERTY)
        ~~~~~~~~~~~~~~~~~
        2 errors, 0 warnings
        """.trimIndent()
      )
      .expectFixDiffs(
        """
        Fix for src/slack/model/NoAnnotationsIsOk.kt line 37: Remove '@Retention(AnnotationRetention.BINARY)':
        @@ -37 +37
        - @Retention(AnnotationRetention.BINARY)
        +
        """.trimIndent()
      )
  }

  @Test
  fun java_jsonQualifierAnnotation_ok() {
    val source = java(
      """
          package slack.model;

          import com.squareup.moshi.JsonQualifier;
          import java.lang.annotation.ElementType;
          import static java.lang.annotation.ElementType.FIELD;
          import java.lang.annotation.Retention;
          import java.lang.annotation.RetentionPolicy;
          import static java.lang.annotation.RetentionPolicy.RUNTIME;
          import java.lang.annotation.Target;

          @Retention(RetentionPolicy.RUNTIME)
          @JsonQualifier
          public @interface NoTargetIsOk {}

          @Target(ElementType.FIELD)
          @Retention(RetentionPolicy.RUNTIME)
          @JsonQualifier
          public @interface CorrectTargetAndRetention {}

          @Target({FIELD, ElementType.METHOD})
          @Retention(RUNTIME)
          @JsonQualifier
          public @interface CorrectTargetAndRetention2 {}

          @Target(ElementType.METHOD)
          @Retention(RUNTIME)
          @JsonQualifier
          public @interface MissingField {}

          @Target(FIELD)
          @Retention(RetentionPolicy.CLASS)
          @JsonQualifier
          public @interface WrongRetention {}

          @Target(FIELD)
          @JsonQualifier
          public @interface MissingRetention {}
        """
    ).indented()

    lint()
      .files(*testFiles(), source)
      .run()
      .expect(
        """
          src/slack/model/NoTargetIsOk.java:31: Error: JsonQualifiers must have RUNTIME retention. [MoshiUsageQualifierRetention]
          @Retention(RetentionPolicy.CLASS)
          ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
          src/slack/model/NoTargetIsOk.java:37: Error: JsonQualifiers must have RUNTIME retention. [MoshiUsageQualifierRetention]
          public @interface MissingRetention {}
                            ~~~~~~~~~~~~~~~~
          src/slack/model/NoTargetIsOk.java:25: Error: JsonQualifiers must include FIELD targeting. [MoshiUsageQualifierTarget]
          @Target(ElementType.METHOD)
          ~~~~~~~~~~~~~~~~~~~~~~~~~~~
          3 errors, 0 warnings
        """.trimIndent()
      )
      .expectFixDiffs(
        """
        Fix for src/slack/model/NoTargetIsOk.java line 31: Replace with RUNTIME:
        @@ -31 +31
        - @Retention(RetentionPolicy.CLASS)
        + @Retention(RetentionPolicy.RUNTIME)
        """.trimIndent()
      )
  }

  @Test
  fun redundantJsonName() {
    val source = kotlin(
      """
          package slack.model

          import com.squareup.moshi.Json
          import com.squareup.moshi.JsonClass

          @JsonClass(generateAdapter = true)
          data class Example(@Json(name = "value") val value: String)
        """
    ).indented()

    lint()
      .files(*testFiles(), source)
      .run()
      .expect(
        """
          src/slack/model/Example.kt:7: Warning: Json.name with the same value as the property/enum member name is redundant. [MoshiUsageRedundantJsonName]
          data class Example(@Json(name = "value") val value: String)
                                           ~~~~~
          0 errors, 1 warnings
        """.trimIndent()
      )
      .expectFixDiffs(
        """
        Fix for src/slack/model/Example.kt line 7: Remove '@Json(name = "value")':
        @@ -7 +7
        - data class Example(@Json(name = "value") val value: String)
        @@ -8 +7
        + data class Example( val value: String)
        """.trimIndent()
      )
  }

  @Test
  fun serializedNameIssues() {
    val serializedName = java(
      """
        package com.google.gson.annotations;

        public @interface SerializedName {
          String value();
          String[] alternate() default {};
        }
      """.trimIndent()
    )
    val source = kotlin(
      """
          package slack.model

          import com.squareup.moshi.Json
          import com.squareup.moshi.JsonClass
          import com.google.gson.annotations.SerializedName

          @JsonClass(generateAdapter = true)
          data class Example(
            val noAnnotations: String,
            @Json(name = "full_moshi") val fullMoshi: String,
            @SerializedName("full_gson") val fullGson: String,
            @SerializedName("full_gson_alts", alternate = ["foo"]) val fullGsonAlternates: String,
            @Json(name = "mixed") @SerializedName("mixed") val mixedSame: String,
            @Json(name = "mixed_diff") @SerializedName("mixed_diff_2") val mixedDiff: String,
            @Json(name = "mixed_alts") @SerializedName("mixed_alts", alternate = ["foo"]) val mixedAlternates: String,
          )
        """
    ).indented()

    lint()
      .files(*testFiles(), serializedName, source)
      .run()
      .expect(
        """
          src/slack/model/Example.kt:11: Error: Use Moshi's @Json rather than Gson's @SerializedName. [MoshiUsageSerializedName]
            @SerializedName("full_gson") val fullGson: String,
            ~~~~~~~~~~~~~~~~~~~~~~~~~~~~
          src/slack/model/Example.kt:12: Error: Use Moshi's @Json rather than Gson's @SerializedName. [MoshiUsageSerializedName]
            @SerializedName("full_gson_alts", alternate = ["foo"]) val fullGsonAlternates: String,
            ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
          src/slack/model/Example.kt:13: Error: Use Moshi's @Json rather than Gson's @SerializedName. [MoshiUsageSerializedName]
            @Json(name = "mixed") @SerializedName("mixed") val mixedSame: String,
                                  ~~~~~~~~~~~~~~~~~~~~~~~~
          src/slack/model/Example.kt:14: Error: Use Moshi's @Json rather than Gson's @SerializedName. [MoshiUsageSerializedName]
            @Json(name = "mixed_diff") @SerializedName("mixed_diff_2") val mixedDiff: String,
                                       ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
          src/slack/model/Example.kt:15: Error: Use Moshi's @Json rather than Gson's @SerializedName. [MoshiUsageSerializedName]
            @Json(name = "mixed_alts") @SerializedName("mixed_alts", alternate = ["foo"]) val mixedAlternates: String,
                                       ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
          5 errors, 0 warnings
        """.trimIndent()
      )
      .expectFixDiffs(
        """
        Fix for src/slack/model/Example.kt line 11: Replace with @Json(name = "full_gson"):
        @@ -11 +11
        -   @SerializedName("full_gson") val fullGson: String,
        +   @Json(name = "full_gson") val fullGson: String,
        Fix for src/slack/model/Example.kt line 13: Remove '@SerializedName("mixed")':
        @@ -13 +13
        -   @Json(name = "mixed") @SerializedName("mixed") val mixedSame: String,
        +   @Json(name = "mixed")  val mixedSame: String,
        """.trimIndent()
      )
  }

  @Test
  fun duplicateNames() {
    val source = kotlin(
      """
          package slack.model

          import com.squareup.moshi.Json
          import com.squareup.moshi.JsonClass

          @JsonClass(generateAdapter = true)
          data class Example(
            val value: String,
            @Json(name = "value") val anotherValue: String,
            @Json(name = "value2") val anotherValue2: String,
            @Json(name = "value2") val anotherValue3: String
          )
        """
    ).indented()

    lint()
      .files(*testFiles(), source)
      .run()
      .expect(
        """
          src/slack/model/Example.kt:8: Error: Name 'value' is duplicated by member 'anotherValue'. [MoshiUsageDuplicateJsonName]
            val value: String,
                ~~~~~
          src/slack/model/Example.kt:9: Error: Name 'value' is duplicated by member 'value'. [MoshiUsageDuplicateJsonName]
            @Json(name = "value") val anotherValue: String,
                                      ~~~~~~~~~~~~
          src/slack/model/Example.kt:10: Error: Name 'value2' is duplicated by member 'anotherValue3'. [MoshiUsageDuplicateJsonName]
            @Json(name = "value2") val anotherValue2: String,
                                       ~~~~~~~~~~~~~
          src/slack/model/Example.kt:11: Error: Name 'value2' is duplicated by member 'anotherValue2'. [MoshiUsageDuplicateJsonName]
            @Json(name = "value2") val anotherValue3: String
                                       ~~~~~~~~~~~~~
          4 errors, 0 warnings
        """.trimIndent()
      )
  }

  private fun testFiles() = arrayOf(
    keepAnnotation,
    jsonClassAnnotation,
    jsonAnnotation,
    jsonQualifierAnnotation,
    typeLabel,
    defaultObject,
    adaptedBy,
    jsonAdapter
  )
}
