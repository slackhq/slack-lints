// Copyright (C) 2020 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint

import com.android.tools.lint.checks.infrastructure.TestFile
import com.android.tools.lint.checks.infrastructure.TestMode
import org.junit.Ignore
import org.junit.Test

class JavaOnlyDetectorTest : BaseSlackLintTest() {

  companion object {
    val ANNOTATIONS: TestFile =
      kotlin(
          """
          package slack.lint.annotations
          annotation class KotlinOnly(val reason: String)
          annotation class JavaOnly(val reason: String)
          """
        )
        .indented()
  }

  override fun getDetector() = JavaOnlyDetector()

  override fun getIssues() = listOf(JavaOnlyDetector.ISSUE)

  // TODO fix these
  override val skipTestModes: Array<TestMode> = arrayOf(TestMode.SUPPRESSIBLE)

  @Test
  fun positive() {
    lint()
      .detector(JavaOnlyDetector())
      .files(
        ANNOTATIONS,
        kotlin(
            "test/test/pkg/Test.kt",
            """
                  package test.pkg
                  import slack.lint.annotations.JavaOnly
                  class Test {
                    @JavaOnly fun g() {}
                    @JavaOnly("satisfying explanation") fun f() {}
                    fun m() {
                      g()
                      f()
                      val r = this::g
                    }
                  }
                  """,
          )
          .indented(),
      )
      .run()
      .expect(
        """
          test/test/pkg/Test.kt:7: Error: This method should not be called from Kotlin, see its documentation for details. [JavaOnlyDetector]
              g()
              ~~~
          test/test/pkg/Test.kt:8: Error: This method should not be called from Kotlin: satisfying explanation [JavaOnlyDetector]
              f()
              ~~~
          test/test/pkg/Test.kt:9: Error: This method should not be called from Kotlin, see its documentation for details. [JavaOnlyDetector]
              val r = this::g
                      ~~~~~~~
          3 errors, 0 warnings
        """
          .trimIndent()
      )
  }

  @Ignore("Un-ignore when https://groups.google.com/forum/#!topic/lint-dev/8Nr0-SDdHbk is fixed")
  @Test
  fun positivePackage() {
    lint()
      .detector(JavaOnlyDetector())
      .files(
        ANNOTATIONS,
        java(
            "test/test/pkg/package-info.java",
            """
                  @slack.lint.annotations.JavaOnly
                  package test.pkg;
                  import slack.lint.annotations.JavaOnly;""",
          )
          .indented(),
        java(
            "test/test/pkg/A.java",
            """
                  package test.pkg;
                  public class A {
                    public static void f() {}
                  }
                """,
          )
          .indented(),
        kotlin(
            "test/test/pkg2/Test.kt",
            """
                  package test.pkg2
                  import slack.lint.annotations.JavaOnly
                  class Test {
                    fun m() {
                      test.pkg.A.f()
                    }
                  }
                  """,
          )
          .indented(),
      )
      .run()
      .expect(
        """
          test/test/pkg2/Test.kt:5: Error: This method should not be called from Kotlin: see its documentation for details. [JavaOnlyDetector]
              f()
              ~~~
          1 errors, 0 warnings
        """
          .trimIndent()
      )
  }

  @Test
  fun annotateBothFunction() {
    lint()
      .detector(JavaOnlyDetector())
      .files(
        ANNOTATIONS,
        kotlin(
            "test/test/pkg/Test.kt",
            """
                  package test.pkg
                  import slack.lint.annotations.JavaOnly
                  import slack.lint.annotations.KotlinOnly
                  interface Test {
                    @JavaOnly @KotlinOnly fun f()
                  }
                  """,
          )
          .indented(),
      )
      .run()
      .expect(
        """
          test/test/pkg/Test.kt:5: Error: Cannot annotate functions with both @KotlinOnly and @JavaOnly [JavaOnlyDetector]
            @JavaOnly @KotlinOnly fun f()
            ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
          1 errors, 0 warnings
        """
          .trimIndent()
      )
  }

