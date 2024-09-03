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
import java.nio.charset.StandardCharsets.UTF_8

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

    fun processArgs(args: Array<String>): ParseResult {
      val arguments =
          if (args.size == 1 && args[0].startsWith("@")) {
            File(args[0].substring(1)).readLines(UTF_8).toTypedArray()
          } else {
            args
          }
      return parseOptions(arguments)
    }

    val HELP_TEXT: String =
        """
        |ktfmt - command line Kotlin source code pretty-printer
        |
        |Usage:
        |  ktfmt [OPTIONS] <File1.kt> <File2.kt> ...
        |  ktfmt @ARGFILE
        |
        |ktfmt formats Kotlin source code files in-place, reporting for each file whether the
        |formatting succeeded or failed on standard error. If none of the style options are
        |passed, Meta's style is used.
        |
        |Alternatively, ktfmt can read Kotlin source code from standard input and write the 
        |formatted result on standard output.
        |
        |Example:
        |     $ ktfmt --kotlinlang-style Main.kt src/Parser.kt
        |     Done formatting Main.kt
        |     Error formatting src/Parser.kt: @@@ERROR@@@; skipping.
        |    
        |Commands options:
        |  -h, --help                        Show this help message
        |  -n, --dry-run                     Don't write to files, only report files which 
        |                                        would have changed
        |  --meta-style                      Use 2-space block indenting (default)
        |  --google-style                    Google internal style (2 spaces)
        |  --kotlinlang-style                Kotlin language guidelines style (4 spaces)
        |  --stdin-name=<name>               Name to report when formatting code from stdin
        |  --set-exit-if-changed             Sets exit code to 1 if any input file was not 
        |                                        formatted/touched
        |  --do-not-remove-unused-imports    Leaves all imports in place, even if not used
        |  
        |ARGFILE:
        |  If the only argument begins with '@', the remainder of the argument is treated
        |  as the name of a file to read options and arguments from, one per line.
        |  
        |  e.g.
        |      $ cat arg-file.txt
        |      --google-style
        |      -n
        |      File1.kt
        |      File2.kt
        |      $ ktfmt @arg-file1.txt
        |      Done formatting File1.kt
        |      Done formatting File2.kt
        |"""
            .trimMargin()

    /** parseOptions parses command-line arguments passed to ktfmt. */
    fun parseOptions(args: Array<out String>): ParseResult {
      val fileNames = mutableListOf<String>()
      var formattingOptions = Formatter.META_FORMAT
      var dryRun = false
      var setExitIfChanged = false
      var removeUnusedImports = true
      var stdinName: String? = null

      if ("--help" in args || "-h" in args) return ParseResult.ShowMessage(HELP_TEXT)

      for (arg in args) {
        when {
          arg == "--meta-style" -> formattingOptions = Formatter.META_FORMAT
          arg == "--google-style" -> formattingOptions = Formatter.GOOGLE_FORMAT
          arg == "--kotlinlang-style" -> formattingOptions = Formatter.KOTLINLANG_FORMAT
          arg == "--dry-run" || arg == "-n" -> dryRun = true
          arg == "--set-exit-if-changed" -> setExitIfChanged = true
          arg == "--do-not-remove-unused-imports" -> removeUnusedImports = false
          arg.startsWith("--stdin-name=") ->
              stdinName =
                  parseKeyValueArg("--stdin-name", arg)
                      ?: return ParseResult.Error(
                          "Found option '${arg}', expected '${"--stdin-name"}=<value>'")
          arg.startsWith("--") -> return ParseResult.Error("Unexpected option: $arg")
          arg.startsWith("@") -> return ParseResult.Error("Unexpected option: $arg")
          else -> fileNames.add(arg)
        }
      }

      if (fileNames.contains("-")) {
        // We're reading from stdin
        if (fileNames.size > 1) {
          val filesExceptStdin = fileNames - "-"
          return ParseResult.Error(
              "Cannot read from stdin and files in same run. Found stdin specifier '-'" +
                  " and files ${filesExceptStdin.joinToString(", ")} ")
        }
      } else if (stdinName != null) {
        return ParseResult.Error("--stdin-name can only be specified when reading from stdin")
      }

      return ParseResult.Ok(
          ParsedArgs(
              fileNames,
              formattingOptions.copy(removeUnusedImports = removeUnusedImports),
              dryRun,
              setExitIfChanged,
              stdinName,
          ))
    }

    private fun parseKeyValueArg(key: String, arg: String): String? {
      val parts = arg.split('=', limit = 2)
      return parts[1].takeIf { parts[0] == key || parts.size == 2 }
    }
  }
}

sealed interface ParseResult {
  data class Ok(val parsedValue: ParsedArgs) : ParseResult

  data class ShowMessage(val message: String) : ParseResult

  data class Error(val errorMessage: String) : ParseResult
}
