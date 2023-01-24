// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint.compose

import com.android.tools.lint.detector.api.Context
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.android.tools.lint.detector.api.StringOption
import slack.lint.compose.ContentEmitterAwareDetector.Companion.PROVIDED_CONTENT_EMITTERS

/** A [Detector] that supports reading a [PROVIDED_CONTENT_EMITTERS] configuration. */
abstract class ContentEmitterAwareDetector : Detector(), SourceCodeScanner {

  companion object {
    val PROVIDED_CONTENT_EMITTERS =
      StringOption(
        "content-emitters",
        "A comma-separated list of known content-emitting composables.",
        null,
        "This property should define a comma-separated list of known content-emitting composables."
      )

    /**
     * Loads a comma-separated list of allowed names from the [PROVIDED_CONTENT_EMITTERS] option.
     */
    fun loadProvidedContentEmitters(context: Context): Set<String> {
      return PROVIDED_CONTENT_EMITTERS.getValue(context.configuration)
        ?.splitToSequence(",")
        .orEmpty()
        .map(String::trim)
        .filter(String::isNotBlank)
        .toSet()
    }
  }

  private var providedContentEmitters: Set<String>? = null

  override fun beforeCheckRootProject(context: Context) {
    super.beforeCheckRootProject(context)
    providedContentEmitters = loadProvidedContentEmitters(context)
  }

  protected fun providedContentEmitters() = providedContentEmitters.orEmpty()
}
