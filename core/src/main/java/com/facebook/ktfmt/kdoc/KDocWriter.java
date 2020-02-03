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

package com.facebook.ktfmt.kdoc;

import static com.facebook.ktfmt.kdoc.KDocWriter.AutoIndent.AUTO_INDENT;
import static com.facebook.ktfmt.kdoc.KDocWriter.AutoIndent.NO_AUTO_INDENT;
import static com.facebook.ktfmt.kdoc.KDocWriter.RequestedWhitespace.BLANK_LINE;
import static com.facebook.ktfmt.kdoc.KDocWriter.RequestedWhitespace.NEWLINE;
import static com.facebook.ktfmt.kdoc.KDocWriter.RequestedWhitespace.NONE;
import static com.facebook.ktfmt.kdoc.KDocWriter.RequestedWhitespace.WHITESPACE;
import static com.facebook.ktfmt.kdoc.Token.Type.HEADER_OPEN_TAG;
import static com.facebook.ktfmt.kdoc.Token.Type.LIST_ITEM_OPEN_TAG;
import static com.facebook.ktfmt.kdoc.Token.Type.PARAGRAPH_OPEN_TAG;
import static com.google.common.collect.Sets.immutableEnumSet;

import com.facebook.ktfmt.kdoc.Token.Type;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Ordering;

/*
 * This was copied from https://github.com/google/google-java-format
 * Modifications:
 * 1. The package name and imports were changed to com.facebook.ktfmt.kdoc to compile more easily.
 * 2. Multiple removals of no longer called methods
 * 3. Renamed to KDocWriter, and adjust write commands and logic
 */

/**
 * Stateful object that accepts "requests" and "writes," producing formatted Javadoc.
 *
 * <p>Our Javadoc formatter doesn't ever generate a parse tree, only a stream of tokens, so the
 * writer must compute and store the answer to questions like "How many levels of nested HTML list
 * are we inside?"
 */
final class KDocWriter {
  private final int blockIndent;
  private final StringBuilder output = new StringBuilder();
  /**
   * Whether we are inside an {@code <li>} element, excluding the case in which the {@code <li>}
   * contains a {@code <ul>} or {@code <ol>} that we are also inside -- unless of course we're
   * inside an {@code <li>} element in that inner list :)
   */
  private boolean continuingListItemOfInnermostList;

  private final NestingCounter continuingListItemCount = new NestingCounter();
  private final NestingCounter continuingListCount = new NestingCounter();
  private int remainingOnLine;
  private boolean atStartOfLine;
  private RequestedWhitespace requestedWhitespace = NONE;

  KDocWriter(int blockIndent) {
    this.blockIndent = blockIndent;
  }

  /**
   * Requests whitespace between the previously written token and the next written token. The
   * request may be honored, or it may be overridden by a request for "more significant" whitespace,
   * like a newline.
   */
  void requestWhitespace() {
    requestWhitespace(WHITESPACE);
  }

  void writeKDocWhitespace() {
    if (requestedWhitespace == NEWLINE) {
      requestedWhitespace = BLANK_LINE;
    } else {
      requestedWhitespace = NEWLINE;
    }
  }

  void writeBeginJavadoc() {
    /*
     * JavaCommentsHelper will make sure this is indented right. But it seems sensible enough that,
     * if our input starts with ∕✱✱, so too does our output.
     */
    output.append("/**");
  }

  void writeEndJavadoc() {
    output.append("\n");
    appendSpaces(blockIndent + 1);
    output.append("*/");
  }

  void writeListItemOpen(Token token) {
    requestNewline();

    if (continuingListItemOfInnermostList) {
      continuingListItemOfInnermostList = false;
      continuingListItemCount.decrementIfPositive();
    }
    writeToken(token);
    continuingListItemOfInnermostList = true;
    continuingListItemCount.increment();
  }

  void writeBlockquoteOpenOrClose(Token token) {
    requestBlankLine();

    writeToken(token);

    requestBlankLine();
  }

  void writePreOpen(Token token) {
    requestBlankLine();

    writeToken(token);
  }

  void writePreClose(Token token) {
    writeToken(token);

    requestBlankLine();
  }

  void writeCodeOpen(Token token) {
    writeToken(token);
  }

  void writeCodeClose(Token token) {
    writeToken(token);
  }

