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
  id("org.jetbrains.intellij") version "0.7.2"
  java
  id("com.diffplug.spotless") version "5.10.2"
}

val ktfmtVersion = rootProject.file("../version.txt").readText().trim()
val pluginVersion = "1.1"

group = "com.facebook"

version = "$pluginVersion.$ktfmtVersion"

repositories {
  mavenCentral()
  mavenLocal()
}

java {
  sourceCompatibility = JavaVersion.VERSION_11
  targetCompatibility = JavaVersion.VERSION_11
}

dependencies {
  implementation("com.facebook", "ktfmt", ktfmtVersion)
  implementation("com.google.googlejavaformat", "google-java-format", "1.8")
}

// See https://github.com/JetBrains/gradle-intellij-plugin/
intellij {
  // Version with which to build (and run; unless alternativeIdePath is specified)
  version = "2020.3"
  // To run on a different IDE, uncomment and specify a path.
  // alternativeIdePath = "/Applications/Android Studio.app"
}

tasks {
  patchPluginXml {
    sinceBuild("201")
    untilBuild("")
  }
  publishPlugin { token(System.getenv("JETBRAINS_MARKETPLACE_TOKEN")) }
  runPluginVerifier { ideVersions(listOf("211.6432.7")) }
}

spotless { java { googleJavaFormat() } }
