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

import com.facebook.ktfmt.testutil.assertFormatted
import com.facebook.ktfmt.testutil.assertThatFormatting
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/**
 * Tests for the Klimat style ([Formatter.KLIMAT_FORMAT]) and the options behind it:
 * [FormattingOptions.glueBlockLikeToOperator], [FormattingOptions.preserveChainBreaks] and
 * [FormattingOptions.compactClassHeader].
 */
@Suppress("FunctionNaming")
@RunWith(JUnit4::class)
class KlimatStyleFormatterTest {

  // region glueBlockLikeToOperator

  @Test
  fun `call with arguments and trailing lambda stays glued to the equals sign`() = assertFormatted(
      """
      |fun f() {
      |    val b = remember(isShake) {
      |        computeSomething()
      |    }
      |}
      |"""
          .trimMargin(),
      formattingOptions = Formatter.KLIMAT_FORMAT,
  )

  @Test
  fun `call with type arguments and trailing lambda stays glued to the equals sign`() = assertFormatted(
      """
      |fun f() {
      |    val c = remember<Int> {
      |        computeSomething()
      |    }
      |}
      |"""
          .trimMargin(),
      formattingOptions = Formatter.KLIMAT_FORMAT,
  )

  @Test
  fun `when initializer stays glued to the equals sign`() = assertFormatted(
      """
      |fun f() {
      |    val mode = when (input) {
      |        is A -> 1
      |        else -> 2
      |    }
      |}
      |"""
          .trimMargin(),
      formattingOptions = Formatter.KLIMAT_FORMAT,
  )

  @Test
  fun `braced if initializer stays glued to the equals sign`() = assertFormatted(
      """
      |fun f() {
      |    val fillMode = if (isEnabled) {
      |        breakMode
      |    } else {
      |        noBreakMode
      |    }
      |}
      |"""
          .trimMargin(),
      formattingOptions = Formatter.KLIMAT_FORMAT,
  )

  @Test
  fun `braceless if initializer still breaks after the equals sign when too long`() = assertFormatted(
      """
      |//////////////////////////////
      |fun f() {
      |    val x =
      |        if (c) aaaa else bbb
      |}
      |"""
          .trimMargin(),
      formattingOptions = Formatter.KLIMAT_FORMAT,
      deduceMaxWidth = true,
  )

  @Test
  fun `property delegate with arguments and trailing lambda stays glued to the by keyword`() = assertFormatted(
      """
      |fun f() {
      |    val x by remember(key) {
      |        mutableStateOf(1)
      |    }
      |}
      |"""
          .trimMargin(),
      formattingOptions = Formatter.KLIMAT_FORMAT,
  )

  @Test
  fun `function expression body stays glued to the equals sign`() = assertFormatted(
      """
      |fun g() = buildList(capacity) {
      |    add(1)
      |}
      |"""
          .trimMargin(),
      formattingOptions = Formatter.KLIMAT_FORMAT,
  )

  @Test
  fun `assignment statement stays glued to the equals sign`() = assertFormatted(
      """
      |fun f() {
      |    state.value = remember(key) {
      |        compute()
      |    }
      |}
      |"""
          .trimMargin(),
      formattingOptions = Formatter.KLIMAT_FORMAT,
  )

  @Test
  fun `named argument value stays glued to the equals sign`() = assertFormatted(
      """
      |fun f() {
      |    Surface(
      |        modifier = remember(key) {
      |            compute()
      |        },
      |    ) {
      |        content()
      |    }
      |}
      |"""
          .trimMargin(),
      formattingOptions = Formatter.KLIMAT_FORMAT,
  )

  @Test
  fun `qualified factory call with trailing comma stays glued to the equals sign`() = assertFormatted(
      """
      |fun f() {
      |    val component = componentFactory.create(
      |        context = context,
      |        router = router,
      |    )
      |}
      |"""
          .trimMargin(),
      formattingOptions = Formatter.KLIMAT_FORMAT,
  )

  @Test
  fun `nested class constructor call with trailing comma stays glued to the equals sign`() = assertFormatted(
      """
      |fun f() {
      |    val homeScreen = HomeComponent.Screen(
      |        origin = input.origin,
      |        isOnboardScreenShown = shown,
      |    )
      |}
      |"""
          .trimMargin(),
      formattingOptions = Formatter.KLIMAT_FORMAT,
  )

  @Test
  fun `qualified factory call stays glued inside a property declaration with override`() = assertFormatted(
      """
      |class C(factory: Factory) {
      |    override val noticeComponent = noticeComponentFactory.create(
      |        componentContext = childContext("notice_component"),
      |        callbacks = MainNoticeCallbacks(rootNavigator.router),
      |    )
      |}
      |"""
          .trimMargin(),
      formattingOptions = Formatter.KLIMAT_FORMAT,
  )

  @Test
  fun `when branch qualified call with trailing comma stays glued to the arrow`() = assertFormatted(
      """
      |fun f() {
      |    when (screen) {
      |        is ChannelScreen -> channelComponentFactory.create(
      |            context = componentContext.childContext(screen.toString()),
      |            router = router,
      |        )
      |
      |        else -> null
      |    }
      |}
      |"""
          .trimMargin(),
      formattingOptions = Formatter.KLIMAT_FORMAT,
  )

  @Test
  fun `when branch call with trailing lambda stays glued to the arrow`() = assertFormatted(
      """
      |fun f() {
      |    when (x) {
      |        1 -> launch {
      |            doSomething()
      |        }
      |
      |        else -> Unit
      |    }
      |}
      |"""
          .trimMargin(),
      formattingOptions = Formatter.KLIMAT_FORMAT,
  )

