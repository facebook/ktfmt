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
import org.jetbrains.kotlin.psi.KtContainerNodeForControlStructureBody
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtEnumEntry
import org.jetbrains.kotlin.psi.KtIfExpression
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtStringTemplateEntry
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.KtWhileExpression
import org.jetbrains.kotlin.psi.psiUtil.getNextSiblingIgnoringWhitespaceAndComments
import org.jetbrains.kotlin.psi.psiUtil.getPrevSiblingIgnoringWhitespaceAndComments
import org.jetbrains.kotlin.psi.psiUtil.prevLeaf
import org.jetbrains.kotlin.psi.psiUtil.siblings

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
    if (nextConcreteSibling is KtLambdaExpression) {
      /**
       * Example: `val x = foo(0) ; { dead -> lambda }`
       *
       * There are a huge number of cases here because the trailing lambda syntax is so flexible.
       * Therefore, we just assume that all semicolons followed by lambdas are meaningful. The cases
       * where they could be removed are too rare to justify the risk of changing behaviour.
       */
      return false
    }

    return true
  }
}
