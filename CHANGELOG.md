# Changelog

All notable changes to the ktfmt project (starting on v0.51) should be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/).

## [1.0.0 Unreleased]

### Changed
- All styles managing trailing commas now (https://github.com/facebook/ktfmt/issues/216, https://github.com/facebook/ktfmt/issues/442)


## [Unreleased]

### Changed
- Support guard conditions (https://github.com/facebook/ktfmt/issues/530, https://github.com/facebook/ktfmt/pull/537)
- Update `kotlin-compiler-embeddable` to `2.2.0-Beta2` for forward compatibility with context parameters. (https://github.com/facebook/ktfmt/pull/538)


## [0.53]

### Fixed
- Comments respecting max line width (https://github.com/facebook/ktfmt/pull/511)
- Exception while parsing property accessor on Kotlin 2.0.20-Beta2+ (https://github.com/facebook/ktfmt/pull/513)

## Changed
- Updated Google Java Format to 1.23.0 (https://github.com/facebook/ktfmt/commit/ed949e89eea22843ac10d4fb91685453754abd25)


## [0.52]

### Fixed
- IntelliJ plugin crash (https://github.com/facebook/ktfmt/pull/501)
- Ordering of `@property` and `@param` in KDoc (https://github.com/facebook/ktfmt/pull/498)
- Annotation in return expressions (https://github.com/facebook/ktfmt/issues/497)

### Changed
- KotlinLang style also managing trailing commas (https://github.com/facebook/ktfmt/issues/216, https://github.com/facebook/ktfmt/issues/442)
- Converted IntelliJ plugin to Kotlin (https://github.com/facebook/ktfmt/pull/502)

### Added
- More stability tests (https://github.com/facebook/ktfmt/pull/488)
- Custom profile in plugin settings, mirroring Gradle/Maven plugins (https://github.com/facebook/ktfmt/pull/503)


## [0.51]

### Added
- Created CHANGELOG.md
- Added --help option to CLI (https://github.com/facebook/ktfmt/pull/477)

### Changed
- Preserves blank spaces between when clauses (https://github.com/facebook/ktfmt/issues/342)
- Named the default style as `Formatter.META_FORMAT` / `--meta-style` (https://github.com/facebook/ktfmt/commit/96a7b1e2539eef43044f676f60400d22265fd115)
- `FormattingOptions` constructor parameters order was changed (https://github.com/facebook/ktfmt/commit/520706e6d010d48619781d7113e5b1522f07a2ba)

### Fixed
- Compilation issues with online formatter (https://github.com/facebook/ktfmt/commit/8605080cb0aadb7eaba20f3b469d6ddafe32c941)
- Removing valid semicolons (https://github.com/facebook/ktfmt/issues/459)
- Incorrect detection of unused `assign` import (https://github.com/facebook/ktfmt/issues/411)

### Removed
- **Deleted `Formatter.DROPBOX_FORMAT` / `--dropbox-style` (BREAKING CHANGE)** (https://github.com/facebook/ktfmt/commit/4a393bb8c1156a4a0fd1ab736c02ca8dbd39a969)
- Deleted `FormattingOptions.Style` enum (https://github.com/facebook/ktfmt/commit/7edeff14c3738427e53427eb6e39675dc30d1d05)
