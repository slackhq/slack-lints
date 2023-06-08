// Copyright Square, Inc.
// Apache-2.0
package slack.lint.denylistedapis

import com.android.tools.lint.checks.infrastructure.TestMode.Companion.FULLY_QUALIFIED
import com.android.tools.lint.checks.infrastructure.TestMode.Companion.IMPORT_ALIAS
import com.android.tools.lint.detector.api.Detector
import org.junit.Ignore
import org.junit.Test
import slack.lint.BaseSlackLintTest

// Adapted from https://gist.github.com/JakeWharton/19b1e7b8d5c648b2935ba89148b791ed
class DenyListedApiDetectorTest : BaseSlackLintTest() {

  override fun getIssues() = listOf(DenyListedApiDetector.ISSUE)

  override fun getDetector(): Detector = DenyListedApiDetector()

  @Test
  fun `flag function with params in deny list`() {
    lint()
      .files(
        CONTEXT_STUB,
        DRAWABLE_STUB,
        CONTEXT_COMPAT_STUB,
        kotlin(
            """
          package foo

          import android.content.Context
          import android.graphics.drawable.Drawable
          import androidx.core.content.ContextCompat

          class SomeView(context: Context) {
            init {
              ContextCompat.getDrawable(context, 42)
            }
          }
          """
          )
          .indented()
      )
      .run()
      .expect(
        """
        src/foo/SomeView.kt:9: Error: Use Context#getDrawableCompat() instead [DenyListedApi]
            ContextCompat.getDrawable(context, 42)
            ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        1 errors, 0 warnings
        """
          .trimIndent()
      )
  }

  @Test
  fun `allow function not in deny list`() {
    lint()
      .files(
        OBSERVABLE_STUB,
        TEST_OBSERVER_STUB,
        RX_RULE_STUB,
        kotlin(
            """
          package cash

          import io.reactivex.rxjava3.core.Observable
          import io.reactivex.rxjava3.observers.TestObserver
          import com.squareup.util.rx3.test.test
          import com.squareup.util.rx3.test.RxRule

          class FooTest {
            @get:Rule val rxRule = RxRule()

            fun test() {
              observable().test(rxRule).assertValue(42)
            }

            fun observable(): Observable<String> = TODO()
          }
          """
          )
          .indented()
      )
      .run()
      .expectClean()
  }

  @Test
  fun `setOnClickListener with null argument in deny list`() {
    lint()
      .files(
        VIEW_STUB,
        kotlin(
            """
          package foo

          import android.view.View;

          class SomeView(view: View) {
            init {
              view.setOnClickListener(null);
            }
          }
          """
          )
          .indented()
      )
      .run()
      .expect(
        """
        src/foo/SomeView.kt:7: Error: This fails to also set View#isClickable. Use View#clearOnClickListener() instead [DenyListedApi]
            view.setOnClickListener(null);
            ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        1 errors, 0 warnings
        """
          .trimIndent()
      )
  }

  @Test
  fun `setOnClickListener with non-null argument not in deny list`() {
    lint()
      .files(
        VIEW_STUB,
        kotlin(
            """
          package foo

          import android.view.View;

          class SomeView(view: View) {
            init {
              view.setOnClickListener {
                // do something
              }
            }
          }
          """
          )
          .indented()
      )
      .run()
      .expectClean()
  }

  @Test
  fun `setId with explicit id not in deny list`() {
    lint()
      .files(
        VIEWPAGER2_STUB,
        kotlin(
            """
          package foo

          import androidx.viewpager2.widget.ViewPager2;

          class SomeView(view: ViewPager2) {
            init {
              view.setId(1)
            }
          }
          """
          )
          .indented()
      )
      .run()
      .expectClean()
  }

