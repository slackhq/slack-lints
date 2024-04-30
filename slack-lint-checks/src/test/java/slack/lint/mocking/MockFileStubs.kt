// Copyright (C) 2021 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint.mocking

import com.android.tools.lint.checks.infrastructure.TestFiles.java
import com.android.tools.lint.checks.infrastructure.TestFiles.kotlin

private val mockito =
  java(
      """
      package org.mockito;

      public final class Mockito {
        public static <T> T mock(Class<T> clazz) {
          return null;
        }
        public static <T> T spy(T instance) {
          return null;
        }
      }
    """
    )
    .indented()

private val mock =
  java(
      """
      package org.mockito;

      public @interface Mock {

      }
    """
    )
    .indented()

private val spy =
  java(
      """
      package org.mockito;

      public @interface Spy {

      }
    """
    )
    .indented()

private val mockitoHelpers =
  kotlin(
      "test/slack/test/mockito/MockitoHelpers.kt",
      """
      package slack.test.mockito

      import org.mockito.Mockito

      inline fun <reified T : Any> mock(): T = Mockito.mock(T::class.java)

      inline fun <reified T : Any> mock(block: T.() -> Unit): T {
        return Mockito.mock(T::class.java).apply(block)
      }
    """,
    )
    .indented()

internal fun mockFileStubs() = arrayOf(mockito, mock, spy, mockitoHelpers)
