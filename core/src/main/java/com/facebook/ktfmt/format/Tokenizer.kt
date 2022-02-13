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

import java.util.regex.Pattern
import org.jetbrains.kotlin.com.intellij.psi.PsiComment
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset

/**
 * Tokenizer traverses a Kotlin parse tree (which blessedly contains whitespaces and comments,
 * unlike Javac) and constructs a list of 'Tok's.
 *
 * <p>The google-java-format infra expects newline Toks to be separate from maximal-whitespace Toks,
 * but Kotlin emits them together. So, we split them using Java's \R regex matcher. We don't use
 * 'split' et al. because we want Toks for the newlines themselves.
 */
class Tokenizer(private val fileText: String, val file: KtFile) : KtTreeVisitorVoid() {

  companion object {
    private val WHITESPACE_NEWLINE_REGEX: Pattern = Pattern.compile("\\R|( )+")
  }

  val toks = mutableListOf<KotlinTok>()
  var index = 0

  override fun visitElement(element: PsiElement) {
    val startIndex = element.startOffset
    when (element) {
      is PsiComment -> {
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
      is KtStringTemplateExpression -> {
        toks.add(
            KotlinTok(
                index,
                WhitespaceTombstones.replaceTrailingWhitespaceWithTombstone(
                    fileText.substring(startIndex, element.endOffset)),
                element.text,
                startIndex,
                0,
                true,
                KtTokens.EOF))
        index++
        return
      }
      is LeafPsiElement -> {
        val elementText = element.text
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
