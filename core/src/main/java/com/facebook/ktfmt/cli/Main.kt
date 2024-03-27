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
import com.facebook.ktfmt.format.ParseError
import com.google.googlejavaformat.FormattingError
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.PrintStream
import java.nio.charset.StandardCharsets.UTF_8
import java.util.concurrent.atomic.AtomicInteger
import kotlin.system.exitProcess

class Main(
    private val input: InputStream,
    private val out: PrintStream,
    private val err: PrintStream,
    args: Array<String>
) {
  companion object {
    @JvmStatic
    fun main(args: Array<String>) {
      exitProcess(Main(System.`in`, System.out, System.err, args).run())
    }

    /**
     * expandArgsToFileNames expands 'args' to a list of .kt files to format.
     *
     * Most commonly, 'args' is either a list of .kt files, or a name of a directory whose contents
     * the user wants to format.
     */
    fun expandArgsToFileNames(args: List<String>): List<File> {
      if (args.size == 1 && File(args[0]).isFile) {
        return listOf(File(args[0]))
      }
      val result = mutableListOf<File>()
      for (arg in args) {
        if (arg == "-") {
          error(
              "Error: '-', which causes ktfmt to read from stdin, should not be mixed with file name")
        }
        result.addAll(
            File(arg).walkTopDown().filter {
              it.isFile && (it.extension == "kt" || it.extension == "kts")
            })
      }
      return result
    }
  }

  private val parsedArgs: ParsedArgs = ParsedArgs.processArgs(err, args)

  fun run(): Int {
    if (parsedArgs.fileNames.isEmpty()) {
      err.println(
          "Usage: ktfmt [--dropbox-style | --google-style | --kotlinlang-style] [--dry-run] [--set-exit-if-changed] [--stdin-name=<name>] [--do-not-remove-unused-imports] File1.kt File2.kt ...")
      err.println("Or: ktfmt @file")
      return 1
    }

    if (parsedArgs.fileNames.size == 1 && parsedArgs.fileNames[0] == "-") {
      return try {
        val alreadyFormatted = format(null)
        if (!alreadyFormatted && parsedArgs.setExitIfChanged) 1 else 0
      } catch (e: Exception) {
        1
      }
    } else if (parsedArgs.stdinName != null) {
      err.println("Error: --stdin-name can only be used with stdin")
      return 1
    }

    val files: List<File>
    try {
      files = expandArgsToFileNames(parsedArgs.fileNames)
    } catch (e: java.lang.IllegalStateException) {
      err.println(e.message)
      return 1
    }

    if (files.isEmpty()) {
      err.println("Error: no .kt files found")
      return 1
    }

    val retval = AtomicInteger(0)
    files.parallelStream().forEach {
      try {
        if (!format(it) && parsedArgs.setExitIfChanged) {
          retval.set(1)
        }
      } catch (e: Exception) {
        retval.set(1)
      }
    }
    return retval.get()
  }

  /**
   * Handles the logic for formatting and flags.
   *
   * If dry run mode is active, this simply prints the name of the [source] (file path or `<stdin>`)
   * to [out]. Otherwise, this will run the appropriate formatting as normal.
   *
   * @param file The file to format. If null, the code is read from <stdin>.
   * @return true iff input is valid and already formatted.
   */
  private fun format(file: File?): Boolean {
    val fileName = file?.toString() ?: parsedArgs.stdinName ?: "<stdin>"
    try {
      val bytes = if (file == null) input else FileInputStream(file)
      val code = BufferedReader(InputStreamReader(bytes, UTF_8)).readText()
      val formattedCode = Formatter.format(parsedArgs.formattingOptions, code)
      val alreadyFormatted = code == formattedCode

      // stdin
      if (file == null) {
        if (parsedArgs.dryRun) {
          if (!alreadyFormatted) {
            out.println(fileName)
          }
        } else {
          BufferedWriter(OutputStreamWriter(out, UTF_8)).use { it.write(formattedCode) }
        }
        return alreadyFormatted
      }

      if (parsedArgs.dryRun) {
        if (!alreadyFormatted) {
          out.println(fileName)
        }
      } else {
        // TODO(T111284144): Add tests
        if (!alreadyFormatted) {
          file.writeText(formattedCode, UTF_8)
        }
        err.println("Done formatting $fileName")
      }

      return alreadyFormatted
    } catch (e: IOException) {
      err.println("Error formatting $fileName: ${e.message}; skipping.")
      throw e
    } catch (e: ParseError) {
      err.println("$fileName:${e.message}")
      throw e
    } catch (e: FormattingError) {
      for (diagnostic in e.diagnostics()) {
        err.println("$fileName:$diagnostic")
      }
      e.printStackTrace(err)
      throw e
    }
  }
}
