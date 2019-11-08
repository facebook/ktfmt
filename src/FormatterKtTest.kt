package com.google.googlejavaformat.kotlin

import com.google.common.truth.FailureMetadata
import com.google.common.truth.Subject
import com.google.common.truth.Truth.assertAbout
import com.google.common.truth.Truth.assertThat
import org.junit.Test

import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class FormatterKtTest {

  @Test
  fun `kitchen sink of tests`() {
    // Don't add more tests here
    val code = """
fun 
f  (
a : Int  
 , b: Double , c:String) {           var result = 0
 val aVeryLongLongLongLongLongLongLongLongLongLongLongLongLongLongLongLongLongLongLongLongLongLongVar = 43
          foo.bar.zed.accept(
             
          )
          
          foo(
             
          )
          
          foo.bar.zed.accept(
                DoSomething.bar()             
          )
          
          bar(
          ImmutableList.newBuilder().add(1).add(1).add(1).add(1).add(1).add(1).add(1).add(1).add(1).add(1).build())
          

          ImmutableList.newBuilder().add(1).add(1).add(1).add(1).add(1).add(1).add(1).add(1).add(1).add(1).build()
     }        
""".trimIndent()

    val expected = """
        fun f(a: Int, b: Double, c: String) {
            var result = 0
            val
                aVeryLongLongLongLongLongLongLongLongLongLongLongLongLongLongLongLongLongLongLongLongLongLongVar =
                    43
            foo.bar.zed.accept()
        
            foo()
        
            foo.bar.zed.accept(DoSomething.bar())
        
            bar(
                ImmutableList
                    .newBuilder()
                    .add(1)
                    .add(1)
                    .add(1)
                    .add(1)
                    .add(1)
                    .add(1)
                    .add(1)
                    .add(1)
                    .add(1)
                    .add(1)
                    .build())
        
            ImmutableList
                .newBuilder()
                .add(1)
                .add(1)
                .add(1)
                .add(1)
                .add(1)
                .add(1)
                .add(1)
                .add(1)
                .add(1)
                .add(1)
                .build()
        }
        """.trimIndent()

    assertThatFormatting(code).isEqualTo(expected)
    // Don't add more tests here
  }

  @Test
  fun `spacing around variable declarations`() = assertFormatted(
      """
      |fun f() {
      |    var x: Int = 4
      |    val y = 0
      |}
        """.trimMargin())

  @Test
  fun `class without a body nor properties`() = assertFormatted("class Foo")

  @Test
  fun `interface without a body nor properties`() = assertFormatted("interface Foo")

  @Test
  fun `preserve empty primary constructor`() = assertFormatted("class Foo()")

  @Test
  fun `class without a body, with explicit ctor params`() =
      assertFormatted("class Foo(a: Int, var b: Double, val c: String)")

  @Test
  fun `class with a body and explicit ctor params`() = assertFormatted(
      """
      |class Foo(a: Int, var b: Double, val c: String) {
      |    val x = 2
      |    fun method() {}
      |    class Bar
      |}
        """.trimMargin())

  @Test
  fun `properties and fields with modifiers`() = assertFormatted(
      """
      |class Foo(public val p1: Int, private val p2: Int, open val p3: Int, final val p4: Int) {
      |    private var f1 = 0
      |    public var f2 = 0
      |    open var f3 = 0
      |    final var f4 = 0
      |}
        """.trimMargin())

  @Test
  fun `properties with multiple modifiers`() = assertFormatted(
      """
      |class Foo(public open inner val p1: Int) {
      |    public open inner var f2 = 0
      |}
        """.trimMargin())

  @Test
  fun `spaces around binary operations`() = assertFormatted(
      """
      |fun foo() {
      |    a = 5
      |    x + 1
      |}
        """.trimMargin())

  @Test
  fun `properties with accessors`() = assertFormatted(
      """
      |class Foo {
      |    var x: Int
      |        get() = field
      |    var y: Boolean
      |        get() = x.equals(123)
      |        set(value) {
      |            field = value
      |        }
      |    var z: Boolean
      |        get() {
      |            x.equals(123)
      |        }
      |    var zz = false
      |        private set
      |}
        """.trimMargin())

  @Test
  fun `multi-character unary and binary operators such as ==`() = assertFormatted(
      """
      |fun f() {
      |    3 == 4
      |    true && false
      |    a++
      |    a === b
      |}
        """.trimMargin())

  @Test
  fun `package names stay in one line`() {
    val code = """
      | package  com  .likeuberbutfordogs. sitterapp
      |
      |fun f() = 1
        """.trimMargin()
    val expected = """
      |package com.likeuberbutfordogs.sitterapp
      |
      |fun f() = 1
        """.trimMargin()

    assertThatFormatting(code).isEqualTo(expected)
  }

  @Test
  fun `safe dot operator expression`() = assertFormatted(
      """
      |fun f() {
      |    node?.name
      |}
        """.trimMargin())

  @Test
  fun `safe dot operator expression with normal`() = assertFormatted(
      """
      |fun f() {
      |    node?.name.hello
      |}
        """.trimMargin())

  @Test
  fun `import list`() {
    val code = """
      | import  com .google.common.truth. FailureMetadata
      |  import  com .google.common.truth. FailureMetadata2  as  testFailure
      |   import com .google.common.truth. *
      | import  foo.bar // Test
      |  import  abc.def /*
      |                  test */
        """.trimMargin()
    val expected = """
      |import com.google.common.truth.FailureMetadata
      |import com.google.common.truth.FailureMetadata2 as testFailure
      |import com.google.common.truth.*
      |import foo.bar // Test
      |import abc.def /*
      |               test */
        """.trimMargin()
    assertThatFormatting(code).isEqualTo(expected)
  }

  @Test
  fun `basic annotations`() = assertFormatted(
      """
      |@Fancy class Foo {
      |    @Fancy fun baz(@Fancy foo: Int) {
      |        @Fancy 1
      |    }
      |}
        """.trimMargin())

  @Test
  fun `function calls with multiple arguments`() = assertFormatted(
      """
      |fun f() {
      |    foo(1, 2, 3)
      |
      |    foo(
      |        123456789012345678901234567890,
      |        123456789012345678901234567890,
      |        123456789012345678901234567890)
      |}
        """.trimMargin())

  @Test
  fun `function calls with multiple named arguments`() = assertFormatted(
      """
      |fun f() {
      |    foo(1, b = 2, c = 3)
      |
      |    foo(
      |        123456789012345678901234567890,
      |        b = 23456789012345678901234567890,
      |        c = 3456789012345678901234567890)
      |}
        """.trimMargin())

  @Test
  fun `when() with a subject expression`() = assertFormatted(
      """
      |fun f(x: Int) {
      |    when (x) {
      |        1 -> print(1)
      |        2 -> print(2)
      |        else -> {
      |            print(3)
      |        }
      |    }
      |}
        """.trimMargin())

  @Test
  fun `when() expression with complex predicates`() = assertFormatted(
      """
      |fun f(x: Int) {
      |    when {
      |        x == 1 || x == 2 -> print(1)
      |        x == 3 && x != 4 -> print(2)
      |        else -> {
      |            print(3)
      |        }
      |    }
      |}
        """.trimMargin())

  @Test
  fun `when() expression with several conditions`() = assertFormatted(
      """
      |fun f(x: Int) {
      |    when {
      |        0, 1 -> print(1)
      |        else -> print(0)
      |    }
      |}
        """.trimMargin())

  @Test
  fun `when() expression with is and in`() = assertFormatted(
      """
      |fun f(x: Int) {
      |    when (x) {
      |        is String -> print(1)
      |        !is String -> print(2)
      |        in 1..3 -> print()
      |        in a..b -> print()
      |        in a..3 -> print()
      |        in 1..b -> print()
      |        !in 1..b -> print()
      |        else -> print(3)
      |    }
      |}
        """.trimMargin())

  @Test
  fun `function return types`() = assertFormatted(
      """
      |fun f1(): Int = 0
      |fun f2(): Int {}
        """.trimMargin())

  @Test
  fun `list of superclasses`() = assertFormatted(
      """
      |class Derived2 : Super1, Super2 {}
      |class Derived1 : Super1, Super2
      |class Derived3(a: Int) : Super1(a)
      |class Derived4 : Super1()
      |class Derived5 : Super3<Int>()
        """.trimMargin())

  @Test
  fun `annotations with parameters`() = assertFormatted(
      """
      |@AnnWithArrayValue(1, 2, 3) class C
        """.trimMargin())

  @Test
  fun `method modifiers`() = assertFormatted(
      """
      |override internal fun f() {}
        """.trimMargin())

  @Test
  fun `class modifiers`() = assertFormatted(
      """
      |abstract class Foo
      |inner class Foo
      |final class Foo
      |open class Foo
        """.trimMargin())

  @Test
  fun `javadoc comments`() {
    val code = """
      |/** 
      | * foo 
      | */ class F {
      | 
      | }
        """.trimMargin()
    val expected = """
      |/** foo */
      |class F {}
        """.trimMargin()
    assertThatFormatting(code).isEqualTo(expected)
  }

  @Test
  fun `return statement with value`() = assertFormatted(
      """
      |fun random(): Int {
      |    return 4
      |}
        """.trimMargin())

  @Test
  fun `return statement without value`() = assertFormatted(
      """
      |fun print(b: Boolean) {
      |    print(b)
      |    return
      |}
        """.trimMargin())

  @Test
  fun `if statement without else`() = assertFormatted(
      """
      |fun maybePrint(b: Boolean) {
      |    if (b) {
      |        println(b)
      |    }
      |}
        """.trimMargin())

  @Test
  fun `if statement with else`() = assertFormatted(
      """
      |fun maybePrint(b: Boolean) {
      |    if (b) {
      |        println(2)
      |    } else {
      |        println(1)
      |    }
      |}
        """.trimMargin())

  @Test
  fun `if expression with else`() = assertFormatted(
      """
      |fun maybePrint(b: Boolean) {
      |    println(if (b) 1 else 2)
      |    println(
      |        if (b) {
      |            val a = 1 + 1
      |            2 * a
      |        } else 2)
      |    return if (b) 1 else 2
      |}
        """.trimMargin())

  @Test
  fun `A program that tickled a bug in KotlinInput`() = assertFormatted(
      """
      |val x = 2
        """.trimMargin())

  @Test
  fun `a few variations of constructors`() = assertFormatted(
      """
      |class Foo constructor(number: Int) {}
      |class Foo2 private constructor(number: Int) {}
      |class Foo3 @Inject constructor(number: Int) {}
      |class Foo4 @Inject private constructor(number: Int) {}
        """.trimMargin())

  @Test
  fun `handle objects`() = assertFormatted(
      """
      |object Foo(n: Int) {}
        """.trimMargin())

  @Test
  fun `handle object expression`() = assertFormatted(
      """
      |fun f(): Any {
      |    return object : MouseAdapter() {}
      |}
        """.trimMargin())

  @Test
  fun `handle array indexing operator`() = assertFormatted(
      """
      |fun f(a: Magic) {
      |    a[3]
      |    b[3, 4]
      |}
        """.trimMargin())


  @Test
  fun `handle destructuring declaration`() = assertFormatted(
      """
      |fun f() {
      |    val (a, b: Int) = listOf(1, 2)
      |}
        """.trimMargin())

  @Test
  fun `handle ? for nullalble types`() = assertFormatted(
      """
      |fun doItWithNullReturns(a: String, b: String): Int? {
      |    return 5
      |}
      |fun doItWithNulls(a: String, b: String?) {}
        """.trimMargin())

  @Test
  fun `handle string literals`() = assertFormatted(
      """
      |fun doIt(world: String) {
      |    println("Hello world!")
      |    println("Hello! ${'$'}world")
      |    println("Hello! ${'$'}{"wor" + "ld"}")
      |}
        """.trimMargin())

  @Test
  fun `when there is an expression in a template string it gets formatted accordingly`() {
    val code = """
      |fun doIt() {
      |    println("Hello! ${'$'}{"wor"+"ld"}")
      |}
        """.trimMargin()
    val expected = """
      |fun doIt() {
      |    println("Hello! ${'$'}{"wor" + "ld"}")
      |}
        """.trimMargin()
    assertThatFormatting(code).isEqualTo(expected)
  }

  @Test
  fun `handle multiline string literals`() = assertFormatted(
      """
      |fun doIt(world: String) {
      |    println(${"\"".repeat(3)}Hello
      |          world!${"\"".repeat(3)})
      |}
        """.trimMargin())

  @Test
  fun `handle some basic generics scenarios`() = assertFormatted(
      """
      |fun <T> doIt(a: List<T>): List<Int>? {
      |    val b: List<Int> = convert<Int>(listOf(5, 4))
      |    return b
      |}
      |class Foo<T>
        """.trimMargin())

  @Test
  fun `handle for loops`() = assertFormatted(
      """
      |fun f(a: List<Int>) {
      |    for (i in a.indices) {
      |        println(i)
      |    }
      |}
        """.trimMargin())

  @Test
  fun `Qualified type`() = assertFormatted(
      """
      |fun f() {
      |    var plusFour: Indent.Const
      |    var x: Map.Entry<String, Integer>
      |    var x: List<String>.Iterator
      |}
    """.trimMargin())

  @Test
  fun `handle destructuring declaration in for loop`() = assertFormatted(
      """
      |fun f(a: List<Pair<Int, Int>>) {
      |    for ((x, y: Int) in a) {}
      |}
        """.trimMargin())

  @Test
  fun `handle scope operator`() = assertFormatted(
      """
      |fun f(a: List<Int>) {
      |    a.forEach(::println)
      |    a.map(Int::toString)
      |}
        """.trimMargin())

  @Test
  fun `handle escaped identifier`() = assertFormatted(
      """
      |import foo as `foo foo`
      |import org.mockito.Mockito.`when` as `yay yay`
      |fun `spaces in functions`() {
      |    val `when` = NEVER
      |    val (`do not`, `ever write`) = SERIOUSLY
      |    val `a a`: Int
      |}
      |class `more spaces`
        """.trimMargin())

  @Test
  fun `handle annotations with arguments`() = assertFormatted(
      """
      |@Px fun f(): Int = 5
      |@Dimenstion(unit = DP) fun g(): Int = 5
      |@RunWith(MagicRunner::class) class Test
        """.trimMargin())

  @Test
  fun `Unary expressions`() = assertFormatted(
      """
      |fun f() {
      |    !a
      |    -4
      |    val x = -foo()
      |    +4
      |    ++a
      |    --a
      |}
      """.trimMargin())

  @Test
  fun `handle wildcard generics`() = assertFormatted(
      """
      |fun f() {
      |    val l: List<*>
      |    val p: Pair<*, *>
      |}
      """.trimMargin())

  @Test
  fun `handle covariant and contravariant type arugments`() = assertFormatted(
      """
      |val p: Pair<in T, out S>
      """.trimMargin())

  @Test
  fun `handle covariant and contravariant type parameters`() = assertFormatted(
      """
      |class Foo<in T, out S>
      """.trimMargin())

  @Test
  fun `handle bounds for type parameters`() = assertFormatted(
      """
      |class Foo<in T : List<*>, out S : Any?>
      """.trimMargin())

  /** Verifies the given code passes through formatting, and stays the same at the end */
  fun assertFormatted(code: String) {
    assertThatFormatting(code).isEqualTo(code)
  }

  @Test
  fun `handle method calls with lambda arg only`() = assertFormatted(
      """
      |fun f() {
      |    val a =
      |        g {
      |            1 + 1
      |        }
      |}
      """.trimMargin())

  @Test
  fun `handle method calls value args and a lambda arg`() = assertFormatted(
      """
      |fun f() {
      |    val a =
      |        g(1, 2) {
      |            1 + 1
      |        }
      |}
      """.trimMargin())

  @Test
  fun `handle top level constants`() = assertFormatted(
      """
      |val a = 5
      |const val b = 6
      """.trimMargin())

  @Test
  fun `handle lambda arg with named arguments`() = assertFormatted(
      """
      |fun f() {
      |    val b =
      |        { x: Int, y: Int ->
      |            x + y
      |        }
      |}
      """.trimMargin())

  @Test
  fun `handle labeled this pointer`() = assertFormatted(
      """
      |class Foo {
      |    fun f() {
      |        g {
      |            println(this@Foo)
      |        }
      |    }
      |}
      """.trimMargin())

  @Test
  fun `handle extension and operator functions`() = assertFormatted(
      """
      |operator fun Point.component1() = x
      """.trimMargin())

  @Test
  fun `handle file annotations`() = assertFormatted(
      """
      |@file:JvmName("RemoteFiles")
      |package com.facebook.example
      |
      |import com.facebook.example2
      |class Foo {}
      """.trimMargin())

  @Test
  fun `handle init block`() = assertFormatted(
      """
      |class Foo {
      |    init {
      |        println("Init!")
      |    }
      |}
      """.trimMargin())

  @Test
  fun `handle property delegation`() = assertFormatted(
      """
      |val a by lazy {
      |        1 + 1
      |    }
      """.trimMargin())

  @Test
  fun `handle lambda types`() = assertFormatted(
      """
      |val listener1: (Boolean) -> Unit =
      |    { b ->
      |        !b
      |    }
      |val listener2: () -> Unit = {}
      |val listener3: (Int, Double) -> Int =
      |    { a, b ->
      |        a
      |    }
      |val listener4: Int.(Int, Boolean) -> Unit
      """.trimMargin())

  @Test
  fun `handle unicode in string literals`() = assertFormatted(
      """
      |val a = "\uD83D\uDC4D"
      """.trimMargin())

  @Test
  fun `handle casting`() = assertFormatted(
      """
      |fun castIt(o: Object) {
      |    println(o is Double)
      |    println(o !is Double)
      |    doIt(o as Int)
      |    doIt(o as? Int)
      |}
      """.trimMargin())

  @Test
  fun `handle colleciton literals in annotations`() = assertFormatted(
      """
      |@Foo(a = [1, 2]) fun doIt(o: Object) {}
      """.trimMargin())

  @Test
  fun `handle try, catch and finally`() = assertFormatted(
      """
      |fun foo() {
      |    try {
      |        bar()
      |    } catch (e: Exception) {
      |        throw e
      |    } finally {
      |        println("finally")
      |    }
      |}
      """.trimMargin())

  @Test
  fun `handle infix methods`() = assertFormatted(
      """
      |fun numbers() {
      |    (0 until 100).size
      |}
      """.trimMargin())

  @Test
  fun `handle while loops`() = assertFormatted(
      """
      |fun numbers() {
      |    while (1 < 2) {
      |        println("Everything is okay")
      |    }
      |}
      """.trimMargin())

  @Test
  fun `handle do while loops`() = assertFormatted(
      """
      |fun numbers() {
      |    do {
      |        println("Everything is okay")
      |    } while (1 < 2)
      |}
      """.trimMargin())

  @Test
  fun `handle break and continue`() = assertFormatted(
      """
      |fun numbers() {
      |    while (1 < 2) {
      |        if (true) {
      |            break
      |        }
      |        if (false) {
      |            continue
      |        }
      |    }
      |}
      """.trimMargin())

  @Test
  fun `handle all kinds of labels and jumps`() = assertFormatted(
      """
      |fun f(a: List<Int>) {
      |    a
      |        .map {
      |        myloop@ for (i in a) {
      |            if (true) {
      |                break@myloop
      |            } else if (false) {
      |                continue@myloop
      |            } else {
      |                a
      |                    .map `inner map`@ {
      |                    return@`inner map`
      |                }
      |            }
      |        }
      |        return@map 2 * it
      |    }
      |}
      """.trimMargin())

  @Test
  fun `when imports or package have semicolons remove them`() {
    val code = """
      |package org.examples.wow;
      |import org.examples.wow.MuchWow;
      |import org.examples.wow.ManyAmaze
        """.trimMargin()
    val expected = """
      |package org.examples.wow
      |import org.examples.wow.MuchWow
      |import org.examples.wow.ManyAmaze
        """.trimMargin()
    assertThatFormatting(code).isEqualTo(expected)
  }

  @Test
  fun `remove unnecessary parenthesis in lambda calls`() {
    val code = """
      |fun f() {
      |    a() {
      |        println("a")
      |    }
      |}
      """.trimMargin()
    val expected = """
      |fun f() {
      |    a {
      |        println("a")
      |    }
      |}
      """.trimMargin()
    assertThatFormatting(code).isEqualTo(expected)
  }

  @Test
  fun `handle reified types`() = assertFormatted(
      """
      |inline fun <reified T> foo(t: T) {
      |    println(t)
      |}
      |inline fun <reified in T> foo2(t: T) {
      |    println(t)
      |}
      """.trimMargin())

  fun assertThatFormatting(code: String): FormattedCodeSubject {
    fun codes(): Subject.Factory<FormattedCodeSubject, String> {
      return Subject.Factory { metadata, subject -> FormattedCodeSubject(metadata, subject) }
    }
    return assertAbout(codes()).that(code)
  }

  class FormattedCodeSubject(metadata: FailureMetadata, subject: String?) :
      Subject<FormattedCodeSubject, String>(metadata, subject) {
    fun isEqualTo(expectedFormatting: String) {
      val code = actual()
      try {
        val actualFormatting = format(code)
        if (actualFormatting != expectedFormatting) {
          reportError(code)
        }
        assertThat(actualFormatting).isEqualTo(expectedFormatting)
      } catch (e: Error) {
        reportError(code)
        throw e
      }
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
      println()
    }
  }
}
