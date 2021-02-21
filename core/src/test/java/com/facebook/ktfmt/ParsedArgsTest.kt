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

import com.google.common.truth.Truth.assertThat
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

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

    val (fileNames, formattingOptions) =
        parseOptions(PrintStream(out), arrayOf("--dropbox-style", "foo.kt", "--unknown"))

    assertThat(fileNames).containsExactly("foo.kt")
    assertThat(formattingOptions.blockIndent).isEqualTo(4)
    assertThat(formattingOptions.continuationIndent).isEqualTo(4)
    assertThat(out.toString()).isEqualTo("Unexpected option: --unknown\n")
  }

  @Test
  fun `parseOptions recognizes --google-style`() {
    val out = ByteArrayOutputStream()

    val (_, formattingOptions) = parseOptions(PrintStream(out), arrayOf("--google-style", "foo.kt"))

    assertThat(formattingOptions).isEqualTo(GOOGLE_FORMAT)
  }
}
