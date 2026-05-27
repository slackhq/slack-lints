// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint.util

import com.android.tools.lint.client.api.Configuration
import com.android.tools.lint.detector.api.StringOption

open class StringListLintOption(private val option: StringOption) : LintOption {
  var value: List<String> = emptyList()
    private set

  override fun load(configuration: Configuration) {
    value = option.loadAsList(configuration)
  }
}

internal fun StringOption.loadAsList(
  configuration: Configuration,
  delimiter: String = ",",
): List<String> {
  return getValue(configuration)
    ?.splitToSequence(delimiter)
    .orEmpty()
    .map(String::trim)
    .filter(String::isNotBlank)
    .toList()
}
