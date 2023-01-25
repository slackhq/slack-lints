// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint.compose

import com.android.tools.lint.client.api.Configuration
import com.android.tools.lint.detector.api.StringOption
import slack.lint.util.LintOption
import slack.lint.util.loadAsSet

open class StringSetLintOption(private val option: StringOption) : LintOption {
  var value: Set<String> = emptySet()
    private set

  override fun load(configuration: Configuration) {
    value = option.loadAsSet(configuration)
  }
}
