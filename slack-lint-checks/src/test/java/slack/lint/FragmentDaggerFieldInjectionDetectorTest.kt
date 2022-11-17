// Copyright (C) 2021 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
@file:Suppress("UnstableApiUsage")

package slack.lint

import org.junit.Test

class FragmentDaggerFieldInjectionDetectorTest : BaseSlackLintTest() {

  private val javaxInjectStubs =
    kotlin("""
        package javax.inject

        annotation class Inject
      """).indented()

  private val assistedInjectStubs =
    kotlin("""
      package dagger.assisted

      annotation class AssistedInject
      """)
      .indented()

  private val topLevelFragment =
    java(
        """
      package androidx.fragment.app;

      public abstract class Fragment {
      }
    """
      )
      .indented()

  private val coreUiAbstractFragment =
    kotlin(
        """
      package slack.coreui.fragment

      import androidx.fragment.app.Fragment

      abstract class ViewBindingFragment: Fragment() {

      }
    """
      )
      .indented()

  override fun getDetector() = FragmentDaggerFieldInjectionDetector()
  override fun getIssues() = FragmentDaggerFieldInjectionDetector.issues.toList()

  @Test
  fun `Kotlin - fragment has field injection warnings`() {
    lint()
      .files(
        javaxInjectStubs,
        assistedInjectStubs,
        topLevelFragment,
        coreUiAbstractFragment,
        kotlin(
            """
            package foo

            import javax.inject.Inject
            import slack.coreui.fragment.ViewBindingFragment

            class MyFragment : ViewBindingFragment() {

              private lateinit var notAnnotated: String
              private val defaulted: String = "defaulted"

              @Inject
              private lateinit var stringValue1: String
              @Inject
              private lateinit var intValue1: Int

              fun onCreate() {
                notAnnotated = "fast"
              }
            }
          """
              .trimIndent()
          )
          .indented()
      )
      .allowCompilationErrors(false)
      .run()
      .expect(
        """
          src/foo/MyFragment.kt:11: Error: Fragment dependencies should be injected using the Fragment's constructor. [FragmentFieldInjection]
            @Inject
            ^
          src/foo/MyFragment.kt:13: Error: Fragment dependencies should be injected using the Fragment's constructor. [FragmentFieldInjection]
            @Inject
            ^
          2 errors, 0 warnings
        """
          .trimIndent()
      )
  }

  @Test
  fun `Java - fragment has field injection warnings`() {
    lint()
      .files(
        javaxInjectStubs,
        assistedInjectStubs,
        topLevelFragment,
        coreUiAbstractFragment,
        java(
            """
            package foo;

            import javax.inject.Inject;
            import slack.coreui.fragment.ViewBindingFragment;

            public class MyFragment extends ViewBindingFragment {

              private static String notAnnotated;
              private final String defaulted = "defaulted";

              @Inject
              String stringValue1;
              @Inject
              Int intValue1;

              public void onCreate() {
                notAnnotated = "fast";
              }
            }
          """
              .trimIndent()
          )
          .indented()
      )
      .allowCompilationErrors(false)
      .run()
      .expect(
        """
          src/foo/MyFragment.java:11: Error: Fragment dependencies should be injected using the Fragment's constructor. [FragmentFieldInjection]
            @Inject
            ^
          src/foo/MyFragment.java:13: Error: Fragment dependencies should be injected using the Fragment's constructor. [FragmentFieldInjection]
            @Inject
            ^
          2 errors, 0 warnings
        """
          .trimIndent()
      )
  }

