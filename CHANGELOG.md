Changelog
=========

0.4.0
-----

_2023-07-20_

- **New**: Denylist blocking RxJava 3 operators and coroutines' `runBlocking` in favor of `TestObserver` and `runTest`/Turbine.
- **New**: Denylist coroutines' `runCatching`.
- **New**: Denylist `java.util.Date`, `java.text.DateFormat`, and `java.text.SimpleDateFormat` in favor of `java.time.*`/`kotlin.time.*`/etc APIs.
- **Enhancement**: Specifically report denylisted function name only in lint report, not the whole call expression.
- **Enhancement**: Update kotlinx-metadata to `0.7.0` to better support Kotlin 2.0.
- Update to lint `31.2.0-alpha13` (lint API `14`).

0.3.0
-----

_2023-05-31_

- **New**: Use kotlinx-metadata to parse `Metadata` annotations of `PsiCompiledElement` types to better handle Kotlin language features. Currently used in mock checks and Moshi checks. Please star this issue: https://issuetracker.google.com/issues/283654244.
- **New**: Add DoNotMock check for `object` types.
- **New**: Add DoNotMock check for `sealed` types. Subtypes should be used instead.
- **New**: Add DoNotMock check for `record` types. Same motivation as data classes.
- **New**: Add DoNotMock check for platform types (e.g. `java.*`, `kotlin.*`, `android.*`, their `*x.*` variants). Prefer real implementations or fakes instead.
  - This is a big change so this one is just a warning for now.
- **Enhancement**: `MockDetector` revamp. All mock checks now run within the same detector to better utilize metadata catching.
- **Enhancement**: Improve mock check location reporting.
- **Enhancement**: Improve mock check messages to specify the erroring type.
- **Enhancement**: Add `reason` properties to `@KotlinOnly`/`@JavaOnly` annotations.
- **Enhancement**: Add more information to the `Vendor` details.
- Raise min lint API to `14`.
- Update kotlin to `1.8.21`. Updated language version to this too to match lint.
- Update lint to `31.2.0-alpha06`.

0.2.3
-----

_2023-02-22_

- **New**: `ParcelizeFunctionProperty` check that errors when a `@Parcelize` class has a function property.

0.2.2
-----

_2023-02-09_

- **Removed**: Compose lints have been removed and published in a separate project: https://github.com/slackhq/compose-lints

0.2.1
-----

_2023-01-26_

- **Fix**: Improve and fix a number of explanation string formatting in the new compose lints.

0.2.0
-----

_2023-01-25_

- **New**: Ported most of the Twitter [compose-rules](https://github.com/twitter/compose-rules) checks to lint. We're packaging them in this project right now, but will likely publish them from a separate repo in the future.
- Target lint-api `31.1.0-alpha01`.
- Update to Kotlin API version `1.7`. Lint `8.1.0-alpha01` or later is now required.
- Modernize various build infra (Kotlin `1.8.0`, JDK 19, Gradle 7.6).

0.1.1
-----

_2022-11-30_

- **Fix**: Fallback to file package name in `MissingResourceImportAliasDetector` if project package name is null.

0.1.0
-----

_2022-11-17_

* Initial release on maven central.
