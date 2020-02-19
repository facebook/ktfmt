// (c) Facebook, Inc. and its affiliates. Confidential and proprietary.

package com.facebook.ktfmt

import java.io.PrintStream

/**
 * ParsedArgs holds the arguments passed to ktfmt on the command-line, after parsing.
 */
data class ParsedArgs(val fileNames: List<String>)

/**
 * parseOptions parses command-line arguments passed to ktfmt.
 */
fun parseOptions(err: PrintStream, args: Array<String>): ParsedArgs {
  val fileNames = mutableListOf<String>()
  for (arg in args) {
    if (arg.startsWith("--")) {
      err.println("Unexpected option: $arg")
    } else {
      fileNames.add(arg)
    }
  }
  return ParsedArgs(fileNames)
}
