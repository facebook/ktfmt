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

package com.facebook.ktfmt.cli

import com.facebook.ktfmt.format.Formatter
import com.facebook.ktfmt.format.FormattingOptions
import java.io.File
import java.io.PrintStream

/** ParsedArgs holds the arguments passed to ktfmt on the command-line, after parsing. */
data class ParsedArgs(
    val fileNames: List<String>,
    val formattingOptions: FormattingOptions,
    /**
     * Run the formatter without writing changes to any files. This will print the path of any files
     * that would be changed if the formatter is run normally.
     */
    val dryRun: Boolean,

    /** Return exit code 1 if any formatting changes are detected. */
    val setExitIfChanged: Boolean,
    /** File name to report when formating code from stdin */
    val stdinName: String?,
) {
  companion object {

    fun processArgs(err: PrintStream, args: Array<String>): ParsedArgs {
      if (args.size == 1 && args[0].startsWith("@")) {
        return parseOptions(err, File(args[0].substring(1)).readLines().toTypedArray())
      } else {
        return parseOptions(err, args)
      }
    }

    /** parseOptions parses command-line arguments passed to ktfmt. */
    fun parseOptions(err: PrintStream, args: Array<String>): ParsedArgs {
      val fileNames = mutableListOf<String>()
      var formattingOptions = FormattingOptions()
      var dryRun = false
      var setExitIfChanged = false
      var removeUnusedImports = true
      var stdinName: String? = null

      for (arg in args) {
        when {
          arg == "--dropbox-style" -> formattingOptions = Formatter.DROPBOX_FORMAT
          arg == "--google-style" -> formattingOptions = Formatter.GOOGLE_FORMAT
          arg == "--kotlinlang-style" -> formattingOptions = Formatter.KOTLINLANG_FORMAT
          arg == "--dry-run" || arg == "-n" -> dryRun = true
          arg == "--set-exit-if-changed" -> setExitIfChanged = true
          arg == "--do-not-remove-unused-imports" -> removeUnusedImports = false
          arg.startsWith("--stdin-name") -> stdinName = parseKeyValueArg(err, "--stdin-name", arg)
          arg.startsWith("--") -> err.println("Unexpected option: $arg")
          arg.startsWith("@") -> err.println("Unexpected option: $arg")
          else -> fileNames.add(arg)
        }
      }

      return ParsedArgs(
          fileNames,
          formattingOptions.copy(removeUnusedImports = removeUnusedImports),
          dryRun,
          setExitIfChanged,
          stdinName,
      )
    }

    private fun parseKeyValueArg(err: PrintStream, key: String, arg: String): String? {
      val parts = arg.split('=', limit = 2)
      if (parts[0] != key || parts.size != 2) {
        err.println("Found option '${arg}', expected '${key}=<value>'")
        return null
      }
      return parts[1]
    }
  }
}
