/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Convention plugin (`id("ktfmt.native-image")`) owning ktfmt's GraalVM Native Image config: image
// flags, GC/arch knobs, and PGO. The native source set, helper tasks, and native deps stay in
// core/build.gradle.kts (they use the version catalog); this plugin references them lazily.

import java.nio.file.Paths
import org.graalvm.buildtools.gradle.dsl.GraalVMExtension
import org.gradle.api.tasks.bundling.Jar

plugins { id("org.graalvm.buildtools.native") }

// Entry point for the native binary. Set directly on the GraalVM binary below; we intentionally do
// not apply the `application` plugin, which would only add unused `run`/`distZip`/`distTar` tasks.
val entrypoint = "com.facebook.ktfmt.cli.Main"

object DefaultArchitectureTarget {
  // x86-64 family (a.k.a. amd64). `v3` = ~2013+ CPUs (AVX2). Avoid `v4`: it needs AVX-512, which
  // many cloud/CI hosts lack and would SIGILL on.
  val x86_64 = "x86-64-v3"
  // AArch64 family (a.k.a. arm64). `armv8-a` is the ARMv8.0 baseline that runs on all AArch64
  // hardware. native-image only accepts `armv8-a`, `armv8.1-a`, `compatibility`, or `native` here.
  val arm64 = "armv8-a"
}

// Pass `-Pktfmt.native.release=true` to enable release mode for Native Image.
val nativeRelease = findProperty("ktfmt.native.release") == "true"

// Pass `-Pktfmt.native.target=xx` to pass `-march=xx` to Native Image. Defaults favor broad CPU
// compatibility over peak performance; pass `native` to target this exact machine.
val nativeTarget =
    findProperty("ktfmt.native.target")
        ?: when (val hostArch = System.getProperty("os.arch")) {
          "amd64",
          "x86_64" -> DefaultArchitectureTarget.x86_64
          "aarch64",
          "arm64" -> DefaultArchitectureTarget.arm64
          else -> error("Unrecognized host architecture: '$hostArch'")
        }

// Pass `-Pktfmt.native.gc=xx` to select a garbage collector; options include `serial`, `G1`, and
// `epsilon`. Defaults to `serial` because:
// - it's the best fit for ktfmt's short-lived CLI runs
// - it's the only GC available in GraalVM Community Edition
// - it's the only GC supported by `native-image` on macOS
// - avoids the Oracle GraalVM licensing considerations of `G1`. Anyone that needs this can do it
// from source and opt into `G1` for large, long-running batch formatting on Linux with Oracle
// GraalVM.
val nativeGc = findProperty("ktfmt.native.gc") ?: "serial"

// Pass `-Pktfmt.native.debug=true` to build the Native Image binary with debug info.
val nativeDebug = findProperty("ktfmt.native.debug") == "true"

// Pass `-Pktfmt.native.lto=true` to enable LTO for the Native Image binary.
val enableLto = findProperty("ktfmt.native.lto") == "true"

// Pass `-Pktfmt.native.muslHome=xx` or set MUSL_HOME to point to the Musl sysroot when building for
// Musl Libc.
val muslSysroot = (findProperty("ktfmt.native.muslHome") ?: System.getenv("MUSL_HOME"))?.toString()

// Pass `-Pktfmt.native.musl=true` to build a fully-static binary against Musl Libc.
val preferMusl =
    (findProperty("ktfmt.native.musl") == "true").also { preferMusl ->
      require(!preferMusl || muslSysroot != null) {
        "When `ktfmt.native.musl` is true, -Pktfmt.native.muslHome or MUSL_HOME must be set to the Musl sysroot. " +
            "See https://www.graalvm.org/latest/reference-manual/native-image/guides/build-static-executables/"
      }
    }

// Pass `-Pktfmt.native.smol=true` to build a small, instead of a fast, binary.
val preferSmol = (findProperty("ktfmt.native.smol") == "true")

// Pass `-Pktfmt.native.opt=s` to pass e.g. `-Os` to Native Image.
val nativeOpt =
    when (val opt = findProperty("ktfmt.native.opt")) {
      null ->
          when {
            preferSmol -> "s"
            nativeRelease -> "3"
            else -> "b" // prefer build speed
          }
      else -> opt
    }

// PGO profiles live in `src/main/native-image/profiles`. They are git-ignored (see `.gitignore`)
// and are generated via `pgo_train.sh` or materialized in CI, so they may be absent. Only profiles
// that actually exist on disk are used; a missing profile degrades to a non-PGO build instead of
// failing `native-image` with a file-not-found.
val existingPgoProfiles =
    listOf("default.iprof")
        .map { profileName ->
          layout.projectDirectory
              .file(Paths.get("src", "main", "native-image", "profiles", profileName).toString())
              .asFile
        }
        .filter { it.exists() }

val pgoProfileArgs =
    if (existingPgoProfiles.isEmpty()) emptyList()
    else listOf("--pgo=${existingPgoProfiles.joinToString(",") { it.absolutePath }}")

// Pass `-Pktfmt.native.pgo=true` to build with PGO; pass `train` to enable instrumentation.
val pgoArgs =
    when (val pgo = findProperty("ktfmt.native.pgo")) {
      null -> if (nativeRelease) pgoProfileArgs else emptyList()
      "true" -> pgoProfileArgs
      "false" -> emptyList()
      "train" -> listOf("--pgo-instrument")
      else -> error("Unrecognized `ktfmt.native.pgo` argument: '$pgo'")
    }

