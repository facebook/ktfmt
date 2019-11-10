package com.facebook.ktfmt

import com.intellij.openapi.util.Disposer
import com.intellij.psi.PsiManager
import com.intellij.testFramework.LightVirtualFile
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.psi.KtFile

/**
 * Parser parses a Kotlin file given as a string and returns its parse tree.
 */
open class Parser {
  fun parse(code: String): KtFile {
    val disposable = Disposer.newDisposable()
    try {
      val env = KotlinCoreEnvironment.createForProduction(
          disposable, CompilerConfiguration(), EnvironmentConfigFiles.JVM_CONFIG_FILES
      )
      val file = LightVirtualFile("temp.kt", KotlinFileType.INSTANCE, code)
      return PsiManager.getInstance(env.project).findFile(file) as KtFile
    } finally {
      disposable.dispose()
    }
  }

  companion object : Parser() {
    init {
      // To hide annoying warning on Windows
      System.setProperty("idea.use.native.fs.for.win", "false")
    }
  }
}
