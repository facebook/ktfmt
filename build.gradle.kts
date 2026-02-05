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
  alias(libs.plugins.dokka) apply false
  alias(libs.plugins.intelliJPlatform) apply false
  alias(libs.plugins.kotlin) apply false
  alias(libs.plugins.ktfmt) apply false
  alias(libs.plugins.nexusPublish)
  alias(libs.plugins.shadowJar) apply false
  alias(libs.plugins.graalvm) apply false
}

version = providers.gradleProperty("ktfmt.version").get()

tasks.wrapper { distributionType = Wrapper.DistributionType.ALL }

nexusPublishing {
  repositories {
    sonatype {
      nexusUrl = uri("https://ossrh-staging-api.central.sonatype.com/service/local/")
      snapshotRepositoryUrl = uri("https://central.sonatype.com/repository/maven-snapshots/")

      stagingProfileId.set("com.facebook")

      username = System.getenv("OSSRH_USERNAME")
      password = System.getenv("OSSRH_PASSWORD")
    }
  }
}
