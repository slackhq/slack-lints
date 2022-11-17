// Copyright (C) 2021 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint.rx

import com.android.tools.lint.checks.infrastructure.TestFiles.LibraryReferenceTestFile
import java.io.File
import slack.lint.BaseSlackLintTest

/** Loads the test RxJava 3 jar from resources. */
fun BaseSlackLintTest.rxJavaJar3() =
  LibraryReferenceTestFile(
    File(javaClass.classLoader.getResource("rxjava-3.1.0.jar")!!.toURI()),
  )
