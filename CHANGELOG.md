# Changelog

All notable changes to the ktfmt project (starting on v0.51) should be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/).

## [Unreleased]

### Changed
- All styles managing trailing commas now (https://github.com/facebook/ktfmt/issues/216, https://github.com/facebook/ktfmt/issues/442)


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
