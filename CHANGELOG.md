Changelog
=========

0.7.0
-----

_2023-10-27_

- Lower lint API back to `31.3.0-alpha05` as newer versions targeted kotlin 1.9.20 betas without us realizing it.
- Improve explanation for sealed class mock detector to mention that Mockito can't mock them at all in Java 17+.
- Promote `PlatformTypeMockDetector` to error severity.
- Make `DenyListedApi` entries more configurable. Initial change is that blocking APIs are now reported with the ID `DenyListedBlockingApi`.
- Support multiple mock report modes for the `mock-report` option. Modes are `NONE`, `ERRORS`, and `ALL`. Default is `NONE`. Now the report file is `build/reports/mockdetector/mock-report.csv` and the second column is the severity. This allows reporting all mocks for extra analysis.

0.6.1
-----

_2023-10-09_

- **Enhancement**: Add `mock-report` option to `MockDetector`s to generate a report of all mocked types in a project.
- Update to lint `31.3.0-alpha07`.

0.6.0
-----

_2023-09-28_

- **New**: Add `ExceptionMessage` check that ensures that calls to `check`, `checkNotNull`, `require`, and `requireNotNull` functions always include a message.
- **Enhancement**: Add support for custom mock factories and mock annotations to `MockDetector`.
  - `mock-annotations` is a comma-separated list of mock annotations' fully-qualified names. Default is `org.mockito.Mock,org.mockito.Spy`.
  - `mock-factories` is a comma-separated list of mock factories (i.e. `org.mockito.Mockito#methodName`). Default is `org.mockito.Mockito#mock,org.mockito.Mockito#spy,slack.test.mockito.MockitoHelpers#mock,slack.test.mockito.MockitoHelpersKt#mock`.
- Update lint to `31.3.0-alpha05`.

Special thanks to [@SimonMarquis](https://github.com/SimonMarquis) for contributing to this release!

0.5.1
-----

_2023-09-09_

- **Fix**: Allow `@Provides` in companion objects of `@Module` classes.

0.5.0
-----

_2023-09-08_

- **New**: Add a bunch more checks around correct usage of Dagger `@Binds` and `@Provides` methods.
- **Fix**: Remove `BindsCanBeExtensionFunction` lint as this is prohibited now in Dagger 2.48+.
- Update to lint `31.3.0-alpha03`.
- Update to Kotlin `1.9.10`.

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
