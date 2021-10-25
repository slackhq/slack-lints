/*
 * Copyright (C) 2021 Slack Technologies, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package slack.lint.mocking

import com.android.tools.lint.checks.infrastructure.TestFiles.java
import com.android.tools.lint.checks.infrastructure.TestFiles.kotlin

private val mockito = java(
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
).indented()

private val mock = java(
  """
      package org.mockito;

      public @interface Mock {

      }
    """
).indented()

private val spy = java(
  """
      package org.mockito;

      public @interface Spy {

      }
    """
).indented()

private val mockitoHelpers = kotlin(
  "test/slack/test/mockito/MockitoHelpers.kt",
  """
      package slack.test.mockito

      import org.mockito.Mockito

      inline fun <reified T : Any> mock(): T = Mockito.mock(T::class.java)

      inline fun <reified T : Any> mock(block: T.() -> Unit): T {
        return Mockito.mock(T::class.java).apply(block)
      }
    """
).indented()

internal fun mockFileStubs() = arrayOf(
  mockito,
  mock,
  spy,
  mockitoHelpers
)
