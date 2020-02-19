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
import com.google.common.collect.ImmutableList
import com.google.common.collect.Range
import com.google.googlejavaformat.Doc
import com.google.googlejavaformat.DocBuilder
import com.google.googlejavaformat.OpsBuilder
import com.google.googlejavaformat.java.JavaOutput

const val DEFAULT_MAX_WIDTH: Int = 100

class FormattingOptions(
    /** ktfmt breaks lines longer than maxWidth. */
    val maxWidth: Int = DEFAULT_MAX_WIDTH,

    /**
     * blockIndent is the size of the indent used when a new block is opened, in spaces.
     *
     * For example, ```
     * fun f() {
     *   //
     * }
     * ```
     */
    val blockIndent: Int = 2,

    /**
     * continuationIndent is the size of the indent used when a line is broken because it's too
     * long, in spaces.
     *
     * For example, ```
     * val foo = bar(
     *     1)
     * ```
     */
    val continuationIndent: Int = 4)

/**
 * format formats the Kotlin code given in 'code' and returns it as a string. This method is
 * accessed through Reflection.
 */
fun format(code: String): String = format(FormattingOptions(), code)

/**
 * format formats the Kotlin code given in 'code' with the 'maxWidth' and returns it as a string.
 */
fun format(options: FormattingOptions, code: String): String {
  val file = Parser.parse(code)

  val kotlinInput = KotlinInput(code, file)
  val javaOutput = JavaOutput("\n", kotlinInput, KDocCommentsHelper("\n"))
  val builder = OpsBuilder(kotlinInput, javaOutput)
  file.accept(KotlinInputAstVisitor(options.blockIndent, options.continuationIndent, builder))
  builder.sync(kotlinInput.text.length)
  builder.drain()
  val doc = DocBuilder().withOps(builder.build()).build()
  doc.computeBreaks(javaOutput.commentsHelper, options.maxWidth, Doc.State(+0, 0))
  doc.write(javaOutput)
  javaOutput.flush()

  val tokenRangeSet =
      kotlinInput.characterRangesToTokenRanges(ImmutableList.of(Range.closedOpen(0, code.length)))
  return JavaOutput.applyReplacements(code, javaOutput.getFormatReplacements(tokenRangeSet))
}