  @Test
  fun annotateBothClass() {
    lint()
      .detector(JavaOnlyDetector())
      .files(
        ANNOTATIONS,
        kotlin(
            "test/test/pkg/Test.kt",
            """
                  package test.pkg
                  import slack.lint.annotations.JavaOnly
                  import slack.lint.annotations.KotlinOnly
                  @JavaOnly @KotlinOnly interface Test {
                    fun f()
                  }
                  """,
          )
          .indented(),
      )
      .run()
      .expect(
        """
          test/test/pkg/Test.kt:4: Error: Cannot annotate types with both @KotlinOnly and @JavaOnly [JavaOnlyDetector]
          @JavaOnly @KotlinOnly interface Test {
          ^
          1 errors, 0 warnings
        """
          .trimIndent()
      )
  }

  @Test
  fun requiredOverrideKotlin() {
    lint()
      .detector(JavaOnlyDetector())
      .files(
        ANNOTATIONS,
        kotlin(
            "test/test/pkg/A.kt",
            """
                  package test.pkg
                  import slack.lint.annotations.KotlinOnly
                  interface A {
                    @KotlinOnly fun f()
                  }
                  """,
          )
          .indented(),
        kotlin(
            "test/test/pkg/B.kt",
            """
                  package test.pkg
                  class B : A {
                    override fun f()
                  }
                  """,
          )
          .indented(),
      )
      .run()
      .expect(
        """
          test/test/pkg/B.kt:3: Error: Function overrides f in A which is annotated @KotlinOnly, it should also be annotated. [JavaOnlyDetector]
            override fun f()
            ~~~~~~~~~~~~~~~~
          1 errors, 0 warnings
        """
          .trimIndent()
      )
      .expectFixDiffs(
        """
          Fix for test/test/pkg/B.kt line 3: Add @KotlinOnly:
          @@ -3 +3
          -   override fun f()
          +   @slack.lint.annotations.KotlinOnly override fun f()
        """
          .trimIndent()
      )
  }

  @Test
  fun annotatedOverrideKotlin() {
    lint()
      .detector(JavaOnlyDetector())
      .files(
        ANNOTATIONS,
        kotlin(
            "test/test/pkg/A.kt",
            """
                  package test.pkg
                  import slack.lint.annotations.KotlinOnly
                  interface A {
                    @KotlinOnly fun f()
                  }
                  """,
          )
          .indented(),
        kotlin(
            "test/test/pkg/B.kt",
            """
                  package test.pkg
                  import slack.lint.annotations.KotlinOnly
                  class B : A {
                    @KotlinOnly override fun f()
                  }
                  """,
          )
          .indented(),
      )
      .run()
      .expectClean()
  }

  @Test
  fun requiredOverrideJava() {
    lint()
      .detector(JavaOnlyDetector())
      .files(
        ANNOTATIONS,
        kotlin(
            "test/test/pkg/A.kt",
            """
                  package test.pkg
                  import slack.lint.annotations.JavaOnly
                  interface A {
                    @JavaOnly fun f()
                  }
                  """,
          )
          .indented(),
        kotlin(
            "test/test/pkg/B.kt",
            """
                  package test.pkg
                  class B : A {
                    override fun f()
                  }
                  """,
          )
          .indented(),
      )
      .run()
      .expect(
        """
          test/test/pkg/B.kt:3: Error: Function overrides f in A which is annotated @JavaOnly, it should also be annotated. [JavaOnlyDetector]
            override fun f()
            ~~~~~~~~~~~~~~~~
          1 errors, 0 warnings
        """
          .trimIndent()
      )
      .expectFixDiffs(
        """
          Fix for test/test/pkg/B.kt line 3: Add @JavaOnly:
          @@ -3 +3
          -   override fun f()
          +   @slack.lint.annotations.JavaOnly override fun f()
        """
          .trimIndent()
      )
  }

  @Test
  fun annotatedOverrideJava() {
    lint()
      .detector(JavaOnlyDetector())
      .files(
        ANNOTATIONS,
        kotlin(
            "test/test/pkg/A.kt",
            """
                  package test.pkg
                  import slack.lint.annotations.JavaOnly
                  interface A {
                    @JavaOnly fun f()
                  }
                  """,
          )
          .indented(),
        kotlin(
            "test/test/pkg/B.kt",
            """
                  package test.pkg
                  import slack.lint.annotations.JavaOnly
                  class B : A {
                    @JavaOnly override fun f()
                  }
                  """,
          )
          .indented(),
      )
      .run()
      .expectClean()
  }