  @Test
  fun `glueBlockLikeToOperator disabled keeps the default layout`() = assertThatFormatting(
      """
      |fun f() {
      |    val b = remember(isShake) {
      |        computeSomething()
      |    }
      |}
      |"""
          .trimMargin(),
  )
      .withOptions(Formatter.KOTLINLANG_FORMAT)
      .isEqualTo(
          """
          |fun f() {
          |    val b =
          |        remember(isShake) {
          |            computeSomething()
          |        }
          |}
          |"""
              .trimMargin(),
      )

  // endregion

  // region preserveChainBreaks

  @Test
  fun `multiline chain is not joined even when it would fit`() = assertFormatted(
      """
      |fun f() {
      |    val m = modifier
      |        .rotate(rotation)
      |        .scale(scale)
      |}
      |"""
          .trimMargin(),
      formattingOptions = Formatter.KLIMAT_FORMAT,
  )

  @Test
  fun `single line chain stays on one line`() = assertFormatted(
      """
      |fun f() {
      |    val m = modifier.rotate(rotation).scale(scale)
      |}
      |"""
          .trimMargin(),
      formattingOptions = Formatter.KLIMAT_FORMAT,
  )

  @Test
  fun `attached chain prefix stays attached`() = assertFormatted(
      """
      |fun f() {
      |    val list = ImmutableList.newBuilder()
      |        .add(1)
      |        .build()
      |}
      |"""
          .trimMargin(),
      formattingOptions = Formatter.KLIMAT_FORMAT,
  )

  @Test
  fun `chain breaks and trailing comma are preserved inside named arguments`() = assertFormatted(
      """
      |fun f() {
      |    Surface(
      |        modifier = modifier
      |            .rotate(rotation)
      |            .scale(scale),
      |    ) {
      |        content()
      |    }
      |}
      |"""
          .trimMargin(),
      formattingOptions = Formatter.KLIMAT_FORMAT,
  )

  @Test
  fun `preserveChainBreaks disabled joins short multiline chains`() = assertThatFormatting(
      """
      |fun f() {
      |    val m = modifier
      |        .rotate(rotation)
      |        .scale(scale)
      |}
      |"""
          .trimMargin(),
  )
      .withOptions(Formatter.KOTLINLANG_FORMAT)
      .isEqualTo(
          """
          |fun f() {
          |    val m = modifier.rotate(rotation).scale(scale)
          |}
          |"""
              .trimMargin(),
      )

  // endregion

  // region compactClassHeader

  @Test
  fun `annotated primary constructor stays glued to the class name`() = assertFormatted(
      """
      |internal class AiCellComponentImpl @AssistedInject constructor(
      |    @Assisted componentContext: ComponentContext,
      |    @Assisted private val router: Router,
      |    private val aiRepository: AiRepository,
      |) : ComponentContext by componentContext,
      |    AiCellComponentInternal,
      |    StateComponent<State> by StateComponent(State()) {
      |
      |    fun f() = Unit
      |}
      |"""
          .trimMargin(),
      formattingOptions = Formatter.KLIMAT_FORMAT,
  )

  @Test
  fun `primary constructor with visibility modifier stays glued to the class name`() = assertFormatted(
      """
      |class Foo private constructor(
      |    val a: A,
      |) {
      |
      |    fun f() = Unit
      |}
      |"""
          .trimMargin(),
      formattingOptions = Formatter.KLIMAT_FORMAT,
  )

  @Test
  fun `supertypes authored on one line stay on one line`() = assertFormatted(
      """
      |class Foo(
      |    private val a: A,
      |) : Base(), Iface {
      |
      |    fun f() = Unit
      |}
      |"""
          .trimMargin(),
      formattingOptions = Formatter.KLIMAT_FORMAT,
  )

  @Test
  fun `authored breaks between supertypes are preserved`() = assertFormatted(
      """
      |class Foo(
      |    private val a: A,
      |) : Base(),
      |    Iface {
      |
      |    fun f() = Unit
      |}
      |"""
          .trimMargin(),
      formattingOptions = Formatter.KLIMAT_FORMAT,
  )

  @Test
  fun `overflowing supertype list keeps the first entry glued to the colon`() = assertThatFormatting(
      """
      |class Foo(val a: A) : FirstBaseInterface, SecondBaseInterface {
      |
      |    fun f() = Unit
      |}
      |"""
          .trimMargin(),
  )
      .withOptions(Formatter.KLIMAT_FORMAT.copy(maxWidth = 50))
      .isEqualTo(
          """
          |class Foo(val a: A) : FirstBaseInterface,
          |    SecondBaseInterface {
          |
          |    fun f() = Unit
          |}
          |"""
              .trimMargin(),
      )

  @Test
  fun `compactClassHeader disabled keeps the default exploded layout`() = assertThatFormatting(
      """
      |class VeryLongComponentImplementationName @AssistedInject constructor(
      |    private val firstDependency: FirstDependency,
      |    private val secondDependency: SecondDependency,
      |) : ComponentContext by componentContext,
      |    FirstVeryLongComponentInterface,
      |    SecondVeryLongComponentInterface {
      |
      |    fun f() = Unit
      |}
      |"""
          .trimMargin(),
  )
      .withOptions(Formatter.KOTLINLANG_FORMAT)
      .isEqualTo(
          """
          |class VeryLongComponentImplementationName
          |@AssistedInject
          |constructor(
          |    private val firstDependency: FirstDependency,
          |    private val secondDependency: SecondDependency,
          |) :
          |    ComponentContext by componentContext,
          |    FirstVeryLongComponentInterface,
          |    SecondVeryLongComponentInterface {
          |
          |    fun f() = Unit
          |}
          |"""
              .trimMargin(),
      )

  // endregion
}
