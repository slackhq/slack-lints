// Copyright (C) 2021 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint.annotations

/**
 * An annotation that is used to denote methods that should not be called from any context other
 * than Java code. This is important for cases in APIs that support both Kotlin and Java in
 * different ways.
 */
annotation class JavaOnly
