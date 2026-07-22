package com.facebook.ktfmt

import kotlin.text.appendLine

interface Directive {
    val name: String
    val callback: (String) -> Unit

    fun matches(line: String): Boolean = line.startsWith("// $name")

    fun apply(line: String) {
        callback(line.removePrefix("// $name").trim())
    }
}

interface DirectiveContainer<T> {
    val result: T
    val directives: List<Directive>

    fun processDirectives(code: String): String = buildString {
        code.lines().forEach { line ->
            val directive = directives.firstOrNull { it.matches(line) }
            if (directive != null) directive.apply(line) else appendLine(line)
        }
    }
}