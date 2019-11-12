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

class KotlinInputTest {
  @Test
  fun `Comments are toks not tokens`() {
    val code = "/** foo */ class F {}"
    val input = KotlinInput(code, Parser.parse(code))
    assertThat(input.getTokens()
        .map { it.tok.text })
        .containsExactly("class", "F", "{", "}", "")
        .inOrder()
    assertThat(input.getTokens()[0].toksBefore.map { it.text })
        .containsExactly("/** foo */", " ")
        .inOrder()
  }
}