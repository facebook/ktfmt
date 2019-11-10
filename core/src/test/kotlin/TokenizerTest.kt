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
