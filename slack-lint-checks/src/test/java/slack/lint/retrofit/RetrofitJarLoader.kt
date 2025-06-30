// Copyright (C) 2021 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint.retrofit

import com.android.tools.lint.checks.infrastructure.TestFiles.LibraryReferenceTestFile
import java.io.File
import slack.lint.BaseSlackLintTest

/** Loads the test Retrofit 3 jar from resources. */
fun BaseSlackLintTest.retrofit3Jar() =
  LibraryReferenceTestFile(File(javaClass.classLoader.getResource("retrofit-3.0.0.jar")!!.toURI()))
