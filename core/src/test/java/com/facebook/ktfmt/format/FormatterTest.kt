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

package com.facebook.ktfmt.format

import com.facebook.ktfmt.format.Formatter.META_FORMAT
import com.facebook.ktfmt.assertFormatted
import com.facebook.ktfmt.assertThatFormatting
import com.facebook.ktfmt.defaultTestFormattingOptions
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.fail
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@Suppress("FunctionNaming")
@RunWith(JUnit4::class)
class FormatterTest {

  @Test
  fun `handle extension methods with very long names`() = assertFormatted(
      """
      |//////////////////////////////////////////
      |fun LongReceiverNameThatRequiresBreaking
      |    .doIt() {}
      |
      |fun LongButNotTooLong.doIt(
      |    n: Int,
      |    f: Float
      |) {}
      |"""
          .trimMargin(),
      deduceMaxWidth = true,
  )

  @Test
  fun `handle extension properties`() = assertFormatted(
      """
      |val Int.isPrime: Boolean
      |  get() = runMillerRabinPrimality(this)
      |"""
          .trimMargin(),
  )

  @Test
  fun `generic extension property`() = assertFormatted(
      """
      |val <T> List<T>.twiceSize = 2 * size()
      |"""
          .trimMargin(),
  )

  @Test
  fun `handle file annotations`() {
    assertFormatted(
        """
        |@file:JvmName("DifferentName")
        |
        |package com.somecompany.example
        |
        |import com.somecompany.example2
        |
        |class Foo {
        |  val a = example2("and 1")
        |}
        |"""
            .trimMargin(),
    )

    assertFormatted(
        """
        |@file:JvmName("DifferentName") // Comment
        |
        |package com.somecompany.example
        |
        |import com.somecompany.example2
        |
        |class Foo {
        |  val a = example2("and 1")
        |}
        |"""
            .trimMargin(),
    )

    assertFormatted(
        """
        |@file:JvmName("DifferentName")
        |
        |// Comment
        |
        |package com.somecompany.example
        |
        |import com.somecompany.example2
        |
        |class Foo {
        |  val a = example2("and 1")
        |}
        |"""
            .trimMargin(),
    )

    assertFormatted(
        """
        |@file:JvmName("DifferentName")
        |
        |// Comment
        |package com.somecompany.example
        |
        |import com.somecompany.example2
        |
        |class Foo {
        |  val a = example2("and 1")
        |}
        |"""
            .trimMargin(),
    )
  }

  @Test
  fun `handle init block`() = assertFormatted(
      """
      |class Foo {
      |  init {
      |    println("Init!")
      |  }
      |}
      |"""
          .trimMargin(),
  )

  @Test
  fun `handle interface delegation`() = assertFormatted(
      """
      |class MyList(impl: List<Int>) : Collection<Int> by impl
      |"""
          .trimMargin(),
  )

  @Test
  fun `handle property delegation`() = assertFormatted(
      """
      |val a by lazy { 1 + 1 }
      |"""
          .trimMargin(),
  )

  @Test
  fun `handle property delegation with type and breaks`() = assertFormatted(
      """
      |/////////////////////////////////
      |val importantValue: Int by lazy {
      |  1 + 1
      |}
      |
      |val importantValue: Int by lazy {
      |  val b = 1 + 1
      |  b + b
      |}
      |
      |val importantValueLonger:
      |    Int by lazy { 1 + 1 }
      |
      |val importantValue: Int by
      |    doIt(1 + 1)
      |"""
          .trimMargin(),
      deduceMaxWidth = true,
  )

  @Test
  fun `handle multi-annotations with use-site targets`() = assertFormatted(
      """
      |class Something {
      |  @field:[Inject Named("WEB_VIEW")]
      |  internal lateinit var httpClient: OkHttpClient
      |
      |  @field:[Inject Named("WEB_VIEW")]
      |  var httpClient: OkHttpClient
      |
      |  @Px
      |  @field:[Inject Named("WEB_VIEW")]
      |  var httpClient: OkHttpClient
      |}
      |"""
          .trimMargin(),
  )

  @Test
  fun `handle parameters with annoations with parameters`() = assertFormatted(
      """
      |class Something {
      |  fun doIt(@Magic(withHat = true) foo: Foo) {
      |    println(foo)
      |  }
      |}
      |"""
          .trimMargin(),
  )

  @Test
  fun `handle lambda types`() = assertFormatted(
      """
      |val listener1: (Boolean) -> Unit = { b -> !b }
      |
      |val listener2: () -> Unit = {}
      |
      |val listener3: (Int, Double) -> Int = { a, b -> a }
      |
      |val listener4: Int.(Int, Boolean) -> Unit
      |"""
          .trimMargin(),
  )

  @Test
  fun `handle unicode in string literals`() = assertFormatted(
      """
      |val a = "\uD83D\uDC4D"
      |"""
          .trimMargin(),
  )

  @Test
  fun `handle casting`() = assertFormatted(
      """
      |fun castIt(o: Object) {
      |  println(o is Double)
      |  println(o !is Double)
      |  doIt(o as Int)
      |  doIt(o as? Int)
      |}
      |"""
          .trimMargin(),
  )

  @Test
  fun `handle casting with breaks`() = assertFormatted(
      """
      |///////////////////////
      |fun castIt(
      |    something: Any
      |) {
      |  doIt(
      |      something
      |          as List<*>)
      |  doIt(
      |      something
      |          is List<*>)
      |  println(
      |      something
      |          is
      |          List<String>)
      |  doIt(
      |      something
      |          as
      |          List<String>)
      |  println(
      |      something
      |          is
      |          PairList<
      |              String,
      |              Int>)
      |  doIt(
      |      something
      |          as
      |          PairList<
      |              String,
      |              Int>)
      |  println(
      |      a is Int &&
      |          b is String)
      |  l.b?.s?.sOrNull() is
      |      SomethingLongEnough
      |}
      |
      |val a =
      |    l.sOrNull() is
      |        SomethingLongEnough
      |"""
          .trimMargin(),
      deduceMaxWidth = true,
  )

  @Test
  fun `handle collection literals in annotations`() = assertFormatted(
      """
      |@Foo(a = [1, 2])
      |fun doIt(o: Object) {
      |  //
      |}
      |"""
          .trimMargin(),
  )

  @Test
  fun `handle try, catch and finally`() = assertFormatted(
      """
      |fun foo() {
      |  try {
      |    bar()
      |  } catch (e: Exception) {
      |    throw e
      |  } finally {
      |    println("finally")
      |  }
      |}
      |"""
          .trimMargin(),
  )

  @Test
  fun `handle infix methods`() = assertFormatted(
      """
      |fun numbers() {
      |  (0 until 100).size
      |}
      |"""
          .trimMargin(),
  )

  @Test
  fun `handle while loops`() = assertFormatted(
      """
      |fun numbers() {
      |  while (1 < 2) {
      |    println("Everything is okay")
      |  }
      |}
      |"""
          .trimMargin(),
  )

  @Test
  fun `handle do while loops`() = assertFormatted(
      """
      |fun numbers() {
      |  do {
      |    println("Everything is okay")
      |  } while (1 < 2)
      |
      |  do while (1 < 2)
      |}
      |"""
          .trimMargin(),
  )

  @Test
  fun `handle break and continue`() = assertFormatted(
      """
      |fun numbers() {
      |  while (1 < 2) {
      |    if (true) {
      |      break
      |    }
      |    if (false) {
      |      continue
      |    }
      |  }
      |}
      |"""
          .trimMargin(),
  )

  @Test
  fun `handle all kinds of labels and jumps`() = assertFormatted(
      """
      |fun f(a: List<Int>) {
      |  a.map {
      |    myloop@ for (i in a) {
      |      if (true) {
      |        break@myloop
      |      } else if (false) {
      |        continue@myloop
      |      } else {
      |        a.map `inner map`@{
      |          return@`inner map`
      |        }
      |      }
      |    }
      |    return@map 2 * it
      |  }
      |}
      |"""
          .trimMargin(),
  )

  @Test
  fun `don't crash on top level statements with semicolons`() {
    val code =
        """
        |val x = { 0 };
        |
        |foo({ 0 });
        |
        |foo { 0 };
        |
        |val fill = 0;
        |"""
            .trimMargin()
    val expected =
        """
        |val x = { 0 }
        |
        |foo({ 0 })
        |
        |foo { 0 }
        |
        |val fill = 0
        |"""
            .trimMargin()
    assertThatFormatting(code).isEqualTo(expected)
  }

  @Test
  fun `preserve semicolons in enums`() {
    val code =
        """
        |enum class SemiColonIsNotRequired {
        |  TRUE, FALSE;
        |}
        |
        |enum class SemiColonIsRequired {
        |  ONE, TWO;
        |
        |  fun isOne(): Boolean = this == ONE
        |}
        |"""
            .trimMargin()
    val expected =
        """
        |enum class SemiColonIsNotRequired {
        |  TRUE,
        |  FALSE
        |}
        |
        |enum class SemiColonIsRequired {
        |  ONE,
        |  TWO;
        |
        |  fun isOne(): Boolean = this == ONE
        |}
        |"""
            .trimMargin()
    assertThatFormatting(code).isEqualTo(expected)
  }

  @Test
  fun `preserve semicolons in comments and strings`() {
    val code =
        """
        |fun f() {
        |  val x = ";"
        |  val x = $TQ  don't touch ; in raw strings $TQ
        |}
        |
        |// Don't touch ; inside comments.
        |
        |/** Don't touch ; inside comments. */
        |"""
            .trimMargin()
    val expected =
        """
        |fun f() {
        |  val x = ";"
        |  val x = $TQ  don't touch ; in raw strings $TQ
        |}
        |
        |// Don't touch ; inside comments.
        |
        |/** Don't touch ; inside comments. */
        |"""
            .trimMargin()
    assertThatFormatting(code).isEqualTo(expected)
  }

  @Test
  fun `preserve semicolons in empty if-s and while-s`() {
    val code =
        """
        |fun f() {
        |  while (true);
        |  while (true) /** a */ ;
        |
        |  if (true);
        |  if (true) /** a */ ;
        |
        |  if (true)
        |    else
        |  ;
        |}
        |"""
            .trimMargin()
    val expected =
        """
        |fun f() {
        |  while (true) ;
        |  while (true)
        |  /** a */
        |  ;
        |
        |  if (true) ;
        |  if (true)
        |  /** a */
        |   ;
        |
        |  if (true)  else ;
        |}
        |"""
            .trimMargin()
    assertThatFormatting(code).isEqualTo(expected)
  }

  @Test
  fun `preserve semicolons between calls and dead lambdas`() {
    val code =
        """
        |fun f() {
        |  foo(0); { dead -> lambda }
        |
        |  foo(0) ; { dead -> lambda }
        |
        |  foo(0) /** a */ ; /** b */ { dead -> lambda }
        |
        |  foo(0) { trailing -> lambda }; { dead -> lambda }
        |
        |  foo { trailing -> lambda }; { dead -> lambda }
        |
        |  val x = foo(); { dead -> lambda }
        |
        |  val x = bar() && foo(); { dead -> lambda }
        |
        |  // `z` has a property and a method both named `bar`
        |  val x = z.bar; { dead -> lambda }
        |
        |  // `this` has a property and a method both named `bar`
        |  val x = bar; { dead -> lambda }
        |
        |  // Literally any callable expression is dangerous
        |  val x = (if (cond) x::foo else x::bar); { dead -> lambda }
        |
        |  funcCall(); { dead -> lambda }.withChained(call)
        |}
        |"""
            .trimMargin()
    val expected =
        """
        |fun f() {
        |  foo(0);
        |  { dead -> lambda }
        |
        |  foo(0);
        |  { dead -> lambda }
        |
        |  foo(0)
        |  /** a */
        |  ;
        |  /** b */
        |  { dead -> lambda }
        |
        |  foo(0) { trailing -> lambda };
        |  { dead -> lambda }
        |
        |  foo { trailing -> lambda };
        |  { dead -> lambda }
        |
        |  val x = foo();
        |  { dead -> lambda }
        |
        |  val x = bar() && foo();
        |  { dead -> lambda }
        |
        |  // `z` has a property and a method both named `bar`
        |  val x = z.bar;
        |  { dead -> lambda }
        |
        |  // `this` has a property and a method both named `bar`
        |  val x = bar;
        |  { dead -> lambda }
        |
        |  // Literally any callable expression is dangerous
        |  val x = (if (cond) x::foo else x::bar);
        |  { dead -> lambda }
        |
        |  funcCall();
        |  { dead -> lambda }.withChained(call)
        |}
        |"""
            .trimMargin()
    assertThatFormatting(code).isEqualTo(expected)
  }

  @Test
  fun `drop redundant semicolons`() {
    val code =
        """
        |package org.examples;
        |import org.examples.wow.MuchWow;
        |import org.examples.wow.ManyAmaze
        |
        |typealias Int2 = Int;
        |
        |fun f() {
        |  val a = 3;
        |  val x = 5 ; val y = ManyAmaze();
        |  myThingMap.forEach { val (key, value) = it; println("mapped ${"$"}MuchWow") }
        |  when {
        |    true -> "1"; false -> "0"
        |  }
        |  someLongVariableName.let {
        |    someReallyLongFunctionNameThatMakesThisNotFitInOneLineWithTheAboveVariable();
        |  }
        |  if (cond) ; else 6
        |} ;
        |
        |"""
            .trimMargin()
    val expected =
        """
        |package org.examples
        |
        |import org.examples.wow.ManyAmaze
        |import org.examples.wow.MuchWow
        |
        |typealias Int2 = Int
        |
        |fun f() {
        |  val a = 3
        |  val x = 5
        |  val y = ManyAmaze()
        |  myThingMap.forEach {
        |    val (key, value) = it
        |    println("mapped ${"$"}MuchWow")
        |  }
        |  when {
        |    true -> "1"
        |    false -> "0"
        |  }
        |  someLongVariableName.let {
        |    someReallyLongFunctionNameThatMakesThisNotFitInOneLineWithTheAboveVariable()
        |  }
        |  if (cond)  else 6
        |}
        |"""
            .trimMargin()
    assertThatFormatting(code).isEqualTo(expected)
  }

