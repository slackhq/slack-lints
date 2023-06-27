/*
Copyright 2022 Square, Inc.
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at
   http://www.apache.org/licenses/LICENSE-2.0
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package slack.lint.denylistedapis

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category.Companion.CORRECTNESS
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.LocationType.NAME
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity.ERROR
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.android.tools.lint.detector.api.XmlContext
import com.android.tools.lint.detector.api.XmlScanner
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod
import java.util.EnumSet
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.UImportStatement
import org.jetbrains.uast.ULiteralExpression
import org.jetbrains.uast.UQualifiedReferenceExpression
import org.jetbrains.uast.util.isConstructorCall
import org.w3c.dom.Element
import slack.lint.denylistedapis.DenyListedEntry.Companion.MatchAll

/**
 * Deny-listed APIs that we don't want people to use.
 *
 * Adapted from https://gist.github.com/JakeWharton/1f102d98cd10133b03a5f374540c327a
 */
internal class DenyListedApiDetector : Detector(), SourceCodeScanner, XmlScanner {
  private val config =
    DenyListConfig(
      DenyListedEntry(
        className = "io.reactivex.rxjava3.core.Observable",
        functionName = "hide",
        errorMessage =
          "There should be no reason to defend against downcasting an Observable to " +
            "an implementation type like Relay or Subject in a closed codebase. Doing this incurs " +
            "needless runtime memory and performance overhead. Relays and Subjects both extend from " +
            "Observable and can be supplied to functions accepting Observable directly. When " +
            "returning a Relay or Subject, declare the return type explicitly as Observable " +
            "(e.g., fun foo(): Observable<Foo> = fooRelay)."
      ),
      DenyListedEntry(
        className = "io.reactivex.rxjava3.core.Flowable",
        functionName = "hide",
        errorMessage =
          "There should be no reason to defend against downcasting an Flowable to " +
            "an implementation type like FlowableProcessor in a closed codebase. Doing this incurs " +
            "needless runtime memory and performance overhead. FlowableProcessor extends from " +
            "Flowable and can be supplied to functions accepting Flowable directly. When " +
            "returning a FlowableProcessor, declare the return type explicitly as Flowable " +
            "(e.g., fun foo(): Flowable<Foo> = fooProcessor)."
      ),
      DenyListedEntry(
        className = "io.reactivex.rxjava3.core.Completable",
        functionName = "hide",
        errorMessage =
          "There should be no reason to defend against downcasting a Completable to " +
            "an implementation type like CompletableSubject in a closed codebase. Doing this incurs " +
            "needless runtime memory and performance overhead. CompletableSubject extends from " +
            "Completable and can be supplied to functions accepting Completable directly. When " +
            "returning a CompletableSubject, declare the return type explicitly as Completable " +
            "(e.g., fun foo(): Completable<Foo> = fooSubject)."
      ),
      DenyListedEntry(
        className = "io.reactivex.rxjava3.core.Maybe",
        functionName = "hide",
        errorMessage =
          "There should be no reason to defend against downcasting a Maybe to " +
            "an implementation type like MaybeSubject in a closed codebase. Doing this incurs " +
            "needless runtime memory and performance overhead. MaybeSubject extends from " +
            "Maybe and can be supplied to functions accepting Maybe directly. When " +
            "returning a MaybeSubject, declare the return type explicitly as Maybe " +
            "(e.g., fun foo(): Maybe<Foo> = fooSubject)."
      ),
      DenyListedEntry(
        className = "io.reactivex.rxjava3.core.Single",
        functionName = "hide",
        errorMessage =
          "There should be no reason to defend against downcasting a Single to " +
            "an implementation type like SingleSubject in a closed codebase. Doing this incurs " +
            "needless runtime memory and performance overhead. SingleSubject extends from " +
            "Single and can be supplied to functions accepting Single directly. When " +
            "returning a SingleSubject, declare the return type explicitly as Single " +
            "(e.g., fun foo(): Single<Foo> = fooSubject)."
      ),
      DenyListedEntry(
        className = "androidx.core.content.ContextCompat",
        functionName = "getDrawable",
        parameters = listOf("android.content.Context", "int"),
        errorMessage = "Use Context#getDrawableCompat() instead"
      ),
      DenyListedEntry(
        className = "androidx.core.content.res.ResourcesCompat",
        functionName = "getDrawable",
        parameters = listOf("android.content.Context", "int"),
        errorMessage = "Use Context#getDrawableCompat() instead"
      ),
      DenyListedEntry(
        className = "android.support.test.espresso.matcher.ViewMatchers",
        functionName = "withId",
        parameters = listOf("int"),
        errorMessage =
          "Consider matching the content description instead. IDs are " +
            "implementation details of how a screen is built, not how it works. You can't" +
            " tell a user to click on the button with ID 428194727 so our tests should not" +
            " be doing that. "
      ),
      DenyListedEntry(
        className = "android.view.View",
        functionName = "setOnClickListener",
        parameters = listOf("android.view.View.OnClickListener"),
        arguments = listOf("null"),
        errorMessage =
          "This fails to also set View#isClickable. Use View#clearOnClickListener() instead"
      ),
      DenyListedEntry(
        // If you are deny listing an extension method you need to ascertain the fully qualified
        // name
        // of the class the extension method ends up on.
        className = "kotlinx.coroutines.flow.FlowKt__CollectKt",
        functionName = "launchIn",
        errorMessage =
          "Use the structured concurrent CoroutineScope#launch and Flow#collect " +
            "APIs instead of reactive Flow#onEach and Flow#launchIn. Suspend calls like Flow#collect " +
            "can be refactored into standalone suspend funs and mixed in with regular control flow " +
            "in a suspend context, but calls that invoke CoroutineScope#launch and Flow#collect at " +
            "the same time hide the suspend context, encouraging the developer to continue working in " +
            "the reactive domain."
      ),
      DenyListedEntry(
        className = "androidx.viewpager2.widget.ViewPager2",
        functionName = "setId",
        parameters = listOf("int"),
        arguments = listOf("ViewCompat.generateViewId()"),
        errorMessage =
          "Use an id defined in resources or a statically created instead of generating with ViewCompat.generateViewId(). See https://issuetracker.google.com/issues/185820237"
      ),
      DenyListedEntry(
        className = "androidx.viewpager2.widget.ViewPager2",
        functionName = "setId",
        parameters = listOf("int"),
        arguments = listOf("View.generateViewId()"),
        errorMessage =
          "Use an id defined in resources or a statically created instead of generating with View.generateViewId(). See https://issuetracker.google.com/issues/185820237"
      ),
      DenyListedEntry(
        className = "java.util.LinkedList",
        functionName = "<init>",
        errorMessage =
          "For a stack/queue/double-ended queue use ArrayDeque, for a list use ArrayList. Both are more efficient internally."
      ),
      DenyListedEntry(
        className = "java.util.Stack",
        functionName = "<init>",
        errorMessage = "For a stack use ArrayDeque which is more efficient internally."
      ),
      DenyListedEntry(
        className = "java.util.Vector",
        functionName = "<init>",
        errorMessage =
          "For a vector use ArrayList or ArrayDeque which are more efficient internally."
      ),
      DenyListedEntry(
        className = "io.reactivex.rxjava3.schedulers.Schedulers",
        functionName = "newThread",
        errorMessage =
          "Use a scheduler which wraps a cached set of threads. There should be no reason to be arbitrarily creating threads on Android."
      ),
      // TODO this would conflict with MagicNumber in detekt, revisit
      //      DenyListedEntry(
      //        className = "android.os.Build.VERSION_CODES",
      //        fieldName = MatchAll,
      //        errorMessage =
      //        "No one remembers what these constants map to. Use the API level integer value
      // directly since it's self-defining."
      //      ),
      // TODO we should do this too, but don't currently.
      //    DenyListedEntry(
      //      className = "java.time.Instant",
      //      functionName = "now",
      //      errorMessage = "Use com.squareup.cash.util.Clock to get the time."
      //    ),
      DenyListedEntry(
        className = "kotlinx.coroutines.rx3.RxCompletableKt",
        functionName = "rxCompletable",
        errorMessage =
          "rxCompletable defaults to Dispatchers.Default, which will silently introduce multithreading. Provide an explicit dispatcher. Dispatchers.Unconfined is usually the best choice, as it behaves in an rx-y way.",
        parameters =
          listOf(
            "kotlin.coroutines.CoroutineContext",
            "kotlin.jvm.functions.Function2<? super kotlinx.coroutines.CoroutineScope,? super kotlin.coroutines.Continuation<? super kotlin.Unit>,? extends java.lang.Object>",
          ),
        arguments = listOf("*"),
      ),
      DenyListedEntry(
        className = "kotlinx.coroutines.rx3.RxMaybeKt",
        functionName = "rxMaybe",
        errorMessage =
          "rxMaybe defaults to Dispatchers.Default, which will silently introduce multithreading. Provide an explicit dispatcher. Dispatchers.Unconfined is usually the best choice, as it behaves in an rx-y way.",
        parameters =
          listOf(
            "kotlin.coroutines.CoroutineContext",
            "kotlin.jvm.functions.Function2<? super kotlinx.coroutines.CoroutineScope,? super kotlin.coroutines.Continuation<? super T>,? extends java.lang.Object>",
          ),
        arguments = listOf("*"),
      ),
      DenyListedEntry(
        className = "kotlinx.coroutines.rx3.RxSingleKt",
        functionName = "rxSingle",
        errorMessage =
          "rxSingle defaults to Dispatchers.Default, which will silently introduce multithreading. Provide an explicit dispatcher. Dispatchers.Unconfined is usually the best choice, as it behaves in an rx-y way.",
        parameters =
          listOf(
            "kotlin.coroutines.CoroutineContext",
            "kotlin.jvm.functions.Function2<? super kotlinx.coroutines.CoroutineScope,? super kotlin.coroutines.Continuation<? super T>,? extends java.lang.Object>",
          ),
        arguments = listOf("*"),
      ),
      DenyListedEntry(
        className = "kotlinx.coroutines.rx3.RxObservableKt",
        functionName = "rxObservable",
        errorMessage =
          "rxObservable defaults to Dispatchers.Default, which will silently introduce multithreading. Provide an explicit dispatcher. Dispatchers.Unconfined is usually the best choice, as it behaves in an rx-y way.",
        parameters =
          listOf(
            "kotlin.coroutines.CoroutineContext",
            "kotlin.jvm.functions.Function2<? super kotlinx.coroutines.channels.ProducerScope<T>,? super kotlin.coroutines.Continuation<? super kotlin.Unit>,? extends java.lang.Object>",
          ),
        arguments = listOf("*"),
      ),
    )

