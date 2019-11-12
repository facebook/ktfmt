package com.facebook.ktfmt

import com.google.googlejavaformat.FormattingError
import java.io.File
import java.io.IOException

fun main(args: Array<String>) {
  if (args.isEmpty()) {
    println("Usage: ktfmt File1.kt File2.kt ...")
    return
  }

  for (fileName in args) try {
    val code = File(fileName).readText()
    File(fileName).writeText(format(code))
    println("Done formatting $fileName")
  } catch (e: IOException) {
    println("Error formatting $fileName: ${e.message}; skipping.")
  } catch (e: FormattingError) {
    println("Formatting Error when processing $fileName: ${e.message}; skipping.")
    if (args.size == 1) {
      e.printStackTrace()
    }
  }
}
