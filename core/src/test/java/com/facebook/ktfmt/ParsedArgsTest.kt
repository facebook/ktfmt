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

    val parsed = parseOptions(PrintStream(out), arrayOf("foo.kt", "--unknown"))

    assertThat(parsed.fileNames).containsExactly("foo.kt")
    assertThat(out.toString()).isEqualTo("Unexpected option: --unknown\n")
  }
}