  @Test
  fun `pretty-print after dropping redundant semicolons`() {
    val code =
        """
        |fun f() {
        |  val veryLongName = 5;
        |}
        |"""
            .trimMargin()
    val expected =
        """
        |fun f() {
        |  val veryLongName = 5
        |}
        |"""
            .trimMargin()
    assertThatFormatting(code)
        .withOptions(defaultTestFormattingOptions.copy(maxWidth = 22))
        .isEqualTo(expected)
  }

  @Test
  fun `handle no parenthesis in lambda calls`() = assertFormatted(
      """
      |fun f() {
      |  a { println("a") }
      |}
      |"""
          .trimMargin(),
  )

  @Test
  fun `handle multi statement lambdas`() = assertFormatted(
      """
      |fun f() {
      |  a {
      |    println("a")
      |    println("b")
      |  }
      |}
      |"""
          .trimMargin(),
  )

  @Test
  fun `handle multi line one statement lambda`() = assertFormatted(
      """
      |/////////////////////////
      |fun f() {
      |  a {
      |    println(foo.bar.boom)
      |  }
      |}
      |"""
          .trimMargin(),
      deduceMaxWidth = true,
  )

  @Test
  fun `statements are wrapped in blocks`() = assertFormatted(
      """
      |fun f() {
      |  builder.block {
      |    getArgumentName().accept
      |    return
      |  }
      |}
      |"""
          .trimMargin(),
  )

  @Test
  fun `properly break fully qualified nested user types`() = assertFormatted(
      """
      |///////////////////////////////////////////////////////
      |val complicated:
      |    com.example.interesting.SomeType<
      |        com.example.interesting.SomeType<Int, Nothing>,
      |        com.example.interesting.SomeType<
      |            com.example.interesting.SomeType<
      |                Int, Nothing>,
      |            Nothing>> =
      |    DUMMY
      |"""
          .trimMargin(),
      deduceMaxWidth = true,
  )

  @Test
  fun `handle multi-line lambdas within lambdas and calling chains`() = assertFormatted(
      """
      |fun f() {
      |  builder.block(ZERO) {
      |    builder.token("when")
      |    expression1.let { subjectExp ->
      |      builder.token(")")
      |      return
      |    }
      |  }
      |  builder.block(ZERO) {
      |    expression2.subjectExpression.let { subjectExp ->
      |      builder.token(")")
      |      return
      |    }
      |  }
      |  builder.block(ZERO) {
      |    expression2.subjectExpression
      |        .let { subjectExp ->
      |          builder.token(")")
      |          return
      |        }
      |        .sum
      |  }
      |}
      |"""
          .trimMargin(),
  )

  @Test
  fun `handle multi line lambdas with explicit args`() = assertFormatted(
      """
      |////////////////////
      |fun f() {
      |  a { (x, y) ->
      |    x + y
      |  }
      |}
      |"""
          .trimMargin(),
      deduceMaxWidth = true,
  )

  @Test
  fun `handle lambda with destructuring and type`() = assertFormatted(
      """
      |fun f() {
      |  g { (a, b): List<Int> -> a }
      |  g { (a, b): List<Int>, (c, d): List<Int> -> a }
      |}
      |"""
          .trimMargin(),
  )

  @Test
  fun `handle parenthesis in lambda calls for now`() = assertFormatted(
      """
      |fun f() {
      |  a() { println("a") }
      |}
      |"""
          .trimMargin(),
  )

  @Test
  fun `handle chaining of calls with lambdas`() = assertFormatted(
      """
      |fun f() {
      |  bobby
      |      .map { x -> x * x }
      |      .map { x -> x * x }
      |      ?.map { x ->
      |        val y = x * x
      |        y
      |      }
      |      .sum
      |}
      |"""
          .trimMargin(),
  )

  @Test
  fun `handle break of lambda args per line with indentation`() = assertFormatted(
      """
      |///////////
      |fun f() {
      |  a() {
      |      arg1,
      |      arg2,
      |      x ->
      |    doIt()
      |    doIt()
      |  }
      |  a() {
      |      arg1,
      |      arg2,
      |      arg3
      |    ->
      |    doIt()
      |    doIt()
      |  }
      |}
      |"""
          .trimMargin(),
      deduceMaxWidth = true,
  )

  @Test
  fun `handle trailing comma in lambda`() = assertFormatted(
      """
      |///////////
      |fun f() {
      |  a() {
      |      arg1,
      |      arg2,
      |      x,
      |    ->
      |    doIt()
      |    doIt()
      |  }
      |}
      |"""
          .trimMargin(),
      deduceMaxWidth = true,
  )

  @Test
  fun `break before Elvis operator`() = assertFormatted(
      """
      |//////////////////////////////////////////////////
      |fun f() {
      |  someObject
      |      .someMethodReturningCollection()
      |      .map { it.someProperty }
      |      .find { it.contains(someSearchValue) }
      |      ?: someDefaultValue
      |}
      |"""
          .trimMargin(),
      deduceMaxWidth = true,
  )

  @Test
  fun `chain of Elvis operator`() = assertFormatted(
      """
      |///////////////////////////
      |fun f() {
      |  return option1()
      |      ?: option2()
      |      ?: option3()
      |      ?: option4()
      |      ?: option5()
      |}
      |"""
          .trimMargin(),
      deduceMaxWidth = true,
  )

  @Test
  fun `Elvis operator mixed with plus operator breaking on plus`() = assertFormatted(
      """
      |////////////////////////
      |fun f() {
      |  return option1()
      |      ?: option2() +
      |          option3()
      |      ?: option4() +
      |          option5()
      |}
      |"""
          .trimMargin(),
      deduceMaxWidth = true,
  )

  @Test
  fun `Elvis operator mixed with plus operator breaking on elvis`() = assertFormatted(
      """
      |/////////////////////////////////
      |fun f() {
      |  return option1()
      |      ?: option2() + option3()
      |      ?: option4() + option5()
      |}
      |"""
          .trimMargin(),
      deduceMaxWidth = true,
  )

  @Test
  fun `handle comments in the middle of calling chain`() = assertFormatted(
      """
      |///////////////////////////
      |fun f() {
      |  someObject
      |      .letsDoIt()
      |      // this is a comment
      |      .doItOnce()
      |      // this is a comment
      |      .doItTwice()
      |}
      |"""
          .trimMargin(),
      deduceMaxWidth = true,
  )

  @Test
  fun `handle reified types`() = assertFormatted(
      """
      |inline fun <reified T> foo(t: T) {
      |  println(t)
      |}
      |
      |inline fun <reified in T> foo2(t: T) {
      |  println(t)
      |}
      |"""
          .trimMargin(),
  )

  @Test
  fun `handle suspended types`() = assertFormatted(
      """
      |private val reader: suspend (Key) -> Output?
      |
      |private val delete: (suspend (Key) -> Unit)? = null
      |
      |inline fun <R> foo(noinline block: suspend () -> R): suspend () -> R
      |
      |inline fun <R> bar(noinline block: (suspend () -> R)?): (suspend () -> R)?
      |"""
          .trimMargin(),
  )

  @Test
  fun `handle simple enum classes`() = assertFormatted(
      """
      |enum class BetterBoolean {
      |  TRUE,
      |  FALSE,
      |  FILE_NOT_FOUND,
      |}
      |"""
          .trimMargin(),
  )

  @Test
  fun `handle enum class with functions`() = assertFormatted(
      """
      |enum class BetterBoolean {
      |  TRUE,
      |  FALSE,
      |  FILE_NOT_FOUND;
      |
      |  fun isGood(): Boolean {
      |    return true
      |  }
      |}
      |"""
          .trimMargin(),
  )

  @Test
  fun `handle enum with annotations`() = assertFormatted(
      """
      |enum class BetterBoolean {
      |  @True TRUE,
      |  @False @WhatIsTruth FALSE,
      |}
      |"""
          .trimMargin(),
  )

  @Test
  fun `handle enum constructor calls`() = assertFormatted(
      """
      |enum class BetterBoolean(val name: String, val value: Boolean = true) {
      |  TRUE("true"),
      |  FALSE("false", false),
      |}
      |"""
          .trimMargin(),
  )

  @Test
  fun `handle enum entries with body`() = assertFormatted(
      """
      |enum class Animal(canWalk: Boolean = true) {
      |  DOG {
      |    fun speak() = "woof"
      |  },
      |  FISH(false) {},
      |}
      |"""
          .trimMargin(),
  )

  @Test
  fun `handle empty enum`() = assertFormatted(
      """
      |enum class YTho {}
      |"""
          .trimMargin(),
  )

  @Test
  fun `expect enum class`() = assertFormatted(
      """
      |expect enum class ExpectedEnum
      |"""
          .trimMargin(),
  )

  @Test
  fun `enum without trailing comma`() = assertFormatted(
      """
      |enum class Highlander {
      |  ONE
      |}
      |"""
          .trimMargin(),
  )

  @Test
  fun `enum comma and semicolon`() {
    assertThatFormatting(
        """
        |enum class Highlander {
        |  ONE,;
        |}
        |"""
            .trimMargin(),
    )
        .isEqualTo(
            """
            |enum class Highlander {
            |  ONE,
            |}
            |"""
                .trimMargin(),
        )
  }

  @Test
  fun `empty enum with semicolons`() {
    assertThatFormatting(
        """
        |enum class Empty {
        |  ;
        |}
        |"""
            .trimMargin(),
    )
        .isEqualTo(
            """
            |enum class Empty {}
            |"""
                .trimMargin(),
        )

    assertThatFormatting(
        """
        |enum class Empty {
        |  ;
        |  ;
        |  ;
        |}
        |"""
            .trimMargin(),
    )
        .isEqualTo(
            """
            |enum class Empty {}
            |"""
                .trimMargin(),
        )
  }

  @Test
  fun `semicolon is placed on next line when there's a trailing comma in an enum declaration`() = assertFormatted(
      """
      |enum class Highlander {
      |  ONE,
      |  TWO,
      |  ;
      |
      |  fun f() {}
      |}
      |"""
          .trimMargin(),
  )

  @Test
  fun `semicolon is removed from empty enum`() {
    val code =
        """
        |enum class SingleSemi {
        |  ;
        |}
        |
        |enum class MultSemi {
        |  // a
        |  ;
        |  // b
        |  ;
        |  // c
        |  ;
        |}
        |"""
            .trimMargin()
    val expected =
        """
        |enum class SingleSemi {}
        |
        |enum class MultSemi {
        |  // a
        |
        |  // b
        |
        |  // c
        |
        |}
        |"""
            .trimMargin()
    assertThatFormatting(code).isEqualTo(expected)
  }

  @Test
  fun `semicolon management in enum with no entries but other members`() {
    val code =
        """
        |enum class Empty {
        |  ;
        |
        |  fun f() {}
        |  ;
        |  fun g() {}
        |}
        |"""
            .trimMargin()
    val expected =
        """
        |enum class Empty {
        |  ;
        |
        |  fun f() {}
        |
        |  fun g() {}
        |}
        |"""
            .trimMargin()
    assertThatFormatting(code).isEqualTo(expected)
  }

  @Test
  fun `handle varargs and spread operator`() = assertFormatted(
      """
      |fun foo(vararg args: String) {
      |  foo2(*args)
      |  foo3(options = *args)
      |}
      |"""
          .trimMargin(),
  )

  @Test
  fun `handle typealias`() = assertFormatted(
      """
      |//////////////////////////////////////////////
      |private typealias TextChangedListener =
      |    (string: String) -> Unit
      |
      |typealias PairPair<X, Y> = Pair<Pair<X, Y>, X>
      |
      |class Foo
      |"""
          .trimMargin(),
      deduceMaxWidth = true,
  )

  @Test
  fun `handle the 'dynamic' type`() = assertFormatted(
      """
      |fun x(): dynamic = "x"
      |
      |val dyn: dynamic = 1
      |"""
          .trimMargin(),
  )

  @Test
  fun `handle class expression with generics`() = assertFormatted(
      """
      |fun f() {
      |  println(Array<String>::class.java)
      |}
      |"""
          .trimMargin(),
  )

  @Test
  fun `ParseError contains correct line and column numbers`() {
    val code =
        """
        |// Foo
        |fun good() {
        |  //
        |}
        |
        |fn (
        |"""
            .trimMargin()
    try {
      Formatter.format(code)
      fail()
    } catch (e: ParseError) {
      assertThat(e.lineColumn.line).isEqualTo(6)
      assertThat(e.lineColumn.column).isEqualTo(0)
      assertThat(e.errorDescription).containsMatch("Expecting an (expression|argument)")
    }
  }

  @Test
  fun `fail() reports line+column number`() {
    val code =
        """
        |// Foo
        |fun good() {
        |  return@ 5
        |}
        |"""
            .trimMargin()
    try {
      Formatter.format(code)
      fail()
    } catch (e: ParseError) {
      assertThat(e.lineColumn.line).isEqualTo(2)
      assertThat(e.lineColumn.column).isEqualTo(8)
    }
  }

  @Test
  fun `annotations on class, fun, parameters and literals`() = assertFormatted(
      """
      |@Fancy
      |class Foo {
      |  @Fancy
      |  fun baz(@Fancy foo: Int): Int {
      |    return (@Fancy 1)
      |  }
      |}
      |"""
          .trimMargin(),
  )

  @Test
  fun `annotations on function types`() = assertFormatted(
      """
      |fun foo(bar: @StringRes Int) {}
      |
      |fun foo(error: @Composable ((x) -> Unit)) {}
      |
      |fun foo(error: (@Composable (x) -> Unit)) {}
      |
      |fun foo(
      |    error:
      |        @field:[Inject Named("WEB_VIEW")]
      |        ((x) -> Unit)
      |) {}
      |
      |fun foo(
      |    error:
      |        (@field:[Inject Named("WEB_VIEW")]
      |        (x) -> Unit)
      |) {}
      |"""
          .trimMargin(),
  )

