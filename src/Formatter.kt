package com.google.googlejavaformat.kotlin

import com.google.googlejavaformat.Doc
import com.google.googlejavaformat.DocBuilder
import com.google.googlejavaformat.OpsBuilder
import com.google.googlejavaformat.java.JavaCommentsHelper
import com.google.googlejavaformat.java.JavaFormatterOptions
import com.google.googlejavaformat.java.JavaOutput

/**
 * format formats the Kotlin code given in 'code' and returns it as a string.
 */
fun format(code: String): String {
  val file = Parser.parse(code)

  val javaInput = KotlinInput(code, file)
  val options = JavaFormatterOptions.defaultOptions()
  val javaOutput = JavaOutput("\n", javaInput, JavaCommentsHelper("\n", options))
  val builder = OpsBuilder(javaInput, javaOutput)
  file.accept(KotlinInputAstVisitor(builder, 1))
  builder.sync(javaInput.text.length)
  builder.drain()
  val doc = DocBuilder().withOps(builder.build()).build()
  doc.computeBreaks(javaOutput.commentsHelper, 100, Doc.State(+0, 0))
  doc.write(javaOutput)
  javaOutput.flush()

  return (0 until javaOutput.lineCount).joinToString("\n") { javaOutput.getLine(it) }
}
