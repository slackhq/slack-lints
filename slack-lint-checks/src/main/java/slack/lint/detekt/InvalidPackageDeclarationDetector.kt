// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint.detekt

import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Context
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Location
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.android.tools.lint.detector.api.isKotlin
import org.jetbrains.uast.UFile
import slack.lint.util.sourceImplementation

class InvalidPackageDeclarationDetector : Detector(), SourceCodeScanner {

  override fun applicableSuperClasses(): List<String>? = null

  override fun afterCheckFile(context: Context) {
    val javaContext = context as? JavaContext ?: return
    if (!isKotlin(javaContext.psiFile)) return
    val uFile = javaContext.uastFile ?: return
    checkPackage(javaContext, uFile)
  }

  private fun checkPackage(context: JavaContext, file: UFile) {
    val declaredPackage = file.packageName
    val filePath = context.file.path
    val srcSegments =
      listOf(
        "/src/main/java/",
        "/src/main/kotlin/",
        "/src/test/java/",
        "/src/test/kotlin/",
        "/src/debug/java/",
        "/src/debug/kotlin/",
        "/src/release/java/",
        "/src/release/kotlin/",
        "/src/androidTest/java/",
        "/src/androidTest/kotlin/",
      )
    val srcIndex =
      srcSegments.firstNotNullOfOrNull { segment ->
        val idx = filePath.indexOf(segment)
        if (idx >= 0) idx + segment.length else null
      } ?: return

    val relativePath = filePath.substring(srcIndex)
    val expectedPackage = relativePath.substringBeforeLast('/').replace('/', '.')

    if (expectedPackage.isNotEmpty() && declaredPackage != expectedPackage) {
      context.report(
        ISSUE,
        Location.create(context.file),
        "Package declaration `$declaredPackage` does not match directory structure. Expected `$expectedPackage`.",
      )
    }
  }

  companion object {
    val ISSUE =
      Issue.create(
        id = "InvalidPackageDeclaration",
        briefDescription = "Package declaration does not match directory structure",
        explanation =
          "The package declaration should match the file's directory structure " +
            "relative to the source root.",
        category = Category.CORRECTNESS,
        priority = 5,
        severity = Severity.WARNING,
        implementation = sourceImplementation<InvalidPackageDeclarationDetector>(),
      )
  }
}
