package com.facebook.ktfmt

import com.facebook.ktfmt.format.Formatter
import com.facebook.ktfmt.format.FormattingOptions
import com.facebook.ktfmt.format.TrailingCommaManagementStrategy

class FormattingOptionsDirectives(
    default: FormattingOptions = Formatter.META_FORMAT,
) : DirectiveContainer<FormattingOptions> {
  private val builder = default.toBuilder()

  override val result: FormattingOptions
    get() = builder.build()

  override val directives =
      listOf(
          FormattingOptionDirective("MAX_WIDTH") { builder.maxWidth(it.toInt()) },
          FormattingOptionDirective("BLOCK_INDENT") { builder.blockIndent(it.toInt()) },
          FormattingOptionDirective("CONTINUATION_INDENT") {
            builder.continuationIndent(it.toInt())
          },
          FormattingOptionDirective("TRAILING_COMMA_STRATEGY") {
            builder.trailingCommaManagementStrategy(TrailingCommaManagementStrategy.valueOf(it))
          },
          FormattingOptionDirective("REMOVE_UNUSED_IMPORTS") {
            builder.removeUnusedImports(it.toBooleanStrict())
          },
          FormattingOptionDirective("PRESERVE_LAMBDA_BREAKS") {
            builder.preserveLambdaBreaks(it.toBooleanStrict())
          },
          FormattingOptionDirective("PRINT_OPTS_AFTER_FORMATTING") {
            builder.debuggingPrintOpsAfterFormatting(it.toBooleanStrict())
          },
      )

  data class FormattingOptionDirective(
      override val name: String,
      override val callback: (String) -> Unit,
  ) : Directive
}
