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

import com.ncorti.ktfmt.gradle.tasks.KtfmtCheckTask
import com.ncorti.ktfmt.gradle.tasks.KtfmtFormatTask
import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType.IntellijIdeaCommunity

plugins {
  java
  alias(libs.plugins.kotlin)
  alias(libs.plugins.intelliJPlatform)
  alias(libs.plugins.ktfmt)
}

val ktfmtVersion = rootProject.file("../version.txt").readText().trim()
val pluginVersion = "1.2"

group = "com.facebook"

version = "$pluginVersion.$ktfmtVersion"

kotlin { jvmToolchain(17) }

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

  implementation("com.facebook:ktfmt:$ktfmtVersion")
}

intellijPlatform {
  pluginConfiguration.ideaVersion {
    sinceBuild = "223.7571.182" // 2022.3
    untilBuild = provider { null }
  }

  publishing { token = System.getenv("JETBRAINS_MARKETPLACE_TOKEN") }

  pluginVerification { ides { recommended() } }
}

val runIntellij242 by
    intellijPlatformTesting.runIde.registering {
      type = IntellijIdeaCommunity
      version = "2024.2"
    }

tasks {
  // Set up ktfmt formatting tasks
  val ktfmtFormatKts by
      creating(KtfmtFormatTask::class) {
        source = fileTree(rootDir)
        include("**/*.kts")
      }
  val ktfmtCheckKts by
      creating(KtfmtCheckTask::class) {
        source = fileTree(rootDir)
        include("**/*.kts")
        mustRunAfter("compileKotlin")
        mustRunAfter("prepareSandbox")
        mustRunAfter("prepareTestSandbox")
        mustRunAfter("instrumentCode")
        mustRunAfter("instrumentTestCode")
        mustRunAfter("buildSearchableOptions")
        mustRunAfter("prepareJarSearchableOptions")
      }
  val ktfmtFormat by getting { dependsOn(ktfmtFormatKts) }
  val ktfmtCheck by getting { dependsOn(ktfmtCheckKts) }
  val check by getting { dependsOn(ktfmtCheck) }
}
