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
package slack.lint.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class LintUtilsTest {
  @Test
  fun snakeToCamelTests() {
    "noChange" assertSnakeToCamel "noChange"
    "NoChange" assertSnakeToCamel "NoChange"
    "test-value" assertSnakeToCamel "testValue"
    "test_value" assertSnakeToCamel "testValue"
    "test_value_multiple" assertSnakeToCamel "testValueMultiple"
    "multi__scores" assertSnakeToCamel "multiScores"
    "trailing__" assertSnakeToCamel "trailing"
    "___leading" assertSnakeToCamel "leading"
  }

  @Test
  fun toScreamingSnakeTests() {
    "camelCase" assertCamelToScreamingSnake "CAMEL_CASE"
    "CapCamelCase" assertCamelToScreamingSnake "CAP_CAMEL_CASE"
    "NO_CHANGE" assertCamelToScreamingSnake "NO_CHANGE"
    "test-value" assertCamelToScreamingSnake "TEST_VALUE"
    "test_value" assertCamelToScreamingSnake "TEST_VALUE"
    "test_value_multiple" assertCamelToScreamingSnake "TEST_VALUE_MULTIPLE"
    "multi__scores" assertCamelToScreamingSnake "MULTI_SCORES"
    "trailing__" assertCamelToScreamingSnake "TRAILING"
    "___leading" assertCamelToScreamingSnake "LEADING"
  }

  private infix fun String.assertSnakeToCamel(other: String) {
    assertThat(this.snakeToCamel()).isEqualTo(other)
  }

  private infix fun String.assertCamelToScreamingSnake(other: String) {
    assertThat(this.toScreamingSnakeCase()).isEqualTo(other)
  }
}
