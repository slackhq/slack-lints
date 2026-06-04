// Copyright (C) 2021 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint

import com.android.tools.lint.client.api.IssueRegistry
import com.android.tools.lint.client.api.Vendor
import com.android.tools.lint.detector.api.CURRENT_API
import com.android.tools.lint.detector.api.Issue
import com.google.auto.service.AutoService
import slack.lint.complexity.CyclomaticComplexMethodDetector
import slack.lint.complexity.LongMethodDetector
import slack.lint.complexity.LongParameterListDetector
import slack.lint.complexity.NestedBlockDepthDetector
import slack.lint.complexity.TooManyFunctionsDetector
import slack.lint.denylistedapis.DenyListedApiDetector
import slack.lint.eithernet.DoNotExposeEitherNetInRepositoriesDetector
import slack.lint.exceptions.SwallowedExceptionDetector
import slack.lint.exceptions.ThrowingExceptionsWithoutMessageDetector
import slack.lint.exceptions.TooGenericExceptionCaughtDetector
import slack.lint.exceptions.TooGenericExceptionThrownDetector
import slack.lint.inclusive.InclusiveNamingChecker
import slack.lint.mocking.ErrorProneDoNotMockDetector
import slack.lint.mocking.MockDetector
import slack.lint.naming.FunctionNamingDetector
import slack.lint.naming.InvalidPackageDeclarationDetector
import slack.lint.naming.TopLevelPropertyNamingDetector
import slack.lint.parcel.ParcelizeFunctionPropertyDetector
import slack.lint.resources.FullyQualifiedResourceDetector
import slack.lint.resources.MissingResourceImportAliasDetector
import slack.lint.resources.WrongResourceImportAliasDetector
import slack.lint.retrofit.RetrofitUsageDetector
import slack.lint.rx.RxObservableEmitDetector
import slack.lint.rx.RxSubscribeOnMainDetector
import slack.lint.style.FunctionOnlyReturningConstantDetector
import slack.lint.style.MagicNumberDetector
import slack.lint.style.ReturnCountDetector
import slack.lint.style.UnderscoresInNumericLiteralsDetector
import slack.lint.style.UnnecessaryAbstractClassDetector
import slack.lint.style.UnnecessaryBackticksDetector
import slack.lint.style.UnusedPrivateMemberDetector
import slack.lint.style.VarCouldBeValDetector
import slack.lint.text.SpanMarkPointMissingMaskDetector
import slack.lint.ui.DoNotCallViewToString
import slack.lint.ui.ItemDecorationViewBindingDetector

@AutoService(IssueRegistry::class)
class SlackIssueRegistry : IssueRegistry() {

  override val vendor: Vendor =
    Vendor(
      vendorName = "slack",
      identifier = "slack-lint",
      feedbackUrl = "https://github.com/slackhq/slack-lints",
      contact = "https://github.com/slackhq/slack-lints",
    )

  override val api: Int = CURRENT_API
  override val minApi: Int = CURRENT_API

  override val issues: List<Issue> = buildList {
    addAll(ViewContextDetector.issues)
    addAll(ArgInFormattedQuantityStringResDetector.issues)
    addAll(DaggerIssuesDetector.ISSUES)
    addAll(NonKotlinPairDetector.issues)
    add(DoNotCallProvidersDetector.ISSUE)
    addAll(InclusiveNamingChecker.ISSUES)
    add(DeprecatedAnnotationDetector.ISSUE_DEPRECATED_CALL)
    add(DeprecatedSqlUsageDetector.ISSUE)
    add(JavaOnlyDetector.ISSUE)
    add(SerializableDetector.ISSUE)
    add(RawDispatchersUsageDetector.ISSUE)
    add(MainScopeUsageDetector.ISSUE)
    add(RxSubscribeOnMainDetector.ISSUE)
    addAll(RxObservableEmitDetector.issues)
    addAll(GuavaPreconditionsDetector.issues)
    addAll(MockDetector.ALL_ISSUES)
    add(ErrorProneDoNotMockDetector.ISSUE)
    addAll(MoshiUsageDetector.issues())
    addAll(FragmentDaggerFieldInjectionDetector.issues)
    addAll(RedactedUsageDetector.ISSUES)
    add(InjectInJavaDetector.ISSUE)
    add(RetrofitUsageDetector.ISSUE)
    add(RestrictCallsToDetector.ISSUE)
    add(SpanMarkPointMissingMaskDetector.ISSUE)
    add(DoNotExposeEitherNetInRepositoriesDetector.ISSUE)
    add(FullyQualifiedResourceDetector.ISSUE)
    add(MissingResourceImportAliasDetector.ISSUE)
    add(WrongResourceImportAliasDetector.ISSUE)
    addAll(DenyListedApiDetector.ISSUES)
    add(ParcelizeFunctionPropertyDetector.ISSUE)
    add(ExceptionMessageDetector.ISSUE)
    add(TestParameterSiteTargetDetector.ISSUE)
    add(MustUseNamedParamsDetector.ISSUE)
    add(NotNullOperatorDetector.ISSUE)
    add(DoNotCallViewToString.ISSUE)
    add(ItemDecorationViewBindingDetector.ISSUE)
    add(NullableConcurrentHashMapDetector.ISSUE)
    add(CircuitScreenDataClassDetector.ISSUE)
    add(JsonInflaterMoshiCompatibilityDetector.ISSUE)
    // Detekt replacement rules
    add(ReturnCountDetector.ISSUE)
    add(UnnecessaryBackticksDetector.ISSUE)
    add(UnderscoresInNumericLiteralsDetector.ISSUE)
    add(FunctionNamingDetector.ISSUE)
    add(TopLevelPropertyNamingDetector.ISSUE)
    add(InvalidPackageDeclarationDetector.ISSUE)
    add(SwallowedExceptionDetector.ISSUE)
    add(ThrowingExceptionsWithoutMessageDetector.ISSUE)
    add(TooGenericExceptionCaughtDetector.ISSUE)
    add(TooGenericExceptionThrownDetector.ISSUE)
    add(LongMethodDetector.ISSUE)
    add(LongParameterListDetector.ISSUE)
    add(NestedBlockDepthDetector.ISSUE)
    add(TooManyFunctionsDetector.ISSUE)
    add(CyclomaticComplexMethodDetector.ISSUE)
    add(MagicNumberDetector.ISSUE)
    add(VarCouldBeValDetector.ISSUE)
    add(UnnecessaryAbstractClassDetector.ISSUE)
    add(UnusedPrivateMemberDetector.ISSUE)
    add(FunctionOnlyReturningConstantDetector.ISSUE)
  }
}
