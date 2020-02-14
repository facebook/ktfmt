// Copyright (c) Facebook, Inc. and its affiliates.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.facebook.ktfmt

import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.text.LineColumn
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.PsiManager
import com.intellij.testFramework.LightVirtualFile
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer.PLAIN_RELATIVE_PATHS
import org.jetbrains.kotlin.cli.common.messages.PrintingMessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.psi.psiUtil.startOffset

/** Parser parses a Kotlin file given as a string and returns its parse tree. */
open class Parser {
  fun parse(code: String): KtFile {
    val disposable = Disposer.newDisposable()
    try {
      val configuration = CompilerConfiguration()
      configuration.put(
          CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY,
          PrintingMessageCollector(System.err, PLAIN_RELATIVE_PATHS, false))
      val env =
          KotlinCoreEnvironment.createForProduction(
              disposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES)
      val virtualFile = LightVirtualFile("temp.kt", KotlinFileType.INSTANCE, code)
      val ktFile = PsiManager.getInstance(env.project).findFile(virtualFile) as KtFile
      ktFile.collectDescendantsOfType<PsiErrorElement>().let {
        if (it.isNotEmpty()) throwParseError(code, it[0])
      }
      return ktFile
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

fun throwParseError(fileContents: String, error: PsiErrorElement): Nothing {
  throw ParseError(error.errorDescription, StringUtil.offsetToLineColumn(fileContents, error.startOffset))
}

class ParseError(val errorDescription: String, val lineColumn: LineColumn) :
    IllegalArgumentException("${lineColumn.line + 1}:${lineColumn.column + 1}: error: $errorDescription")
