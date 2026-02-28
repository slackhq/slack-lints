// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint.compose

import com.android.tools.lint.checks.infrastructure.LintDetectorTest.java
import com.android.tools.lint.checks.infrastructure.LintDetectorTest.kotlin
import com.android.tools.lint.checks.infrastructure.TestFile
import org.junit.Ignore
import org.junit.Test
import slack.lint.BaseSlackLintTest

class RememberSaveableAcceptableDetectorTest : BaseSlackLintTest() {

  override fun getDetector() = RememberSaveableAcceptableDetector()

  override fun getIssues() = listOf(RememberSaveableAcceptableDetector.ISSUE)

  @Test
  fun acceptableTypes_primitives_noError() {
    val source =
      kotlin(
          """
          package test

          import androidx.compose.runtime.Composable
          import androidx.compose.runtime.saveable.rememberSaveable

          @Composable
          fun TestComposable() {
              val stringValue = rememberSaveable { "test" }
              val intValue = rememberSaveable { 42 }
              val booleanValue = rememberSaveable { true }
              val floatValue = rememberSaveable { 1.0f }
              val doubleValue = rememberSaveable { 1.0 }
              val longValue = rememberSaveable { 1L }
          }

                """
            .trimIndent()
        )
        .indented()

    test(source).expectClean()
  }

  @Test
  fun acceptableTypes_arrays_noError() {
    val source =
      kotlin(
          """
          package test

          import androidx.compose.runtime.Composable
          import androidx.compose.runtime.saveable.rememberSaveable

          @Composable
          fun TestComposable() {
              val stringArray = rememberSaveable { arrayOf("test") }
              val intArray = rememberSaveable { intArrayOf(42) }
              val booleanArray = rememberSaveable { booleanArrayOf(true) }
              val floatArray = rememberSaveable { floatArrayOf(1.0f) }
              val doubleArray = rememberSaveable { doubleArrayOf(1.0) }
              val longArray = rememberSaveable { longArrayOf(1L) }
              val parcelableArray = rememberSaveable { arrayOf<Parcelable>() }
          }

                """
            .trimIndent()
        )
        .indented()

    test(source).expectClean()
  }

  @Test
  fun acceptableTypes_classes_noError() {
    val source =
      kotlin(
          """
          package test

          import androidx.compose.runtime.Composable
          import androidx.compose.runtime.saveable.rememberSaveable
          import java.util.ArrayList

          @Composable
          fun TestComposable() {
            val serializableValue = rememberSaveable { TestSerializable() }
            val parcelableValue = rememberSaveable { TestParcelable() }
            val arrayListValue = rememberSaveable { ArrayList<String>() }
          }

                """
            .trimIndent()
        )
        .indented()

    test(source).expectClean()
  }

  @Test
  fun acceptableTypes_mutableStates_noError() {
    val source =
      kotlin(
          """
          package test

          import androidx.compose.runtime.Composable
          import androidx.compose.runtime.mutableDoubleStateOf
          import androidx.compose.runtime.mutableFloatStateOf
          import androidx.compose.runtime.mutableIntStateOf
          import androidx.compose.runtime.mutableLongStateOf
          import androidx.compose.runtime.mutableStateOf
          import androidx.compose.runtime.structuralEqualityPolicy
          import androidx.compose.runtime.saveable.rememberSaveable

          @Composable
          fun TestComposable() {
              val mutableState = rememberSaveable { mutableStateOf("value") }
              val mutableStateWithPolicy = rememberSaveable { mutableStateOf("value", structuralEqualityPolicy()) }
              val mutableIntState = rememberSaveable { mutableIntStateOf(1) }
              val mutableFloatState = rememberSaveable { mutableFloatStateOf(1f) }
              val mutableDoubleState = rememberSaveable { mutableDoubleStateOf(1.0) }
              val mutableLongState = rememberSaveable { mutableLongStateOf(1L) }
              val mutableTestParcelableState = rememberSaveable {
                  mutableStateOf(TestParcelable()).apply { value = TestParcelable() }
              }
              val mutableTestSerializableState = rememberSaveable {
                  mutableStateOf(TestSerializable()).apply { value = TestSerializable() }
              }
          }

                """
            .trimIndent()
        )
        .indented()

    test(source).expectClean()
  }

  @Test
  fun acceptableTypes_autoSaver_noError() {
    val source =
      kotlin(
          """
          package test

          import androidx.compose.runtime.Composable
          import androidx.compose.runtime.mutableStateOf
          import androidx.compose.runtime.saveable.autoSaver
          import androidx.compose.runtime.saveable.rememberSaveable

          @Composable
          fun TestComposable() {
              val mutableStateLabeled =
                  rememberSaveable(saver = autoSaver(), init = { mutableStateOf("value") })
          }

                """
            .trimIndent()
        )
        .indented()

    test(source).expectClean()
  }

