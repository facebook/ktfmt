package com.facebook.ktfmt.format

import com.facebook.ktfmt.TEST_DATA_ROOT
import com.facebook.ktfmt.runFormatterTest
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.nio.file.Files
import kotlin.io.path.extension
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import kotlin.streams.asSequence
import kotlin.test.Test

@RunWith(Parameterized::class)
class FormatterTest2(private val relativePath: String) {

    @Test
    fun format() = runFormatterTest(relativePath)

    companion object {
        val TEST_DATA_PATH = TEST_DATA_ROOT.resolve("format/meta")

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data(): List<Array<String>> =
            Files.walk(TEST_DATA_PATH).asSequence()
                .filter { it.isRegularFile() }
                .filter { it.extension == "kt" || it.extension == "kts" }
                .filterNot { it.name.contains(".expected.") } // skip golden files
                .map { TEST_DATA_ROOT.relativize(it).toString() }
                .sorted()
                .map { arrayOf<String>(it) }
                .toList()
    }
}
