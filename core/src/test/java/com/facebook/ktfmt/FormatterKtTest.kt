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

import com.google.common.truth.FailureMetadata
import com.google.common.truth.Subject
import com.google.common.truth.Truth.assertAbout
import com.google.common.truth.Truth.assertThat
import com.google.googlejavaformat.FormattingError
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@Suppress("FunctionNaming")
@RunWith(JUnit4::class)
class FormatterKtTest {

  @Test
  fun `first selector stays on same line`() = assertFormatted(
      """
      |fun f() {
      |  ImmutableList.newBuilder()
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
      |
      |  // ... unless it's a function call
      |  ImmutableList()
      |      .newBuilder()
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
      |}
      |""".trimMargin(), 50)

  @Test
  fun `line breaks in function arguments`() = assertFormatted(
      """
      |fun f() {
      |  computeBreaks(
      |      javaOutput.commentsHelper,
      |      maxWidth,
      |      Doc.State(+0, 0))
      |  computeBreaks(
      |      output.commentsHelper, maxWidth, State(0))
      |  doc.computeBreaks(
      |      javaOutput.commentsHelper,
      |      maxWidth,
      |      Doc.State(+0, 0))
      |  doc.computeBreaks(
      |      output.commentsHelper, maxWidth, State(0))
      |}
      |""".trimMargin(), 50)

  @Test
  fun `parameters and return type in function definitions`() = assertFormatted(
      """
      |fun format(
      |    code: String,
      |    maxWidth: Int =
      |        DEFAULT_MAX_WIDTH_VERY_LONG
      |): String {
      |  val a = 0
      |}
      |fun print(
      |    code: String,
      |    maxWidth: Int =
      |        DEFAULT_MAX_WIDTH_VERY_LONG) {
      |  val a = 0
      |}
      |""".trimMargin(), 40)