  @Test
  fun acceptableTypes_mutableStateWithAcceptablePolicy_noError() {
    val source =
      kotlin(
          """
          package test

          import androidx.compose.runtime.Composable
          import androidx.compose.runtime.mutableStateOf
          import androidx.compose.runtime.neverEqualPolicy
          import androidx.compose.runtime.referentialEqualityPolicy
          import androidx.compose.runtime.structuralEqualityPolicy
          import androidx.compose.runtime.saveable.rememberSaveable

          @Composable
          fun TestComposable() {
              val structuralPolicy = rememberSaveable { mutableStateOf("value", structuralEqualityPolicy()) }
              val referentialPolicy = rememberSaveable { mutableStateOf("value", referentialEqualityPolicy()) }
              val neverEqualPolicy = rememberSaveable { mutableStateOf("value", neverEqualPolicy()) }
              val defaultPolicy = rememberSaveable { mutableStateOf("value") }
          }

                """
            .trimIndent()
        )
        .indented()

    test(source).expectClean()
  }

  @Test
  fun unacceptableTypes_mutableStateWithCustomPolicy_hasError() {
    val source =
      kotlin(
          """
          package test

          import androidx.compose.runtime.Composable
          import androidx.compose.runtime.mutableStateOf
          import androidx.compose.runtime.saveable.rememberSaveable

          @Composable
          fun TestComposable() {
              fun <T> customPolicy(): SnapshotMutationPolicy<T> = TODO()

              val customPolicyState = rememberSaveable { mutableStateOf("value", customPolicy()) }
          }

                """
            .trimIndent()
        )
        .indented()

    test(source)
      .expect(
        """
        src/test/test.kt:11: Error: Brief description (Custom policy) [RememberSaveableTypeMustBeAcceptable]
            val customPolicyState = rememberSaveable { mutableStateOf("value", customPolicy()) }
                                                       ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        1 error
        """
          .trimIndent()
      )
  }

  @Test
  fun unacceptableTypes_mutableStateWithCustomPolicy_noError() {
    val source =
      kotlin(
          """
          package test

          import androidx.compose.runtime.Composable
          import androidx.compose.runtime.mutableStateOf
          import androidx.compose.runtime.saveable.rememberSaveable

          @Composable
          fun TestComposable() {
            fun <T> customPolicy(): SnapshotMutationPolicy<T> = TODO()
            val saver =
              Saver<MutableState<String>, String>(
                save = { it.value },
                restore = { mutableStateOf(value = it, customPolicy()) },
              )
            val customPolicyState =
              rememberSaveable(saver = saver) {
                mutableStateOf(value = "value", policy = customPolicy())
              }
          }
          """
            .trimIndent()
        )
        .indented()

    test(source).expectClean()
  }

  @Ignore // TODO implement
  @Test
  fun acceptableTypes_collections_wrong_saver_error() {
    val source =
      kotlin(
          """
          package test

          import androidx.compose.runtime.Composable
          import androidx.compose.runtime.saveable.rememberSaveable

          @Composable
          fun TestComposable() {
            val listValue = rememberSaveable { listOf("test") }
            val mapValue = rememberSaveable { mapOf("key" to "value") }
          }

                """
            .trimIndent()
        )
        .indented()

    test(source).expectClean()
  }

  @Test
  fun acceptableTypes_lambdas_noError() {
    val source =
      kotlin(
          """
          package test

          import androidx.compose.runtime.Composable
          import androidx.compose.runtime.saveable.rememberSaveable

          @Composable
          fun TestComposable() {
            val lambdaInvokedValue = rememberSaveable { { "value" }() }
          }

                """
            .trimIndent()
        )
        .indented()

    test(source).expectClean()
  }

  @Test
  fun acceptableTypes_lambda_errors() {
    val source =
      kotlin(
          """
          package test

          import androidx.compose.runtime.Composable
          import androidx.compose.runtime.saveable.rememberSaveable

          @Composable
          fun TestComposable() {
            val lambdaValue = rememberSaveable { { "value" } }
          }

                """
            .trimIndent()
        )
        .indented()

    test(source)
      .expect(
        """
          src/test/test.kt:8: Error: Brief description (LAMBDA) [RememberSaveableTypeMustBeAcceptable]
            val lambdaValue = rememberSaveable { { "value" } }
                              ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
          1 error
        """
          .trimIndent()
      )
  }

