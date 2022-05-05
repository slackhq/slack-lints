package slack.lint

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.android.tools.lint.detector.api.TextFormat
import com.google.common.annotations.VisibleForTesting
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.util.InheritanceUtil
import org.jetbrains.kotlin.utils.keysToMap
import org.jetbrains.uast.UClass
import slack.lint.util.sourceImplementation

class UseAnvilForMultibindingDetector : Detector(), SourceCodeScanner {
    override fun getApplicableUastTypes() = listOf(UClass::class.java)

    override fun createUastHandler(context: JavaContext): UElementHandler {
        return object : UElementHandler() {
            override fun visitClass(node: UClass) {
                // Check if the class has the multibinding annotation
                val mutibindingAnnotations by lazy {
                    node.getAnnotations()
                        .filter { it.qualifiedName == FQCN_CONTRIBUTES_MULTIBINDING }
                }
                USE_ANVIL_FOR_MULTIBINDING_INTERFACES.forEach {
                    // Check if the class implements the interface that should be bound in the multi-bound set or map using Anvil.
                    val implementsInterface = InheritanceUtil.isInheritor(
                        node.javaPsi,
                        true,
                        it
                    )
                    if (node.isFinal && !node.isInterface && implementsInterface && !mutibindingAnnotations.matchingBoundTypeFound(it)) {
                        val issue = issuesMap[it]!!
                        context.report(
                            issue,
                            context.getNameLocation(node),
                            issue.getBriefDescription(TextFormat.TEXT),
                            quickfixData = null
                        )
                    }

                }
            }
        }
    }

    companion object {

        // TODO Hard-coded for now. See if we can read this from the gradle.properties file for flexibility.
        @VisibleForTesting
        internal val USE_ANVIL_FOR_MULTIBINDING_INTERFACES = listOf(
            "slack.commons.android.persistence.cachebuster.CacheResetAware",
            "slack.commons.android.persistence.cachebuster.CacheFileLogoutAware"
        )

        @VisibleForTesting
        internal val FQCN_CONTRIBUTES_MULTIBINDING =
            "com.squareup.anvil.annotations.ContributesMultibinding"

        private val issuesMap =
            USE_ANVIL_FOR_MULTIBINDING_INTERFACES.keysToMap { createIssue(it) }

        val issues = issuesMap.values.toList()

        private fun String.toSimpleName() = this.split(".").last()

        private fun List<PsiAnnotation>.matchingBoundTypeFound(fqcn: String) =
            find { annotation -> annotation.findMatchingBoundType(fqcn) } != null

        // TODO Find a better way to get FQCN of the bound type argument
        private fun PsiAnnotation.findMatchingBoundType(fqcn: String): Boolean {
            val boundTypeValue = findAttributeValue("boundType")?.text
            return "$fqcn::class" == boundTypeValue || "${fqcn.toSimpleName()}::class" == boundTypeValue
        }

        private fun createIssue(fqcn: String): Issue {
            val simpleName = fqcn.toSimpleName()
            return Issue.create(
                "UseAnvilForMultibindingFor${simpleName}",
                "Consider using `@ContributesMultibinding` instead of writing a binding method for `$simpleName`",
                "Consider using `@ContributesMultibinding` instead of writing a binding method for `$simpleName`",
                Category.CORRECTNESS,
                10,
                Severity.ERROR,
                sourceImplementation<UseAnvilForMultibindingDetector>()
            )
        }
    }
}