package com.facebook.ktfmt

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.register

@Suppress("unused")
class GenerateKtfmtFilePlugin : Plugin<Project> {
  override fun apply(project: Project) {
    val propertiesFile = project.rootProject.file("gradle.properties")
    if (!propertiesFile.exists()) {
      error("gradle.properties not found in root project, can't generate Ktfmt.kt")
    }

    val outputDir = project.layout.buildDirectory.dir("generated/main/java").get().asFile
    val outputFile = outputDir.resolve("com/facebook/ktfmt/util/Ktfmt.kt")

    project.tasks.register<GenerateKtfmtFileTask>("generateKtfmtFile") {
      this.propertiesFile.set(propertiesFile)
      this.outputFile.set(outputFile)
    }
  }
}
