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
import kotlin.annotation.AnnotationTarget.CLASS

/**
 * Annotation representing a type that should not be mocked.
 *
 * When marking a type `@DoNotMock`, you should always point to alternative testing
 * solutions such as standard fakes or other testing utilities.
 *
 * Mockito tests can enforce this annotation by using a custom MockMaker which intercepts
 * creation of mocks.
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
