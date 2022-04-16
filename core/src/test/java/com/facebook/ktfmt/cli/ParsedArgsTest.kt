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

package com.facebook.ktfmt.cli

import com.facebook.ktfmt.format.Formatter
import com.facebook.ktfmt.format.FormattingOptions
import com.google.common.truth.Truth.assertThat
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException
import java.io.PrintStream
import kotlin.io.path.createTempDirectory
import kotlin.test.assertFailsWith
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@Suppress("FunctionNaming")
@RunWith(JUnit4::class)
class ParsedArgsTest {

  private val root = createTempDirectory().toFile()

  @After
  fun tearDown() {
    root.deleteRecursively()
  }

  @Test
  fun `files to format are returned and unknown flags are reported`() {
    val out = ByteArrayOutputStream()

    val (fileNames, _) = ParsedArgs.parseOptions(PrintStream(out), arrayOf("foo.kt", "--unknown"))

    assertThat(fileNames).containsExactly("foo.kt")
    assertThat(out.toString()).isEqualTo("Unexpected option: --unknown\n")
  }

  @Test
  fun `files to format are returned and flags starting with @ are reported`() {
    val out = ByteArrayOutputStream()

    val (fileNames, _) = ParsedArgs.parseOptions(PrintStream(out), arrayOf("foo.kt", "@unknown"))

    assertThat(fileNames).containsExactly("foo.kt")
    assertThat(out.toString()).isEqualTo("Unexpected option: @unknown\n")
  }

  @Test
  fun `parseOptions uses default values when args are empty`() {
    val out = ByteArrayOutputStream()

    val parsed = ParsedArgs.parseOptions(PrintStream(out), arrayOf("foo.kt"))

    val formattingOptions = parsed.formattingOptions
    assertThat(formattingOptions.style).isEqualTo(FormattingOptions.Style.FACEBOOK)
    assertThat(formattingOptions.maxWidth).isEqualTo(100)
    assertThat(formattingOptions.blockIndent).isEqualTo(2)
    assertThat(formattingOptions.continuationIndent).isEqualTo(4)
    assertThat(formattingOptions.removeUnusedImports).isTrue()
    assertThat(formattingOptions.debuggingPrintOpsAfterFormatting).isFalse()

    assertThat(parsed.dryRun).isFalse()
    assertThat(parsed.setExitIfChanged).isFalse()
  }

  @Test
  fun `parseOptions recognizes --dropbox-style and rejects unknown flags`() {
    val out = ByteArrayOutputStream()

    val (fileNames, formattingOptions) =
        ParsedArgs.parseOptions(PrintStream(out), arrayOf("--dropbox-style", "foo.kt", "--unknown"))

    assertThat(fileNames).containsExactly("foo.kt")
    assertThat(formattingOptions.blockIndent).isEqualTo(4)
    assertThat(formattingOptions.continuationIndent).isEqualTo(4)
    assertThat(out.toString()).isEqualTo("Unexpected option: --unknown\n")
  }

  @Test
  fun `parseOptions recognizes --google-style`() {
    val out = ByteArrayOutputStream()

    val (_, formattingOptions) =
        ParsedArgs.parseOptions(PrintStream(out), arrayOf("--google-style", "foo.kt"))

    assertThat(formattingOptions).isEqualTo(Formatter.GOOGLE_FORMAT)
  }

  @Test
  fun `parseOptions recognizes --dry-run`() {
    val out = ByteArrayOutputStream()

    val parsed = ParsedArgs.parseOptions(PrintStream(out), arrayOf("--dry-run", "foo.kt"))

    assertThat(parsed.dryRun).isTrue()
  }

  @Test
  fun `parseOptions recognizes -n as --dry-run`() {
    val out = ByteArrayOutputStream()

    val parsed = ParsedArgs.parseOptions(PrintStream(out), arrayOf("-n", "foo.kt"))

    assertThat(parsed.dryRun).isTrue()
  }

  @Test
  fun `parseOptions recognizes --set-exit-if-changed`() {
    val out = ByteArrayOutputStream()

    val parsed =
        ParsedArgs.parseOptions(PrintStream(out), arrayOf("--set-exit-if-changed", "foo.kt"))

    assertThat(parsed.setExitIfChanged).isTrue()
  }

  @Test
  fun `processArgs use the @file option with non existing file`() {
    val out = ByteArrayOutputStream()

    val e =
        assertFailsWith<FileNotFoundException> {
          ParsedArgs.processArgs(PrintStream(out), arrayOf("@non-existing-file"))
        }
    assertThat(e.message).contains("non-existing-file (No such file or directory)")
  }

  @Test
  fun `processArgs use the @file option with file containing arguments`() {
    val out = ByteArrayOutputStream()
    val file = root.resolve("existing-file")
    file.writeText("--google-style\n--dry-run\n--set-exit-if-changed\nFile1.kt\nFile2.kt\n")

    val parsed = ParsedArgs.processArgs(PrintStream(out), arrayOf("@" + file.absolutePath))

    assertThat(parsed.formattingOptions).isEqualTo(Formatter.GOOGLE_FORMAT)
    assertThat(parsed.dryRun).isTrue()
    assertThat(parsed.setExitIfChanged).isTrue()
    assertThat(parsed.fileNames).containsExactlyElementsIn(listOf("File1.kt", "File2.kt"))
  }
}
