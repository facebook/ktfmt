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

package com.facebook.ktfmt.testutil

import com.facebook.ktfmt.debughelpers.PrintAstVisitor
import com.facebook.ktfmt.format.Formatter
import com.facebook.ktfmt.format.FormattingOptions
import com.facebook.ktfmt.format.Parser
import com.google.common.truth.FailureMetadata
import com.google.common.truth.Subject
import com.google.common.truth.Truth
import org.intellij.lang.annotations.Language
import org.junit.Assert

var defaultTestFormattingOptions: FormattingOptions = Formatter.META_FORMAT

/**
 * Verifies the given code passes through formatting, and stays the same at the end
 *
 * @param code a code string that contains an optional first line made of at least 8 '-' or '/' in
 *   the case [deduceMaxWidth] is true. For example:
 * ```
 * ////////////////////////
 * // exactly 24 `/` above
 * // and that will be the
 * // size of the line
 * fun f()
 * ```
 *
 * @param deduceMaxWidth if this is true the code string should start with a line of "-----" in the
 *   beginning to indicate the max width to format by
 */
fun assertFormatted(
    @Language("kts") code: String,
    formattingOptions: FormattingOptions = defaultTestFormattingOptions,
    deduceMaxWidth: Boolean = false,
) {
  val first = code.lines().first()
  var deducedCode = code
  var maxWidth = FormattingOptions.DEFAULT_MAX_WIDTH
  val lineWidthMarkers = setOf('-', '/')
  val isFirstLineAMaxWidthMarker = first.length >= 8 && first.all { it in lineWidthMarkers }
  if (deduceMaxWidth) {
    if (!isFirstLineAMaxWidthMarker) {
      throw RuntimeException(
          "When deduceMaxWidth is true the first line needs to be all dashes only (i.e. ---)")
    }
    deducedCode = code.substring(code.indexOf('\n') + 1)
    maxWidth = first.length
  } else {
    if (isFirstLineAMaxWidthMarker) {
      throw RuntimeException(
          "deduceMaxWidth is false, please remove the first dashes only line from the code (i.e. ---)")
    }
  }
  assertThatFormatting(deducedCode)
      .withOptions(formattingOptions.copy(maxWidth = maxWidth))
      .isEqualTo(deducedCode)
}

fun assertThatFormatting(@Language("kts") code: String): FormattedCodeSubject {
  fun codes(): Subject.Factory<FormattedCodeSubject, String> {
    return Subject.Factory { metadata, subject ->
      FormattedCodeSubject(metadata, checkNotNull(subject))
    }
  }
  return Truth.assertAbout(codes()).that(code)
}

@Suppress("ClassNameDoesNotMatchFileName")
class FormattedCodeSubject(metadata: FailureMetadata, private val code: String) :
    Subject(metadata, code) {
  private var options: FormattingOptions = defaultTestFormattingOptions
  private var allowTrailingWhitespace = false

  fun withOptions(options: FormattingOptions): FormattedCodeSubject {
    this.options = options
    return this
  }

  fun allowTrailingWhitespace(): FormattedCodeSubject {
    this.allowTrailingWhitespace = true
    return this
  }

  fun isEqualTo(@Language("kts") expectedFormatting: String) {
    if (!allowTrailingWhitespace && expectedFormatting.lines().any { it.endsWith(" ") }) {
      throw RuntimeException(
          "Expected code contains trailing whitespace, which the formatter usually doesn't output:\n" +
              expectedFormatting
                  .lines()
                  .map { if (it.endsWith(" ")) "[$it]" else it }
                  .joinToString("\n"))
    }
    val actualFormatting: String
    try {
      actualFormatting = Formatter.format(options, code)
      if (actualFormatting != expectedFormatting) {
        reportError(code)
        println("# Output: ")
        println("#".repeat(20))
        println(actualFormatting)
        println("# Expected: ")
        println("#".repeat(20))
        println(expectedFormatting)
        println("#".repeat(20))
        println(
            "Need more information about the break operations? " +
                "Run test with assertion with \"FormattingOptions(debuggingPrintOpsAfterFormatting = true)\"")
      }
    } catch (e: Error) {
      reportError(code)
      throw e
    }
    Assert.assertEquals(expectedFormatting, actualFormatting)
  }

  private fun reportError(code: String) {
    val file = Parser.parse(code)
    println("# Parse tree of input: ")
    println("#".repeat(20))
    file.accept(PrintAstVisitor())
    println()
    println("# Input: ")
    println("#".repeat(20))
    println(code)
    println()
  }
}
