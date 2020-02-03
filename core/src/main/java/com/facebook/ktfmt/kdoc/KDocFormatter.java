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
 * This was copied from https://github.com/google/google-java-format
 * Modifications:
 * 1. The package name and imports were changed to com.facebook.ktfmt.kdoc to compile more easily.
 * 2. The file and class were renamed to KDocFormatter.
 */

package com.facebook.ktfmt.kdoc;

import static com.facebook.ktfmt.kdoc.Token.Type.BEGIN_KDOC;
import static com.facebook.ktfmt.kdoc.Token.Type.BLANK_LINE;
import static com.facebook.ktfmt.kdoc.Token.Type.END_KDOC;
import static com.facebook.ktfmt.kdoc.Token.Type.LIST_ITEM_OPEN_TAG;
import static com.facebook.ktfmt.kdoc.Token.Type.LITERAL;
import static com.facebook.ktfmt.kdoc.Token.Type.WHITESPACE;
import static java.util.regex.Pattern.compile;
import static org.jetbrains.kotlin.lexer.KtTokens.WHITE_SPACE;

import com.google.common.collect.ImmutableList;
import com.intellij.psi.tree.IElementType;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jetbrains.kotlin.kdoc.lexer.KDocLexer;
import org.jetbrains.kotlin.kdoc.lexer.KDocTokens;

/**
 * Entry point for formatting KDoc.
 *
 * <p>This stateless class reads tokens from the stateful lexer and translates them to "requests"
 * and "writes" to the stateful writer. It also munges tokens into "standardized" forms. Finally, it
 * performs postprocessing to convert the written KDoc to a one-liner if possible or to leave a
 * single blank line if it's empty.
 */
public final class KDocFormatter {

  static final int MAX_LINE_LENGTH = 100;

  /**
   * Formats the given Javadoc comment, which must start with ∕✱✱ and end with ✱∕. The output will
   * start and end with the same characters.
   */
  public static String formatKDoc(String input, int blockIndent) {
    KDocLexer kDocLexer = new KDocLexer();
    kDocLexer.start(input);
    ImmutableList.Builder<Token> newTokensBuilder = new ImmutableList.Builder<>();
    while (kDocLexer.getTokenType() != null) {
      IElementType tokenType = kDocLexer.getTokenType();
      String tokenText = kDocLexer.getTokenText();
      if (tokenType == KDocTokens.START) {
        newTokensBuilder.add(new Token(BEGIN_KDOC, tokenText));
      } else if (tokenType == KDocTokens.LEADING_ASTERISK) {
        // ignore
      } else if (tokenType == KDocTokens.END) {
        newTokensBuilder.add(new Token(END_KDOC, tokenText));
      } else if (tokenType == KDocTokens.TEXT) {
        String[] words = tokenText.trim().split(" +");
        boolean first = true;
        for (String word : words) {
          if (first) {
            if (word.equals("-")) {
              newTokensBuilder.add(new Token(LIST_ITEM_OPEN_TAG, ""));
            }
            first = false;
          }
          newTokensBuilder.add(new Token(LITERAL, word));
          newTokensBuilder.add(new Token(WHITESPACE, " "));
        }
      } else if (tokenType == KDocTokens.TAG_NAME) {
        newTokensBuilder.add(new Token(LITERAL, tokenText));
      } else if (tokenType == KDocTokens.CODE_BLOCK_TEXT) {
        newTokensBuilder.add(new Token(LITERAL, tokenText));
      } else if (tokenType == KDocTokens.MARKDOWN_INLINE_LINK) {
        newTokensBuilder.add(new Token(LITERAL, tokenText));
      } else if (tokenType == KDocTokens.MARKDOWN_LINK) {
        newTokensBuilder.add(new Token(LITERAL, tokenText));
      } else if (tokenType == WHITE_SPACE) {
        newTokensBuilder.add(new Token(BLANK_LINE, ""));
      } else {
        throw new RuntimeException("Unexpected: " + tokenType);
      }

      kDocLexer.advance();
    }
    String result = render(newTokensBuilder.build(), blockIndent);
    return makeSingleLineIfPossible(blockIndent, result);
  }

  private static String render(List<Token> input, int blockIndent) {
    KDocWriter output = new KDocWriter(blockIndent);
    for (Token token : input) {
      switch (token.getType()) {
        case BEGIN_KDOC:
          output.writeBeginJavadoc();
          break;
        case END_KDOC:
          output.writeEndJavadoc();
          return output.toString();
        case LIST_ITEM_OPEN_TAG:
          output.writeListItemOpen(token);
          break;
        case PRE_OPEN_TAG:
          output.writePreOpen(token);
          break;
        case PRE_CLOSE_TAG:
          output.writePreClose(token);
          break;
        case CODE_OPEN_TAG:
          output.writeCodeOpen(token);
          break;
        case CODE_CLOSE_TAG:
          output.writeCodeClose(token);
          break;
        case TABLE_OPEN_TAG:
          output.writeTableOpen(token);
          break;
        case TABLE_CLOSE_TAG:
          output.writeTableClose(token);
          break;
        case BLANK_LINE:
          output.writeKDocWhitespace();
          break;
        case WHITESPACE:
          output.requestWhitespace();
          break;
        case LITERAL:
          output.writeLiteral(token);
          break;
        default:
          throw new AssertionError(token.getType());
      }
    }
    throw new AssertionError();
  }

  private static final Pattern ONE_CONTENT_LINE_PATTERN = compile(" */[*][*]\n *[*] (.*)\n *[*]/");

  /**
   * Returns the given string or a one-line version of it (e.g., "∕✱✱ Tests for foos. ✱∕") if it
   * fits on one line.
   */
  private static String makeSingleLineIfPossible(int blockIndent, String input) {
    int oneLinerContentLength = MAX_LINE_LENGTH - "/**  */".length() - blockIndent;
    Matcher matcher = ONE_CONTENT_LINE_PATTERN.matcher(input);
    if (matcher.matches() && matcher.group(1).isEmpty()) {
      return "/** */";
    } else if (matcher.matches() && matcher.group(1).length() <= oneLinerContentLength) {
      return "/** " + matcher.group(1) + " */";
    }
    return input;
  }

  private KDocFormatter() {}
}
