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

import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtContainerNodeForControlStructureBody
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtEnumEntry
import org.jetbrains.kotlin.psi.KtIfExpression
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtStringTemplateEntry
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.KtWhileExpression
import org.jetbrains.kotlin.psi.psiUtil.prevLeaf
import org.jetbrains.kotlin.psi.psiUtil.siblings
import org.jetbrains.kotlin.psi.psiUtil.getNextSiblingIgnoringWhitespaceAndComments
import org.jetbrains.kotlin.psi.psiUtil.getPrevSiblingIgnoringWhitespaceAndComments

internal class RedundantSemicolonDetector {
  private val extraSemicolons = mutableListOf<PsiElement>()

  fun getRedundantSemicolonElements(): List<PsiElement> = extraSemicolons

  /** returns **true** if this element was an extra comma, **false** otherwise. */
  fun takeElement(element: PsiElement, superBlock: () -> Unit) {
    if (isExtraSemicolon(element)) {
      extraSemicolons += element
    } else {
      superBlock.invoke()
    }
  }

  private fun isExtraSemicolon(element: PsiElement): Boolean {
    if (element.text != ";") {
      return false
    }
    val parent = element.parent
    if (parent is KtStringTemplateExpression || parent is KtStringTemplateEntry) {
      return false
    }
    if (parent is KtEnumEntry &&
        parent.siblings(forward = true, withItself = false).any { it is KtDeclaration }) {
      return false
    }
    val prevLeaf = element.prevLeaf(false)
    val prevConcreteSibling = element.getPrevSiblingIgnoringWhitespaceAndComments()
    if ((prevConcreteSibling is KtIfExpression || prevConcreteSibling is KtWhileExpression) &&
        prevLeaf is KtContainerNodeForControlStructureBody &&
        prevLeaf.text.isEmpty()) {
      return false
    }
    val nextConcreteSibling = element.getNextSiblingIgnoringWhitespaceAndComments()
    if (prevConcreteSibling is KtCallExpression && nextConcreteSibling is KtLambdaExpression) {
      return false // Example: `foo(0); { dead -> lambda }`
    }

    // do not remove `;` in `val a = 5; private set`
    val nextSibling = element.nextSibling
    if (nextSibling != null && '\n' !in nextSibling.text) {
      return false
    }

    return true
  }
}
