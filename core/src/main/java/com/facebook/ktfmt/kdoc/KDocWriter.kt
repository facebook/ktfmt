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

import com.facebook.ktfmt.kdoc.KDocWriter.AutoIndent.AUTO_INDENT
import com.facebook.ktfmt.kdoc.KDocWriter.AutoIndent.NO_AUTO_INDENT
import com.facebook.ktfmt.kdoc.KDocWriter.RequestedWhitespace.BLANK_LINE
import com.facebook.ktfmt.kdoc.KDocWriter.RequestedWhitespace.CONDITIONAL_WHITESPACE
import com.facebook.ktfmt.kdoc.KDocWriter.RequestedWhitespace.NEWLINE
import com.facebook.ktfmt.kdoc.KDocWriter.RequestedWhitespace.NONE
import com.facebook.ktfmt.kdoc.KDocWriter.RequestedWhitespace.WHITESPACE
import com.facebook.ktfmt.kdoc.Token.Type.HEADER_OPEN_TAG
import com.facebook.ktfmt.kdoc.Token.Type.LIST_ITEM_OPEN_TAG
import com.facebook.ktfmt.kdoc.Token.Type.PARAGRAPH_OPEN_TAG
import com.google.common.base.Strings
import com.google.common.collect.Ordering
import com.google.common.collect.Sets.immutableEnumSet

/**
 * Stateful object that accepts "requests" and "writes," producing formatted Javadoc.
 *
 * Our Javadoc formatter doesn't ever generate a parse tree, only a stream of tokens, so the writer
 * must compute and store the answer to questions like "How many levels of nested HTML list are we
 * inside?"
 */
internal class KDocWriter(private val blockIndent: Int) {

  /**
   * Tokens that are always pinned to the following token. For example, `<p>` in `<p>Foo bar` (never
   * `<p> Foo bar` or `<p>\nFoo bar`).
   *
   * This is not the only kind of "pinning" that we do: See also the joining of LITERAL tokens done
   * by the lexer. The special pinning here is necessary because these tokens are not of type
   * LITERAL (because they require other special handling).
   */
  private val START_OF_LINE_TOKENS =
      immutableEnumSet(LIST_ITEM_OPEN_TAG, PARAGRAPH_OPEN_TAG, HEADER_OPEN_TAG)

  private val output = StringBuilder()
  /**
   * Whether we are inside an `<li>` element, excluding the case in which the `<li>` contains a
   * `<ul>` or `<ol>` that we are also inside -- unless of course we're inside an `<li>` element in
   * that inner list :)
   */
  private var continuingListItemOfInnermostList: Boolean = false

  private val continuingListItemCount = NestingCounter()
  private val continuingListCount = NestingCounter()
  private var remainingOnLine: Int = 0
  private var atStartOfLine: Boolean = false
  private var requestedWhitespace = NONE
  private var inCodeBlock = false

  /**
   * Requests whitespace between the previously written token and the next written token. The
   * request may be honored, or it may be overridden by a request for "more significant" whitespace,
   * like a newline.
   */
  fun requestWhitespace() {
    requestWhitespace(WHITESPACE)
  }

  fun writeBeginJavadoc() {
    /*
     * JavaCommentsHelper will make sure this is indented right. But it seems sensible enough that,
     * if our input starts with ∕✱✱, so too does our output.
     */
    output.append("/**")
  }

  fun writeEndJavadoc() {
    output.append("\n")
    appendSpaces(blockIndent + 1)
    output.append("*/")
  }

  fun writeListItemOpen(token: Token) {
    requestNewline()

    if (continuingListItemOfInnermostList) {
      continuingListItemOfInnermostList = false
      continuingListItemCount.decrementIfPositive()
    }
    writeToken(token)
    continuingListItemOfInnermostList = true
    continuingListItemCount.increment()
  }

  fun writeBlockquoteOpenOrClose(token: Token) {
    requestBlankLine()

    writeToken(token)

    requestBlankLine()
  }

  fun writePreOpen(token: Token) {
    requestBlankLine()

    writeToken(token)
  }

  fun writePreClose(token: Token) {
    writeToken(token)

    requestBlankLine()
  }

  fun writeCodeOpen(token: Token) {
    writeToken(token)
  }

  fun writeCodeClose(token: Token) {
    writeToken(token)
  }

  fun writeTableOpen(token: Token) {
    requestBlankLine()

    writeToken(token)
  }

  fun writeTableClose(token: Token) {
    writeToken(token)

    requestBlankLine()
  }

  fun writeLineBreakNoAutoIndent() {
    writeNewline(NO_AUTO_INDENT)
  }

  fun writeTag(token: Token) {
    requestNewline()
    writeToken(token)
  }

