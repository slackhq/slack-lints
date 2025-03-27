// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint

import org.junit.Test

class NullableConcurrentHashMapDetectorTest : BaseSlackLintTest() {
  override fun getDetector() = NullableConcurrentHashMapDetector()

  override fun getIssues() = listOf(NullableConcurrentHashMapDetector.ISSUE)

  @Test
  fun concurrentHashMapWithNonNullableTypes() {
    lint()
      .files(
        kotlin(
          """
           fun test() {
               val map = java.util.concurrent.ConcurrentHashMap<String, Int>()
               val map2: ConcurrentHashMap<String, Int> = java.util.concurrent.ConcurrentHashMap()
           }
           """
        )
      )
      .run()
      .expectClean()
  }

  @Test
  fun concurrentHashMapWithNullableKeyType() {
    lint()
      .files(
        kotlin(
          """
           fun test() {
               val map = java.util.concurrent.ConcurrentHashMap<String?, Int>()
               val map2: java.util.concurrent.ConcurrentHashMap<String?, Int> = java.util.concurrent.ConcurrentHashMap()
           }
           """
        )
      )
      .run()
      .expect(
        """
        src/test.kt:3: Error: ConcurrentHashMap should not use nullable key types [NullableConcurrentHashMap]
                       val map = java.util.concurrent.ConcurrentHashMap<String?, Int>()
                                                                        ~~~~~~~
        src/test.kt:4: Error: ConcurrentHashMap should not use nullable key types [NullableConcurrentHashMap]
                       val map2: java.util.concurrent.ConcurrentHashMap<String?, Int> = java.util.concurrent.ConcurrentHashMap()
                                                                        ~~~~~~~
        2 errors
        """
          .trimIndent()
      )
  }

  @Test
  fun concurrentHashMapWithNullableValueType() {
    lint()
      .files(
        kotlin(
          """
          fun test() {
              val map = java.util.concurrent.ConcurrentHashMap<String, Int?>()
          }
          """
        )
      )
      .run()
      .expect(
        """
        src/test.kt:3: Error: ConcurrentHashMap should not use nullable value types [NullableConcurrentHashMap]
                      val map = java.util.concurrent.ConcurrentHashMap<String, Int?>()
                                                                               ~~~~
        1 error
        """
          .trimIndent()
      )
  }

  @Test
  fun concurrentHashMapWithBothNullableTypes() {
    lint()
      .files(
        kotlin(
          """
          fun test() {
              val map = java.util.concurrent.ConcurrentHashMap<String?, Int?>()
          }
          """
        )
      )
      .run()
      .expect(
        """
        src/test.kt:3: Error: ConcurrentHashMap should not use nullable key types [NullableConcurrentHashMap]
                      val map = java.util.concurrent.ConcurrentHashMap<String?, Int?>()
                                                                       ~~~~~~~
        src/test.kt:3: Error: ConcurrentHashMap should not use nullable value types [NullableConcurrentHashMap]
                      val map = java.util.concurrent.ConcurrentHashMap<String?, Int?>()
                                                                                ~~~~
        2 errors
        """
          .trimIndent()
      )
  }

  @Test
  fun javaConcurrentHashMapWithNullableAnnotation() {
    lint()
      .files(
        java(
          """
          package test;

          import java.lang.annotation.ElementType;
          import java.lang.annotation.Target;
          import java.util.concurrent.ConcurrentHashMap;

          public class Test {
              @Target(ElementType.TYPE)
              public @interface Nullable {}

              public void test() {
                  var map = new ConcurrentHashMap<@Nullable String, Integer>();
                  ConcurrentHashMap<@Nullable String, Integer> map2 = new ConcurrentHashMap<>();
              }
          }
          """
        )
      )
      .run()
      .expect(
        """
        src/test/Test.java:13: Error: ConcurrentHashMap should not use nullable key types [NullableConcurrentHashMap]
                          var map = new ConcurrentHashMap<@Nullable String, Integer>();
                                                          ~~~~~~~~~~~~~~~~
        src/test/Test.java:14: Error: ConcurrentHashMap should not use nullable key types [NullableConcurrentHashMap]
                          ConcurrentHashMap<@Nullable String, Integer> map2 = new ConcurrentHashMap<>();
                                            ~~~~~~~~~~~~~~~~
        2 errors
        """
          .trimIndent()
      )
  }
}
