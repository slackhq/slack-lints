// Copyright (C) 2021 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
@file:Suppress("UnstableApiUsage")

package slack.lint

import org.junit.Ignore
import org.junit.Test

class DoNotCallProvidersDetectorTest : BaseSlackLintTest() {

  private companion object {
    private val javaxAnnotation =
      kotlin(
          """
        package javax.annotation

        annotation class Generated(val message: String)
      """
        )
        .indented()
    private val daggerStubs =
      kotlin(
          """
        package dagger

        annotation class Binds
        annotation class Provides
        annotation class Module
      """
        )
        .indented()

    private val daggerProducerStubs =
      kotlin("""
        package dagger.producers

        annotation class Produces
      """)
        .indented()
  }

  override fun getDetector() = DoNotCallProvidersDetector()

  override fun getIssues() = listOf(DoNotCallProvidersDetector.ISSUE)

  @Ignore(
    "This fails on github actions for some reason, but we're upstreaming this to Dagger anyway"
  )
  @Test
  fun kotlin() {
    lint()
      .files(
        javaxAnnotation,
        daggerStubs,
        daggerProducerStubs,
        kotlin(
            """
                  package foo
                  import dagger.Binds
                  import dagger.Module
                  import dagger.Provides
                  import dagger.producers.Produces
                  import javax.annotation.Generated

                  @Module
                  abstract class MyModule {

                    @Binds fun binds1(input: String): Comparable<String>
                    @Binds fun String.binds2(): Comparable<String>

                    fun badCode() {
                      binds1("this is bad")
                      "this is bad".binds2()
                      provider()
                      producer()
                    }

                    companion object {
                      @Provides
                      fun provider(): String {
                        return ""
                      }
                      @Produces
                      fun producer(): String {
                        return ""
                      }
                    }
                  }

                  @Generated("Totes generated code")
                  abstract class GeneratedCode {
                    fun doStuff() {
                      moduleInstance().binds1("this is technically fine but would never happen in dagger")
                      MyModule.provider()
                      MyModule.producer()
                    }

                    abstract fun moduleInstance(): MyModule
                  }
                """
          )
          .indented(),
      )
      .allowCompilationErrors(false)
      .run()
      .expect(
        """
              src/foo/MyModule.kt:15: Error: Dagger provider methods should not be called directly by user code. [DoNotCallProviders]
                  binds1("this is bad")
                  ~~~~~~~~~~~~~~~~~~~~~
              src/foo/MyModule.kt:16: Error: Dagger provider methods should not be called directly by user code. [DoNotCallProviders]
                  "this is bad".binds2()
                   ~~~~~~~~~~~~~~~~~~~~~
              src/foo/MyModule.kt:17: Error: Dagger provider methods should not be called directly by user code. [DoNotCallProviders]
                  provider()
                  ~~~~~~~~~~
              src/foo/MyModule.kt:18: Error: Dagger provider methods should not be called directly by user code. [DoNotCallProviders]
                  producer()
                  ~~~~~~~~~~
              4 errors, 0 warnings
        """
          .trimIndent()
      )
  }

  @Test
  fun java() {
    lint()
      .files(
        javaxAnnotation,
        daggerStubs,
        daggerProducerStubs,
        java(
            """
                  package foo;
                  import dagger.Binds;
                  import dagger.Module;
                  import dagger.Provides;
                  import dagger.producers.Produces;
                  import javax.annotation.Generated;

                  class Holder {
                    @Module
                    abstract class MyModule {

                      @Binds Comparable<String> binds1(String input);

                      void badCode() {
                        binds1("this is bad");
                        provider();
                        producer();
                      }

                      @Provides
                      static String provider() {
                        return "";
                      }
                      @Produces
                      static String producer() {
                        return "";
                      }
                    }

                    @Generated("Totes generated code")
                    abstract class GeneratedCode {
                      void doStuff() {
                        moduleInstance().binds1("this is technically fine but would never happen in dagger");
                        MyModule.provider();
                        MyModule.producer();
                      }

                      abstract MyModule moduleInstance();
                    }
                  }
                """
          )
          .indented(),
      )
      .allowCompilationErrors(false)
      .run()
      .expect(
        """
              src/foo/Holder.java:15: Error: Dagger provider methods should not be called directly by user code. [DoNotCallProviders]
                    binds1("this is bad");
                    ~~~~~~~~~~~~~~~~~~~~~
              src/foo/Holder.java:16: Error: Dagger provider methods should not be called directly by user code. [DoNotCallProviders]
                    provider();
                    ~~~~~~~~~~
              src/foo/Holder.java:17: Error: Dagger provider methods should not be called directly by user code. [DoNotCallProviders]
                    producer();
                    ~~~~~~~~~~
              3 errors, 0 warnings
        """
          .trimIndent()
      )
  }
}
