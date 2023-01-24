// Copyright (C) 2021 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint

import com.android.tools.lint.client.api.IssueRegistry
import com.android.tools.lint.client.api.Vendor
import com.android.tools.lint.detector.api.CURRENT_API
import com.android.tools.lint.detector.api.Issue
import com.google.auto.service.AutoService
import slack.lint.compose.ComposeModifierMissingDetector
import slack.lint.compose.CompositionLocalNamingDetector
import slack.lint.compose.CompositionLocalUsageDetector
import slack.lint.compose.ContentEmitterReturningValuesDetector
import slack.lint.denylistedapis.DenyListedApiDetector
import slack.lint.eithernet.DoNotExposeEitherNetInRepositoriesDetector
import slack.lint.inclusive.InclusiveNamingChecker
import slack.lint.mocking.AutoValueMockDetector
import slack.lint.mocking.DataClassMockDetector
import slack.lint.mocking.DoNotMockMockDetector
import slack.lint.mocking.ErrorProneDoNotMockDetector
import slack.lint.resources.FullyQualifiedResourceDetector
import slack.lint.resources.MissingResourceImportAliasDetector
import slack.lint.resources.WrongResourceImportAliasDetector
import slack.lint.retrofit.RetrofitUsageDetector
import slack.lint.rx.RxSubscribeOnMainDetector
import slack.lint.text.SpanMarkPointMissingMaskDetector

@AutoService(IssueRegistry::class)
class SlackIssueRegistry : IssueRegistry() {

  override val vendor: Vendor = Vendor(vendorName = "slack", identifier = "slack-lint")

  override val api: Int = CURRENT_API
  override val minApi: Int = 12 // 7.2.0-beta02

  @Suppress("SpreadOperator")
  override val issues: List<Issue> =
    listOf(
      *ViewContextDetector.issues,
      *ArgInFormattedQuantityStringResDetector.issues,
      *DaggerKotlinIssuesDetector.issues,
      *NonKotlinPairDetector.issues,
      DoNotCallProvidersDetector.ISSUE,
      *InclusiveNamingChecker.ISSUES,
      DeprecatedAnnotationDetector.ISSUE_DEPRECATED_CALL,
      DeprecatedSqlUsageDetector.ISSUE,
      JavaOnlyDetector.ISSUE,
      SerializableDetector.ISSUE,
      RawDispatchersUsageDetector.ISSUE,
      MainScopeUsageDetector.ISSUE,
      RxSubscribeOnMainDetector.ISSUE,
      *GuavaPreconditionsDetector.issues,
      DataClassMockDetector.ISSUE,
      AutoValueMockDetector.ISSUE,
      DoNotMockMockDetector.ISSUE,
      ErrorProneDoNotMockDetector.ISSUE,
      *MoshiUsageDetector.issues(),
      *FragmentDaggerFieldInjectionDetector.issues,
      *InjectWithUsageDetector.ISSUES,
      *RedactedUsageDetector.ISSUES,
      InjectInJavaDetector.ISSUE,
      RetrofitUsageDetector.ISSUE,
      RestrictCallsToDetector.ISSUE,
      SpanMarkPointMissingMaskDetector.ISSUE,
      DoNotExposeEitherNetInRepositoriesDetector.ISSUE,
      FullyQualifiedResourceDetector.ISSUE,
      MissingResourceImportAliasDetector.ISSUE,
      WrongResourceImportAliasDetector.ISSUE,
      DenyListedApiDetector.ISSUE,
      CompositionLocalUsageDetector.ISSUE,
      CompositionLocalNamingDetector.ISSUE,
      ContentEmitterReturningValuesDetector.ISSUE,
      ComposeModifierMissingDetector.ISSUE,
    )
}
