// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint.compose

import org.junit.Test
import slack.lint.BaseSlackLintTest

class RememberSaveableAcceptableDetectorTest : BaseSlackLintTest() {

  private val javaIo =
    java(
        """
    package java.io;

    public interface Serializable {
    }

    """
          .trimIndent()
      )
      .indented()

  private val javaUtil =
    java(
        """
    package java.util;

    import java.io.Serializable;

    public class ArrayList<E> implements Serializable {}

    """
          .trimIndent()
      )
      .indented()

  private val androidOs =
    java(
      """
    package android.os;

    public interface Parcelable {}

    public final class Bundle  implements Parcelable {}


    """
        .trimIndent()
    )

  private val androidUtil =
    java(
      """
    package android.util;

    public class SparseArray<E> {}
    public final class Size {}
    public final class SizeF implements Parcelable {}

    """
        .trimIndent()
    )

  private val composeRuntime =
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
    ): MutableState<T> = createSnapshotMutableState(value, policy)

    fun <T> createSnapshotMutableState(
          value: T,
          policy: SnapshotMutationPolicy<T>,
    ): SnapshotMutableState<T> = ParcelableSnapshotMutableState(value, policy)

    private class ParcelableSnapshotMutableState<T>(
      val value: T,
      val policy: SnapshotMutationPolicy<T>,
        ) : Parcelable

    fun mutableIntStateOf(value: Int): MutableIntState = createSnapshotMutableIntState(value)

    fun createSnapshotMutableIntState(value: Int): MutableIntState =
      ParcelableSnapshotMutableIntState(value)

    private class ParcelableSnapshotMutableIntState(val value: Int) : Parcelable

    interface IntState : State<Int> {
      override val value: Int
        get() = intValue

      val intValue: Int
    }

    interface MutableIntState : IntState, MutableState<Int> {
      override var value: Int
      override var intValue: Int
    }

    fun mutableFloatStateOf(value: Float): MutableFloatState = TODO()

    interface MutableFloatState : FloatState, MutableState<Float> {
      override var value: Float
        get() = floatValue
        set(value) {
          floatValue = value
        }

      override var floatValue: Float
    }

    interface FloatState : State<Float> {
      override val value: Float
        get() = floatValue

      val floatValue: Float
    }

    """
          .trimIndent()
      )
      .indented()

  private val composeSaveable =
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

  private val testSaveables =
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

  override fun getDetector() = RememberSaveableAcceptableDetector()

  override fun getIssues() = listOf(RememberSaveableAcceptableDetector.ISSUE)

  @Test
  fun acceptableTypes_autoSaver_noError() {
    val source =
      kotlin(
          """
      package test

      import androidx.compose.runtime.Composable
      import androidx.compose.runtime.saveable.rememberSaveable
      import androidx.compose.runtime.saveable.autoSaver
      import androidx.compose.runtime.mutableStateOf
      import androidx.compose.runtime.mutableIntStateOf
      import androidx.compose.runtime.mutableFloatStateOf
      import java.util.ArrayList

      @Composable
      fun TestComposable() {
      
        val mutableStateLabeled = rememberSaveable(saver = autoSaver(), init = { mutableStateOf("value") })
        // Acceptable primitive types
        val stringValue = rememberSaveable { "test" }
        val intValue = rememberSaveable { 42 }
        val booleanValue = rememberSaveable { true }
        val floatValue = rememberSaveable { 1.0f }
        val doubleValue = rememberSaveable { 1.0 }
        val longValue = rememberSaveable { 1L }

        // Acceptable array types
        val stringArray = rememberSaveable { arrayOf("test") }
        val intArray = rememberSaveable { intArrayOf(42) }
        val booleanArray = rememberSaveable { booleanArrayOf(true) }
        val floatArray = rememberSaveable { floatArrayOf(1.0f) }
        val doubleArray = rememberSaveable { doubleArrayOf(1.0) }
        val longArray = rememberSaveable { longArrayOf(1L) }
        val parcelableArray = rememberSaveable { arrayOf<Parcelable>() }

        // Acceptable class types
        val serializableValue = rememberSaveable { TestSerializable() }
        val parcelableValue = rememberSaveable { TestParcelable() }
        val arrayListValue = rememberSaveable { ArrayList<String>() }

       // Nullable acceptable types
        val nullableString = rememberSaveable<String?> { null }
        val nullableInt = rememberSaveable<Int?> { null }
        val nullableBoolean = rememberSaveable<Boolean?> { null }

        // Mutable state types
        // Check policy and internal type      
        val mutableStateValue = rememberSaveable { mutableStateOf("value") }
        val mutableIntStateValue = rememberSaveable { mutableIntStateOf(1) }
        val mutableFloatStateValue = rememberSaveable { mutableFloatStateOf(1f) }

        // Acceptable collections with acceptable types

        // Kotlin promises listOf is serializable on jvm
        val listValue = rememberSaveable { listOf("test") }
        // Fail without map saver
        val mapValue = rememberSaveable { mapOf("key" to "value") }
        // Check policy and internal type
        val mutableStateValue = rememberSaveable { mutableStateOf("value") }

      }
      """
            .trimIndent()
        )
        .indented()

    lint()
      .files(
        javaIo,
        javaUtil,
        androidOs,
        androidUtil,
        composeRuntime,
        composeSaveable,
        testSaveables,
        source,
      )
      .run()
      .expectClean()
  }
}
