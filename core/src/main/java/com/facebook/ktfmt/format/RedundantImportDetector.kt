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
import org.jetbrains.kotlin.kdoc.psi.impl.KDocImpl
import org.jetbrains.kotlin.kdoc.psi.impl.KDocLink
import org.jetbrains.kotlin.kdoc.psi.impl.KDocName
import org.jetbrains.kotlin.kdoc.psi.impl.KDocSection
import org.jetbrains.kotlin.kdoc.psi.impl.KDocTag
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.KtImportList
import org.jetbrains.kotlin.psi.KtPackageDirective
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.psi.psiUtil.getChildrenOfType

internal class RedundantImportDetector(val enabled: Boolean) {
  companion object {
    private val OPERATORS =
        setOf(
            // Unary prefix operators
            "unaryPlus",
            "unaryMinus",
            "not",
            // Increments and decrements
            "inc",
            "dec",
            // Arithmetic operators
            "plus",
            "minus",
            "times",
            "div",
            "rem",
            "mod", // deprecated
            "rangeTo",
            // 'In' operator
            "contains",
            // Indexed access operator
            "get",
            "set",
            // Invoke operator
            "invoke",
            // Augmented assignments
            "plusAssign",
            "minusAssign",
            "timesAssign",
            "divAssign",
            "remAssign",
            "modAssign", // deprecated
            // Equality and inequality operators
            "equals",
            // Comparison operators
            "compareTo",
            // Iterator operators
            "iterator",
            "next",
            "hasNext",
            // Bitwise operators
            "and",
            "or",
            // Property delegation operators
            "getValue",
            "setValue",
            "provideDelegate")

    private val COMPONENT_OPERATOR_REGEX = Regex("component\\d+")

    private val KDOC_TAG_SKIP_FIRST_REFERENCE_REGEX = Regex("^@(param|property) (.+)")
  }

  private var thisPackage: FqName? = null

  private val usedReferences = OPERATORS.toMutableSet()

  private lateinit var importCleanUpCandidates: Set<KtImportDirective>

  private var isPackageElement = false
  private var isImportElement = false

  fun takePackageDirective(directive: KtPackageDirective, superBlock: () -> Unit) {
    if (!enabled) {
      return superBlock.invoke()
    }

    thisPackage = directive.fqName

    isPackageElement = true
    superBlock.invoke()
    isPackageElement = false
  }

  fun takeImportList(importList: KtImportList, superBlock: () -> Unit) {
    if (!enabled) {
      return superBlock.invoke()
    }

    importCleanUpCandidates =
        importList.imports
            .filter { import ->
              import.isValidImport &&
                  !import.isAllUnder &&
                  import.identifier != null &&
                  requireNotNull(import.identifier) !in OPERATORS &&
                  !COMPONENT_OPERATOR_REGEX.matches(import.identifier.orEmpty())
            }
            .toSet()

    isImportElement = true
    superBlock.invoke()
    isImportElement = false
  }

  fun takeKdoc(kdoc: KDocImpl) {
    kdoc.getChildrenOfType<KDocSection>().forEach { kdocSection ->
      val tagLinks =
          kdocSection.getChildrenOfType<KDocTag>().flatMap { tag ->
            val tagLinks = tag.getChildrenOfType<KDocLink>().toList()
            when {
              KDOC_TAG_SKIP_FIRST_REFERENCE_REGEX.matches(tag.text) -> tagLinks.drop(1)
              else -> tagLinks
            }
          }

      val links = kdocSection.getChildrenOfType<KDocLink>() + tagLinks

      val references =
          links.flatMap { link ->
            link.getChildrenOfType<KDocName>().mapNotNull {
              it.getQualifiedName().firstOrNull()?.trim('[', ']')
            }
          }

      usedReferences += references
    }
  }

  fun takeReferenceExpression(expression: KtReferenceExpression) {
    if (!enabled) return

    if (!isPackageElement && !isImportElement && expression.children.isEmpty()) {
      usedReferences += expression.text.trim('`')
    }
  }

  fun getRedundantImportElements(): List<PsiElement> {
    if (!enabled) return emptyList()

    val redundantImports = mutableListOf<PsiElement>()

    // Collect unused imports
    for (import in importCleanUpCandidates) {
      val isUnused = import.aliasName !in usedReferences && import.identifier !in usedReferences
      val isFromSamePackage = import.importedFqName?.parent() == thisPackage && import.alias == null
      if (isUnused || isFromSamePackage) {
        redundantImports += import
      }
    }

    return redundantImports
  }

  private inline val KtImportDirective.identifier: String?
    get() = importPath?.importedName?.identifier?.trim('`')
}
