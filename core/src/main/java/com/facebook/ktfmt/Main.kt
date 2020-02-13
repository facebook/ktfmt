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

import com.google.googlejavaformat.FormattingError
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.PrintStream

fun main(args: Array<String>) {
  if (args.isEmpty()) {
    println("Usage: ktfmt File1.kt File2.kt ...")
    return
  }

  if (args.size == 1 && args[0] == "-") {
    formatStdin(System.`in`, System.out)
    return
  }

  val fileNames: List<File>
  try {
    fileNames = expandArgsToFileNames(args)
  } catch (e: java.lang.IllegalStateException) {
    println(e.message)
    return
  }

  if (fileNames.isEmpty()) {
    println("Error: no .kt files found")
    return
  }

  val printStack = fileNames.size == 1
  fileNames.parallelStream().forEach { formatFile(it, printStack) }
}

fun formatStdin(inputStream: InputStream, printStream: PrintStream) {
  val code = BufferedReader(InputStreamReader(inputStream)).readText()
  printStream.print(format(code))
}

/**
 * expandArgsToFileNames expands 'args' to a list of .kt files to format.
 *
 * Most commonly, 'args' is either a list of .kt files, or a name of a directory whose contents the
 * user wants to format.
 */
fun expandArgsToFileNames(args: Array<String>): List<File> {
  if (args.size == 1 && File(args[0]).isFile) {
    return listOf(File(args[0]))
  }
  val result = mutableListOf<File>()
  for (arg in args) {
    if (arg == "-") {
      error(
          "Error: '-', which causes ktfmt to read from stdin, should not be mixed with file name")
    }
    result.addAll(File(arg).walkTopDown().filter { it.isFile && it.extension == "kt" })
  }
  return result
}

private fun formatFile(file: File, printStack: Boolean) {
  try {
    val code = file.readText()
    file.writeText(format(code))
    println("Done formatting $file")
  } catch (e: IOException) {
    println("Error formatting $file: ${e.message}; skipping.")
  } catch (e: FormattingError) {
    println("Formatting Error when processing $file: ${e.message}; skipping.")
    if (printStack) {
      e.printStackTrace()
    }
  }
}