  @Test
  fun `Kotlin - fragment has field injection errors when constructor injection exists`() {
    lint()
      .files(
        javaxInjectStubs,
        assistedInjectStubs,
        topLevelFragment,
        coreUiAbstractFragment,
        kotlin(
            """
            package foo

            import javax.inject.Inject
            import dagger.assisted.AssistedInject
            import slack.coreui.fragment.ViewBindingFragment

            class MyFragment @Inject constructor(
              private val flag: Boolean
            ): ViewBindingFragment() {

              private lateinit var notAnnotated: String
              private val defaulted: String = "defaulted"

              @Inject
              private lateinit var stringValue1: String
              @Inject
              private lateinit var intValue1: Int

              fun onCreate() {
                notAnnotated = "fast"
              }
            }

            class MyFragmentAssistedInject @AssistedInject constructor(
              private val flag: Boolean
            ): ViewBindingFragment() {

              private lateinit var notAnnotated: String
              private val defaulted: String = "defaulted"

              @Inject
              private lateinit var stringValue1: String
              @Inject
              private lateinit var intValue1: Int

              fun onCreate() {
                notAnnotated = "fast"
              }
            }
          """
              .trimIndent()
          )
          .indented()
      )
      .allowCompilationErrors(false)
      .run()
      .expect(
        """
          src/foo/MyFragment.kt:14: Error: Fragment dependencies should be injected using constructor injections only. [FragmentConstructorInjection]
            @Inject
            ^
          src/foo/MyFragment.kt:16: Error: Fragment dependencies should be injected using constructor injections only. [FragmentConstructorInjection]
            @Inject
            ^
          src/foo/MyFragment.kt:31: Error: Fragment dependencies should be injected using constructor injections only. [FragmentConstructorInjection]
            @Inject
            ^
          src/foo/MyFragment.kt:33: Error: Fragment dependencies should be injected using constructor injections only. [FragmentConstructorInjection]
            @Inject
            ^
          4 errors, 0 warnings
        """
          .trimIndent()
      )
  }

  @Test
  fun `Java - fragment has field injection errors when constructor injection with basic Inject exists`() {
    lint()
      .files(
        javaxInjectStubs,
        assistedInjectStubs,
        topLevelFragment,
        coreUiAbstractFragment,
        java(
            """
            package foo;

            import javax.inject.Inject;
            import slack.coreui.fragment.ViewBindingFragment;

            public class MyFragment extends ViewBindingFragment {

              private static String notAnnotated;
              private final String defaulted = "defaulted";

              @Inject
              String stringValue1;
              @Inject
              Int intValue1;

              @Inject
              public MyFragment(Boolean flag) {

              }

              public void onCreate() {
                notAnnotated = "fast";
              }
            }
          """
              .trimIndent()
          )
          .indented()
      )
      .allowCompilationErrors(false)
      .run()
      .expect(
        """
          src/foo/MyFragment.java:11: Error: Fragment dependencies should be injected using constructor injections only. [FragmentConstructorInjection]
            @Inject
            ^
          src/foo/MyFragment.java:13: Error: Fragment dependencies should be injected using constructor injections only. [FragmentConstructorInjection]
            @Inject
            ^
          2 errors, 0 warnings
        """
          .trimIndent()
      )
  }

  @Test
  fun `Java - fragment has field injection errors when constructor injection with AssistedInject exists`() {
    lint()
      .files(
        javaxInjectStubs,
        assistedInjectStubs,
        topLevelFragment,
        coreUiAbstractFragment,
        java(
            """
            package foo;

            import javax.inject.Inject;
            import dagger.assisted.AssistedInject;
            import slack.coreui.fragment.ViewBindingFragment;

            public class MyFragment extends ViewBindingFragment {

              private static String notAnnotated;
              private final String defaulted = "defaulted";

              @Inject
              String stringValue1;
              @Inject
              Int intValue1;

              @AssistedInject
              public MyFragment(Boolean flag) {

              }

              public void onCreate() {
                notAnnotated = "fast";
              }
            }
          """
              .trimIndent()
          )
          .indented()
      )
      .allowCompilationErrors(false)
      .run()
      .expect(
        """
          src/foo/MyFragment.java:12: Error: Fragment dependencies should be injected using constructor injections only. [FragmentConstructorInjection]
            @Inject
            ^
          src/foo/MyFragment.java:14: Error: Fragment dependencies should be injected using constructor injections only. [FragmentConstructorInjection]
            @Inject
            ^
          2 errors, 0 warnings
        """
          .trimIndent()
      )
  }