// Warn (rather than fail) when PGO was requested but no profiles are available.
if (
    (nativeRelease || findProperty("ktfmt.native.pgo") == "true") && existingPgoProfiles.isEmpty()
) {
  logger.warn(
      "[ktfmt native] PGO was requested but no profiles were found in " +
          "src/main/native-image/profiles; building without PGO. Generate one with `bash pgo_train.sh`."
  )
}

configure<GraalVMExtension> {
  binaries {
    named("main") {
      imageName.set("ktfmt")
      mainClass.set(entrypoint)
      // classpath is wired below in `afterEvaluate`, once core/build.gradle.kts has registered
      // `nativeImageJar` and `jar`.

      buildArgs(
          buildList {
            // If PGO flags are present, add them first; if not, add `-Ox`.
            when (pgoArgs.isEmpty()) {
              true -> add("-O$nativeOpt")
              false -> addAll(pgoArgs)
            }

            // Common flags for Native Image.
            addAll(
                buildList {
                  add("-march=$nativeTarget")
                  if (nativeDebug) {
                    add("-g")
                    add("-H:+SourceLevelDebug")
                  }
                  // --
                  add("--gc=$nativeGc")
                  add("--future-defaults=all")
                  add("--link-at-build-time=com.facebook")
                  add("--initialize-at-build-time=com.facebook")
                  add("--add-opens=java.base/java.util=ALL-UNNAMED")
                  add("--emit=build-report")
                  add("--color=always")
                  add("--enable-sbom=cyclonedx,embed")
                  // -- ▼ SVM Hosted Options
                  add("-H:+UseCompressedReferences")
                  add("-H:+ReportExceptionStackTraces")
                  // -- ▼ SVM Runtime Options
                  add("-R:+InstallSegfaultHandler")
                  // -- ▼ Experimental Options
                  add("-H:+UnlockExperimentalVMOptions")
                  add("-H:-ReduceImplicitExceptionStackTraceInformation")
                  add("-H:+ReportDynamicAccess")
                  add("-H:-UnlockExperimentalVMOptions")
                  // -- ▼ VM flags
                  add("-J--enable-native-access=ALL-UNNAMED")
                  add("-J--illegal-native-access=allow")
                  add("-J--sun-misc-unsafe-memory-access=allow")
                  // -- ▼ C Compiler / Linker Flags
                  if (enableLto) {
                    add("--native-compiler-options=-flto")
                    add("-H:NativeLinkerOption=-flto")
                  }
                  if (preferMusl) {
                    add("-H:NativeLinkerOption=-L${muslSysroot}/lib")
                  }
                }
            )

            // Mark what should be initialized at build-time, i.e. persisted to the heap image.
            // See `src/main/native-image/initialize-at-build-time.txt` for a list of such classes.
            addLinesFromFile("src", "main", "native-image", "initialize-at-build-time.txt") {
              "--initialize-at-build-time=$it"
            }

            // Still other classes must be initialized at runtime only.
            // See `src/main/native-image/initialize-at-run-time.txt` for a list of such classes.
            addLinesFromFile("src", "main", "native-image", "initialize-at-run-time.txt") {
              "--initialize-at-run-time=$it"
            }

            // Here, we prefer static linking, for startup performance and release simplicity.
            // On Linux amd64, we target musl to avoid linking conflicts with older glibc.
            // On macOS, pass `--static-nolibc` for the closest option available.
            when (System.getProperty("os.name")) {
              "Linux" ->
                  when (System.getProperty("os.arch")) {
                    "amd64" ->
                        when (preferMusl) {
                          true -> addAll(listOf("--static", "--libc=musl", "-H:+StaticLibStdCpp"))
                          false -> add("--static-nolibc")
                        }
                    else -> add("--static-nolibc")
                  }
              "Mac OS X" -> add("--static-nolibc")
            }
          }
      )
    }
  }
}

// The image classpath includes `nativeImageJar` and `jar`, which core/build.gradle.kts registers
// after this plugin is applied. Wire them in once the project is evaluated, using their archiveFile
// providers so the producing tasks are tracked as dependencies (otherwise downstream GraalVM tasks
// such as `generateResourcesConfigFile` fail Gradle's implicit-dependency validation).
afterEvaluate {
  configure<GraalVMExtension> {
    binaries {
      named("main") {
        classpath.from(
            tasks.named<Jar>("nativeImageJar").flatMap { it.archiveFile },
            tasks.named<Jar>("jar").flatMap { it.archiveFile },
            configurations.named("compileClasspath"),
            configurations.named("runtimeClasspath"),
            configurations.named("nativeImageClasspath"),
        )
      }
    }
  }
}

fun MutableList<String>.addLinesFromFile(vararg path: String, mapper: (String) -> String) {
  file(Paths.get(path.first(), *path.drop(1).toTypedArray()).toString())
      .useLines { lines ->
        lines
            .map { it.trim() }
            // Skip blank lines and `#` comments so these metadata files can be documented.
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .map(mapper)
            .toList()
      }
      .also { addAll(it) }
}
