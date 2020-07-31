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
import org.jetbrains.kotlin.kdoc.psi.impl.KDocImpl
import org.jetbrains.kotlin.kdoc.psi.impl.KDocLink
import org.jetbrains.kotlin.kdoc.psi.impl.KDocName
import org.jetbrains.kotlin.kdoc.psi.impl.KDocSection
import org.jetbrains.kotlin.kdoc.psi.impl.KDocTag
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtContainerNodeForControlStructureBody
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtEnumEntry
import org.jetbrains.kotlin.psi.KtIfExpression
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.KtImportList
import org.jetbrains.kotlin.psi.KtPackageDirective
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.psi.KtStringTemplateEntry
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.psi.KtWhileExpression
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.getChildrenOfType
import org.jetbrains.kotlin.psi.psiUtil.prevLeaf
import org.jetbrains.kotlin.psi.psiUtil.siblings
import org.jetbrains.kotlin.psi.psiUtil.startOffset

/** Removes elements that are not needed in the code, such as semicolons and unused imports. */
object RedundantElementRemover {
  /** Remove extra semicolons and unused imports, if enabled in the [options] */
  fun dropRedundantElements(code: String, options: FormattingOptions): String {
    val file = Parser.parse(code)
    val redundantImportDetector = RedundantImportDetector(enabled = options.removeUnusedImports)
    val redundantCommaDetector = RedundantCommaDetector()

    file.accept(
        object : KtTreeVisitorVoid() {
          override fun visitElement(element: PsiElement) {
            if (element is KDocImpl) {
              redundantImportDetector.takeKdoc(element)
            } else {
              redundantCommaDetector.takeElement(element) { super.visitElement(element) }
            }
          }

          override fun visitPackageDirective(directive: KtPackageDirective) {
            redundantImportDetector.takePackageDirective(directive) {
              super.visitPackageDirective(directive)
            }
          }

          override fun visitImportList(importList: KtImportList) {
            redundantImportDetector.takeImportList(importList) { super.visitImportList(importList) }
          }

          override fun visitReferenceExpression(expression: KtReferenceExpression) {
            redundantImportDetector.takeReferenceExpression(expression)
            super.visitReferenceExpression(expression)
          }
        })

    val result = StringBuilder(code)
    val elementsToRemove =
        redundantCommaDetector.getRedundantCommaElements() +
            redundantImportDetector.getRedundantImportElements()

    for (element in elementsToRemove.sortedByDescending(PsiElement::endOffset)) {
      result.replace(element.startOffset, element.endOffset, "")
    }

    return result.toString()
  }
}

private class RedundantCommaDetector {
  private val extraCommas = mutableListOf<PsiElement>()

  fun getRedundantCommaElements(): List<PsiElement> = extraCommas

  /** returns **true** if this element was an extra comma, **false** otherwise. */
  fun takeElement(element: PsiElement, superBlock: () -> Unit) {
    if (isExtraSemicolon(element)) {
      extraCommas += element
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
    val prevSibling = element.prevSibling
    if ((prevSibling is KtIfExpression || prevSibling is KtWhileExpression) &&
        prevLeaf is KtContainerNodeForControlStructureBody &&
        prevLeaf.text.isEmpty()) {
      return false
    }
    return true
  }
}

private class RedundantImportDetector(val enabled: Boolean) {
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
