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
  fun `kitchen sink of tests`() {
    // Don't add more tests here
    val code =
        """
        |fun
        |f  (
        |a : Int
        | , b: Double , c:String) {           var result = 0
        | val aVeryLongLongLongLongLongLongLongLongLongLongLongLongLongLongLongLongLongLongLongLongLongLongVar = 43
        |          foo.bar.zed.accept(
        |
        |          )
        |
        |          foo(
        |
        |          )
        |
        |          foo.bar.zed.accept(
        |                DoSomething.bar()
        |          )
        |
        |          bar(
        |          ImmutableList.newBuilder().add(1).add(1).add(1).add(1).add(1).add(1).add(1).add(1).add(1).add(1).build())
        |
        |
        |          ImmutableList.newBuilder().add(1).add(1).add(1).add(1).add(1).add(1).add(1).add(1).add(1).add(1).build()
        |     }
        |""".trimMargin()

    val expected =
        """
        |fun f(a: Int, b: Double, c: String) {
        |  var result = 0
        |  val aVeryLongLongLongLongLongLongLongLongLongLongLongLongLongLongLongLongLongLongLongLongLongLongVar =
        |    43
        |  foo.bar.zed.accept()
        |
        |  foo()
        |
        |  foo.bar.zed.accept(DoSomething.bar())
        |
        |  bar(
        |    ImmutableList.newBuilder()
        |      .add(1)
        |      .add(1)
        |      .add(1)
        |      .add(1)
        |      .add(1)
        |      .add(1)
        |      .add(1)
        |      .add(1)
        |      .add(1)
        |      .add(1)
        |      .build()
        |  )
        |
        |  ImmutableList.newBuilder()
        |    .add(1)
        |    .add(1)
        |    .add(1)
        |    .add(1)
        |    .add(1)
        |    .add(1)
        |    .add(1)
        |    .add(1)
        |    .add(1)
        |    .add(1)
        |    .build()
        |}
        |""".trimMargin()

    assertThatFormatting(code).withOptions(GOOGLE_FORMAT).isEqualTo(expected)
    // Don't add more tests here
  }

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

  @Test
  fun `indent parameters after a break when there's a lambda afterwards`() =
      assertFormatted(
          """
      |---------------------------
      |class C {
      |  fun method() {
      |    Foo.FooBar(
      |        param1,
      |        param2
      |      )
      |      .apply {
      |        //
      |        foo
      |      }
      |  }
      |}
      |""".trimMargin(),
          formattingOptions = GOOGLE_FORMAT,
          deduceMaxWidth = true)

  @Test
  fun `function calls with multiple arguments`() =
      assertFormatted(
          """
      |fun f() {
      |  foo(1, 2, 3)
      |
      |  foo(
      |    123456789012345678901234567890,
      |    123456789012345678901234567890,
      |    123456789012345678901234567890
      |  )
      |}
      |""".trimMargin(),
          formattingOptions = GOOGLE_FORMAT)

  @Test
  fun `line breaks inside when expressions and conditions`() =
      assertFormatted(
          """
      |fun f() {
      |  return Text.create(c)
      |    .onTouch {
      |      when (it.motionEvent.action) {
      |        ACTION_DOWN ->
      |          Toast.makeText(it.view.context, "Down!", Toast.LENGTH_SHORT, blablabla).show()
      |        ACTION_UP -> Toast.makeText(it.view.context, "Up!", Toast.LENGTH_SHORT).show()
      |        ACTION_DOWN ->
      |          Toast.makeText(
      |              it.view.context,
      |              "Down!",
      |              Toast.LENGTH_SHORT,
      |              blablabla,
      |              blablabl,
      |              blablabl,
      |              blablabl,
      |              blabla
      |            )
      |            .show()
      |      }
      |    }
      |    .build()
      |}
      |""".trimMargin(),
          formattingOptions = GOOGLE_FORMAT,
      )

  @Test
  fun `anonymous function`() =
      assertFormatted(
          """
      |fun f() {
      |  setListener(
      |    fun(number: Int) {
      |      println(number)
      |    }
      |  )
      |}
      |""".trimMargin(),
          formattingOptions = GOOGLE_FORMAT,
      )

  @Test
  fun `avoid newline before lambda argument if it is named`() =
      assertFormatted(
          """
      |private fun f(items: List<Int>) {
      |  doSomethingCool(
      |    items,
      |    lambdaArgument = {
      |      step1()
      |      step2()
      |    }
      |  ) { it.doIt() }
      |}
      |""".trimMargin(),
          formattingOptions = GOOGLE_FORMAT,
      )

  @Test
  fun `anonymous function with receiver`() =
      assertFormatted(
          """
      |fun f() {
      |  setListener(
      |    fun View.() {
      |      println(this)
      |    }
      |  )
      |}
      |""".trimMargin(),
          formattingOptions = GOOGLE_FORMAT,
      )

  @Test
  fun `function calls with multiple named arguments`() =
      assertFormatted(
          """
      |fun f() {
      |  foo(1, b = 2, c = 3)
      |
      |  foo(
      |    123456789012345678901234567890,
      |    b = 23456789012345678901234567890,
      |    c = 3456789012345678901234567890
      |  )
      |}
      |""".trimMargin(),
          formattingOptions = GOOGLE_FORMAT)

  @Test
  fun `Arguments are blocks`() =
      assertFormatted(
          """
      |--------------------------------------------------
      |override fun visitProperty(property: KtProperty) {
      |  builder.sync(property)
      |  builder.block(ZERO) {
      |    declareOne(
      |      kind = DeclarationKind.FIELD,
      |      modifiers = property.modifierList,
      |      valOrVarKeyword =
      |        property.valOrVarKeyword.text,
      |      typeParametersBlaBla =
      |        property.typeParameterList,
      |      receiver = property.receiverTypeReference,
      |      name = property.nameIdentifier?.text,
      |      type = property.typeReference,
      |      typeConstraintList =
      |        property.typeConstraintList,
      |      delegate = property.delegate,
      |      initializer = property.initializer
      |    )
      |  }
      |}
      |""".trimMargin(),
          formattingOptions = GOOGLE_FORMAT,
          deduceMaxWidth = true)

  @Test
  fun `keep last expression in qualified indented`() =
      assertFormatted(
          """
      |-----------------------
      |fun f() {
      |  Stuff()
      |    .doIt(
      |      Foo.doIt()
      |        .doThat()
      |    )
      |    .doIt(
      |      Foo.doIt()
      |        .doThat()
      |    )
      |}
      |""".trimMargin(),
          formattingOptions = GOOGLE_FORMAT,
          deduceMaxWidth = true)

  @Test
  fun `if expression with else`() =
      assertFormatted(
          """
      |fun maybePrint(b: Boolean) {
      |  println(if (b) 1 else 2)
      |  println(
      |    if (b) {
      |      val a = 1 + 1
      |      2 * a
      |    } else 2
      |  )
      |  return if (b) 1 else 2
      |}
      |""".trimMargin(),
          formattingOptions = GOOGLE_FORMAT)

  @Test
  fun `named arguments indent their value expression`() =
      assertFormatted(
          """
      |fun f() =
      |  Bar(
      |    tokens =
      |      mutableListOf<Token>().apply {
      |        // Printing
      |        print()
      |      },
      |    duration = duration
      |  )
      |""".trimMargin(),
          formattingOptions = GOOGLE_FORMAT)

  @Test
  fun `breaking long binary operations`() =
      assertFormatted(
          """
      |--------------------
      |fun foo() {
      |  val finalWidth =
      |    value1 +
      |      value2 +
      |      (value3 +
      |        value4 +
      |        value5) +
      |      foo(v) +
      |      (1 + 2) +
      |      function(
      |        value7,
      |        value8
      |      ) +
      |      value9
      |}
      |""".trimMargin(),
          formattingOptions = GOOGLE_FORMAT,
          deduceMaxWidth = true)

  @Test
  fun `binary operators dont break when the last one is a lambda`() =
      assertFormatted(
          """
      |-----------------------
      |fun binaryOps() {
      |  foo =
      |    foo + bar + dsl {
      |      baz = 1
      |    }
      |  boo =
      |    boo + ba + f(1) {
      |      bam = 1
      |    }
      |}
      |""".trimMargin(),
          formattingOptions = GOOGLE_FORMAT,
          deduceMaxWidth = true)

  @Test
  fun `binary operators break correctly when there's multiple before a lambda`() =
      assertFormatted(
          """
      |----------------------
      |foo =
      |  foo +
      |    bar +
      |    dsl +
      |    foo +
      |      bar {
      |    baz = 1
      |  }
      |""".trimMargin(),
          formattingOptions = GOOGLE_FORMAT,
          deduceMaxWidth = true)

  @Test
  fun `handle casting with breaks`() =
      assertFormatted(
          """
      |-------------------
      |fun castIt(
      |  something: Any
      |) {
      |  println(
      |    something is
      |      List<String>
      |  )
      |  doIt(
      |    something as
      |      List<String>
      |  )
      |  println(
      |    something is
      |      PairList<
      |        String,
      |        Int>
      |  )
      |  doIt(
      |    something as
      |      PairList<
      |        String,
      |        Int>
      |  )
      |  println(
      |    a is Int &&
      |      b is String
      |  )
      |}
      |""".trimMargin(),
          formattingOptions = GOOGLE_FORMAT,
          deduceMaxWidth = true)

  @Test
  fun `line breaks in function arguments`() =
      assertFormatted(
          """
      |--------------------------------------------------
      |fun f() {
      |  computeBreaks(
      |    javaOutput.commentsHelper,
      |    maxWidth,
      |    Doc.State(+0, 0)
      |  )
      |  computeBreaks(
      |    output.commentsHelper,
      |    maxWidth,
      |    State(0)
      |  )
      |  doc.computeBreaks(
      |    javaOutput.commentsHelper,
      |    maxWidth,
      |    Doc.State(+0, 0)
      |  )
      |  doc.computeBreaks(
      |    output.commentsHelper,
      |    maxWidth,
      |    State(0)
      |  )
      |}
      |""".trimMargin(),
          formattingOptions = GOOGLE_FORMAT,
          deduceMaxWidth = true)

  // TODO: there's a bug here - the last case shouldn't break after 'foo'.
  @Test
  fun `different indentation in chained calls`() =
      assertFormatted(
          """
      |----------------------
      |fun f() {
      |  fooDdoIt(
      |    foo1,
      |    foo2,
      |    foo3
      |  )
      |  foo.doIt(
      |    foo1,
      |    foo2,
      |    foo3
      |  )
      |  foo
      |    .doIt(
      |      foo1,
      |      foo2,
      |      foo3
      |    )
      |    .doThat()
      |}
      |""".trimMargin(),
          formattingOptions = GOOGLE_FORMAT,
          deduceMaxWidth = true)

  @Test
  fun `a secondary constructor with many arguments passed to delegate`() =
      assertFormatted(
          """
      |--------------------------------------------------
      |data class Foo {
      |  constructor(
      |    val number: Int,
      |    val name: String,
      |    val age: Int,
      |    val title: String,
      |    val offspring: List<Foo>
      |  ) : this(
      |    number,
      |    name,
      |    age,
      |    title,
      |    offspring,
      |    offspring
      |  )
      |}
      |""".trimMargin(),
          formattingOptions = GOOGLE_FORMAT,
          deduceMaxWidth = true)

  @Test
  fun `a secondary constructor with no arguments passed to delegate`() =
      assertFormatted(
          """
      |--------------------------------------------------
      |data class Foo {
      |  constructor() :
      |    this(
      |      Foo.createSpeciallyDesignedParameter(),
      |      Foo.createSpeciallyDesignedParameter(),
      |    )
      |}
      |""".trimMargin(),
          formattingOptions = GOOGLE_FORMAT,
          deduceMaxWidth = true)

  @Test
  fun `handle trailing commas (function calls)`() =
      assertFormatted(
          """
      |------------------------
      |fun main() {
      |  foo(
      |    3,
      |  )
      |
      |  foo<Int>(
      |    3,
      |  )
      |
      |  foo<
      |    Int,
      |  >(
      |    3,
      |  )
      |
      |  foo<Int>(
      |    "asdf",
      |    "asdf"
      |  )
      |
      |  foo<
      |    Int,
      |  >(
      |    "asd",
      |    "asd",
      |  )
      |
      |  foo<
      |    Int,
      |    Boolean,
      |  >(
      |    3,
      |  )
      |}
      |""".trimMargin(),
          formattingOptions = GOOGLE_FORMAT,
          deduceMaxWidth = true)

  @Test
  fun `an assortment of tests for emitQualifiedExpression`() =
      assertFormatted(
          """
      |--------------------------------------
      |fun f() {
      |  // Regression test: https://github.com/facebookincubator/ktfmt/issues/56
      |  kjsdfglkjdfgkjdfkgjhkerjghkdfj
      |    ?.methodName1()
      |
      |  // a series of field accesses followed by a single call expression
      |  // is kept together.
      |  abcdefghijkl.abcdefghijkl
      |    ?.methodName2()
      |
      |  // Similar to above.
      |  abcdefghijkl.abcdefghijkl
      |    ?.methodName3?.abcdefghijkl()
      |
      |  // Multiple call expressions cause each part of the expression
      |  // to be placed on its own line.
      |  abcdefghijkl
      |    ?.abcdefghijkl
      |    ?.methodName4()
      |    ?.abcdefghijkl()
      |
      |  // Don't break first call expression if it fits.
      |  foIt(something.something.happens())
      |    .thenReturn(result)
      |
      |  // Break after `longerThanFour(` because it's longer than 4 chars
      |  longerThanFour(
      |      something.somethingBlaBla
      |        .happens()
      |    )
      |    .thenReturn(result)
      |
      |  // Similarly to above, when part of qualified expression.
      |  foo
      |    .longerThanFour(
      |      something.somethingBlaBla
      |        .happens()
      |    )
      |    .thenReturn(result)
      |
      |  // Keep 'super' attached to the method name
      |  super.abcdefghijkl
      |    .methodName4()
      |    .abcdefghijkl()
      |}
      |""".trimMargin(),
          formattingOptions = GOOGLE_FORMAT,
          deduceMaxWidth = true)

  @Test
  fun `keep parenthesis and braces together when there's only one lambda argument`() =
      assertFormatted(
          """
      |fun f() {
      |  doIt({})
      |  doIt({ it + it })
      |  doIt({
      |    val a = it
      |    a + a
      |  })
      |  doIt(functor = { it + it })
      |  doIt(
      |    functor = {
      |      val a = it
      |      a + a
      |    }
      |  )
      |}
      |""".trimMargin(),
          formattingOptions = GOOGLE_FORMAT)

  @Test
  fun `chained calls that don't fit in one line`() =
      assertFormatted(
          """
      |---------------------------
      |fun f() {
      |  foo(
      |      println("a"),
      |      println("b")
      |    )
      |    .bar(
      |      println("b"),
      |      println("b")
      |    )
      |}
      |""".trimMargin(),
          formattingOptions = GOOGLE_FORMAT,
          deduceMaxWidth = true)

  @Test
  fun `lambda assigned to variable does not break before brace`() =
      assertFormatted(
          """
      |fun doIt() {
      |  val lambda = {
      |    doItOnce()
      |    doItTwice()
      |  }
      |}
      |
      |fun foo() = {
      |  doItOnce()
      |  doItTwice()
      |}
      |""".trimMargin())

  @Test
  fun `assignment of a scoping function`() =
      assertFormatted(
          """
      |----------------------------
      |fun longName() =
      |    coroutineScope {
      |  foo()
      |  //
      |}
      |""".trimMargin(),
          formattingOptions = GOOGLE_FORMAT,
          deduceMaxWidth = true)

  @Test
  fun `if expression with multiline condition`() =
      assertFormatted(
          """
      |----------------------------
      |fun foo() {
      |  if (expressions1 &&
      |      expression2 &&
      |      expression3
      |  ) {
      |    bar()
      |  }
      |
      |  if (foo(
      |      expressions1 &&
      |        expression2 &&
      |        expression3
      |    )
      |  ) {
      |    bar()
      |  }
      |}
      |""".trimMargin(),
          formattingOptions = GOOGLE_FORMAT,
          deduceMaxWidth = true)

  @Test
  fun `when() expression with multiline condition`() =
      assertFormatted(
          """
      |-----------------------
      |fun foo() {
      |  when (expressions1 +
      |      expression2 +
      |      expression3
      |  ) {
      |    1 -> print(1)
      |    2 -> print(2)
      |  }
      |
      |  when (foo(
      |      expressions1 &&
      |        expression2 &&
      |        expression3
      |    )
      |  ) {
      |    1 -> print(1)
      |    2 -> print(2)
      |  }
      |}
      |""".trimMargin(),
          formattingOptions = GOOGLE_FORMAT,
          deduceMaxWidth = true)

  @Test
  fun `handle destructuring declaration`() =
      assertFormatted(
          """
      |-------------------------------------------
      |fun f() {
      |  val (a, b: Int) = listOf(1, 2)
      |  val (asd, asd, asd, asd, asd, asd, asd) =
      |    foo.bar(asdasd, asdasd)
      |
      |  val (accountType, accountId) =
      |    oneTwoThreeFourFiveSixSeven(
      |      foo,
      |      bar,
      |      zed,
      |      boo
      |    )
      |}
      |""".trimMargin(),
          formattingOptions = GOOGLE_FORMAT,
          deduceMaxWidth = true)

  @Test
  fun `assignment in a dsl does not break if not needed`() =
      assertFormatted(
          """
      |---------------------
      |foo = fooDsl {
      |  bar = barDsl {
      |    baz = bazDsl {
      |      bal = balDsl {
      |        bim = 1
      |      }
      |    }
      |  }
      |}
      |""".trimMargin(),
          formattingOptions = GOOGLE_FORMAT,
          deduceMaxWidth = true)

  @Test
  fun `assignment in a dsl breaks when needed`() =
      assertFormatted(
          """
      |------------------
      |foo = fooDsl {
      |  bar += barDsl {
      |    baz = bazDsl {
      |      bal =
      |          balDsl {
      |        bim = 1
      |      }
      |    }
      |  }
      |}
      |""".trimMargin(),
          formattingOptions = GOOGLE_FORMAT,
          deduceMaxWidth = true)
}