  override fun getApplicableUastTypes() = config.applicableTypes()

  override fun createUastHandler(context: JavaContext) = config.visitor(context)

  override fun getApplicableElements() = config.applicableLayoutInflaterElements.keys

  override fun visitElement(context: XmlContext, element: Element) =
    config.visitor(context, element)

  private class DenyListConfig(vararg entries: DenyListedEntry) {
    private class TypeConfig(entries: List<DenyListedEntry>) {
      @Suppress("UNCHECKED_CAST") // Safe because of filter call.
      val functionEntries =
        entries.groupBy { it.functionName }.filterKeys { it != null }
          as Map<String, List<DenyListedEntry>>

      @Suppress("UNCHECKED_CAST") // Safe because of filter call.
      val referenceEntries =
        entries.groupBy { it.fieldName }.filterKeys { it != null }
          as Map<String, List<DenyListedEntry>>
    }

    private val typeConfigs =
      entries.groupBy { it.className }.mapValues { (_, entries) -> TypeConfig(entries) }

    val applicableLayoutInflaterElements =
      entries
        .filter { it.functionName == "<init>" }
        .filter {
          it.arguments == null ||
            it.arguments == listOf("android.content.Context", "android.util.AttributeSet")
        }
        .groupBy { it.className }
        .mapValues { (cls, entries) ->
          entries.singleOrNull() ?: error("Multiple two-arg init rules for $cls")
        }

