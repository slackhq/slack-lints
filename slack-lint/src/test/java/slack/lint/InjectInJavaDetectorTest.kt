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

class InjectInJavaDetectorTest : BaseSlackLintTest() {

  companion object {
    private val JAVAX_STUBS = kotlin(
      """
      package javax.inject

      annotation class Inject
      """.trimIndent()
    )
    private val DAGGER_STUBS = kotlin(
      """
      package dagger

      annotation class Module
      """.trimIndent()
    )
    private val ASSISTED_STUBS = kotlin(
      """
      package dagger.assisted

      annotation class AssistedInject
      annotation class AssistedFactory
      """.trimIndent()
    )
  }

  override fun getDetector() = InjectInJavaDetector()

  override fun getIssues() = listOf(InjectInJavaDetector.ISSUE)

  @Test
  fun kotlinIsOk() {
    lint()
      .files(
        JAVAX_STUBS,
        DAGGER_STUBS,
        ASSISTED_STUBS,
        kotlin(
          """
            package test.pkg

            import javax.inject.Inject
            import dagger.Module
            import dagger.assisted.AssistedInject
            import dagger.assisted.AssistedFactory

            class KotlinClass @Inject constructor(val constructorInjected: String) {
              @Inject lateinit var memberInjected: String

              @Inject fun methodInject(value: String) {

              }
            }

            class KotlinAssistedClass @AssistedInject constructor(
              @Assisted val assistedParam: String
            ) {
              @AssistedFactory
              interface Factory {
                fun create(assistedParam: String): KotlinAssistedClass
              }
            }

            @Module
            object ExampleModule
          """
        ).indented()
      )
      .allowCompilationErrors(false)
      .run()
      .expectClean()
  }
  @Test
  fun javaIsNotOk() {
    lint()
      .files(
        JAVAX_STUBS,
        DAGGER_STUBS,
        ASSISTED_STUBS,
        java(
          """
            package test.pkg;

            import javax.inject.Inject;
            import dagger.Module;
            import dagger.assisted.AssistedInject;
            import dagger.assisted.AssistedFactory;

            class JavaClass {
              @Inject String memberInjected;

              @Inject JavaClass(String constructorInjected) {

              }

              @Inject void methodInject(String value) {

              }

              static class JavaAssistedClass {

                @AssistedInject JavaAssistedClass(@Assisted String assistedParam) {

                }

                @AssistedFactory
                interface Factory {
                  JavaAssistedClass create(String assistedParam);
                }
              }

              @Module static abstract class ExampleModule {

              }
            }

          """
        ).indented()
      )
      .allowCompilationErrors(false)
      .run()
      .expect(
        """
          src/test/pkg/JavaClass.java:9: Error: Only Kotlin classes should be injected in order for Anvil to work. [InjectInJava]
            @Inject String memberInjected;
            ~~~~~~~
          src/test/pkg/JavaClass.java:11: Error: Only Kotlin classes should be injected in order for Anvil to work. [InjectInJava]
            @Inject JavaClass(String constructorInjected) {
            ~~~~~~~
          src/test/pkg/JavaClass.java:15: Error: Only Kotlin classes should be injected in order for Anvil to work. [InjectInJava]
            @Inject void methodInject(String value) {
            ~~~~~~~
          src/test/pkg/JavaClass.java:21: Error: Only Kotlin classes should be injected in order for Anvil to work. [InjectInJava]
              @AssistedInject JavaAssistedClass(@Assisted String assistedParam) {
              ~~~~~~~~~~~~~~~
          src/test/pkg/JavaClass.java:25: Error: Only Kotlin classes should be injected in order for Anvil to work. [InjectInJava]
              @AssistedFactory
              ~~~~~~~~~~~~~~~~
          src/test/pkg/JavaClass.java:31: Error: Only Kotlin classes should be injected in order for Anvil to work. [InjectInJava]
            @Module static abstract class ExampleModule {
            ~~~~~~~
          6 errors, 0 warnings
        """.trimIndent()
      )
  }
}
