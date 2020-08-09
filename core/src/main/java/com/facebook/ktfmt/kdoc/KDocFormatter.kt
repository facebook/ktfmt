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

import com.facebook.ktfmt.kdoc.Token.Type.BEGIN_KDOC
import com.facebook.ktfmt.kdoc.Token.Type.BLANK_LINE
import com.facebook.ktfmt.kdoc.Token.Type.CODE
import com.facebook.ktfmt.kdoc.Token.Type.CODE_BLOCK_MARKER
import com.facebook.ktfmt.kdoc.Token.Type.CODE_CLOSE_TAG
import com.facebook.ktfmt.kdoc.Token.Type.CODE_OPEN_TAG
import com.facebook.ktfmt.kdoc.Token.Type.END_KDOC
import com.facebook.ktfmt.kdoc.Token.Type.LIST_ITEM_OPEN_TAG
import com.facebook.ktfmt.kdoc.Token.Type.LITERAL
import com.facebook.ktfmt.kdoc.Token.Type.MARKDOWN_LINK
import com.facebook.ktfmt.kdoc.Token.Type.PRE_CLOSE_TAG
import com.facebook.ktfmt.kdoc.Token.Type.PRE_OPEN_TAG
import com.facebook.ktfmt.kdoc.Token.Type.TABLE_CLOSE_TAG
import com.facebook.ktfmt.kdoc.Token.Type.TABLE_OPEN_TAG
import com.facebook.ktfmt.kdoc.Token.Type.TAG
import com.facebook.ktfmt.kdoc.Token.Type.WHITESPACE
import org.jetbrains.kotlin.com.intellij.psi.tree.IElementType
import java.util.regex.Pattern.compile
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

  internal val MAX_LINE_LENGTH = 100

  private val ONE_CONTENT_LINE_PATTERN = compile(" */[*][*]\n *[*] (.*)\n *[*]/")

  private val NUMBERED_LIST_PATTERN = "[0-9]+\\.".toRegex()

  /**
   * Formats the given Javadoc comment, which must start with ∕✱✱ and end with ✱∕. The output will
   * start and end with the same characters.
   */
  fun formatKDoc(input: String, blockIndent: Int): String {
    val kDocLexer = KDocLexer()
    kDocLexer.start(input)
    val tokens = mutableListOf<Token>()
    var previousType: IElementType? = null
    while (kDocLexer.tokenType != null) {
      val tokenType = kDocLexer.tokenType
      val tokenText =
          with(kDocLexer.tokenText) {
            if (previousType == KDocTokens.LEADING_ASTERISK && first() == ' ') substring(1)
            else this
          }

      when (tokenType) {
        KDocTokens.START -> tokens.add(Token(BEGIN_KDOC, tokenText))
        KDocTokens.END -> tokens.add(Token(END_KDOC, tokenText))
        KDocTokens.LEADING_ASTERISK -> Unit // Ignore, no need to output anything
        KDocTokens.TAG_NAME -> tokens.add(Token(TAG, tokenText))
        KDocTokens.CODE_BLOCK_TEXT -> tokens.add(Token(CODE, tokenText))
        KDocTokens.MARKDOWN_INLINE_LINK, KDocTokens.MARKDOWN_LINK -> {
          tokens.add(Token(MARKDOWN_LINK, tokenText))
        }
        KDocTokens.TEXT -> {
          if (tokenText.isBlank()) {
            tokens.add(Token(WHITESPACE, " "))
          } else {
            val words = tokenText.trim().split(" +".toRegex())
            var first = true
            for (word in words) {
              if (first) {
                if (word == "-" || word == "*" || word.matches(NUMBERED_LIST_PATTERN)) {
                  tokens.add(Token(LIST_ITEM_OPEN_TAG, ""))
                }
                first = false
              }
              // If the KDoc is malformed (e.g. unclosed code block) KDocLexer doesn't report an
              // END_KDOC properly. We want to recover in such cases
              if (word == "*/") {
                tokens.add(Token(END_KDOC, word))
              } else if (word == "```") {
                tokens.add(Token(CODE_BLOCK_MARKER, word))
              } else {
                tokens.add(Token(LITERAL, word))
                tokens.add(Token(WHITESPACE, " "))
              }
            }
          }
        }
        WHITE_SPACE -> {
          if (previousType === KDocTokens.TAG_NAME || previousType === KDocTokens.MARKDOWN_LINK) {
            tokens.add(Token(WHITESPACE, " "))
          } else if (previousType == KDocTokens.LEADING_ASTERISK ||
              tokenText.count { it == '\n' } >= 2) {
            tokens.add(Token(BLANK_LINE, ""))
          }
        }
        else -> throw RuntimeException("Unexpected: $tokenType")
      }

      previousType = tokenType
      kDocLexer.advance()
    }
    val result = render(tokens, blockIndent)
    return makeSingleLineIfPossible(blockIndent, result)
  }

  private fun render(input: List<Token>, blockIndent: Int): String {
    val output = KDocWriter(blockIndent)
    for (token in input) {
      when (token.type) {
        BEGIN_KDOC -> output.writeBeginJavadoc()
        END_KDOC -> {
          output.writeEndJavadoc()
          return output.toString()
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
  private fun makeSingleLineIfPossible(blockIndent: Int, input: String): String {
    val oneLinerContentLength = MAX_LINE_LENGTH - "/**  */".length - blockIndent
    val matcher = ONE_CONTENT_LINE_PATTERN.matcher(input)
    if (matcher.matches() && matcher.group(1).isEmpty()) {
      return "/** */"
    } else if (matcher.matches() && matcher.group(1).length <= oneLinerContentLength) {
      return "/** " + matcher.group(1) + " */"
    }
    return input
  }
}
