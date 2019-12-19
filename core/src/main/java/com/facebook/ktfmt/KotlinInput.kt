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

import com.google.common.base.MoreObjects
import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import com.google.common.collect.ImmutableRangeMap
import com.google.common.collect.Range
import com.google.googlejavaformat.Input
import com.google.googlejavaformat.Newlines
import com.google.googlejavaformat.java.JavaOutput
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.impl.source.tree.LeafPsiElement
import java.util.regex.Pattern
import org.jetbrains.kotlin.lexer.KtToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset

// TODO: share the code with JavaInput instead of copy-pasting here.
/**
 * KotlinInput is for Kotlin what JavaInput is for Java.
 *
 * <p>KotlinInput is duplicating most of JavaInput's code, but uses the Kotlin compiler as a lexer instead of Javac.
 * This is required because some valid Kotlin programs are not valid Java programs, e.g., "a..b".
 *
 * <p>See javadoc for JavaInput.
 */
class KotlinInput(private val text: String, file: KtFile) : Input() {
  private val tokens: ImmutableList<Token> // The Tokens for this input.
  private val positionToColumnMap: ImmutableMap<Int, Int> // Map Tok position to column.
  private val positionTokenMap: ImmutableRangeMap<Int, Token> // Map position to Token.
  private var kN = 0 // The number of numbered toks (tokens or comments), excluding the EOF.
  private val kToToken: Array<Token?>

  init {
    setLines(ImmutableList.copyOf(Newlines.lineIterator(text)))
    val toks = buildToks(file, text)
    positionToColumnMap = makePositionToColumnMap(toks)
    tokens = buildTokens(toks)
    val tokenLocations = ImmutableRangeMap.builder<Int, Token>()
    for (token in tokens) {
      val end = JavaOutput.endTok(token)
      var upper = end.position
      if (end.text.isNotEmpty()) {
        upper += end.length() - 1
      }
      tokenLocations.put(Range.closed(JavaOutput.startTok(token).position, upper), token)
    }
    positionTokenMap = tokenLocations.build()

    // adjust kN for EOF
    kToToken = arrayOfNulls(kN + 1)
    for (token in tokens) {
      for (tok in token.toksBefore) {
        if (tok.index < 0) {
          continue
        }
        kToToken[tok.index] = token
      }
      kToToken[token.tok.index] = token
      for (tok in token.toksAfter) {
        if (tok.index < 0) {
          continue
        }
        kToToken[tok.index] = token
      }
    }
  }

  private fun makePositionToColumnMap(toks: List<KotlinTok>): ImmutableMap<Int, Int> {
    val builder = ImmutableMap.builder<Int, Int>()
    for (tok in toks) {
      builder.put(tok.position, tok.column)
    }
    return builder.build()
  }

  private fun buildToks(file: KtFile, fileText: String): ImmutableList<KotlinTok> {
    val tokenizer = Tokenizer(fileText, file)
    file.accept(tokenizer)
    val toks = tokenizer.toks
    toks.add(KotlinTok(tokenizer.index, "", "", fileText.length, 0, true, KtTokens.EOF))
    kN = tokenizer.index
    computeRanges(toks)
    return ImmutableList.copyOf(toks)
  }

  private fun buildTokens(toks: List<KotlinTok>): ImmutableList<Token> {
    val tokens = ImmutableList.builder<Token>()
    var k = 0
    val kN = toks.size

    // Remaining non-tokens before the token go here.
    var toksBefore: ImmutableList.Builder<KotlinTok> = ImmutableList.builder()

    OUTERMOST@ while (k < kN) {
      while (!toks[k].isToken) {
        val tok = toks[k++]
        toksBefore.add(tok)
        if (isParamComment(tok)) {
          while (toks[k].isNewline) {
            // drop newlines after parameter comments
            k++
          }
        }
      }
      val tok = toks[k++]

      // Non-tokens starting on the same line go here too.
      val toksAfter = ImmutableList.builder<KotlinTok>()
      OUTER@ while (k < kN && !toks[k].isToken) {
        // Don't attach inline comments to certain leading tokens, e.g. for `f(/*flag1=*/true).
        //
        // Attaching inline comments to the right token is hard, and this barely
        // scratches the surface. But it's enough to do a better job with parameter
        // name comments.
        //
        // TODO(cushon): find a better strategy.
        if (toks[k].isSlashStarComment && (tok.text == "(" || tok.text == "<" || tok.text == ".")) break@OUTER
        if (toks[k].isJavadocComment && tok.text == ";") break@OUTER
        if (isParamComment(toks[k])) {
          tokens.add(KotlinToken(toksBefore.build(), tok, toksAfter.build()))
          toksBefore = ImmutableList.builder<KotlinTok>().add(toks[k++])
          // drop newlines after parameter comments
          while (toks[k].isNewline) {
            k++
          }
          continue@OUTERMOST
        }
        val nonTokenAfter = toks[k++]
        toksAfter.add(nonTokenAfter)
        if (Newlines.containsBreaks(nonTokenAfter.text)) {
          break
        }
      }
      tokens.add(KotlinToken(toksBefore.build(), tok, toksAfter.build()))
      toksBefore = ImmutableList.builder()
    }
    return tokens.build()
  }

