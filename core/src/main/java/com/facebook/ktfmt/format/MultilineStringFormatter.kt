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

import org.jetbrains.kotlin.psi.KtQualifiedExpression
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.psi.psiUtil.startOffset

/**
 * Adds and removes elements that are not strictly needed in the code, such as semicolons and unused
 * imports.
 */
class MultilineStringFormatter(val continuationIndentSize: Int) {
  class Candidate(
      val isMargin: Boolean,
      /* The start offset of the trim method call, right before `.trimX` */
      val trimMethodCallOffset: Int,
      /* The start offset of the string template, starting with `"""` or `$$"""` */
      val stringOffset: Int,
  ) {
    val indentationSuffix: String = if (isMargin) "|" else ""
  }

  companion object {
    private const val TQ = "\"\"\""
  }

  private val String.indentLevel: Int
    get() = length - trimStart().length

  fun format(code: String): String {
    val candidates = getCandidates(code)
    val result = StringBuilder(code)

    val openTemplateExpressionRegex =
        Regex("""\$\$?\{(((?<!\\)"([^"]|(\\"))*?[^\\]")|([^\\]'\\?.')|[^"'}])*$""")

    for (candidate in candidates.sortedByDescending(Candidate::stringOffset)) {
      val (indentCount, lines) =
          result.substring(candidate.stringOffset, candidate.trimMethodCallOffset).lines().let {
            result.substring(0, candidate.stringOffset).lines().last().length to it.dropLast(1)
          }
      if (lines.size < 2) {
        // Single line multiline strings are left alone
        continue
      }
      if (candidate.isMargin && lines.any { openTemplateExpressionRegex.find(it) != null }) {
        // Do not mess with multiline template expressions, as those can be a mess
        // Why?
        // 1. They span multiple lines
        // 2. They can be nested recursively
        // 3. We need to be careful as the closing character ('}') could be inside a string/char
        continue
      }
      val indentation = " ".repeat(indentCount)
      val continuationIndentation = " ".repeat(continuationIndentSize)
      val minIndentForTrimIndent: Int =
          lines.subList(1, lines.size).minOf { if (it.isEmpty()) Int.MAX_VALUE else it.indentLevel }

      val multiline = StringBuilder()
      lines.forEachIndexed { i, line ->
        if (i == 0) {
          val (before, after) = line.split(TQ, limit = 2)
          if (after.isNotEmpty()) {
            multiline.append(before)
            multiline.appendLine(TQ)
            multiline.append(indentation)
            val lineContents =
                if (candidate.isMargin && after.trimStart().firstOrNull() == '|') {
                  after.substringAfter("|")
                } else {
                  after
                }
            multiline.append(candidate.indentationSuffix)
            multiline.appendLine(lineContents)
          } else {
            multiline.appendLine(line)
          }
        } else {
          val lineContents =
              if (candidate.isMargin) {
                if (i == lines.lastIndex && "|" !in line && line.substringBefore(TQ).isBlank()) {
                  // trimMargin has a special handling of the final line, where it ignores it if
                  // it's blank

                  // Drop last new line character
                  multiline.deleteAt(multiline.lastIndex)

                  multiline.appendLine(line.substring(line.indexOf(TQ)))
                  return@forEachIndexed
                }

                if (line.trimStart().firstOrNull() == '|') {
                  line.substringAfter("|")
                } else {
                  line
                }
              } else {
                line.drop(minIndentForTrimIndent)
              }
          if (candidate.isMargin || line.isNotEmpty()) {
            multiline.append(indentation)
          }
          multiline.append(candidate.indentationSuffix)
          multiline.appendLine(lineContents)
        }
      }
      multiline.append(indentation)
      multiline.append(continuationIndentation)
      result.replace(candidate.stringOffset, candidate.trimMethodCallOffset, multiline.toString())
    }

    return result.toString()
  }

  private fun getCandidates(code: String): List<Candidate> {
    val file = Parser.parse(code)
    val candidates = mutableListOf<Candidate>()
    file.accept(
        object : KtTreeVisitorVoid() {
          override fun visitQualifiedExpression(expression: KtQualifiedExpression) {
            val receiver = expression.receiverExpression
            if (receiver !is KtStringTemplateExpression) return
            val selectorExpression = expression.selectorExpression?.text.orEmpty().trim()
            val isTrimMargin = selectorExpression.startsWith("trimMargin(")
            val isTrimIndent = selectorExpression.startsWith("trimIndent(")
            if (isTrimIndent || isTrimMargin) {
              // -1 here to account for the space after the dot
              val trimOffset = checkNotNull(expression.selectorExpression).startOffset - 1
              val stringOffset = receiver.startOffset
              candidates.add(Candidate(isTrimMargin, trimOffset, stringOffset))
            }
          }
        })
    return candidates
  }
}
