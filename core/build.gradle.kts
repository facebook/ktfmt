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

import java.nio.file.Paths
import kotlin.io.path.writeText
import org.jetbrains.intellij.platform.gradle.utils.asPath

plugins {
  kotlin("jvm")
  id("com.gradleup.shadow")
  id("com.ncorti.ktfmt.gradle")
  id("maven-publish")
  id("org.graalvm.buildtools.native")
  id("org.jetbrains.dokka")
  id("signing")
  application
}

val entrypoint = "com.facebook.ktfmt.cli.Main"

application { mainClass = entrypoint }

repositories {
  mavenLocal()
  mavenCentral()
}

// Configuration for building `src/native-image/java`.
val nativeImageJavacClasspath by
    configurations.creating {
      extendsFrom(configurations.implementation.get())
      isCanBeResolved = true
    }

dependencies {
  api(libs.googleJavaformat)
  api(libs.guava)
  api(libs.jna)
  api(libs.kotlin.stdlib)
  api(libs.kotlin.compilerEmbeddable)
  implementation(libs.ec4j)
  testImplementation(libs.kotlin.test.junit4)
  testImplementation(libs.googleTruth)
  testImplementation(libs.junit)

  nativeImageJavacClasspath(libs.graalvm.nativeimage)
  nativeImageClasspath(libs.jline.terminal)
  nativeImageClasspath(libs.jline.terminal.jansi)
  nativeImageClasspath(libs.jline.terminal.jna)
  nativeImageClasspath(libs.jline.terminal.jni)
}

val generateSources by
    tasks.registering {
      outputs.dir(layout.buildDirectory.dir("generated/main/java"))
      dependsOn(tasks.named("generateKtfmtFile"))
    }

tasks {
  // Create Ktfmt.kt file with version information
  register("generateKtfmtFile") {
    val genVersionFileScript = rootProject.rootDir.resolve("gen_version_file.sh")
    val versionPropertiesFile = rootProject.rootDir.resolve("gradle.properties")
    val versionFile =
        layout.buildDirectory.file("generated/main/java/com/facebook/ktfmt/util/Ktfmt.kt")

    inputs.files(genVersionFileScript, versionPropertiesFile)
    outputs.file(versionFile)
    outputs.cacheIf { true }

    // provider to run the shell script genVersionFileScript with versionPropertiesFile as argument
    val scriptProcess =
        providers.exec {
          workingDir = rootProject.rootDir
          commandLine = listOf(genVersionFileScript.toString(), versionPropertiesFile.toString())
        }

    doLast {
      val scriptOutput = scriptProcess.standardOutput.asText.get()
      if (scriptProcess.result.get().exitValue != 0) {
        val scriptError = scriptProcess.standardError.asText.get()
        error("Failed to generate version file!\nstdout:\n$scriptOutput\n\nstderr:\n$scriptError")
      }
      versionFile.get().asPath.writeText(scriptOutput)
      logger.info("Generated version file at ${versionFile.get()}")
    }
  }

  // Run tests with UTF-16 encoding
  test { jvmArgs("-Dfile.encoding=UTF-16") }

  // Handle multiple versions of Kotlin here
  withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    // Only get major and minor version, e.g. 1.8.0-beta1 -> 1.8
    val kotlinVersion = rootProject.libs.versions.kotlin.get().substringBeforeLast(".")
    exclude {
      val path = it.file.path
      "com/facebook/ktfmt/util/kotlin-" in path && "kotlin-$kotlinVersion" !in path
    }
  }

  // Add main class to jar manifest
  withType(Jar::class) { manifest { attributes["Main-Class"] = "com.facebook.ktfmt.cli.Main" } }

  // Sources
  register("sourcesJar", Jar::class) {
    archiveClassifier.set("sources")
    from(sourceSets["main"].allSource)
  }

  // Javadoc
  register("javadocJar", Jar::class) {
    val dokkaJavadocTask = named("dokkaJavadoc", org.jetbrains.dokka.gradle.DokkaTask::class)
    dependsOn(dokkaJavadocTask)
    from(dokkaJavadocTask.flatMap { it.outputDirectory })
    archiveClassifier.set("javadoc")
  }

  // Fat jar
  shadowJar {
    archiveClassifier = "with-dependencies"
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    failOnDuplicateEntries = true
  }
}

sourceSets {
  create("nativeImage") {
    java { srcDir("src/main/native-image/java") }
    resources { srcDir("src/main/native-image/resources") }
  }
}

val compileNativeImageClasses by
    tasks.registering(JavaCompile::class) {
      group = "build"
      description = "Compiles Native Image helper classes"
      source = sourceSets["nativeImage"].java
      classpath = nativeImageJavacClasspath
      destinationDirectory = layout.buildDirectory.dir("classes/native-image")
      dependsOn(tasks.named("compileJava"))
    }

