// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint.rx

import com.android.tools.lint.checks.infrastructure.TestFile
import org.junit.Test
import slack.lint.BaseSlackLintTest

class RxObservableEmitDetectorTest : BaseSlackLintTest() {
  override fun getDetector() = RxObservableEmitDetector()

  override fun getIssues() = RxObservableEmitDetector.issues

  @Test
  fun `rxObservable - does not call send or trySend`() {
    lint()
      .files(
        *files,
        kotlin(
            """
                    package test

                    import kotlinx.coroutines.rx3.rxObservable

                    class Foo {
                      fun foo() {
                        rxObservable { println("foo") }
                      }
                    }
                    """
          )
          .indented(),
      )
      .run()
      .expect(
        """
                src/test/Foo.kt:7: Hint: rxObservable does not call send() or trySend() [RxObservableDoesNotEmit]
                    rxObservable { println("foo") }
                    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                0 errors, 0 warnings, 1 hint
                """
          .trimIndent()
      )
  }

  @Test
  fun `rxFlowable - does not call send or trySend`() {
    lint()
      .files(
        *files,
        kotlin(
            """
                    package test

                    import kotlinx.coroutines.rx3.rxFlowable

                    class Foo {
                      fun foo() {
                        rxFlowable { println("foo") }
                      }
                    }
                    """
          )
          .indented(),
      )
      .run()
      .expect(
        """
                src/test/Foo.kt:7: Hint: rxFlowable does not call send() or trySend() [RxFlowableDoesNotEmit]
                    rxFlowable { println("foo") }
                    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                0 errors, 0 warnings, 1 hint
                """
          .trimIndent()
      )
  }

  @Test
  fun `rxObservable - calls send`() {
    testWhenResultsOfSendAreReturned(RX_OBSERVABLE, SEND)
  }

  @Test
  fun `rxObservable - calls trySend`() {
    testWhenResultsOfSendAreReturned(RX_OBSERVABLE, TRY_SEND)
  }

  @Test
  fun `rxFlowable - calls send`() {
    testWhenResultsOfSendAreReturned("rxFlowable", SEND)
  }

  @Test
  fun `rxFlowable - calls trySend`() {
    testWhenResultsOfSendAreReturned("rxFlowable", TRY_SEND)
  }

  @Test
  fun `rxObservable - results of send are not the lambda return value`() {
    testWhenResultsOfSendAreNotReturned(RX_OBSERVABLE, SEND)
  }

  @Test
  fun `rxObservable - results of trySend are not the lambda return value`() {
    testWhenResultsOfSendAreNotReturned(RX_OBSERVABLE, TRY_SEND)
  }

  @Test
  fun `rxFlowable - results of send are not the lambda return value`() {
    testWhenResultsOfSendAreNotReturned("rxFlowable", SEND)
  }

  @Test
  fun `rxFlowable - results of trySend are not the lambda return value`() {
    testWhenResultsOfSendAreNotReturned("rxFlowable", TRY_SEND)
  }

  private fun testWhenResultsOfSendAreReturned(method: String, emitter: String) {
    lint()
      .files(
        *files,
        kotlin(
            """
                    package test

                    import kotlinx.coroutines.rx3.$method

                    class Foo {
                      fun foo() {
                        $method { $emitter("foo") }
                      }
                    }
                    """
          )
          .indented(),
      )
      .run()
      .expectClean()
  }

  private fun testWhenResultsOfSendAreNotReturned(method: String, emitter: String) {
    lint()
      .files(
        *files,
        kotlin(
            """
                    package test

                    import kotlinx.coroutines.rx3.$method

                    class Foo {
                      fun foo() {
                        $method {
                          $emitter("foo")
                          println("bar")
                        }
                      }
                    }
                    """
          )
          .indented(),
      )
      .run()
      .expectClean()
  }

  private companion object {
    const val RX_OBSERVABLE = "rxObservable"
    const val RX_FLOWABLE = "rxFlowable"
    const val SEND = "send"
    const val TRY_SEND = "trySend"

    val COROUTINE_CONTEXT_API: TestFile =
      kotlin(
          """
          package kotlin.coroutines

          interface CoroutineContext
          """
        )
        .indented()

    val PRODUCER_SCOPE_API: TestFile =
      kotlin(
          """
          package kotlin.coroutines

          class ChannelResult<T>

          interface ProducerScope<in E> {
              suspend fun send(element: E)
              fun trySend(element: E): ChannelResult<Unit>
          }
          """
        )
        .indented()

    val RX_OBSERVABLE_API: TestFile =
      kotlin(
          """
            package kotlinx.coroutines.rx3

            import io.reactivex.rxjava3.core.Observable
            import kotlin.coroutines.CoroutineContext
            import kotlin.coroutines.ProducerScope

            fun <T : Any> rxObservable(
              context: CoroutineContext,
              block: suspend ProducerScope<T>.() -> Unit
            ): Observable<T>
          """
        )
        .indented()

    val RX_FLOWABLE_API: TestFile =
      kotlin(
          """
            package kotlinx.coroutines.rx3

            import io.reactivex.rxjava3.core.Flowable
            import kotlin.coroutines.CoroutineContext
            import kotlin.coroutines.ProducerScope

            fun <T : Any> rxFlowable(
              context: CoroutineContext,
              block: suspend ProducerScope<T>.() -> Unit
            ): Flowable<T>
          """
        )
        .indented()

    val OBSERVABLE_API: TestFile =
      kotlin(
          """
            package io.reactivex.rxjava3.core

            import io.reactivex.rxjava4.annotations.NonNull

            interface Observable<@NonNull T>
            """
        )
        .indented()

    val FLOWABLE_API: TestFile =
      kotlin(
          """
            package io.reactivex.rxjava3.core

            import io.reactivex.rxjava4.annotations.NonNull

            interface Flowable<@NonNull T>
            """
        )
        .indented()

    val NON_NULL_API: TestFile =
      kotlin(
          """
            package io.reactivex.rxjava4.annotations

            annotation class NonNull
            """
        )
        .indented()

    val files =
      arrayOf(
        COROUTINE_CONTEXT_API,
        PRODUCER_SCOPE_API,
        RX_OBSERVABLE_API,
        RX_FLOWABLE_API,
        OBSERVABLE_API,
        FLOWABLE_API,
        NON_NULL_API,
      )
  }
}