  @Test
  fun `handle annotations with use-site targets`() = assertFormatted(
      """
      |class FooTest {
      |  @get:Rule val exceptionRule: ExpectedException = ExpectedException.none()
      |
      |  @set:Magic(name = "Jane") var field: String
      |}
      |"""
          .trimMargin(),
  )

  @Test
  fun `handle annotations mixed with keywords since we cannot reorder them for now`() = assertFormatted(
      """
      |public @Magic final class Foo
      |
      |public @Magic(1) final class Foo
      |
      |@Magic(1) public final class Foo
      |"""
          .trimMargin(),
  )

  @Test
  fun `handle annotations more`() = assertFormatted(
      """
      |/////////////////////////////////////////////////
      |@Anno1
      |@Anno2(param = Param1::class)
      |@Anno3
      |@Anno4(param = Param2::class)
      |class MyClass {}
      |
      |fun f() {
      |  @Suppress("MagicNumber") add(10)
      |
      |  @Annotation // test a comment after annotations
      |  return 5
      |}
      |"""
          .trimMargin(),
      deduceMaxWidth = true,
  )

  @Test
  fun `annotated expressions`() = assertFormatted(
      """
      |////////////////////////////////////////////////
      |fun f() {
      |  @Suppress("MagicNumber") add(10) && add(20)
      |
      |  @Suppress("MagicNumber")
      |  add(10) && add(20)
      |
      |  @Anno1 @Anno2(param = Param1::class)
      |  add(10) && add(20)
      |
      |  @Anno1
      |  @Anno2(param = Param1::class)
      |  @Anno3
      |  @Anno4(param = Param2::class)
      |  add(10) && add(20)
      |
      |  @Anno1
      |  @Anno2(param = Param1::class)
      |  @Anno3
      |  @Anno4(param = Param2::class)
      |  add(10) && add(20)
      |
      |  @Suppress("MagicNumber") add(10) &&
      |      add(20) &&
      |      add(30)
      |
      |  add(@Suppress("MagicNumber") 10) &&
      |      add(20) &&
      |      add(30)
      |}
      |"""
          .trimMargin(),
      deduceMaxWidth = true,
  )

  @Test
  fun `annotated function declarations`() = assertFormatted(
      """
      |@Anno
      |fun f() {
      |  add(10)
      |}
      |
      |@Anno(param = 1)
      |fun f() {
      |  add(10)
      |}
      |"""
          .trimMargin(),
  )

  @Test
  fun `annotated class declarations`() = assertFormatted(
      """
      |@Anno class F
      |
      |@Anno(param = 1) class F
      |
      |@Anno(P)
      |// Foo
      |@Anno("param")
      |class F
      |"""
          .trimMargin(),
  )

  @Test
  fun `handle type arguments in annotations`() = assertFormatted(
      """
      |@TypeParceler<UUID, UUIDParceler>() class MyClass {}
      |"""
          .trimMargin(),
  )

  @Test
  fun `handle one line KDoc`() = assertFormatted(
      """
      |/** Hi, I am a one line kdoc */
      |class MyClass {}
      |"""
          .trimMargin(),
  )

  @Test
  fun `handle KDoc with Link`() = assertFormatted(
      """
      |/** This links to [AnotherClass] */
      |class MyClass {}
      |"""
          .trimMargin(),
  )

  @Test
  fun `handle KDoc with paragraphs`() = assertFormatted(
      """
      |/**
      | * Hi, I am a two paragraphs kdoc
      | *
      | * There's a space line to preserve between them
      | */
      |class MyClass {}
      |"""
          .trimMargin(),
  )

  @Test
  fun `handle KDoc with blocks`() = assertFormatted(
      """
      |/**
      | * Hi, I am a two paragraphs kdoc
      | *
      | * @param param1 this is param1
      | * @param[param2] this is param2
      | */
      |class MyClass {}
      |"""
          .trimMargin(),
  )

  @Test
  fun `handle KDoc with code examples`() = assertFormatted(
      """
      |/**
      | * This is how you write a simple hello world in Kotlin:
      | * ```
      | * fun main(args: Array<String>) {
      | *   println("Hello World!")
      | * }
      | * ```
      | *
      | * Amazing ah?
      | *
      | * ```
      | * fun `code can be with a blank line above it` () {}
      | * ```
      | *
      | * Or after it!
      | */
      |class MyClass {}
      |"""
          .trimMargin(),
  )

  @Test
  fun `handle KDoc with tagged code examples`() = assertFormatted(
      """
      |/**
      | * ```kotlin
      | * fun main(args: Array<String>) {
      | *   println("Hello World!")
      | * }
      | * ```
      | */
      |class MyClass {}
      |"""
          .trimMargin(),
  )

  @Test
  fun `handle stray code markers in lines and produce stable output`() {
    val code =
        """
        |/**
        | * Look! code: ``` aaa
        | * fun f() = Unit
        | * foo
        | * ```
        | */
        |class MyClass {}
        |"""
            .trimMargin()
    assertFormatted(Formatter.format(code))
  }

  @Test
  fun `code block with triple backtick`() {
    val code =
        """
        |/**
        | * Look! code:
        | * ```
        | * aaa ``` wow
        | * ```
        | */
        |class MyClass {}
        |"""
            .trimMargin()
    val expected =
        """
        |/**
        | * Look! code:
        | * ```
        | * aaa ``` wow
        | * ```
        | */
        |class MyClass {}
        |"""
            .trimMargin()
    assertThatFormatting(code).isEqualTo(expected)
  }

  @Test
  fun `when code closer in mid of line, produce stable output`() {
    val code =
        """
        |/**
        | * Look! code: ``` aaa
        | * fun f() = Unit
        | * foo ``` wow
        | */
        |class MyClass {}
        |"""
            .trimMargin()
    assertFormatted(Formatter.format(code))
  }

  @Test
  fun `handle KDoc with link reference`() = assertFormatted(
      """
      |/** Doc line with a reference to [AnotherClass] in the middle of a sentence */
      |class MyClass {}
      |"""
          .trimMargin(),
  )

  @Test
  fun `handle KDoc with links one after another`() = assertFormatted(
      """
      |/** Here are some links [AnotherClass] [AnotherClass2] */
      |class MyClass {}
      |"""
          .trimMargin(),
  )

  @Test
  fun `don't add spaces after links in Kdoc`() = assertFormatted(
      """
      |/** Here are some links [AnotherClass][AnotherClass2]hello */
      |class MyClass {}
      |"""
          .trimMargin(),
  )

  @Test
  fun `don't remove spaces after links in Kdoc`() = assertFormatted(
      """
      |/** Please see [onNext] (which has more details) */
      |class MyClass {}
      |"""
          .trimMargin(),
  )

  @Test
  fun `link anchor in KDoc are preserved`() = assertFormatted(
      """
      |/** [link anchor](the URL for the link anchor goes here) */
      |class MyClass {}
      |"""
          .trimMargin(),
  )

  @Test
  fun `don't add spaces between links in KDoc (because they're actually references)`() = assertFormatted(
      """
      |/** Here are some links [AnotherClass][AnotherClass2] */
      |class MyClass {}
      |
      |/** The final produced value may have [size][ByteString.size] < [bufferSize]. */
      |class MyClass {}
      |"""
          .trimMargin(),
  )

  @Test
  fun `collapse spaces after links in KDoc`() {
    val code =
        """
        |/** Here are some links [Class1], [Class2]   [Class3]. hello */
        |class MyClass {}
        |"""
            .trimMargin()
    val expected =
        """
        |/** Here are some links [Class1], [Class2] [Class3]. hello */
        |class MyClass {}
        |"""
            .trimMargin()
    assertThatFormatting(code).isEqualTo(expected)
  }

  @Test
  fun `collapse newlines after links in KDoc`() {
    val code =
        """
        |/**
        | * Here are some links [Class1]
        | * [Class2]
        | */
        |class MyClass {}
        |"""
            .trimMargin()
    val expected =
        """
        |/** Here are some links [Class1] [Class2] */
        |class MyClass {}
        |"""
            .trimMargin()
    assertThatFormatting(code).isEqualTo(expected)
  }

  @Test
  fun `do not crash because of malformed KDocs and produce stable output`() {
    val code =
        """
        |/** Surprise ``` */
        |class MyClass {}
        |"""
            .trimMargin()
    assertFormatted(Formatter.format(code))
  }

  @Test
  fun `Respect spacing of text after link`() = assertFormatted(
      """
      |/** Enjoy this link [linkstuff]. */
      |class MyClass {}
      |
      |/** There are many [FooObject]s. */
      |class MyClass {}
      |"""
          .trimMargin(),
  )

  @Test
  fun `handle KDoc with multiple separated param tags, breaking and merging lines and missing asterisk`() {
    val code =
        """
        |/**
        | * Trims leading whitespace characters followed by [marginPrefix] from every line of a source string and removes
        | * the first and the last lines if they are blank (notice difference blank vs empty).
        |
        | * Doesn't affect a line if it doesn't contain [marginPrefix] except the first and the last blank lines.
        | *
        | * Doesn't preserve the original line endings.
        | *
        | * @param marginPrefix non-blank string, which is used as a margin delimiter. Default is `|` (pipe character).
        | *
        | * @sample samples.text.Strings.trimMargin
        | * @see trimIndent
        | * @see kotlin.text.isWhitespace
        | */
        |class ThisWasCopiedFromTheTrimMarginMethod {}
        |"""
            .trimMargin()
    val expected =
        """
        |/**
        | * Trims leading whitespace characters followed by [marginPrefix] from every line of a source string
        | * and removes the first and the last lines if they are blank (notice difference blank vs empty).
        | *
        | * Doesn't affect a line if it doesn't contain [marginPrefix] except the first and the last blank
        | * lines.
        | *
        | * Doesn't preserve the original line endings.
        | *
        | * @param marginPrefix non-blank string, which is used as a margin delimiter. Default is `|` (pipe
        | *   character).
        | * @sample samples.text.Strings.trimMargin
        | * @see trimIndent
        | * @see kotlin.text.isWhitespace
        | */
        |class ThisWasCopiedFromTheTrimMarginMethod {}
        |"""
            .trimMargin()
    assertThatFormatting(code).isEqualTo(expected)
  }

  @Test
  fun `KDoc is reflowed`() {
    val code =
        """
        |/** Lorem ipsum dolor sit amet, consectetur */
        |class MyClass {}
        |"""
            .trimMargin()
    val expected =
        """
        |/**
        | * Lorem ipsum dolor sit amet,
        | * consectetur
        | */
        |class MyClass {}
        |"""
            .trimMargin()
    assertThatFormatting(code)
        .withOptions(defaultTestFormattingOptions.copy(maxWidth = 33))
        .isEqualTo(expected)
  }

  @Test
  fun `sanity - block and continuation indents are 4`() {
    val code =
        """
        |fun f() {
        |    for (child in
        |        node.next.next.next.next
        |            .data()) {
        |        println(child)
        |    }
        |}
        |"""
            .trimMargin()
    assertThatFormatting(code)
        .withOptions(
            FormattingOptions(
                maxWidth = 35,
                blockIndent = 4,
                continuationIndent = 4,
                trailingCommaManagementStrategy = TrailingCommaManagementStrategy.NONE,
            ),
        )
        .isEqualTo(code)
  }

  @Test
  fun `comment after a block is stable and does not add space lines`() = assertFormatted(
      """
      |fun doIt() {}
      |
      |/* this is the first comment */
      |"""
          .trimMargin(),
  )

  @Test
  fun `preserve LF, CRLF and CR line endings`() {
    val lines = listOf("fun main() {", "  println(\"test\")", "}")
    for (ending in listOf("\n", "\r\n", "\r")) {
      val code = lines.joinToString(ending) + ending
      assertThatFormatting(code).isEqualTo(code)
    }
  }

  @Test
  fun `handle trailing commas (constructors)`() = assertFormatted(
      """
      |////////////////////
      |class Foo(
      |    a: Int,
      |)
      |
      |class Foo(
      |    a: Int,
      |    b: Int,
      |)
      |
      |class Foo(
      |    a: Int,
      |    b: Int
      |)
      |"""
          .trimMargin(),
      deduceMaxWidth = true,
  )

  @Test
  fun `handle trailing commas (explicit constructors)`() = assertFormatted(
      """
      |////////////////////////
      |class Foo
      |constructor(
      |    a: Int,
      |)
      |
      |class Foo
      |constructor(
      |    a: Int,
      |    b: Int,
      |)
      |
      |class Foo
      |constructor(
      |    a: Int,
      |    b: Int
      |)
      |"""
          .trimMargin(),
      deduceMaxWidth = true,
  )

  @Test
  fun `handle trailing commas (secondary constructors)`() = assertFormatted(
      """
      |////////////////////////
      |class Foo {
      |  constructor(
      |      a: Int,
      |  )
      |}
      |
      |class Foo {
      |  constructor(
      |      a: Int,
      |      b: Int,
      |  )
      |}
      |
      |class Foo {
      |  constructor(
      |      a: Int,
      |      b: Int
      |  )
      |}
      |"""
          .trimMargin(),
      deduceMaxWidth = true,
  )

  @Test
  fun `handle trailing commas (function definitions)`() = assertFormatted(
      """
      |////////////////////////
      |fun <
      |    T,
      |> foo() {}
      |
      |fun <
      |    T,
      |    S,
      |> foo() {}
      |
      |fun foo(
      |    a: Int,
      |) {}
      |
      |fun foo(
      |    a: Int,
      |    b: Int
      |) {}
      |
      |fun foo(
      |    a: Int,
      |    b: Int,
      |) {}
      |
      |fun foo(
      |    a: Int,
      |    b: Int,
      |    c: Int,
      |) {}
      |"""
          .trimMargin(),
      deduceMaxWidth = true,
  )

