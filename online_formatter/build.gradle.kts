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
}

repositories {
  mavenLocal()
  mavenCentral()
}

dependencies {
  implementation(libs.amazon.aws.lambda.core)
  implementation(libs.amazon.aws.lambda.events)
  implementation(platform(libs.amazon.aws.sdk.bom))
  implementation(libs.amazon.aws.sdk.lambda)
  implementation(libs.gson)
  implementation(project(":ktfmt"))
  testImplementation(kotlin("test-junit"))
}

kotlin {
  val javaVersion: String = rootProject.libs.versions.java.get()
  jvmToolchain(javaVersion.toInt())
}

tasks {
  test { useJUnit() }

  register("packageFat", Zip::class) {
    from(compileKotlin)
    from(processResources)
    into("lib") { from(configurations.runtimeClasspath) }
    // 0755
    dirPermissions { unix("rwxr-xr-x") }
    filePermissions { unix("rwxr-xr-x") }
  }

  register("packageLibs", Zip::class) {
    into("java/lib") { from(configurations.runtimeClasspath) }
    // 0755
    dirPermissions { unix("rwxr-xr-x") }
    filePermissions { unix("rwxr-xr-x") }
  }

  val packageSkinny by
      registering(Zip::class) {
        from(compileKotlin)
        from(processResources)
      }

  build { dependsOn(packageSkinny) }
}
