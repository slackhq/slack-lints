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
package slack.lint

import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.TestFile
import com.android.tools.lint.checks.infrastructure.TestLintClient
import com.android.tools.lint.checks.infrastructure.TestLintTask
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import com.android.utils.SdkUtils
import junit.framework.TestCase
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.InputStream
import java.net.MalformedURLException

@Suppress("UnstableApiUsage")
@RunWith(JUnit4::class)
abstract class BaseSlackLintTest : LintDetectorTest() {
  private val rootPath = "resources/test/com/slack/lint/data/"
  private val stubsPath = "testStubs"

  /** Optional override to customize the lint client name when running lint test tasks. */
  open val lintClientName: String? = null

  fun loadStub(stubName: String): TestFile {
    return copy(stubsPath + File.separatorChar + stubName, "src/main/java/$stubName")
  }

  abstract override fun getDetector(): Detector

  abstract override fun getIssues(): List<Issue>

  override fun lint(): TestLintTask {
    val sdkLocation = System.getProperty("android.sdk")
    val lintTask = super.lint()
    lintClientName?.let {
      lintTask.clientFactory { TestLintClient(it) }
    }
    sdkLocation?.let {
      lintTask.sdkHome(File(it))
    }
    lintTask.allowCompilationErrors(false)
    return lintTask
  }

  /**
   * The default finder for resources doesn't work with our file structure; this ensures it will.
   *
   * https://www.bignerdranch.com/blog/building-custom-lint-checks-in-android/
   */
  override fun getTestResource(relativePath: String, expectExists: Boolean): InputStream {
    val path = (rootPath + relativePath).replace('/', File.separatorChar)
    val file = File(getTestDataRootDir(), path)
    if (file.exists()) {
      try {
        return BufferedInputStream(FileInputStream(file))
      } catch (e: FileNotFoundException) {
        if (expectExists) {
          TestCase.fail("Could not find file $relativePath")
        }
      }
    }
    return BufferedInputStream(ByteArrayInputStream("".toByteArray()))
  }

  private fun getTestDataRootDir(): File? {
    val source = javaClass.protectionDomain.codeSource
    if (source != null) {
      val location = source.location
      try {
        val classesDir = SdkUtils.urlToFile(location)
        return classesDir.parentFile!!.absoluteFile.parentFile!!.parentFile
      } catch (e: MalformedURLException) {
        fail(e.localizedMessage)
      }
    }
    return null
  }
}