  @Test
  fun `handle trailing commas (function calls)`() = assertFormatted(
      """
      |////////////////////////
      |fun main() {
      |  foo(
      |      3,
      |  )
      |
      |  foo<Int>(
      |      3,
      |  )
      |
      |  foo<
      |      Int,
      |  >(
      |      3,
      |  )
      |
      |  foo<Int>(
      |      "asdf", "asdf")
      |
      |  foo<
      |      Int,
      |  >(
      |      "asd",
      |      "asd",
      |  )
      |
      |  foo<
      |      Int,
      |      Boolean,
      |  >(
      |      3,
      |  )
      |}
      |"""
          .trimMargin(),
      deduceMaxWidth = true,
  )

  @Test
  fun `handle trailing commas (proprties)`() = assertFormatted(
      """
      |//////////////////////////
      |val foo: String
      |  set(
      |      value,
      |  ) {}
      |"""
          .trimMargin(),
      deduceMaxWidth = true,
  )

  @Test
  fun `handle trailing commas (higher-order functions)`() = assertFormatted(
      """
      |//////////////////////////
      |fun foo(
      |    x:
      |        (
      |            Int,
      |        ) -> Unit
      |) {}
      |"""
          .trimMargin(),
      deduceMaxWidth = true,
  )

  @Test
  fun `handle trailing commas (after lambda arg)`() = assertFormatted(
      """
      |//////////////////////////
      |fun foo() {
      |  foo(
      |      { it },
      |  )
      |}
      |"""
          .trimMargin(),
      deduceMaxWidth = true,
  )

  @Test
  fun `handle trailing commas (other)`() = assertFormatted(
      """
      |//////////////////////////
      |fun main() {
      |  val (
      |      x: Int,
      |  ) = foo()
      |  val (
      |      x: Int,
      |      y: Int,
      |  ) = foo()
      |
      |  val (
      |      x: Int,
      |  ) = foo(
      |      blablablablFoobar,
      |      alskdjasld)
      |
      |  val (
      |      x: Int,
      |      y: Int,
      |  ) = foo(
      |      blablablablFoobar,
      |      asldkalsd)
      |
      |  a[
      |      0,
      |  ] = 43
      |  a[
      |      0,
      |      1,
      |  ] = 43
      |
      |  [
      |      0,
      |  ]
      |  [
      |      0,
      |      1,
      |  ]
      |
      |  when (foo) {
      |    'x',
      |    -> 43
      |    'x',
      |    'y',
      |    -> 43
      |    'x',
      |    'y',
      |    'z',
      |    'w',
      |    'a',
      |    'b',
      |    -> 43
      |  }
      |
      |  try {
      |    //
      |  } catch (e: Error,) {
      |    //
      |  }
      |}
      |"""
          .trimMargin(),
      deduceMaxWidth = true,
  )

  @Test
  fun `assignment of a scoping function`() = assertFormatted(
      """
      |////////////////////////////
      |val foo = coroutineScope {
      |  foo()
      |  //
      |}
      |
      |fun foo() = coroutineScope {
      |  foo()
      |  //
      |}
      |
      |fun foo() = use { x ->
      |  foo()
      |  //
      |}
      |
      |fun foo() = scope label@{
      |  foo()
      |  //
      |}
      |
      |fun foo() =
      |    coroutineScope { x ->
      |      foo()
      |      //
      |    }
      |
      |fun foo() =
      |    coroutineScope label@{
      |      foo()
      |      //
      |    }
      |
      |fun foo() =
      |    Runnable @Px {
      |      foo()
      |      //
      |    }
      |
      |fun longName() =
      |    coroutineScope {
      |      foo()
      |      //
      |    }
      |"""
          .trimMargin(),
      deduceMaxWidth = true,
  )

  @Test
  fun `dot-qualified scoping functions are block-like`() = assertFormatted(
      """
      |/////////////////////////////////
      |fun f() {
      |  val fn = scope.launch {
      |    doThing()
      |    doAnother()
      |  }
      |}
      |
      |fun g() {
      |  val longVariableName =
      |      scope.launch {
      |        doThing()
      |        doAnother()
      |      }
      |}
      |
      |fun h() = scope.launch {
      |  doThing()
      |  doAnother()
      |}
      |
      |fun longFunctionName() =
      |    scope.launch {
      |      doThing()
      |      doAnother()
      |    }
      |
      |fun j() {
      |  x = scope.launch {
      |    doThing()
      |    doAnother()
      |  }
      |}
      |
      |fun k() {
      |  longVariableName =
      |      scope.launch {
      |        doThing()
      |        doAnother()
      |      }
      |}
      |
      |fun l() {
      |  val fn = scope?.launch {
      |    doThing()
      |    doAnother()
      |  }
      |}
      |
      |fun m() {
      |  val longVariableName =
      |      scope?.launch {
      |        doThing()
      |        doAnother()
      |      }
      |}
      |"""
          .trimMargin(),
      deduceMaxWidth = true,
  )

  @Test
  fun `dot-qualified scoping functions single-line`() = assertFormatted(
      """
      |///////////////////////////////////////////
      |fun f() {
      |  val fn = scope.launch { doThing() }
      |}
      |"""
          .trimMargin(),
      deduceMaxWidth = true,
  )

  @Test
  fun `top level properties with other types preserve newline spacing`() {
    assertFormatted(
        """
        |/////////////////////////////////////////
        |fun something() {
        |  println("hi")
        |}
        |
        |const val SOME_CONST = 1
        |val SOME_STR = "hi"
        |// Single comment
        |val SOME_INT = 1
        |
        |// Intentional space above single comment
        |val SOME_INT2 = 1
        |
        |val FOO = 2
        |const val BAR = 3
        |
        |fun baz() = 1
        |
        |val d = 1
        |
        |class Bar {}
        |
        |val e = 1
        |/** Doc block */
        |val f = 1
        |
        |/** Intent. space above doc */
        |val g = 1
        |
        |data class Qux(val foo: String)
        |"""
            .trimMargin(),
        deduceMaxWidth = true,
    )

    assertThatFormatting(
        """
        |import com.example.foo
        |import com.example.bar
        |const val SOME_CONST = foo.a
        |val SOME_STR = bar.a
        |"""
            .trimMargin(),
    )
        .isEqualTo(
            """
            |import com.example.bar
            |import com.example.foo
            |
            |const val SOME_CONST = foo.a
            |val SOME_STR = bar.a
            |"""
                .trimMargin(),
        )
  }

  @Test
  fun `first line is never empty`() = assertThatFormatting(
      """
      |
      |fun f() {}
      |"""
          .trimMargin(),
  )
      .isEqualTo(
          """
          |fun f() {}
          |"""
              .trimMargin(),
      )

  @Test
  fun `at most one newline between any adjacent top-level elements`() = assertThatFormatting(
      """
      |import com.Bar
      |
      |
      |import com.Foo
      |
      |
      |fun f() {}
      |
      |
      |fun f() {}
      |
      |
      |class C {}
      |
      |
      |class C {}
      |
      |
      |val x = Foo()
      |
      |
      |val x = Bar()
      |"""
          .trimMargin(),
  )
      .isEqualTo(
          """
          |import com.Bar
          |import com.Foo
          |
          |fun f() {}
          |
          |fun f() {}
          |
          |class C {}
          |
          |class C {}
          |
          |val x = Foo()
          |
          |val x = Bar()
          |"""
              .trimMargin(),
      )

  @Test
  fun `at least one newline between any adjacent top-level elements, unless it's a property`() = assertThatFormatting(
      """
      |import com.Bar
      |import com.Foo
      |fun f() {}
      |fun f() {}
      |class C {}
      |class C {}
      |val x = Foo()
      |val x = Bar()
      |"""
          .trimMargin(),
  )
      .isEqualTo(
          """
          |import com.Bar
          |import com.Foo
          |
          |fun f() {}
          |
          |fun f() {}
          |
          |class C {}
          |
          |class C {}
          |
          |val x = Foo()
          |val x = Bar()
          |"""
              .trimMargin(),
      )

  @Test
  fun `handle array of annotations with field prefix`() {
    val code: String =
        """
        |class MyClass {
        |  @field:[JvmStatic Volatile]
        |  var myVar: String? = null
        |}
        |"""
            .trimMargin()
    assertThatFormatting(code).isEqualTo(code)
  }

  @Test
  fun `handle array of annotations without field prefix`() {
    val code: String =
        """
        |class MyClass {
        |  @[JvmStatic Volatile]
        |  var myVar: String? = null
        |}
        |"""
            .trimMargin()
    assertThatFormatting(code).isEqualTo(code)
  }

  // Regression test against https://github.com/facebook/ktfmt/issues/243
  @Test
  fun `regression test against Issue 243`() {
    val code =
        """
        |class Foo {
        |  companion object {
        |    var instance: Foo? = null
        |
        |    fun getInstance() {
        |      return instance ?: synchronized(Foo::class) {
        |        Foo().also { instance = it }
        |      }
        |    }
        |  }
        |}
        |"""
            .trimMargin()

    // Don't throw.
    Formatter.format(code)
  }

  // Regression test against https://github.com/facebook/ktfmt/issues/557
  @Test
  fun `empty companion object`() {
    assertFormatted(
        """
        |class Foo {
        |  val a: String
        |
        |  companion object;
        |
        |  init {
        |    a = "Hello"
        |  }
        |}
        |"""
            .trimMargin(),
    )
  }

  // Regression test against https://github.com/facebook/ktfmt/issues/557
  @Test
  fun `empty companion object with nothing after`() {
    val code =
        """
        |class Foo {
        |  companion object;
        |}
        |"""
            .trimMargin()

    val expected =
        """
        |class Foo {
        |  companion object
        |}
        |"""
            .trimMargin()

    assertThatFormatting(code).isEqualTo(expected)
  }

  @Test
  fun `lambda with required arrow`() = assertFormatted(
      """
      |val a = { x: Int -> }
      |val b = { x: Int -> 0 }
      |val c = { x: Int ->
      |  val y = 0
      |  y
      |}
      |"""
          .trimMargin(),
  )

  @Test
  fun `lambda with optional arrow`() = assertFormatted(
      """
      |val a = { -> }
      |val b = { -> 0 }
      |val c = { ->
      |  val y = 0
      |  y
      |}
      |"""
          .trimMargin(),
  )

  @Test
  fun `lambda missing optional arrow`() = assertFormatted(
      """
      |val a = {}
      |val b = { 0 }
      |val c = {
      |  val y = 0
      |  y
      |}
      |"""
          .trimMargin(),
  )

  @Test
  fun `lambda with only comments`() {
    assertFormatted(
        """
        |val a = { /* do nothing */ }
        |val b = {
        |  /* do nothing */
        |  /* also do nothing */
        |}
        |val c = { -> /* do nothing */ }
        |val d = { _ -> /* do nothing */ }
        |private val e = Runnable {
        |  // do nothing
        |}
        |private val f: () -> Unit = {
        |  // no-op
        |}
        |private val g: () -> Unit = { /* no-op */ }
        |"""
            .trimMargin(),
    )

    assertFormatted(
        """
        |//////////////////////////////
        |val a = { /* do nothing */ }
        |val b = {
        |  /* do nothing */
        |  /* also do nothing */
        |}
        |val c = { ->
        |  /* do nothing */
        |}
        |val d = { _ ->
        |  /* do nothing */
        |}
        |private val e = Runnable {
        |  // do nothing
        |}
        |private val f: () -> Unit = {
        |  // no-op
        |}
        |private val g: () -> Unit = {
        |  /* no-op */
        |}
        |"""
            .trimMargin(),
        deduceMaxWidth = true,
    )
  }

  @Test
  fun `lambda block with single and multiple statements`() = assertFormatted(
      """
      |private val a = Runnable {
      |  foo()
      |  TODO("implement me")
      |}
      |
      |private val b = Runnable { TODO("implement me") }
      |
      |private val c: () -> Unit = {
      |  foo()
      |  TODO("implement me")
      |}
      |
      |private val d: () -> Unit = { TODO("implement me") }
      |"""
          .trimMargin(),
  )

  @Test
  fun `lambda block with comments and statements mix`() = assertFormatted(
      """
      |private val a = Runnable {
      |  // no-op
      |  TODO("implement me")
      |}
      |
      |private val b = Runnable {
      |  TODO("implement me")
      |  // no-op
      |}
      |
      |private val c: () -> Unit = {
      |  /* no-op */ TODO("implement me")
      |}
      |
      |private val d: () -> Unit = { ->
      |  /* no-op */ TODO("implement me")
      |}
      |
      |private val e: (String, Int) -> Unit = { _, i -> foo(i) /* do nothing ... */ }
      |"""
          .trimMargin(),
  )

  @Test
  fun `lambda block with comments and with statements have same formatting treatment`() = assertFormatted(
      """
      |private val a = Runnable { /* no-op */ }
      |private val A = Runnable { TODO("...") }
      |
      |private val b = Runnable {
      |  /* no-op 1 */
      |  /* no-op 2 */
      |}
      |private val B = Runnable {
      |  TODO("no-op")
      |  TODO("no-op")
      |}
      |
      |private val c: () -> Unit = { /* no-op */ }
      |private val C: () -> Unit = { TODO("...") }
      |
      |private val d: () -> Unit = {
      |  /*.*/
      |  /* do nothing ... */
      |}
      |private val D: () -> Unit = {
      |  foo()
      |  TODO("implement me")
      |}
      |"""
          .trimMargin(),
  )

