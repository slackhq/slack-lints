// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint.annotations

/**
 * When applied to a suspend Retrofit function, this annotation permits the function to have a
 * return type of [Unit], which otherwise will be flagged as an issue.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class AllowUnitResult
