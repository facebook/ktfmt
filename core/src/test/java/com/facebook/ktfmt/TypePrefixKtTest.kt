// (c) Facebook, Inc. and its affiliates. Confidential and proprietary.

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