    fun applicableTypes() =
      listOf<Class<out UElement>>(
        UCallExpression::class.java,
        UImportStatement::class.java,
        UQualifiedReferenceExpression::class.java,
      )

    fun visitor(context: JavaContext) =
      object : UElementHandler() {
        override fun visitCallExpression(node: UCallExpression) {
          val function = node.resolve() ?: return

          val className = function.containingClass?.qualifiedName
          val typeConfig = typeConfigs[className] ?: return

          val functionName =
            if (node.isConstructorCall()) {
              "<init>"
            } else {
              // Kotlin compiler mangles function names that use inline value types as parameters by
              // suffixing them
              // with a hyphen.
              // https://github.com/Kotlin/KEEP/blob/master/proposals/inline-classes.md#mangling-rules
              function.name.substringBefore("-")
            }

          val deniedFunctions =
            typeConfig.functionEntries.getOrDefault(functionName, emptyList()) +
              typeConfig.functionEntries.getOrDefault(MatchAll, emptyList())

          deniedFunctions.forEach { denyListEntry ->
            if (denyListEntry.allowInTests && context.isTestSource) {
              return@forEach
            } else if (
              denyListEntry.parametersMatchWith(function) && denyListEntry.argumentsMatchWith(node)
            ) {
              context.report(
                issue = ISSUE,
                location = context.getLocation(node),
                message = denyListEntry.errorMessage
              )
            }
          }
        }

