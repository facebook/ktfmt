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

plugins {
  kotlin("jvm")
  id("com.ncorti.ktfmt.gradle")
  id("maven-publish")
  id("com.gradleup.shadow")
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

kotlin {
  val javaVersion: String = rootProject.libs.versions.java.get()
  jvmToolchain(javaVersion.toInt())
}

tasks {
  // Run tests with UTF-16 encoding
  test { jvmArgs("-Dfile.encoding=UTF-16") }

  check {
    // Set up ktfmt formatting task dependencies
    dependsOn(named("ktfmtCheck"))
    dependsOn(named("ktfmtCheckScripts"))
  }

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

  // Fat jar
  shadowJar { archiveClassifier = "with-dependencies" }
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

  repositories {
    maven {
      name = "OSSRH"
      url = uri("https://oss.sonatype.org/content/repositories/snapshots/")
      credentials {
        username = System.getenv("MAVEN_USERNAME")
        password = System.getenv("MAVEN_PASSWORD")
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
