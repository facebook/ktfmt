/*
 * Copyright 2016 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

/*
 * This was copied from https://github.com/google/google-java-format and modified extensively to
 * work for Kotlin formatting
 */

package com.facebook.ktfmt.kdoc

import com.facebook.ktfmt.kdoc.KDocToken.Type.BEGIN_KDOC
import com.facebook.ktfmt.kdoc.KDocToken.Type.BLANK_LINE
import com.facebook.ktfmt.kdoc.KDocToken.Type.CODE
import com.facebook.ktfmt.kdoc.KDocToken.Type.CODE_BLOCK_MARKER
import com.facebook.ktfmt.kdoc.KDocToken.Type.CODE_CLOSE_TAG
import com.facebook.ktfmt.kdoc.KDocToken.Type.CODE_OPEN_TAG
import com.facebook.ktfmt.kdoc.KDocToken.Type.END_KDOC
import com.facebook.ktfmt.kdoc.KDocToken.Type.LIST_ITEM_OPEN_TAG
import com.facebook.ktfmt.kdoc.KDocToken.Type.LITERAL
import com.facebook.ktfmt.kdoc.KDocToken.Type.MARKDOWN_LINK
import com.facebook.ktfmt.kdoc.KDocToken.Type.PRE_CLOSE_TAG
import com.facebook.ktfmt.kdoc.KDocToken.Type.PRE_OPEN_TAG
import com.facebook.ktfmt.kdoc.KDocToken.Type.TABLE_CLOSE_TAG
import com.facebook.ktfmt.kdoc.KDocToken.Type.TABLE_OPEN_TAG
import com.facebook.ktfmt.kdoc.KDocToken.Type.TAG
import com.facebook.ktfmt.kdoc.KDocToken.Type.WHITESPACE
import java.util.regex.Pattern.compile
import org.jetbrains.kotlin.com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.kdoc.lexer.KDocLexer
import org.jetbrains.kotlin.kdoc.lexer.KDocTokens
import org.jetbrains.kotlin.lexer.KtTokens.WHITE_SPACE

/**
 * Entry point for formatting KDoc.
 *
 * This stateless class reads tokens from the stateful lexer and translates them to "requests" and
 * "writes" to the stateful writer. It also munges tokens into "standardized" forms. Finally, it
 * performs postprocessing to convert the written KDoc to a one-liner if possible or to leave a
 * single blank line if it's empty.
 */
object KDocFormatter {

  private val ONE_CONTENT_LINE_PATTERN = compile(" */[*][*]\n *[*] (.*)\n *[*]/")

  private val NUMBERED_LIST_PATTERN = "[0-9]+\\.".toRegex()

  /**
   * Formats the given Javadoc comment, which must start with ∕✱✱ and end with ✱∕. The output will
   * start and end with the same characters.
   */
  fun formatKDoc(input: String, blockIndent: Int, maxLineLength: Int): String {
    val escapedInput = Escaping.escapeKDoc(input)
    val kDocLexer = KDocLexer()
    kDocLexer.start(escapedInput)
    val tokens = mutableListOf<KDocToken>()
    var previousType: IElementType? = null
    while (kDocLexer.tokenType != null) {
      val tokenType = kDocLexer.tokenType
      val tokenText =
          with(kDocLexer.tokenText) {
            if (previousType == KDocTokens.LEADING_ASTERISK && first() == ' ') substring(1)
            else this
          }

      processToken(tokenType, tokens, tokenText, previousType)

      previousType = tokenType
      kDocLexer.advance()
    }
    val result = render(tokens, blockIndent, maxLineLength)
    return makeSingleLineIfPossible(blockIndent, result, maxLineLength)
  }