        override fun visitImportStatement(node: UImportStatement) {
          val reference = node.resolve() as? PsiField ?: return
          visitField(reference, node)
        }

        override fun visitQualifiedReferenceExpression(node: UQualifiedReferenceExpression) {
          val reference = node.resolve() as? PsiField ?: return
          visitField(reference, node)
        }

        private fun visitField(reference: PsiField, node: UElement) {
          val className = reference.containingClass?.qualifiedName
          val typeConfig = typeConfigs[className] ?: return

          val referenceName = reference.name
          val deniedFunctions =
            typeConfig.referenceEntries.getOrDefault(referenceName, emptyList()) +
              typeConfig.referenceEntries.getOrDefault(MatchAll, emptyList())

          deniedFunctions.forEach { denyListEntry ->
            if (denyListEntry.allowInTests && context.isTestSource) {
              return@forEach
            }
            context.report(
              issue = ISSUE,
              location = context.getLocation(node),
              message = denyListEntry.errorMessage
            )
          }
        }
      }

    fun visitor(context: XmlContext, element: Element) {
      val denyListEntry = applicableLayoutInflaterElements.getValue(element.tagName)
      context.report(
        issue = ISSUE,
        location = context.getLocation(element, type = NAME),
        message = denyListEntry.errorMessage,
      )
    }

    private fun DenyListedEntry.parametersMatchWith(function: PsiMethod): Boolean {
      val expected = parameters
      val actual = function.parameterList.parameters.map { it.type.canonicalText }

      return when {
        expected == null -> true
        expected.isEmpty() && actual.isEmpty() -> true
        expected.size != actual.size -> false
        else -> expected == actual
      }
    }

    private fun DenyListedEntry.argumentsMatchWith(node: UCallExpression): Boolean {
      // "arguments" being null means we don't care about this check and it should just return true.
      val expected = arguments ?: return true
      val actual = node.valueArguments

      return when {
        expected.size != actual.size -> false
        else ->
          expected.zip(actual).all { (expectedValue, actualValue) ->
            argumentMatches(expectedValue, actualValue)
          }
      }
    }

    private fun argumentMatches(expectedValue: String, actualValue: UExpression): Boolean {
      if (expectedValue == "*") return true
      val renderString =
        (actualValue as? ULiteralExpression)?.asRenderString()
          ?: (actualValue as? UQualifiedReferenceExpression)
            ?.asRenderString() // Helps to match against static method params
      // 'Class.staticMethod()'.
      if (expectedValue == renderString) return true

      return false
    }
  }

  companion object {
    val ISSUE =
      Issue.create(
        id = "DenyListedApi",
        briefDescription = "Deny-listed API",
        explanation =
          "This lint check flags usages of APIs in external libraries that we prefer not to use.",
        category = CORRECTNESS,
        priority = 5,
        severity = ERROR,
        implementation =
          Implementation(
            DenyListedApiDetector::class.java,
            EnumSet.of(Scope.JAVA_FILE, Scope.RESOURCE_FILE, Scope.TEST_SOURCES),
            EnumSet.of(Scope.JAVA_FILE),
            EnumSet.of(Scope.RESOURCE_FILE),
            EnumSet.of(Scope.TEST_SOURCES),
          )
      )
  }
}

data class DenyListedEntry(
  val className: String,
  /** The function name to match, [MatchAll] to match all functions, or null if matching a field. */
  val functionName: String? = null,
  /** The field name to match, [MatchAll] to match all fields, or null if matching a function. */
  val fieldName: String? = null,
  /** Fully-qualified types of function parameters to match, or null to match all overloads. */
  val parameters: List<String>? = null,
  /** Argument expressions to match at the call site, or null to match all invocations. */
  val arguments: List<String>? = null,
  val errorMessage: String,
  /**
   * Option to allow this issue in tests. Should _only_ be reserved for invocations that make sense
   * in tests.
   */
  val allowInTests: Boolean = false,
) {
  init {
    require((functionName == null) xor (fieldName == null)) {
      "One of functionName or fieldName must be set"
    }
  }

  companion object {
    const val MatchAll = "*"
  }
}
