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

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins { kotlin("jvm") version "1.5.0" }

repositories {
  mavenLocal()
  mavenCentral()
}

val ktfmtVersion = rootProject.file("../version.txt").readText().trim()

dependencies {
  implementation("com.facebook:ktfmt:$ktfmtVersion")
  implementation(platform("software.amazon.awssdk:bom:2.10.73"))
  implementation("software.amazon.awssdk:lambda")
  implementation("com.amazonaws:aws-lambda-java-core:1.2.1")
  implementation("com.amazonaws:aws-lambda-java-events:2.2.9")
  implementation("com.google.code.gson:gson:2.8.6")
  testImplementation(kotlin("test-junit"))
}

tasks {
  test { useJUnit() }

  withType<KotlinCompile>() { kotlinOptions.jvmTarget = "11" }

  val packageFat by
      creating(Zip::class) {
        from(compileKotlin)
        from(processResources)
        into("lib") { from(configurations.runtimeClasspath) }
        dirMode = 0b111101101 // 0755
        fileMode = 0b111101101 // 0755
      }

  val packageLibs by
      creating(Zip::class) {
        into("java/lib") { from(configurations.runtimeClasspath) }
        dirMode = 0b111101101 // 0755
        fileMode = 0b111101101 // 0755
      }

  val packageSkinny by
      creating(Zip::class) {
        from(compileKotlin)
        from(processResources)
      }

  build { dependsOn(packageSkinny) }
}
