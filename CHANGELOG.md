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
- Named the default style as `Formatter.META_FORMAT` / `--meta-style`
- `FormattingOptions` constructor parameters order was changed

### Fixed
- Compilation issues with online formatter (https://github.com/facebook/ktfmt/commit/8605080cb0aadb7eaba20f3b469d6ddafe32c941)
- Removing valid semicolons (https://github.com/facebook/ktfmt/issues/459)
- Incorrect detection of unused `assign` import (https://github.com/facebook/ktfmt/issues/411)

### Removed
- **Deleted `Formatter.DROPBOX_FORMAT` / `--dropbox-style` (BREAKING CHANGE)**
- Deleted `FormattingOptions.Style` enum
