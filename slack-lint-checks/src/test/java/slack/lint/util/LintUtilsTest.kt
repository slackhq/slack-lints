// Copyright (C) 2021 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
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
