// Copyright (C) 2021 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint.mocking

import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.Severity
import slack.lint.util.sourceImplementation

/** A [MockDetector.TypeChecker] that checks for mocking AutoValue classes. */
object AutoValueMockDetector : MockDetector.TypeChecker {
  override val issue: Issue =
    Issue.create(
      "DoNotMockAutoValue",
      "AutoValue classes represent pure data classes, so mocking them should not be necessary.",
      """
        `AutoValue` classes represent pure data classes, so mocking them should not be necessary. \
        Construct a real instance of the class instead.
      """,
      Category.CORRECTNESS,
      6,
      Severity.ERROR,
      sourceImplementation<MockDetector>(),
    )

  override val annotations: Set<String> =
    setOf("com.google.auto.value.AutoValue", "com.google.auto.value.AutoValue.Builder")
}
