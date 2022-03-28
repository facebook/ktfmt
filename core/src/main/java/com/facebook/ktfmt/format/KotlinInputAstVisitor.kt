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

import com.google.common.base.Throwables
import com.google.common.collect.ImmutableList
import com.google.googlejavaformat.Doc
import com.google.googlejavaformat.FormattingError
import com.google.googlejavaformat.Indent
import com.google.googlejavaformat.Indent.Const.ZERO
import com.google.googlejavaformat.OpsBuilder
import com.google.googlejavaformat.Output
import com.google.googlejavaformat.Output.BreakTag
import java.util.ArrayDeque
import java.util.Optional
import org.jetbrains.kotlin.com.intellij.psi.PsiComment
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtAnnotatedExpression
import org.jetbrains.kotlin.psi.KtAnnotation
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtAnnotationUseSiteTarget
import org.jetbrains.kotlin.psi.KtArrayAccessExpression
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtBinaryExpressionWithTypeRHS
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtBreakExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtCallableReferenceExpression
import org.jetbrains.kotlin.psi.KtCatchClause
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassInitializer
import org.jetbrains.kotlin.psi.KtClassLiteralExpression
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtCollectionLiteralExpression
import org.jetbrains.kotlin.psi.KtConstantExpression
import org.jetbrains.kotlin.psi.KtConstructorDelegationCall
import org.jetbrains.kotlin.psi.KtContinueExpression
import org.jetbrains.kotlin.psi.KtDelegatedSuperTypeEntry
import org.jetbrains.kotlin.psi.KtDestructuringDeclaration
import org.jetbrains.kotlin.psi.KtDestructuringDeclarationEntry
import org.jetbrains.kotlin.psi.KtDoWhileExpression
import org.jetbrains.kotlin.psi.KtDynamicType
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtEnumEntry
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFileAnnotationList
import org.jetbrains.kotlin.psi.KtFinallySection
import org.jetbrains.kotlin.psi.KtForExpression
import org.jetbrains.kotlin.psi.KtFunctionType
import org.jetbrains.kotlin.psi.KtIfExpression
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.KtImportList
import org.jetbrains.kotlin.psi.KtIsExpression
import org.jetbrains.kotlin.psi.KtLabelReferenceExpression
import org.jetbrains.kotlin.psi.KtLabeledExpression
import org.jetbrains.kotlin.psi.KtLambdaArgument
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtModifierList
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtNullableType
import org.jetbrains.kotlin.psi.KtPackageDirective
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtParameterList
import org.jetbrains.kotlin.psi.KtParenthesizedExpression
import org.jetbrains.kotlin.psi.KtPrefixExpression
import org.jetbrains.kotlin.psi.KtPrimaryConstructor
import org.jetbrains.kotlin.psi.KtProjectionKind
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import org.jetbrains.kotlin.psi.KtPropertyDelegate
import org.jetbrains.kotlin.psi.KtQualifiedExpression
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.psi.KtReturnExpression
import org.jetbrains.kotlin.psi.KtScript
import org.jetbrains.kotlin.psi.KtSecondaryConstructor
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.KtSuperExpression
import org.jetbrains.kotlin.psi.KtSuperTypeCallEntry
import org.jetbrains.kotlin.psi.KtSuperTypeList
import org.jetbrains.kotlin.psi.KtThisExpression
import org.jetbrains.kotlin.psi.KtThrowExpression
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.psi.KtTryExpression
import org.jetbrains.kotlin.psi.KtTypeAlias
import org.jetbrains.kotlin.psi.KtTypeArgumentList
import org.jetbrains.kotlin.psi.KtTypeConstraint
import org.jetbrains.kotlin.psi.KtTypeConstraintList
import org.jetbrains.kotlin.psi.KtTypeParameter
import org.jetbrains.kotlin.psi.KtTypeParameterList
import org.jetbrains.kotlin.psi.KtTypeProjection
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.psi.KtUnaryExpression
import org.jetbrains.kotlin.psi.KtUserType
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.psi.KtValueArgumentList
import org.jetbrains.kotlin.psi.KtWhenConditionInRange
import org.jetbrains.kotlin.psi.KtWhenConditionIsPattern
import org.jetbrains.kotlin.psi.KtWhenConditionWithExpression
import org.jetbrains.kotlin.psi.KtWhenExpression
import org.jetbrains.kotlin.psi.KtWhileExpression
import org.jetbrains.kotlin.psi.psiUtil.children
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.psi.psiUtil.startsWithComment

