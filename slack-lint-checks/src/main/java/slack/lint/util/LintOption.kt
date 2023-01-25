package slack.lint.util

import com.android.tools.lint.client.api.Configuration
import com.android.tools.lint.detector.api.Context

/**
 * A layer of indirection for implementations of option loaders without needing to extend from Detector. This goes along
 * with [OptionLoadingDetector].
 */
interface LintOption {
  fun load(configuration: Configuration)
}