// (c) Facebook, Inc. and its affiliates. Confidential and proprietary.

package com.facebook.ktfmt

import java.io.PrintStream

/** ParsedArgs holds the arguments passed to ktfmt on the command-line, after parsing. */
data class ParsedArgs(val fileNames: List<String>, val formattingOptions: FormattingOptions)

/** parseOptions parses command-line arguments passed to ktfmt. */
fun parseOptions(err: PrintStream, args: Array<String>): ParsedArgs {
  val fileNames = mutableListOf<String>()
  var isDropboxStyle = false
  for (arg in args) {
    when {
      arg == "--dropbox-style" -> isDropboxStyle = true
      arg.startsWith("--") -> err.println("Unexpected option: $arg")
      else -> fileNames.add(arg)
    }
  }
  return ParsedArgs(
      fileNames,
      if (isDropboxStyle) FormattingOptions(blockIndent = 4, continuationIndent = 4)
      else FormattingOptions())
}
