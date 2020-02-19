// (c) Facebook, Inc. and its affiliates. Confidential and proprietary.

package com.facebook.ktfmt

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.ByteArrayOutputStream
import java.io.PrintStream

@Suppress("FunctionNaming")
@RunWith(JUnit4::class)
class ParsedArgsTest {

  @Test
  fun `files to format are returned and unknown flags are reported`() {
    val out = ByteArrayOutputStream()

    val (fileNames, _) = parseOptions(PrintStream(out), arrayOf("foo.kt", "--unknown"))

    assertThat(fileNames).containsExactly("foo.kt")
    assertThat(out.toString()).isEqualTo("Unexpected option: --unknown\n")
  }

  @Test
  fun `parseOptions returns default indent sizes when --dropbox-style is not present`() {
    val out = ByteArrayOutputStream()

    val (_, formattingOptions) = parseOptions(PrintStream(out), arrayOf("foo.kt"))

    assertThat(formattingOptions.blockIndent).isEqualTo(2)
    assertThat(formattingOptions.continuationIndent).isEqualTo(4)
  }

  @Test
  fun `parseOptions recognizes --dropbox-style and rejects unknown flags`() {
    val out = ByteArrayOutputStream()

    val (fileNames, formattingOptions) = parseOptions(
        PrintStream(out), arrayOf("--dropbox-style", "foo.kt", "--unknown"))

    assertThat(fileNames).containsExactly("foo.kt")
    assertThat(formattingOptions.blockIndent).isEqualTo(4)
    assertThat(formattingOptions.continuationIndent).isEqualTo(4)
    assertThat(out.toString()).isEqualTo("Unexpected option: --unknown\n")
  }
}
