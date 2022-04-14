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

import com.google.common.truth.Truth.assertThat
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.lang.IllegalStateException
import java.nio.file.Files
import java.util.concurrent.ForkJoinPool
import kotlin.io.path.createTempDirectory
import org.junit.After
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@Suppress("FunctionNaming")
@RunWith(JUnit4::class)
class MainTest {

  private val root = createTempDirectory().toFile()

  private val emptyInput = "".byteInputStream()
  private val out = ByteArrayOutputStream()
  private val err = ByteArrayOutputStream()

  @After
  fun tearDown() {
    root.deleteRecursively()
  }

  /**
   * Scenario: someone _really_ wants to format this file, regardless of its extension. When a
   * single argument file is given, it is used as is without filtering by extension.
   */
  @Test
  fun `expandArgsToFileNames - single file arg is used as is`() {
    val fooBar = root.resolve("foo.bar")
    fooBar.writeText("hi")
    assertThat(Main.expandArgsToFileNames(listOf(fooBar.toString()))).containsExactly(fooBar)
  }

  @Test
  fun `expandArgsToFileNames - single arg which is not a file is not returned`() {
    val fooBar = root.resolve("foo.bar")
    assertThat(Main.expandArgsToFileNames(listOf(fooBar.toString()))).isEmpty()
  }

  @Test
  fun `expandArgsToFileNames - single arg which is a directory is resolved to its recursively contained kt files`() {
    val dir = root.resolve("dir")
    dir.mkdirs()
    val foo = dir.resolve("foo.kt")
    foo.writeText("")
    val bar = dir.resolve("bar.kt")
    bar.writeText("")
    assertThat(Main.expandArgsToFileNames(listOf(dir.toString()))).containsExactly(foo, bar)
  }

  @Test
  fun `expandArgsToFileNames - multiple directory args are resolved to their recursively contained kt files`() {
    val dir1 = root.resolve("dir1")
    dir1.mkdirs()
    val foo1 = dir1.resolve("foo1.kt")
    foo1.writeText("")
    val bar1 = dir1.resolve("bar1.kt")
    bar1.writeText("")

    val dir2 = root.resolve("dir2")
    dir1.mkdirs()
    val foo2 = dir1.resolve("foo2.kt")
    foo2.writeText("")
    val bar2 = dir1.resolve("bar2.kt")
    bar2.writeText("")

    assertThat(Main.expandArgsToFileNames(listOf(dir1.toString(), dir2.toString())))
        .containsExactly(foo1, bar1, foo2, bar2)
  }

  @Test
  fun `expandArgsToFileNames - a dash is an error`() {
    try {
      Main.expandArgsToFileNames(listOf(root.resolve("foo.bar").toString(), File("-").toString()))
      fail("expected exception, but nothing was thrown")
    } catch (e: IllegalStateException) {
      assertThat(e.message).contains("Error")
    }
  }

  @Test
  fun `Using '-' as the filename formats an InputStream`() {
    val code = "fun    f1 (  ) :    Int =    0"
    Main(code.byteInputStream(), PrintStream(out), PrintStream(err), arrayOf("-")).run()

    val expected = "fun f1(): Int = 0\n"
    assertThat(out.toString("UTF-8")).isEqualTo(expected)
  }

  @Test
  fun `Parsing errors are reported (stdin)`() {
    val code = "fun    f1 (  "
    val returnValue =
        Main(code.byteInputStream(), PrintStream(out), PrintStream(err), arrayOf("-")).run()

    assertThat(returnValue).isEqualTo(1)
    assertThat(err.toString("UTF-8")).startsWith("<stdin>:1:14: error: ")
  }

  @Test
  fun `Parsing errors are reported (file)`() {
    val fooBar = root.resolve("foo.kt")
    fooBar.writeText("fun    f1 (  ")
    val returnValue =
        Main(emptyInput, PrintStream(out), PrintStream(err), arrayOf(fooBar.toString())).run()

    assertThat(returnValue).isEqualTo(1)
    assertThat(err.toString("UTF-8")).contains("foo.kt:1:14: error: ")
  }

  @Test
  fun `all files in args are processed, even if one of them has an error`() {
    val file1 = root.resolve("file1.kt")
    val file2Broken = root.resolve("file2.kt")
    val file3 = root.resolve("file3.kt")
    file1.writeText("fun    f1 ()  ")
    file2Broken.writeText("fun    f1 (  ")
    file3.writeText("fun    f1 ()  ")

    // Make Main() process files serially.
    val forkJoinPool = ForkJoinPool(1)

    val returnValue: Int =
        forkJoinPool
            .submit<Int> {
              Main(
                      emptyInput,
                      PrintStream(out),
                      PrintStream(err),
                      arrayOf(file1.toString(), file2Broken.toString(), file3.toString()))
                  .run()
            }
            .get()

    assertThat(returnValue).isEqualTo(1)
    assertThat(err.toString("UTF-8")).contains("Done formatting $file1")
    assertThat(err.toString("UTF-8")).contains("file2.kt:1:14: error: ")
    assertThat(err.toString("UTF-8")).contains("Done formatting $file3")
  }

