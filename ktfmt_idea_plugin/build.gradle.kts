import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType.*

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

import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType.IntellijIdeaCommunity

plugins {
  java
  alias(libs.plugins.intelliJPlatform)
  alias(libs.plugins.spotless)
}

val ktfmtVersion = rootProject.file("../version.txt").readText().trim()
val pluginVersion = "1.1"

group = "com.facebook"

version = "$pluginVersion.$ktfmtVersion"

java {
  toolchain {
    targetCompatibility = JavaVersion.VERSION_17
    sourceCompatibility = JavaVersion.VERSION_17
  }
}

repositories {
  mavenCentral()
  intellijPlatform { defaultRepositories() }
  mavenLocal()
}

dependencies {
  intellijPlatform {
    create(IntellijIdeaCommunity, "2022.3")
    instrumentationTools()
    pluginVerifier()
    zipSigner()
  }

  implementation("com.facebook", "ktfmt", ktfmtVersion)
  implementation(libs.googleJavaFormat)
}

intellijPlatform {
  pluginConfiguration.ideaVersion {
    sinceBuild = "223.7571.182" // 2022.3
    untilBuild = provider { null }
  }

  publishing { token = System.getenv("JETBRAINS_MARKETPLACE_TOKEN") }

  pluginVerification { ides { recommended() } }
}

spotless { java { googleJavaFormat(libs.versions.googleJavaFormat.get()) } }
