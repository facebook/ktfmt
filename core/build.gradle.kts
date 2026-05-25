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

import kotlin.io.path.writeText
import org.jetbrains.intellij.platform.gradle.utils.asPath
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation

plugins {
  kotlin("jvm")
  id("com.gradleup.shadow")
  id("com.ncorti.ktfmt.gradle")
  id("maven-publish")
  // Applies the GraalVM Native Image plugin and configures the `ktfmt` native binary. See
  // build-logic/src/main/kotlin/ktfmt.native-image.gradle.kts.
  id("ktfmt.native-image")
  id("org.jetbrains.dokka")
  id("signing")
}

repositories {
  mavenLocal()
  mavenCentral()
}

// Resolvable classpath for compiling the Native Image substitution: the main compile deps (for the
// kotlinc types it targets) plus GraalVM's `svm`. Kept standalone so `svm` does NOT leak into the
// `nativeImage` source set's configurations (and from there into the GraalVM image classpath).
val nativeImageHelperClasspath by configurations.creating {
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

  nativeImageHelperClasspath(libs.graalvm.nativeimage)
  // `nativeImageClasspath` is created by the GraalVM plugin (applied via `ktfmt.native-image`), so
  // reference it by name rather than via a generated accessor.
  "nativeImageClasspath"(libs.jline.terminal)
  "nativeImageClasspath"(libs.jline.terminal.jansi)
  "nativeImageClasspath"(libs.jline.terminal.jna)
  "nativeImageClasspath"(libs.jline.terminal.jni)
}

val generateSources by tasks.registering {
  outputs.dir(layout.buildDirectory.dir("generated/main/java"))
  dependsOn(tasks.named("generateKtfmtFile"))
}

tasks {
  // Create Ktfmt.kt file with version information
  register("generateKtfmtFile") {
    val version = providers.gradleProperty("ktfmt.version")
    val versionFile =
        layout.buildDirectory.file("generated/main/java/com/facebook/ktfmt/util/Ktfmt.kt")

    inputs.property("version", version)
    outputs.file(versionFile)
    outputs.cacheIf { true }

    doLast {
      versionFile
          .get()
          .asPath
          .writeText(
              """
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

          package com.facebook.ktfmt.util

          object Ktfmt {
            const val version = "${version.get()}"
          }
          """
                  .trimIndent() + "\n"
          )
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

// The Native Image substitution helper + reachability metadata live in their own source set so the
// GraalVM-only `svm` dependency stays out of the main/published artifact. The Kotlin source is
// compiled by the auto-created `compileNativeImageKotlin` task (kotlin srcDir configured below).
sourceSets { create("nativeImage") { resources { srcDir("src/main/native-image/resources") } } }

// Native Image artifacts jar (local only, not published)
val nativeImageJar by
    tasks.registering(Jar::class) {
      group = "build"
      description = "Assembles Native Image jar and resources"
      from(sourceSets["nativeImage"].output)
      archiveClassifier = "nativeimage"
    }

// Compile the substitution against `svm` without putting it on the source set's configurations.
tasks.named<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>("compileNativeImageKotlin") {
  libraries.from(nativeImageHelperClasspath)
}

kotlin {
  @OptIn(ExperimentalAbiValidation::class) abiValidation { enabled = true }

  val javaVersion: String = rootProject.libs.versions.java.get()
  jvmToolchain(javaVersion.toInt())

  sourceSets {
    main {
      kotlin {
        // Include generated code
        srcDir(generateSources)
      }
    }
    named("nativeImage") { kotlin { srcDir("src/main/native-image/kotlin") } }
  }
}

ktfmt {
  trailingCommaManagementStrategy.set(
      com.ncorti.ktfmt.gradle.TrailingCommaManagementStrategy.ONLY_ADD
  )
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
