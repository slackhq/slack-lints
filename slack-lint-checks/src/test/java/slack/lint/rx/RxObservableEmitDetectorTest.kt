// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint.rx

import com.android.tools.lint.checks.infrastructure.TestFile
import java.util.Locale.getDefault
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

    @Suppress("LintDocExample")
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

  @Test
  fun `rxObservable - should fail when outer factor does not call send`() {
    testWhenOuterFactoryDoesNotCallSend(RX_OBSERVABLE, SEND)
  }

  @Test
  fun `rxObservable - should fail when outer factory does not call trySend`() {
    testWhenOuterFactoryDoesNotCallSend(RX_OBSERVABLE, TRY_SEND)
  }

  @Test
  fun `rxFlowable - should fail when outer factor does not call send`() {
    testWhenOuterFactoryDoesNotCallSend(RX_FLOWABLE, SEND)
  }

  @Test
  fun `rxFlowable - should fail when outer factory does not call trySend`() {
    testWhenOuterFactoryDoesNotCallSend(RX_FLOWABLE, TRY_SEND)
  }

  private fun testWhenOuterFactoryDoesNotCallSend(method: String, emitter: String) {
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
                    $method {
                        $emitter("foo")
                    }
                }
              }
            }
            """
          )
          .indented(),
      )
      .run()
      .expect(
        """
            src/test/Foo.kt:7: Hint: $method does not call send() or trySend() [${getError(method)}]
                $method {
                ^
            0 errors, 0 warnings, 1 hint
            """
          .trimIndent()
      )
  }

  @Test
  fun `rxObservable - should fail when inner factor does not call send`() {
    testWhenInnerFactoryDoesNotCallSend(RX_OBSERVABLE, SEND)
  }

  @Test
  fun `rxObservable - should fail when inner factory does not call trySend`() {
    testWhenInnerFactoryDoesNotCallSend(RX_OBSERVABLE, TRY_SEND)
  }

  @Test
  fun `rxFlowable - should fail when inner factor does not call send`() {
    testWhenInnerFactoryDoesNotCallSend(RX_FLOWABLE, SEND)
  }

  @Test
  fun `rxFlowable - should fail when inner factory does not call trySend`() {
    testWhenInnerFactoryDoesNotCallSend(RX_FLOWABLE, TRY_SEND)
  }

  private fun testWhenInnerFactoryDoesNotCallSend(method: String, emitter: String) {
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
                    $method {
                      println("bar!")
                    }
                }
              }
            }
            """
          )
          .indented(),
      )
      .run()
      .expect(
        """
            src/test/Foo.kt:9: Hint: $method does not call send() or trySend() [${getError(method)}]
                    $method {
                    ^
            0 errors, 0 warnings, 1 hint
            """
          .trimIndent()
      )
  }

  @Test
  fun `rxObservable - should succeed when inner and outer call send`() {
    testWhenInnerAndOuterFactoriesCallSend(RX_OBSERVABLE, SEND)
  }

  @Test
  fun `rxObservable - should succeed when inner and outer call trySend`() {
    testWhenInnerAndOuterFactoriesCallSend(RX_OBSERVABLE, TRY_SEND)
  }

  @Test
  fun `rxFlowable - should succeed when inner and outer call send`() {
    testWhenInnerAndOuterFactoriesCallSend(RX_FLOWABLE, SEND)
  }

  @Test
  fun `rxFlowable - should succeed when inner and outer call trySend`() {
    testWhenInnerAndOuterFactoriesCallSend(RX_FLOWABLE, TRY_SEND)
  }

  private fun testWhenInnerAndOuterFactoriesCallSend(method: String, emitter: String) {
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
                    $method { inner -> inner.$emitter("foo") }
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

  @Test
  fun `rxObservable - should succeed when aliased factory calls send`() {
    testWhenAliasedFactoryCallsSend(RX_OBSERVABLE, SEND)
  }

  @Test
  fun `rxObservable - should succeed when aliased factory calls trySend`() {
    testWhenAliasedFactoryCallsSend(RX_OBSERVABLE, TRY_SEND)
  }

  @Test
  fun `rxFlowable - should succeed when aliased factory calls send`() {
    testWhenAliasedFactoryCallsSend(RX_FLOWABLE, SEND)
  }

  @Test
  fun `rxFlowable - should succeed when aliased factory calls trySend`() {
    testWhenAliasedFactoryCallsSend(RX_FLOWABLE, TRY_SEND)
  }

  private fun testWhenAliasedFactoryCallsSend(method: String, emitter: String) {
    lint()
      .files(
        *files,
        kotlin(
          """
            package test

            import kotlinx.coroutines.rx3.$method as factory

            class Foo {
              fun foo() {
                factory { $emitter("foo") }
              }
            }
            """
        )
          .indented(),
      )
      .run()
      .expectClean()
  }

  private fun getError(method: String) =
    method.replaceFirstChar {
      if (it.isLowerCase()) it.titlecase(getDefault()) else it.toString()
    } + "DoesNotEmit"

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