  @Test
  fun acceptableTypes_multiReturnLambdas_errors() {
    val source =
      kotlin(
          """
          package test

          import androidx.compose.runtime.Composable
          import androidx.compose.runtime.saveable.rememberSaveable
          import kotlin.random.Random

          @Composable
          fun TestComposable() {
            val lambdaBlockValue = rememberSaveable {
              val a = { "value" }
              val b = { "other" }
              val c = if (Random.nextInt() == 42) 1 else 2
              if (c == 1) {
                a
              } else {
                ({ "other" })
              }
            }
          }

                """
            .trimIndent()
        )
        .indented()

    test(source)
      .expect(
        """
          src/test/test.kt:9: Error: Brief description (LAMBDA) [RememberSaveableTypeMustBeAcceptable]
            val lambdaBlockValue = rememberSaveable {
                                   ^
          1 error
        """
          .trimIndent()
      )
  }

  @Test
  fun customSaver_acceptableTypes_primitives_noError() {
    val source =
      kotlin(
          """
          package test

          import androidx.compose.runtime.Composable
          import androidx.compose.runtime.saveable.Saver
          import androidx.compose.runtime.saveable.rememberSaveable

          @Composable
          fun TestComposable() {
            val string1Saver = Saver<String, String>(save = { it }, restore = { it })
            val string3Saver =
              object : Saver<String, String> {
                override fun restore(value: String) = value

                override fun SaverScope.save(value: String) = value
              }
            val string1Value = rememberSaveable(saver = string1Saver) { "test" }
            val string2Value = rememberSaveable(saver = Saver(save = { it }, restore = { it })) { "test" }
            val string3Value = rememberSaveable(saver = string3Saver) { "test" }
            val intSaver = Saver<Int, Int>(save = { it }, restore = { it })
            val intValue = rememberSaveable(saver = intSaver) { 42 }
            val booleanSaver = Saver<Boolean, Boolean>(save = { it }, restore = { it })
            val booleanValue = rememberSaveable(saver = booleanSaver) { true }
            val floatSaver = Saver<Float, Float>(save = { it }, restore = { it })
            val floatValue = rememberSaveable { 1.0f }
            val doubleSaver = Saver<Double, Double>(save = { it }, restore = { it })
            val doubleValue = rememberSaveable(saver = doubleSaver) { 1.0 }
            val longSaver = Saver<Long, Long>(save = { it }, restore = { it })
            val longValue = rememberSaveable { 1L }
          }

                """
            .trimIndent()
        )
        .indented()

    test(source).expectClean()
  }

  @Test
  fun customSaver_acceptableTypes_arrays_noError() {
    val source =
      kotlin(
          """
          package test

          import androidx.compose.runtime.Composable
          import androidx.compose.runtime.saveable.Saver
          import androidx.compose.runtime.saveable.rememberSaveable

          @Composable
          fun TestComposable() {
              val stringSaver = Saver<Array<String>, Array<String>>(save = { it }, restore = { it })
              val stringArray = rememberSaveable(saver = stringSaver) { arrayOf("test") }
              val intSaver = Saver<IntArray, IntArray>(save = { it }, restore = { it })
              val intArray = rememberSaveable(saver = intSaver) { intArrayOf(42) }
              val booleanSaver = Saver<BooleanArray, BooleanArray>(save = { it }, restore = { it })
              val booleanArray = rememberSaveable(saver = booleanSaver) { booleanArrayOf(true) }
              val floatSaver = Saver<FloatArray, FloatArray>(save = { it }, restore = { it })
              val floatArray = rememberSaveable(saver = floatSaver) { floatArrayOf(1.0f) }
              val doubleSaver = Saver<DoubleArray, DoubleArray>(save = { it }, restore = { it })
              val doubleArray = rememberSaveable(saver = doubleSaver) { doubleArrayOf(1.0) }
              val longSaver = Saver<LongArray, LongArray>(save = { it }, restore = { it })
              val longArray = rememberSaveable(saver = longSaver) { longArrayOf(1L) }
              val parcelableSaver =
                Saver<Array<Parcelable>, Array<Parcelable>>(save = { it }, restore = { it })
              val parcelableArray = rememberSaveable(saver = parcelableSaver) { arrayOf() }
          }

                """
            .trimIndent()
        )
        .indented()

    test(source).expectClean()
  }

  @Test
  fun customSaver_acceptableTypes_classes_noError() {
    val saver =
      kotlin(
          """
          package test.saver

          import androidx.compose.runtime.saveable.Saver
          import test.TestParcelable

          class CustomTestParcelableSaver : Saver<TestParcelable, TestParcelable> {
            override fun restore(value: TestParcelable) = value

            override fun SaverScope.save(value: TestParcelable) = value
          }

          class CustomTestSerializableSaver : Saver<TestSerializable, TestSerializable> {
            override fun restore(value: TestSerializable) = value

            override fun SaverScope.save(value: TestSerializable) = value
          }

          class CustomArrayListSaver : Saver<ArrayList<String>, ArrayList<String>> {
            override fun restore(value: ArrayList<String>) = value

            override fun SaverScope.save(value: ArrayList<String>) = value
          }

                """
            .trimIndent()
        )
        .indented()

    val source =
      kotlin(
          """
          package test

          import androidx.compose.runtime.Composable
          import androidx.compose.runtime.saveable.rememberSaveable
          import test.saver.CustomTestParcelableSaver
          import test.saver.CustomTestSerializableSaver

          @Composable
          fun TestComposable() {
              val serializableValue =
                rememberSaveable(saver = CustomTestSerializableSaver()) { TestSerializable() }
              val parcelableValue =
                rememberSaveable(saver = CustomTestParcelableSaver()) { TestParcelable() }
              val arrayListValue = rememberSaveable(saver = CustomArrayListSaver()) { ArrayList() }
          }

                """
            .trimIndent()
        )
        .indented()

    test(saver, source).expectClean()
  }