  @Test
  fun requiredOverrideKotlinClass() {
    lint()
      .detector(JavaOnlyDetector())
      .files(
        ANNOTATIONS,
        kotlin(
            "test/test/pkg/A.kt",
            """
                  package test.pkg
                  import slack.lint.annotations.KotlinOnly
                  @KotlinOnly interface A {
                    fun f()
                  }
                  """,
          )
          .indented(),
        kotlin(
            "test/test/pkg/B.kt",
            """
                  package test.pkg
                  class B : A {
                    override fun f()
                  }
                  """,
          )
          .indented(),
      )
      .run()
      .expect(
        """
          test/test/pkg/B.kt:2: Error: Type subclasses/implements A in A.kt which is annotated @KotlinOnly, it should also be annotated. [JavaOnlyDetector]
          class B : A {
          ^
          1 errors, 0 warnings
        """
          .trimIndent()
      )
      .expectFixDiffs(
        """
          Fix for test/test/pkg/B.kt line 2: Add @KotlinOnly:
          @@ -2 +2
          - class B : A {
          + @slack.lint.annotations.KotlinOnly class B : A {
        """
          .trimIndent()
      )
  }

  @Test
  fun annotatedOverrideKotlinClass() {
    lint()
      .detector(JavaOnlyDetector())
      .files(
        ANNOTATIONS,
        kotlin(
            "test/test/pkg/A.kt",
            """
                  package test.pkg
                  import slack.lint.annotations.KotlinOnly
                  @KotlinOnly interface A {
                    fun f()
                  }
                  """,
          )
          .indented(),
        kotlin(
            "test/test/pkg/B.kt",
            """
                  package test.pkg
                  import slack.lint.annotations.KotlinOnly
                  @KotlinOnly class B : A {
                    override fun f()
                  }
                  """,
          )
          .indented(),
      )
      .run()
      .expectClean()
  }

  @Test
  fun requiredOverrideJavaClass() {
    lint()
      .detector(JavaOnlyDetector())
      .files(
        ANNOTATIONS,
        kotlin(
            "test/test/pkg/A.kt",
            """
                  package test.pkg
                  import slack.lint.annotations.JavaOnly
                  @JavaOnly interface A {
                    fun f()
                  }
                  """,
          )
          .indented(),
        kotlin(
            "test/test/pkg/B.kt",
            """
                  package test.pkg
                  class B : A {
                    override fun f()
                  }
                  """,
          )
          .indented(),
      )
      .run()
      .expect(
        """
          test/test/pkg/B.kt:2: Error: Type subclasses/implements A in A.kt which is annotated @JavaOnly, it should also be annotated. [JavaOnlyDetector]
          class B : A {
          ^
          1 errors, 0 warnings
        """
          .trimIndent()
      )
      .expectFixDiffs(
        """
          Fix for test/test/pkg/B.kt line 2: Add @JavaOnly:
          @@ -2 +2
          - class B : A {
          + @slack.lint.annotations.JavaOnly class B : A {
        """
          .trimIndent()
      )
  }

  @Test
  fun annotatedOverrideJavaClass() {
    lint()
      .detector(JavaOnlyDetector())
      .files(
        ANNOTATIONS,
        kotlin(
            "test/test/pkg/A.kt",
            """
                  package test.pkg
                  import slack.lint.annotations.JavaOnly
                  @JavaOnly interface A {
                    fun f()
                  }
                  """,
          )
          .indented(),
        kotlin(
            "test/test/pkg/B.kt",
            """
                  package test.pkg
                  import slack.lint.annotations.JavaOnly
                  @JavaOnly class B : A {
                    override fun f()
                  }
                  """,
          )
          .indented(),
      )
      .run()
      .expectClean()
  }

