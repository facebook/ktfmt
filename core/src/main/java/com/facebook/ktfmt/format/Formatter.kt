/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
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

package com.facebook.ktfmt.format

import com.facebook.ktfmt.debughelpers.printOps
import com.facebook.ktfmt.format.RedundantElementManager.addRedundantElements
import com.facebook.ktfmt.format.RedundantElementManager.dropRedundantElements
import com.facebook.ktfmt.format.WhitespaceTombstones.indexOfWhitespaceTombstone
import com.facebook.ktfmt.kdoc.Escaping
import com.facebook.ktfmt.kdoc.KDocCommentsHelper
import com.google.common.collect.ImmutableList
import com.google.common.collect.Range
import com.google.googlejavaformat.Doc
import com.google.googlejavaformat.DocBuilder
import com.google.googlejavaformat.Newlines
import com.google.googlejavaformat.OpsBuilder
import com.google.googlejavaformat.java.FormatterException
import com.google.googlejavaformat.java.JavaOutput
import org.jetbrains.kotlin.com.intellij.openapi.util.text.StringUtil
import org.jetbrains.kotlin.com.intellij.openapi.util.text.StringUtilRt.convertLineSeparators
import org.jetbrains.kotlin.com.intellij.psi.PsiComment
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset

object Formatter {

  @JvmField
  val META_FORMAT =
      FormattingOptions(
          blockIndent = 2,
          continuationIndent = 4,
          manageTrailingCommas = false,
      )

  @JvmField
  val GOOGLE_FORMAT =
      FormattingOptions(
          blockIndent = 2,
          continuationIndent = 2,
      )

  /** A format that attempts to reflect https://kotlinlang.org/docs/coding-conventions.html. */
  @JvmField
  val KOTLINLANG_FORMAT =
      FormattingOptions(
          blockIndent = 4,
          continuationIndent = 4,
      )

  private val MINIMUM_KOTLIN_VERSION = KotlinVersion(1, 4)

  /**
   * format formats the Kotlin code given in 'code' and returns it as a string. This method is
   * accessed through Reflection.
   */
  @JvmStatic
  @Throws(FormatterException::class, ParseError::class)
  fun format(code: String): String = format(META_FORMAT, code)

  /**
   * format formats the Kotlin code given in 'code' with 'removeUnusedImports' and returns it as a
   * string. This method is accessed through Reflection.
   */
  @JvmStatic
  @Throws(FormatterException::class, ParseError::class)
  fun format(code: String, removeUnusedImports: Boolean): String =
      format(META_FORMAT.copy(removeUnusedImports = removeUnusedImports), code)

  /**
   * format formats the Kotlin code given in 'code' with the 'maxWidth' and returns it as a string.
   */
  @JvmStatic
  @Throws(FormatterException::class, ParseError::class)
  fun format(options: FormattingOptions, code: String): String {
    val (shebang, kotlinCode) =
        if (code.startsWith("#!")) {
          code.split("\n".toRegex(), limit = 2)
        } else {
          listOf("", code)
        }
    checkEscapeSequences(kotlinCode)

    return kotlinCode
        .let { convertLineSeparators(it) }
        .let { sortedAndDistinctImports(it) }
        .let { dropRedundantElements(it, options) }
        .let { prettyPrint(it, options, "\n") }
        .let { addRedundantElements(it, options) }
        .let { convertLineSeparators(it, checkNotNull(Newlines.guessLineSeparator(kotlinCode))) }
        .let { if (shebang.isEmpty()) it else shebang + "\n" + it }
  }

  /** prettyPrint reflows 'code' using google-java-format's engine. */
  private fun prettyPrint(code: String, options: FormattingOptions, lineSeparator: String): String {
    val file = Parser.parse(code)
    val kotlinInput = KotlinInput(code, file)
    val javaOutput =
        JavaOutput(lineSeparator, kotlinInput, KDocCommentsHelper(lineSeparator, options.maxWidth))
    val builder = OpsBuilder(kotlinInput, javaOutput)
    file.accept(createAstVisitor(options, builder))
    builder.sync(kotlinInput.text.length)
    builder.drain()
    val ops = builder.build()
    if (options.debuggingPrintOpsAfterFormatting) {
      printOps(ops)
    }
    val doc = DocBuilder().withOps(ops).build()
    doc.computeBreaks(javaOutput.commentsHelper, options.maxWidth, Doc.State(+0, 0))
    doc.write(javaOutput)
    javaOutput.flush()

    val tokenRangeSet =
        kotlinInput.characterRangesToTokenRanges(ImmutableList.of(Range.closedOpen(0, code.length)))
    return WhitespaceTombstones.replaceTombstoneWithTrailingWhitespace(
        JavaOutput.applyReplacements(code, javaOutput.getFormatReplacements(tokenRangeSet)))
  }

  private fun createAstVisitor(options: FormattingOptions, builder: OpsBuilder): PsiElementVisitor {
    if (KotlinVersion.CURRENT < MINIMUM_KOTLIN_VERSION) {
      throw RuntimeException("Unsupported runtime Kotlin version: " + KotlinVersion.CURRENT)
    }
    return KotlinInputAstVisitor(options, builder)
  }

  private fun checkEscapeSequences(code: String) {
    var index = code.indexOfWhitespaceTombstone()
    if (index == -1) {
      index = Escaping.indexOfCommentEscapeSequences(code)
    }
    if (index != -1) {
      throw ParseError(
          "ktfmt does not support code which contains one of {\\u0003, \\u0004, \\u0005} character" +
              "; escape it",
          StringUtil.offsetToLineColumn(code, index))
    }
  }

  private fun sortedAndDistinctImports(code: String): String {
    val file = Parser.parse(code)

    val importList = file.importList ?: return code
    if (importList.imports.isEmpty()) {
      return code
    }

    val commentList = mutableListOf<PsiElement>()
    // Find non-import elements; comments are moved, in order, to the top of the import list. Other
    // non-import elements throw a ParseError.
    var element = importList.firstChild
    while (element != null) {
      if (element is PsiComment) {
        commentList.add(element)
      } else if (element !is KtImportDirective && element !is PsiWhiteSpace) {
        throw ParseError(
            "Imports not contiguous: " + element.text,
            StringUtil.offsetToLineColumn(code, element.startOffset))
      }
      element = element.nextSibling
    }
    fun canonicalText(importDirective: KtImportDirective) =
        importDirective.importedFqName?.asString() +
            " " +
            importDirective.alias?.text?.replace("`", "") +
            " " +
            if (importDirective.isAllUnder) "*" else ""

    val sortedImports = importList.imports.sortedBy(::canonicalText).distinctBy(::canonicalText)
    val importsWithComments = commentList + sortedImports

    return code.replaceRange(
        importList.startOffset,
        importList.endOffset,
        importsWithComments.joinToString(separator = "\n") { imprt -> imprt.text } + "\n")
  }
}