  private fun isParamComment(tok: Tok): Boolean {
    return tok.isSlashStarComment && tok.text.matches("/\\*[A-Za-z0-9\\s_\\-]+=\\s*\\*/".toRegex())
  }

  override fun getkN(): Int = kN

  override fun getToken(k: Int): Token? = kToToken[k]

  override fun getTokens(): ImmutableList<out Token> = tokens

  override fun getPositionTokenMap(): ImmutableRangeMap<Int, out Token> = positionTokenMap

  override fun getPositionToColumnMap(): ImmutableMap<Int, Int> = positionToColumnMap

  override fun getText(): String = text

  override fun getLineNumber(inputPosition: Int) =
      StringUtil.offsetToLineColumn(text, inputPosition).line + 1

  override fun getColumnNumber(inputPosition: Int) =
      StringUtil.offsetToLineColumn(text, inputPosition).column
}

class KotlinTok(
    private val index: Int,
    private val originalText: String,
    private val text: String,
    private val position: Int,
    private val columnI: Int,
    val isToken: Boolean,
    private val kind: KtToken
) : Input.Tok {

  override fun getIndex(): Int = index

  override fun getText(): String = text

  override fun getOriginalText(): String = originalText

  override fun length(): Int = originalText.length

  override fun getPosition(): Int = position

  override fun getColumn(): Int = columnI

  override fun isNewline(): Boolean = Newlines.isNewline(text)

  override fun isSlashSlashComment(): Boolean = text.startsWith("//")

  override fun isSlashStarComment(): Boolean = text.startsWith("/*")

  override fun isJavadocComment(): Boolean = text.startsWith("/**") && text.length > 4

  override fun isComment(): Boolean = isSlashSlashComment || isSlashStarComment

  fun kind(): KtToken = kind

  override fun toString(): String {
    return MoreObjects.toStringHelper(this)
        .add("index", index)
        .add("text", text)
        .add("position", position)
        .add("columnI", columnI)
        .add("isToken", isToken)
        .toString()
  }
}

class KotlinToken(
    private val toksBefore: ImmutableList<KotlinTok>,
    private val kotlinTok: KotlinTok,
    private val toksAfter: ImmutableList<KotlinTok>
) : Input.Token {

  override fun getTok(): KotlinTok = kotlinTok

  override fun getToksBefore(): ImmutableList<out Input.Tok> = toksBefore

  override fun getToksAfter(): ImmutableList<out Input.Tok> = toksAfter

  override fun toString(): String {
    return MoreObjects.toStringHelper(this)
        .add("tok", kotlinTok)
        .add("toksBefore", toksBefore)
        .add("toksAfter", toksAfter)
        .toString()
  }
}

internal val WHITESPACE_NEWLINE_REGEX: Pattern = Pattern.compile("\\R|( )+")

/**
 * Tokenizer traverses a Kotlin parse tree (which blessedly contains whitespaces and comments, unlike Javac) and
 * constructs a list of 'Tok's.
 *
 * <p>The google-java-format infra expects newline Toks to be separate from maximal-whitespace Toks, but Kotlin emits
 * them together. So, we split them using Java's \R regex matcher. We don't use 'split' et al. because we want Toks for
 * the newlines themselves.
 */
class Tokenizer(private val fileText: String, val file: KtFile) : KtTreeVisitorVoid() {
  val toks = mutableListOf<KotlinTok>()
  var index = 0

  override fun visitElement(element: PsiElement) {
    when (element) {
      is PsiComment -> {
        val startIndex = element.startOffset
        toks.add(
            KotlinTok(
                index,
                fileText.substring(startIndex, element.endOffset),
                element.text,
                startIndex,
                0,
                false,
                KtTokens.EOF))
        index++
        return
      }
      is LeafPsiElement -> {
        val elementText = element.text
        val startIndex = element.startOffset
        val endIndex = element.endOffset
        if (element is PsiWhiteSpace) {
          val matcher = WHITESPACE_NEWLINE_REGEX.matcher(elementText)
          while (matcher.find()) {
            val text = matcher.group()
            toks.add(
                KotlinTok(
                    -1,
                    fileText.substring(startIndex + matcher.start(), startIndex + matcher.end()),
                    text,
                    startIndex + matcher.start(),
                    0,
                    false,
                    KtTokens.EOF))
          }
        } else {
          toks.add(
              KotlinTok(
                  index,
                  fileText.substring(startIndex, endIndex),
                  elementText,
                  startIndex,
                  0,
                  true,
                  KtTokens.EOF))
          index++
        }
      }
    }
    super.visitElement(element)
  }
}