  void writeTableOpen(Token token) {
    requestBlankLine();

    writeToken(token);
  }

  void writeTableClose(Token token) {
    writeToken(token);

    requestBlankLine();
  }

  void writeLineBreakNoAutoIndent() {
    writeNewline(NO_AUTO_INDENT);
  }

  void writeLiteral(Token token) {
    writeToken(token);
  }

  @Override
  public String toString() {
    return output.toString();
  }

  private void requestBlankLine() {
    requestWhitespace(BLANK_LINE);
  }

  public void requestNewline() {
    requestWhitespace(NEWLINE);
  }

  private void requestWhitespace(RequestedWhitespace requestedWhitespace) {
    this.requestedWhitespace =
        Ordering.natural().max(requestedWhitespace, this.requestedWhitespace);
  }

  /**
   * The kind of whitespace that has been requested between the previous and next tokens. The order
   * of the values is significant: It goes from lowest priority to highest. For example, if the
   * previous token requests {@link #BLANK_LINE} after it but the next token requests only {@link
   * #NEWLINE} before it, we insert {@link #BLANK_LINE}.
   */
  enum RequestedWhitespace {
    NONE,
    WHITESPACE,
    NEWLINE,
    BLANK_LINE,
    ;
  }

  private void writeToken(Token token) {
    if (requestedWhitespace == BLANK_LINE) {
      // A blank line means all lists are terminated
      if (continuingListItemCount.isPositive()) {
        continuingListCount.reset();
        continuingListItemCount.reset();
      }
    }

    if (requestedWhitespace == BLANK_LINE) {
      writeBlankLine();
      requestedWhitespace = NONE;
    } else if (requestedWhitespace == NEWLINE) {
      writeNewline();
      requestedWhitespace = NONE;
    }
    boolean needWhitespace = (requestedWhitespace == WHITESPACE);

    /*
     * Write a newline if necessary to respect the line limit. (But if we're at the beginning of the
     * line, a newline won't help. Or it might help but only by separating "<p>veryverylongword,"
     * which goes against our style.)
     */
    if (!atStartOfLine && token.length() + (needWhitespace ? 1 : 0) > remainingOnLine) {
      writeNewline();
    }
    if (!atStartOfLine && needWhitespace) {
      output.append(" ");
      remainingOnLine--;
    }

    output.append(token.getValue());

    if (!START_OF_LINE_TOKENS.contains(token.getType())) {
      atStartOfLine = false;
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
    remainingOnLine -= token.length();
    requestedWhitespace = NONE;
  }

  private void writeBlankLine() {
    output.append("\n");
    appendSpaces(blockIndent + 1);
    output.append("*");
    writeNewline();
  }

  private void writeNewline() {
    writeNewline(AUTO_INDENT);
  }

  private void writeNewline(AutoIndent autoIndent) {
    output.append("\n");
    appendSpaces(blockIndent + 1);
    output.append("*");
    appendSpaces(1);
    remainingOnLine = KDocFormatter.MAX_LINE_LENGTH - blockIndent - 3;
    if (autoIndent == AUTO_INDENT) {
      appendSpaces(innerIndent());
      remainingOnLine -= innerIndent();
    }
    atStartOfLine = true;
  }

  enum AutoIndent {
    AUTO_INDENT,
    NO_AUTO_INDENT
  }

  private int innerIndent() {
    return continuingListItemCount.value() * 4 + continuingListCount.value() * 2;
  }

  // If this is a hotspot, keep a String of many spaces around, and call append(string, start, end).
  private void appendSpaces(int count) {
    output.append(Strings.repeat(" ", count));
  }

  /**
   * Tokens that are always pinned to the following token. For example, {@code <p>} in {@code <p>Foo
   * bar} (never {@code <p> Foo bar} or {@code <p>\nFoo bar}).
   *
   * <p>This is not the only kind of "pinning" that we do: See also the joining of LITERAL tokens
   * done by the lexer. The special pinning here is necessary because these tokens are not of type
   * LITERAL (because they require other special handling).
   */
  private static final ImmutableSet<Type> START_OF_LINE_TOKENS =
      immutableEnumSet(LIST_ITEM_OPEN_TAG, PARAGRAPH_OPEN_TAG, HEADER_OPEN_TAG);
}
