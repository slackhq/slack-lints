// Copyright (C) 2021 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.android.tools.lint.detector.api.TextFormat
import org.jetbrains.uast.UClass
import slack.lint.util.implements
import slack.lint.util.sourceImplementation

class SerializableDetector : Detector(), SourceCodeScanner {
  override fun getApplicableUastTypes() = listOf(UClass::class.java)

  override fun createUastHandler(context: JavaContext): UElementHandler {
    return object : UElementHandler() {
      override fun visitClass(node: UClass) {
        if (node.isEnum) return
        val implementsSerializable =
          node.implements("java.io.Serializable") { fqcn ->
            // Only look in slack sources
            "slack" in fqcn
          }
        if (implementsSerializable) {
          // TODO after we drop Serializable entirely, we should always make it an error.
          if (node.implements("android.os.Parcelable")) return

          context.report(
            ISSUE,
            context.getNameLocation(node),
            ISSUE.getBriefDescription(TextFormat.TEXT),
          )
        }
      }
    }
  }

  companion object {
    val ISSUE: Issue =
      Issue.create(
        "SerializableUsage",
        "Don't use Serializable.",
        """
        Don't use Serializable. It's brittle, requires reflection, does not \
        work well with Kotlin, and prevents us from using Core Library Desugaring. \
        Either implement Parcelable too or use another safer serialization mechanism.
      """,
        Category.CORRECTNESS,
        6,
        Severity.ERROR,
        sourceImplementation<SerializableDetector>(),
      )
  }
}