  @Test
  fun `last parameter with comment and with statements have same formatting treatment`() {
    assertFormatted(
        """
        |private val a =
        |    call(param) {
        |      // no-op
        |      /* comment */
        |    }
        |private val A =
        |    call(param) {
        |      a.run()
        |      TODO("implement me")
        |    }
        |
        |private val b = call(param) { /* no-op */ }
        |private val B = call(param) { TODO("implement me") }
        |
        |private val c = firstCall().prop.call(param) { /* no-op */ }
        |private val C = firstCall().prop.call(param) { TODO("implement me") }
        |"""
            .trimMargin(),
    )

    assertFormatted(
        """
        |////////////////////////////////////////
        |private val a =
        |    firstCall().prop.call(
        |        mySuperInterestingParameter) {
        |          /* no-op */
        |        }
        |private val A =
        |    firstCall().prop.call(
        |        mySuperInterestingParameter) {
        |          TODO("...")
        |        }
        |
        |fun b() {
        |  myProp.funCall(param) { /* 12345 */ }
        |  myProp.funCall(param) { TODO("123") }
        |
        |  myProp.funCall(param) { /* 123456 */ }
        |  myProp.funCall(param) { TODO("1234") }
        |
        |  myProp.funCall(param) {
        |    /* 1234567 */
        |  }
        |  myProp.funCall(param) {
        |    TODO("12345")
        |  }
        |
        |  myProp.funCall(param) {
        |    /* 12345678 */
        |  }
        |  myProp.funCall(param) {
        |    TODO("123456")
        |  }
        |
        |  myProp.funCall(param) {
        |    /* 123456789 */
        |  }
        |  myProp.funCall(param) {
        |    TODO("1234567")
        |  }
        |
        |  myProp.funCall(param) {
        |    /* very_very_long_comment_that_should_go_on_its_own_line */
        |  }
        |  myProp.funCall(param) {
        |    TODO(
        |        "_a_very_long_comment_that_should_go_on_its_own_line")
        |  }
        |}
        |
        |private val c =
        |    firstCall().prop.call(param) {
        |      /* no-op */
        |    }
        |private val C =
        |    firstCall().prop.call(param) {
        |      TODO("...")
        |    }
        |"""
            .trimMargin(),
        deduceMaxWidth = true,
    )
  }

  @Test
  fun `chaining - many dereferences`() = assertFormatted(
      """
      |/////////////////////////
      |rainbow.red.orange.yellow
      |    .green
      |    .blue
      |    .indigo
      |    .violet
      |    .cyan
      |    .magenta
      |    .key
      |"""
          .trimMargin(),
      deduceMaxWidth = true,
  )

  @Test
  fun `chaining - many dereferences, fit on one line`() = assertFormatted(
      """
      |///////////////////////////////////////////////////////////////////////////
      |rainbow.red.orange.yellow.green.blue.indigo.violet.cyan.magenta.key
      |"""
          .trimMargin(),
      deduceMaxWidth = true,
  )

  @Test
  fun `chaining - many dereferences, one invocation at end`() = assertFormatted(
      """
      |/////////////////////////
      |rainbow.red.orange.yellow
      |    .green
      |    .blue
      |    .indigo
      |    .violet
      |    .cyan
      |    .magenta
      |    .key
      |    .build()
      |"""
          .trimMargin(),
      deduceMaxWidth = true,
  )

  @Test
  fun `chaining - many dereferences, one invocation at end, fit on one line`() = assertFormatted(
      """
      |///////////////////////////////////////////////////////////////////////////
      |rainbow.red.orange.yellow.green.blue.indigo.violet.cyan.magenta.key.build()
      |"""
          .trimMargin(),
      deduceMaxWidth = true,
  )

  @Test
  fun `chaining - many dereferences, two invocations at end`() = assertFormatted(
      """
      |/////////////////////////
      |rainbow.red.orange.yellow
      |    .green
      |    .blue
      |    .indigo
      |    .violet
      |    .cyan
      |    .magenta
      |    .key
      |    .build()
      |    .shine()
      |"""
          .trimMargin(),
      deduceMaxWidth = true,
  )

  @Test
  fun `chaining - many dereferences, one invocation in the middle`() = assertFormatted(
      """
      |/////////////////////////
      |rainbow.red.orange.yellow
      |    .green
      |    .blue
      |    .shine()
      |    .indigo
      |    .violet
      |    .cyan
      |    .magenta
      |    .key
      |"""
          .trimMargin(),
      deduceMaxWidth = true,
  )

  @Test
  fun `chaining - many dereferences, two invocations in the middle`() = assertFormatted(
      """
      |/////////////////////////
      |rainbow.red.orange.yellow
      |    .green
      |    .blue
      |    .indigo
      |    .shine()
      |    .bright()
      |    .violet
      |    .cyan
      |    .magenta
      |    .key
      |"""
          .trimMargin(),
      deduceMaxWidth = true,
  )

  @Test
  fun `chaining - many dereferences, one lambda at end`() = assertFormatted(
      """
      |/////////////////////////
      |rainbow.red.orange.yellow
      |    .green
      |    .blue
      |    .indigo
      |    .violet
      |    .cyan
      |    .magenta
      |    .key
      |    .build { it.appear }
      |"""
          .trimMargin(),
      deduceMaxWidth = true,
  )

  @Test
  fun `chaining - many dereferences, one short lambda at end`() = assertFormatted(
      """
      |/////////////////////////
      |rainbow.red.orange.yellow
      |    .green
      |    .blue
      |    .indigo
      |    .violet
      |    .cyan
      |    .magenta
      |    .key
      |    .z { it }
      |"""
          .trimMargin(),
      deduceMaxWidth = true,
  )

  @Test
  fun `chaining - many dereferences, one multiline lambda at end`() = assertFormatted(
      """
      |/////////////////////////
      |rainbow.red.orange.yellow
      |    .green
      |    .blue
      |    .indigo
      |    .violet
      |    .cyan
      |    .magenta
      |    .key
      |    .z {
      |      it
      |      it
      |    }
      |"""
          .trimMargin(),
      deduceMaxWidth = true,
  )

  @Test
  fun `chaining - many dereferences, one short lambda in the middle`() = assertFormatted(
      """
      |/////////////////////////
      |rainbow.red.orange.yellow
      |    .green
      |    .blue
      |    .z { it }
      |    .indigo
      |    .violet
      |    .cyan
      |    .magenta
      |    .key
      |"""
          .trimMargin(),
      deduceMaxWidth = true,
  )

  @Test
  fun `chaining - many dereferences, one multiline lambda in the middle`() = assertFormatted(
      """
      |/////////////////////////
      |rainbow.red.orange.yellow
      |    .green
      |    .blue
      |    .z {
      |      it
      |      it
      |    }
      |    .indigo
      |    .violet
      |    .cyan
      |    .magenta
      |    .key
      |"""
          .trimMargin(),
      deduceMaxWidth = true,
  )

  @Test
  fun `chaining - many dereferences, one multiline lambda in the middle, remainder could fit on one line`() = assertFormatted(
      """
      |/////////////////////////////////////////////////////////////////////////////////////////
      |rainbow.red.orange.yellow.green.blue
      |    .z {
      |      it
      |      it
      |    }
      |    .indigo
      |    .violet
      |    .cyan
      |    .magenta
      |    .key
      |"""
          .trimMargin(),
      deduceMaxWidth = true,
  )

  @Test
  fun `chaining - many dereferences, one multiline lambda and two invocations in the middle, remainder could fit on one line`() = assertFormatted(
      """
      |/////////////////////////////////////////////////////////////////////////////////////////
      |rainbow.red.orange.yellow.green.blue
      |    .z {
      |      it
      |      it
      |    }
      |    .shine()
      |    .bright()
      |    .indigo
      |    .violet
      |    .cyan
      |    .magenta
      |    .key
      |"""
          .trimMargin(),
      deduceMaxWidth = true,
  )

  @Test
  fun `chaining - many dereferences, one lambda and invocation at end`() = assertFormatted(
      """
      |/////////////////////////
      |rainbow.red.orange.yellow
      |    .green
      |    .blue
      |    .indigo
      |    .violet
      |    .cyan
      |    .magenta
      |    .key
      |    .z { it }
      |    .shine()
      |"""
          .trimMargin(),
      deduceMaxWidth = true,
  )

  @Test
  fun `chaining - many dereferences, one multiline lambda and invocation at end`() = assertFormatted(
      """
      |/////////////////////////
      |rainbow.red.orange.yellow
      |    .green
      |    .blue
      |    .indigo
      |    .violet
      |    .cyan
      |    .magenta
      |    .key
      |    .z {
      |      it
      |      it
      |    }
      |    .shine()
      |"""
          .trimMargin(),
      deduceMaxWidth = true,
  )

  @Test
  fun `chaining - many dereferences, one invocation and lambda at end`() = assertFormatted(
      """
      |/////////////////////////
      |rainbow.red.orange.yellow
      |    .green
      |    .blue
      |    .indigo
      |    .violet
      |    .cyan
      |    .magenta
      |    .key
      |    .shine()
      |    .z { it }
      |"""
          .trimMargin(),
      deduceMaxWidth = true,
  )

  @Test
  fun `chaining - many dereferences, one short lambda and invocation in the middle`() = assertFormatted(
      """
      |/////////////////////////
      |rainbow.red.orange.yellow
      |    .green
      |    .blue
      |    .indigo
      |    .z { it }
      |    .shine()
      |    .violet
      |    .cyan
      |    .magenta
      |    .key
      |"""
          .trimMargin(),
      deduceMaxWidth = true,
  )

  @Test
  fun `chaining - many dereferences, invocation and one short lambda in the middle`() = assertFormatted(
      """
      |/////////////////////////
      |rainbow.red.orange.yellow
      |    .green
      |    .blue
      |    .indigo
      |    .shine()
      |    .z { it }
      |    .violet
      |    .cyan
      |    .magenta
      |    .key
      |"""
          .trimMargin(),
      deduceMaxWidth = true,
  )

  @Test
  fun `chaining - many dereferences, starting with this`() = assertFormatted(
      """
      |/////////////////////////
      |this.red.orange.yellow
      |    .green
      |    .blue
      |    .indigo
      |    .violet
      |    .cyan
      |    .magenta
      |    .key
      |"""
          .trimMargin(),
      deduceMaxWidth = true,
  )

  @Test
  fun `chaining - many dereferences, starting with this, one invocation at end`() = assertFormatted(
      """
      |/////////////////////////
      |this.red.orange.yellow
      |    .green
      |    .blue
      |    .indigo
      |    .violet
      |    .cyan
      |    .magenta
      |    .key
      |    .build()
      |"""
          .trimMargin(),
      deduceMaxWidth = true,
  )

  @Test
  fun `chaining - many dereferences, starting with super`() = assertFormatted(
      """
      |/////////////////////////
      |super.red.orange.yellow
      |    .green
      |    .blue
      |    .indigo
      |    .violet
      |    .cyan
      |    .magenta
      |    .key
      |"""
          .trimMargin(),
      deduceMaxWidth = true,
  )

  @Test
  fun `chaining - many dereferences, starting with super, one invocation at end`() = assertFormatted(
      """
      |/////////////////////////
      |super.red.orange.yellow
      |    .green
      |    .blue
      |    .indigo
      |    .violet
      |    .cyan
      |    .magenta
      |    .key
      |    .build()
      |"""
          .trimMargin(),
      deduceMaxWidth = true,
  )

  @Test
  fun `chaining - many dereferences, starting with short variable`() = assertFormatted(
      """
      |/////////////////////////
      |z123.red.orange.yellow
      |    .green
      |    .blue
      |    .indigo
      |    .violet
      |    .cyan
      |    .magenta
      |    .key
      |"""
          .trimMargin(),
      deduceMaxWidth = true,
  )

  @Test
  fun `chaining - many dereferences, starting with short variable, one invocation at end`() = assertFormatted(
      """
      |/////////////////////////
      |z123.red.orange.yellow
      |    .green
      |    .blue
      |    .indigo
      |    .violet
      |    .cyan
      |    .magenta
      |    .key
      |    .build()
      |"""
          .trimMargin(),
      deduceMaxWidth = true,
  )

  @Test
  fun `chaining - many dereferences, starting with short variable and lambda, invocation at end`() = assertFormatted(
      """
      |/////////////////////////
      |z12.z { it }
      |    .red
      |    .orange
      |    .yellow
      |    .green
      |    .blue
      |    .indigo
      |    .violet
      |    .cyan
      |    .magenta
      |    .key
      |    .shine()
      |"""
          .trimMargin(),
      deduceMaxWidth = true,
  )

  @Test
  fun `chaining - many dereferences, starting with this and lambda, invocation at end`() = assertFormatted(
      """
      |/////////////////////////
      |this.z { it }
      |    .red
      |    .orange
      |    .yellow
      |    .green
      |    .blue
      |    .indigo
      |    .violet
      |    .cyan
      |    .magenta
      |    .key
      |    .shine()
      |"""
          .trimMargin(),
      deduceMaxWidth = true,
  )

  @Test
  fun `chaining - many invocations`() = assertFormatted(
      """
      |/////////////////////////
      |rainbow.a().b().c()
      |"""
          .trimMargin(),
      deduceMaxWidth = true,
  )

  @Test
  fun `chaining - many invocations, with multiline lambda at end`() = assertFormatted(
      """
      |/////////////////////////
      |rainbow.a().b().c().zz {
      |  it
      |  it
      |}
      |"""
          .trimMargin(),
      deduceMaxWidth = true,
  )

  @Test
  fun `chaining - many dereferences, starting type name`() = assertFormatted(
      """
      |/////////////////////////
      |com.sky.Rainbow.red
      |    .orange
      |    .yellow
      |    .green
      |    .blue
      |    .indigo
      |    .violet
      |    .cyan
      |    .magenta
      |    .key
      |"""
          .trimMargin(),
      deduceMaxWidth = true,
  )

  @Test
  fun `chaining - many invocations, starting with short variable, lambda at end`() = assertFormatted(
      """
      |/////////////
      |z12.shine()
      |    .bright()
      |    .z { it }
      |"""
          .trimMargin(),
      deduceMaxWidth = true,
  )

  @Test
  fun `chaining - start with invocation, lambda at end`() = assertFormatted(
      """
      |/////////////////////
      |getRainbow(
      |        aa, bb, cc)
      |    .z { it }
      |"""
          .trimMargin(),
      deduceMaxWidth = true,
  )

  @Test
  fun `chaining - many invocations, start with lambda`() = assertFormatted(
      """
      |/////////////////////
      |z { it }
      |    .shine()
      |    .bright()
      |"""
          .trimMargin(),
      deduceMaxWidth = true,
  )

