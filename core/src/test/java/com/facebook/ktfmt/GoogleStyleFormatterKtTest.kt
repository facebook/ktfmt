/*
 * Copyright (c) Facebook, Inc. and its affiliates.
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

package com.facebook.ktfmt

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@Suppress("FunctionNaming")
@RunWith(JUnit4::class)
class GoogleStyleFormatterKtTest {

  @Test
  fun `class params are placed each in their own line`() =
      assertFormatted(
          """
      |-----------------------------------------
      |class Foo(
      |  a: Int,
      |  var b: Double,
      |  val c: String
      |) {
      |  //
      |}
      |
      |class Foo(
      |  a: Int,
      |  var b: Double,
      |  val c: String
      |)
      |
      |class Foo(
      |  a: Int,
      |  var b: Int,
      |  val c: Int
      |) {
      |  //
      |}
      |
      |class Bi(
      |  a: Int,
      |  var b: Int,
      |  val c: Int
      |) {
      |  //
      |}
      |
      |class C(a: Int, var b: Int, val c: Int) {
      |  //
      |}
      |""".trimMargin(),
          formattingOptions = GOOGLE_FORMAT,
          deduceMaxWidth = true)

  @Test
  fun `function params are placed each in their own line`() =
      assertFormatted(
          """
      |-----------------------------------------
      |fun foo12(
      |  a: Int,
      |  var b: Double,
      |  val c: String
      |) {
      |  //
      |}
      |
      |fun foo12(
      |  a: Int,
      |  var b: Double,
      |  val c: String
      |)
      |
      |fun foo12(
      |  a: Int,
      |  var b: Double,
      |  val c: String
      |) = 5
      |
      |fun foo12(
      |  a: Int,
      |  var b: Int,
      |  val c: Int
      |) {
      |  //
      |}
      |
      |fun bi12(
      |  a: Int,
      |  var b: Int,
      |  val c: Int
      |) {
      |  //
      |}
      |
      |fun c12(a: Int, var b: Int, val c: Int) {
      |  //
      |}
      |""".trimMargin(),
          formattingOptions = GOOGLE_FORMAT,
          deduceMaxWidth = true)

  @Test
  fun `return type doesn't fit in one line`() =
      assertFormatted(
          """
      |--------------------------------------------------
      |interface X {
      |  fun f(
      |    arg1: Arg1Type,
      |    arg2: Arg2Type
      |  ): Map<String, Map<String, Double>>? {
      |    //
      |  }
      |
      |  fun functionWithGenericReturnType(
      |    arg1: Arg1Type,
      |    arg2: Arg2Type
      |  ): Map<String, Map<String, Double>>? {
      |    //
      |  }
      |}
      |""".trimMargin(),
          formattingOptions = GOOGLE_FORMAT,
          deduceMaxWidth = true)
}
