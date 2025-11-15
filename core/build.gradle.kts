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

// Pass `-Pktfmt.native.release=true` to enable release mode for Native Image.
val nativeRelease = findProperty("ktfmt.native.release") == "true"

// Pass `-Pktfmt.native.target=xx` to pass `-march=xx` to Native Image.
val nativeTarget = findProperty("ktfmt.native.target") ?: "compatibility"

// Pass `-Pktfmt.native.opt=s` to pass e.g. `-Os` to Native Image.
val nativeOpt =
    when (val opt = findProperty("ktfmt.native.opt")) {
      null -> if (nativeRelease) "3" else "b"
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

application { mainClass = entrypoint }

repositories {
  mavenLocal()
  mavenCentral()
}

dependencies {
  api(libs.googleJavaformat)
  api(libs.guava)
  api(libs.jna)
  api(libs.kotlin.stdlib)
  api(libs.kotlin.test)
  api(libs.kotlin.compilerEmbeddable)
  testImplementation(libs.googleTruth)
  testImplementation(libs.junit)

  compileOnly(libs.graalvm.nativeimage)
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

    doLast {
      // run the shell script genVersionFileScript with versionPropertiesFile as argument
      val scriptProcess =
          providers.exec {
            workingDir = rootProject.rootDir
            commandLine = listOf(genVersionFileScript.toString(), versionPropertiesFile.toString())
          }

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

graalvmNative {
  binaries {
    named("main") {
      imageName = "ktfmt"
      mainClass = entrypoint

      buildArgs(
          buildList {
            // If PGO flags are present, add them first; if not, add `-Ox`.
            when (pgoArgs.isEmpty()) {
              true -> add("-O$nativeOpt")
              false -> addAll(pgoArgs)
            }

            // Common flags for Native Image.
            addAll(
                listOf(
                    "-march=$nativeTarget",
                    "--gc=serial",
                    "--future-defaults=all",
                    "--link-at-build-time=com.facebook",
                    "--initialize-at-build-time=com.facebook",
                    "--add-opens=java.base/java.util=ALL-UNNAMED",
                    "--emit=build-report",
                    "--color=always",
                    "--enable-sbom=cyclonedx,embed",
                    // -- ▼ SVM Hosted Options
                    "-H:+UseCompressedReferences",
                    "-H:+ReportExceptionStackTraces",
                    // -- ▼ SVM Runtime Options
                    "-R:MaxHeapSize=268435456", // 256mb max heap
                    // -- ▼ Experimental Options
                    "-H:+UnlockExperimentalVMOptions",
                    "-H:-ReduceImplicitExceptionStackTraceInformation",
                    "-H:+ReportDynamicAccess",
                    "-H:-UnlockExperimentalVMOptions",
                    // -- ▼ VM flags
                    "-J--enable-native-access=ALL-UNNAMED",
                    "-J--illegal-native-access=allow",
                    "-J--sun-misc-unsafe-memory-access=allow",
                )
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
                    "amd64" -> addAll(listOf("--static", "--libc=musl"))
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
  // Mark what should be initialized at build-time, i.e. persisted to the heap image.
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