  @Test
  fun `chaining - start with type name, end with invocation`() = assertFormatted(
      """
      |/////////////////////////
      |com.sky.Rainbow
      |    .colorFactory
      |    .build()
      |"""
          .trimMargin(),
      deduceMaxWidth = true,
  )

  @Test
  fun `chaining (indentation) - multiline lambda`() = assertFormatted(
      """
      |/////////////////////////
      |rainbow.z {
      |  it
      |  it
      |}
      |"""
          .trimMargin(),
      deduceMaxWidth = true,
  )

  @Test
  fun `chaining (indentation) - multiline lambda with trailing dereferences`() = assertFormatted(
      """
      |/////////////////////////
      |rainbow
      |    .z {
      |      it
      |      it
      |    }
      |    .red
      |"""
          .trimMargin(),
      deduceMaxWidth = true,
  )

  @Test
  fun `chaining (indentation) - multiline lambda with long name`() = assertFormatted(
      """
      |/////////////////////////
      |rainbow
      |    .someLongLambdaName {
      |      it
      |      it
      |    }
      |"""
          .trimMargin(),
      deduceMaxWidth = true,
  )

  @Test
  fun `chaining (indentation) - multiline lambda with long name and trailing dereferences`() = assertFormatted(
      """
      |/////////////////////////
      |rainbow
      |    .someLongLambdaName {
      |      it
      |      it
      |    }
      |    .red
      |"""
          .trimMargin(),
      deduceMaxWidth = true,
  )

  @Test
  fun `chaining (indentation) - multiline lambda with prefix`() = assertFormatted(
      """
      |/////////////////////////
      |rainbow.red.z {
      |  it
      |  it
      |}
      |"""
          .trimMargin(),
      deduceMaxWidth = true,
  )

  @Test
  fun `chaining (indentation) - multiline lambda with prefix, forced to next line`() = assertFormatted(
      """
      |/////////////////////////
      |rainbow.red.orange.yellow
      |    .longLambdaName {
      |      it
      |      it
      |    }
      |"""
          .trimMargin(),
      deduceMaxWidth = true,
  )

  @Test
  fun `chaining (indentation) - multiline lambda with prefix, forced to next line with another expression`() = assertFormatted(
      """
      |/////////////////////////
      |rainbow.red.orange.yellow
      |    .key
      |    .longLambdaName {
      |      it
      |      it
      |    }
      |"""
          .trimMargin(),
      deduceMaxWidth = true,
  )

  @Test
  fun `chaining (indentation) - multiline arguments`() = assertFormatted(
      """
      |/////////////////////////
      |rainbow.shine(
      |    infrared,
      |    ultraviolet,
      |)
      |"""
          .trimMargin(),
      deduceMaxWidth = true,
  )

  @Test
  fun `chaining (indentation) - multiline arguments with trailing dereferences`() = assertFormatted(
      """
      |/////////////////////////
      |rainbow
      |    .shine(
      |        infrared,
      |        ultraviolet,
      |    )
      |    .red
      |"""
          .trimMargin(),
      deduceMaxWidth = true,
  )

  @Test
  fun `chaining (indentation) - multiline arguments, forced to next line`() = assertFormatted(
      """
      |/////////////////////////
      |rainbow.red.orange.yellow
      |    .shine(
      |        infrared,
      |        ultraviolet,
      |    )
      |"""
          .trimMargin(),
      deduceMaxWidth = true,
  )

  @Test
  fun `chaining (indentation) - multiline arguments, forced to next line with another expression`() = assertFormatted(
      """
      |/////////////////////////
      |rainbow.red.orange.yellow
      |    .key
      |    .shine(
      |        infrared,
      |        ultraviolet,
      |    )
      |"""
          .trimMargin(),
      deduceMaxWidth = true,
  )

  @Test
  fun `chaining (indentation) - multiline arguments, forced to next line with another expression, with trailing dereferences`() = assertFormatted(
      """
      |/////////////////////////
      |rainbow.red.orange.yellow
      |    .key
      |    .shine(
      |        infrared,
      |        ultraviolet,
      |    )
      |    .red
      |"""
          .trimMargin(),
      deduceMaxWidth = true,
  )

  @Test
  fun `chaining (indentation) - multiline arguments, with trailing invocation`() = assertFormatted(
      """
      |/////////////////////////
      |rainbow
      |    .shine(
      |        infrared,
      |        ultraviolet,
      |    )
      |    .bright()
      |"""
          .trimMargin(),
      deduceMaxWidth = true,
  )

  @Test
  fun `chaining (indentation) - multiline arguments, with trailing lambda`() = assertFormatted(
      """
      |/////////////////////////
      |rainbow
      |    .shine(
      |        infrared,
      |        ultraviolet,
      |    )
      |    .z { it }
      |"""
          .trimMargin(),
      deduceMaxWidth = true,
  )

  @Test
  fun `chaining (indentation) - multiline arguments, prefixed with super, with trailing invocation`() = assertFormatted(
      """
      |/////////////////////////
      |super.shine(
      |    infrared,
      |    ultraviolet,
      |)
      |    .bright()
      |"""
          .trimMargin(),
      deduceMaxWidth = true,
  )

  @Test
  fun `chaining (indentation) - multiline arguments, starting with short variable, with trailing invocation`() = assertFormatted(
      """
      |/////////////////////////
      |z12.shine(
      |    infrared,
      |    ultraviolet,
      |)
      |    .bright()
      |"""
          .trimMargin(),
      deduceMaxWidth = true,
  )

  @Test
  fun `chaining (indentation) - start with multiline arguments`() = assertFormatted(
      """
      |/////////////////////////
      |getRainbow(
      |    infrared,
      |    ultraviolet,
      |)
      |"""
          .trimMargin(),
      deduceMaxWidth = true,
  )

  @Test
  fun `chaining (indentation) - start with multiline arguments, with trailing invocation`() = assertFormatted(
      """
      |/////////////////////////
      |getRainbow(
      |    infrared,
      |    ultraviolet,
      |)
      |    .z { it }
      |"""
          .trimMargin(),
      deduceMaxWidth = true,
  )

  @Test
  fun `annotations for expressions`() = assertFormatted(
      """
      |fun f() {
      |  var b
      |  @Suppress("UNCHECKED_CAST") b = f(1) as Int
      |  @Suppress("UNCHECKED_CAST")
      |  b = f(1) as Int
      |
      |  @Suppress("UNCHECKED_CAST") b = f(1) to 5
      |  @Suppress("UNCHECKED_CAST")
      |  b = f(1) to 5
      |
      |  @Suppress("UNCHECKED_CAST") f(1) as Int + 5
      |  @Suppress("UNCHECKED_CAST")
      |  f(1) as Int + 5
      |
      |  @Anno1 /* comment */ @Anno2 f(1) as Int
      |}
      |"""
          .trimMargin(),
  )

  @Test
  fun `annotations for expressions 2`() {
    val code =
        """
        |fun f() {
        |  @Suppress("UNCHECKED_CAST") f(1 + f(1) as Int)
        |  @Suppress("UNCHECKED_CAST")
        |  f(1 + f(1) as Int)
        |}
        |"""
            .trimMargin()

    val expected =
        """
        |fun f() {
        |  @Suppress("UNCHECKED_CAST") f(1 + f(1) as Int)
        |  @Suppress("UNCHECKED_CAST") f(1 + f(1) as Int)
        |}
        |"""
            .trimMargin()

    assertThatFormatting(code).isEqualTo(expected)
  }

  @Test
  fun `function call following long multiline string`() = assertFormatted(
      """
          |////////////////////////////////
          |fun stringFitsButNotMethod() {
          |  val str1 =
          |      $TQ Some string $TQ
          |          .trimIndent()
          |
          |  val str2 =
          |      $TQ Some string $TQ
          |          .trimIndent(someArg)
          |}
          |
          |fun stringTooLong() {
          |  val str1 =
          |      $TQ
          |      Some very long string that might mess things up
          |      $TQ
          |          .trimIndent()
          |
          |  val str2 =
          |      $TQ
          |      Some very long string that might mess things up
          |      $TQ
          |          .trimIndent(someArg)
          |}
          |"""
          .trimMargin(),
      deduceMaxWidth = true,
  )

  @Test
  fun `array-literal in annotation`() = assertFormatted(
      """
      |////////////////////////////////
      |@Anno(
      |    array =
      |        [
      |            someItem,
      |            andAnother,
      |            noTrailingComma])
      |class Host
      |
      |@Anno(
      |    array =
      |        [
      |            someItem,
      |            andAnother,
      |            withTrailingComma,
      |        ])
      |class Host
      |
      |@Anno(
      |    array =
      |        [
      |            // Comment
      |            someItem,
      |            // Comment
      |            andAnother,
      |            // Comment
      |            withTrailingComment
      |            // Comment
      |            // Comment
      |            ])
      |class Host
      |"""
          .trimMargin(),
      deduceMaxWidth = true,
  )

  @Test
  fun `force blank line between class members`() {
    val code =
        """
        |class Foo {
        |  val x = 0
        |  fun foo() {}
        |  class Bar {}
        |  enum class Enum {
        |    A {
        |      val x = 0
        |      fun foo() {}
        |    };
        |    abstract fun foo(): Unit
        |  }
        |}
        |"""
            .trimMargin()

    val expected =
        """
        |class Foo {
        |  val x = 0
        |
        |  fun foo() {}
        |
        |  class Bar {}
        |
        |  enum class Enum {
        |    A {
        |      val x = 0
        |
        |      fun foo() {}
        |    };
        |
        |    abstract fun foo(): Unit
        |  }
        |}
        |"""
            .trimMargin()

    assertThatFormatting(code).isEqualTo(expected)
  }

  @Test
  fun `preserve blank line between class members between properties`() {
    val code =
        """
        |class Foo {
        |  val x = 0
        |  val x = 0
        |
        |  val x = 0
        |}
        |"""
            .trimMargin()

    assertThatFormatting(code).isEqualTo(code)
  }

  @Test
  fun `force blank line between class members preserved between properties with accessors`() {
    val code =
        """
        |class Foo {
        |  val _x = 0
        |  val x = 0
        |    private get
        |  val y = 0
        |}
        |
        |class Foo {
        |  val _x = 0
        |  val x = 0
        |    private set
        |  val y = 0
        |}
        |"""
            .trimMargin()

    val expected =
        """
        |class Foo {
        |  val _x = 0
        |  val x = 0
        |    private get
        |
        |  val y = 0
        |}
        |
        |class Foo {
        |  val _x = 0
        |  val x = 0
        |    private set
        |
        |  val y = 0
        |}
        |"""
            .trimMargin()

    assertThatFormatting(code).isEqualTo(expected)
  }

  @Test
  fun `context receivers`() {
    val code =
        """
        |context(Something)
        |
        |class A {
        |  context(
        |  // Test comment.
        |  Logger, Raise<Error>)
        |
        |  @SomeAnnotation
        |
        |  fun doNothing() {}
        |
        |  context(SomethingElse)
        |
        |  private class NestedClass {}
        |
        |  fun <T> testSuspend(
        |    mock: T,
        |    block: suspend context(SomeContext) T.() -> Unit,
        |  ) = startCoroutine {
        |    T.block()
        |  }
        |}
        |"""
            .trimMargin()

    val expected =
        """
        |context(Something)
        |class A {
        |  context(
        |  // Test comment.
        |  Logger,
        |  Raise<Error>)
        |  @SomeAnnotation
        |  fun doNothing() {}
        |
        |  context(SomethingElse)
        |  private class NestedClass {}
        |
        |  fun <T> testSuspend(
        |      mock: T,
        |      block: suspend context(SomeContext) T.() -> Unit,
        |  ) = startCoroutine {
        |    T.block()
        |  }
        |}
        |"""
            .trimMargin()

    assertThatFormatting(code).isEqualTo(expected)
  }

  @Test
  fun `context parameters`() {
    if (KotlinVersion.CURRENT < KotlinVersion(2, 2)) return
    val code =
        """
        |context(something: Something)
        |
        |class A {
        |  context(
        |  // Test comment.
        |  logger: Logger, raise: Raise<Error>, _: Ignored)
        |
        |  @SomeAnnotation
        |
        |  fun doNothing() {}
        |
        |  context(somethingElse: SomethingElse)
        |
        |  private class NestedClass {}
        |
        |  fun <T> testSuspend(
        |    mock: T,
        |    block: suspend context(someContext: SomeContext) T.() -> Unit,
        |  ) = startCoroutine {
        |    T.block()
        |  }
        |}
        |"""
            .trimMargin()

    val expected =
        """
        |context(something: Something)
        |class A {
        |  context(
        |  // Test comment.
        |  logger: Logger,
        |  raise: Raise<Error>,
        |  _: Ignored)
        |  @SomeAnnotation
        |  fun doNothing() {}
        |
        |  context(somethingElse: SomethingElse)
        |  private class NestedClass {}
        |
        |  fun <T> testSuspend(
        |      mock: T,
        |      block: suspend context(someContext: SomeContext) T.() -> Unit,
        |  ) = startCoroutine {
        |    T.block()
        |  }
        |}
        |"""
            .trimMargin()

    assertThatFormatting(code).isEqualTo(expected)
  }

  @Test
  fun `context receivers on secondary constructor`() {
    val code =
        """
        |class A(val x: Int) {
        |  context(Something)
        |  constructor() : this(0)
        |}
        |"""
            .trimMargin()

    val expected =
        """
        |class A(val x: Int) {
        |  context(Something)
        |  constructor() : this(0)
        |}
        |"""
            .trimMargin()

    assertThatFormatting(code).isEqualTo(expected)
  }

  @Test
  fun `trailing comment after function in class`() = assertFormatted(
      """
      |class Host {
      |  fun fooBlock() {
      |    return
      |  } // Trailing after fn
      |  // Hanging after fn
      |
      |  // End of class
      |}
      |
      |class Host {
      |  fun fooExpr() = 0 // Trailing after fn
      |  // Hanging after fn
      |
      |  // End of class
      |}
      |
      |class Host {
      |  constructor() {} // Trailing after fn
      |  // Hanging after fn
      |
      |  // End of class
      |}
      |
      |class Host
      |// Primary constructor
      |constructor() // Trailing after fn
      |  // Hanging after fn
      |{
      |  // End of class
      |}
      |
      |class Host {
      |  fun fooBlock() {
      |    return
      |  }
      |
      |  // Between elements
      |
      |  fun fooExpr() = 0
      |
      |  // Between elements
      |
      |  fun fooBlock() {
      |    return
      |  }
      |}
      |"""
          .trimMargin(),
  )

