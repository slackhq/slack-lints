// Copyright (C) 2021 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint.mocking

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Context
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.android.tools.lint.detector.api.StringOption
import com.android.tools.lint.detector.api.isJava
import com.android.tools.lint.detector.api.isKotlin
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassType
import com.intellij.util.containers.map2Array
import java.util.Locale
import kotlin.io.path.bufferedWriter
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.io.path.deleteExisting
import kotlin.io.path.exists
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.uast.UAnnotated
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UClassLiteralExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UField
import org.jetbrains.uast.UReferenceExpression
import org.jetbrains.uast.UastCallKind
import slack.lint.mocking.MockDetector.Companion.TYPE_CHECKERS
import slack.lint.mocking.MockDetector.TypeChecker
import slack.lint.util.MetadataJavaEvaluator
import slack.lint.util.OptionLoadingDetector
import slack.lint.util.StringSetLintOption

private data class MockFactory(val declarationContainer: String, val factoryName: String)

/**
 * A detector for detecting different kinds of mocking behavior. Implementations of [TypeChecker]
 * can indicate annotated types that should be reported via [TypeChecker.checkType] or
 * [TypeChecker.annotations] for more dynamic control.
 *
 * New [TypeChecker] implementations should be added to [TYPE_CHECKERS] to run in this.
 */
