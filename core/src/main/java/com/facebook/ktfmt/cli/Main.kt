/*
 * Copyright (c) Facebook, Inc. and its affiliates.
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

import com.facebook.ktfmt.format.ParseError
import com.facebook.ktfmt.format.format
import com.google.googlejavaformat.FormattingError
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.PrintStream
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.system.exitProcess

fun main(args: Array<String>) {
  exitProcess(Main(System.`in`, System.out, System.err, args).run())
}

class Main(
    private val input: InputStream,
    private val out: PrintStream,
    private val err: PrintStream,
    args: Array<String>
) {
  private val parsedArgs: ParsedArgs = parseOptions(err, args)

  fun run(): Int {
    if (parsedArgs.fileNames.isEmpty()) {
      err.println(
          "Usage: ktfmt [--dropbox-style | --google-style | --kotlinlang-style] File1.kt File2.kt ...")
      return 1
    }

    if (parsedArgs.fileNames.size == 1 && parsedArgs.fileNames[0] == "-") {
      val success = format(null)
      return if (success) 0 else 1
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

    val success = AtomicBoolean(true)
    files.parallelStream().forEach { success.compareAndSet(true, format(it)) }
    return if (success.get()) 0 else 1
  }

  /**
   * Handles the logic for formatting and flags.
   *
   * If dry run mode is active, this simply prints the name of the [source] (file path or `<stdin>`)
   * to [out]. Otherwise, this will run the appropriate formatting as normal.
   *
   * @param file The file to format. If null, the code is read from <stdin>.
   * @return True if changes were made or no changes were needed, false if there was a failure or if
   * changes were made and the --set-exit-if-changed flag is set.
   */
  private fun format(file: File?): Boolean {
    val fileName = file?.toString() ?: "<stdin>"
    try {
      val code = file?.readText() ?: BufferedReader(InputStreamReader(input)).readText()

      val formattedCode = format(parsedArgs.formattingOptions, code)

      if (code == formattedCode) {
        // The code was already formatted, nothing more to do here
        err.println("No changes: $fileName")
        return true
      }

      if (parsedArgs.dryRun) {
        out.println(fileName)
      } else {
        if (file != null) {
          file.writeText(formattedCode)
        } else {
          out.print(formattedCode)
        }
        err.println("Done formatting $fileName")
      }

      // If setExitIfChanged is true, then any change should result in an exit code of 1.
      return if (parsedArgs.setExitIfChanged) false else true
    } catch (e: IOException) {
      err.println("Error formatting $fileName: ${e.message}; skipping.")
    } catch (e: ParseError) {
      handleParseError(fileName, e)
    } catch (e: FormattingError) {
      for (diagnostic in e.diagnostics()) {
        System.err.println("$fileName:$diagnostic")
      }
      e.printStackTrace(err)
    }
    return false
  }

  private fun handleParseError(fileName: String, e: ParseError) {
    err.println("$fileName:${e.message}")
  }
}

/**
 * expandArgsToFileNames expands 'args' to a list of .kt files to format.
 *
 * Most commonly, 'args' is either a list of .kt files, or a name of a directory whose contents the
 * user wants to format.
 */
fun expandArgsToFileNames(args: List<String>): List<File> {
  if (args.size == 1 && File(args[0]).isFile) {
    return listOf(File(args[0]))
  }
  val result = mutableListOf<File>()
  for (arg in args) {
    if (arg == "-") {
      error("Error: '-', which causes ktfmt to read from stdin, should not be mixed with file name")
    }
    result.addAll(
        File(arg).walkTopDown().filter {
          it.isFile && (it.extension == "kt" || it.extension == "kts")
        })
  }
  return result
}
