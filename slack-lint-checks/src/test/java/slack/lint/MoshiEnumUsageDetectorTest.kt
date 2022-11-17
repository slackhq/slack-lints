// Copyright (C) 2021 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint

import org.junit.Test

class MoshiEnumUsageDetectorTest : BaseSlackLintTest() {

  private val jsonClassAnnotation =
    java(
        """
        package com.squareup.moshi;

        public @interface JsonClass {
          boolean generateAdapter();
          String generator() default "";
        }
      """
      )
      .indented()

  private val jsonAnnotation =
    java(
        """
      package com.squareup.moshi;

      public @interface Json {
        String name();
      }
    """
      )
      .indented()

  override fun getDetector() = MoshiUsageDetector()
  override fun getIssues() = MoshiUsageDetector.issues().toList()

  @Test
  fun java_correct() {
    val correctJava =
      java(
          """
          package slack.model;

          import com.squareup.moshi.JsonClass;

          @JsonClass(generateAdapter = false)
          enum TestEnum {
            UNKNOWN, TEST
          }
        """
        )
        .indented()

    lint().files(jsonClassAnnotation, jsonAnnotation, correctJava).run().expectClean()
  }

  @Test
  fun java_expected_unknown_is_ok() {
    val correctJava =
      java(
          """
          package slack.model;

          import com.squareup.moshi.JsonClass;
          import com.squareup.moshi.Json;

          @JsonClass(generateAdapter = false)
          enum TestEnum {
            UNKNOWN, TEST, @Json(name = "UNKNOWN") EXPECTED_UNKNOWN
          }
        """
        )
        .indented()

    lint().files(jsonClassAnnotation, jsonAnnotation, correctJava).run().expectClean()
  }

  @Test
  fun java_ignored() {
    val source =
      java(
          """
          package slack.model;

          import com.squareup.moshi.JsonClass;

          public enum TestEnum {
            TEST
          }
        """
        )
        .indented()

    lint().files(jsonClassAnnotation, jsonAnnotation, source).run().expectClean()
  }

  @Test
  fun java_generateAdapter_isTrue() {
    val source =
      java(
          """
          package slack.model;

          import com.squareup.moshi.JsonClass;

          @JsonClass(generateAdapter = true)
          public enum TestEnum {
            UNKNOWN, TEST
          }
        """
        )
        .indented()

    lint()
      .files(jsonClassAnnotation, jsonAnnotation, source)
      .run()
      .expect(
        """
        src/slack/model/TestEnum.java:5: Error: Enums annotated with @JsonClass must not set generateAdapter to true. [MoshiUsageEnumJsonClassGenerated]
        @JsonClass(generateAdapter = true)
                                     ~~~~
        1 errors, 0 warnings
        """
          .trimIndent()
      )
      .expectFixDiffs(
        """
        Fix for src/slack/model/TestEnum.java line 5: Set to false:
        @@ -5 +5
        - @JsonClass(generateAdapter = true)
        + @JsonClass(generateAdapter = false)
        """
          .trimIndent()
      )
  }

  @Test
  fun java_custom_generator_is_ignored() {
    val source =
      java(
          """
          package slack.model;

          import com.squareup.moshi.JsonClass;

          @JsonClass(generateAdapter = true, generator = "custom")
          public enum TestEnum {
            TEST
          }
        """
        )
        .indented()

    lint().files(jsonClassAnnotation, jsonAnnotation, source).run().expectClean()
  }

  @Test
  fun java_unknown_annotated() {
    val source =
      java(
          """
          package slack.model;

          import com.squareup.moshi.JsonClass;
          import com.squareup.moshi.Json;

          @JsonClass(generateAdapter = false)
          public enum TestEnum {
            @Json(name = "unknown")
            UNKNOWN,
            TEST
          }
        """
        )
        .indented()

    lint()
      .files(jsonClassAnnotation, jsonAnnotation, source)
      .run()
      .expect(
        """
        src/slack/model/TestEnum.java:7: Error: UNKNOWN members in @JsonClass-annotated enums should not be annotated with @Json [MoshiUsageEnumAnnotatedUnknown]
        public enum TestEnum {
                    ~~~~~~~~
        1 errors, 0 warnings
        """
          .trimIndent()
      )
  }

