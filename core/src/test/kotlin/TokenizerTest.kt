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

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class TokenizerTest {
  @Test
  fun `PsiWhiteSpace are split to newlines and maximal-length whitespaces`() {
    val code = """
            |val  a = 
            |
            |     
            |     15
        """.trimMargin()
    val file = Parser.parse(code)
    println("# Parse tree of input: ")
    println("#".repeat(20))
    val tokenizer = Tokenizer(code, file)
    file.accept(tokenizer)

    assertThat(tokenizer.toks.map { it.text })
        .containsExactly("val", "  ", "a", " ", "=", " ", "\n", "\n", "     ", "\n", "     ", "15")
        .inOrder()
  }
}