  @Test
  fun `kitchen sink of tests`() {
    // Don't add more tests here
    val code = """
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

    val expected = """
        |fun f(a: Int, b: Double, c: String) {
        |  var result = 0
        |  val aVeryLongLongLongLongLongLongLongLongLongLongLongLongLongLongLongLongLongLongLongLongLongLongVar =
        |      43
        |  foo.bar.zed.accept()
        |
        |  foo()
        |
        |  foo.bar.zed.accept(DoSomething.bar())
        |
        |  bar(
        |      ImmutableList.newBuilder()
        |          .add(1)
        |          .add(1)
        |          .add(1)
        |          .add(1)
        |          .add(1)
        |          .add(1)
        |          .add(1)
        |          .add(1)
        |          .add(1)
        |          .add(1)
        |          .build())
        |
        |  ImmutableList.newBuilder()
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
        |}
        |""".trimMargin()

    assertThatFormatting(code).isEqualTo(expected)
    // Don't add more tests here
  }

  @Test
  fun `spacing around variable declarations`() = assertFormatted(
      """
      |fun f() {
      |  var x: Int = 4
      |  val y = 0
      |}
      |""".trimMargin())

  @Test
  fun `class without a body nor properties`() = assertFormatted("class Foo\n")

  @Test
  fun `interface without a body nor properties`() = assertFormatted("interface Foo\n")

  @Test
  fun `preserve empty primary constructor`() = assertFormatted("class Foo()\n")

  @Test
  fun `class without a body, with explicit ctor params`() =
      assertFormatted("class Foo(a: Int, var b: Double, val c: String)\n")

  @Test
  fun `class with a body and explicit ctor params`() = assertFormatted(
      """
      |class Foo(a: Int, var b: Double, val c: String) {
      |  val x = 2
      |  fun method() {}
      |  class Bar
      |}
      |""".trimMargin())

  @Test
  fun `properties and fields with modifiers`() = assertFormatted(
      """
      |class Foo(public val p1: Int, private val p2: Int, open val p3: Int, final val p4: Int) {
      |  private var f1 = 0
      |  public var f2 = 0
      |  open var f3 = 0
      |  final var f4 = 0
      |}
      |""".trimMargin())

  @Test
  fun `properties with multiple modifiers`() = assertFormatted(
      """
      |class Foo(public open inner val p1: Int) {
      |  public open inner var f2 = 0
      |}
      |""".trimMargin())

  @Test
  fun `spaces around binary operations`() = assertFormatted(
      """
      |fun foo() {
      |  a = 5
      |  x + 1
      |}
      |""".trimMargin())

  @Test
  fun `breaking long binary operations`() = assertFormatted(
      """
      |fun foo() {
      |  val finalWidth =
      |      value1 +
      |          value2 +
      |          (value3 +
      |              value4 +
      |              value5) +
      |          foo(v) +
      |          (1 + 2) +
      |          function(
      |              value7,
      |              value8) +
      |          value9
      |}
      |""".trimMargin(), 20)

  @Test
  fun `properties with accessors`() = assertFormatted(
      """
      |class Foo {
      |  var x: Int
      |    get() = field
      |  var y: Boolean
      |    get() = x.equals(123)
      |    set(value) {
      |      field = value
      |    }
      |  var z: Boolean
      |    get() {
      |      x.equals(123)
      |    }
      |  var zz = false
      |    private set
      |}
      |""".trimMargin())

  @Test
  fun `a property with a too long name being broken on multiple lines`() = assertFormatted(
      """
      |class Foo {
      |  val thisIsALongName
      |      : String =
      |      "Hello there this is long"
      |    get() = field
      |}
      |""".trimMargin(),
      maxWidth = 20)

  @Test
  fun `multi-character unary and binary operators such as ==`() = assertFormatted(
      """
      |fun f() {
      |  3 == 4
      |  true && false
      |  a++
      |  a === b
      |}
      |""".trimMargin())

  @Test
  fun `package names stay in one line`() {
    val code = """
      | package  com  .example. subexample
      |
      |fun f() = 1
      |""".trimMargin()
    val expected = """
      |package com.example.subexample
      |
      |fun f() = 1
      |""".trimMargin()

    assertThatFormatting(code).isEqualTo(expected)
  }

  @Test
  fun `safe dot operator expression`() = assertFormatted(
      """
      |fun f() {
      |  node?.name
      |}
      |""".trimMargin())

  @Test
  fun `safe dot operator expression with normal`() = assertFormatted(
      """
      |fun f() {
      |  node?.name.hello
      |}
      |""".trimMargin())

  @Test
  fun `import list`() {
    val code = """
      | import  com .example.common.reality. FooBar
      |  import  com .example.common.reality. FooBar2  as  foosBars
      |   import com .example.common.reality. *
      | import  foo.bar // Test
      |  import  abc.def /*
      |                  test */
      |""".trimMargin()
    val expected = """
      |import com.example.common.reality.FooBar
      |import com.example.common.reality.FooBar2 as foosBars
      |import com.example.common.reality.*
      |import foo.bar // Test
      |import abc.def /*
      |               test */
      |""".trimMargin()
    assertThatFormatting(code).isEqualTo(expected)
  }

  @Test
  fun `basic annotations`() = assertFormatted(
      """
      |@Fancy
      |class Foo {
      |  @Fancy
      |  fun baz(@Fancy foo: Int) {
      |    @Fancy val a = 1 + foo
      |  }
      |}
      |""".trimMargin())

  @Test
  fun `function calls with multiple arguments`() = assertFormatted(
      """
      |fun f() {
      |  foo(1, 2, 3)
      |
      |  foo(
      |      123456789012345678901234567890,
      |      123456789012345678901234567890,
      |      123456789012345678901234567890)
      |}
      |""".trimMargin())

  @Test
  fun `function calls with multiple named arguments`() = assertFormatted(
      """
      |fun f() {
      |  foo(1, b = 2, c = 3)
      |
      |  foo(
      |      123456789012345678901234567890,
      |      b = 23456789012345678901234567890,
      |      c = 3456789012345678901234567890)
      |}
      |""".trimMargin())

  @Test
  fun `Arguments are blocks`() = assertFormatted(
      """
      |override fun visitProperty(property: KtProperty) {
      |  builder.sync(property)
      |  builder.block(ZERO) {
      |    declareOne(
      |        kind = DeclarationKind.FIELD,
      |        modifiers = property.modifierList,
      |        valOrVarKeyword =
      |            property.valOrVarKeyword.text,
      |        typeParameters =
      |            property.typeParameterList,
      |        receiver = property.receiverTypeReference,
      |        name = property.nameIdentifier?.text,
      |        type = property.typeReference,
      |        typeConstraintList =
      |            property.typeConstraintList,
      |        delegate = property.delegate,
      |        initializer = property.initializer)
      |  }
      |}
      |""".trimMargin(), 50)

  @Test
  fun `anonymous function`() = assertFormatted(
      """
      |fun f() {
      |  setListener(
      |      fun(number: Int) {
      |        println(number)
      |      })
      |}
      |""".trimMargin())

  @Test
  fun `anonymous function with receiver`() = assertFormatted(
      """
      |fun f() {
      |  setListener(
      |      fun View.() {
      |        println(this)
      |      })
      |}
      |""".trimMargin())

  @Test
  fun `when() with a subject expression`() = assertFormatted(
      """
      |fun f(x: Int) {
      |  when (x) {
      |    1 -> print(1)
      |    2 -> print(2)
      |    else -> {
      |      print(3)
      |    }
      |  }
      |}
      |""".trimMargin())

  @Test
  fun `when() expression with complex predicates`() = assertFormatted(
      """
      |fun f(x: Int) {
      |  when {
      |    x == 1 || x == 2 -> print(1)
      |    x == 3 && x != 4 -> print(2)
      |    else -> {
      |      print(3)
      |    }
      |  }
      |}
      |""".trimMargin())

  @Test
  fun `when() expression with several conditions`() = assertFormatted(
      """
      |fun f(x: Int) {
      |  when {
      |    0, 1 -> print(1)
      |    else -> print(0)
      |  }
      |}
      |""".trimMargin())

  @Test
  fun `when() expression with is and in`() = assertFormatted(
      """
      |fun f(x: Int) {
      |  when (x) {
      |    is String -> print(1)
      |    !is String -> print(2)
      |    in 1..3 -> print()
      |    in a..b -> print()
      |    in a..3 -> print()
      |    in 1..b -> print()
      |    !in 1..b -> print()
      |    else -> print(3)
      |  }
      |}
      |""".trimMargin())

  @Test
  fun `when() expression with enum values`() = assertFormatted(
      """
      |fun f(x: Color) {
      |  when (x) {
      |    is Color.Red -> print(1)
      |    is Color.Green -> print(2)
      |    else -> print(3)
      |  }
      |}
      |""".trimMargin())

  @Test
  fun `when() expression with generic matcher and exhaustive`() = assertFormatted(
      """
      |fun f(x: Result) {
      |  when (x) {
      |    is Success<*> -> print(1)
      |    is Failure -> print(2)
      |  }.exhaustive
      |}
      |""".trimMargin())

  @Test
  fun `line breaks inside when expressions and conditions`() = assertFormatted(
      """
      |fun f() {
      |  return Text.create(c)
      |      .onTouch {
      |        when (it.motionEvent.action) {
      |          ACTION_DOWN ->
      |              Toast.makeText(it.view.context, "Down!", Toast.LENGTH_SHORT, blablabla).show()
      |          ACTION_UP -> Toast.makeText(it.view.context, "Up!", Toast.LENGTH_SHORT).show()
      |          ACTION_DOWN ->
      |              Toast.makeText(
      |                  it.view.context, "Down!", Toast.LENGTH_SHORT, blablabla, blablabl, blabla)
      |                  .show()
      |        }
      |      }
      |      .build()
      |}
      |""".trimMargin())

  @Test
  fun `function return types`() = assertFormatted(
      """
      |fun f1(): Int = 0
      |fun f2(): Int {}
      |""".trimMargin())

  @Test
  fun `multi line function without a block body`() = assertFormatted(
      """
      |fun longFunctionNoBlock():
      |    Int =
      |    1234567 + 1234567
      |fun shortFun(): Int =
      |    1234567 + 1234567
      |""".trimMargin(), 25)

  @Test
  fun `return type doesn't fit in one line`() = assertFormatted(
  """
      |interface X {
      |  fun f(arg1: Arg1Type, arg2: Arg2Type):
      |      Map<String, Map<String, Double>>? {
      |    //
      |  }
      |
      |  fun functionWithGenericReturnType(
      |      arg1: Arg1Type, arg2: Arg2Type
      |  ): Map<String, Map<String, Double>>? {
      |    //
      |  }
      |}
      |""".trimMargin(), 50)

  @Test
  fun `list of superclasses`() = assertFormatted(
      """
      |class Derived2 : Super1, Super2 {}
      |class Derived1 : Super1, Super2
      |class Derived3(a: Int) : Super1(a)
      |class Derived4 : Super1()
      |class Derived5 : Super3<Int>()
      |""".trimMargin())

  @Test
  fun `list of superclasses over multiple lines`() = assertFormatted(
      """
      |class Derived2 :
      |    Super1,
      |    Super2 {}
      |class Derived1 :
      |    Super1, Super2
      |class Derived3(
      |    a: Int) :
      |    Super1(a)
      |class Derived4 :
      |    Super1()
      |class Derived5 :
      |    Super3<Int>()
      |""".trimMargin(), 20)

  @Test
  fun `annotations with parameters`() = assertFormatted(
      """
      |@AnnWithArrayValue(1, 2, 3)
      |class C
      |""".trimMargin())

  @Test
  fun `method modifiers`() = assertFormatted(
      """
      |override internal fun f() {}
      |""".trimMargin())

  @Test
  fun `class modifiers`() = assertFormatted(
      """
      |abstract class Foo
      |inner class Foo
      |final class Foo
      |open class Foo
      |""".trimMargin())

  @Test
  fun `kdoc comments`() {
    val code = """
      |/**
      | * foo
      | */ class F {
      |
      | }
      |""".trimMargin()
    val expected = """
      |/** foo */
      |class F {}
      |""".trimMargin()
    assertThatFormatting(code).isEqualTo(expected)
  }

  @Test
  fun `formatting kdoc doesn't add p HTML tags`() = assertFormatted(
      """
      |/**
      | * Bla bla bla bla
      | *
      | * This is an inferred paragraph, and as you can see, we don't add a p tag to it, even though bla
      | * bla.
      | *
      | * <p>On the other hand, we respect existing tags, and don't remove them.
      | */
      |""".trimMargin())

  @Test
  fun `return statement with value`() = assertFormatted(
      """
      |fun random(): Int {
      |  return 4
      |}
      |""".trimMargin())

  @Test
  fun `return statement without value`() = assertFormatted(
      """
      |fun print(b: Boolean) {
      |  print(b)
      |  return
      |}
      |""".trimMargin())

  @Test
  fun `if statement without else`() = assertFormatted(
      """
      |fun maybePrint(b: Boolean) {
      |  if (b) {
      |    println(b)
      |  }
      |}
      |""".trimMargin())

  @Test
  fun `if statement with else`() = assertFormatted(
      """
      |fun maybePrint(b: Boolean) {
      |  if (b) {
      |    println(2)
      |  } else {
      |    println(1)
      |  }
      |}
      |""".trimMargin())

  @Test
  fun `if expression with else`() = assertFormatted(
      """
      |fun maybePrint(b: Boolean) {
      |  println(if (b) 1 else 2)
      |  println(
      |      if (b) {
      |        val a = 1 + 1
      |        2 * a
      |      } else 2)
      |  return if (b) 1 else 2
      |}
      |""".trimMargin())

  @Test
  fun `assignment expression on multiple lines`() = assertFormatted(
      """
      |fun f() {
      |  var myVariable = 5;
      |  myVariable =
      |      function1(4, 60, 8) + function2(57, 39, 20)
      |}
      |""".trimMargin(), 50)

  @Test
  fun `A program that tickled a bug in KotlinInput`() = assertFormatted(
      """
      |val x = 2
      |""".trimMargin())

  @Test
  fun `a few variations of constructors`() = assertFormatted(
      """
      |class Foo constructor(number: Int) {}
      |class Foo2 private constructor(number: Int) {}
      |class Foo3 @Inject constructor(number: Int) {}
      |class Foo4 @Inject private constructor(number: Int) {}
      |class Foo5
      |    @Inject
      |    private constructor(
      |        number: Int, number2: Int, number3: Int, number4: Int, number5: Int, number6: Int) {}
      |""".trimMargin())

  @Test
  fun `a constructor with many arguments over breaking to next line`() = assertFormatted(
      """
      |data class Foo(
      |    val number: Int, val name: String, val age: Int, val title: String, val offspring2: List<Foo>)
      |""".trimMargin())

  @Test
  fun `a constructor with keyword and many arguments over breaking to next line`() = assertFormatted(
      """
      |data class Foo
      |    constructor(
      |        val name: String, val age: Int, val title: String, val offspring: List<Foo>)
      |""".trimMargin())

  @Test
  fun `a constructor with many arguments over multiple lines`() = assertFormatted(
      """
      |data class Foo
      |    constructor(
      |        val number: Int,
      |        val name: String,
      |        val age: Int,
      |        val title: String,
      |        val offspring: List<Foo>)
      |""".trimMargin(), 50)

  @Test
  fun `handle secondary constructors`() = assertFormatted(
      """
      |class Foo private constructor(number: Int) {
      |  private constructor(n: Float) : this(1)
      |  private constructor(n: Double) : this(1) {
      |    println("built")
      |  }
      |}
      |""".trimMargin())

  @Test
  fun `a secondary constructor with many arguments over multiple lines`() = assertFormatted(
      """
      |data class Foo {
      |  constructor(
      |      val number: Int,
      |      val name: String,
      |      val age: Int,
      |      val title: String,
      |      val offspring: List<Foo>)
      |}
      |""".trimMargin(), 50)

  @Test
  fun `a secondary constructor with many arguments passed to delegate`() = assertFormatted(
      """
      |data class Foo {
      |  constructor(
      |      val number: Int,
      |      val name: String,
      |      val age: Int,
      |      val title: String,
      |      val offspring: List<Foo>) :
      |          this(
      |              number,
      |              name,
      |              age,
      |              title,
      |              offspring,
      |              offspring)
      |}
      |""".trimMargin(), 50)

  @Test
  fun `handle calling super constructor in secondary constructor`() = assertFormatted(
      """
      |class Foo : Bar {
      |  internal constructor(number: Int) : super(number) {}
      |}
      |""".trimMargin())

  @Test
  fun `handle objects`() = assertFormatted(
      """
      |object Foo(n: Int) {}
      |""".trimMargin())

  @Test
  fun `handle object expression`() = assertFormatted(
      """
      |fun f(): Any {
      |  return object : Adapter() {}
      |}
      |""".trimMargin())

  @Test
  fun `handle object expression in parenthesis`() = assertFormatted(
      """
      |fun f(): Any {
      |  return (object : Adapter() {})
      |}
      |""".trimMargin())

  @Test
  fun `handle array indexing operator`() = assertFormatted(
      """
      |fun f(a: Magic) {
      |  a[3]
      |  b[3, 4]
      |}
      |""".trimMargin())

  @Test
  fun `handle destructuring declaration`() = assertFormatted(
      """
      |fun f() {
      |  val (a, b: Int) = listOf(1, 2)
      |}
      |""".trimMargin())

  @Test
  fun `handle ? for nullalble types`() = assertFormatted(
      """
      |fun doItWithNullReturns(a: String, b: String): Int? {
      |  return 5
      |}
      |fun doItWithNulls(a: String, b: String?) {}
      |""".trimMargin())

  @Test
  fun `nullable function type`() = assertFormatted(
      """
      |var listener: ((Boolean) -> Unit)? = null
      |""".trimMargin())

  @Test
  fun `redundant parenthesis in function types`() = assertFormatted(
      """
      |val a: (Int) = 7
      |var listener: ((Boolean) -> Unit) = foo
      |""".trimMargin())

  @Test
  fun `handle string literals`() = assertFormatted(
      """
      |fun doIt(world: String) {
      |  println("Hello world!")
      |  println("Hello! ${'$'}world")
      |  println("Hello! ${'$'}{"wor" + "ld"}")
      |}
      |""".trimMargin())

  @Test
  fun `when there is an expression in a template string it gets formatted accordingly`() {
    val code = """
      |fun doIt() {
      |  println("Hello! ${'$'}{"wor"+"ld"}")
      |}
      |""".trimMargin()
    val expected = """
      |fun doIt() {
      |  println("Hello! ${'$'}{"wor" + "ld"}")
      |}
      |""".trimMargin()
    assertThatFormatting(code).isEqualTo(expected)
  }

  @Test
  fun `handle multiline string literals`() = assertFormatted(
      """
      |fun doIt(world: String) {
      |  println(${"\"".repeat(3)}Hello
      |      world!${"\"".repeat(3)})
      |}
      |""".trimMargin())

  @Test
  fun `handle some basic generics scenarios`() = assertFormatted(
      """
      |fun <T> doIt(a: List<T>): List<Int>? {
      |  val b: List<Int> = convert<Int>(listOf(5, 4))
      |  return b
      |}
      |class Foo<T>
      |""".trimMargin())

  @Test
  fun `handle for loops`() = assertFormatted(
      """
      |fun f(a: List<Int>) {
      |  for (i in a.indices) {
      |    println(i)
      |  }
      |}
      |""".trimMargin())

  @Test
  fun `Qualified type`() = assertFormatted(
      """
      |fun f() {
      |  var plusFour: Indent.Const
      |  var x: Map.Entry<String, Integer>
      |  var x: List<String>.Iterator
      |}
      |""".trimMargin())

  @Test
  fun `handle destructuring declaration in for loop`() = assertFormatted(
      """
      |fun f(a: List<Pair<Int, Int>>) {
      |  for ((x, y: Int) in a) {}
      |}
      |""".trimMargin())

  @Test
  fun `handle scope operator`() = assertFormatted(
      """
      |fun f(a: List<Int>) {
      |  a.forEach(::println)
      |  a.map(Int::toString)
      |}
      |""".trimMargin())

  @Test
  fun `handle escaped identifier`() = assertFormatted(
      """
      |import foo as `foo foo`
      |import org.mockito.Mockito.`when` as `yay yay`
      |
      |fun `spaces in functions`() {
      |  val `when` = NEVER
      |  val (`do not`, `ever write`) = SERIOUSLY
      |  val `a a`: Int
      |}
      |class `more spaces`
      |""".trimMargin())

  @Test
  fun `handle annotations with arguments`() = assertFormatted(
      """
      |@Px fun f(): Int = 5
      |@Dimenstion(unit = DP)
      |fun g(): Int = 5
      |@RunWith(MagicRunner::class)
      |class Test
      |""".trimMargin())

  @Test
  fun `annotations on functions types parameters`() = assertFormatted(
      """
      |val callback: (List<@JvmSuppressWildcards String>) -> Unit = foo
      |""".trimMargin())

  @Test
  fun `Unary expressions`() = assertFormatted(
      """
      |fun f() {
      |  !a
      |  -4
      |  val x = -foo()
      |  +4
      |  ++a
      |  --a
      |}
      |""".trimMargin())

  @Test
  fun `handle wildcard generics`() = assertFormatted(
      """
      |fun f() {
      |  val l: List<*>
      |  val p: Pair<*, *>
      |}
      |""".trimMargin())

  @Test
  fun `handle covariant and contravariant type arguments`() = assertFormatted(
      """
      |val p: Pair<in T, out S>
      |""".trimMargin())
  
  @Test
  fun `handle covariant and contravariant type parameters`() = assertFormatted(
      """
      |class Foo<in T, out S>
      |""".trimMargin())

  @Test
  fun `handle bounds for type parameters`() = assertFormatted(
      """
      |class Foo<in T : List<*>, out S : Any?>
      |""".trimMargin())

  @Test
  fun `handle compound generic bounds on classes`() = assertFormatted(
      """
      |class Foo<T>(n: Int) where T : Bar, T : FooBar {}
      |""".trimMargin())

  @Test
  fun `handle compound generic bounds on functions`() = assertFormatted(
      """
      |fun <T> foo(n: Int) where T : Bar, T : FooBar {}
      |""".trimMargin())

  @Test
  fun `handle compound generic bounds on properties`() = assertFormatted(
      """
      |val <T> List<T>.twiceSum: Int where T : Int
      |  get() {
      |    return 2 * sum()
      |  }
      |""".trimMargin())

  @Test
  fun `explicit type on property getter`() = assertFormatted(
      """
      |class Foo {
      |  val silly: Int
      |    get(): Int = 1
      |}
      |""".trimMargin())

  @Test
  fun `handle method calls with lambda arg only`() = assertFormatted(
      """
      |fun f() {
      |  val a = g { 1 + 1 }
      |}
      |""".trimMargin())

  @Test
  fun `handle method calls value args and a lambda arg`() = assertFormatted(
      """
      |fun f() {
      |  val a = g(1, 2) { 1 + 1 }
      |}
      |""".trimMargin())

  @Test
  fun `handle top level constants`() = assertFormatted(
      """
      |val a = 5
      |const val b = 6
      |""".trimMargin())

  @Test
  fun `handle lambda arg with named arguments`() = assertFormatted(
      """
      |fun f() {
      |  val b = { x: Int, y: Int -> x + y }
      |}
      |""".trimMargin())

  @Test
  fun `handle labeled this pointer`() = assertFormatted(
      """
      |class Foo {
      |  fun f() {
      |    g { println(this@Foo) }
      |  }
      |}
      |""".trimMargin())

  @Test
  fun `handle extension and operator functions`() = assertFormatted(
      """
      |operator fun Point.component1() = x
      |""".trimMargin())

  @Test
  fun `handle extension properties`() = assertFormatted(
      """
      |val Int.isPrime: Boolean
      |  get() = runMillerRabinPrimality(this)
      |""".trimMargin())

  @Test
  fun `generic extension property`() = assertFormatted(
      """
      |val <T> List<T>.twiceSize = 2 * size()
      |""".trimMargin())

  @Test
  fun `handle file annotations`() = assertFormatted(
      """
      |@file:JvmName("DifferentName")
      |package com.somecompany.example
      |
      |import com.somecompany.example2
      |
      |class Foo {}
      |""".trimMargin())

  @Test
  fun `handle init block`() = assertFormatted(
      """
      |class Foo {
      |  init {
      |    println("Init!")
      |  }
      |}
      |""".trimMargin())

  @Test
  fun `handle interface delegation`() = assertFormatted(
      """
      |class MyList(impl: List<Int>) : Collection<Int> by impl
      |""".trimMargin())

  @Test
  fun `handle property delegation`() = assertFormatted(
      """
      |val a by lazy { 1 + 1 }
      |""".trimMargin())

  @Test
  fun `handle lambda types`() = assertFormatted(
      """
      |val listener1: (Boolean) -> Unit = { b -> !b }
      |val listener2: () -> Unit = {}
      |val listener3: (Int, Double) -> Int = { a, b -> a }
      |val listener4: Int.(Int, Boolean) -> Unit
      |""".trimMargin())

  @Test
  fun `handle unicode in string literals`() = assertFormatted(
      """
      |val a = "\uD83D\uDC4D"
      |""".trimMargin())

  @Test
  fun `handle casting`() = assertFormatted(
      """
      |fun castIt(o: Object) {
      |  println(o is Double)
      |  println(o !is Double)
      |  doIt(o as Int)
      |  doIt(o as? Int)
      |}
      |""".trimMargin())

  @Test
  fun `handle colleciton literals in annotations`() = assertFormatted(
      """
      |@Foo(a = [1, 2])
      |fun doIt(o: Object) {}
      |""".trimMargin())

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
      |""".trimMargin())

