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

import java.io.PrintStream

/** ParsedArgs holds the arguments passed to ktfmt on the command-line, after parsing. */
data class ParsedArgs(val fileNames: List<String>, val formattingOptions: FormattingOptions)

/** parseOptions parses command-line arguments passed to ktfmt. */
fun parseOptions(err: PrintStream, args: Array<String>): ParsedArgs {
  val fileNames = mutableListOf<String>()
  var formattingOptions = FormattingOptions()
  for (arg in args) {
    when {
      arg == "--dropbox-style" -> formattingOptions = FormattingOptions.dropboxStyle()
      arg == "--google-style" -> formattingOptions = FormattingOptions.googleStyle()
      arg.startsWith("--") -> err.println("Unexpected option: $arg")
      else -> fileNames.add(arg)
    }
  }
  return ParsedArgs(fileNames, formattingOptions)
}
