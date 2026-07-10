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

import com.facebook.ktfmt.testutil.assertFormatted
import com.facebook.ktfmt.testutil.assertThatFormatting
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/**
 * Tests for the Klimat style ([Formatter.KLIMAT_FORMAT]) and the two options behind it:
 * [FormattingOptions.glueBlockLikeToOperator] and [FormattingOptions.preserveChainBreaks].
 */
@Suppress("FunctionNaming")
@RunWith(JUnit4::class)
class KlimatStyleFormatterTest {

  // region glueBlockLikeToOperator

  @Test
  fun `call with arguments and trailing lambda stays glued to the equals sign`() =
      assertFormatted(
          """
          |fun f() {
          |    val b = remember(isShake) {
          |        computeSomething()
          |    }
          |}
          |"""
              .trimMargin(),
          formattingOptions = Formatter.KLIMAT_FORMAT,
      )

  @Test
  fun `call with type arguments and trailing lambda stays glued to the equals sign`() =
      assertFormatted(
          """
          |fun f() {
          |    val c = remember<Int> {
          |        computeSomething()
          |    }
          |}
          |"""
              .trimMargin(),
          formattingOptions = Formatter.KLIMAT_FORMAT,
      )

  @Test
  fun `when initializer stays glued to the equals sign`() =
      assertFormatted(
          """
          |fun f() {
          |    val mode = when (input) {
          |        is A -> 1
          |        else -> 2
          |    }
          |}
          |"""
              .trimMargin(),
          formattingOptions = Formatter.KLIMAT_FORMAT,
      )

  @Test
  fun `braced if initializer stays glued to the equals sign`() =
      assertFormatted(
          """
          |fun f() {
          |    val fillMode = if (isEnabled) {
          |        breakMode
          |    } else {
          |        noBreakMode
          |    }
          |}
          |"""
              .trimMargin(),
          formattingOptions = Formatter.KLIMAT_FORMAT,
      )

  @Test
  fun `braceless if initializer still breaks after the equals sign when too long`() =
      assertFormatted(
          """
          |//////////////////////////////
          |fun f() {
          |    val x =
          |        if (c) aaaa else bbb
          |}
          |"""
              .trimMargin(),
          formattingOptions = Formatter.KLIMAT_FORMAT,
          deduceMaxWidth = true,
      )

  @Test
  fun `property delegate with arguments and trailing lambda stays glued to the by keyword`() =
      assertFormatted(
          """
          |fun f() {
          |    val x by remember(key) {
          |        mutableStateOf(1)
          |    }
          |}
          |"""
              .trimMargin(),
          formattingOptions = Formatter.KLIMAT_FORMAT,
      )

  @Test
  fun `function expression body stays glued to the equals sign`() =
      assertFormatted(
          """
          |fun g() = buildList(capacity) {
          |    add(1)
          |}
          |"""
              .trimMargin(),
          formattingOptions = Formatter.KLIMAT_FORMAT,
      )

  @Test
  fun `assignment statement stays glued to the equals sign`() =
      assertFormatted(
          """
          |fun f() {
          |    state.value = remember(key) {
          |        compute()
          |    }
          |}
          |"""
              .trimMargin(),
          formattingOptions = Formatter.KLIMAT_FORMAT,
      )

  @Test
  fun `named argument value stays glued to the equals sign`() =
      assertFormatted(
          """
          |fun f() {
          |    Surface(
          |        modifier = remember(key) {
          |            compute()
          |        }
          |    ) {
          |        content()
          |    }
          |}
          |"""
              .trimMargin(),
          formattingOptions = Formatter.KLIMAT_FORMAT,
      )

  @Test
  fun `glueBlockLikeToOperator disabled keeps the default layout`() =
      assertThatFormatting(
              """
              |fun f() {
              |    val b = remember(isShake) {
              |        computeSomething()
              |    }
              |}
              |"""
                  .trimMargin()
          )
          .withOptions(Formatter.KOTLINLANG_FORMAT)
          .isEqualTo(
              """
              |fun f() {
              |    val b =
              |        remember(isShake) {
              |            computeSomething()
              |        }
              |}
              |"""
                  .trimMargin()
          )

  // endregion

  // region preserveChainBreaks

  @Test
  fun `multiline chain is not joined even when it would fit`() =
      assertFormatted(
          """
          |fun f() {
          |    val m = modifier
          |        .rotate(rotation)
          |        .scale(scale)
          |}
          |"""
              .trimMargin(),
          formattingOptions = Formatter.KLIMAT_FORMAT,
      )

  @Test
  fun `single line chain stays on one line`() =
      assertFormatted(
          """
          |fun f() {
          |    val m = modifier.rotate(rotation).scale(scale)
          |}
          |"""
              .trimMargin(),
          formattingOptions = Formatter.KLIMAT_FORMAT,
      )

  @Test
  fun `attached chain prefix stays attached`() =
      assertFormatted(
          """
          |fun f() {
          |    val list = ImmutableList.newBuilder()
          |        .add(1)
          |        .build()
          |}
          |"""
              .trimMargin(),
          formattingOptions = Formatter.KLIMAT_FORMAT,
      )

  @Test
  fun `chain breaks and trailing comma are preserved inside named arguments`() =
      assertFormatted(
          """
          |fun f() {
          |    Surface(
          |        modifier = modifier
          |            .rotate(rotation)
          |            .scale(scale),
          |    ) {
          |        content()
          |    }
          |}
          |"""
              .trimMargin(),
          formattingOptions = Formatter.KLIMAT_FORMAT,
      )

  @Test
  fun `preserveChainBreaks disabled joins short multiline chains`() =
      assertThatFormatting(
              """
              |fun f() {
              |    val m = modifier
              |        .rotate(rotation)
              |        .scale(scale)
              |}
              |"""
                  .trimMargin()
          )
          .withOptions(Formatter.KOTLINLANG_FORMAT)
          .isEqualTo(
              """
              |fun f() {
              |    val m = modifier.rotate(rotation).scale(scale)
              |}
              |"""
                  .trimMargin()
          )

  // endregion
}
