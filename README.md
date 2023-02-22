slack-lints
===========

This repository contains a collection of custom Android/Kotlin lint checks we use in our Android and Kotlin code bases at Slack.

While we do publish artifacts to Maven Central, some of the lints checks may only really be relevant to Slack's codebase. We [develop
these in the open](https://slack.engineering/developing-in-the-open/) to knowledge-share with the community.

## Installation

Add the dependency to the `lintChecks` configuration. Note for non-android projects, you must apply the `com.android.lint` Gradle plugin to use this.

[![Maven Central](https://img.shields.io/maven-central/v/com.slack.lint/slack-lint-checks.svg)](https://mvnrepository.com/artifact/com.slack.lint/slack-lint-checks)

```kotlin
dependencies {
  lintChecks("com.slack.lint:slack-lint-checks:<version>")
}
```

## Overview

### Do Not Mock

The `slack.lint.mocking` package contains several detectors and utilities for detecting mocking
of types that should not be mocked. This is similar to ErrorProne's `DoNotMockChecker` and acts as
an enforcement layer to APIs and classes annotated with `@DoNotMock`. This also detects common types that should never be mocked, such as Kotlin `data` classes or AutoValue classes.

### Inclusivity

In order to write more inclusive code, we have an `InclusiveNamingChecker` tool to check for a
configurable list of non-inclusive names.

### Moshi

`MoshiUsageDetector` contains a wealth of checks for common programmer errors when writing classes
for use with [Moshi](https://github.com/square/moshi) and [MoshiX](https://github.com/ZacSweers/MoshiX).

### Misc

* `JavaOnlyDetector` - detects use of Java-only APIs from Kotlin. Based on the original unreleased implementation in [uber/lint-checks](https://github.com/uber/lint-checks).
* `DaggerKotlinIssuesDetector` - detects some known issues when using [Dagger](https://github.com/google/dagger) in Kotlin code.
* `RetrofitUsageDetector` - detects some common issues when using [Retrofit](https://github.com/square/retrofit).
* `DenyListedApi` – detects use of APIs that just shouldn't be used.
* `MustUseNamedParams` – can be used on functions that should _always_ use named parameters. Useful for APIs that have a lot of parameters and/or may change their order and you want to keep changes source-compatible.
* ...and a plethora of others!

License
--------

    Copyright 2021 Slack Technologies, LLC

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
