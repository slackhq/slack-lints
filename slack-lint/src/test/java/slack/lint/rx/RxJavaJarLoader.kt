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
package slack.lint.rx

import com.android.tools.lint.checks.infrastructure.TestFiles.LibraryReferenceTestFile
import slack.lint.BaseSlackLintTest
import java.io.File

/** Loads the test RxJava 3 jar from resources. */
fun BaseSlackLintTest.rxJavaJar3() = LibraryReferenceTestFile(
  File(javaClass.classLoader.getResource("rxjava-3.1.0.jar")!!.toURI()),
)
