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

package com.facebook.ktfmt

import com.facebook.ktfmt.format.DROPBOX_FORMAT
import com.facebook.ktfmt.format.FormattingOptions
import com.facebook.ktfmt.format.GOOGLE_FORMAT
import com.facebook.ktfmt.format.KOTLINLANG_FORMAT
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
)

/** parseOptions parses command-line arguments passed to ktfmt. */
fun parseOptions(err: PrintStream, args: Array<String>): ParsedArgs {
  val fileNames = mutableListOf<String>()
  var formattingOptions = FormattingOptions()
  var dryRun = false
  var setExitIfChanged = false

  for (arg in args) {
    when {
      arg == "--dropbox-style" -> formattingOptions = DROPBOX_FORMAT
      arg == "--google-style" -> formattingOptions = GOOGLE_FORMAT
      arg == "--kotlinlang-style" -> formattingOptions = KOTLINLANG_FORMAT
      arg == "--dry-run" || arg == "-n" -> dryRun = true
      arg == "--set-exit-if-changed" -> setExitIfChanged = true
      arg.startsWith("--") -> err.println("Unexpected option: $arg")
      else -> fileNames.add(arg)
    }
  }
  return ParsedArgs(fileNames, formattingOptions, dryRun, setExitIfChanged)
}