class MockDetector
@JvmOverloads
constructor(
  private val mockAnnotationsOption: StringSetLintOption = StringSetLintOption(MOCK_ANNOTATIONS),
  private val mockFactoriesOption: StringSetLintOption = StringSetLintOption(MOCK_FACTORIES),
  private val mockReport: StringOption = MOCK_REPORT,
) : OptionLoadingDetector(mockAnnotationsOption, mockFactoriesOption), SourceCodeScanner {
  companion object {

    internal const val MOCK_REPORT_PATH = "build/reports/mockdetector/mock-report.csv"

    internal val MOCK_ANNOTATIONS =
      StringOption(
        "mock-annotations",
        "A comma-separated list of mock annotations.",
        "org.mockito.Mock,org.mockito.Spy",
        "This property should define comma-separated list of mock annotation class names (FQCN).",
      )

    internal val MOCK_FACTORIES =
      StringOption(
        "mock-factories",
        "A comma-separated list of mock factories (org.mockito.Mockito#methodName).",
        "org.mockito.Mockito#mock,org.mockito.Mockito#spy,slack.test.mockito.MockitoHelpers#mock,slack.test.mockito.MockitoHelpersKt#mock",
        "A comma-separated list of mock factories (org.mockito.Mockito#methodName).",
      )

    internal val MOCK_REPORT =
      StringOption(
        "mock-report",
        "If enabled, writes a mock report to <project-dir>/$MOCK_REPORT_PATH.",
        "none",
        "If enabled, writes a mock report to <project-dir>/$MOCK_REPORT_PATH. The format of the file is a csv of (type,isError) of mocked classes.",
      )

    private val TYPE_CHECKERS =
      listOf(
        // Loosely defined in the order of most likely to be hit
        AnyMockDetector,
        PlatformTypeMockDetector,
        DataClassMockDetector,
        DoNotMockMockDetector,
        SealedClassMockDetector,
        AutoValueMockDetector,
        ObjectClassMockDetector,
        RecordClassMockDetector,
      )
    private val OPTIONS = listOf(MOCK_ANNOTATIONS, MOCK_FACTORIES, MOCK_REPORT)
    val ISSUES = TYPE_CHECKERS.map2Array { it.issue.setOptions(OPTIONS) }
  }

  // A mapping of mocked types
  private val reports = mutableListOf<Pair<String, Boolean>>()

  override fun getApplicableUastTypes() = listOf(UCallExpression::class.java, UField::class.java)

  override fun createUastHandler(context: JavaContext): UElementHandler? {
    if (!context.isTestSource) return null

    val checkers =
      TYPE_CHECKERS.filter { context.isEnabled(it.issue) }
        .ifEmpty {
          return null
        }
    val slackEvaluator = MetadataJavaEvaluator(context.file.name, context.evaluator)

    val reportingMode = reportMode(context)

    val reportErrors = reportingMode == MockReportMode.ALL || reportingMode == MockReportMode.ERRORS
    val reportAll = reportingMode == MockReportMode.ALL

    val mockFactories: Map<String, Set<String>> =
      mockFactoriesOption.value
        .map { factory ->
          val (declarationContainer, factoryName) = factory.split("#")
          MockFactory(declarationContainer, factoryName)
        }
        .groupBy { it.factoryName }
        .mapValues { it.value.mapTo(mutableSetOf()) { it.declarationContainer } }

    return object : UElementHandler() {

      // Checks for mock()/spy() calls
      override fun visitCallExpression(node: UCallExpression) {
        // We only want method calls
        if (node.kind != UastCallKind.METHOD_CALL) return

        // Check our known mock methods
        val mockFactoryContainers =
          mockFactories[node.methodName]?.takeIf { it.isNotEmpty() } ?: return

        // Matches a known method, now check if this one's in that method's known declaration
        // container
        val resolvedContainer =
          node.resolve()?.let {
            it.containingClass?.qualifiedName ?: context.evaluator.getPackage(it)?.qualifiedName
          } ?: return

        if (resolvedContainer !in mockFactoryContainers) return

        // Now resolve the mocked type
        var argumentType: PsiClass? = null
        val expressionType = node.getExpressionType()
        when {
          expressionType != null -> {
            argumentType = slackEvaluator.getTypeClass(expressionType)
          }
          node.typeArgumentCount == 1 -> {
            // We can read the type here for the fun <reified T> mock() helpers
            argumentType = slackEvaluator.getTypeClass(node.typeArguments[0])
          }
          node.valueArgumentCount != 0 -> {
            when (val firstArg = node.valueArguments[0]) {
              is UClassLiteralExpression -> {
                // It's Foo.class, we can just use it directly
                argumentType = slackEvaluator.getTypeClass(firstArg.type)
              }
              is UReferenceExpression -> {
                val type = firstArg.getExpressionType()
                if (node.methodName == "spy") {
                  // spy takes an instance, so take the type at face value
                  argumentType = slackEvaluator.getTypeClass(type)
                } else if (type is PsiClassType && type.parameterCount == 1) {
                  // If it's a Class and not a "spy" method, assume it's the mock type
                  val classGeneric = type.parameters[0]
                  argumentType = slackEvaluator.getTypeClass(classGeneric)
                }
              }
            }
          }
        }

        argumentType?.let { checkMock(node, argumentType) }
      }

      // Checks properties and fields, usually annotated with @Mock/@Spy
      override fun visitField(node: UField) {
        if (isKotlin(node.language)) {
          val sourcePsi = node.sourcePsi ?: return
          if (sourcePsi is KtProperty && isMockAnnotated(node)) {
            val type = slackEvaluator.getTypeClass(node.type) ?: return
            checkMock(node, type)
            return
          }
        } else if (isJava(node.language) && isMockAnnotated(node)) {
          val type = slackEvaluator.getTypeClass(node.type) ?: return
          checkMock(node, type)
          return
        }
      }

      private fun isMockAnnotated(node: UAnnotated): Boolean {
        return mockAnnotationsOption.value.any { node.findAnnotation(it) != null }
      }

      private fun addReport(type: PsiClass, isError: Boolean) {
        if (reportAll || (isError && reportErrors)) {
          type.qualifiedName?.let { reports += (it to isError) }
        }
      }

      private fun checkMock(node: UElement, type: PsiClass) {
        for (checker in checkers) {
          val reason = checker.checkType(context, slackEvaluator, type)
          if (reason != null) {
            addReport(type, isError = true)
            context.report(checker.issue, context.getLocation(node), reason.reason)
            return
          }
          val disallowedAnnotation = checker.annotations.find { type.hasAnnotation(it) } ?: continue
          addReport(type, isError = true)
          context.report(
            checker.issue,
            context.getLocation(node),
            "Mocked type is annotated with non-mockable annotation $disallowedAnnotation",
          )
          return
        }
        addReport(type, isError = false)
      }
    }
  }

  private fun reportMode(context: Context): MockReportMode {
    return mockReport.getValue(context)?.let { MockReportMode.valueOf(it.uppercase(Locale.US)) }
      ?: MockReportMode.NONE
  }

  override fun afterCheckEachProject(context: Context) {
    if (reportMode(context) != MockReportMode.NONE && reports.isNotEmpty()) {
      val outputFile = context.project.dir.toPath().resolve(MOCK_REPORT_PATH)
      if (outputFile.exists()) {
        outputFile.deleteExisting()
      }
      // TODO use createParentDirectories() when we upgrade to Kotlin 1.9
      outputFile.parent.createDirectories()
      outputFile.createFile()
      outputFile.bufferedWriter().use { writer ->
        writer.write(
          reports
            .sortedBy { it.first }
            .joinToString(prefix = "type,isError\n", separator = "\n") { (type, isError) ->
              "$type,$isError"
            }
        )
      }
    }
    reports.clear()
  }

  interface TypeChecker {
    val issue: Issue

    /** Set of annotation FQCNs that should not be mocked */
    val annotations: Set<String>
      get() = emptySet()

    fun checkType(
      context: JavaContext,
      evaluator: MetadataJavaEvaluator,
      mockedType: PsiClass,
    ): Reason? {
      return null
    }
  }

  /**
   * @property type a [PsiClass] object representing the class that should not be mocked.
   * @property reason The reason this class should not be mocked, which may be as simple as "it is
   *   annotated to forbid mocking" but may also provide a suggested workaround.
   */
  data class Reason(val type: PsiClass, val reason: String)

  /** Represents different modes for generating mock reports. */
  enum class MockReportMode {
    /** The default – no reporting is done. */
    NONE,
    /** Only DoNotMock errors are reported. */
    ERRORS,
    /** All mocked types are reported, even types that are not errors. */
    ALL,
  }
}