  @Test
  fun java_unknown_wrong_order() {
    val source =
      java(
          """
          package slack.model;

          import com.squareup.moshi.JsonClass;

          @JsonClass(generateAdapter = false)
          public enum TestEnum {
            TEST,
            UNKNOWN
          }
        """
        )
        .indented()

    lint()
      .files(jsonClassAnnotation, jsonAnnotation, source)
      .run()
      .expect(
        """
        src/slack/model/TestEnum.java:8: Error: Enums serialized with Moshi must reserve the first member as UNKNOWN. [MoshiUsageEnumMissingUnknown]
          UNKNOWN
          ~~~~~~~
        1 errors, 0 warnings
        """
          .trimIndent()
      )
  }

  @Test
  fun java_unknown_wrong_order_inferred_enum() {
    val source =
      java(
          """
        package slack.model;

        import com.squareup.moshi.JsonClass;
        import com.squareup.moshi.Json;

        @JsonClass(generateAdapter = false)
        public enum TestEnum {
          @Json(name = "test")
          TEST,
          UNKNOWN
        }
      """
        )
        .indented()

    lint()
      .files(jsonClassAnnotation, jsonAnnotation, source)
      .run()
      .expect(
        """
        src/slack/model/TestEnum.java:10: Error: Enums serialized with Moshi must reserve the first member as UNKNOWN. [MoshiUsageEnumMissingUnknown]
          UNKNOWN
          ~~~~~~~
        1 errors, 0 warnings
        """
          .trimIndent()
      )
  }

  @Test
  fun java_json_but_missing_json_class() {
    val source =
      java(
          """
        package slack.model;

        import com.squareup.moshi.JsonClass;
        import com.squareup.moshi.Json;

        public enum TestEnum {
          UNKNOWN,
          @Json(name = "test")
          TEST;
        }
      """
        )
        .indented()

    lint()
      .files(jsonClassAnnotation, jsonAnnotation, source)
      .run()
      .expect(
        """
        src/slack/model/TestEnum.java:6: Error: Enums serialized with Moshi should be annotated with @JsonClass. [MoshiUsageEnumMissingJsonClass]
        public enum TestEnum {
                    ~~~~~~~~
        1 errors, 0 warnings
        """
          .trimIndent()
      )
  }

  @Test
  fun java_lower_casing() {
    val source =
      java(
          """
        package slack.model;

        import com.squareup.moshi.JsonClass;

        @JsonClass(generateAdapter = false)
        enum TestEnum {
          UNKNOWN,
          test
        }
      """
        )
        .indented()

    lint()
      .files(jsonClassAnnotation, jsonAnnotation, source)
      .run()
      .expect(
        """
          src/slack/model/TestEnum.java:8: Warning: Consider using @Json(name = ...) rather than lower casing. [MoshiUsageEnumCasing]
            test
            ~~~~
          0 errors, 1 warnings
        """
          .trimIndent()
      )
  }

  @Test
  fun java_lower_casing_already_annotated() {
    val source =
      java(
          """
        package slack.model;

        import com.squareup.moshi.Json;
        import com.squareup.moshi.JsonClass;

        @JsonClass(generateAdapter = false)
        enum TestEnum {
          UNKNOWN,
          @Json(name = "taken") test
        }
      """
        )
        .indented()

    lint()
      .files(jsonClassAnnotation, jsonAnnotation, source)
      .run()
      .expect(
        """
          src/slack/model/TestEnum.java:9: Warning: Consider using @Json(name = ...) rather than lower casing. [MoshiUsageEnumCasing]
            @Json(name = "taken") test
                                  ~~~~
          0 errors, 1 warnings
        """
          .trimIndent()
      )
      .expectFixDiffs(
        """
        Fix for src/slack/model/TestEnum.java line 9: Rename to 'TEST':
        @@ -9 +9
        -   @Json(name = "taken") test
        +   @Json(name = "taken") TEST
        """
          .trimIndent()
      )
  }