  // The interface tries to make Object#toString @JavaOnly, and because
  // the declaration in B is implicit it doesn't get checked.
  // In practice, making default Object methods @JavaOnly isn't super
  // useful - typically users interface with the interface directly
  // (e.g. Hasher) or there's an override that has unwanted behaviour (Localizable).
  // NOTE: This has slightly different behavior than the error prone checker, as in UAST
  // the overridden method actually propagates down through types and ErrorProne's doesn't.
  @Test
  fun interfaceRedeclaresObjectMethod() {
    lint()
      .detector(JavaOnlyDetector())
      .files(
        ANNOTATIONS,
        kotlin(
            "test/test/pkg/I.kt",
            """
                  package test.pkg
                  import slack.lint.annotations.JavaOnly
                  interface I {
                    @JavaOnly override fun toString(): String
                  }
                  """,
          )
          .indented(),
        kotlin(
            "test/test/pkg/B.kt",
            """
                  package test.pkg
                  class B : I
                  """,
          )
          .indented(),
        kotlin(
            "test/test/pkg/Test.kt",
            """
                  package test.pkg
                  class Test {
                    fun f(b: B) {
                      b.toString()
                      val i: I = b
                      i.toString()
                    }
                  }
                  """,
          )
          .indented(),
      )
      .run()
      .expect(
        """
          test/test/pkg/Test.kt:4: Error: This method should not be called from Kotlin, see its documentation for details. [JavaOnlyDetector]
              b.toString()
              ~~~~~~~~~~~~
          test/test/pkg/Test.kt:6: Error: This method should not be called from Kotlin, see its documentation for details. [JavaOnlyDetector]
              i.toString()
              ~~~~~~~~~~~~
          2 errors, 0 warnings
        """
          .trimIndent()
      )
  }

  @Test
  fun lambdaPositiveClass() {
    lint()
      .detector(JavaOnlyDetector())
      .files(
        ANNOTATIONS,
        kotlin(
            "test/test/pkg/A.kt",
            """
                  package test.pkg
                  import slack.lint.annotations.JavaOnly
                  @JavaOnly interface A {
                    fun f()
                  }
                  """,
          )
          .indented(),
        kotlin(
            "test/test/pkg/B.kt",
            """
                  package test.pkg
                  class B {
                    fun f(): A = {}
                  }
                  """,
          )
          .indented(),
      )
      .run()
      .expect(
        """
            test/test/pkg/B.kt:3: Error: Cannot create lambda instances of @JavaOnly-annotated type A (in A.kt) in Kotlin. Make a concrete class instead. [JavaOnlyDetector]
              fun f(): A = {}
                           ~~
            1 errors, 0 warnings
        """
          .trimIndent()
      )
  }

  @Test
  fun lambdaPositiveMethod() {
    lint()
      .detector(JavaOnlyDetector())
      .files(
        ANNOTATIONS,
        kotlin(
            "test/test/pkg/A.kt",
            """
                  package test.pkg
                  import slack.lint.annotations.JavaOnly
                  interface A {
                    @JavaOnly fun f()
                  }
                  """,
          )
          .indented(),
        kotlin(
            "test/test/pkg/B.kt",
            """
                  package test.pkg
                  class B {
                    fun f(): A = {}
                  }
                  """,
          )
          .indented(),
      )
      .run()
      .expect(
        """
            test/test/pkg/B.kt:3: Error: This method should not be expressed as a lambda in Kotlin, see its documentation for details. [JavaOnlyDetector]
              fun f(): A = {}
                           ~~
            1 errors, 0 warnings
        """
          .trimIndent()
      )
  }

  @Test
  fun lambdaNegative() {
    lint()
      .detector(JavaOnlyDetector())
      .files(
        ANNOTATIONS,
        kotlin(
            "test/test/pkg/A.kt",
            """
                  package test.pkg
                  interface A {
                    fun f()
                  }
                  """,
          )
          .indented(),
        kotlin(
            "test/test/pkg/B.kt",
            """
                  package test.pkg
                  class B {
                    fun f(): A = {}
                  }
                  """,
          )
          .indented(),
      )
      .run()
      .expectClean()
  }

