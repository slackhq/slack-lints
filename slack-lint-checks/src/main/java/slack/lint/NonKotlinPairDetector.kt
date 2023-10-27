// Copyright (C) 2021 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.android.tools.lint.detector.api.TextFormat
import java.util.EnumSet
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.getContainingUClass
import org.jetbrains.uast.resolveToUElement
import org.jetbrains.uast.util.isConstructorCall

/**
 * Checks to make sure that the code base will use the [kotlin.Pair] instead of any other
 * alternative Pairs. Pairs might come from other APIs, so this Detector is only concerned about
 * creating new Pairs.
 *
 * Cases that this detector should be warning about from the Java (and Kotlin) perspective.
 * - new androidx.core.util.Pair()
 * - androidx.core.util.Pair.create()
 * - new slack.commons.Pair()
 * - new android.util.Pair()
 */
class NonKotlinPairDetector : Detector(), SourceCodeScanner {

  override fun getApplicableUastTypes(): List<Class<out UElement>> {
    return listOf(UCallExpression::class.java)
  }

  override fun createUastHandler(context: JavaContext): UElementHandler {
    return object : UElementHandler() {
      override fun visitCallExpression(node: UCallExpression) {
        when {
          node.isConstructorCall() -> {
            checkForBannedPairType(context, node, node.resolveToUElement()?.getContainingUClass())
          }
          node.methodName == METHOD_NAME_PAIR_CREATE -> {
            val resolved = node.resolveToUElement() ?: return
            if (resolved is UMethod && resolved.isStatic) {
              checkForBannedPairType(context, node, resolved.getContainingUClass())
            }
          }
        }
      }
    }
  }

  private fun checkForBannedPairType(context: JavaContext, node: UCallExpression, uClass: UClass?) {
    if (uClass?.qualifiedName in BANNED_PAIR_TYPES) {
      val location = context.getLocation(node)
      val issueToReport = ISSUE_KOTLIN_PAIR_NOT_CREATED
      context.report(
        issueToReport,
        location,
        issueToReport.getBriefDescription(TextFormat.TEXT),
        null
      )
    }
  }

  companion object {
    private const val FQN_KOTLIN_PAIR = "kotlin.Pair"
    private const val FQN_SLACK_COMMONS_PAIR = "slack.commons.Pair"
    private const val FQN_ANDROIDX_PAIR = "androidx.core.util.Pair"
    private const val FQN_ANDROID_DEPRECATED_PAIR = "android.util.Pair"
    private const val FQN_ANDROID_PAIR = "com.android.utils"
    private const val METHOD_NAME_PAIR_CREATE = "create"

    private val BANNED_PAIR_TYPES =
      listOf(
        FQN_SLACK_COMMONS_PAIR,
        FQN_ANDROIDX_PAIR,
        FQN_ANDROID_DEPRECATED_PAIR,
        FQN_ANDROID_PAIR
      )

    /** Scope-set used for detectors which are affected by a single Java source file */
    private val JAVA_FILE_AND_TEST_SOURCES_SCOPE: EnumSet<Scope> =
      EnumSet.of(Scope.JAVA_FILE, Scope.TEST_SOURCES)

    private val ISSUE_KOTLIN_PAIR_NOT_CREATED: Issue =
      Issue.create(
        "KotlinPairNotCreated",
        "Use Kotlin's $FQN_KOTLIN_PAIR instead of other Pair types from other libraries like AndroidX and Slack commons",
        """
          We should consolidate to create and use a single type of Pair in the code base. Kotlin's Pair is preferred as it \
          works well with Java and Kotlin. It comes with extension functions that Slack's Pair doesn't offer.
      """,
        Category.CORRECTNESS,
        6,
        Severity.WARNING,
        Implementation(NonKotlinPairDetector::class.java, JAVA_FILE_AND_TEST_SOURCES_SCOPE)
      )

    val issues: List<Issue> = listOf(ISSUE_KOTLIN_PAIR_NOT_CREATED)
  }
}