  private fun test(vararg source: TestFile) =
    lint()
      .files(
        JAVA_IO,
        JAVA_UTIL,
        ANDROID_OS,
        ANDROID_UTIL,
        COMPOSE_RUNTIME,
        COMPOSE_SAVEABLE,
        TEST_SAVEABLES,
        *source,
      )
      .run()
}

private val JAVA_IO =
  java(
      """
      package java.io;

      public interface Serializable {
      }

      """
        .trimIndent()
    )
    .indented()

private val JAVA_UTIL =
  java(
      """
      package java.util;

      import java.io.Serializable;

      public class ArrayList<E> implements Serializable {}

      """
        .trimIndent()
    )
    .indented()

private val ANDROID_OS =
  java(
    """
    package android.os;

    public interface Parcelable {}

    public final class Bundle  implements Parcelable {}


    """
      .trimIndent()
  )

private val ANDROID_UTIL =
  java(
    """
    package android.util;

    public class SparseArray<E> {}
    public final class Size {}
    public final class SizeF implements Parcelable {}

    """
      .trimIndent()
  )

private val COMPOSE_RUNTIME =
  kotlin(
      """
      package androidx.compose.runtime

      annotation class Composable

      annotation class Stable

      interface SnapshotMutationPolicy<T>

      interface State<out T> {
        val value: T
      }

      interface MutableState<T> : State<T> {
        override var value: T
      }

      fun <T> mutableStateOf(
        value: T,
        policy: SnapshotMutationPolicy<T> = structuralEqualityPolicy(),
      ): MutableState<T> = TODO()

      interface MutableIntState : IntState, MutableState<Int> {
        override var value: Int
        override var intValue: Int
      }

      fun mutableIntStateOf(value: Int): MutableIntState = TODO()

      interface MutableFloatState : FloatState, MutableState<Float> {
        override var value: Float
        override var floatValue: Float
      }

      fun mutableFloatStateOf(value: Float): MutableFloatState = TODO()

      interface MutableDoubleState : MutableState<Double> {
        override var value: Double
        override var doubleValue: Double
      }

      fun mutableDoubleStateOf(value: Double): MutableDoubleState = TODO()

      interface MutableLongState : MutableState<Long> {
        override var value: Long
        override var longValue: Long
      }

      fun mutableLongStateOf(value: Long): MutableLongState = TODO()

      fun <T> structuralEqualityPolicy(): SnapshotMutationPolicy<T> = TODO()

      fun <T> referentialEqualityPolicy(): SnapshotMutationPolicy<T> = TODO()

      fun <T> neverEqualPolicy(): SnapshotMutationPolicy<T> = TODO()

            """
        .trimIndent()
    )
    .indented()

private val COMPOSE_SAVEABLE =
  kotlin(
      """
      package androidx.compose.runtime.saveable

      interface Saver<Original, Saveable : Any> {
        fun save(value: Original): Saveable?
        fun restore(value: Saveable): Original?
      }

      public fun <Original, Saveable : Any> Saver(
          save: SaverScope.(value: Original) -> Saveable?,
          restore: (value: Saveable) -> Original?,
      ): Saver<Original, Saveable>  = TODO()

      fun <T> autoSaver(): Saver<T, Any> = TODO()

      @Composable
      fun <T : Any> rememberSaveable(
          vararg inputs: Any?,
          saver: Saver<T, out Any> = autoSaver(),
          key: String? = null,
          init: () -> T
      ): T = TODO()

      @Composable
      fun <T> rememberSaveable(
          vararg inputs: Any?,
          stateSaver: Saver<T, out Any>,
          key: String? = null,
          init: () -> MutableState<T>
      ): MutableState<T>  = TODO()

      """
        .trimIndent()
    )
    .indented()

@Suppress("")
private val TEST_SAVEABLES =
  kotlin(
      """
      package test

      import android.os.Parcelable
      import java.io.Serializable

      class TestSerializable : Serializable
      class TestParcelable : Parcelable
      """
        .trimIndent()
    )
    .indented()
