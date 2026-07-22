package com.facebook.ktfmt

data class FormatterTestOptions(
    val allowTrailingWhitespace: Boolean = false
)

class FormatterTestDirectives : DirectiveContainer<FormatterTestOptions> {
    override val result get() = FormatterTestOptions(allowTrailingWhitespace = allowTrailingWhitespace)

    var allowTrailingWhitespace: Boolean = false

    override val directives = listOf(
        FormatterTestOptionsDirective("ALLOW_TRAILING_WHITESPACE") { allowTrailingWhitespace = true },
    )

    data class FormatterTestOptionsDirective(
        override val name: String,
        override val callback: (String) -> Unit,
    ) : Directive
}