  @Test
  fun `trailing comment after function top-level`() {
    assertFormatted(
        """
        |fun fooBlock() {
        |  return
        |} // Trailing after fn
        |// Hanging after fn
        |
        |// End of file
        |"""
            .trimMargin(),
    )

    assertFormatted(
        """
        |fun fooExpr() = 0 // Trailing after fn
        |// Hanging after fn
        |
        |// End of file
        |"""
            .trimMargin(),
    )

    assertFormatted(
        """
        |fun fooBlock() {
        |  return
        |}
        |
        |// Between elements
        |
        |fun fooExpr() = 0
        |
        |// Between elements
        |
        |fun fooBlock() {
        |  return
        |}
        |"""
            .trimMargin(),
    )
  }

  @Test
  fun `line break on base class`() = assertFormatted(
      """
      |///////////////////////////
      |class Basket<T>() :
      |    WovenObject {
      |  // some body
      |}
      |"""
          .trimMargin(),
      deduceMaxWidth = true,
  )

  @Test
  fun `line break on type specifier`() = assertFormatted(
      """
      |///////////////////////////
      |class Basket<T>()
      |    where T : Fruit {
      |  // some body
      |}
      |"""
          .trimMargin(),
      deduceMaxWidth = true,
  )

  @Test
  fun `don't crash on empty enum with semicolons`() {
    assertFormatted(
        """
        |///////////////////////////
        |enum class Foo {
        |  ;
        |
        |  fun foo(): Unit
        |}
        |"""
            .trimMargin(),
        deduceMaxWidth = true,
    )

    assertFormatted(
        """
        |///////////////////////////
        |enum class Foo {
        |  ;
        |
        |  companion object Bar
        |}
        |"""
            .trimMargin(),
        deduceMaxWidth = true,
    )

    assertThatFormatting(
        """
        |enum class Foo {
        |  ;
        |  ;
        |  ;
        |
        |  fun foo(): Unit
        |}
        |"""
            .trimMargin(),
    )
        .isEqualTo(
            """
            |enum class Foo {
            |  ;
            |
            |  fun foo(): Unit
            |}
            |"""
                .trimMargin(),
        )
  }

  @Test
  fun `comment stable test`() {
    // currently unstable
    val first =
        """
        |class Foo { // This is a very long comment that is very long and needs to be line broken because it is long
        |}
        |"""
            .trimMargin()
    val second =
        """
        |class Foo { // This is a very long comment that is very long and needs to be line broken because it
        |            // is long
        |}
        |"""
            .trimMargin()
    val third =
        """
        |class Foo { // This is a very long comment that is very long and needs to be line broken because it
        |  // is long
        |}
        |"""
            .trimMargin()

    assertThatFormatting(first).isEqualTo(second)
    assertThatFormatting(second).isEqualTo(third)
    assertFormatted(third)
  }

  @Test
  fun `comment stable test - no block`() {
    val first =
        """
        |class Fooez // This is a very long comment that is very long and needs to be line broken because it is long
        |"""
            .trimMargin()
    val second =
        """
        |class Fooez // This is a very long comment that is very long and needs to be line broken because it
        |            // is long
        |"""
            .trimMargin()

    assertThatFormatting(first).isEqualTo(second)
    assertFormatted(second)
  }

  @Test
  fun `comment stable test - two blocks`() {
    // currently unstable
    val first =
        """
        |class Fooez // This is a very long comment that is very long and needs to be line broken because it is long
        |class Bar
        |"""
            .trimMargin()
    val second =
        """
        |class Fooez // This is a very long comment that is very long and needs to be line broken because it
        |            // is long
        |
        |class Bar
        |"""
            .trimMargin()
    val third =
        """
        |class Fooez // This is a very long comment that is very long and needs to be line broken because it
        |
        |// is long
        |
        |class Bar
        |"""
            .trimMargin()

    assertThatFormatting(first).isEqualTo(second)
    assertThatFormatting(second).isEqualTo(third)
    assertFormatted(third)
  }

  @Test
  fun `comment stable test - within block`() {
    // currently unstable
    val first =
        """
        |class Foo {
        |  class Bar // This is a very long comment that is very long and needs to be line broken because it is long
        |}
        |"""
            .trimMargin()
    val second =
        """
        |class Foo {
        |  class Bar // This is a very long comment that is very long and needs to be line broken because it
        |            // is long
        |}
        |"""
            .trimMargin()
    val third =
        """
        |class Foo {
        |  class Bar // This is a very long comment that is very long and needs to be line broken because it
        |  // is long
        |}
        |"""
            .trimMargin()

    assertThatFormatting(first).isEqualTo(second)
    assertThatFormatting(second).isEqualTo(third)
    assertFormatted(third)
  }

  @Test
  fun `comment formatting respects max width`() {
    val code =
        """
        |// This is a very long comment that is very long but does not need to be line broken as it is within maxWidth
        |class MyClass {}
        |"""
            .trimMargin()
    assertThatFormatting(code)
        .withOptions(defaultTestFormattingOptions.copy(maxWidth = 120))
        .isEqualTo(code)
  }

  @Test
  fun `guard conditions with subject`() {
    val code =
        """
        |fun feedAnimal(animal: Animal) {
        |    when (animal) {
        |        is Animal.Dog -> animal.feedDog()
        |        is Animal.Cat if !animal.mouseHunter -> animal.feedCat()
        |        is Animal.Cat           if     !animal.birdHunter -> animal.feedCat()
        |        is Animal.Cat if
        |          !animal.birdHunter -> animal.feedCat()
        |        is Animal.Cat if     (!animal.birdHunter) -> animal.feedCat()
        |          else  if animal
        |              .eatsPlants -> animal.giveLettuce()   
        |        else -> println("Unknown animal")
        |    }
        |}
        |"""
            .trimMargin()

    val expected =
        """
        |fun feedAnimal(animal: Animal) {
        |  when (animal) {
        |    is Animal.Dog -> animal.feedDog()
        |    is Animal.Cat if !animal.mouseHunter -> animal.feedCat()
        |    is Animal.Cat if !animal.birdHunter -> animal.feedCat()
        |    is Animal.Cat if !animal.birdHunter -> animal.feedCat()
        |    is Animal.Cat if (!animal.birdHunter) -> animal.feedCat()
        |    else if animal.eatsPlants -> animal.giveLettuce()
        |    else -> println("Unknown animal")
        |  }
        |}
        |"""
            .trimMargin()

    assertThatFormatting(code).isEqualTo(expected)
  }

  @Test
  fun `line with max length that needs a trailing comma`() {
    val code =
        """
        |fun foo(a: String, b: String) {
        |  foo(
        |    a = "this is a very very very very very very veryy long line that has precisely 100 characters",
        |    b = "also is a very very very very very very veryyy long line that has precisely 100 characters"
        |  )
        |}
        |"""
            .trimMargin()

    val expected =
        """
        |fun foo(a: String, b: String) {
        |  foo(
        |    a = "this is a very very very very very very veryy long line that has precisely 100 characters",
        |    b =
        |      "also is a very very very very very very veryyy long line that has precisely 100 characters",
        |  )
        |}
        |"""
            .trimMargin()

    assertThatFormatting(code)
        .withOptions(
            defaultTestFormattingOptions.copy(
                maxWidth = 100,
                blockIndent = 2,
                continuationIndent = 2,
                trailingCommaManagementStrategy = TrailingCommaManagementStrategy.ONLY_ADD,
            ),
        )
        .isEqualTo(expected)
  }

  @Test
  fun `single-line parameter list breaking to multi-line should add trailing comma in one pass`() {
    val code =
        """
        |fun foo(param1: String, param2: String, param3: String) {
        |  functionCall(param1 = "value1", param2 = "value2", param3 = "value3", param4 = "value4", param5 = "value5")
        |}
        |"""
            .trimMargin()

    val expected =
        """
        |fun foo(param1: String, param2: String, param3: String) {
        |  functionCall(
        |    param1 = "value1",
        |    param2 = "value2",
        |    param3 = "value3",
        |    param4 = "value4",
        |    param5 = "value5",
        |  )
        |}
        |"""
            .trimMargin()

    assertThatFormatting(code)
        .withOptions(
            defaultTestFormattingOptions.copy(
                maxWidth = 100,
                blockIndent = 2,
                continuationIndent = 2,
                trailingCommaManagementStrategy = TrailingCommaManagementStrategy.ONLY_ADD,
            ),
        )
        .isEqualTo(expected)
  }

  @Test
  fun `single-line parameter list breaking to multi-line when a parameter spans multiple lines`() {
    val code =
        """
        |fun foo(param1: String, param2: String, param3: String) {
        |  functionCall(param1 = "value1", param2 = "value2", param3 = "this one is very long and will have to sit on its own line otherwise the line would overflow")
        |}
        |"""
            .trimMargin()

    val expected =
        """
        |fun foo(param1: String, param2: String, param3: String) {
        |  functionCall(
        |    param1 = "value1",
        |    param2 = "value2",
        |    param3 =
        |      "this one is very long and will have to sit on its own line otherwise the line would overflow",
        |  )
        |}
        |"""
            .trimMargin()

    assertThatFormatting(code)
        .withOptions(
            defaultTestFormattingOptions.copy(
                maxWidth = 100,
                blockIndent = 2,
                continuationIndent = 2,
                trailingCommaManagementStrategy = TrailingCommaManagementStrategy.ONLY_ADD,
            ),
        )
        .isEqualTo(expected)
  }

  @Test
  fun `a trailing comma added while breaking a list does not push the last line over the max width`() {
    val code =
        """
        |functionCall(shortArg, namedArgument = "string sized so the trailing comma ktfmt adds tips it one char over the limit.")
        |"""
            .trimMargin()

    val expected =
        """
        |functionCall(
        |    shortArg,
        |    namedArgument =
        |        "string sized so the trailing comma ktfmt adds tips it one char over the limit.",
        |)
        |"""
            .trimMargin()

    assertThatFormatting(code).withOptions(Formatter.KOTLINLANG_FORMAT).isEqualTo(expected)
  }

  @Test
  fun `formatting a list whose trailing comma lands at the max width is idempotent`() {
    val code =
        """
        |functionCall(shortArg, namedArgument = "string sized so the trailing comma ktfmt adds tips it one char over the limit.")
        |"""
            .trimMargin()

    val formattedOnce = Formatter.format(Formatter.KOTLINLANG_FORMAT, code)
    assertThatFormatting(formattedOnce)
        .withOptions(Formatter.KOTLINLANG_FORMAT)
        .isEqualTo(formattedOnce)
  }

  @Test
  fun `preserve lambda breaks - keeps multi-line lambda multi-line`() {
    val code =
        """
        |fun compose() {
        |  App {
        |    SelectableCard {
        |      Button { Text("Hello") }
        |    }
        |  }
        |}
        |"""
            .trimMargin()

    assertThatFormatting(code)
        .withOptions(
            FormattingOptions(
                preserveLambdaBreaks = true,
                blockIndent = 2,
                continuationIndent = 4,
            ),
        )
        .isEqualTo(code)
  }

  @Test
  fun `preserve lambda breaks - keeps single-line lambda single-line`() {
    // The classic Gradle/Compose single-line case the option must NOT disturb:
    // dependencies { implementation(libs.androidx.activity) }
    // remember { mutableStateOf(false) }
    val code =
        """
        |fun build() {
        |  dependencies { implementation(libs.androidx.activity) }
        |  val state = remember { mutableStateOf(false) }
        |}
        |"""
            .trimMargin()

    assertThatFormatting(code)
        .withOptions(
            FormattingOptions(
                preserveLambdaBreaks = true,
                blockIndent = 2,
                continuationIndent = 4,
            ),
        )
        .isEqualTo(code)
  }

  @Test
  fun `preserve lambda breaks - disabled collapses multi-line lambda to single line`() {
    val code =
        """
        |fun compose() {
        |  App {
        |    Button { Text("Hello") }
        |  }
        |}
        |"""
            .trimMargin()

    val expected =
        """
        |fun compose() {
        |  App { Button { Text("Hello") } }
        |}
        |"""
            .trimMargin()

    assertThatFormatting(code)
        .withOptions(
            FormattingOptions(
                preserveLambdaBreaks = false,
                blockIndent = 2,
                continuationIndent = 4,
            ),
        )
        .isEqualTo(expected)
  }

  @Test
  fun `preserve lambda breaks - mixed single-line and multi-line preserved independently`() {
    // Inner single-line lambdas stay single-line; outer multi-line lambdas stay multi-line.
    val code =
        """
        |fun compose() {
        |  App {
        |    val state = remember { mutableStateOf(0) }
        |    SelectableCard {
        |      Button { Text("Count: ${'$'}{state.value}") }
        |    }
        |  }
        |}
        |"""
            .trimMargin()

    assertThatFormatting(code)
        .withOptions(
            FormattingOptions(
                preserveLambdaBreaks = true,
                blockIndent = 2,
                continuationIndent = 4,
            ),
        )
        .isEqualTo(code)
  }

  @Test
  fun `preserve lambda breaks - applies to non-trailing lambdas too`() {
    // Lambda passed as a non-trailing argument that is multi-line in source stays multi-line.
    val code =
        """
        |fun test() {
        |  withCallback(
        |      onClick = {
        |        log("clicked")
        |      },
        |      label = "Press",
        |  )
        |}
        |"""
            .trimMargin()

    assertThatFormatting(code)
        .withOptions(
            FormattingOptions(
                preserveLambdaBreaks = true,
                blockIndent = 2,
                continuationIndent = 4,
            ),
        )
        .isEqualTo(code)
  }

