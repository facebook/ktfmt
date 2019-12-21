[![](https://github.com/facebookincubator/ktfmt/workflows/build/badge.svg)](https://github.com/facebookincubator/ktfmt/actions?query=workflow%3Abuild)

# ktfmt

`ktfmt` is a program that pretty-prints (formats) Kotlin code, based on [google-java-format](https://github.com/google/google-java-format).

**Note** that `ktfmt` still has some rough edges which we're constantly working on fixing.

## Demo

|Before Formatting| Formatted by `ktfmt`| 
| ---- | ---- |
| ![Original](docs/images/before.png) | ![ktfmt](docs/images/ktfmt.png) |

For comparison, this is how the same code is formatted by [`ktlint`](https://github.com/pinterest/ktlint) and IntelliJ:

| Formatted by `ktlint`|Formatted by IntelliJ|
| ------ | --------|
| ![ktlint](docs/images/ktlint.png) | ![IntelliJ](docs/images/intellij.png) |



# Using on the command-line

* Make sure the `vendor/google-java/format` submodule is populated. Either clone with submodules (`git pull --recurse-submodules https://github.com/facebookincubator/ktfmt.git`) or populate the submodule after cloning (`git submodule update --init`)
* Run `mvn install`
* Run `java -jar core/target/ktfmt-0.1-SNAPSHOT-jar-with-dependencies.jar`

# FAQ

## `ktfmt` uses a 2-space indent; why not 4?

Two reasons -
1. Many of our projects use a mixture of Kotlin and Java, and we found the back-and-forth in styles to be distracting.
2. From a pragmatic standpoint, the formatting engine behind google-java-format uses more whitespace and newlines than other formatters. Using an indentation of 4 spaces quickly reaches the maximal column width.

# Developer's Guide

## Setup

* Make sure the `vendor/google-java/format` submodule is populated. Either clone with submodules (`git pull --recurse-submodules https://github.com/facebookincubator/ktfmt.git`) or populate the submodule after cloning (`git submodule update --init`)
* Open `pom.xml` in IntelliJ. Choose "Open as a Project"
* The IntelliJ project will unfortunately be broken on import. To fix,
    * Turn off ErrorProne by removing the compiler parameters in IntelliJ at the bottom of "Settings -> Build, Execution, Deployment -> Compiler -> Java Compiler" (see https://github.com/google/google-java-format/issues/417)
    * Right click on "vendor/google-java-format/pom.xml" and choose "Add maven project"    

## Development

* Currently, we mainly develop by adding tests to `FormatterKtTest.kt`.

# License

Apache License 2.0