  @Test
  fun java_json_name_blank() {
    val source =
      java(
          """
        package slack.model;

        import com.squareup.moshi.Json;
        import com.squareup.moshi.JsonClass;

        @JsonClass(generateAdapter = false)
        enum TestEnum {
          UNKNOWN,
          @Json(name = " ") TEST
        }
      """
        )
        .indented()

    lint()
      .files(jsonClassAnnotation, jsonAnnotation, source)
      .run()
      .expect(
        """
          src/slack/model/TestEnum.java:9: Error: Don't use blank names in @Json. [MoshiUsageBlankJsonName]
            @Json(name = " ") TEST
                         ~~~
          1 errors, 0 warnings
        """
          .trimIndent()
      )
  }

  @Test
  fun java_json_name_empty() {
    val source =
      java(
          """
        package slack.model;

        import com.squareup.moshi.Json;
        import com.squareup.moshi.JsonClass;

        @JsonClass(generateAdapter = false)
        enum TestEnum {
          UNKNOWN,
          @Json(name = "") TEST
        }
      """
        )
        .indented()

    lint()
      .files(jsonClassAnnotation, jsonAnnotation, source)
      .run()
      .expect(
        """
          src/slack/model/TestEnum.java:9: Error: Don't use blank names in @Json. [MoshiUsageBlankJsonName]
            @Json(name = "") TEST
                         ~~
          1 errors, 0 warnings
        """
          .trimIndent()
      )
  }

  @Test
  fun java_redundantJsonName() {
    val source =
      java(
          """
          package slack.model;

          import com.squareup.moshi.Json;
          import com.squareup.moshi.JsonClass;

          @JsonClass(generateAdapter = false)
          public enum Example {
            UNKNOWN,
            @Json(name = "VALUE") VALUE
          }
        """
        )
        .indented()

    lint()
      .files(jsonClassAnnotation, jsonAnnotation, source)
      .run()
      .expect(
        """
          src/slack/model/Example.java:9: Warning: Json.name with the same value as the property/enum member name is redundant. [MoshiUsageRedundantJsonName]
            @Json(name = "VALUE") VALUE
                         ~~~~~~~
          0 errors, 1 warnings
        """
          .trimIndent()
      )
      .expectFixDiffs(
        """
        Fix for src/slack/model/Example.java line 9: Remove '@Json(name = "VALUE")':
        @@ -9 +9
        -   @Json(name = "VALUE") VALUE
        +    VALUE
        """
          .trimIndent()
      )
  }

  @Test
  fun java_duplicateJsonNames() {
    val source =
      java(
          """
          package slack.model;

          import com.squareup.moshi.Json;
          import com.squareup.moshi.JsonClass;

          @JsonClass(generateAdapter = false)
          public enum Example {
            UNKNOWN,
            VALUE,
            @Json(name = "VALUE") VALUE_1,
            @Json(name = "value2") VALUE_2,
            @Json(name = "value2") VALUE_3;
          }
        """
        )
        .indented()

    lint()
      .files(jsonAnnotation, jsonClassAnnotation, source)
      .run()
      .expect(
        """
          src/slack/model/Example.java:9: Error: Name 'VALUE' is duplicated by member 'VALUE_1'. [MoshiUsageDuplicateJsonName]
            VALUE,
            ~~~~~
          src/slack/model/Example.java:10: Error: Name 'VALUE' is duplicated by member 'VALUE'. [MoshiUsageDuplicateJsonName]
            @Json(name = "VALUE") VALUE_1,
                                  ~~~~~~~
          src/slack/model/Example.java:11: Error: Name 'value2' is duplicated by member 'VALUE_3'. [MoshiUsageDuplicateJsonName]
            @Json(name = "value2") VALUE_2,
                                   ~~~~~~~
          src/slack/model/Example.java:12: Error: Name 'value2' is duplicated by member 'VALUE_2'. [MoshiUsageDuplicateJsonName]
            @Json(name = "value2") VALUE_3;
                                   ~~~~~~~
          4 errors, 0 warnings
        """
          .trimIndent()
      )
  }