  @Test
  fun `preserve lambda breaks - empty lambda always collapses even when multi-line in source`() {
    val code =
        """
        |fun test() {
        |  noop {
        |  }
        |}
        |"""
            .trimMargin()

    val expected =
        """
        |fun test() {
        |  noop {}
        |}
        |"""
            .trimMargin()

    assertThatFormatting(code)
        .withOptions(
            FormattingOptions(
                preserveLambdaBreaks = true,
                blockIndent = 2,
                continuationIndent = 4,
            ),
        )
        .isEqualTo(expected)
  }

  @Test
  fun `preserve lambda breaks - single statement broken across lines is kept multi-line`() {
    val code =
        """
        |fun test() {
        |  scope {
        |    doSomething()
        |  }
        |}
        |"""
            .trimMargin()

    assertThatFormatting(code)
        .withOptions(
            FormattingOptions(
                preserveLambdaBreaks = true,
                blockIndent = 2,
                continuationIndent = 4,
            ),
        )
        .isEqualTo(code)
  }

  @Test
  fun `preserve lambda breaks - still fixes a dangling closing brace`() {
    // Preserving breaks must not mean "leave the source untouched". The outer lambda is multi-line
    // in source, so it stays multi-line, but the trailing '}' that was left on the inner lambda's
    // line is still pulled onto its own line
    val code =
        """
        |fun compose() {
        |  App {
        |    Button { Text("Hello") } }
        |}
        |"""
            .trimMargin()

    val expected =
        """
        |fun compose() {
        |  App {
        |    Button { Text("Hello") }
        |  }
        |}
        |"""
            .trimMargin()

    assertThatFormatting(code)
        .withOptions(
            FormattingOptions(
                preserveLambdaBreaks = true,
                blockIndent = 2,
                continuationIndent = 4,
            ),
        )
        .isEqualTo(expected)
  }

  @Test
  fun `single parameter gets trailing comma when wrapped`() = assertFormatted(
      """
      |///////////////////////////////////////////
      |fun foo(
      |    aLongParameterNameThatForcesWrapping:
      |        String,
      |) {}
      |"""
          .trimMargin(),
      formattingOptions = META_FORMAT,
      deduceMaxWidth = true,
  )

  @Test
  fun `single parameter gets wrapped if line limit reached after trailing comma added`() {
    val code =
        """
        |////////////////////////////////////////////////
        |fun foo(
        |    aLongParameterNameThatForcesWrapping: String
        |) {}
        |"""
            .trimMargin()
    val expected =
        """
        |////////////////////////////////////////////////
        |fun foo(
        |    aLongParameterNameThatForcesWrapping:
        |        String,
        |) {}
        |"""
            .trimMargin()

    assertThatFormatting(code)
        .withOptions(
            META_FORMAT.copy(
                maxWidth = code.lineSequence().first().length,
            ),
        )
        .isEqualTo(expected)
  }

  @Test
  fun `correct indentation for lambda with chained function call (#589)`() = assertFormatted(
      """
      |fun quux() = runnnnn {
      |  foo()
      |  bar()
      |}
      |    .baz()
      |"""
          .trimMargin(),
  )

  @Test
  fun `correct indentation for lambda with multiple chained function call (#589)`() = assertFormatted(
      """
      |fun quux() = runnnnn {
      |  foo()
      |  bar()
      |}
      |    .baz {
      |      foo()
      |      bar()
      |    }
      |"""
          .trimMargin(),
  )

  @Test
  fun `correct indentation for lambda with chained function call in block (#589)`() = assertFormatted(
      """
      |fun quux() {
      |  runnnnn {
      |    foo()
      |    bar()
      |  }
      |      .baz()
      |}
      |"""
          .trimMargin(),
  )

  @Test
  fun `correct indentation for lambda with multiple chained function call in block (#589)`() = assertFormatted(
      """
      |fun quux() {
      |  runnnnn {
      |    foo()
      |    bar()
      |  }
      |      .baz {
      |        foo()
      |        bar()
      |      }
      |}
      |"""
          .trimMargin(),
  )

  @Test
  fun `correct indentation for lambda with multiple chained function call in nested block (#589)`() = assertFormatted(
      """
      |fun quux() {
      |  runnnnn {
      |    foo()
      |    runnnnn {
      |      foo()
      |      bar()
      |    }
      |        .baz {
      |          foo()
      |          bar()
      |        }
      |  }
      |      .baz {
      |        foo()
      |        runnnnn {
      |          foo()
      |          bar()
      |        }
      |            .baz {
      |              foo()
      |              bar()
      |            }
      |      }
      |}
      |"""
          .trimMargin(),
  )

  @Test
  fun `correct indentation for lambda with multiple chained function call in declaration (#589)`() = assertFormatted(
      """
      |val baz = runnnnn {
      |  foo()
      |  bar()
      |}
      |    .baz {
      |      foo()
      |      bar()
      |    }
      |"""
          .trimMargin(),
  )

  @Test
  fun `correct indentation for lambda with multiple chained function call in local declaration (#589)`() = assertFormatted(
      """
      |fun quux() {
      |  val baz = runnnnn {
      |    foo()
      |    bar()
      |  }
      |      .baz {
      |        foo()
      |        bar()
      |      }
      |}
      |"""
          .trimMargin(),
  )

  @Test
  fun `property with chained scoping function with value arguments in selector`() = assertFormatted(
      """
      |val foo = runnnnn {
      |  bar()
      |  baz()
      |}
      |    .fold({ a -> a }, { b -> b })
      |"""
          .trimMargin(),
  )

  @Test
  fun `property with single-line lambda chained call`() = assertFormatted(
      """
      |val foo = runnnnn { singleStatement() }.baz()
      |"""
          .trimMargin(),
  )

  @Test
  fun `property with chained scoping function with actual value arguments`() = assertFormatted(
      """
      |val foo = runnnnn {
      |  bar()
      |  baz()
      |}
      |    .someMethod(arg1, arg2)
      |"""
          .trimMargin(),
  )

  @Test
  fun `consistent formatting for chained scoping function in property vs function body`() {
    val propertyCase =
        """
        |val foo = runnnnn { singleLine() }.baz()
        |"""
            .trimMargin()

    val functionCase =
        """
        |fun quux() = runnnnn { singleLine() }.baz()
        |"""
            .trimMargin()

    // Both should format the same way
    assertFormatted(propertyCase)
    assertFormatted(functionCase)
  }

  @Test
  fun `property delegate with chained scoping function and value arguments`() = assertFormatted(
      """
      |val foo by runnnnn {
      |  bar()
      |  baz()
      |}
      |    .someMethod(arg1, arg2)
      |"""
          .trimMargin(),
  )

  @Test
  fun `backing field with chained scoping function and value arguments`() = assertFormatted(
      """
      |var foo: Int
      |  field = runnnnn {
      |    bar()
      |    baz()
      |  }
      |      .fold({ a -> a }, { b -> b })
      |"""
          .trimMargin(),
  )

  @Test
  fun `property with chained scoping function and safe call value arguments`() = assertFormatted(
      """
      |val foo = runnnnn {
      |  bar()
      |  baz()
      |}
      |    ?.someMethod(arg1, arg2)
      |"""
          .trimMargin(),
  )

  @Test
  fun `property delegate with chained scoping function no value arguments breaks after by`() = assertFormatted(
      """
      |val foo by runnnnn {
      |  bar()
      |  baz()
      |}
      |    .baz()
      |"""
          .trimMargin(),
  )

  @Test
  fun `backing field with chained scoping function no value arguments breaks after equals`() = assertFormatted(
      """
      |var foo: Int
      |  field = runnnnn {
      |    bar()
      |    baz()
      |  }
      |      .baz()
      |"""
          .trimMargin(),
  )

  @Test
  fun `property with leading comment before chained scoping falls back to break`() = assertFormatted(
      """
      |val foo = /* comment */ runnnnn {
      |  bar()
      |  baz()
      |}
      |    .someMethod(arg1, arg2)
      |"""
          .trimMargin(),
  )

  @Test
  fun `property with chained scoping mixed value and no-value selectors keeps same line`() = assertFormatted(
      """
      |val foo = runnnnn {
      |  bar()
      |  baz()
      |}
      |    .map { it }
      |    .fold(a, b)
      |"""
          .trimMargin(),
  )

  @Test
  fun `property initializer no-value chain breaks after equals`() = assertFormatted(
      """
      |val foo = runnnnn {
      |  bar()
      |  baz()
      |}
      |    .baz()
      |"""
          .trimMargin(),
  )

  @Test
  fun `property delegate leading comment falls back to break`() = assertFormatted(
      """
      |val foo by /* comment */ runnnnn {
      |  bar()
      |  baz()
      |}
      |    .someMethod(arg1, arg2)
      |"""
          .trimMargin(),
  )

  @Test
  fun `backing field leading comment falls back to break`() = assertFormatted(
      """
      |var foo: Int
      |  field =
      |  /* comment */ runnnnn {
      |    bar()
      |    baz()
      |  }
      |      .fold(a, b)
      |"""
          .trimMargin(),
  )

  @Test
  fun `property with empty parens in chain treats as no-value and breaks`() = assertFormatted(
      """
      |val foo = runnnnn {
      |  bar()
      |  baz()
      |}
      |    .baz()
      |"""
          .trimMargin(),
  )

  @Test
  fun `property with type arguments only in chain treats as no-value and breaks`() = assertFormatted(
      """
      |val foo = runnnnn {
      |  bar()
      |  baz()
      |}
      |    .map<String>()
      |"""
          .trimMargin(),
  )

  // region call with chained call(s) (#633)

  @Test
  fun `multiline call without chained call (#633)`() = assertFormatted(
      """
      |fun f() {
      |  foo(
      |      1,
      |      2,
      |  )
      |}
      |"""
          .trimMargin(),
  )

  @Test
  fun `multiline call with chained call same line closing paren (#633)`() = assertThatFormatting(
      """
      |fun f() {
      |  foo(
      |      1,
      |      2,
      |  ).bar()
      |}
      |"""
          .trimMargin(),
  )
      .isEqualTo(
          """
          |fun f() {
          |  foo(
          |      1,
          |      2,
          |  )
          |      .bar()
          |}
          |"""
              .trimMargin(),
      )

  @Test
  fun `multiline call with chained call next line selector (#633)`() = assertFormatted(
      """
      |fun f() {
      |  foo(
      |      1,
      |      2,
      |  )
      |      .bar()
      |}
      |"""
          .trimMargin(),
  )

  @Test
  fun `multiline call in property initializer (#633)`() = assertFormatted(
      """
      |val x = foo(
      |    1,
      |    2,
      |)
      |"""
          .trimMargin(),
  )

  @Test
  fun `multiline call in property initializer with chain (#633)`() = assertFormatted(
      """
      |val x = foo(
      |    1,
      |    2,
      |)
      |    .bar()
      |"""
          .trimMargin(),
  )

  @Test
  fun `multiline call in property delegate (#633)`() = assertFormatted(
      """
      |val x by foo(
      |    1,
      |    2,
      |)
      |"""
          .trimMargin(),
  )

  @Test
  fun `multiline call in property delegate with chain (#633)`() = assertFormatted(
      """
      |val x by foo(
      |    1,
      |    2,
      |)
      |    .bar()
      |"""
          .trimMargin(),
  )

  @Test
  fun `multiline call in backing field (#633)`() = assertFormatted(
      """
      |var x: Int
      |  field = foo(
      |      1,
      |      2,
      |  )
      |"""
          .trimMargin(),
  )

  @Test
  fun `multiline call in backing field with chain (#633)`() = assertFormatted(
      """
      |var x: Int
      |  field = foo(
      |      1,
      |      2,
      |  )
      |      .bar()
      |"""
          .trimMargin(),
  )

  @Test
  fun `multiline call with leading line comment (#633)`() = assertFormatted(
      """
      |val x = // comment
      |    foo(
      |        1,
      |        2,
      |    )
      |"""
          .trimMargin(),
  )

  @Test
  fun `multiline call with leading block comment (#633)`() = assertFormatted(
      """
      |val y =
      |    /* comment */ foo(
      |        1,
      |        2,
      |    )
      |"""
          .trimMargin(),
  )

  @Test
  fun `inner multiline call (#633)`() = assertFormatted(
      """
      |val x = foo(
      |    bar(
      |        1,
      |        2,
      |    ),
      |    3,
      |)
      |"""
          .trimMargin(),
  )

  @Test
  fun `inner multiline call with chain (#633)`() = assertFormatted(
      """
      |val x = foo(
      |    bar(
      |        1,
      |        2,
      |    )
      |        .baz(),
      |    3,
      |)
      |"""
          .trimMargin(),
  )

  @Test
  fun `expression-body call with nested multiline calls and chains (#633)`() = assertFormatted(
      """
      |fun quux() = listOf(
      |    "a",
      |    "b",
      |    listOf(
      |        "a",
      |        "b",
      |    ),
      |    listOf(
      |        "a",
      |        "b",
      |    )
      |        .baz()
      |        .bar(1, 2)
      |        .fold { it.boom() }
      |)
      |    .baz()
      |"""
          .trimMargin(),
  )

  @Test
  fun `block-body call with nested multiline calls and chains (#633)`() = assertFormatted(
      """
      |fun quux() {
      |  listOf(
      |      "a",
      |      "b",
      |  )
      |
      |  listOf(
      |      "a",
      |      "b",
      |      listOf(
      |          "a",
      |          "b",
      |      ),
      |      listOf(
      |          "a",
      |          "b",
      |      )
      |          .baz()
      |          .bar(1, 2)
      |          .fold { it.boom() }
      |  )
      |      .baz()
      |}
      |"""
          .trimMargin(),
  )

  // endregion

  companion object {
    /** Triple quotes, useful to use within triple-quoted strings. */
    private const val TQ = "\"\"\""

    @JvmStatic
    @BeforeClass
    fun setUp(): Unit {
      defaultTestFormattingOptions =
          META_FORMAT.copy(trailingCommaManagementStrategy = TrailingCommaManagementStrategy.NONE)
    }
  }
}
