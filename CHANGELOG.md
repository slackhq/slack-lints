Changelog
=========

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