// Native Image artifacts jar (local only, not published)
val nativeImageJar by
    tasks.registering(Jar::class) {
      group = "build"
      description = "Assembles Native Image jar and resources"
      dependsOn(compileNativeImageClasses)
      from(layout.buildDirectory.dir("classes/native-image"))
      from(sourceSets["nativeImage"].resources)
      archiveClassifier = "nativeimage"
    }

tasks.nativeCompile.configure { dependsOn(compileNativeImageClasses, nativeImageJar) }

kotlin {
  val javaVersion: String = rootProject.libs.versions.java.get()
  jvmToolchain(javaVersion.toInt())

  sourceSets {
    main {
      kotlin {
        // Include generated code
        srcDir(generateSources)
      }
    }
  }
}

ktfmt {
  trailingCommaManagementStrategy.set(
      com.ncorti.ktfmt.gradle.TrailingCommaManagementStrategy.ONLY_ADD
  )
}

object DefaultArchitectureTarget {
  val amd64 = "x86-64-v4"
  val arm64 = "armv8.4-a+crypto+sve"
}

// Pass `-Pktfmt.native.release=true` to enable release mode for Native Image.
val nativeRelease = findProperty("ktfmt.native.release") == "true"

// Pass `-Pktfmt.native.target=xx` to pass `-march=xx` to Native Image.
val nativeTarget =
    findProperty("ktfmt.native.target")
        ?: when (val hostArch = System.getProperty("os.arch")) {
          "amd64",
          "x86_64" -> DefaultArchitectureTarget.amd64
          "aarch64",
          "arm64" -> DefaultArchitectureTarget.arm64
          else -> error("Unrecognized host architecture: '$hostArch'")
        }

// Pass `-Pktfmt.native.gc=xx` to select a garbage collector; options include `serial`, `G1`, and
// `epsilon`.
val nativeGc = findProperty("ktfmt.native.gc") ?: "G1"

// Pass `-Pktfmt.native.gc=xx` to select a garbage collector; options include `serial`, `G1`, and
// `epsilon`.
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

// List of PGO profiles, which are held in `src/main/native-image/profiles`.
val pgoProfiles =
    listOf("default.iprof")
        .map { profileName ->
          layout.projectDirectory.file(
              Paths.get("src", "main", "native-image", "profiles", profileName).toString()
          )
        }
        .let { allProfiles -> listOf("--pgo=${allProfiles.joinToString(",")}") }

// Pass `-Pktfmt.native.pgo=true` to build with PGO; pass `train` to enable instrumentation.
val pgoArgs =
    when (val pgo = findProperty("ktfmt.native.pgo")) {
      null -> if (nativeRelease) pgoProfiles else emptyList()
      "true" -> pgoProfiles
      "false" -> emptyList()
      "train" -> listOf("--pgo-instrument")
      else -> error("Unrecognized `ktfmt.native.pgo` argument: '$pgo'")
    }

graalvmNative {
  binaries {
    named("main") {
      imageName = "ktfmt"
      mainClass = entrypoint
      classpath =
          files(
              nativeImageJar.get().archiveFile,
              tasks.jar.get().archiveFile,
              configurations["compileClasspath"],
              configurations["runtimeClasspath"],
              configurations["nativeImageClasspath"],
          )

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
                  add("--gc=G1")
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

group = "com.facebook"

version = rootProject.version

publishing {
  publications {
    create<MavenPublication>("maven") {
      groupId = "com.facebook"
      artifactId = "ktfmt"
      version = rootProject.version.toString()

      from(components["java"])
      artifact(tasks.named("sourcesJar"))
      artifact(tasks.named("javadocJar"))

      pom {
        name = "Ktfmt"
        description =
            "A program that reformats Kotlin source code to comply with the common community standard for Kotlin code conventions."
        url = "https://github.com/facebook/ktfmt"
        inceptionYear = "2019"
        developers { developer { name = "Facebook" } }
        licenses {
          license {
            name = "The Apache License, Version 2.0"
            url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
          }
        }
        scm {
          connection = "scm:git:https://github.com/facebook/ktfmt.git"
          developerConnection = "scm:git:git@github.com:facebook/ktfmt.git"
          url = "https://github.com/facebook/ktfmt.git"
        }
      }
    }
  }
}

if (System.getenv("SIGN_BUILD") != null) {
  signing {
    useGpgCmd()
    sign(publishing.publications["maven"])
  }
}

fun MutableList<String>.addLinesFromFile(vararg path: String, mapper: (String) -> String) {
  file(Paths.get(path.first(), *path.drop(1).toTypedArray()).toString())
      .useLines { lines ->
        lines
            .filter { line ->
              // filter empty lines
              line.isNotEmpty()
            }
            .map(mapper)
            .toList()
      }
      .also { addAll(it) }
}
