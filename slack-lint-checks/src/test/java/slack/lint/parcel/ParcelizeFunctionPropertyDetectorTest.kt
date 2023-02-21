// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint.parcel

import org.junit.Test
import slack.lint.BaseSlackLintTest

class ParcelizeFunctionPropertyDetectorTest : BaseSlackLintTest() {

  companion object {
    private val PARCELIZE_STUBS =
      kotlin(
          "test/kotlinx/parcelize/Parcelize.kt",
          """
        package kotlinx.parcelize

        annotation class Parcelize
        annotation class IgnoredOnParcel
      """
        )
        .indented()
    private val PARCELABLE_STUBS =
      kotlin(
          "test/android/os/Parcelable.kt",
          """
        package android.os

        interface Parcelable
      """
        )
        .indented()
  }

  override fun getDetector() = ParcelizeFunctionPropertyDetector()
  override fun getIssues() = listOf(ParcelizeFunctionPropertyDetector.ISSUE)

  @Test
  fun simple() {
    lint()
      .files(
        PARCELIZE_STUBS,
        PARCELABLE_STUBS,
        kotlin(
            """
          package test.pkg

          import android.os.Parcelable
          import kotlinx.parcelize.Parcelize
          import kotlinx.parcelize.IgnoredOnParcel

          typealias FunctionType = () -> String

          @Parcelize
          class Example1(
            val foo: String,
            val functionType1: () -> String,
            val functionType2: (String) -> String,
            val functionType3: String.() -> String,
            val functionType4: () -> Unit,
            val functionType5: suspend () -> Unit,
            val aliasedFunction: FunctionType,
            val functionClass: FunctionClass,
            @IgnoredOnParcel
            val ignoredFunction: () -> Unit = {}, // This is allowed
          ) : Parcelable
          """
          )
          .indented()
      )
      .allowCompilationErrors(false)
      .run()
      .expect(
        """
        src/test/pkg/Example1.kt:12: Error: While technically (and surprisingly) supported by Parcelize, function types should not be used in Parcelize classes. There are only limited conditions where it will work and it's usually a sign that you're modeling your data wrong. [ParcelizeFunctionProperty]
          val functionType1: () -> String,
                             ~~~~~~~~~~~~
        src/test/pkg/Example1.kt:13: Error: While technically (and surprisingly) supported by Parcelize, function types should not be used in Parcelize classes. There are only limited conditions where it will work and it's usually a sign that you're modeling your data wrong. [ParcelizeFunctionProperty]
          val functionType2: (String) -> String,
                             ~~~~~~~~~~~~~~~~~~
        src/test/pkg/Example1.kt:14: Error: While technically (and surprisingly) supported by Parcelize, function types should not be used in Parcelize classes. There are only limited conditions where it will work and it's usually a sign that you're modeling your data wrong. [ParcelizeFunctionProperty]
          val functionType3: String.() -> String,
                             ~~~~~~~~~~~~~~~~~~~
        src/test/pkg/Example1.kt:15: Error: While technically (and surprisingly) supported by Parcelize, function types should not be used in Parcelize classes. There are only limited conditions where it will work and it's usually a sign that you're modeling your data wrong. [ParcelizeFunctionProperty]
          val functionType4: () -> Unit,
                             ~~~~~~~~~~
        src/test/pkg/Example1.kt:16: Error: While technically (and surprisingly) supported by Parcelize, function types should not be used in Parcelize classes. There are only limited conditions where it will work and it's usually a sign that you're modeling your data wrong. [ParcelizeFunctionProperty]
          val functionType5: suspend () -> Unit,
                             ~~~~~~~~~~~~~~~~~~
        src/test/pkg/Example1.kt:17: Error: While technically (and surprisingly) supported by Parcelize, function types should not be used in Parcelize classes. There are only limited conditions where it will work and it's usually a sign that you're modeling your data wrong. [ParcelizeFunctionProperty]
          val aliasedFunction: FunctionType,
                               ~~~~~~~~~~~~
        6 errors, 0 warnings
        """
          .trimIndent()
      )
  }
}