  @Test
  fun `file is not modified if it is already formatted`() {
    val code = """fun f() = println("hello, world")""" + "\n"
    val formattedFile = root.resolve("formatted_file.kt")
    formattedFile.writeText(code)
    val formattedFilePath = formattedFile.toPath()

    val lastModifiedTimeBeforeRunningFormatter =
        Files.getLastModifiedTime(formattedFilePath).toMillis()
    Main(emptyInput, PrintStream(out), PrintStream(err), arrayOf(formattedFile.toString())).run()
    val lastModifiedTimeAfterRunningFormatter =
        Files.getLastModifiedTime(formattedFilePath).toMillis()

    assertThat(lastModifiedTimeBeforeRunningFormatter)
        .isEqualTo(lastModifiedTimeAfterRunningFormatter)
  }

  @Test
  fun `file is modified if it is not formatted`() {
    val code = """fun f() =   println(  "hello, world")""" + "\n"
    val unformattedFile = root.resolve("unformatted_file.kt")
    unformattedFile.writeText(code)
    val unformattedFilePath = unformattedFile.toPath()

    val lastModifiedTimeBeforeRunningFormatter =
        Files.getLastModifiedTime(unformattedFilePath).toMillis()
    // The test may run under 1ms, and we need to make sure the new file timestamp will be different
    Thread.sleep(100)
    Main(emptyInput, PrintStream(out), PrintStream(err), arrayOf(unformattedFile.toString())).run()
    val lastModifiedTimeAfterRunningFormatter =
        Files.getLastModifiedTime(unformattedFilePath).toMillis()

    assertThat(lastModifiedTimeBeforeRunningFormatter)
        .isLessThan(lastModifiedTimeAfterRunningFormatter)
  }

  @Test
  fun `dropbox-style is passed to formatter (file)`() {
    val code =
        """fun f() {
    for (child in
        node.next.next.next.next.next.next.next.next.next.next.next.next.next.next.data()) {
        println(child)
    }
}
"""
    val fooBar = root.resolve("foo.kt")
    fooBar.writeText(code)

    Main(
            emptyInput,
            PrintStream(out),
            PrintStream(err),
            arrayOf("--dropbox-style", fooBar.toString()))
        .run()

    assertThat(fooBar.readText()).isEqualTo(code)
  }

  @Test
  fun `dropbox-style is passed to formatter (stdin)`() {
    val code =
        """fun f() {
          |for (child in
          |node.next.next.next.next.next.next.next.next.next.next.next.next.next.next.data()) {
          |println(child)
          |}
          |}
          |""".trimMargin()
    val formatted =
        """fun f() {
          |    for (child in
          |        node.next.next.next.next.next.next.next.next.next.next.next.next.next.next.data()) {
          |        println(child)
          |    }
          |}
          |""".trimMargin()
    Main(
            code.byteInputStream(),
            PrintStream(out),
            PrintStream(err),
            arrayOf("--dropbox-style", "-"))
        .run()

    assertThat(out.toString("UTF-8")).isEqualTo(formatted)
  }

  @Test
  fun `expandArgsToFileNames - resolves 'kt' and 'kts' filenames only (recursively)`() {
    val f1 = root.resolve("1.kt")
    val f2 = root.resolve("2.kt")
    val f3 = root.resolve("3")
    val f4 = root.resolve("4.dummyext")
    val f5 = root.resolve("5.kts")

    val dir = root.resolve("foo")
    dir.mkdirs()
    val f6 = root.resolve("foo/1.kt")
    val f7 = root.resolve("foo/2.kts")
    val f8 = root.resolve("foo/3.dummyext")
    val files = listOf(f1, f2, f3, f4, f5, f6, f7, f8)
    for (f in files) {
      f.createNewFile()
    }
    assertThat(Main.expandArgsToFileNames(files.map { it.toString() }))
        .containsExactly(f1, f2, f5, f6, f7)
  }

  @Test
  fun `formatting from stdin prints formatted code to stdout regardless of whether it was already formatted`() {
    val expected = """fun f() = println("hello, world")""" + "\n"

    Main(
            """fun f (   ) =    println("hello, world")""".byteInputStream(),
            PrintStream(out),
            PrintStream(err),
            arrayOf("-"))
        .run()
    assertThat(out.toString("UTF-8")).isEqualTo(expected)

    out.reset()

    Main(
            """fun f () = println("hello, world")""".byteInputStream(),
            PrintStream(out),
            PrintStream(err),
            arrayOf("-"))
        .run()
    assertThat(out.toString("UTF-8")).isEqualTo(expected)
  }