  @Test
  fun lambdaNegativeReturnFun() {
    lint()
      .detector(JavaOnlyDetector())
      .files(
        ANNOTATIONS,
        kotlin(
            "test/test/pkg/A.kt",
            """
                  package test.pkg
                  import slack.lint.annotations.JavaOnly
                  interface A {
                    @JavaOnly fun f()
                  }
                  """,
          )
          .indented(),
        kotlin(
            "test/test/pkg/B.kt",
            """
                  package test.pkg
                  import slack.lint.annotations.JavaOnly
                  class B {
                    @JavaOnly fun f(): A = {}
                  }
                  """,
          )
          .indented(),
      )
      .run()
      .expectClean()
  }

  @Test
  fun lambdaNegativeReturnClass() {
    lint()
      .detector(JavaOnlyDetector())
      .files(
        ANNOTATIONS,
        kotlin(
            "test/test/pkg/A.kt",
            """
                  package test.pkg
                  import slack.lint.annotations.JavaOnly
                  @JavaOnly interface A {
                    fun f()
                  }
                  """,
          )
          .indented(),
        kotlin(
            "test/test/pkg/B.kt",
            """
                  package test.pkg
                  import slack.lint.annotations.JavaOnly
                  class B {
                    @JavaOnly fun f(): A = {}
                  }
                  """,
          )
          .indented(),
      )
      .run()
      .expectClean()
  }

  @Test
  fun anonymousClassPositiveClass() {
    lint()
      .detector(JavaOnlyDetector())
      .files(
        ANNOTATIONS,
        kotlin(
            "test/test/pkg/A.kt",
            """
                  package test.pkg
                  import slack.lint.annotations.JavaOnly
                  @JavaOnly interface A {
                    fun f()
                  }
                  """,
          )
          .indented(),
        kotlin(
            "test/test/pkg/B.kt",
            """
                  package test.pkg
                  class B {
                    fun f(): A = object : A() {
                      override fun f() {}
                    }
                  }
                  """,
          )
          .indented(),
      )
      .run()
      .expect(
        """
            test/test/pkg/B.kt:3: Error: Cannot create anonymous instances of @JavaOnly-annotated type A (in A.kt) in Kotlin. Make a concrete class instead. [JavaOnlyDetector]
              fun f(): A = object : A() {
                           ^
            1 errors, 0 warnings
        """
          .trimIndent()
      )
  }

  @Test
  fun anonymousClassNegative() {
    lint()
      .detector(JavaOnlyDetector())
      .files(
        ANNOTATIONS,
        kotlin(
            "test/test/pkg/A.kt",
            """
                  package test.pkg
                  interface A {
                    fun f()
                  }
                  """,
          )
          .indented(),
        kotlin(
            "test/test/pkg/B.kt",
            """
                  package test.pkg
                  class B {
                    fun f(): A = object : A() {
                      override fun f() {}
                    }
                  }
                  """,
          )
          .indented(),
      )
      .run()
      .expectClean()
  }

  @Test
  fun anonymousClassNegativeReturnMethod() {
    lint()
      .detector(JavaOnlyDetector())
      .files(
        ANNOTATIONS,
        kotlin(
            "test/test/pkg/A.kt",
            """
                  package test.pkg
                  import slack.lint.annotations.JavaOnly
                  interface A {
                    @JavaOnly fun f()
                  }
                  """,
          )
          .indented(),
        kotlin(
            "test/test/pkg/B.kt",
            """
                  package test.pkg
                  import slack.lint.annotations.JavaOnly
                  class B {
                    @JavaOnly fun f(): A = object : A() {
                      @JavaOnly override fun f() {}
                    }
                  }
                  """,
          )
          .indented(),
      )
      .run()
      .expectClean()
  }

  @Test
  fun anonymousClassNegativeReturnClass() {
    lint()
      .detector(JavaOnlyDetector())
      .files(
        ANNOTATIONS,
        kotlin(
            "test/test/pkg/A.kt",
            """
                  package test.pkg
                  import slack.lint.annotations.JavaOnly
                  @JavaOnly interface A {
                    fun f()
                  }
                  """,
          )
          .indented(),
        kotlin(
            "test/test/pkg/B.kt",
            """
                  package test.pkg
                  import slack.lint.annotations.JavaOnly
                  class B {
                    @JavaOnly fun f(): A = object : A() {
                      override fun f() {}
                    }
                  }
                  """,
          )
          .indented(),
      )
      .run()
      .expectClean()
  }
}