  @Test
  fun `setId with ViewCompat#generateViewId() in deny list`() {
    lint()
      .files(
        VIEWCOMPAT_STUB,
        VIEWPAGER2_STUB,
        kotlin(
            """
          package foo

          import androidx.viewpager2.widget.ViewPager2;
          import androidx.core.view.ViewCompat;

          class SomeView(view: ViewPager2) {
            init {
              view.setId(ViewCompat.generateViewId())
            }
          }
          """
          )
          .indented()
      )
      .skipTestModes(FULLY_QUALIFIED, IMPORT_ALIAS) // TODO relies on non-qualified matching.
      .run()
      .expect(
        """
        src/foo/SomeView.kt:8: Error: Use an id defined in resources or a statically created instead of generating with ViewCompat.generateViewId(). See https://issuetracker.google.com/issues/185820237 [DenyListedApi]
            view.setId(ViewCompat.generateViewId())
            ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        1 errors, 0 warnings
        """
          .trimIndent()
      )
  }

  @Test
  fun `setId with View#generateViewId() in deny list`() {
    lint()
      .files(
        VIEW_STUB,
        VIEWPAGER2_STUB,
        kotlin(
            """
          package foo

          import androidx.viewpager2.widget.ViewPager2;
          import android.view.View;

          class SomeView(view: ViewPager2) {
            init {
              view.setId(View.generateViewId())
            }
          }
          """
          )
          .indented()
      )
      .skipTestModes(FULLY_QUALIFIED, IMPORT_ALIAS) // TODO relies on non-qualified matching.
      .run()
      .expect(
        """
        src/foo/SomeView.kt:8: Error: Use an id defined in resources or a statically created instead of generating with View.generateViewId(). See https://issuetracker.google.com/issues/185820237 [DenyListedApi]
            view.setId(View.generateViewId())
            ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        1 errors, 0 warnings
        """
          .trimIndent()
      )
  }

  @Test
  fun errorLinkedList() {
    lint()
      .files(
        kotlin(
            """
          package foo

          import java.util.LinkedList

          class SomeClass {
            val stuff = LinkedList<String>()
          }
          """
          )
          .indented()
      )
      .run()
      .expect(
        """
        src/foo/SomeClass.kt:6: Error: For a stack/queue/double-ended queue use ArrayDeque, for a list use ArrayList. Both are more efficient internally. [DenyListedApi]
          val stuff = LinkedList<String>()
                      ~~~~~~~~~~~~~~~~~~~~
        1 errors, 0 warnings
        """
          .trimIndent()
      )
  }

  @Test
  fun errorStack() {
    lint()
      .files(
        kotlin(
            """
          package foo

          import java.util.Stack

          class SomeClass {
            val stuff = Stack<String>()
          }
          """
          )
          .indented()
      )
      .run()
      .expect(
        """
        src/foo/SomeClass.kt:6: Error: For a stack use ArrayDeque which is more efficient internally. [DenyListedApi]
          val stuff = Stack<String>()
                      ~~~~~~~~~~~~~~~
        1 errors, 0 warnings
        """
          .trimIndent()
      )
  }

  @Test
  fun errorVector() {
    lint()
      .files(
        kotlin(
            """
          package foo

          import java.util.Vector

          class SomeClass {
            val stuff = Vector<String>()
          }
          """
          )
          .indented()
      )
      .run()
      .expect(
        """
        src/foo/SomeClass.kt:6: Error: For a vector use ArrayList or ArrayDeque which are more efficient internally. [DenyListedApi]
          val stuff = Vector<String>()
                      ~~~~~~~~~~~~~~~~
        1 errors, 0 warnings
        """
          .trimIndent()
      )
  }

  @Test
  fun schedulersNewThread() {
    lint()
      .files(
        SCHEDULERS_STUB,
        kotlin(
            """
          package foo

          import io.reactivex.rxjava3.schedulers.Schedulers

          class SomeClass {
            val scheduler = Schedulers.newThread()
          }
          """
          )
          .indented()
      )
      .run()
      .expect(
        """
        src/foo/SomeClass.kt:6: Error: Use a scheduler which wraps a cached set of threads. There should be no reason to be arbitrarily creating threads on Android. [DenyListedApi]
          val scheduler = Schedulers.newThread()
                          ~~~~~~~~~~~~~~~~~~~~~~
        1 errors, 0 warnings
        """
          .trimIndent()
      )
  }

