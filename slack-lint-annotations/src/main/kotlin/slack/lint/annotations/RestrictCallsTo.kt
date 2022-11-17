// Copyright (C) 2021 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint.annotations

import java.lang.annotation.Inherited
import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.FUNCTION

/**
 * Annotation representing a function or property that should not be called outside of a given
 * [scope]. Similar to androidx's `RestrictTo` annotation but just for calls.
 */
@Inherited
@Target(FUNCTION)
@Retention(RUNTIME)
annotation class RestrictCallsTo(
  /** The target scope. Only file is supported for now, toe-hold left for possible future scopes. */
  val scope: Int = FILE
) {
  companion object {
    const val FILE = 0
  }
}
