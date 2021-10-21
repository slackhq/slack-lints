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

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.android.tools.lint.detector.api.TextFormat
import com.android.tools.lint.detector.api.isKotlin
import org.jetbrains.uast.UAnnotated
import org.jetbrains.uast.UAnnotation
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UField
import org.jetbrains.uast.UMethod
import slack.lint.util.sourceImplementation

/**
 * A simple detector that ensures that `@Inject`, `@Module`, and `@AssistedInject` are not used in
 * Java files in order to properly support Anvil factory generation.
 */
class InjectInJavaDetector : Detector(), SourceCodeScanner {

  override fun getApplicableUastTypes(): List<Class<out UElement>> {
    return listOf(UClass::class.java, UField::class.java, UMethod::class.java)
  }

  override fun createUastHandler(context: JavaContext): UElementHandler? {
    // Only applicable to Java files
    if (isKotlin(context.psiFile)) return null

    return object : UElementHandler() {
      private fun checkNode(node: UAnnotated) {
        node.findInjectionAnnotation()?.let { annotation ->
          context.report(
            ISSUE,
            context.getNameLocation(annotation),
            ISSUE.getBriefDescription(TextFormat.TEXT),
            quickfixData = null
          )
        }
      }

      override fun visitClass(node: UClass) = checkNode(node)
      override fun visitMethod(node: UMethod) = checkNode(node)
      override fun visitField(node: UField) = checkNode(node)
    }
  }

  companion object {
    private const val FQCN_INJECT = "javax.inject.Inject"
    private const val FQCN_MODULE = "dagger.Module"
    private const val FQCN_ASSISTED_INJECT = "dagger.assisted.AssistedInject"
    private const val FQCN_ASSISTED_FACTORY = "dagger.assisted.AssistedFactory"

    val ISSUE: Issue = Issue.create(
      "InjectInJava",
      "Only Kotlin classes should be injected in order for Anvil to work.",
      """
        Only Kotlin classes should be injected in order for Anvil to work. If you \
        cannot easily convert this to Kotlin, consider manually providing it via a Kotlin \
        `@Module`-annotated object.
      """,
      Category.CORRECTNESS,
      9,
      Severity.ERROR,
      sourceImplementation<InjectInJavaDetector>()
    )

    private val ANNOTATIONS = setOf(
      FQCN_INJECT, FQCN_ASSISTED_INJECT, FQCN_MODULE, FQCN_ASSISTED_FACTORY
    )

    private fun UAnnotated.findInjectionAnnotation(): UAnnotation? {
      for (annotation in ANNOTATIONS) {
        return findAnnotation(annotation) ?: continue
      }
      return null
    }
  }
}