  @Ignore("Revisiting after we look more into how this would conflict with MagicNumber")
  @Test
  fun buildVersionCodes() {
    lint()
      .files(
        BUILD_STUB,
        kotlin(
            """
          package foo

          import android.os.Build
          import android.os.Build.VERSION_CODES
          import android.os.Build.VERSION_CODES.S

          class SomeClass {
            val p = android.os.Build.VERSION_CODES.P
            val q = Build.VERSION_CODES.Q
            val r = VERSION_CODES.R
            val s = S
          }
          """
          )
          .indented()
      )
      .run()
      .expect(
        """
        src/foo/SomeClass.kt:5: Error: No one remembers what these constants map to. Use the API level integer value directly since it's self-defining. [DenyListedApi]
        import android.os.Build.VERSION_CODES.S
        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        src/foo/SomeClass.kt:8: Error: No one remembers what these constants map to. Use the API level integer value directly since it's self-defining. [DenyListedApi]
          val p = android.os.Build.VERSION_CODES.P
                  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        src/foo/SomeClass.kt:9: Error: No one remembers what these constants map to. Use the API level integer value directly since it's self-defining. [DenyListedApi]
          val q = Build.VERSION_CODES.Q
                  ~~~~~~~~~~~~~~~~~~~~~
        src/foo/SomeClass.kt:10: Error: No one remembers what these constants map to. Use the API level integer value directly since it's self-defining. [DenyListedApi]
          val r = VERSION_CODES.R
                  ~~~~~~~~~~~~~~~
        4 errors, 0 warnings
        """
          .trimIndent()
      )
  }

  @Ignore("Not enabled currently")
  @Test
  fun javaTimeInstantNow() {
    lint()
      .files(
        kotlin(
            """
          package foo

          import java.time.Instant

          class SomeClass {
            val now = Instant.now()
          }
          """
          )
          .indented()
      )
      .run()
      .expect(
        """
        src/foo/SomeClass.kt:6: Error: Use com.squareup.cash.util.Clock to get the time. [DenyListedApi]
          val now = Instant.now()
                    ~~~~~~~~~~~~~
        1 errors, 0 warnings
        """
          .trimIndent()
      )
  }

  @Test
  fun rxCompletableParameterless() {
    lint()
      .files(
        COROUTINE_SCOPE_STUB,
        COMPLETABLE_STUB,
        RX_COMPLETABLE_STUB,
        kotlin(
            """
          package foo

          import kotlinx.coroutines.rx3.rxCompletable

          class SomeClass {
            val now = rxCompletable {}
          }
          """
          )
          .indented()
      )
      .run()
      .expect(
        """
        src/foo/SomeClass.kt:6: Error: rxCompletable defaults to Dispatchers.Default, which will silently introduce multithreading. Provide an explicit dispatcher. Dispatchers.Unconfined is usually the best choice, as it behaves in an rx-y way. [DenyListedApi]
          val now = rxCompletable {}
                    ~~~~~~~~~~~~~~~~
        1 errors, 0 warnings
        """
          .trimIndent()
      )
  }

  @Test
  fun rxSingleParameterless() {
    lint()
      .files(
        COROUTINE_SCOPE_STUB,
        SINGLE_STUB,
        RX_SINGLE_STUB,
        kotlin(
            """
          package foo

          import kotlinx.coroutines.rx3.rxSingle

          class SomeClass {
            val now = rxSingle { "a" }
          }
          """
          )
          .indented()
      )
      .run()
      .expect(
        """
        src/foo/SomeClass.kt:6: Error: rxSingle defaults to Dispatchers.Default, which will silently introduce multithreading. Provide an explicit dispatcher. Dispatchers.Unconfined is usually the best choice, as it behaves in an rx-y way. [DenyListedApi]
          val now = rxSingle { "a" }
                    ~~~~~~~~~~~~~~~~
        1 errors, 0 warnings
        """
          .trimIndent()
      )
  }

