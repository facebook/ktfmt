package com.facebook.ktfmt.cli

import com.facebook.ktfmt.format.Formatter
import com.facebook.ktfmt.format.FormattingOptions
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters

@RunWith(Parameterized::class)
class ParsedArgsParameterizedTest(
    // Description is unused in tests
    //  only included here to fulfil contract of @Parameterized test runner
    private val description: String,
    private val expectedResult: ParseResult,
    private val inputArgs: Array<String>
) {

  @Test
  fun `combination of args gives expected result`() {
    val testResult = ParsedArgs.parseOptions(inputArgs)
    assertThat(testResult).isEqualTo(expectedResult)
  }

  companion object {


    /**
    Creates the list of parameters for ParsedArgsParameterizedTest
    Each instance of the parameters is an array containing:
      - a description string, which junit outputs to the console
      - the expected result of parsing,
      - an array of the input arguments
     */
    @JvmStatic
    @Parameters(name = "{index}: {0} - parseOptions({1})={2}")
    fun testData(): Iterable<Array<Any>> =
        arrayListOf(
            testCase(
                description = "Parses multiple args successfully",
                expectedParseResult =
                    parseResultOk(
                        fileNames = listOf("File.kt"),
                        formattingOptions = Formatter.GOOGLE_FORMAT,
                        dryRun = true,
                        setExitIfChanged = true),
                "--google-style",
                "--dry-run",
                "--set-exit-if-changed",
                "File.kt"),
            testCase(
                description = "Last style in args wins",
                expectedParseResult =
                    parseResultOk(
                        fileNames = listOf("File.kt"),
                        formattingOptions = Formatter.DROPBOX_FORMAT),
                "--google-style",
                "--dropbox-style",
                "File.kt"),
            testCase(
                description = "Error when parsing multiple args and one is unknown",
                expectedParseResult = ParseResult.Error("Unexpected option: @unknown"),
                "@unknown",
                "--google-style"))

    private fun testCase(
        description: String,
        expectedParseResult: ParseResult,
        vararg inputArgs: String
    ) = arrayOf(description, expectedParseResult, inputArgs)

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
}
