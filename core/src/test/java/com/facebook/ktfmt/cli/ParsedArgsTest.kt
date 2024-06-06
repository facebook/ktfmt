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
import java.io.FileNotFoundException
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
  fun `unknown flags return an error`() {
    val result = ParsedArgs.parseOptions(arrayOf("--unknown"))
    assertThat(result).isInstanceOf(ParseResult.Error::class.java)
  }

  @Test
  fun `unknown flags starting with '@' return an error`() {
    val result = ParsedArgs.parseOptions(arrayOf("@unknown"))
    assertThat(result).isInstanceOf(ParseResult.Error::class.java)
  }

  @Test
  fun `parseOptions uses default values when args are empty`() {
    val parsed = assertSucceeds(ParsedArgs.parseOptions(arrayOf("foo.kt")))

    val formattingOptions = parsed.formattingOptions

    val defaultFormattingOptions = FormattingOptions()
    assertThat(formattingOptions).isEqualTo(defaultFormattingOptions)
  }

  @Test
  fun `parseOptions recognizes --dropbox-style`() {
    val parsed = assertSucceeds(ParsedArgs.parseOptions(arrayOf("--dropbox-style", "foo.kt")))
    assertThat(parsed.formattingOptions).isEqualTo(Formatter.DROPBOX_FORMAT)
  }

  @Test
  fun `parseOptions recognizes --google-style`() {
    val parsed = assertSucceeds(ParsedArgs.parseOptions(arrayOf("--google-style", "foo.kt")))
    assertThat(parsed.formattingOptions).isEqualTo(Formatter.GOOGLE_FORMAT)
  }

  @Test
  fun `parseOptions recognizes --dry-run`() {
    val parsed = assertSucceeds(ParsedArgs.parseOptions(arrayOf("--dry-run", "foo.kt")))
    assertThat(parsed.dryRun).isTrue()
  }

  @Test
  fun `parseOptions recognizes -n as --dry-run`() {
    val parsed = assertSucceeds(ParsedArgs.parseOptions(arrayOf("-n", "foo.kt")))
    assertThat(parsed.dryRun).isTrue()
  }

  @Test
  fun `parseOptions recognizes --set-exit-if-changed`() {
    val parsed = assertSucceeds(ParsedArgs.parseOptions(arrayOf("--set-exit-if-changed", "foo.kt")))
    assertThat(parsed.setExitIfChanged).isTrue()
  }

  @Test
  fun `parseOptions defaults to removing imports`() {
    val parsed = assertSucceeds(ParsedArgs.parseOptions(arrayOf("foo.kt")))
    assertThat(parsed.formattingOptions.removeUnusedImports).isTrue()
  }

  @Test
  fun `parseOptions recognizes --do-not-remove-unused-imports to removing imports`() {
    val parsed =
        assertSucceeds(ParsedArgs.parseOptions(arrayOf("--do-not-remove-unused-imports", "foo.kt")))
    assertThat(parsed.formattingOptions.removeUnusedImports).isFalse()
  }

  @Test
  fun `parseOptions recognizes --stdin-name`() {
    val parsed = assertSucceeds(ParsedArgs.parseOptions(arrayOf("--stdin-name=my/foo.kt")))
    assertThat(parsed.stdinName).isEqualTo("my/foo.kt")
  }

  @Test
  fun `parseOptions accepts --stdin-name with empty value`() {
    val parsed = assertSucceeds(ParsedArgs.parseOptions(arrayOf("--stdin-name=")))
    assertThat(parsed.stdinName).isEqualTo("")
  }

  @Test
  fun `parseOptions --stdin-name without value`() {
    val parseResult = ParsedArgs.parseOptions(arrayOf("--stdin-name"))
    assertThat(parseResult).isInstanceOf(ParseResult.Error::class.java)
  }

  @Test
  fun `processArgs use the @file option with non existing file`() {
    val e =
        assertFailsWith<FileNotFoundException> {
          ParsedArgs.processArgs(arrayOf("@non-existing-file"))
        }
    assertThat(e.message).contains("non-existing-file (No such file or directory)")
  }

  @Test
  fun `processArgs use the @file option with file containing arguments`() {
    val file = root.resolve("existing-file")
    file.writeText("--google-style\n--dry-run\n--set-exit-if-changed\nFile1.kt\nFile2.kt\n")

    val result = ParsedArgs.processArgs(arrayOf("@" + file.absolutePath))
    assertThat(result).isInstanceOf(ParseResult.Ok::class.java)

    val parsed = (result as ParseResult.Ok).parsedValue

    assertThat(parsed.formattingOptions).isEqualTo(Formatter.GOOGLE_FORMAT)
    assertThat(parsed.dryRun).isTrue()
    assertThat(parsed.setExitIfChanged).isTrue()
    assertThat(parsed.fileNames).containsExactlyElementsIn(listOf("File1.kt", "File2.kt"))
  }

  @Test
  fun `parses multiple args successfully`() {
    val testResult =
        ParsedArgs.parseOptions(
            arrayOf("--google-style", "--dry-run", "--set-exit-if-changed", "File.kt"),
        )
    assertThat(testResult)
        .isEqualTo(
            parseResultOk(
                fileNames = listOf("File.kt"),
                formattingOptions = Formatter.GOOGLE_FORMAT,
                dryRun = true,
                setExitIfChanged = true,
            ))
  }

  @Test
  fun `last style in args wins`() {
    val testResult =
        ParsedArgs.parseOptions(arrayOf<String>("--google-style", "--dropbox-style", "File.kt"))
    assertThat(testResult)
        .isEqualTo(
            parseResultOk(
                fileNames = listOf("File.kt"),
                formattingOptions = Formatter.DROPBOX_FORMAT,
            ))
  }

  @Test
  fun `error when parsing multiple args and one is unknown`() {
    val testResult =
        ParsedArgs.parseOptions(arrayOf<String>("@unknown", "--google-style", "File.kt"))
    assertThat(testResult).isEqualTo(ParseResult.Error("Unexpected option: @unknown"))
  }

  private fun assertSucceeds(parseResult: ParseResult): ParsedArgs {
    assertThat(parseResult).isInstanceOf(ParseResult.Ok::class.java)
    return (parseResult as ParseResult.Ok).parsedValue
  }

  private fun parseResultOk(
      fileNames: List<String> = emptyList(),
      formattingOptions: FormattingOptions = FormattingOptions(),
      dryRun: Boolean = false,
      setExitIfChanged: Boolean = false,
      removedUnusedImports: Boolean = true,
      stdinName: String? = null
  ): ParseResult.Ok {
    val returnedFormattingOptions =
        formattingOptions.copy(removeUnusedImports = removedUnusedImports)
    return ParseResult.Ok(
        ParsedArgs(fileNames, returnedFormattingOptions, dryRun, setExitIfChanged, stdinName))
  }
}
