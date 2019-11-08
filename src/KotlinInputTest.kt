package com.google.googlejavaformat.kotlin

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