  @Test
  fun kotlin_correct() {
    val source =
      kotlin(
          """
          package slack.model

          import com.squareup.moshi.JsonClass

          @JsonClass(generateAdapter = false)
          enum class TestEnum {
            UNKNOWN, TEST
          }
        """
        )
        .indented()

    lint().files(jsonClassAnnotation, jsonAnnotation, source).run().expectClean()
  }

  @Test
  fun kotlin_expected_unknown_is_ok() {
    val source =
      kotlin(
          """
          package slack.model

          import com.squareup.moshi.JsonClass
          import com.squareup.moshi.Json

          @JsonClass(generateAdapter = false)
          enum class TestEnum {
            UNKNOWN, TEST, @Json(name = "UNKNOWN") EXPECTED_UNKNOWN
          }
        """
        )
        .indented()

    lint().files(jsonClassAnnotation, jsonAnnotation, source).run().expectClean()
  }

  @Test
  fun kotlin_ignored() {
    val source =
      kotlin(
          """
          package slack.model

          import com.squareup.moshi.JsonClass

          enum class TestEnum {
            TEST
          }
        """
        )
        .indented()

    lint().files(jsonClassAnnotation, jsonAnnotation, source).run().expectClean()
  }

  @Test
  fun kotlin_generateAdapter_isTrue() {
    val source =
      kotlin(
          """
          package slack.model

          import com.squareup.moshi.JsonClass

          @JsonClass(generateAdapter = true)
          enum class TestEnum {
            UNKNOWN, TEST
          }
        """
        )
        .indented()

    lint()
      .files(jsonClassAnnotation, jsonAnnotation, source)
      .run()
      .expect(
        """
        src/slack/model/TestEnum.kt:5: Error: Enums annotated with @JsonClass must not set generateAdapter to true. [MoshiUsageEnumJsonClassGenerated]
        @JsonClass(generateAdapter = true)
                                     ~~~~
        1 errors, 0 warnings
        """
          .trimIndent()
      )
      .expectFixDiffs(
        """
        Fix for src/slack/model/TestEnum.kt line 5: Set to false:
        @@ -5 +5
        - @JsonClass(generateAdapter = true)
        + @JsonClass(generateAdapter = false)
        """
          .trimIndent()
      )
  }

  @Test
  fun kotlin_custom_generator_is_ignored() {
    val source =
      kotlin(
          """
          package slack.model

          import com.squareup.moshi.JsonClass

          @JsonClass(generateAdapter = true, generator = "custom")
          enum class TestEnum {
            TEST
          }
        """
        )
        .indented()

    lint().files(jsonClassAnnotation, jsonAnnotation, source).run().expectClean()
  }

  @Test
  fun kotlin_unknown_annotated() {
    val source =
      kotlin(
          """
          package slack.model

          import com.squareup.moshi.JsonClass
          import com.squareup.moshi.Json

          @JsonClass(generateAdapter = false)
          enum class TestEnum {
            @Json(name = "unknown")
            UNKNOWN,
            TEST
          }
        """
        )
        .indented()

    lint()
      .files(jsonClassAnnotation, jsonAnnotation, source)
      .run()
      .expect(
        """
        src/slack/model/TestEnum.kt:7: Error: UNKNOWN members in @JsonClass-annotated enums should not be annotated with @Json [MoshiUsageEnumAnnotatedUnknown]
        enum class TestEnum {
                   ~~~~~~~~
        1 errors, 0 warnings
        """
          .trimIndent()
      )
  }

