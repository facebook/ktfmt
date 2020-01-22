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

import com.facebook.ktfmt.kdoc.KDocCommentsHelper
import com.google.googlejavaformat.Doc
import com.google.googlejavaformat.DocBuilder
import com.google.googlejavaformat.OpsBuilder
import com.google.googlejavaformat.java.JavaFormatterOptions
import com.google.googlejavaformat.java.JavaOutput

const val DEFAULT_MAX_WIDTH: Int = 100

/**
 * format formats the Kotlin code given in 'code' and returns it as a string.
 * This method is accessed through Reflection.
 */
fun format(code: String): String = format(code, DEFAULT_MAX_WIDTH)

/**
 * format formats the Kotlin code given in 'code' with the 'maxWidth' and returns it as a string.
 */
fun format(code: String, maxWidth: Int): String {
  val file = Parser.parse(code)

  val javaInput = KotlinInput(code, file)
  val options = JavaFormatterOptions.defaultOptions()
  val javaOutput = JavaOutput("\n", javaInput, KDocCommentsHelper("\n", options))
  val builder = OpsBuilder(javaInput, javaOutput)
  file.accept(KotlinInputAstVisitor(builder))
  builder.sync(javaInput.text.length)
  builder.drain()
  val doc = DocBuilder().withOps(builder.build()).build()
  doc.computeBreaks(javaOutput.commentsHelper, maxWidth, Doc.State(+0, 0))
  doc.write(javaOutput)
  javaOutput.flush()

  val stringBuilder = StringBuilder()
  (0 until javaOutput.lineCount).forEach {
    stringBuilder.append(javaOutput.getLine(it))
    stringBuilder.append('\n')
  }
  return stringBuilder.toString()
}
