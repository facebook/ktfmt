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

import com.facebook.ktfmt.kdoc.KDocCommentsHelper
import com.google.common.collect.ImmutableList
import com.google.common.collect.Range
import com.google.googlejavaformat.Doc
import com.google.googlejavaformat.DocBuilder
import com.google.googlejavaformat.Newlines
import com.google.googlejavaformat.OpsBuilder
import com.google.googlejavaformat.java.JavaOutput
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.util.text.StringUtilRt
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
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
import org.jetbrains.kotlin.psi.psiUtil.prevLeaf
import org.jetbrains.kotlin.psi.psiUtil.siblings
import org.jetbrains.kotlin.psi.psiUtil.startOffset

const val DEFAULT_MAX_WIDTH: Int = 100

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

class FormattingOptions(
    /** ktfmt breaks lines longer than maxWidth. */
    val maxWidth: Int = DEFAULT_MAX_WIDTH,

    /**
     * blockIndent is the size of the indent used when a new block is opened, in spaces.
     *
     * For example,
     * ```
     * fun f() {
     *   //
     * }
     * ```
     */
    val blockIndent: Int = 2,

    /**
     * continuationIndent is the size of the indent used when a line is broken because it's too
     * long, in spaces.
     *
     * For example,
     * ```
     * val foo = bar(
     *     1)
     * ```
     */
    val continuationIndent: Int = 4,

    /** Whether ktfmt should remove imports that are not used. */
    val removeUnusedImports: Boolean = false
) {
  companion object {
    /** Represents dropbox style formatting. */
    fun dropboxStyle(): FormattingOptions =
        FormattingOptions(blockIndent = 4, continuationIndent = 4)
  }
}

/**
 * format formats the Kotlin code given in 'code' and returns it as a string. This method is
 * accessed through Reflection.
 */
fun format(code: String): String = format(FormattingOptions(), code)

/**
 * format formats the Kotlin code given in 'code' with the 'maxWidth' and returns it as a string.
 */
fun format(options: FormattingOptions, code: String): String {
  checkWhitespaceTombstones(code)

  val lfCode = StringUtilRt.convertLineSeparators(code)
  val sortedImports = sortedAndDistinctImports(lfCode)
  val pretty = prettyPrint(sortedImports, options, "\n")
  val noRedundantElements = dropRedundantElements(pretty, options)
  return prettyPrint(noRedundantElements, options, Newlines.guessLineSeparator(code)!!)
}

/** prettyPrint reflows 'code' using google-java-format's engine. */
private fun prettyPrint(code: String, options: FormattingOptions, lineSeparator: String): String {
  val file = Parser.parse(code)
  val kotlinInput = KotlinInput(code, file)
  val javaOutput = JavaOutput(lineSeparator, kotlinInput, KDocCommentsHelper(lineSeparator))
  val builder = OpsBuilder(kotlinInput, javaOutput)
  file.accept(KotlinInputAstVisitor(options.blockIndent, options.continuationIndent, builder))
  builder.sync(kotlinInput.text.length)
  builder.drain()
  val doc = DocBuilder().withOps(builder.build()).build()
  doc.computeBreaks(javaOutput.commentsHelper, options.maxWidth, Doc.State(+0, 0))
  doc.write(javaOutput)
  javaOutput.flush()

  val tokenRangeSet =
      kotlinInput.characterRangesToTokenRanges(ImmutableList.of(Range.closedOpen(0, code.length)))
  return replaceTombstoneWithTrailingWhitespace(
      JavaOutput.applyReplacements(code, javaOutput.getFormatReplacements(tokenRangeSet)))
}

fun dropRedundantElements(code: String, options: FormattingOptions): String {
  val file = Parser.parse(code)
  val toRemove = mutableListOf<PsiElement>()

  var thisPackage: FqName? = null
  lateinit var importCleanUpCandidates: Set<KtImportDirective>
  val usedReferences = OPERATORS.toMutableSet()
  file.accept(
      object : KtTreeVisitorVoid() {
        private var isPackageElement = false
        private var isImportElement = false

        override fun visitElement(el: PsiElement) {
          if (isExtraSemicolon(el)) {
            toRemove += el
          } else {
            super.visitElement(el)
          }
        }

        override fun visitPackageDirective(directive: KtPackageDirective) {
          thisPackage = directive.fqName

          isPackageElement = true
          super.visitPackageDirective(directive)
          isPackageElement = false
        }

        override fun visitImportList(importList: KtImportList) {
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
          super.visitImportList(importList)
          isImportElement = false
        }

        override fun visitReferenceExpression(expression: KtReferenceExpression) {
          if (!isPackageElement && !isImportElement && expression.children.isEmpty()) {
            usedReferences += expression.text.trim('`')
          }
          super.visitReferenceExpression(expression)
        }

        private fun isExtraSemicolon(el: PsiElement): Boolean {
          if (el.text != ";") {
            return false
          }
          val parent = el.parent
          if (parent is KtStringTemplateExpression || parent is KtStringTemplateEntry) {
            return false
          }
          if (parent is KtEnumEntry &&
              parent.siblings(forward = true, withItself = false).any { it is KtDeclaration }) {
            return false
          }
          val prevLeaf = el.prevLeaf(false)
          val prevSibling = el.prevSibling
          if ((prevSibling is KtIfExpression || prevSibling is KtWhileExpression) &&
              prevLeaf is KtContainerNodeForControlStructureBody &&
              prevLeaf.text.isEmpty()) {
            return false
          }
          return true
        }
      })

  val result = StringBuilder(code)

  if (options.removeUnusedImports) {
    // Collect unused imports
    for (import in importCleanUpCandidates) {
      val isUnused = import.aliasName !in usedReferences && import.identifier !in usedReferences
      val isFromSamePackage = import.importedFqName?.parent() == thisPackage && import.alias == null
      if (isUnused || isFromSamePackage) {
        toRemove += import
      }
    }
  }

  for (element in toRemove.sortedByDescending(PsiElement::endOffset)) {
    result.replace(element.startOffset, element.endOffset, "")
  }

  return result.toString()
}

private fun checkWhitespaceTombstones(code: String) {
  val index = code.indexOfWhitespaceTombstone()
  if (index != -1) {
    throw ParseError(
        "ktfmt does not support code which contains a \\u0003 character; escape it",
        StringUtil.offsetToLineColumn(code, index))
  }
}

fun sortedAndDistinctImports(code: String): String {
  val file = Parser.parse(code)

  val importList = file.importList ?: return code

  fun findNonImportElement(): PsiElement? {
    var element = importList.firstChild
    while (element != null) {
      if (element !is KtImportDirective && element !is PsiWhiteSpace) {
        return element
      }
      element = element.nextSibling
    }
    return null
  }

  val nonImportElement = findNonImportElement()
  if (nonImportElement != null) {
    throw ParseError(
        "Imports not contiguous (perhaps a comment separates them?): " + nonImportElement.text,
        StringUtil.offsetToLineColumn(code, nonImportElement.startOffset))
  }
  fun canonicalText(importDirective: KtImportDirective) =
      importDirective.importedFqName?.asString() +
          " " +
          importDirective.alias?.text?.replace("`", "") +
          " " +
          if (importDirective.isAllUnder) "*" else ""
  val sortedImports = importList.imports.sortedBy(::canonicalText).distinctBy(::canonicalText)

  return code.replaceRange(
      importList.startOffset,
      importList.endOffset,
      sortedImports.joinToString(separator = "\n") { imprt -> imprt.text })
}

private inline val KtImportDirective.identifier: String?
  get() = importPath?.importedName?.identifier?.trim('`')
