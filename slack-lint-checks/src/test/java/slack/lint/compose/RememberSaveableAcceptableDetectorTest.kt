// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint.compose

import com.android.tools.lint.checks.infrastructure.LintDetectorTest.java
import com.android.tools.lint.checks.infrastructure.LintDetectorTest.kotlin
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

    lint()
      .files(
        JAVA_IO,
        JAVA_UTIL,
        ANDROID_OS,
        ANDROID_UTIL,
        COMPOSE_RUNTIME,
        COMPOSE_SAVEABLE,
        TEST_SAVEABLES,
        source,
      )
      .run()
      .expectClean()
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

    lint()
      .files(
        JAVA_IO,
        JAVA_UTIL,
        ANDROID_OS,
        ANDROID_UTIL,
        COMPOSE_RUNTIME,
        COMPOSE_SAVEABLE,
        TEST_SAVEABLES,
        source,
      )
      .run()
      .expectClean()
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

    lint()
      .files(
        JAVA_IO,
        JAVA_UTIL,
        ANDROID_OS,
        ANDROID_UTIL,
        COMPOSE_RUNTIME,
        COMPOSE_SAVEABLE,
        TEST_SAVEABLES,
        source,
      )
      .run()
      .expectClean()
  }

  @Test
  fun acceptableTypes_nullables_noError() {
    val source =
      kotlin(
          """
package test

import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable

@Composable
fun TestComposable() {
    val nullableString = rememberSaveable<String?> { null }
    val nullableInt = rememberSaveable<Int?> { null }
    val nullableBoolean = rememberSaveable<Boolean?> { null }
}

      """
            .trimIndent()
        )
        .indented()

    lint()
      .files(
        JAVA_IO,
        JAVA_UTIL,
        ANDROID_OS,
        ANDROID_UTIL,
        COMPOSE_RUNTIME,
        COMPOSE_SAVEABLE,
        TEST_SAVEABLES,
        source,
      )
      .run()
      .expectClean()
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

    lint()
      .files(
        JAVA_IO,
        JAVA_UTIL,
        ANDROID_OS,
        ANDROID_UTIL,
        COMPOSE_RUNTIME,
        COMPOSE_SAVEABLE,
        TEST_SAVEABLES,
        source,
      )
      .run()
      .expectClean()
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

    lint()
      .files(
        JAVA_IO,
        JAVA_UTIL,
        ANDROID_OS,
        ANDROID_UTIL,
        COMPOSE_RUNTIME,
        COMPOSE_SAVEABLE,
        TEST_SAVEABLES,
        source,
      )
      .run()
      .expectClean()
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

    lint()
      .files(
        JAVA_IO,
        JAVA_UTIL,
        ANDROID_OS,
        ANDROID_UTIL,
        COMPOSE_RUNTIME,
        COMPOSE_SAVEABLE,
        TEST_SAVEABLES,
        source,
      )
      .run()
      .expectClean()
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

    lint()
      .files(
        JAVA_IO,
        JAVA_UTIL,
        ANDROID_OS,
        ANDROID_UTIL,
        COMPOSE_RUNTIME,
        COMPOSE_SAVEABLE,
        TEST_SAVEABLES,
        source,
      )
      .run()
      .expect(
        """
src/test/test.kt:11: Error: Brief description [RememberSaveableTypeMustBeAcceptable]
    val customPolicyState = rememberSaveable { mutableStateOf("value", customPolicy()) }
                            ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
1 errors, 0 warnings
        """
          .trimIndent()
      )
  }

  @Test
  fun acceptableTypes_collectionsAndLambdas_noError() {
    val source =
      kotlin(
          """
package test

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import kotlin.random.Random

@Composable
fun TestComposable() {
    val listValue = rememberSaveable { listOf("test") }
    val mapValue = rememberSaveable { mapOf("key" to "value") }
    val mutableStateValue = rememberSaveable { mutableStateOf("value") }
    val lambdaInvokedValue = rememberSaveable { { "value" }() }
    val lambdaValue = rememberSaveable { { "value" } }
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

    lint()
      .files(
        JAVA_IO,
        JAVA_UTIL,
        ANDROID_OS,
        ANDROID_UTIL,
        COMPOSE_RUNTIME,
        COMPOSE_SAVEABLE,
        TEST_SAVEABLES,
        source,
      )
      .run()
      .expectClean()
  }
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
