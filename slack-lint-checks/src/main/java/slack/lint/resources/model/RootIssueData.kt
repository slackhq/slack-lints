// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint.resources.model

import com.android.tools.lint.detector.api.Location

data class RootIssueData(val alias: String, val nameLocation: Location)
