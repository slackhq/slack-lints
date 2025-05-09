package slack.lint.rx

import com.android.tools.lint.checks.infrastructure.LintDetectorTest.kotlin
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import org.junit.Test
import org.junit.runner.RunWith
import slack.lint.BaseSlackLintTest

@Suppress("Junit4RunWithInspection")
@RunWith(TestParameterInjector::class)
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
    fun `rx factory - calls send`(
        @TestParameter("rxObservable", "rxFlowable") method: String,
        @TestParameter("send", "trySend") emitter: String,
    ) {
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

    @Test
    fun `rx factory - results of send are not the lambda return value`(
        @TestParameter("rxObservable", "rxFlowable") method: String,
        @TestParameter("send", "trySend") emitter: String,
    ) {
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
}

private val COROUTINE_CONTEXT_API =
    kotlin(
        """
          package kotlin.coroutines

          interface CoroutineContext
          """
    )
        .indented()

private val PRODUCER_SCOPE_API =
    kotlin(
        """
          package kotlin.coroutines

          interface ProducerScope<in E> {
              suspend fun send(element: E)
          }
          """
    )
        .indented()

private val RX_OBSERVABLE_API =
    kotlin(
        """
            package kotlinx.coroutines.rx3

            fun <T : Any> rxObservable(
              context: CoroutineContext,
              block: suspend ProducerScope<T>.() -> Unit
            ): Observable<T>
          """
    )
        .indented()

private val RX_FLOWABLE_API =
    kotlin(
        """
            package kotlinx.coroutines.rx3

            fun <T : Any> rxFlowable(
              context: CoroutineContext,
              block: suspend ProducerScope<T>.() -> Unit
            ): Flowable<T>
          """
    )
        .indented()

private val OBSERVABLE_API =
    kotlin(
        """
            package io.reactivex.rxjava3.core

            interface Observable<@NonNull T>
            """
    )
        .indented()

private val FLOWABLE_API =
    kotlin(
        """
            package io.reactivex.rxjava3.core

            interface Flowable<@NonNull T>
            """
    )
        .indented()

private val NON_NULL_API =
    kotlin(
        """
            package io.reactivex.rxjava4.annotations

            annotation class NonNull
            """
    )
        .indented()

private val files =
    arrayOf(
        COROUTINE_CONTEXT_API,
        PRODUCER_SCOPE_API,
        RX_OBSERVABLE_API,
        RX_FLOWABLE_API,
        OBSERVABLE_API,
        FLOWABLE_API,
        NON_NULL_API,
    )
