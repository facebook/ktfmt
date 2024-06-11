# Changelog

All notable changes to the ktfmt project (starting on v0.51) should be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/).

## [Unreleased]

### Added
- Created CHANGELOG.md

### Changed
- Preserves blank spaces between when clauses (https://github.com/facebook/ktfmt/issues/342)
- Named the default style as `Formatter.META_FORMAT` / `--meta-style`

### Fixed
- Compilation issues with online formatter (https://github.com/facebook/ktfmt/commit/8605080cb0aadb7eaba20f3b469d6ddafe32c941)

### Removed
- **Deleted `Formatter.DROPBOX_FORMAT` / `--dropbox-style` (BREAKING CHANGE)**
- Deleted `FormattingOptions.Style` enum
