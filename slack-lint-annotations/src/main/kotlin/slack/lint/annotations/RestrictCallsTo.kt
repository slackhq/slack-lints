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
  /**
   * The target scope. Only file is supported for now, toe-hold left for possible future scopes.
   */
  val scope: Int = FILE
) {
  companion object {
    const val FILE = 0
  }
}