/** An AST visitor that builds a stream of {@link Op}s to format. */
class KotlinInputAstVisitor(
    private val options: FormattingOptions,
    private val builder: OpsBuilder
) : KtTreeVisitorVoid() {

  private val isGoogleStyle = options.style == FormattingOptions.Style.GOOGLE

  /** Standard indentation for a block */
  private val blockIndent: Indent.Const = Indent.Const.make(options.blockIndent, 1)

  /**
   * Standard indentation for a long expression or function call, it is different than block
   * indentation on purpose
   */
  private val expressionBreakIndent: Indent.Const = Indent.Const.make(options.continuationIndent, 1)

  private val blockPlusExpressionBreakIndent: Indent.Const =
      Indent.Const.make(options.blockIndent + options.continuationIndent, 1)

  private val doubleExpressionBreakIndent: Indent.Const =
      Indent.Const.make(options.continuationIndent, 2)

  private val expressionBreakNegativeIndent: Indent.Const =
      Indent.Const.make(-options.continuationIndent, 1)

  /** A record of whether we have visited into an expression. */
  private val inExpression = ArrayDeque(ImmutableList.of(false))

  /** Tracks whether we are handling an import directive */
  private var inImport = false

  /**
   * Represents a logical "chunk" of [expressions], and whether or not they should be kept on the
   * same line
   *
   * As an example, consider the expression:
   * ```
   *     rainbow.red.orange.shine().yellow
   * ```
   * This might be split into chunks as such:
   * ```
   *     chunks: [
   *         chunk(expressions=[rainbow, red, orange, shine()], shouldKeepOnSameLine=true),
   *         chunk(expressions=[yellow], shouldKeepOnSameLine=false)
   *     ]
   * ```
   */
  data class Chunk(
      val expressions: List<KtExpression>,
      val shouldKeepOnSameLine: Boolean,
  )

  /** Example: `fun foo(n: Int) { println(n) }` */
  override fun visitNamedFunction(function: KtNamedFunction) {
    builder.sync(function)
    builder.block(ZERO) {
      visitFunctionLikeExpression(
          function.modifierList,
          "fun",
          function.typeParameterList,
          function.receiverTypeReference,
          function.nameIdentifier?.text,
          true,
          function.valueParameterList,
          function.typeConstraintList,
          function.bodyBlockExpression,
          function.bodyExpression,
          function.typeReference,
          function.bodyBlockExpression?.lBrace != null)
    }
  }

  /** Example `Int`, `(String)` or `() -> Int` */
  override fun visitTypeReference(typeReference: KtTypeReference) {
    builder.sync(typeReference)
    // Normally we'd visit the children nodes through accessors on 'typeReference', and  we wouldn't
    // loop over children.
    // But, in this case the modifier list can either be inside the parenthesis:
    // ... (@Composable (x) -> Unit)
    // or outside of them:
    // ... @Composable ((x) -> Unit)
    val modifierList = typeReference.modifierList
    val typeElement = typeReference.typeElement
    for (child in typeReference.node.children()) {
      when {
        child.psi == modifierList -> visit(modifierList)
        child.psi == typeElement -> visit(typeElement)
        child.elementType == KtTokens.LPAR -> builder.token("(")
        child.elementType == KtTokens.RPAR -> builder.token(")")
      }
    }
  }

  override fun visitDynamicType(type: KtDynamicType) {
    builder.token("dynamic")
  }

  /** Example: `String?` or `((Int) -> Unit)?` */
  override fun visitNullableType(nullableType: KtNullableType) {
    builder.sync(nullableType)
    val innerType = nullableType.innerType
    val addParenthesis = innerType is KtFunctionType
    if (addParenthesis) {
      builder.token("(")
    }
    visit(nullableType.modifierList)
    visit(innerType)
    if (addParenthesis) {
      builder.token(")")
    }
    builder.token("?")
  }

  /** Example: `String` or `List<Int>`, */
  override fun visitUserType(type: KtUserType) {
    builder.sync(type)

    if (type.qualifier != null) {
      visit(type.qualifier)
      builder.token(".")
    }
    visit(type.referenceExpression)
    val typeArgumentList = type.typeArgumentList
    if (typeArgumentList != null) {
      builder.block(expressionBreakIndent) { visit(typeArgumentList) }
    }
  }

  /** Example `<Int, String>` in `List<Int, String>` */
  override fun visitTypeArgumentList(typeArgumentList: KtTypeArgumentList) {
    builder.sync(typeArgumentList)
    builder.block(ZERO) {
      builder.token("<")
      builder.breakOp(Doc.FillMode.UNIFIED, "", ZERO)
      builder.block(ZERO) {
        emitParameterLikeList(
            typeArgumentList.arguments, typeArgumentList.trailingComma != null, wrapInBlock = true)
      }
    }
    builder.token(">")
  }

  override fun visitTypeProjection(typeProjection: KtTypeProjection) {
    builder.sync(typeProjection)
    val typeReference = typeProjection.typeReference
    when (typeProjection.projectionKind) {
      KtProjectionKind.IN -> {
        builder.token("in")
        builder.space()
        visit(typeReference)
      }
      KtProjectionKind.OUT -> {
        builder.token("out")
        builder.space()
        visit(typeReference)
      }
      KtProjectionKind.STAR -> builder.token("*")
      KtProjectionKind.NONE -> visit(typeReference)
    }
  }

  /**
   * @param keyword e.g., "fun" or "class".
   * @param typeOrDelegationCall for functions, the return typeOrDelegationCall; for classes, the
   * list of supertypes.
   */
  private fun visitFunctionLikeExpression(
      modifierList: KtModifierList?,
      keyword: String,
      typeParameters: KtTypeParameterList?,
      receiverTypeReference: KtTypeReference?,
      name: String?,
      emitParenthesis: Boolean,
      parameterList: KtParameterList?,
      typeConstraintList: KtTypeConstraintList?,
      bodyBlockExpression: KtBlockExpression?,
      nonBlockBodyExpressions: KtExpression?,
      typeOrDelegationCall: KtElement?,
      emitBraces: Boolean
  ) {
    builder.block(ZERO) {
      if (modifierList != null) {
        visitModifierList(modifierList)
      }
      builder.token(keyword)
      if (typeParameters != null) {
        builder.space()
        builder.block(ZERO) { visit(typeParameters) }
      }

      if (name != null || receiverTypeReference != null) {
        builder.space()
      }
      builder.block(ZERO) {
        if (receiverTypeReference != null) {
          visit(receiverTypeReference)
          builder.breakOp(Doc.FillMode.INDEPENDENT, "", expressionBreakIndent)
          builder.token(".")
        }
        if (name != null) {
          builder.token(name)
        }
      }
      if (emitParenthesis) {
        builder.token("(")
      }
      var paramBlockNeedsClosing = false
      builder.block(ZERO) {
        if (parameterList != null && parameterList.parameters.isNotEmpty()) {
          paramBlockNeedsClosing = true
          builder.open(expressionBreakIndent)
          builder.breakOp(Doc.FillMode.UNIFIED, "", expressionBreakIndent)
          visit(parameterList)
        }
        if (emitParenthesis) {
          if (parameterList != null && parameterList.parameters.isNotEmpty()) {
            builder.breakOp(Doc.FillMode.UNIFIED, "", expressionBreakNegativeIndent)
          }
          builder.token(")")
        } else {
          if (paramBlockNeedsClosing) {
            builder.close()
          }
        }
        if (typeOrDelegationCall != null) {
          builder.block(ZERO) {
            if (typeOrDelegationCall is KtConstructorDelegationCall) {
              builder.space()
            }
            builder.token(":")
            if (parameterList?.parameters.isNullOrEmpty()) {
              builder.breakOp(Doc.FillMode.INDEPENDENT, " ", expressionBreakIndent)
              builder.block(expressionBreakIndent) { visit(typeOrDelegationCall) }
            } else {
              builder.space()
              builder.block(expressionBreakNegativeIndent) { visit(typeOrDelegationCall) }
            }
          }
        }
      }
      if (paramBlockNeedsClosing) {
        builder.close()
      }
      if (typeConstraintList != null) {
        builder.space()
        visit(typeConstraintList)
      }
      if (bodyBlockExpression != null) {
        builder.space()
        visitBlockBody(bodyBlockExpression, emitBraces)
      } else if (nonBlockBodyExpressions != null) {
        builder.space()
        builder.block(ZERO) {
          builder.token("=")
          if (isLambdaOrScopingFunction(nonBlockBodyExpressions)) {
            visitLambdaOrScopingFunction(nonBlockBodyExpressions)
          } else {
            builder.block(expressionBreakIndent) {
              builder.breakOp(Doc.FillMode.INDEPENDENT, " ", ZERO)
              builder.block(ZERO) { visit(nonBlockBodyExpressions) }
            }
          }
        }
      }
      builder.guessToken(";")
    }
    if (name != null) {
      builder.forcedBreak()
    }
  }

  private fun genSym(): Output.BreakTag {
    return Output.BreakTag()
  }

  private fun visitBlockBody(bodyBlockExpression: PsiElement, emitBraces: Boolean) {
    if (emitBraces) {
      builder.token("{", Doc.Token.RealOrImaginary.REAL, blockIndent, Optional.of(blockIndent))
    }
    val statements = bodyBlockExpression.children
    if (statements.isNotEmpty()) {
      builder.block(blockIndent) {
        builder.forcedBreak()
        builder.blankLineWanted(OpsBuilder.BlankLineWanted.PRESERVE)
        visitStatements(statements)
      }
      builder.forcedBreak()
      builder.blankLineWanted(OpsBuilder.BlankLineWanted.NO)
    }
    if (emitBraces) {
      builder.token("}", blockIndent)
    }
  }

  private fun visitStatement(statement: PsiElement) {
    builder.block(ZERO) { visit(statement) }
    builder.guessToken(";")
  }

  private fun visitStatements(statements: Array<PsiElement>) {
    var first = true
    builder.guessToken(";")
    for (statement in statements) {
      builder.forcedBreak()
      if (!first) {
        builder.blankLineWanted(OpsBuilder.BlankLineWanted.PRESERVE)
      }
      first = false
      visitStatement(statement)
    }
  }

  override fun visitProperty(property: KtProperty) {
    builder.sync(property)
    builder.block(ZERO) {
      declareOne(
          kind = DeclarationKind.FIELD,
          modifiers = property.modifierList,
          valOrVarKeyword = property.valOrVarKeyword.text,
          typeParameters = property.typeParameterList,
          receiver = property.receiverTypeReference,
          name = property.nameIdentifier?.text,
          type = property.typeReference,
          typeConstraintList = property.typeConstraintList,
          delegate = property.delegate,
          initializer = property.initializer,
          accessors = property.accessors)
    }
    builder.guessToken(";")
    if (property.parent !is KtWhenExpression) {
      builder.forcedBreak()
    }
  }

  /**
   * Example: "com.facebook.bla.bla" in imports or "a.b.c.d" in expressions.
   *
   * There's a few cases that are different. We deal with imports by keeping them on the same line.
   * For regular chained expressions we go the left most descendant so we can start indentation only
   * before the first break (a `.` or `?.`), and keep the seem indentation for this chain of calls.
   */
  override fun visitQualifiedExpression(expression: KtQualifiedExpression) {
    builder.sync(expression)
    val receiver = expression.receiverExpression
    if (inImport) {
      visit(receiver)
      val selectorExpression = expression.selectorExpression
      if (selectorExpression != null) {
        builder.token(".")
        visit(selectorExpression)
      }
      return
    }

    if (receiver is KtWhenExpression || receiver is KtStringTemplateExpression) {
      builder.block(ZERO) {
        visit(receiver)
        builder.token(expression.operationSign.value)
        visit(expression.selectorExpression)
      }
      return
    }

    emitQualifiedExpression(breakIntoChunks(expression))
  }

  /**
   * Decomposes a qualified expression into chunks.
   *
   * So this expression:
   * ```
   *     rainbow.red.orange.shine().yellow
   * ```
   *
   * Becomes:
   * ```
   *     chunks: [
   *         chunk(expressions=[rainbow, red, orange, shine()], shouldKeepOnSameLine=true),
   *         chunk(expressions=[yellow], shouldKeepOnSameLine=false)
   *     ]
   * ```
   */
  private fun breakIntoChunks(expression: KtExpression): List<Chunk> {
    val parts = breakIntoParts(expression)

    val prefixes = mutableSetOf<Int>()

    // Check if the dot chain has a prefix that looks like a type name, so we can
    // treat the type name-shaped part as a single syntactic unit.
    TypeNameClassifier.typePrefixLength(simpleNames(parts)).ifPresent { prefixes.add(it) }

    val invocationCount = parts.count { it.isCallExpression() }
    val firstInvocationIndex = parts.indexOfFirst { it.isCallExpression() }
    val isFirstInvocationLambda = parts.getOrNull(firstInvocationIndex)?.isLambda() ?: false
    val hasTrailingLambda = parts.last().isLambda()

    // If there's only one invocation, treat leading field accesses as a single
    // unit. In the normal case we want to preserve the alignment of subsequent
    // method calls, and would emit e.g.:
    //
    // myField
    //     .foo()
    //     .bar();
    //
    // But if there's no 'bar()' to worry about the alignment of we prefer:
    //
    // myField.foo();
    //
    // to:
    //
    // myField
    //     .foo();
    //
    val singleInvocation = invocationCount == 1

    // For this case, don't count trailing lambdas as call expressions so they look like:
    // ```
    // blah.foo().bar().map {
    //   // blah
    // }
    // ```
    //
    val singleInvocationWithTrailingLambda = invocationCount == 2 && hasTrailingLambda

    if ((singleInvocation || singleInvocationWithTrailingLambda) && firstInvocationIndex > 0) {
      prefixes.add(
          if (firstInvocationIndex != parts.size - 1 && isFirstInvocationLambda) {
            firstInvocationIndex - 1
          } else {
            firstInvocationIndex
          })
    }

    // keep `super` and `this` attached to the first dereference
    if (prefixes.isEmpty() &&
        (parts.first() is KtSuperExpression || parts.first() is KtThisExpression)) {
      prefixes.add(1)
    }

    // now that we've found the prefixes, break the parts into chunks
    val chunks = mutableListOf<Chunk>()
    val currentChunk = mutableListOf<KtExpression>()
    val unconsumedPrefixes = ArrayDeque(prefixes.sorted())

    parts.forEachIndexed { index, part ->
      currentChunk.add(part)
      if (!unconsumedPrefixes.isEmpty() && index == unconsumedPrefixes.peekFirst()) {
        unconsumedPrefixes.removeFirst()
        chunks.add(Chunk(currentChunk.toList(), shouldKeepOnSameLine = true))
        currentChunk.clear()
      }
    }

    // the last chunk is part of a prefix, so it's not grouped
    if (currentChunk.isNotEmpty()) {
      chunks.add(Chunk(currentChunk.toList(), shouldKeepOnSameLine = false))
    }

    return chunks
  }

  /**
   * Decomposes a qualified expression into parts, so `rainbow.red.orange.yellow` becomes `[rainbow,
   * rainbow.red, rainbow.red.orange, rainbow.orange.yellow]`
   */
  private fun breakIntoParts(expression: KtExpression): List<KtExpression> {
    val parts = ArrayDeque<KtExpression>()

    // use an ArrayDeque and add elements to the beginning so the innermost expression comes first
    parts.addFirst(expression)

    var node = expression
    while (node is KtQualifiedExpression) {
      node = node.receiverExpression
      parts.addFirst(node)
    }

    return parts.toList()
  }

  /** Returns true if the expression represents an invocation */
  private fun KtExpression.isCallExpression(): Boolean {
    return extractCallExpression(this) != null
  }

  /** Returns true if the expression represents an invocation that is also a lambda */
  private fun KtExpression.isLambda(): Boolean {
    return extractCallExpression(this)?.lambdaArguments?.isNotEmpty() ?: false
  }

  /**
   * emitQualifiedExpression formats call expressions that are either part of a qualified
   * expression, or standing alone. This method makes it easier to handle both cases uniformly.
   */
  private fun extractCallExpression(expression: KtExpression): KtCallExpression? {
    val ktExpression = (expression as? KtQualifiedExpression)?.selectorExpression ?: expression
    return ktExpression as? KtCallExpression
  }

  /**
   * Returns the simple names of expressions in a "." chain, e.g., "foo.bar().zed[5]" --> [foo, bar,
   * zed]
   */
  private fun simpleNames(stack: List<KtExpression>): List<String> {
    val simpleNames = mutableListOf<String>()
    loop@ for (expression in stack) {
      val callExpression = extractCallExpression(expression)
      if (callExpression != null) {
        callExpression.calleeExpression?.text?.let { simpleNames.add(it) }
        break@loop
      }
      when (expression) {
        is KtQualifiedExpression -> expression.selectorExpression?.let { simpleNames.add(it.text) }
        is KtReferenceExpression -> simpleNames.add(expression.text)
        else -> break@loop
      }
    }
    return simpleNames
  }

  /**
   * Output a chain of dereferences, some of which should be grouped together.
   *
   * Example 1:
   * ```
   * field1.field2.field3.field4
   *     .method1(...)
   * ```
   *
   * Example 2:
   * ```
   * com.facebook.ktfmt.KotlinInputAstVisitor
   *     .method1()
   * ```
   */
  private fun emitQualifiedExpression(
      chunks: List<Chunk>,
  ) {
    // Keeps track of how much text we've emitted so far, and is used to avoid line breaks that are
    // shorter than the indentation length. For example:
    // ```
    //    user.field1
    //        .field2
    //          ...
    //        .field9
    // ```
    // Since `user` is less than or equal to 4 characters (the indentation length here), `field1` is
    // kept on the same line.
    var textLength = 0

    // is the last expression a lambda? e.g.`foo.bar.apply { ... }`
    val hasTrailingLambda = chunks.last().expressions.last().isLambda()

    // When the last chunk is meant to be on one line, reduce the indentation of arguments:
    // ```
    // rainbow.shine(
    //     infrared,
    //     ultraviolet,
    // )
    // ```
    // Here's an example of the line being broken, so we don't reduce the indentation:
    // ```
    // rainbow.red.orange.yellow
    //     .shine(
    //         infrared,
    //         ultraviolet,
    //     )
    // ```
    // Here's a negative side effect, can't be fixed unless we can detect that the invocation is not
    // on the same line as the first reference (not currently possible):
    // ```
    // rainbow.red.orange.yellow
    //     .key.shine(
    //     infrared,
    //     ultraviolet,
    // )
    // ```
    val argsIndentElse = if (chunks.last().shouldKeepOnSameLine) ZERO else expressionBreakIndent

    // When we have a trailing lambda and the line it's on isn't broken, reduce its indentation:
    // ```
    // rainbow.let {
    //   it.shine()
    // }
    // ```
    // Here's an example of the line being broken, so we don't reduce the indentation:
    // ```
    // rainbow
    //     .red
    //     .orange
    //     .let {
    //       it.shine()
    //     }
    // ```
    // Here's a negative side effect, can't be fixed unless we can detect that the lambda is not on
    // the same line as the first reference (not currently possible):
    // ```
    // rainbow.red.orange
    //     .yellow.let {
    //   it.shine()
    // }
    // ```
    val lambdaIndentElse = if (hasTrailingLambda) expressionBreakNegativeIndent else ZERO

    builder.block(expressionBreakIndent) {
      // trailing lambdas get their own block, so wrap everything before it in a block
      if (hasTrailingLambda) {
        builder.open(ZERO)
      }

      // chunks that are grouped get their own block
      chunks.filter { it.shouldKeepOnSameLine }.forEach { builder.open(ZERO) }

      // each chunk represents a list of related expressions.
      // if the expressions are "grouped", they'll be in the same block and be on the same line.
      // otherwise they'll be broken onto several lines (assuming they don't fit on one).
      for ((chunkIndex, chunk) in chunks.withIndex()) {
        // get a unique name for this chunk, used for keeping track of indents and line breaks
        val nameTag = genSym()

        // each item represents a dereference or a call invocation
        val items = chunk.expressions
        for ((itemIndex, item) in items.withIndex()) {

          // for everything after the very first element, emit a break and a dot
          if (chunkIndex > 0 || itemIndex > 0) {

            // break if there's a lambda, or the line is long enough
            if (textLength > options.continuationIndent || item.isLambda()) {
              val fillMode =
                  if (chunk.shouldKeepOnSameLine) Doc.FillMode.INDEPENDENT else Doc.FillMode.UNIFIED
              builder.breakOp(fillMode, "", ZERO, Optional.of(nameTag))
            }

            val operator = (item as KtQualifiedExpression).operationSign.value
            builder.token(operator)
            textLength += operator.length
          }

          // emit the reference or method name
          emitSelectorUpToParenthesis(item)

          // we've reached the last element of this chunk
          if (itemIndex == items.indices.last()) {

            // close the grouping block before visiting the call expression body (if any)
            if (chunk.shouldKeepOnSameLine) {
              builder.close()
            }

            // we've reached the trailing lambda, close its block before visiting the body
            if (chunkIndex == chunks.indices.last && hasTrailingLambda) {
              builder.close()
            }
          }

          // visit the call expression body (if any)
          extractCallExpression(item)?.apply {
            visitCallElement(
                null,
                typeArgumentList,
                valueArgumentList,
                lambdaArguments,
                argumentsIndent = Indent.If.make(nameTag, expressionBreakIndent, argsIndentElse),
                lambdaIndent = Indent.If.make(nameTag, ZERO, lambdaIndentElse))
          }

          textLength += item.text.length
        }
      }
    }
  }

  /**
   * Emits a method name up to its parenthesis and arguments.
   *
   * More generally, emits the selector excluding its parameters if it's a method call.
   */
  private fun emitSelectorUpToParenthesis(e: KtExpression) {
    val callExpression = extractCallExpression(e)
    when {
      callExpression != null -> visit(callExpression.calleeExpression)
      e is KtQualifiedExpression -> visit(e.selectorExpression)
      else -> visit(e)
    }
  }

  override fun visitCallExpression(callExpression: KtCallExpression) {
    builder.sync(callExpression)
    with(callExpression) {
      visitCallElement(
          calleeExpression,
          typeArgumentList,
          valueArgumentList,
          lambdaArguments,
          lambdaIndent = ZERO)
    }
  }

  /** Examples `foo<T>(a, b)`, `foo(a)`, `boo()`, `super(a)` */
  private fun visitCallElement(
      callee: KtExpression?,
      typeArgumentList: KtTypeArgumentList?,
      argumentList: KtValueArgumentList?,
      lambdaArguments: List<KtLambdaArgument>,
      argumentsIndent: Indent = expressionBreakIndent,
      lambdaIndent: Indent = ZERO
  ) {
    builder.block(ZERO) {
      visit(callee)
      val arguments = argumentList?.arguments.orEmpty()
      builder.block(argumentsIndent) { visit(typeArgumentList) }
      builder.block(argumentsIndent) {
        if (argumentList != null) {
          builder.token("(")
        }
        if (arguments.isNotEmpty()) {
          if (isGoogleStyle) {
            visit(argumentList)
            val first = arguments.first()
            if (arguments.size != 1 ||
                first?.isNamed() != false ||
                first.getArgumentExpression() !is KtLambdaExpression) {
              builder.breakOp(Doc.FillMode.UNIFIED, "", expressionBreakNegativeIndent)
            }
          } else {
            builder.block(ZERO) { visit(argumentList) }
          }
        }
        if (argumentList != null) {
          builder.token(")")
        }
      }
      if (lambdaArguments.isNotEmpty()) {
        builder.space()
        builder.block(lambdaIndent) { lambdaArguments.forEach { visit(it) } }
      }
    }
  }

  /** Example (`1, "hi"`) in a function call */
  override fun visitValueArgumentList(list: KtValueArgumentList) {
    builder.sync(list)
    val arguments = list.arguments
    val isSingleUnnamedLambda =
        arguments.size == 1 &&
            arguments.first().getArgumentExpression() is KtLambdaExpression &&
            arguments.first().getArgumentName() == null
    if (isSingleUnnamedLambda) {
      builder.block(expressionBreakNegativeIndent) {
        visit(arguments.first())
        if (list.trailingComma != null) {
          builder.token(",")
        }
      }
    } else {
      // Break before args.
      builder.breakOp(Doc.FillMode.UNIFIED, "", ZERO)
      emitParameterLikeList(
          list.arguments, list.trailingComma != null, wrapInBlock = !isGoogleStyle)
    }
  }

  /** Example `{ 1 + 1 }` (as lambda) or `{ (x, y) -> x + y }` */
  override fun visitLambdaExpression(lambdaExpression: KtLambdaExpression) {
    visitLambdaExpression(lambdaExpression, null as BreakTag?)
  }

  private fun visitLambdaExpression(
      lambdaExpression: KtLambdaExpression,
      brokeBeforeBrace: BreakTag?,
  ) {
    builder.sync(lambdaExpression)

    val valueParams = lambdaExpression.valueParameters
    val hasParams = valueParams.isNotEmpty()
    val statements = (lambdaExpression.bodyExpression ?: fail()).children
    val hasStatements = statements.isNotEmpty()
    val hasArrow = lambdaExpression.functionLiteral.arrow != null

    fun ifBrokeBeforeBrace(onTrue: Indent, onFalse: Indent): Indent {
      if (brokeBeforeBrace == null) return onFalse
      return Indent.If.make(brokeBeforeBrace, onTrue, onFalse)
    }

    /**
     * Enable correct formatting of the `fun foo() = scope {` syntax.
     *
     * We can't denote the lambda (+ scope function) as a block, since (for multiline lambdas) the
     * rectangle rule would force the entire lambda onto a lower line. Instead, we conditionally
     * indent all the interior levels of the lambda based on whether we had to break before the
     * opening brace (or scope function). This mimics the look of a block when the break is taken.
     *
     * These conditional indents should not be used inside interior blocks, since that would apply
     * the condition twice.
     */
    val bracePlusBlockIndent = ifBrokeBeforeBrace(blockPlusExpressionBreakIndent, blockIndent)
    val bracePlusExpressionIndent =
        ifBrokeBeforeBrace(doubleExpressionBreakIndent, expressionBreakIndent)
    val bracePlusZeroIndent = ifBrokeBeforeBrace(expressionBreakIndent, ZERO)

    builder.token("{")

    if (hasParams || hasArrow) {
      builder.space()
      builder.block(bracePlusExpressionIndent) {
        forEachCommaSeparated(valueParams) { it.accept(this) }
      }
      builder.block(bracePlusBlockIndent) {
        if (lambdaExpression.functionLiteral.valueParameterList?.trailingComma != null) {
          builder.token(",")
          builder.forcedBreak()
        } else if (hasParams) {
          builder.breakOp(Doc.FillMode.INDEPENDENT, " ", ZERO)
        }
        builder.token("->")
      }
      builder.breakOp(Doc.FillMode.UNIFIED, "", bracePlusZeroIndent)
    }

    if (hasStatements) {
      builder.breakOp(Doc.FillMode.UNIFIED, " ", bracePlusBlockIndent)
      builder.block(bracePlusBlockIndent) {
        builder.blankLineWanted(OpsBuilder.BlankLineWanted.NO)
        if (statements.size == 1 &&
            statements.first() !is KtReturnExpression &&
            lambdaExpression.bodyExpression?.startsWithComment() != true) {
          visitStatement(statements[0])
        } else {
          visitStatements(statements)
        }
      }
    }

    if (hasParams || hasArrow || hasStatements) {
      // If we had to break in the body, ensure there is a break before the closing brace
      builder.breakOp(Doc.FillMode.UNIFIED, " ", bracePlusZeroIndent)
      builder.blankLineWanted(OpsBuilder.BlankLineWanted.NO)
    }
    builder.block(bracePlusZeroIndent) {
      // If there are closing comments, make sure they and the brace are indented together
      // The comments will indent themselves, so consume the previous break as a blank line
      builder.breakOp(Doc.FillMode.INDEPENDENT, "", ZERO)
      builder.token("}", blockIndent)
    }
  }

  /** Example `this` or `this@Foo` */
  override fun visitThisExpression(expression: KtThisExpression) {
    builder.sync(expression)
    builder.token("this")
    visit(expression.getTargetLabel())
  }

  /** Example `Foo` or `@Foo` */
  override fun visitSimpleNameExpression(expression: KtSimpleNameExpression) {
    builder.sync(expression)
    when (expression) {
      is KtLabelReferenceExpression -> {
        if (expression.text[0] == '@') {
          builder.token("@")
          builder.token(expression.getIdentifier()?.text ?: fail())
        } else {
          builder.token(expression.getIdentifier()?.text ?: fail())
          builder.token("@")
        }
      }
      else -> {
        if (expression.text.isNotEmpty()) {
          builder.token(expression.text)
        }
      }
    }
  }

  /** e.g., `a: Int, b: Int, c: Int` in `fun foo(a: Int, b: Int, c: Int) { ... }`. */
  override fun visitParameterList(list: KtParameterList) {
    emitParameterLikeList(list.parameters, list.trailingComma != null, wrapInBlock = false)
  }

  /**
   * Emit a list of elements that look like function parameters or arguments, e.g., `a, b, c` in
   * `foo(a, b, c)`
   */
  private fun <T : PsiElement> emitParameterLikeList(
      list: List<T>?,
      hasTrailingComma: Boolean,
      wrapInBlock: Boolean
  ) {
    if (list.isNullOrEmpty()) {
      return
    }

    forEachCommaSeparated(list, hasTrailingComma, wrapInBlock) { visit(it) }
    if (hasTrailingComma) {
      builder.breakOp(Doc.FillMode.UNIFIED, "", expressionBreakNegativeIndent)
    }
  }

  /**
   * Call `function` for each element in `list`, with comma (,) tokens inbetween.
   *
   * Example:
   * ```
   * a, b, c, 3, 4, 5
   * ```
   *
   * Either the entire list fits in one line, or each element is put on its own line:
   * ```
   * a,
   * b,
   * c,
   * 3,
   * 4,
   * 5
   * ```
   *
   * @param hasTrailingComma if true, each element is placed on its own line (even if they could've
   * fit in a single line), and a trailing comma is emitted.
   *
   * Example:
   * ```
   * a,
   * b,
   * ```
   */
  private fun <T> forEachCommaSeparated(
      list: Iterable<T>,
      hasTrailingComma: Boolean = false,
      wrapInBlock: Boolean = true,
      function: (T) -> Unit
  ) {
    if (hasTrailingComma) {
      builder.block(ZERO) {
        builder.forcedBreak()
        for (value in list) {
          function(value)
          builder.token(",")
          builder.forcedBreak()
        }
      }
      return
    }

    builder.block(ZERO, isEnabled = wrapInBlock) {
      var first = true
      builder.breakOp(Doc.FillMode.UNIFIED, "", ZERO)
      for (value in list) {
        if (!first) {
          builder.token(",")
          builder.breakOp(Doc.FillMode.UNIFIED, " ", ZERO)
        }
        first = false

        function(value)
      }
    }
  }

  /** Example `a` in `foo(a)`, or `*a`, or `limit = 50` */
  override fun visitArgument(argument: KtValueArgument) {
    builder.sync(argument)
    val hasArgName = argument.getArgumentName() != null
    val isLambda = argument.getArgumentExpression() is KtLambdaExpression
    builder.block(ZERO) {
      if (hasArgName) {
        visit(argument.getArgumentName())
        builder.space()
        builder.token("=")
        if (isLambda) {
          builder.space()
        }
      }
      builder.block(if (hasArgName && !isLambda) expressionBreakIndent else ZERO) {
        if (hasArgName && !isLambda) {
          builder.breakOp(Doc.FillMode.INDEPENDENT, " ", ZERO)
        }
        if (argument.isSpread) {
          builder.token("*")
        }
        visit(argument.getArgumentExpression())
      }
    }
  }

  override fun visitReferenceExpression(expression: KtReferenceExpression) {
    builder.sync(expression)
    builder.token(expression.text)
  }

  override fun visitReturnExpression(expression: KtReturnExpression) {
    builder.sync(expression)
    builder.token("return")
    visit(expression.getTargetLabel())
    val returnedExpression = expression.returnedExpression
    if (returnedExpression != null) {
      builder.space()
      visit(returnedExpression)
    }
    builder.guessToken(";")
  }

  /**
   * For example `a + b`, `a + b + c` or `a..b`
   *
   * The extra handling here drills to the left most expression and handles it for long chains of
   * binary expressions that are formatted not accordingly to the associative values That is, we
   * want to think of `a + b + c` as `(a + b) + c`, whereas the AST parses it as `a + (b + c)`
   */
  override fun visitBinaryExpression(expression: KtBinaryExpression) {
    builder.sync(expression)
    val op = expression.operationToken

    if (KtTokens.ALL_ASSIGNMENTS.contains(op) && isLambdaOrScopingFunction(expression.right)) {
      // Assignments are statements in Kotlin; we don't have to worry about compound assignment.
      visit(expression.left)
      builder.space()
      builder.token(expression.operationReference.text)
      visitLambdaOrScopingFunction(expression.right)
      return
    }

    val parts =
        ArrayDeque<KtBinaryExpression>().apply {
          var current: KtExpression? = expression
          while (current is KtBinaryExpression && current.operationToken == op) {
            addFirst(current)
            current = current.left
          }
        }

    val leftMostExpression = parts.first()
    visit(leftMostExpression.left)
    for (leftExpression in parts) {
      when (leftExpression.operationToken) {
        KtTokens.RANGE -> {}
        KtTokens.ELVIS -> builder.breakOp(Doc.FillMode.INDEPENDENT, " ", expressionBreakIndent)
        else -> builder.space()
      }
      builder.token(leftExpression.operationReference.text)
      val isFirst = leftExpression === leftMostExpression
      if (isFirst) {
        builder.open(expressionBreakIndent)
      }
      when (leftExpression.operationToken) {
        KtTokens.RANGE -> {}
        KtTokens.ELVIS -> builder.space()
        else -> builder.breakOp(Doc.FillMode.UNIFIED, " ", ZERO)
      }
      visit(leftExpression.right)
    }
    builder.close()
  }

  override fun visitUnaryExpression(expression: KtUnaryExpression) {
    builder.sync(expression)
    builder.block(ZERO) {
      visit(expression.baseExpression)
      builder.token(expression.operationReference.text)
    }
  }

  override fun visitPrefixExpression(expression: KtPrefixExpression) {
    builder.sync(expression)
    builder.block(ZERO) {
      builder.token(expression.operationReference.text)
      visit(expression.baseExpression)
    }
  }

  override fun visitLabeledExpression(expression: KtLabeledExpression) {
    builder.sync(expression)
    visit(expression.labelQualifier)
    if (expression.baseExpression !is KtLambdaExpression) {
      builder.space()
    }
    visit(expression.baseExpression)
  }

  internal enum class DeclarationKind {
    FIELD,
    PARAMETER
  }

  /**
   * Declare one variable or variable-like thing.
   *
   * Examples:
   * - `var a: Int = 5`
   * - `a: Int`
   * - `private val b:
   */
  private fun declareOne(
      kind: DeclarationKind,
      modifiers: KtModifierList?,
      valOrVarKeyword: String?,
      typeParameters: KtTypeParameterList? = null,
      receiver: KtTypeReference? = null,
      name: String?,
      type: KtTypeReference?,
      typeConstraintList: KtTypeConstraintList? = null,
      initializer: KtExpression?,
      delegate: KtPropertyDelegate? = null,
      accessors: List<KtPropertyAccessor>? = null
  ): Int {
    val verticalAnnotationBreak = genSym()

    val isField = kind == DeclarationKind.FIELD

    if (isField) {
      builder.blankLineWanted(OpsBuilder.BlankLineWanted.conditional(verticalAnnotationBreak))
    }

    visit(modifiers)
    builder.block(ZERO) {
      builder.block(ZERO) {
        if (valOrVarKeyword != null) {
          builder.token(valOrVarKeyword)
          builder.space()
        }

        if (typeParameters != null) {
          visit(typeParameters)
          builder.space()
        }

        // conditionally indent the name and initializer +4 if the type spans
        // multiple lines
        if (name != null) {
          if (receiver != null) {
            visit(receiver)
            builder.token(".")
          }
          builder.token(name)
          builder.op("")
        }
      }

      if (name != null) {
        builder.open(expressionBreakIndent) // open block for named values
      }
      // For example `: String` in `val thisIsALongName: String` or `fun f(): String`
      if (type != null) {
        if (name != null) {
          builder.token(":")
          builder.breakOp(Doc.FillMode.UNIFIED, " ", ZERO)
        }
        visit(type)
      }
    }

    // For example `where T : Int` in a generic method
    if (typeConstraintList != null) {
      builder.space()
      visit(typeConstraintList)
      builder.space()
    }

    // for example `by lazy { compute() }`
    if (delegate != null) {
      builder.space()
      builder.token("by")
      if (isLambdaOrScopingFunction(delegate.expression)) {
        builder.space()
        visit(delegate)
      } else {
        builder.breakOp(Doc.FillMode.UNIFIED, " ", expressionBreakIndent)
        builder.block(expressionBreakIndent) { visit(delegate) }
      }
    } else if (initializer != null) {
      builder.space()
      builder.token("=")
      if (isLambdaOrScopingFunction(initializer)) {
        visitLambdaOrScopingFunction(initializer)
      } else {
        builder.breakOp(Doc.FillMode.UNIFIED, " ", expressionBreakIndent)
        builder.block(expressionBreakIndent) { visit(initializer) }
      }
    }
    if (name != null) {
      builder.close() // close block for named values
    }
    // for example `private set` or `get = 2 * field`
    if (accessors?.isNotEmpty() == true) {
      builder.block(blockIndent) {
        for (accessor in accessors) {
          builder.forcedBreak()
          // The semicolon must come after the newline, or the output code will not parse.
          builder.guessToken(";")

          builder.block(ZERO) {
            visitFunctionLikeExpression(
                accessor.modifierList,
                accessor.namePlaceholder.text,
                null,
                null,
                null,
                accessor.bodyExpression != null || accessor.bodyBlockExpression != null,
                accessor.parameterList,
                null,
                accessor.bodyBlockExpression,
                accessor.bodyExpression,
                accessor.returnTypeReference,
                accessor.bodyBlockExpression?.lBrace != null)
          }
        }
      }
    }

    builder.guessToken(";")

    if (isField) {
      builder.blankLineWanted(OpsBuilder.BlankLineWanted.conditional(verticalAnnotationBreak))
    }

    return 0
  }

  /**
   * Returns whether an expression is a lambda or initializer expression in which case we will want
   * to avoid indenting the lambda block
   *
   * Examples:
   * 1. '... = { ... }' is a lambda expression
   * 2. '... = Runnable { ... }' is considered a scoping function
   * 3. '... = scope { ... }' '... = apply { ... }' is a scoping function
   *
   * but not:
   * 1. '... = foo() { ... }' due to the empty parenthesis
   * 2. '... = Runnable @Annotation { ... }' due to the annotation
   */
  private fun isLambdaOrScopingFunction(expression: KtExpression?): Boolean {
    if (expression is KtLambdaExpression) {
      return true
    }
    if (expression is KtCallExpression &&
        expression.valueArgumentList?.leftParenthesis == null &&
        expression.lambdaArguments.isNotEmpty() &&
        expression.typeArgumentList?.arguments.isNullOrEmpty() &&
        expression.lambdaArguments.first().getArgumentExpression() is KtLambdaExpression) {
      return true
    }
    return false
  }

  /** See [isLambdaOrScopingFunction] for examples. */
  private fun visitLambdaOrScopingFunction(expr: PsiElement?) {
    val breakToExpr = genSym()
    builder.breakOp(Doc.FillMode.INDEPENDENT, " ", expressionBreakIndent, Optional.of(breakToExpr))

    when (expr) {
      is KtLambdaExpression -> {
        visitLambdaExpression(expr, breakToExpr)
      }
      is KtCallExpression -> {
        visit(expr.calleeExpression)
        builder.space()
        visitLambdaExpression(expr.lambdaArguments[0].getLambdaExpression() ?: fail(), breakToExpr)
      }
      else -> throw AssertionError(expr)
    }
  }

  override fun visitClassOrObject(classOrObject: KtClassOrObject) {
    builder.sync(classOrObject)
    val modifierList = classOrObject.modifierList
    builder.block(ZERO) {
      if (modifierList != null) {
        visitModifierList(modifierList)
      }
      val declarationKeyword = classOrObject.getDeclarationKeyword()
      if (declarationKeyword != null) {
        builder.token(declarationKeyword.text ?: fail())
      }
      val name = classOrObject.nameIdentifier
      if (name != null) {
        builder.space()
        builder.token(name.text)
        visit(classOrObject.typeParameterList)
      }
      visit(classOrObject.primaryConstructor)
      val superTypes = classOrObject.getSuperTypeList()
      if (superTypes != null) {
        builder.space()
        builder.block(ZERO) {
          builder.token(":")
          builder.breakOp(Doc.FillMode.UNIFIED, " ", expressionBreakIndent)
          visit(superTypes)
        }
      }
      builder.space()
      val typeConstraintList = classOrObject.typeConstraintList
      if (typeConstraintList != null) {
        if (superTypes?.entries?.lastOrNull() is KtDelegatedSuperTypeEntry) {
          builder.forcedBreak()
        }
        visit(typeConstraintList)
        builder.space()
      }
      val body = classOrObject.body
      if (classOrObject.hasModifier(KtTokens.ENUM_KEYWORD)) {
        visitEnumBody(classOrObject as KtClass)
      } else if (body != null) {
        visitBlockBody(body, true)
      }
    }
    if (classOrObject.nameIdentifier != null) {
      builder.forcedBreak()
    }
  }

  /** Example `{ RED, GREEN; fun foo() { ... } }` for an enum class */
  private fun visitEnumBody(enumClass: KtClass) {
    val body = enumClass.body
    if (body == null) {
      return
    }
    builder.token("{", Doc.Token.RealOrImaginary.REAL, blockIndent, Optional.of(blockIndent))
    builder.open(ZERO)
    builder.block(blockIndent) {
      builder.breakOp(Doc.FillMode.UNIFIED, "", ZERO)
      val (enumEntries, nonEnumEntryStatements) =
          body?.children?.partition { it is KtEnumEntry } ?: fail()
      builder.forcedBreak()
      visitEnumEntries(enumEntries)

      if (nonEnumEntryStatements.isNotEmpty()) {
        builder.forcedBreak()
        builder.blankLineWanted(OpsBuilder.BlankLineWanted.PRESERVE)
        visitStatements(nonEnumEntryStatements.toTypedArray())
      }
    }
    builder.forcedBreak()
    builder.blankLineWanted(OpsBuilder.BlankLineWanted.NO)
    builder.token("}", blockIndent)
    builder.close()
  }

  /** Example `RED, GREEN, BLUE,` in an enum class, or `RED, GREEN;` */
  private fun visitEnumEntries(enumEntries: List<PsiElement>) {
    builder.block(ZERO) {
      builder.breakOp(Doc.FillMode.UNIFIED, "", ZERO)
      for (value in enumEntries) {
        visit(value)
        if (builder.peekToken() == Optional.of(",")) {
          builder.token(",")
          builder.forcedBreak()
        }
      }
    }
    builder.guessToken(";")
  }

  override fun visitPrimaryConstructor(constructor: KtPrimaryConstructor) {
    builder.sync(constructor)
    builder.block(ZERO) {
      if (constructor.hasConstructorKeyword()) {
        builder.open(ZERO)
        builder.breakOp(Doc.FillMode.UNIFIED, " ", ZERO)
        visit(constructor.modifierList)
        builder.token("constructor")
      }

      builder.block(ZERO) {
        builder.token("(")
        builder.block(expressionBreakIndent) {
          builder.breakOp(Doc.FillMode.UNIFIED, "", expressionBreakIndent)
          visit(constructor.valueParameterList)
          builder.breakOp(Doc.FillMode.UNIFIED, "", expressionBreakNegativeIndent)
          if (constructor.hasConstructorKeyword()) {
            builder.close()
          }
        }
        builder.token(")")
      }
    }
  }

  /** Example `private constructor(n: Int) : this(4, 5) { ... }` inside a class's body */
  override fun visitSecondaryConstructor(constructor: KtSecondaryConstructor) {
    val delegationCall = constructor.getDelegationCall()
    val bodyExpression = constructor.bodyExpression

    builder.sync(constructor)

    visitFunctionLikeExpression(
        constructor.modifierList,
        "constructor",
        null,
        null,
        null,
        true,
        constructor.valueParameterList,
        null,
        bodyExpression,
        null,
        if (!delegationCall.isImplicit) delegationCall else null,
        true)
  }

  override fun visitConstructorDelegationCall(call: KtConstructorDelegationCall) {
    // Work around a misfeature in kotlin-compiler: call.calleeExpression.accept doesn't call
    // visitReferenceExpression, but calls visitElement instead.
    builder.block(ZERO) {
      builder.token(if (call.isCallToThis) "this" else "super")
      visitCallElement(
          null,
          call.typeArgumentList,
          call.valueArgumentList,
          call.lambdaArguments,
          lambdaIndent = ZERO)
    }
  }

  override fun visitClassInitializer(initializer: KtClassInitializer) {
    builder.sync(initializer)
    builder.token("init")
    builder.space()
    visit(initializer.body)
  }

  override fun visitConstantExpression(expression: KtConstantExpression) {
    builder.sync(expression)
    builder.token(expression.text)
  }

  /** Example `(1 + 1)` */
  override fun visitParenthesizedExpression(expression: KtParenthesizedExpression) {
    builder.sync(expression)
    builder.token("(")
    visit(expression.expression)
    builder.token(")")
  }

  override fun visitPackageDirective(directive: KtPackageDirective) {
    builder.sync(directive)
    if (directive.packageKeyword == null) {
      return
    }
    builder.token("package")
    builder.space()
    var first = true
    for (packageName in directive.packageNames) {
      if (first) {
        first = false
      } else {
        builder.token(".")
      }
      builder.token(packageName.getIdentifier()?.text ?: packageName.getReferencedName())
    }

    builder.guessToken(";")
    builder.forcedBreak()
  }

  /** Example `import com.foo.A; import com.bar.B` */
  override fun visitImportList(importList: KtImportList) {
    builder.sync(importList)
    importList.imports.forEach { visit(it) }
  }

  /** Example `import com.foo.A` */
  override fun visitImportDirective(directive: KtImportDirective) {
    builder.sync(directive)
    builder.token("import")
    builder.space()

    val importedReference = directive.importedReference
    if (importedReference != null) {
      inImport = true
      visit(importedReference)
      inImport = false
    }
    if (directive.isAllUnder) {
      builder.token(".")
      builder.token("*")
    }

    // Possible alias.
    val alias = directive.alias?.nameIdentifier
    if (alias != null) {
      builder.space()
      builder.token("as")
      builder.space()
      builder.token(alias.text ?: fail())
    }

    // Force a newline afterwards.
    builder.guessToken(";")
    builder.forcedBreak()
  }

  /** For example `@Magic private final` */
  override fun visitModifierList(list: KtModifierList) {
    builder.sync(list)
    var onlyAnnotationsSoFar = true

    for (child in list.node.children()) {
      val psi = child.psi
      if (psi is PsiWhiteSpace) {
        continue
      }

      if (child.elementType is KtModifierKeywordToken) {
        onlyAnnotationsSoFar = false
        builder.token(child.text)
      } else {
        visit(psi)
      }

      if (onlyAnnotationsSoFar) {
        builder.breakOp(Doc.FillMode.UNIFIED, " ", ZERO)
      } else {
        builder.space()
      }
    }
  }

  /**
   * Example:
   * ```
   * @SuppressLint("MagicNumber")
   * print(10)
   * ```
   *
   * in
   *
   * ```
   * fun f() {
   *   @SuppressLint("MagicNumber")
   *   print(10)
   * }
   * ```
   */
  override fun visitAnnotatedExpression(expression: KtAnnotatedExpression) {
    builder.sync(expression)
    builder.block(ZERO) {
      loop@ for (child in expression.node.children()) {
        when (val psi = child.psi) {
          is PsiWhiteSpace -> continue@loop
          is KtAnnotationEntry -> {
            visit(psi)
            if (expression.annotationEntries.size != 1) {
              builder.forcedBreak()
            } else {
              builder.breakOp(Doc.FillMode.UNIFIED, " ", ZERO)
            }
          }
          else -> visit(psi)
        }
      }
    }
  }

  /**
   * For example, @field:[Inject Named("WEB_VIEW")]
   *
   * A KtAnnotation is used only to group multiple annotations with the same use-site-target. It
   * only appears in a modifier list since annotated expressions do not have use-site-targets.
   */
  override fun visitAnnotation(annotation: KtAnnotation) {
    builder.sync(annotation)
    builder.block(ZERO) {
      builder.token("@")
      val useSiteTarget = annotation.useSiteTarget
      if (useSiteTarget != null) {
        visit(useSiteTarget)
        builder.token(":")
      }
      builder.block(expressionBreakIndent) {
        builder.token("[")

        builder.block(ZERO) {
          var first = true
          builder.breakOp(Doc.FillMode.UNIFIED, "", ZERO)
          for (value in annotation.entries) {
            if (!first) {
              builder.breakOp(Doc.FillMode.UNIFIED, " ", ZERO)
            }
            first = false

            visit(value)
          }
        }
      }
      builder.token("]")
    }
    builder.forcedBreak()
  }

  /** For example, 'field' in @field:[Inject Named("WEB_VIEW")] */
  override fun visitAnnotationUseSiteTarget(
      annotationTarget: KtAnnotationUseSiteTarget,
      data: Void?
  ): Void? {
    builder.token(annotationTarget.getAnnotationUseSiteTarget().renderName)
    return null
  }

  /** For example `@Magic` or `@Fred(1, 5)` */
  override fun visitAnnotationEntry(annotationEntry: KtAnnotationEntry) {
    builder.sync(annotationEntry)
    if (annotationEntry.atSymbol != null) {
      builder.token("@")
    }
    val useSiteTarget = annotationEntry.useSiteTarget
    if (useSiteTarget != null && useSiteTarget.parent == annotationEntry) {
      visit(useSiteTarget)
      builder.token(":")
    }
    visitCallElement(
        annotationEntry.calleeExpression,
        null, // Type-arguments are included in the annotation's callee expression.
        annotationEntry.valueArgumentList,
        listOf())
  }

  override fun visitFileAnnotationList(
      fileAnnotationList: KtFileAnnotationList,
      data: Void?
  ): Void? {
    for (child in fileAnnotationList.node.children()) {
      if (child is PsiElement) {
        continue
      }
      visit(child.psi)
      builder.forcedBreak()
    }

    return null
  }

  override fun visitSuperTypeList(list: KtSuperTypeList) {
    builder.sync(list)
    builder.block(expressionBreakIndent) { forEachCommaSeparated(list.entries) { visit(it) } }
  }

  override fun visitSuperTypeCallEntry(call: KtSuperTypeCallEntry) {
    builder.sync(call)
    visitCallElement(call.calleeExpression, null, call.valueArgumentList, call.lambdaArguments)
  }

  /**
   * Example `Collection<Int> by list` in `class MyList(list: List<Int>) : Collection<Int> by list`
   */
  override fun visitDelegatedSuperTypeEntry(specifier: KtDelegatedSuperTypeEntry) {
    builder.sync(specifier)
    visit(specifier.typeReference)
    builder.space()
    builder.token("by")
    builder.space()
    visit(specifier.delegateExpression)
  }

  override fun visitWhenExpression(expression: KtWhenExpression) {
    builder.sync(expression)
    builder.block(ZERO) {
      builder.token("when")
      expression.subjectExpression?.let { subjectExp ->
        builder.space()
        builder.block(ZERO) {
          builder.token("(")
          builder.block(if (isGoogleStyle) expressionBreakIndent else ZERO) { visit(subjectExp) }
          if (isGoogleStyle) {
            builder.breakOp(Doc.FillMode.UNIFIED, "", ZERO)
          }
        }
        builder.token(")")
      }
      builder.space()
      builder.token("{", Doc.Token.RealOrImaginary.REAL, blockIndent, Optional.of(blockIndent))

      expression.entries.forEach { whenEntry ->
        builder.block(blockIndent) {
          builder.forcedBreak()
          if (whenEntry.isElse) {
            builder.token("else")
          } else {
            builder.block(ZERO) {
              forEachCommaSeparated(whenEntry.conditions.asIterable()) { visit(it) }
              builder.guessToken(",")
            }
          }
          val whenExpression = whenEntry.expression
          builder.space()
          builder.token("->")
          if (whenExpression is KtBlockExpression) {
            builder.space()
            visit(whenExpression)
          } else {
            builder.block(expressionBreakIndent) {
              builder.breakOp(Doc.FillMode.INDEPENDENT, " ", ZERO)
              visit(whenExpression)
            }
          }
          builder.guessToken(";")
        }
        builder.forcedBreak()
      }
      builder.token("}")
    }
  }

  override fun visitBlockExpression(expression: KtBlockExpression) {
    builder.sync(expression)
    visitBlockBody(expression, true)
  }

  override fun visitWhenConditionWithExpression(condition: KtWhenConditionWithExpression) {
    builder.sync(condition)
    visit(condition.expression)
  }

  override fun visitWhenConditionIsPattern(condition: KtWhenConditionIsPattern) {
    builder.sync(condition)
    builder.token(if (condition.isNegated) "!is" else "is")
    builder.space()
    visit(condition.typeReference)
  }

  /** Example `in 1..2` as part of a when expression */
  override fun visitWhenConditionInRange(condition: KtWhenConditionInRange) {
    builder.sync(condition)
    // TODO: replace with 'condition.isNegated' once https://youtrack.jetbrains.com/issue/KT-34395
    // is fixed.
    val isNegated = condition.firstChild?.node?.findChildByType(KtTokens.NOT_IN) != null
    builder.token(if (isNegated) "!in" else "in")
    builder.space()
    visit(condition.rangeExpression)
  }

  override fun visitIfExpression(expression: KtIfExpression) {
    builder.sync(expression)
    builder.block(ZERO) {
      builder.block(ZERO) {
        builder.token("if")
        builder.space()
        builder.token("(")
        builder.block(if (isGoogleStyle) expressionBreakIndent else ZERO) {
          visit(expression.condition)
        }
        if (isGoogleStyle) {
          builder.breakOp(Doc.FillMode.UNIFIED, "", ZERO)
        }
      }
      builder.token(")")

      if (expression.then is KtBlockExpression) {
        builder.space()
        builder.block(ZERO) { visit(expression.then) }
      } else {
        builder.breakOp(Doc.FillMode.INDEPENDENT, " ", expressionBreakIndent)
        builder.block(expressionBreakIndent) { visit(expression.then) }
      }

      if (expression.elseKeyword != null) {
        if (expression.then is KtBlockExpression) {
          builder.space()
        } else {
          builder.breakOp(Doc.FillMode.UNIFIED, " ", ZERO)
        }

        builder.block(ZERO) {
          builder.token("else")
          if (expression.`else` is KtBlockExpression || expression.`else` is KtIfExpression) {
            builder.space()
            builder.block(ZERO) { visit(expression.`else`) }
          } else {
            builder.breakOp(Doc.FillMode.INDEPENDENT, " ", expressionBreakIndent)
            builder.block(expressionBreakIndent) { visit(expression.`else`) }
          }
        }
      }
    }
  }

  /** Example `a[3]` or `b["a", 5]` */
  override fun visitArrayAccessExpression(expression: KtArrayAccessExpression) {
    builder.sync(expression)
    visit(expression.arrayExpression)
    builder.block(ZERO) {
      builder.token("[")
      builder.breakOp(Doc.FillMode.UNIFIED, "", expressionBreakIndent)
      builder.block(expressionBreakIndent) {
        emitParameterLikeList(
            expression.indexExpressions, expression.trailingComma != null, wrapInBlock = true)
      }
    }
    builder.token("]")
  }

  /** Example `val (a, b: Int) = Pair(1, 2)` */
  override fun visitDestructuringDeclaration(destructuringDeclaration: KtDestructuringDeclaration) {
    builder.sync(destructuringDeclaration)
    val valOrVarKeyword = destructuringDeclaration.valOrVarKeyword
    if (valOrVarKeyword != null) {
      builder.token(valOrVarKeyword.text)
      builder.space()
    }
    val hasTrailingComma = destructuringDeclaration.trailingComma != null
    builder.block(ZERO) {
      builder.token("(")
      builder.breakOp(Doc.FillMode.UNIFIED, "", expressionBreakIndent)
      builder.block(expressionBreakIndent) {
        emitParameterLikeList(
            destructuringDeclaration.entries, hasTrailingComma, wrapInBlock = true)
      }
    }
    builder.token(")")
    val initializer = destructuringDeclaration.initializer
    if (initializer != null) {
      builder.space()
      builder.token("=")
      if (hasTrailingComma) {
        builder.space()
      } else {
        builder.breakOp(Doc.FillMode.INDEPENDENT, " ", expressionBreakIndent)
      }
      builder.block(expressionBreakIndent, !hasTrailingComma) { visit(initializer) }
    }
  }

  /** Example `a: String` which is part of `(a: String, b: String)` */
  override fun visitDestructuringDeclarationEntry(
      multiDeclarationEntry: KtDestructuringDeclarationEntry
  ) {
    builder.sync(multiDeclarationEntry)
    declareOne(
        initializer = null,
        kind = DeclarationKind.PARAMETER,
        modifiers = multiDeclarationEntry.modifierList,
        name = multiDeclarationEntry.nameIdentifier?.text ?: fail(),
        type = multiDeclarationEntry.typeReference,
        valOrVarKeyword = null,
    )
  }

  /** Example `"Hello $world!"` or `"""Hello world!"""` */
  override fun visitStringTemplateExpression(expression: KtStringTemplateExpression) {
    builder.sync(expression)
    builder.token(WhitespaceTombstones.replaceTrailingWhitespaceWithTombstone(expression.text))
  }

  /** Example `super` in `super.doIt(5)` or `super<Foo>` in `super<Foo>.doIt(5)` */
  override fun visitSuperExpression(expression: KtSuperExpression) {
    builder.sync(expression)
    builder.token("super")
    val superTypeQualifier = expression.superTypeQualifier
    if (superTypeQualifier != null) {
      builder.token("<")
      visit(superTypeQualifier)
      builder.token(">")
    }
    visit(expression.labelQualifier)
  }

  /** Example `<T, S>` */
  override fun visitTypeParameterList(list: KtTypeParameterList) {
    builder.sync(list)
    builder.block(ZERO) {
      builder.token("<")
      val parameters = list.parameters
      if (parameters.isNotEmpty()) {
        // Break before args.
        builder.breakOp(Doc.FillMode.UNIFIED, "", expressionBreakIndent)
        builder.block(expressionBreakIndent) {
          emitParameterLikeList(list.parameters, list.trailingComma != null, wrapInBlock = true)
        }
      }
      builder.token(">")
    }
  }

  override fun visitTypeParameter(parameter: KtTypeParameter) {
    builder.sync(parameter)
    visit(parameter.modifierList)
    builder.token(parameter.nameIdentifier?.text ?: "")
    val extendsBound = parameter.extendsBound
    if (extendsBound != null) {
      builder.space()
      builder.token(":")
      builder.space()
      visit(extendsBound)
    }
  }

  /** Example `where T : View, T : Listener` */
  override fun visitTypeConstraintList(list: KtTypeConstraintList) {
    builder.token("where")
    builder.space()
    builder.sync(list)
    forEachCommaSeparated(list.constraints) { visit(it) }
  }

  /** Example `T : Foo` */
  override fun visitTypeConstraint(constraint: KtTypeConstraint) {
    builder.sync(constraint)
    // TODO(nreid260): What about annotations on the type reference? `where @A T : Int`
    visit(constraint.subjectTypeParameterName)
    builder.space()
    builder.token(":")
    builder.space()
    visit(constraint.boundTypeReference)
  }

  /** Example `for (i in items) { ... }` */
  override fun visitForExpression(expression: KtForExpression) {
    builder.sync(expression)
    builder.block(ZERO) {
      builder.token("for")
      builder.space()
      builder.token("(")
      visit(expression.loopParameter)
      builder.space()
      builder.token("in")
      builder.block(ZERO) {
        builder.breakOp(Doc.FillMode.UNIFIED, " ", expressionBreakIndent)
        builder.block(expressionBreakIndent) { visit(expression.loopRange) }
      }
      builder.token(")")
      builder.space()
      visit(expression.body)
    }
  }

  /** Example `while (a < b) { ... }` */
  override fun visitWhileExpression(expression: KtWhileExpression) {
    builder.sync(expression)
    builder.token("while")
    builder.space()
    builder.token("(")
    visit(expression.condition)
    builder.token(")")
    builder.space()
    visit(expression.body)
  }

  /** Example `do { ... } while (a < b)` */
  override fun visitDoWhileExpression(expression: KtDoWhileExpression) {
    builder.sync(expression)
    builder.token("do")
    builder.space()
    visit(expression.body)
    builder.space()
    builder.token("while")
    builder.space()
    builder.token("(")
    visit(expression.condition)
    builder.token(")")
  }

  /** Example `break` or `break@foo` in a loop */
  override fun visitBreakExpression(expression: KtBreakExpression) {
    builder.sync(expression)
    builder.token("break")
    visit(expression.labelQualifier)
  }

  /** Example `continue` or `continue@foo` in a loop */
  override fun visitContinueExpression(expression: KtContinueExpression) {
    builder.sync(expression)
    builder.token("continue")
    visit(expression.labelQualifier)
  }

  /** Example `f: String`, or `private val n: Int` or `(a: Int, b: String)` (in for-loops) */
  override fun visitParameter(parameter: KtParameter) {
    builder.sync(parameter)
    builder.block(ZERO) {
      val destructuringDeclaration = parameter.destructuringDeclaration
      if (destructuringDeclaration != null) {
        visit(destructuringDeclaration)
      } else {
        declareOne(
            kind = DeclarationKind.PARAMETER,
            modifiers = parameter.modifierList,
            valOrVarKeyword = parameter.valOrVarKeyword?.text,
            name = parameter.nameIdentifier?.text,
            type = parameter.typeReference,
            initializer = parameter.defaultValue)
      }
    }
  }

  override fun visitCallableReferenceExpression(expression: KtCallableReferenceExpression) {
    builder.sync(expression)
    visit(expression.receiverExpression)

    // For some reason, expression.receiverExpression doesn't contain the question-mark
    // token in case of a nullable type, e.g., in String?::isNullOrEmpty.
    // Instead, KtCallableReferenceExpression exposes a method that looks for the QUEST token in its
    // children.
    if (expression.hasQuestionMarks) {
      builder.token("?")
    }

    builder.token("::")
    visit(expression.callableReference)
  }

  override fun visitClassLiteralExpression(expression: KtClassLiteralExpression) {
    builder.sync(expression)
    val receiverExpression = expression.receiverExpression
    if (receiverExpression is KtCallExpression) {
      visitCallElement(
          receiverExpression.calleeExpression,
          receiverExpression.typeArgumentList,
          receiverExpression.valueArgumentList,
          receiverExpression.lambdaArguments)
    } else {
      visit(receiverExpression)
    }
    builder.token("::")
    builder.token("class")
  }

  override fun visitFunctionType(type: KtFunctionType) {
    builder.sync(type)
    val receiver = type.receiver
    if (receiver != null) {
      visit(receiver)
      builder.token(".")
    }
    builder.block(expressionBreakIndent) {
      builder.token("(")
      visit(type.parameterList)
    }
    builder.token(")")
    builder.space()
    builder.token("->")
    builder.space()
    builder.block(expressionBreakIndent) { visit(type.returnTypeReference) }
  }

  /** Example `a is Int` or `b !is Int` */
  override fun visitIsExpression(expression: KtIsExpression) {
    builder.block(ZERO) {
      builder.sync(expression)
      visit(expression.leftHandSide)
      builder.space()
      visit(expression.operationReference)
      builder.breakOp(Doc.FillMode.INDEPENDENT, " ", expressionBreakIndent)
      builder.block(expressionBreakIndent) { visit(expression.typeReference) }
    }
  }

  /** Example `a as Int` or `a as? Int` */
  override fun visitBinaryWithTypeRHSExpression(expression: KtBinaryExpressionWithTypeRHS) {
    builder.block(ZERO) {
      builder.sync(expression)
      visit(expression.left)
      builder.space()
      visit(expression.operationReference)
      builder.breakOp(Doc.FillMode.INDEPENDENT, " ", expressionBreakIndent)
      builder.block(expressionBreakIndent) { visit(expression.right) }
    }
  }

  /**
   * Example:
   * ```
   * fun f() {
   *   val a: Array<Int> = [1, 2, 3]
   * }
   * ```
   */
  override fun visitCollectionLiteralExpression(expression: KtCollectionLiteralExpression) {
    builder.sync(expression)
    builder.block(ZERO) {
      builder.token("[")
      builder.breakOp(Doc.FillMode.UNIFIED, "", expressionBreakIndent)
      builder.block(expressionBreakIndent) {
        emitParameterLikeList(
            expression.getInnerExpressions(), expression.trailingComma != null, wrapInBlock = true)
      }
    }
    builder.token("]")
  }

  override fun visitTryExpression(expression: KtTryExpression) {
    builder.sync(expression)
    builder.token("try")
    builder.space()
    visit(expression.tryBlock)
    for (catchClause in expression.catchClauses) {
      visit(catchClause)
    }
    visit(expression.finallyBlock)
  }

  override fun visitCatchSection(catchClause: KtCatchClause) {
    builder.sync(catchClause)
    builder.space()
    builder.token("catch")
    builder.space()
    builder.block(ZERO) {
      builder.token("(")
      builder.block(expressionBreakIndent) {
        builder.breakOp(Doc.FillMode.UNIFIED, "", ZERO)
        visit(catchClause.catchParameter)
        builder.guessToken(",")
      }
    }
    builder.token(")")
    builder.space()
    visit(catchClause.catchBody)
  }

  override fun visitFinallySection(finallySection: KtFinallySection) {
    builder.sync(finallySection)
    builder.space()
    builder.token("finally")
    builder.space()
    visit(finallySection.finalExpression)
  }

  override fun visitThrowExpression(expression: KtThrowExpression) {
    builder.sync(expression)
    builder.token("throw")
    builder.space()
    visit(expression.thrownExpression)
  }

  /** Example `RED(0xFF0000)` in an enum class */
  override fun visitEnumEntry(enumEntry: KtEnumEntry) {
    builder.sync(enumEntry)
    builder.block(ZERO) {
      visit(enumEntry.modifierList)
      builder.token(enumEntry.nameIdentifier?.text ?: fail())
      enumEntry.initializerList?.initializers?.forEach { visit(it) }
      val body = enumEntry.body
      if (body != null) {
        builder.space()
        visitBlockBody(body, true)
      }
    }
  }

  /** Example `private typealias TextChangedListener = (string: String) -> Unit` */
  override fun visitTypeAlias(typeAlias: KtTypeAlias) {
    builder.sync(typeAlias)
    builder.block(ZERO) {
      visit(typeAlias.modifierList)
      builder.token("typealias")
      builder.space()
      builder.token(typeAlias.nameIdentifier?.text ?: fail())
      visit(typeAlias.typeParameterList)

      builder.space()
      builder.token("=")
      builder.breakOp(Doc.FillMode.INDEPENDENT, " ", expressionBreakIndent)
      builder.block(expressionBreakIndent) {
        visit(typeAlias.getTypeReference())
        visit(typeAlias.typeConstraintList)
        builder.guessToken(";")
      }
      builder.forcedBreak()
    }
  }

  /**
   * visitElement is called for almost all types of AST nodes. We use it to keep track of whether
   * we're currently inside an expression or not.
   *
   * @throws FormattingError
   */
  override fun visitElement(element: PsiElement) {
    inExpression.addLast(element is KtExpression || inExpression.peekLast())
    val previous = builder.depth()
    try {
      super.visitElement(element)
    } catch (e: FormattingError) {
      throw e
    } catch (t: Throwable) {
      throw FormattingError(builder.diagnostic(Throwables.getStackTraceAsString(t)))
    } finally {
      inExpression.removeLast()
    }
    builder.checkClosed(previous)
  }

  override fun visitKtFile(file: KtFile) {
    markForPartialFormat()
    var importListEmpty = false
    var isFirst = true
    for (child in file.children) {
      if (child.text.isBlank()) {
        importListEmpty = child is KtImportList
        continue
      }
      if (!isFirst && child !is PsiComment && (child !is KtScript || !importListEmpty)) {
        builder.blankLineWanted(OpsBuilder.BlankLineWanted.YES)
      }
      visit(child)
      isFirst = false
    }
    markForPartialFormat()
  }

  override fun visitScript(script: KtScript) {
    markForPartialFormat()
    var lastChildHadBlankLineBefore = false
    var first = true
    for (child in script.blockExpression.children) {
      if (child.text.isBlank()) {
        continue
      }
      builder.forcedBreak()
      val childGetsBlankLineBefore = child !is KtProperty
      if (first) {
        builder.blankLineWanted(OpsBuilder.BlankLineWanted.PRESERVE)
      } else if (child !is PsiComment &&
          (childGetsBlankLineBefore || lastChildHadBlankLineBefore)) {
        builder.blankLineWanted(OpsBuilder.BlankLineWanted.YES)
      }
      visit(child)
      builder.guessToken(";")
      lastChildHadBlankLineBefore = childGetsBlankLineBefore
      first = false
    }
    markForPartialFormat()
  }

  private fun inExpression(): Boolean {
    return inExpression.peekLast()
  }

  /**
   * markForPartialFormat is used to delineate the smallest areas of code that must be formatted
   * together.
   *
   * When only parts of the code are being formatted, the requested area is expanded until it's
   * covered by an area marked by this method.
   */
  private fun markForPartialFormat() {
    if (!inExpression()) {
      builder.markForPartialFormat()
    }
  }

  /**
   * Emit a [Doc.Token].
   *
   * @param token the [String] to wrap in a [Doc.Token]
   * @param plusIndentCommentsBefore extra block for comments before this token
   */
  private fun OpsBuilder.token(token: String, plusIndentCommentsBefore: Indent = ZERO) {
    token(
        token,
        Doc.Token.RealOrImaginary.REAL,
        plusIndentCommentsBefore,
        /* breakAndIndentTrailingComment */ Optional.empty())
  }

  /**
   * Opens a new level, emits into it and closes it.
   *
   * This is a helper method to make it easier to keep track of [OpsBuilder.open] and
   * [OpsBuilder.close] calls
   *
   * @param plusIndent the block level to pass to the block
   * @param block a code block to be run in this block level
   */
  private inline fun OpsBuilder.block(
      plusIndent: Indent,
      isEnabled: Boolean = true,
      block: () -> Unit
  ) {
    if (isEnabled) {
      open(plusIndent)
    }
    block()
    if (isEnabled) {
      close()
    }
  }

  /** Helper method to sync the current offset to match any element in the AST */
  private fun OpsBuilder.sync(psiElement: PsiElement) {
    sync(psiElement.startOffset)
  }

  /**
   * Throws a formatting error
   *
   * This is used as `expr ?: fail()` to avoid using the !! operator and provide better error
   * messages.
   */
  private fun fail(message: String = "Unexpected"): Nothing {
    throw FormattingError(builder.diagnostic(message))
  }

  /** Helper function to improve readability */
  private fun visit(element: PsiElement?) {
    element?.accept(this)
  }
}