  fun writeCodeLine(token: Token) {
    requestNewline()
    writeToken(token)
  }

  fun writeCodeBlockMarker(token: Token) {
    if (!inCodeBlock) {
      requestNewline()
    }
    writeToken(token)
    requestNewline()
    inCodeBlock = !inCodeBlock
  }

  fun writeLiteral(token: Token) {
    writeToken(token)
  }

  fun writeMarkdownLink(token: Token) {
    writeToken(token)
    requestWhitespace(CONDITIONAL_WHITESPACE)
  }

  override fun toString(): String {
    return output.toString()
  }

  fun requestBlankLine() {
    requestWhitespace(BLANK_LINE)
  }

  fun requestNewline() {
    requestWhitespace(NEWLINE)
  }

  private fun requestWhitespace(requestedWhitespace: RequestedWhitespace) {
    this.requestedWhitespace =
        Ordering.natural<Comparable<*>>().max(requestedWhitespace, this.requestedWhitespace)
  }

  /**
   * The kind of whitespace that has been requested between the previous and next tokens. The order
   * of the values is significant: It goes from lowest priority to highest. For example, if the
   * previous token requests [.BLANK_LINE] after it but the next token requests only [ ][.NEWLINE]
   * before it, we insert [.BLANK_LINE].
   */
  internal enum class RequestedWhitespace {
    NONE,

    /**
     * Add one space, only if the next token seems like a word In contrast, punctuation like a dot
     * does need a space before it.
     */
    CONDITIONAL_WHITESPACE,

    /** Add one space, e.g. " " */
    WHITESPACE,

    /** Break to the next line */
    NEWLINE,

    /** Add a whole blank line between the two lines of content */
    BLANK_LINE,
  }

  private fun writeToken(token: Token) {
    if (requestedWhitespace == BLANK_LINE) {
      // A blank line means all lists are terminated
      if (continuingListItemCount.isPositive) {
        continuingListCount.reset()
        continuingListItemCount.reset()
      }
    }

    if (requestedWhitespace == BLANK_LINE) {
      writeBlankLine()
      requestedWhitespace = NONE
    } else if (requestedWhitespace == NEWLINE) {
      writeNewline()
      requestedWhitespace = NONE
    }
    val needWhitespace =
        requestedWhitespace == WHITESPACE ||
            requestedWhitespace == CONDITIONAL_WHITESPACE && token.value.first().isLetterOrDigit()

    /*
     * Write a newline if necessary to respect the line limit. (But if we're at the beginning of the
     * line, a newline won't help. Or it might help but only by separating "<p>veryverylongword,"
     * which goes against our style.)
     */
    if (!atStartOfLine && token.length() + (if (needWhitespace) 1 else 0) > remainingOnLine) {
      writeNewline()
    }
    if (!atStartOfLine && needWhitespace) {
      output.append(" ")
      remainingOnLine--
    }

    output.append(token.value)

    if (!START_OF_LINE_TOKENS.contains(token.type)) {
      atStartOfLine = false
    }

    /*
     * TODO(cpovirk): We really want the number of "characters," not chars. Figure out what the
     * right way of measuring that is (grapheme count (with BreakIterator?)? sum of widths of all
     * graphemes? I don't think that our style guide is specific about this.). Moreover, I am
     * probably brushing other problems with surrogates, etc. under the table. Hopefully I mostly
     * get away with it by joining all non-space, non-tab characters together.
     *
     * Possibly the "width" question has no right answer:
     * http://denisbider.blogspot.com/2015/09/when-monospace-fonts-arent-unicode.html
     */
    remainingOnLine -= token.length()
    requestedWhitespace = NONE
  }

  private fun writeBlankLine() {
    output.append("\n")
    appendSpaces(blockIndent + 1)
    output.append("*")
    writeNewline()
  }

  private fun writeNewline(autoIndent: AutoIndent = AUTO_INDENT) {
    output.append("\n")
    appendSpaces(blockIndent + 1)
    output.append("*")
    appendSpaces(1)
    remainingOnLine = KDocFormatter.MAX_LINE_LENGTH - blockIndent - 3
    if (autoIndent == AUTO_INDENT) {
      appendSpaces(innerIndent())
      remainingOnLine -= innerIndent()
    }
    atStartOfLine = true
  }

  internal enum class AutoIndent {
    AUTO_INDENT,
    NO_AUTO_INDENT
  }

  private fun innerIndent(): Int {
    return continuingListItemCount.value() * 4 + continuingListCount.value() * 2
  }

  // If this is a hotspot, keep a String of many spaces around, and call append(string, start, end).
  private fun appendSpaces(count: Int) {
    output.append(Strings.repeat(" ", count))
  }
}
