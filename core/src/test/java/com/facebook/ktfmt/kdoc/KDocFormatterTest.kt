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

package com.facebook.ktfmt.kdoc

import com.facebook.ktfmt.kdoc.KDocFormatter.tokenizeKdocText
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class KDocFormatterTest {
  @Test
  fun testTokenizeKdocText() {
    assertThat(tokenizeKdocText("     one two three  ").asIterable())
        .containsExactly(" ", "one", " ", "two", " ", "three", " ")
        .inOrder()
    assertThat(tokenizeKdocText("one two three ").asIterable())
        .containsExactly("one", " ", "two", " ", "three", " ")
        .inOrder()
    assertThat(tokenizeKdocText("one two three").asIterable())
        .containsExactly("one", " ", "two", " ", "three")
        .inOrder()
    assertThat(tokenizeKdocText("onetwothree").asIterable())
        .containsExactly("onetwothree")
        .inOrder()
    assertThat(tokenizeKdocText("").asIterable()).isEmpty()
  }
}
