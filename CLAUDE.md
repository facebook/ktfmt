# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this repo is

A fork of [facebook/ktfmt](https://github.com/facebook/ktfmt) (Kotlin code formatter built on
google-java-format's engine). The fork's reason to exist is the **Klimat style**
(`--klimat-style`, `Formatter.KLIMAT_FORMAT`): kotlinlang style (4-space indents) plus
`glueBlockLikeToOperator` (multiline block-like expressions stay glued to `=`/`by`),
`preserveChainBreaks` (user-authored line breaks in call chains survive), and `ONLY_ADD`
trailing-comma management. `upstream` remote points at facebook/ktfmt; keep diffs against
upstream minimal outside klimat-specific code.

End-user setup docs (Android Studio, pre-commit hook, CI) live in `docs/klimat-setup.md` (Russian).

## Modules

Gradle project names do not match directory names:

- `core/` = `:ktfmt` — the formatter library + CLI (`com.facebook.ktfmt.cli.Main`)
- `ktfmt_idea_plugin/` = `:idea_plugin` — IntelliJ/Android Studio plugin
- `online_formatter/` = `:lambda` — AWS lambda for upstream's playground (unused by this fork's releases)
- `buildSrc/` — convention plugins: generates `Ktfmt.kt` (version constant from `gradle.properties`) and configures GraalVM native-image

## Commands

```sh
./gradlew build                      # everything: compile, tests, ktfmtCheck, ABI check
./gradlew :ktfmt:test                # core tests
./gradlew :ktfmt:test --tests "com.facebook.ktfmt.format.KlimatStyleFormatterTest"   # one class
./gradlew ktfmtFormat                # format the repo with the in-tree formatter (Meta style)
./gradlew ktfmtCheck                 # check formatting; every module's `check` depends on this
./gradlew :ktfmt:updateKotlinAbi     # regenerate core/api/ktfmt.api after public API changes
./gradlew :ktfmt:shadowJar           # CLI fat jar -> core/build/libs/ktfmt-<v>-with-dependencies.jar
./gradlew :idea_plugin:buildPlugin   # plugin zip -> ktfmt_idea_plugin/build/distributions/
./gradlew :ktfmt:nativeCompile       # GraalVM native binary (needs GRAALVM_HOME); then ./native_smoke_test.sh
```

Gotchas:

- The repo is formatted by its own formatter; run `./gradlew ktfmtFormat` before committing or
  CI fails on `ktfmtCheck`.
- Any change to `core`'s public API fails `checkKotlinAbi` until you run `updateKotlinAbi` and
  commit the regenerated `core/api/ktfmt.api`.
- Core tests deliberately run with `-Dfile.encoding=UTF-16` to catch encoding assumptions.
- `core/src/main/java/com/facebook/ktfmt/util/kotlin-2.X/` holds per-Kotlin-version
  `CompatibilityUtils.kt`; only the directory matching the Kotlin version in the version catalog
  is compiled. Bumping Kotlin may require adding a new directory.

## Architecture

Formatting pipeline (`core`, entry point `Formatter.format(options, code)`):

1. `Parser` parses the source with the embedded Kotlin compiler into PSI.
2. Cleanup passes on the whole file: import sorting/dedup (`RedundantImportDetector`),
   redundant-element removal (`RedundantSemicolonDetector`), trailing-comma addition
   (`TrailingCommas`, re-run in a loop with pretty-printing so an inserted comma can't overflow a line).
3. Pretty-printing: `KotlinInput`/`Tokenizer` produce tokens; `KotlinInputAstVisitor` walks the
   PSI and emits Ops into google-java-format's `OpsBuilder`/Doc model, which performs the actual
   line-breaking against `FormattingOptions.maxWidth`. All layout decisions (including the klimat
   options) live in `KotlinInputAstVisitor`.
4. KDoc comments are formatted by the separate `kdoc/` package; multiline strings by
   `MultilineStringFormatter`.

Styles are `FormattingOptions` presets on `Formatter`: `META_FORMAT` (default), `GOOGLE_FORMAT`,
`KOTLINLANG_FORMAT`, `KLIMAT_FORMAT`. The CLI flag mapping is in `cli/ParsedArgs.kt`.

IDEA plugin (`:idea_plugin`): `KtfmtFormattingService` (an `AsyncDocumentFormattingService`)
replaces the IDE formatter for Kotlin files and calls `Formatter.format` in-process — the plugin
bundles this repo's core, so core changes need a plugin rebuild to reach the IDE.
`UiFormatterStyle` maps the settings dropdown to the presets; adding a preset means touching that
enum plus the combobox in `KtfmtConfigurable`. The `Custom` style only round-trips the fields
stored in `KtfmtSettings` — it does not carry `glueBlockLikeToOperator`/`preserveChainBreaks`, so
Klimat is only reachable via its dedicated dropdown entry.

Tests use JUnit4 + Google Truth. Formatter behavior tests assert on whole code blocks
(`assertFormatted` / `assertThatFormatting` helpers in `core/src/test/.../format/`); klimat
behavior is covered in `KlimatStyleFormatterTest.kt`.

## Releases (fork-specific)

Upstream's Maven Central / JetBrains Marketplace publishing is removed. Creating a GitHub Release
with a tag like `v0.65-klimat.N` triggers `.github/workflows/publish_artifacts_on_release.yaml`,
which derives the version from the tag (`-Pktfmt.version=`) and attaches to the release: the CLI
jars, GraalVM native binaries (linux x86_64/aarch64 and darwin aarch64, smoke-tested), and the IDE plugin
zip (installed manually via "Install Plugin from Disk"). Local builds default to the
`-SNAPSHOT` version in `gradle.properties`. CI (`build_and_test.yml`) runs `./gradlew build` on
Java 17 and 21 plus a native-image build and smoke test.
