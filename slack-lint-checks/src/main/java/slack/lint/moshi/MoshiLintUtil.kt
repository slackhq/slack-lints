// Copyright (C) 2021 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint.moshi

import com.intellij.psi.PsiClass

internal object MoshiLintUtil {
  private const val FQCN_ANNOTATION_ADAPTED_BY = "dev.zacsweers.moshix.adapters.AdaptedBy"
  private const val FQCN_ANNOTATION_JSON_CLASS = "com.squareup.moshi.JsonClass"

  fun PsiClass.hasMoshiAnnotation(): Boolean {
    return hasAnnotation(FQCN_ANNOTATION_ADAPTED_BY) || hasAnnotation(FQCN_ANNOTATION_JSON_CLASS)
  }
}
