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

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class TokenizerTest {
  @Test
  fun `PsiWhiteSpace are split to newlines and maximal-length whitespaces`() {
    val code =
        listOf(
                "val  a = ", //
                "", //
                "     ", //
                "     15")
            .joinToString("\n")

    val file = Parser.parse(code)
    val tokenizer = Tokenizer(code, file)
    file.accept(tokenizer)

    assertThat(tokenizer.toks.map { it.originalText })
        .containsExactly("val", "  ", "a", " ", "=", " ", "\n", "\n", "     ", "\n", "     ", "15")
        .inOrder()
  }

  @Test
  fun `Strings are returns as a single token`() {
    val code =
        listOf(
                "val a=\"\"\"",
                "  ",
                "   ",
                "    Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do ",
                "    Lorem",
                "    ",
                "     ",
                "      \"\"\"",
                "val b=\"lorem ipsum\"",
                "      ",
                "    ")
            .joinToString("\n")

    val file = Parser.parse(code)
    val tokenizer = Tokenizer(code, file)
    file.accept(tokenizer)

    assertThat(tokenizer.toks.map { it.originalText })
        .containsExactly(
            "val",
            " ",
            "a",
            "=",
            listOf(
                    "\"\"\"",
                    " ${WhitespaceTombstones.SPACE_TOMBSTONE}",
                    "  ${WhitespaceTombstones.SPACE_TOMBSTONE}",
                    "    Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do${WhitespaceTombstones.SPACE_TOMBSTONE}",
                    "    Lorem",
                    "   ${WhitespaceTombstones.SPACE_TOMBSTONE}",
                    "    ${WhitespaceTombstones.SPACE_TOMBSTONE}",
                    "      \"\"\"")
                .joinToString("\n"),
            "\n",
            "val",
            " ",
            "b",
            "=",
            "\"lorem ipsum\"",
            "\n",
            "      ",
            "\n",
            "    ")
        .inOrder()
  }

  @Test
  fun `Token index is advanced after a string token`() {
    val code =
        """
      |val b="a"
      |val a=5
      |"""
            .trimMargin()
            .trimMargin()

    val file = Parser.parse(code)
    val tokenizer = Tokenizer(code, file)
    file.accept(tokenizer)

    assertThat(tokenizer.toks.map { it.originalText })
        .containsExactly("val", " ", "b", "=", "\"a\"", "\n", "val", " ", "a", "=", "5")
        .inOrder()
    assertThat(tokenizer.toks.map { it.index })
        .containsExactly(0, -1, 1, 2, 3, -1, 4, -1, 5, 6, 7)
        .inOrder()
  }

  @Test
  fun `Context receivers are parsed correctly`() {
    val code =
        """
      |context(Something)
      |class A {
      |  context(
      |  // Test comment.
      |  Logger, Raise<Error>)
      |  fun test() {}
      |}
      |"""
            .trimMargin()
            .trimMargin()

    val file = Parser.parse(code)
    val tokenizer = Tokenizer(code, file)
    file.accept(tokenizer)

    assertThat(tokenizer.toks.map { it.originalText })
        .containsExactly(
            "context",
            "(",
            "Something",
            ")",
            "\n",
            "class",
            " ",
            "A",
            " ",
            "{",
            "\n",
            "  ",
            "context",
            "(",
            "\n",
            "  ",
            "// Test comment.",
            "\n",
            "  ",
            "Logger",
            ",",
            " ",
            "Raise",
            "<",
            "Error",
            ">",
            ")",
            "\n",
            "  ",
            "fun",
            " ",
            "test",
            "(",
            ")",
            " ",
            "{",
            "}",
            "\n",
            "}")
        .inOrder()
    assertThat(tokenizer.toks.map { it.index })
        .containsExactly(
            0,
            1,
            2,
            3,
            -1,
            4,
            -1,
            5,
            -1,
            6,
            -1,
            -1,
            7,
            8,
            -1,
            -1,
            9,
            -1,
            -1,
            10,
            11,
            -1,
            12,
            13,
            14,
            15,
            16,
            -1,
            -1,
            17,
            -1,
            18,
            19,
            20,
            -1,
            21,
            22,
            -1,
            23)
        .inOrder()
  }
}
