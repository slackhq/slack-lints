// Copyright (C) 2021 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint.retrofit

import com.android.tools.lint.checks.infrastructure.TestFiles.LibraryReferenceTestFile
import java.io.File
import slack.lint.BaseSlackLintTest

/** Loads the test Retrofit 2 jar from resources. */
fun BaseSlackLintTest.retrofit2Jar() =
  LibraryReferenceTestFile(File(javaClass.classLoader.getResource("retrofit-2.9.0.jar")!!.toURI()))