  @Test
  fun kotlin_unknown_wrong_order() {
    val source =
      kotlin(
          """
          package slack.model

          import com.squareup.moshi.JsonClass

          @JsonClass(generateAdapter = false)
          enum class TestEnum {
            TEST,
            UNKNOWN
          }
        """
        )
        .indented()

    lint()
      .files(jsonClassAnnotation, jsonAnnotation, source)
      .run()
      .expect(
        """
        src/slack/model/TestEnum.kt:8: Error: Enums serialized with Moshi must reserve the first member as UNKNOWN. [MoshiUsageEnumMissingUnknown]
          UNKNOWN
          ~~~~~~~
        1 errors, 0 warnings
        """
          .trimIndent()
      )
  }

  @Test
  fun kotlin_unknown_wrong_order_inferred_enum() {
    val source =
      kotlin(
          """
        package slack.model

        import com.squareup.moshi.JsonClass
        import com.squareup.moshi.Json

        @JsonClass(generateAdapter = false)
        enum class TestEnum {
          @Json(name = "test")
          TEST,
          UNKNOWN
        }
      """
        )
        .indented()

    lint()
      .files(jsonClassAnnotation, jsonAnnotation, source)
      .run()
      .expect(
        """
        src/slack/model/TestEnum.kt:10: Error: Enums serialized with Moshi must reserve the first member as UNKNOWN. [MoshiUsageEnumMissingUnknown]
          UNKNOWN
          ~~~~~~~
        1 errors, 0 warnings
        """
          .trimIndent()
      )
  }

  @Test
  fun kotlin_json_but_missing_json_class() {
    val source =
      kotlin(
          """
        package slack.model

        import com.squareup.moshi.JsonClass
        import com.squareup.moshi.Json

        enum class TestEnum {
          UNKNOWN,
          @Json(name = "test")
          TEST
        }
      """
        )
        .indented()

    lint()
      .files(jsonClassAnnotation, jsonAnnotation, source)
      .run()
      .expect(
        """
        src/slack/model/TestEnum.kt:6: Error: Enums serialized with Moshi should be annotated with @JsonClass. [MoshiUsageEnumMissingJsonClass]
        enum class TestEnum {
                   ~~~~~~~~
        1 errors, 0 warnings
        """
          .trimIndent()
      )
  }

  @Test
  fun kotlin_lower_casing() {
    val source =
      kotlin(
          """
        package slack.model

        import com.squareup.moshi.JsonClass

        @JsonClass(generateAdapter = false)
        enum class TestEnum {
          UNKNOWN,
          test
        }
      """
        )
        .indented()

    lint()
      .files(jsonClassAnnotation, jsonAnnotation, source)
      .run()
      .expect(
        """
          src/slack/model/TestEnum.kt:8: Warning: Consider using @Json(name = ...) rather than lower casing. [MoshiUsageEnumCasing]
            test
            ~~~~
          0 errors, 1 warnings
        """
          .trimIndent()
      )
      .expectFixDiffs(
        """
        Fix for src/slack/model/TestEnum.kt line 8: Add @Json(name = "test") and rename to 'TEST':
        @@ -8 +8
        -   test
        +   @com.squareup.moshi.Json(name = "test") TEST
        """
          .trimIndent()
      )
  }

  @Test
  fun kotlin_lower_casing_already_annotated() {
    val source =
      kotlin(
          """
        package slack.model

        import com.squareup.moshi.Json
        import com.squareup.moshi.JsonClass

        @JsonClass(generateAdapter = false)
        enum class TestEnum {
          UNKNOWN,
          @Json(name = "taken") test
        }
      """
        )
        .indented()

    lint()
      .files(jsonClassAnnotation, jsonAnnotation, source)
      .run()
      .expect(
        """
          src/slack/model/TestEnum.kt:9: Warning: Consider using @Json(name = ...) rather than lower casing. [MoshiUsageEnumCasing]
            @Json(name = "taken") test
                                  ~~~~
          0 errors, 1 warnings
        """
          .trimIndent()
      )
      .expectFixDiffs(
        """
        Fix for src/slack/model/TestEnum.kt line 9: Rename to 'TEST':
        @@ -9 +9
        -   @Json(name = "taken") test
        +   @Json(name = "taken") TEST
        """
          .trimIndent()
      )
  }