  @Test
  fun rxMaybeParameterless() {
    lint()
      .files(
        COROUTINE_SCOPE_STUB,
        MAYBE_STUB,
        RX_MAYBE_STUB,
        kotlin(
            """
          package foo

          import kotlinx.coroutines.rx3.rxMaybe

          class SomeClass {
            val now = rxMaybe { "a" }
          }
          """
          )
          .indented()
      )
      .run()
      .expect(
        """
        src/foo/SomeClass.kt:6: Error: rxMaybe defaults to Dispatchers.Default, which will silently introduce multithreading. Provide an explicit dispatcher. Dispatchers.Unconfined is usually the best choice, as it behaves in an rx-y way. [DenyListedApi]
          val now = rxMaybe { "a" }
                    ~~~~~~~~~~~~~~~
        1 errors, 0 warnings
        """
          .trimIndent()
      )
  }

  @Test
  fun rxObservableParameterless() {
    lint()
      .files(
        COROUTINE_SCOPE_STUB,
        TEST_OBSERVER_STUB,
        OBSERVABLE_STUB,
        PRODUCER_STUB,
        RX_OBSERVABLE_STUB,
        kotlin(
            """
          package foo

          import kotlinx.coroutines.rx3.rxObservable

          class SomeClass {
            val now = rxObservable { send("a") }
          }
          """
          )
          .indented()
      )
      .run()
      .expect(
        """
        src/foo/SomeClass.kt:6: Error: rxObservable defaults to Dispatchers.Default, which will silently introduce multithreading. Provide an explicit dispatcher. Dispatchers.Unconfined is usually the best choice, as it behaves in an rx-y way. [DenyListedApi]
          val now = rxObservable { send("a") }
                    ~~~~~~~~~~~~~~~~~~~~~~~~~~
        1 errors, 0 warnings
        """
          .trimIndent()
      )
  }

  @Test
  fun rxCompletableWithParameters() {
    lint()
      .files(
        COROUTINE_SCOPE_STUB,
        COMPLETABLE_STUB,
        RX_COMPLETABLE_STUB,
        kotlin(
            """
          package foo

          import kotlinx.coroutines.rx3.rxCompletable

          object MyDispatcher : CoroutineContext

          class SomeClass {
            val now = rxCompletable(MyDispatcher) {}
          }
          """
          )
          .indented()
      )
      .run()
      .expectClean()
  }