  @Test
  fun `handle infix methods`() = assertFormatted(
      """
      |fun numbers() {
      |  (0 until 100).size
      |}
      |""".trimMargin())

  @Test
  fun `handle while loops`() = assertFormatted(
      """
      |fun numbers() {
      |  while (1 < 2) {
      |    println("Everything is okay")
      |  }
      |}
      |""".trimMargin())

  @Test
  fun `handle do while loops`() = assertFormatted(
      """
      |fun numbers() {
      |  do {
      |    println("Everything is okay")
      |  } while (1 < 2)
      |}
      |""".trimMargin())

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
      |""".trimMargin())

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
      |""".trimMargin())

  @Test
  @Ignore("This requires being able to reliably ignore tokens")
  fun `when imports or package have semicolons remove them`() {
    val code = """
      |package org.examples.wow;
      |import org.examples.wow.MuchWow;
      |import org.examples.wow.ManyAmaze
      |""".trimMargin()
    val expected = """
      |package org.examples.wow
      |import org.examples.wow.MuchWow
      |import org.examples.wow.ManyAmaze
      |""".trimMargin()
    assertThatFormatting(code).isEqualTo(expected)
  }

  @Test
  fun `handle optional semicolons until we can reliably remove them`() = assertFormatted(
      """
      |package com.example.bar;
      |
      |import com.example.Foo;
      |
      |typealias Int2 = Int;
      |
      |fun f() {
      |  val a = 5;
      |  a = 7;
      |  println(1);
      |  return 4;
      |};
      |""".trimMargin())

  @Test
  @Ignore("This requires being able to reliably ignore tokens")
  fun `remove unnecessary semicolons`() {
    val code = """
      |val a = 5;
      |fun foo() {
      |  println(1);
      |  println(2); println(3)
      |}
      |""".trimMargin()
    val expected = """
      |val a = 5
      |fun foo() {
      |  println(1)
      |  println(2)
      |  println(3)
      |}
      |""".trimMargin()
    assertThatFormatting(code).isEqualTo(expected)
  }

  @Test
  @Ignore("This requires being able to reliably ignore tokens")
  fun `remove unnecessary parenthesis in lambda calls`() {
    val code = """
      |fun f() {
      |  a() {
      |    println("a")
      |  }
      |}
      |""".trimMargin()
    val expected = """
      |fun f() {
      |  a {
      |    println("a")
      |  }
      |}
      |""".trimMargin()
    assertThatFormatting(code).isEqualTo(expected)
  }

  @Test
  fun `handle no parenthesis in lambda calls`() = assertFormatted(
      """
      |fun f() {
      |  a { println("a") }
      |}
      |""".trimMargin())

  @Test
  fun `handle multi statement lambdas`() = assertFormatted(
      """
      |fun f() {
      |  a {
      |    println("a")
      |    println("b")
      |  }
      |}
      |""".trimMargin())

  @Test
  fun `handle multi line one statement lambda`() = assertFormatted(
      """
      |fun f() {
      |  a {
      |    println(foo.bar.boom)
      |  }
      |}
      |""".trimMargin(), 25)

  @Test
  fun `statements are wrapped in blocks`() = assertFormatted(
      """
      |fun f() {
      |  builder.block {
      |    getArgumentName().accept
      |    return
      |  }
      |}
      |""".trimMargin())

  @Test
  fun `properly break fully qualified nested user types`() = assertFormatted(
      """
      |val complicated
      |    : com.example.interesting.SomeType<
      |    com.example.interesting.SomeType<Int, Nothing>,
      |    com.example.interesting.SomeType<
      |        com.example.interesting.SomeType<
      |            Int,
      |            Nothing>,
      |        Nothing>> =
      |    DUMMY
      |""".trimMargin(), 53)

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
      |    expression2.subjectExpression
      |        .let { subjectExp ->
      |          builder.token(")")
      |          return
      |        }
      |  }
      |}
      |""".trimMargin())

  @Test
  fun `handle multi line lambdas with explicit args`() = assertFormatted(
      """
      |fun f() {
      |  a { (x, y) ->
      |    x + y
      |  }
      |}
      |""".trimMargin(), 20)

  @Test
  fun `handle parenthesis in lambda calls for now`() = assertFormatted(
      """
      |fun f() {
      |  a() { println("a") }
      |}
      |""".trimMargin())

  @Test
  fun `handle chaining of calls with lambdas`() = assertFormatted(
      """
      |fun f() {
      |  bob.map { x -> x * x }
      |      .map { x -> x * x }
      |      ?.map { x ->
      |        val y = x * x
      |        y
      |      }
      |      .sum
      |}
      |""".trimMargin())

  @Test
  fun `handle reified types`() = assertFormatted(
      """
      |inline fun <reified T> foo(t: T) {
      |  println(t)
      |}
      |inline fun <reified in T> foo2(t: T) {
      |  println(t)
      |}
      |""".trimMargin())

  @Test
  fun `handle simple enum classes`() = assertFormatted(
      """
      |enum class BetterBoolean {
      |  TRUE,
      |  FALSE,
      |  FILE_NOT_FOUND,
      |}
      |""".trimMargin())

  @Test
  fun `handle enum class with functions`() = assertFormatted(
      """
      |enum class BetterBoolean {
      |  TRUE,
      |  FALSE,
      |  FILE_NOT_FOUND;
      |  fun isGood(): Boolean {
      |    return true
      |  }
      |}
      |""".trimMargin())

  @Test
  fun `handle enum with annotations`() = assertFormatted(
      """
      |enum class BetterBoolean {
      |  @True TRUE,
      |  @False @WhatIsTruth FALSE,
      |}
      |""".trimMargin())

  @Test
  fun `handle enum constructor calls`() = assertFormatted(
      """
      |enum class BetterBoolean(val name: String, val value: Boolean = true) {
      |  TRUE("true"),
      |  FALSE("false", false),
      |}
      |""".trimMargin())

  @Test
  fun `handle enum entries with body`() = assertFormatted(
      """
      |enum class Animal(canWalk: Boolean = true) {
      |  DOG {
      |    fun speak() = "woof"
      |  },
      |  FISH(false) {},
      |}
      |""".trimMargin())

  @Test
  fun `handle empty enum`() = assertFormatted(
      """
      |enum class YTho {
      |}
      |""".trimMargin())

  @Test
  fun `enum without trailing comma`() = assertFormatted(
      """
      |enum class Highlander {
      |  ONE
      |}
      |""".trimMargin())

  @Test
  fun `enum comma and semicolon`() = assertFormatted(
      """
      |enum class Highlander {
      |  ONE,;
      |}
      |""".trimMargin())

  @Test
  fun `handle varargs and spread operator`() = assertFormatted(
      """
      |fun foo(vararg args: String) {
      |  foo2(*args)
      |  foo3(options = *args)
      |}
      |""".trimMargin())

  @Test
  fun `handle typealias`() = assertFormatted(
      """
      |private typealias TextChangedListener =
      |    (string: String) -> Unit
      |typealias PairPair<X, Y> = Pair<Pair<X, Y>, X>
      |
      |class Foo
      |""".trimMargin(), 50)

  @Test
  fun `handle class expression with generics`() = assertFormatted(
      """
      |fun f() {
      |  println(Array<String>::class.java)
      |}
      |""".trimMargin())

  @Test
  fun `FormattingError contains correct line and column numbers`() {
    val code = """
      |// Foo
      |fun good() {
      |  //
      |}
      |
      |fn () { }
      |""".trimMargin()
    try {
      format(code)
    } catch (e: FormattingError) {
      assertThat(e.diagnostics()[0].line()).isEqualTo(6)
      assertThat(e.diagnostics()[0].column()).isEqualTo(6)
    }
  }

  @Test
  fun `fail() reports line+column number`() {
    val code = """
      |// Foo
      |fun good() {
      |  return@ 5
      |}
      |""".trimMargin()
    try {
      format(code)
    } catch (e: FormattingError) {
      assertThat(e.diagnostics()[0].line()).isEqualTo(3)
      assertThat(e.diagnostics()[0].column()).isEqualTo(10)
    }
  }

  @Test
  fun `handle annotations with use-site targets`() = assertFormatted(
      """
      |class FooTest {
      |  @get:Rule val exceptionRule: ExpectedException = ExpectedException.none()
      |  @set:Magic(name = "Jane")
      |  var field: String
      |}
      |""".trimMargin())

  @Test
  fun `handle annotations mixed with keywords since we cannot reorder them for now`() = assertFormatted(
      """
      |public @Magic final class Foo
      |public @Magic(1) final class Foo
      |@Magic(1)
      |public final class Foo
      |""".trimMargin())

  @Test
  fun `handle annotations more`() = assertFormatted(
      """
      |@Anno1
      |@Anno2(param = Param1::class)
      |@Anno3
      |@Anno4(param = Param2::class)
      |class MyClass {}
      |""".trimMargin())

  /** Verifies the given code passes through formatting, and stays the same at the end */
  private fun assertFormatted(code: String, maxWidth: Int = DEFAULT_MAX_WIDTH) {
    assertThatFormatting(code, maxWidth).isEqualTo(code)
  }

  fun assertThatFormatting(code: String, maxWidth: Int = DEFAULT_MAX_WIDTH): FormattedCodeSubject {
    fun codes(): Subject.Factory<FormattedCodeSubject, String> {
      return Subject.Factory { metadata, subject -> FormattedCodeSubject(metadata, subject, maxWidth) }
    }
    return assertAbout(codes()).that(code)
  }

  class FormattedCodeSubject(
      metadata: FailureMetadata,
      subject: String?,
      val maxWidth: Int
  ) :
      Subject<FormattedCodeSubject, String>(metadata, subject) {
    fun isEqualTo(expectedFormatting: String) {
      val code = actual()
      if (expectedFormatting.lines().any { it.endsWith(" ") }) {
        throw RuntimeException(
            "Expected code contains trailing whitespace, which the formatter will never output:\n" +
                expectedFormatting.lines().map { if (it.endsWith(" ")) "[$it]" else it }.joinToString("\n"))
      }
      val actualFormatting: String
      try {
        actualFormatting = format(code, maxWidth)
        if (actualFormatting != expectedFormatting) {
          reportError(code)
          println("# Output: ")
          println("#".repeat(20))
          println(actualFormatting)
          println()
        }
      } catch (e: Error) {
        reportError(code)
        throw e
      }
      assertThat(actualFormatting).isEqualTo(expectedFormatting)
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
}
