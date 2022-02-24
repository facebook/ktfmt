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

import java.util.ArrayDeque
import org.jetbrains.kotlin.psi.KtArrayAccessExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtPostfixExpression
import org.jetbrains.kotlin.psi.KtQualifiedExpression
import org.jetbrains.kotlin.psi.KtReferenceExpression

/**
 * Structure representing a fluent callchain in a way closer to how humans perceive it.
 *
 * This is an intermediary stage between PSI (compiler friendly) and Docs (formatter friendly). For
 * example, the following code samples are natrually expressed as [FluentChain]s:
 *
 * ```
 * qualifiedLongRoot
 *   ?.nameOnLine[a]++
 *   .nameOnLine[a]!!
 *   .nameOnLine!![a]
 *   ?.nameOnLine!!(a)
 *   .nameOnLine(a)[a](a)
 *   .nameOnLine(a)!!(a)
 *   ?.moreStuff
 *
 * qualified.root(a).tailCallFits(
 *   but = args,
 *   would = not,
 * )
 *
 * my.pkg.Class[1].static(a)
 *   .tailDoes
 *   .notFit(
 *     and = args,
 *     would = notEither,
 *   )
 * ```
 */
data class FluentChain
private constructor(
    val root: KtExpression,
    val staticLinkCount: Int,
    val tailCallLink: Link?,
    val links: List<Link>,
) {

  data class Link(
      val dot: KtQualifiedExpression,
      val name: KtNameReferenceExpression,
      val operators: List<Link.Operator>,
  ) {

    sealed interface Operator {
      @JvmInline value class Call(val call: KtCallExpression) : Operator
      @JvmInline value class Postfix(val postfix: KtPostfixExpression) : Operator
      @JvmInline value class ArrayAccess(val arrayAccess: KtArrayAccessExpression) : Operator
    }

    fun asSimpleCall(): KtCallExpression? {
      if (operators.size != 1) return null
      val op = operators[0]
      if (op !is Operator.Call) return null
      return op.call
    }
  }

  companion object {
    fun tryExtract(expr: KtExpression): FluentChain? {
      val links = ArrayDeque<Link>()

      var curr = expr
      while (true) {
        val link = tryExtractLink(curr) ?: break
        links.addFirst(link)
        curr = link.dot.receiverExpression
      }

      if (links.isEmpty()) return null

      var tailCallLink = links.last()
      if (tailCallLink.asSimpleCall() == null) {
        tailCallLink = null
      } else {
        links.removeLast()
      }

      val staticLinkCount =
          if (curr is KtReferenceExpression) {
            val typenameParts = mutableListOf(curr.text)
            for (link in links) {
              typenameParts.add(link.name.text)
            }
            TypeNameClassifier.typePrefixLength(typenameParts).orElse(0)
          } else {
            0
          }

      return FluentChain(curr, staticLinkCount, tailCallLink, links.toList())
    }

    private fun tryExtractLink(expr: KtExpression): Link? {
      val operators = ArrayDeque<Link.Operator>()

      var curr: KtExpression? = expr
      while (true) {
        when (curr) {
          is KtCallExpression -> {
            operators.addFirst(Link.Operator.Call(curr))
            curr = curr.calleeExpression
          }
          is KtPostfixExpression -> {
            operators.addFirst(Link.Operator.Postfix(curr))
            curr = curr.baseExpression
          }
          is KtArrayAccessExpression -> {
            operators.addFirst(Link.Operator.ArrayAccess(curr))
            curr = curr.arrayExpression
          }
          is KtQualifiedExpression -> {
            var selector = curr.selectorExpression
            if (selector is KtCallExpression) {
              operators.addFirst(Link.Operator.Call(selector))
              selector = selector.calleeExpression
            }

            check(selector is KtNameReferenceExpression) { selector ?: "<null>" }
            return Link(curr, selector, operators.toList())
          }
          else -> return null
        }
      }
    }
  }
}
