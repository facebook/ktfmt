// Copyright (c) Facebook, Inc. and its affiliates.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.facebook.ktfmt

import com.google.common.truth.Truth.assertThat
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.lang.IllegalStateException
import org.junit.After
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@Suppress("FunctionNaming")
@RunWith(JUnit4::class)
class MainKtTest {

  private val root = createTempDir()

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
    assertThat(expandArgsToFileNames(arrayOf(fooBar.toString()))).containsExactly(fooBar)
  }

  @Test
  fun `expandArgsToFileNames - single arg which is not a file is not returned`() {
    val fooBar = root.resolve("foo.bar")
    assertThat(expandArgsToFileNames(arrayOf(fooBar.toString()))).isEmpty()
  }

  @Test
  fun `expandArgsToFileNames - single arg which is a directory is resolved to its recursively contained kt files`() {
    val dir = root.resolve("dir")
    dir.mkdirs()
    val foo = dir.resolve("foo.kt")
    foo.writeText("")
    val bar = dir.resolve("bar.kt")
    bar.writeText("")
    assertThat(expandArgsToFileNames(arrayOf(dir.toString()))).containsExactly(foo, bar)
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

    assertThat(expandArgsToFileNames(arrayOf(dir1.toString(), dir2.toString())))
        .containsExactly(foo1, bar1, foo2, bar2)
  }

  @Test
  fun `expandArgsToFileNames - a dash is an error`() {
    try {
      expandArgsToFileNames(
          arrayOf(
              root.resolve("foo.bar").toString(),
              File("-").toString()))
      fail("expected exception, but nothing was thrown")
    } catch (e: IllegalStateException) {
      assertThat(e.message).contains("Error")
    }
  }

  @Test
  fun `formatStdin formats an InputStream`() {
    val code = "fun    f1 (  ) :    Int =    0"
    val output = ByteArrayOutputStream()
    formatStdin(code.byteInputStream(), PrintStream(output))

    val expected = """fun f1(): Int = 0
      |""".trimMargin()
    assertThat(output.toString("UTF-8")).isEqualTo(expected)
  }
}
