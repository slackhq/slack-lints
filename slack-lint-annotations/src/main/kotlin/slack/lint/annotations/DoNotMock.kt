// Copyright (C) 2021 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint.annotations

import java.lang.annotation.Inherited
import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.CLASS

/**
 * Annotation representing a type that should not be mocked.
 *
 * When marking a type `@DoNotMock`, you should always point to alternative testing solutions such
 * as standard fakes or other testing utilities.
 *
 * Mockito tests can enforce this annotation by using a custom MockMaker which intercepts creation
 * of mocks.
 */
@Inherited
@Target(CLASS)
@Retention(RUNTIME)
annotation class DoNotMock(
  /**
   * The reason why the annotated type should not be mocked.
   *
   * This should suggest alternative APIs to use for testing objects of this type.
   */
  val value: String = "Create a real instance instead"
)
