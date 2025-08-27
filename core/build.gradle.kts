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

plugins {
  kotlin("jvm")
  id("com.gradleup.shadow")
  id("com.ncorti.ktfmt.gradle")
  id("maven-publish")
  id("org.jetbrains.dokka")
  id("signing")
}

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