  @Test
  fun `--dry-run prints filename and does not change file`() {
    val code = """fun f () =    println( "hello, world" )"""
    val file = root.resolve("foo.kt")
    file.writeText(code)

    Main(emptyInput, PrintStream(out), PrintStream(err), arrayOf("--dry-run", file.toString()))
        .run()

    assertThat(file.readText()).isEqualTo(code)
    assertThat(out.toString("UTF-8")).contains(file.toString())
  }

  @Test
  fun `--dry-run prints 'stdin' and does not reformat code from stdin`() {
    val code = """fun f () =    println( "hello, world" )"""

    Main(code.byteInputStream(), PrintStream(out), PrintStream(err), arrayOf("--dry-run", "-"))
        .run()

    assertThat(out.toString("UTF-8")).doesNotContain("hello, world")
    assertThat(out.toString("UTF-8")).isEqualTo("<stdin>\n")
  }

  @Test
  fun `--dry-run prints nothing when there are no changes needed (file)`() {
    val code = """fun f() = println("hello, world")\n"""
    val file = root.resolve("foo.kt")
    file.writeText(code)

    Main(emptyInput, PrintStream(out), PrintStream(err), arrayOf("--dry-run", file.toString()))
        .run()

    assertThat(out.toString("UTF-8")).isEmpty()
  }

  @Test
  fun `--dry-run prints nothing when there are no changes needed (stdin)`() {
    val code = """fun f() = println("hello, world")\n"""

    Main(code.byteInputStream(), PrintStream(out), PrintStream(err), arrayOf("--dry-run", "-"))
        .run()

    assertThat(out.toString("UTF-8")).isEmpty()
  }

  @Test
  fun `Exit code is 0 when there are changes (file)`() {
    val code = """fun f () =    println( "hello, world" )"""
    val file = root.resolve("foo.kt")
    file.writeText(code)

    val exitCode =
        Main(emptyInput, PrintStream(out), PrintStream(err), arrayOf(file.toString())).run()

    assertThat(exitCode).isEqualTo(0)
  }

  @Test
  fun `Exits with 0 when there are changes (stdin)`() {
    val code = """fun f () =    println( "hello, world" )"""

    val exitCode =
        Main(code.byteInputStream(), PrintStream(out), PrintStream(err), arrayOf("-")).run()

    assertThat(exitCode).isEqualTo(0)
  }

  @Test
  fun `Exit code is 1 when there are changes and --set-exit-if-changed is set (file)`() {
    val code = """fun f () =    println( "hello, world" )"""
    val file = root.resolve("foo.kt")
    file.writeText(code)

    val exitCode =
        Main(
                emptyInput,
                PrintStream(out),
                PrintStream(err),
                arrayOf("--set-exit-if-changed", file.toString()))
            .run()

    assertThat(exitCode).isEqualTo(1)
  }

  @Test
  fun `Exit code is 1 when there are changes and --set-exit-if-changed is set (stdin)`() {
    val code = """fun f () =    println( "hello, world" )"""

    val exitCode =
        Main(
                code.byteInputStream(),
                PrintStream(out),
                PrintStream(err),
                arrayOf("--set-exit-if-changed", "-"))
            .run()

    assertThat(exitCode).isEqualTo(1)
  }

  @Test
  fun `--set-exit-if-changed and --dry-run changes nothing, prints filenames, and exits with 1 (file)`() {
    val code = """fun f () =    println( "hello, world" )"""
    val file = root.resolve("foo.kt")
    file.writeText(code)

    val exitCode =
        Main(
                emptyInput,
                PrintStream(out),
                PrintStream(err),
                arrayOf("--dry-run", "--set-exit-if-changed", file.toString()))
            .run()

    assertThat(file.readText()).isEqualTo(code)
    assertThat(out.toString("UTF-8")).contains(file.toString())
    assertThat(exitCode).isEqualTo(1)
  }

  @Test
  fun `--set-exit-if-changed and --dry-run changes nothing, prints filenames, and exits with 1 (stdin)`() {
    val code = """fun f () =    println( "hello, world" )"""

    val exitCode =
        Main(
                code.byteInputStream(),
                PrintStream(out),
                PrintStream(err),
                arrayOf("--dry-run", "--set-exit-if-changed", "-"))
            .run()

    assertThat(out.toString("UTF-8")).doesNotContain("hello, world")
    assertThat(out.toString("UTF-8")).isEqualTo("<stdin>\n")
    assertThat(exitCode).isEqualTo(1)
  }
}
