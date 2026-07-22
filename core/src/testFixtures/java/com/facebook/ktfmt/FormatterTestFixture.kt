package com.facebook.ktfmt

import com.facebook.ktfmt.debughelpers.PrintAstVisitor
import com.facebook.ktfmt.format.Formatter
import com.facebook.ktfmt.format.FormattingOptions
import com.facebook.ktfmt.format.Parser
import com.google.common.truth.FailureMetadata
import com.google.common.truth.Subject
import com.google.common.truth.Truth
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.readText
import kotlin.io.path.writeText
import org.intellij.lang.annotations.Language
import org.junit.Assert

fun runFormatterTest(path: String, body: FormatterTestFixture.() -> Unit = {}) {
  val fixture = FormatterTestFixture(path)
  fixture.body()
  fixture.runTest()
}

class FormatterTestFixture(
    val sourcePath: String,
) {
  val formattingOptionsDirectives = FormattingOptionsDirectives()
  val formatterTestOptions = FormatterTestDirectives()

  fun runTest() {
    val sourceFile = TEST_DATA_PATH.resolve(sourcePath)
    val expectedFile = sourceFile.expectedFile

    var sourceCode = sourceFile.readText()
    sourceCode = formatterTestOptions.processDirectives(sourceCode).trim()
    sourceCode = formattingOptionsDirectives.processDirectives(sourceCode).trim()

    if (!expectedFile.exists()) {
      val formattedCode = formatCode(sourceCode, formattingOptionsDirectives.result)
      expectedFile.writeText(formattedCode)
      error("Expected file $expectedFile does not exist, created code:\n$formattedCode")
    }

    val expectedCode = expectedFile.readText()
    assertThatFormatting(sourceCode).isEqualTo(expectedCode)
  }

  fun formatCode(code: String, options: FormattingOptions): String = Formatter.format(options, code)

  fun assertThatFormatting(@Language("kts") code: String): FormattedCodeSubject {
    fun codes(): Subject.Factory<FormattedCodeSubject, String> {
      return Subject.Factory { metadata, subject ->
        FormattedCodeSubject(
            metadata,
            code = checkNotNull(subject),
            testOptions = formatterTestOptions.result,
            formattingOptions = formattingOptionsDirectives.result,
        )
      }
    }
    return Truth.assertAbout(codes()).that(code)
  }

  class FormattedCodeSubject(
      metadata: FailureMetadata,
      private val code: String,
      private val testOptions: FormatterTestOptions,
      private val formattingOptions: FormattingOptions,
  ) : Subject(metadata, code) {

    fun isEqualTo(@Language("kts") expectedFormatting: String) {
      if (!testOptions.allowTrailingWhitespace && expectedFormatting.lines().any { it.endsWith(" ") }) {
        throw RuntimeException(
            "Expected code contains trailing whitespace, which the formatter usually doesn't output:\n" +
                expectedFormatting
                    .lines()
                    .map { if (it.endsWith(" ")) "[$it]" else it }
                    .joinToString("\n"),
        )
      }
      val actualFormatting: String
      try {
        actualFormatting = Formatter.format(formattingOptions, code)
        if (actualFormatting != expectedFormatting) {
          reportError(code)
          println("# Output: ")
          println("#".repeat(20))
          println(actualFormatting)
          println("# Expected: ")
          println("#".repeat(20))
          println(expectedFormatting)
          println("#".repeat(20))
          println(
              "Need more information about the break operations? " +
                  "Run test with assertion with \"FormattingOptions(debuggingPrintOpsAfterFormatting = true)\"",
          )
        }
      } catch (e: Error) {
        reportError(code)
        throw e
      }
      Assert.assertEquals(expectedFormatting, actualFormatting)
    }

    private fun reportError(code: String) {
      val file = Parser.parse(code)
      println("# Parse tree of input: ")
      println("#".repeat(20))
      file.accept(PrintAstVisitor())
      println()
      println("# Input: ")
      println("#".repeat(20))
      println(code)
      println()
    }
  }

  companion object {
    val TEST_DATA_PATH = Paths.get("testData")

    val Path.expectedFile: Path
      get() {
        val fileName = this.fileName
        val name = fileName.nameWithoutExtension
        val extension = fileName.extension
        return resolveSibling("$name.expected.$extension")
      }
  }
}
