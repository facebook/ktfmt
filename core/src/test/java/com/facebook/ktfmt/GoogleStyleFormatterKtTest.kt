/*
 * Copyright (c) Facebook, Inc. and its affiliates.
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

package com.facebook.ktfmt

import com.google.common.truth.FailureMetadata
import com.google.common.truth.Subject
import com.google.common.truth.Truth.assertAbout
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@Suppress("FunctionNaming")
@RunWith(JUnit4::class)
class GoogleStyleFormatterKtTest {


  @Test
  fun `class params are placed each in their own line`() =
      assertFormatted(
          """
      |-----------------------------------------
      |class Foo(
      |    a: Int,
      |    var b: Double,
      |    val c: String
      |) {
      |    //
      |}
      |
      |class Foo(
      |    a: Int,
      |    var b: Double,
      |    val c: String
      |)
      |
      |class Foo(
      |    a: Int,
      |    var b: Int,
      |    val c: Int
      |) {
      |    //
      |}
      |
      |class Bi(
      |    a: Int,
      |    var b: Int,
      |    val c: Int
      |) {
      |    //
      |}
      |
      |class C(a: Int, var b: Int, val c: Int) {
      |    //
      |}
      |""".trimMargin(), deduceMaxWidth = true)
  
  @Test
  fun `function params are placed each in their own line`() =
      assertFormatted(
          """
      |-----------------------------------------
      |fun foo12(
      |    a: Int,
      |    var b: Double,
      |    val c: String
      |) {
      |    //
      |}
      |
      |fun foo12(
      |    a: Int,
      |    var b: Double,
      |    val c: String
      |)
      |
      |fun foo12(
      |    a: Int,
      |    var b: Double,
      |    val c: String
      |) = 5
      |
      |fun foo12(
      |    a: Int,
      |    var b: Int,
      |    val c: Int
      |) {
      |    //
      |}
      |
      |fun bi12(
      |    a: Int,
      |    var b: Int,
      |    val c: Int
      |) {
      |    //
      |}
      |
      |fun c12(a: Int, var b: Int, val c: Int) {
      |    //
      |}
      |""".trimMargin(), deduceMaxWidth = true)

  @Test
  fun `return type doesn't fit in one line`() =
      assertFormatted(
          """
      |--------------------------------------------------
      |interface X {
      |    fun f(
      |        arg1: Arg1Type,
      |        arg2: Arg2Type
      |    ): Map<String, Map<String, Double>>? {
      |        //
      |    }
      |
      |    fun functionWithGenericReturnType(
      |        arg1: Arg1Type,
      |        arg2: Arg2Type
      |    ): Map<String, Map<String, Double>>? {
      |        //
      |    }
      |}
      |""".trimMargin(),
          deduceMaxWidth = true)


  /**
   * Verifies the given code passes through formatting, and stays the same at the end
   *
   * @param code a code string that continas an optional first line made of "---" in the case
   * [deduceMaxWidth] is true. For example:
   * ```
   * --------------------
   * // exactly 20 `-` above
   * fun f()
   * ```
   * @param deduceMaxWidth if this is true the code string should start with a line of "-----" in
   * the beginning to indicate the max width to format by
   */
  private fun assertFormatted(code: String, deduceMaxWidth: Boolean = false) {
    val first = code.lines().first()
    var deducedCode = code
    var maxWidth = DEFAULT_MAX_WIDTH
    val isFirstLineAMaxWidthMarker = first.all { it == '-' }
    if (deduceMaxWidth) {
      if (!isFirstLineAMaxWidthMarker) {
        throw RuntimeException(
            "deduceMaxWidth is false, please remove the first dashes only line from the code (i.e. ---)")
      }
      deducedCode = code.substring(code.indexOf('\n') + 1)
      maxWidth = first.length
    } else {
      if (isFirstLineAMaxWidthMarker) {
        throw RuntimeException(
            "When deduceMaxWidth is true the first line need to be all dashes only (i.e. ---)")
      }
    }
    assertThatFormatting(deducedCode)
        .withOptions(FormattingOptions.googleStyle().copy(maxWidth))
        .isEqualTo(deducedCode)
  }

  private fun assertThatFormatting(code: String): FormattedCodeSubject {
    fun codes(): Subject.Factory<FormattedCodeSubject, String> {
      return Subject.Factory { metadata, subject -> FormattedCodeSubject(metadata, subject) }
    }
    return assertAbout(codes()).that(code)
  }

  class FormattedCodeSubject(metadata: FailureMetadata, private val code: String) :
      Subject(metadata, code) {
    private var options: FormattingOptions =
        FormattingOptions.googleStyle().copy(debuggingPrintOpsAfterFormatting = true)
    private var allowTrailingWhitespace = false

    fun withOptions(options: FormattingOptions): FormattedCodeSubject {
      this.options = options.copy(debuggingPrintOpsAfterFormatting = true)
      return this
    }

    fun allowTrailingWhitespace(): FormattedCodeSubject {
      this.allowTrailingWhitespace = true
      return this
    }

    fun isEqualTo(expectedFormatting: String) {
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
        actualFormatting = format(options, code)
        if (actualFormatting != expectedFormatting) {
          reportError(code)
          println("# Output: ")
          println("#".repeat(20))
          println(actualFormatting)
          println("#".repeat(20))
        }
      } catch (e: Error) {
        reportError(code)
        throw e
      }
      assertEquals(expectedFormatting, actualFormatting)
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

}