  @Test
  fun `Kotlin - abstract fragment does not get warnings`() {
    lint()
      .files(
        javaxInjectStubs,
        assistedInjectStubs,
        topLevelFragment,
        coreUiAbstractFragment,
        kotlin(
            """
            package foo

            import javax.inject.Inject
            import androidx.fragment.app.Fragment

            abstract class MyFragment : Fragment() {

              private lateinit var notAnnotated: String
              private val defaulted: String = "defaulted"

              @Inject
              private lateinit var stringValue1: String
              @Inject
              private lateinit var intValue1: Int

              fun onCreate() {
                notAnnotated = "fast"
              }
            }
          """
              .trimIndent()
          )
          .indented()
      )
      .allowCompilationErrors(false)
      .run()
      .expect(
        """
          src/foo/MyFragment.kt:11: Error: Fragment dependencies should be injected using the Fragment's constructor. [FragmentFieldInjection]
            @Inject
            ^
          src/foo/MyFragment.kt:13: Error: Fragment dependencies should be injected using the Fragment's constructor. [FragmentFieldInjection]
            @Inject
            ^
          2 errors, 0 warnings
        """
          .trimIndent()
      )
  }

  @Test
  fun `Kotlin - non-fragment class also get warnings`() {
    lint()
      .files(
        javaxInjectStubs,
        kotlin(
            """
            package foo

            import javax.inject.Inject

            class NonFragment {

              private lateinit var notAnnotated: String
              private val defaulted: String = "defaulted"

              @Inject
              private lateinit var stringValue1: String
              @Inject
              private lateinit var intValue1: Int

              fun onCreate() {
                notAnnotated = "fast"
              }
            }
                """
          )
          .indented()
      )
      .allowCompilationErrors(false)
      .run()
      .expectClean()
  }

  @Test
  fun `Java - abstract fragment does not get warnings`() {
    lint()
      .files(
        javaxInjectStubs,
        assistedInjectStubs,
        topLevelFragment,
        coreUiAbstractFragment,
        java(
            """
            package foo;

            import javax.inject.Inject;
            import dagger.assisted.AssistedInject;
            import slack.coreui.fragment.ViewBindingFragment;

            public abstract class MyFragment extends ViewBindingFragment {

              private static String notAnnotated;
              private final String defaulted = "defaulted";

              @Inject
              String stringValue1;
              @Inject
              Int intValue1;

              public void onCreate() {
                notAnnotated = "fast";
              }
            }
          """
              .trimIndent()
          )
          .indented()
      )
      .allowCompilationErrors(false)
      .run()
      .expect(
        """
          src/foo/MyFragment.java:12: Error: Fragment dependencies should be injected using the Fragment's constructor. [FragmentFieldInjection]
            @Inject
            ^
          src/foo/MyFragment.java:14: Error: Fragment dependencies should be injected using the Fragment's constructor. [FragmentFieldInjection]
            @Inject
            ^
          2 errors, 0 warnings
        """
          .trimIndent()
      )
  }

  @Test
  fun `Java - non-fragment does not get warnings`() {
    lint()
      .files(
        javaxInjectStubs,
        assistedInjectStubs,
        topLevelFragment,
        coreUiAbstractFragment,
        java(
            """
            package foo;

            import javax.inject.Inject;
            import dagger.assisted.AssistedInject;

            public class MyFragment {

              private static String notAnnotated;
              private final String defaulted = "defaulted";

              @Inject
              String stringValue1;
              @Inject
              Int intValue1;

              public void onCreate() {
                notAnnotated = "fast";
              }
            }
          """
              .trimIndent()
          )
          .indented()
      )
      .allowCompilationErrors(false)
      .run()
      .expectClean()
  }
}