  @Test
  fun kotlin_json_name_blank() {
    val source =
      kotlin(
          """
        package slack.model

        import com.squareup.moshi.Json
        import com.squareup.moshi.JsonClass

        @JsonClass(generateAdapter = false)
        enum class TestEnum {
          UNKNOWN,
          @Json(name = " ") TEST
        }
      """
        )
        .indented()

    lint()
      .files(jsonClassAnnotation, jsonAnnotation, source)
      .run()
      .expect(
        """
          src/slack/model/TestEnum.kt:9: Error: Don't use blank names in @Json. [MoshiUsageBlankJsonName]
            @Json(name = " ") TEST
                          ~
          1 errors, 0 warnings
        """
          .trimIndent()
      )
  }

  @Test
  fun kotlin_json_name_empty() {
    val source =
      kotlin(
          """
        package slack.model

        import com.squareup.moshi.Json
        import com.squareup.moshi.JsonClass

        @JsonClass(generateAdapter = false)
        enum class TestEnum {
          UNKNOWN,
          @Json(name = "") TEST
        }
      """
        )
        .indented()

    lint()
      .files(jsonClassAnnotation, jsonAnnotation, source)
      .run()
      .expect(
        """
          src/slack/model/TestEnum.kt:9: Error: Don't use blank names in @Json. [MoshiUsageBlankJsonName]
            @Json(name = "") TEST
                         ~~
          1 errors, 0 warnings
        """
          .trimIndent()
      )
  }

  @Test
  fun kotlin_redundantJsonName() {
    val source =
      kotlin(
          """
          package slack.model

          import com.squareup.moshi.Json
          import com.squareup.moshi.JsonClass

          @JsonClass(generateAdapter = false)
          enum class Example {
            UNKNOWN,
            @Json(name = "VALUE") VALUE
          }
        """
        )
        .indented()

    lint()
      .files(jsonClassAnnotation, jsonAnnotation, source)
      .run()
      .expect(
        """
          src/slack/model/Example.kt:9: Warning: Json.name with the same value as the property/enum member name is redundant. [MoshiUsageRedundantJsonName]
            @Json(name = "VALUE") VALUE
                          ~~~~~
          0 errors, 1 warnings
        """
          .trimIndent()
      )
      .expectFixDiffs(
        """
        Fix for src/slack/model/Example.kt line 9: Remove '@Json(name = "VALUE")':
        @@ -9 +9
        -   @Json(name = "VALUE") VALUE
        +    VALUE
        """
          .trimIndent()
      )
  }

  @Test
  fun kotlin_duplicateJsonNames() {
    val source =
      kotlin(
          """
          package slack.model

          import com.squareup.moshi.Json
          import com.squareup.moshi.JsonClass

          @JsonClass(generateAdapter = false)
          enum class Example {
            UNKNOWN,
            VALUE,
            @Json(name = "VALUE") VALUE_1,
            @Json(name = "value2") VALUE_2,
            @Json(name = "value2") VALUE_3
          }
        """
        )
        .indented()

    lint()
      .files(jsonAnnotation, jsonClassAnnotation, source)
      .run()
      .expect(
        """
          src/slack/model/Example.kt:9: Error: Name 'VALUE' is duplicated by member 'VALUE_1'. [MoshiUsageDuplicateJsonName]
            VALUE,
            ~~~~~
          src/slack/model/Example.kt:10: Error: Name 'VALUE' is duplicated by member 'VALUE'. [MoshiUsageDuplicateJsonName]
            @Json(name = "VALUE") VALUE_1,
                                  ~~~~~~~
          src/slack/model/Example.kt:11: Error: Name 'value2' is duplicated by member 'VALUE_3'. [MoshiUsageDuplicateJsonName]
            @Json(name = "value2") VALUE_2,
                                   ~~~~~~~
          src/slack/model/Example.kt:12: Error: Name 'value2' is duplicated by member 'VALUE_2'. [MoshiUsageDuplicateJsonName]
            @Json(name = "value2") VALUE_3
                                   ~~~~~~~
          4 errors, 0 warnings
        """
          .trimIndent()
      )
  }
}