  companion object {
    private val OBSERVABLE_STUB =
      java(
          """
        package io.reactivex.rxjava3.core;

        import io.reactivex.rxjava3.observers.TestObserver;

        public abstract class Observable<T> {
          public static <T> Observable<T> just(T item) {}
          public final TestObserver<T> test() {}
          public final TestObserver<T> test(boolean dispose) {}
        }
      """
        )
        .indented()

    private val TEST_OBSERVER_STUB =
      java(
          """
        package io.reactivex.rxjava3.observers;

        public class TestObserver<T> {
          public final assertValue(T value) {}
        }
      """
        )
        .indented()

    private val RX_RULE_STUB =
      kotlin(
          """
        package com.squareup.util.rx3.test

        import io.reactivex.rxjava3.core.Observable

        class RxRule {
          fun <T : Any> newObserver(): RecordingObserver<T> = TODO()
        }

        fun <T: Any> Observable<T>.test(rxRule: RxRule): RecordingObserver<T>
      """
        )
        .indented()

    private val CONTEXT_COMPAT_STUB =
      java(
          """
        package androidx.core.content;

        import android.graphics.drawable.Drawable;
        import android.content.Context;

        public class ContextCompat {
          public static Drawable getDrawable(Context context, int id) {}
        }
      """
        )
        .indented()

    private val DRAWABLE_STUB =
      java(
          """
        package android.graphics.drawable;

        public class Drawable {}
      """
        )
        .indented()

    private val CONTEXT_STUB =
      java("""
        package android.content;

        public class Context {}
      """)
        .indented()

    private val VIEW_STUB =
      java(
          """
        package android.view;

        public class View {

          public static int generateViewId() { return 0; }

          public void setOnClickListener(View.OnClickListener l) {}

          public interface OnClickListener {}
        }
      """
        )
        .indented()

    private val VIEWCOMPAT_STUB =
      java(
          """
        package androidx.core.view;

        public class ViewCompat {
          public static int generateViewId() { return 0; }
        }
      """
        )
        .indented()

    private val VIEWPAGER2_STUB =
      java(
          """
        package androidx.viewpager2.widget;

        public class ViewPager2 {
          public void setId(int id) {}
        }
      """
        )
        .indented()

    private val SCHEDULERS_STUB =
      java(
          """
        package io.reactivex.rxjava3.schedulers;

        public final class Schedulers {
          public static Object newThread() {
            return null;
          }
        }
      """
        )
        .indented()

    private val COROUTINE_SCOPE_STUB =
      kotlin("""
        package kotlinx.coroutines

        interface CoroutineScope
      """)
        .indented()
    private val COMPLETABLE_STUB =
      java(
          """
        package io.reactivex.rxjava3.core;

        public final class Completable {
          Completable() {}
        }
      """
        )
        .indented()

    private val RX_COMPLETABLE_STUB =
      kotlin(
          """
        package kotlinx.coroutines.rx3

        import kotlin.coroutines.CoroutineContext
        import kotlin.coroutines.EmptyCoroutineContext
        import kotlinx.coroutines.CoroutineScope

        class RxCompletable {}

        public fun rxCompletable(
            context: CoroutineContext = EmptyCoroutineContext,
            block: suspend CoroutineScope.() -> Unit
        ): Completable {
          return Completable
        }
      """
        )
        .indented()

    private val SINGLE_STUB =
      java(
          """
        package io.reactivex.rxjava3.core;

        public final class Single<T> {
          Single() {}
        }
      """
        )
        .indented()

    private val RX_SINGLE_STUB =
      kotlin(
          """
        package kotlinx.coroutines.rx3

        import kotlin.coroutines.CoroutineContext
        import kotlin.coroutines.EmptyCoroutineContext
        import kotlinx.coroutines.CoroutineScope

        class RxSingle {}

        public fun <T> rxSingle(
            context: CoroutineContext = EmptyCoroutineContext,
            block: suspend CoroutineScope.() -> T
        ): Single<T> {
          return Single<T>()
        }
      """
        )
        .indented()

    private val MAYBE_STUB =
      java(
          """
        package io.reactivex.rxjava3.core;

        public final class Maybe<T> {
          Maybe() {}
        }
      """
        )
        .indented()

    private val RX_MAYBE_STUB =
      kotlin(
          """
        package kotlinx.coroutines.rx3

        import kotlin.coroutines.CoroutineContext
        import kotlin.coroutines.EmptyCoroutineContext
        import kotlinx.coroutines.CoroutineScope

        class rxMaybe {}

        public fun <T> rxMaybe(
            context: CoroutineContext = EmptyCoroutineContext,
            block: suspend CoroutineScope.() -> T?
        ): Maybe<T> {
          return Maybe<T>()
        }
      """
        )
        .indented()

    private val PRODUCER_STUB =
      kotlin(
          """
        package kotlinx.coroutines.channels

        object ProducerScope<T> {
          suspend fun send(value: T)
        }
      """
        )
        .indented()

    private val RX_OBSERVABLE_STUB =
      kotlin(
          """
        package kotlinx.coroutines.rx3

        import kotlin.coroutines.CoroutineContext
        import kotlin.coroutines.EmptyCoroutineContext
        import kotlinx.coroutines.CoroutineScope
        import kotlinx.coroutines.channels.ProducerScope

        class RxObservable {}

        public fun <T> rxObservable(
            context: CoroutineContext = EmptyCoroutineContext,
            block: suspend ProducerScope<T>.() -> Unit
        ): Observable<T> {
          return Observable<T>()
        }
      """
        )
        .indented()

    private val BUILD_STUB =
      java(
          """
        package android.os;

        public final class Build {
          public static final class VERSION_CODES {
            public static final int P = 28;
            public static final int Q = 29;
            public static final int R = 30;
            public static final int S = 31;
          }
        }
      """
        )
        .indented()
  }
}
