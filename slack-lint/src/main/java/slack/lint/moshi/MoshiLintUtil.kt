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
package slack.lint.moshi

import com.intellij.psi.PsiClass

internal object MoshiLintUtil {
  private const val FQCN_ANNOTATION_ADAPTED_BY = "dev.zacsweers.moshix.adapters.AdaptedBy"
  private const val FQCN_ANNOTATION_JSON_CLASS = "com.squareup.moshi.JsonClass"

  fun PsiClass.hasMoshiAnnotation(): Boolean {
    return hasAnnotation(FQCN_ANNOTATION_ADAPTED_BY) ||
      hasAnnotation(FQCN_ANNOTATION_JSON_CLASS)
  }
}
