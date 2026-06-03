import com.ncorti.ktfmt.gradle.tasks.KtfmtCheckTask
import org.gradle.kotlin.dsl.`kotlin-dsl`

plugins {
  `kotlin-dsl`
  alias(libs.plugins.ktfmt)
}

gradlePlugin {
  plugins {
    register("ktfmt-file-generator") {
      id = "ktfmt.ktfmt-file-generator"
      implementationClass = "com.facebook.ktfmt.GenerateKtfmtFilePlugin"
    }
  }
}

tasks.named("jar") { dependsOn(tasks.withType<KtfmtCheckTask>()) }
