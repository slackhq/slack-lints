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

import com.android.tools.lint.client.api.IssueRegistry
import com.android.tools.lint.client.api.Vendor
import com.android.tools.lint.detector.api.CURRENT_API
import com.android.tools.lint.detector.api.Issue
import com.google.auto.service.AutoService
import slack.lint.inclusive.InclusiveNamingChecker
import slack.lint.mocking.AutoValueMockDetector
import slack.lint.mocking.DataClassMockDetector
import slack.lint.mocking.DoNotMockMockDetector
import slack.lint.mocking.ErrorProneDoNotMockDetector
import slack.lint.retrofit.RetrofitUsageDetector
import slack.lint.rx.RxSubscribeOnMainDetector

@AutoService(IssueRegistry::class)
class SlackIssueRegistry : IssueRegistry() {

  override val vendor: Vendor = Vendor(vendorName = "slack", identifier = "slack-lint")

  override val api: Int = CURRENT_API

  @Suppress("SpreadOperator")
  override val issues: List<Issue> = listOf(
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
  )
}
