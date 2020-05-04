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

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtQualifiedExpression
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@Suppress("FunctionNaming")
@RunWith(JUnit4::class)
class TypePrefixKtTest {

  @Test
  fun `when first few names look like package names return proper count`() {
    val expression = getQualifiedExpression("com.facebook.ktfmt.Formatter.doIt()")
    assertEquals(3, getTypePrefixLength(expression))
  }

  @Test
  fun `when a chain of field calls return 0`() {
    val expression = getQualifiedExpression("myObject.field.anotherField")
    assertEquals(0, getTypePrefixLength(expression))
  }

  @Test
  fun `when starts with possible class name return 0`() {
    val expression = getQualifiedExpression("Formatter.doIt()")
    assertEquals(0, getTypePrefixLength(expression))
  }

  fun getQualifiedExpression(code: String): KtQualifiedExpression {
    var result: PsiElement? = null
    Parser.parse("fun f() { $code }")
        .accept(
            object : KtTreeVisitorVoid() {
              override fun visitElement(element: PsiElement) {
                if (element.text == code && result == null) {
                  result = element
                  return
                }
                super.visitElement(element)
              }
            })

    return result as KtQualifiedExpression
  }
}