  private fun processToken(
      tokenType: IElementType?,
      tokens: MutableList<KDocToken>,
      tokenText: String,
      previousType: IElementType?
  ) {
    when (tokenType) {
      KDocTokens.START -> tokens.add(KDocToken(BEGIN_KDOC, tokenText))
      KDocTokens.END -> tokens.add(KDocToken(END_KDOC, tokenText))
      KDocTokens.LEADING_ASTERISK -> Unit // Ignore, no need to output anything
      KDocTokens.TAG_NAME -> tokens.add(KDocToken(TAG, tokenText))
      KDocTokens.CODE_BLOCK_TEXT -> tokens.add(KDocToken(CODE, tokenText))
      KDocTokens.MARKDOWN_INLINE_LINK,
      KDocTokens.MARKDOWN_LINK -> {
        tokens.add(KDocToken(MARKDOWN_LINK, tokenText))
      }
      KDocTokens.MARKDOWN_ESCAPED_CHAR,
      KDocTokens.TEXT -> {
        var first = true
        for (word in tokenizeKdocText(tokenText)) {
          if (word.first().isWhitespace()) {
            tokens.add(KDocToken(WHITESPACE, " "))
            continue
          }
          if (first) {
            if (word == "-" || word == "*" || word.matches(NUMBERED_LIST_PATTERN)) {
              tokens.add(KDocToken(LIST_ITEM_OPEN_TAG, ""))
            }
            first = false
          }
          // If the KDoc is malformed (e.g. unclosed code block) KDocLexer doesn't report an
          // END_KDOC properly. We want to recover in such cases
          if (word == "*/") {
            tokens.add(KDocToken(END_KDOC, word))
          } else if (word.startsWith("```")) {
            tokens.add(KDocToken(CODE_BLOCK_MARKER, word))
          } else {
            tokens.add(KDocToken(LITERAL, word))
          }
        }
      }
      WHITE_SPACE -> {
        if (previousType == KDocTokens.LEADING_ASTERISK || tokenText.count { it == '\n' } >= 2) {
          tokens.add(KDocToken(BLANK_LINE, ""))
        } else {
          tokens.add(KDocToken(WHITESPACE, " "))
        }
      }
      else -> throw RuntimeException("Unexpected: $tokenType")
    }
  }
  private fun render(input: List<KDocToken>, blockIndent: Int, maxLineLength: Int): String {
    val output = KDocWriter(blockIndent, maxLineLength)
    for (token in input) {
      when (token.type) {
        BEGIN_KDOC -> output.writeBeginJavadoc()
        END_KDOC -> {
          output.writeEndJavadoc()
          return Escaping.unescapeKDoc(output.toString())
        }
        LIST_ITEM_OPEN_TAG -> output.writeListItemOpen(token)
        PRE_OPEN_TAG -> output.writePreOpen(token)
        PRE_CLOSE_TAG -> output.writePreClose(token)
        CODE_OPEN_TAG -> output.writeCodeOpen(token)
        CODE_CLOSE_TAG -> output.writeCodeClose(token)
        TABLE_OPEN_TAG -> output.writeTableOpen(token)
        TABLE_CLOSE_TAG -> output.writeTableClose(token)
        TAG -> output.writeTag(token)
        CODE -> output.writeCodeLine(token)
        CODE_BLOCK_MARKER -> output.writeExplicitCodeBlockMarker(token)
        BLANK_LINE -> output.requestBlankLine()
        WHITESPACE -> output.requestWhitespace()
        LITERAL -> output.writeLiteral(token)
        MARKDOWN_LINK -> output.writeMarkdownLink(token)
        else -> throw AssertionError(token.type)
      }
    }
    throw AssertionError()
  }

  /**
   * Returns the given string or a one-line version of it (e.g., "∕✱✱ Tests for foos. ✱∕") if it
   * fits on one line.
   */
  private fun makeSingleLineIfPossible(
      blockIndent: Int,
      input: String,
      maxLineLength: Int
  ): String {
    val oneLinerContentLength = maxLineLength - "/**  */".length - blockIndent
    val matcher = ONE_CONTENT_LINE_PATTERN.matcher(input)
    if (matcher.matches() && matcher.group(1).isEmpty()) {
      return "/** */"
    } else if (matcher.matches() && matcher.group(1).length <= oneLinerContentLength) {
      return "/** " + matcher.group(1) + " */"
    }
    return input
  }

  /**
   * tokenizeKdocText splits 's' by whitespace, and returns both whitespace and non-whitespace
   * parts.
   *
   * Multiple adjacent whitespace characters are collapsed into one. Trailing and leading spaces are
   * included in the result.
   *
   * Example: `" one two three "` becomes `[" ", "one", " ", "two", " ", "three", " "]`. See tests
   * for more examples.
   */
  fun tokenizeKdocText(s: String) = sequence {
    if (s.isEmpty()) {
      return@sequence
    }
    var mark = 0
    var inWhitespace = s[0].isWhitespace()
    for (i in 1..s.lastIndex) {
      if (inWhitespace == s[i].isWhitespace()) {
        continue
      }
      val result = if (inWhitespace) " " else s.substring(mark, i)
      inWhitespace = s[i].isWhitespace()
      mark = i
      yield(result)
    }
    yield(if (inWhitespace) " " else s.substring(mark, s.length))
  }
}
