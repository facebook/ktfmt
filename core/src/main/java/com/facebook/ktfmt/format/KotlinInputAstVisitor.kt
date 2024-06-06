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
import org.jetbrains.kotlin.psi.KtClassBody
import org.jetbrains.kotlin.psi.KtClassInitializer
import org.jetbrains.kotlin.psi.KtClassLiteralExpression
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtCollectionLiteralExpression
import org.jetbrains.kotlin.psi.KtConstantExpression
import org.jetbrains.kotlin.psi.KtConstructorDelegationCall
import org.jetbrains.kotlin.psi.KtContainerNode
import org.jetbrains.kotlin.psi.KtContextReceiverList
import org.jetbrains.kotlin.psi.KtContinueExpression
import org.jetbrains.kotlin.psi.KtDelegatedSuperTypeEntry
import org.jetbrains.kotlin.psi.KtDestructuringDeclaration
import org.jetbrains.kotlin.psi.KtDestructuringDeclarationEntry
import org.jetbrains.kotlin.psi.KtDoWhileExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
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
import org.jetbrains.kotlin.psi.KtIntersectionType
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
import org.jetbrains.kotlin.psi.KtPostfixExpression
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
import org.jetbrains.kotlin.psi.KtScriptInitializer
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
import org.jetbrains.kotlin.psi.KtUserType
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.psi.KtValueArgumentList
import org.jetbrains.kotlin.psi.KtWhenConditionInRange
import org.jetbrains.kotlin.psi.KtWhenConditionIsPattern
import org.jetbrains.kotlin.psi.KtWhenConditionWithExpression
import org.jetbrains.kotlin.psi.KtWhenExpression
import org.jetbrains.kotlin.psi.KtWhileExpression
import org.jetbrains.kotlin.psi.psiUtil.children
import org.jetbrains.kotlin.psi.psiUtil.getPrevSiblingIgnoringWhitespace
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.psi.psiUtil.startsWithComment
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes
import org.jetbrains.kotlin.psi.stubs.impl.KotlinPlaceHolderStubImpl

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

  /** Example: `fun foo(n: Int) { println(n) }` */
  override fun visitNamedFunction(function: KtNamedFunction) {
    builder.sync(function)
    builder.block(ZERO) {
      visitFunctionLikeExpression(
          contextReceiverList =
              function.getStubOrPsiChild(KtStubElementTypes.CONTEXT_RECEIVER_LIST),
          modifierList = function.modifierList,
          keyword = "fun",
          typeParameters = function.typeParameterList,
          receiverTypeReference = function.receiverTypeReference,
          name = function.nameIdentifier?.text,
          parameterList = function.valueParameterList,
          typeConstraintList = function.typeConstraintList,
          bodyExpression = function.bodyBlockExpression ?: function.bodyExpression,
          typeOrDelegationCall = function.typeReference,
      )
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

    // Normally we wouldn't loop over children, but there can be multiple layers of parens.
    val modifierList = nullableType.modifierList
    val innerType = nullableType.innerType
    for (child in nullableType.node.children()) {
      when {
        child.psi == modifierList -> visit(modifierList)
        child.psi == innerType -> visit(innerType)
        child.elementType == KtTokens.LPAR -> builder.token("(")
        child.elementType == KtTokens.RPAR -> builder.token(")")
      }
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

  /** Example: `A & B`, */
  override fun visitIntersectionType(type: KtIntersectionType) {
    builder.sync(type)

    // TODO(strulovich): Should this have the same indentation behaviour as `x && y`?
    visit(type.getLeftTypeRef())
    builder.space()
    builder.token("&")
    builder.space()
    visit(type.getRightTypeRef())
  }

  /** Example `<Int, String>` in `List<Int, String>` */
  override fun visitTypeArgumentList(typeArgumentList: KtTypeArgumentList) {
    builder.sync(typeArgumentList)
    visitEachCommaSeparated(
        typeArgumentList.arguments,
        typeArgumentList.trailingComma != null,
        wrapInBlock = !isGoogleStyle,
        prefix = "<",
        postfix = ">",
    )
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
   *   list of supertypes.
   */
  private fun visitFunctionLikeExpression(
      contextReceiverList: KtContextReceiverList?,
      modifierList: KtModifierList?,
      keyword: String?,
      typeParameters: KtTypeParameterList?,
      receiverTypeReference: KtTypeReference?,
      name: String?,
      parameterList: KtParameterList?,
      typeConstraintList: KtTypeConstraintList?,
      bodyExpression: KtExpression?,
      typeOrDelegationCall: KtElement?,
  ) {
    fun emitTypeOrDelegationCall(block: () -> Unit) {
      if (typeOrDelegationCall != null) {
        builder.block(ZERO) {
          if (typeOrDelegationCall is KtConstructorDelegationCall) {
            builder.space()
          }
          builder.token(":")
          block()
        }
      }
    }

    val forceTrailingBreak = name != null
    builder.block(ZERO, isEnabled = forceTrailingBreak) {
      if (contextReceiverList != null) {
        visitContextReceiverList(contextReceiverList)
      }
      if (modifierList != null) {
        visitModifierList(modifierList)
      }
      if (keyword != null) {
        builder.token(keyword)
      }
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

      if (parameterList != null && parameterList.hasEmptyParens()) {
        builder.block(ZERO) {
          builder.token("(")
          builder.token(")")
          emitTypeOrDelegationCall {
            builder.breakOp(Doc.FillMode.INDEPENDENT, " ", expressionBreakIndent)
            builder.block(expressionBreakIndent) { visit(typeOrDelegationCall) }
          }
        }
      } else {
        builder.block(expressionBreakIndent) {
          if (parameterList != null) {
            visitEachCommaSeparated(
                list = parameterList.parameters,
                hasTrailingComma = parameterList.trailingComma != null,
                prefix = "(",
                postfix = ")",
                wrapInBlock = false,
                breakBeforePostfix = true,
            )
          }
          emitTypeOrDelegationCall {
            builder.space()
            builder.block(expressionBreakNegativeIndent) { visit(typeOrDelegationCall) }
          }
        }
      }

      if (typeConstraintList != null) {
        builder.space()
        visit(typeConstraintList)
      }
      if (bodyExpression is KtBlockExpression) {
        builder.space()
        visit(bodyExpression)
      } else if (bodyExpression != null) {
        builder.space()
        builder.block(ZERO) {
          builder.token("=")
          if (isLambdaOrScopingFunction(bodyExpression)) {
            visitLambdaOrScopingFunction(bodyExpression)
          } else {
            builder.block(expressionBreakIndent) {
              builder.breakOp(Doc.FillMode.INDEPENDENT, " ", ZERO)
              builder.block(ZERO) { visit(bodyExpression) }
            }
          }
        }
      }
      builder.guessToken(";")
    }
    if (forceTrailingBreak) {
      builder.forcedBreak()
    }
  }

  private fun genSym(): BreakTag {
    return BreakTag()
  }

  private fun emitBracedBlock(
      bodyBlockExpression: PsiElement,
      emitChildren: (Array<PsiElement>) -> Unit,
  ) {
    builder.token("{", Doc.Token.RealOrImaginary.REAL, blockIndent, Optional.of(blockIndent))
    val statements = bodyBlockExpression.children
    if (statements.isNotEmpty()) {
      builder.block(blockIndent) {
        builder.forcedBreak()
        builder.blankLineWanted(OpsBuilder.BlankLineWanted.PRESERVE)
        emitChildren(statements)
      }
      builder.forcedBreak()
      builder.blankLineWanted(OpsBuilder.BlankLineWanted.NO)
    }
    builder.token("}", blockIndent)
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
    when {
      inImport -> {
        visit(receiver)
        val selectorExpression = expression.selectorExpression
        if (selectorExpression != null) {
          builder.token(".")
          visit(selectorExpression)
        }
      }
      receiver is KtStringTemplateExpression -> {
        builder.block(expressionBreakIndent) {
          visit(receiver)
          builder.breakOp(Doc.FillMode.UNIFIED, "", ZERO)
          builder.token(expression.operationSign.value)
          visit(expression.selectorExpression)
        }
      }
      receiver is KtWhenExpression -> {
        builder.block(ZERO) {
          visit(receiver)
          builder.token(expression.operationSign.value)
          visit(expression.selectorExpression)
        }
      }
      else -> {
        emitQualifiedExpression(expression)
      }
    }
  }

  /** Extra data to help [emitQualifiedExpression] know when to open and close a group */
  private class GroupingInfo {
    var groupOpenCount = 0
    var shouldCloseGroup = false
  }

  /**
   * Handles a chain of qualified expressions, i.e. `a[5].b!!.c()[4].f()`
   *
   * This is by far the most complicated part of this formatter. We start by breaking the expression
   * to the steps it is executed in (which are in the opposite order of how the syntax tree is
   * built).
   *
   * We then calculate information to know which parts need to be groups, and finally go part by
   * part, emitting it to the [builder] while closing and opening groups.
   */
  private fun emitQualifiedExpression(expression: KtExpression) {
    val parts = breakIntoParts(expression)
    // whether we want to make a lambda look like a block, this make Kotlin DSLs look as expected
    val useBlockLikeLambdaStyle = parts.last().isLambda() && parts.count { it.isLambda() } == 1
    val groupingInfos = computeGroupingInfo(parts, useBlockLikeLambdaStyle)
    builder.block(expressionBreakIndent) {
      val nameTag = genSym() // allows adjusting arguments indentation if a break will be made
      for ((index, ktExpression) in parts.withIndex()) {
        if (ktExpression is KtQualifiedExpression) {
          builder.breakOp(Doc.FillMode.UNIFIED, "", ZERO, Optional.of(nameTag))
        }
        repeat(groupingInfos[index].groupOpenCount) { builder.open(ZERO) }
        when (ktExpression) {
          is KtQualifiedExpression -> {
            builder.token(ktExpression.operationSign.value)
            val selectorExpression = ktExpression.selectorExpression
            if (selectorExpression !is KtCallExpression) {
              // selector is a simple field access
              visit(selectorExpression)
              if (groupingInfos[index].shouldCloseGroup) {
                builder.close()
              }
            } else {
              // selector is a function call, we may close a group after its name
              // emit `doIt` from `doIt(1, 2) { it }`
              visit(selectorExpression.calleeExpression)
              // close groups according to instructions
              if (groupingInfos[index].shouldCloseGroup) {
                builder.close()
              }
              // close group due to last lambda to allow block-like style in `as.forEach { ... }`
              val isTrailingLambda = useBlockLikeLambdaStyle && index == parts.size - 1
              if (isTrailingLambda) {
                builder.close()
              }
              val argsIndentElse = if (index == parts.size - 1) ZERO else expressionBreakIndent
              val lambdaIndentElse = if (isTrailingLambda) expressionBreakNegativeIndent else ZERO
              val negativeLambdaIndentElse = if (isTrailingLambda) expressionBreakIndent else ZERO

              // emit `(1, 2) { it }` from `doIt(1, 2) { it }`
              visitCallElement(
                  null,
                  selectorExpression.typeArgumentList,
                  selectorExpression.valueArgumentList,
                  selectorExpression.lambdaArguments,
                  argumentsIndent = Indent.If.make(nameTag, expressionBreakIndent, argsIndentElse),
                  lambdaIndent = Indent.If.make(nameTag, ZERO, lambdaIndentElse),
                  negativeLambdaIndent = Indent.If.make(nameTag, ZERO, negativeLambdaIndentElse),
              )
            }
          }
          is KtArrayAccessExpression -> {
            visitArrayAccessBrackets(ktExpression)
            builder.close()
          }
          is KtPostfixExpression -> {
            builder.token(ktExpression.operationReference.text)
            builder.close()
          }
          else -> {
            check(index == 0)
            visit(ktExpression)
          }
        }
      }
    }
  }

  /**
   * Decomposes a qualified expression into parts, so `rainbow.red.orange.yellow` becomes `[rainbow,
   * rainbow.red, rainbow.red.orange, rainbow.orange.yellow]`
   */
  private fun breakIntoParts(expression: KtExpression): List<KtExpression> {
    val parts = ArrayDeque<KtExpression>()

    // use an ArrayDeque and add elements to the beginning so the innermost expression comes first
    // foo.bar.yay -> [yay, bar.yay, foo.bar.yay]

    var node: KtExpression? = expression
    while (node != null) {
      parts.addFirst(node)
      node =
          when (node) {
            is KtQualifiedExpression -> node.receiverExpression
            is KtArrayAccessExpression -> node.arrayExpression
            is KtPostfixExpression -> node.baseExpression
            else -> null
          }
    }

    return parts.toList()
  }

  /**
   * Generates the [GroupingInfo] array to go with an array of [KtQualifiedExpression] parts
   *
   * For example, the expression `a.b[2].c.d()` is made of four expressions:
   * 1. [KtQualifiedExpression] `a.b[2].c . d()` (this will be `parts[4]`)
   * 1. [KtQualifiedExpression] `a.b[2] . c` (this will be `parts[3]`)
   * 2. [KtArrayAccessExpression] `a.b [2]` (this will be `parts[2]`)
   * 3. [KtQualifiedExpression] `a . b` (this will be `parts[1]`)
   * 4. [KtSimpleNameExpression] `a` (this will be `parts[0]`)
   *
   * Once in parts, these are in the reverse order. To render the array correct we need to make sure
   * `b` and [2] are in a group so we avoid splitting them. To do so we need to open a group for `b`
   * (that will be done in part 2), and always close a group for an array.
   *
   * Here is the same expression, with justified braces marking the groupings it will get:
   * ```
   *  a . b [2] . c . d ()
   * {a . b} --> Grouping `a.b` because it can be a package name or simple field access so we add 1
   *             to the number of groups to open at groupingInfos[0], and mark to close a group at
   *             groupingInfos[1]
   * {a . b [2]} --> Grouping `a.b` with `[2]`, since otherwise we may break inside the brackets
   *                 instead of preferring breaks before dots. So we open a group at [0], but since
   *                 we always close a group after brackets, we don't store that information.
   *             {c . d} --> another group to attach the first function name to the fields before it
   *                         this time we don't start the group in the beginning, and use
   *                         lastIndexToOpen to track the spot after the last time we stopped
   *                         grouping.
   * ```
   *
   * The final expression with groupings:
   * ```
   * {{a.b}[2]}.{c.d}()
   * ```
   */
  private fun computeGroupingInfo(
      parts: List<KtExpression>,
      useBlockLikeLambdaStyle: Boolean
  ): List<GroupingInfo> {
    val groupingInfos = List(parts.size) { GroupingInfo() }
    var lastIndexToOpen = 0
    for ((index, part) in parts.withIndex()) {
      when (part) {
        is KtQualifiedExpression -> {
          val receiverExpression = part.receiverExpression
          val previous =
              (receiverExpression as? KtQualifiedExpression)?.selectorExpression
                  ?: receiverExpression
          val current = checkNotNull(part.selectorExpression)
          if (lastIndexToOpen == 0 &&
              shouldGroupPartWithPrevious(parts, part, index, previous, current)) {
            // this and the previous items should be grouped for better style
            // we add another group to open in index 0
            groupingInfos[0].groupOpenCount++
            // we don't always close a group when emitting this node, so we need this flag to
            // mark if we need to close a group
            groupingInfos[index].shouldCloseGroup = true
          } else {
            // use this index in to open future groups for arrays and postfixes
            // we will also stop grouping field access to the beginning of the expression
            lastIndexToOpen = index
          }
        }
        is KtArrayAccessExpression,
        is KtPostfixExpression -> {
          // we group these with the last item with a name, and we always close them
          groupingInfos[lastIndexToOpen].groupOpenCount++
        }
      }
    }
    if (useBlockLikeLambdaStyle) {
      // a trailing lambda adds a group that we stop before emitting the lambda
      groupingInfos[0].groupOpenCount++
    }
    return groupingInfos
  }

  /** Decide whether a [KtQualifiedExpression] part should be grouped with the previous part */
  private fun shouldGroupPartWithPrevious(
      parts: List<KtExpression>,
      part: KtExpression,
      index: Int,
      previous: KtExpression,
      current: KtExpression
  ): Boolean {
    // this is the second, and the first is short, avoid `.` "hanging in air"
    if (index == 1 && previous.text.length < options.continuationIndent) {
      return true
    }
    // the previous part is `this` or `super`
    if (previous is KtSuperExpression || previous is KtThisExpression) {
      return true
    }
    // this and the previous part are a package name, type name, or property
    if (previous is KtSimpleNameExpression &&
        current is KtSimpleNameExpression &&
        part is KtDotQualifiedExpression) {
      return true
    }
    // this is `Foo` in `com.facebook.Foo`, so everything before it is a package name
    if (current.text.first().isUpperCase() &&
        current is KtSimpleNameExpression &&
        part is KtDotQualifiedExpression) {
      return true
    }
    // this is the `foo()` in `com.facebook.Foo.foo()` or in `Foo.foo()`
    if (current is KtCallExpression &&
        (previous !is KtCallExpression) &&
        previous.text?.firstOrNull()?.isUpperCase() == true) {
      return true
    }
    // this is an invocation and the last item, and the previous it not, i.e. `a.b.c()`
    // keeping it grouped and splitting the arguments makes `a.b(...)` feel like `aab()`
    return current is KtCallExpression &&
        previous !is KtCallExpression &&
        index == parts.indices.last
  }

  override fun visitCallExpression(callExpression: KtCallExpression) {
    builder.sync(callExpression)
    with(callExpression) {
      visitCallElement(
          calleeExpression,
          typeArgumentList,
          valueArgumentList,
          lambdaArguments,
      )
    }
  }

  /**
   * Examples `foo<T>(a, b)`, `foo(a)`, `boo()`, `super(a)`
   *
   * @param lambdaIndent how to indent [lambdaArguments], if present
   * @param negativeLambdaIndent the negative indentation of [lambdaIndent]
   */
  private fun visitCallElement(
      callee: KtExpression?,
      typeArgumentList: KtTypeArgumentList?,
      argumentList: KtValueArgumentList?,
      lambdaArguments: List<KtLambdaArgument>,
      argumentsIndent: Indent = expressionBreakIndent,
      lambdaIndent: Indent = ZERO,
      negativeLambdaIndent: Indent = ZERO,
  ) {
    // Apply the lambda indent to the callee, type args, value args, and the lambda.
    // This is undone for the first three by the negative lambda indent.
    // This way they're in one block, and breaks in the argument list cause a break in the lambda.
    builder.block(lambdaIndent) {

      // Used to keep track of whether or not we need to indent the lambda
      // This is based on if there is a break in the argument list
      var brokeBeforeBrace: BreakTag? = null

      builder.block(negativeLambdaIndent) {
        visit(callee)
        builder.block(argumentsIndent) {
          builder.block(ZERO) { visit(typeArgumentList) }
          if (argumentList != null) {
            brokeBeforeBrace = visitValueArgumentListInternal(argumentList)
          }
        }
      }
      when (lambdaArguments.size) {
        0 -> {}
        1 -> {
          builder.space()
          visitArgumentInternal(
              lambdaArguments.single(),
              wrapInBlock = false,
              brokeBeforeBrace = brokeBeforeBrace,
          )
        }
        else -> throw ParseError("Maximum one trailing lambda is allowed", lambdaArguments[1])
      }
    }
  }

  /** Example (`1, "hi"`) in a function call */
  override fun visitValueArgumentList(list: KtValueArgumentList) {
    visitValueArgumentListInternal(list)
  }

  /**
   * Example (`1, "hi"`) in a function call
   *
   * @return a [BreakTag] which can tell you if a break was taken, but only when the list doesn't
   *   terminate in a negative closing indent. See [visitEachCommaSeparated] for examples.
   */
  private fun visitValueArgumentListInternal(list: KtValueArgumentList): BreakTag? {
    builder.sync(list)

    val arguments = list.arguments
    val isSingleUnnamedLambda =
        arguments.size == 1 &&
            arguments.first().getArgumentExpression() is KtLambdaExpression &&
            arguments.first().getArgumentName() == null
    val hasTrailingComma = list.trailingComma != null
    val hasEmptyParens = list.hasEmptyParens()

    val wrapInBlock: Boolean
    val breakBeforePostfix: Boolean
    val leadingBreak: Boolean
    val breakAfterPrefix: Boolean
    if (isSingleUnnamedLambda) {
      wrapInBlock = true
      breakBeforePostfix = false
      leadingBreak = !hasEmptyParens && hasTrailingComma
      breakAfterPrefix = false
    } else {
      wrapInBlock = !isGoogleStyle
      breakBeforePostfix = isGoogleStyle && !hasEmptyParens
      leadingBreak = !hasEmptyParens
      breakAfterPrefix = !hasEmptyParens
    }

    return visitEachCommaSeparated(
        arguments,
        hasTrailingComma,
        wrapInBlock = wrapInBlock,
        breakBeforePostfix = breakBeforePostfix,
        leadingBreak = leadingBreak,
        prefix = "(",
        postfix = ")",
        breakAfterPrefix = breakAfterPrefix,
    )
  }

  /** Example `{ 1 + 1 }` (as lambda) or `{ (x, y) -> x + y }` */
  override fun visitLambdaExpression(lambdaExpression: KtLambdaExpression) {
    visitLambdaExpressionInternal(lambdaExpression, brokeBeforeBrace = null)
  }

  /**
   * The internal version of [visitLambdaExpression].
   *
   * @param brokeBeforeBrace used for tracking if a break was taken right before the lambda
   *   expression. Useful for scoping functions where we want good looking indentation. For example,
   *   here we have correct indentation before `bar()` and `car()` because we can detect the break
   *   after the equals:
   * ```
   * fun foo() =
   *     coroutineScope { x ->
   *       bar()
   *       car()
   *     }
   * ```
   */
  private fun visitLambdaExpressionInternal(
      lambdaExpression: KtLambdaExpression,
      brokeBeforeBrace: BreakTag?,
  ) {
    builder.sync(lambdaExpression)

    val valueParams = lambdaExpression.valueParameters
    val hasParams = valueParams.isNotEmpty()
    val bodyExpression = lambdaExpression.bodyExpression ?: fail()
    val expressionStatements = bodyExpression.children
    val hasStatements = expressionStatements.isNotEmpty()
    val hasComments = bodyExpression.children().any { it is PsiComment }
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
      builder.block(bracePlusExpressionIndent) { visitEachCommaSeparated(valueParams) }
      builder.block(bracePlusBlockIndent) {
        if (lambdaExpression.functionLiteral.valueParameterList?.trailingComma != null) {
          builder.token(",")
          builder.forcedBreak()
        } else if (hasParams) {
          builder.breakOp(Doc.FillMode.INDEPENDENT, " ", ZERO)
        }
        builder.token("->")
      }
    }

    if (hasParams || hasArrow || hasStatements || hasComments) {
      builder.breakOp(Doc.FillMode.UNIFIED, " ", bracePlusZeroIndent)
    }

    if (hasStatements) {
      builder.breakOp(Doc.FillMode.UNIFIED, "", bracePlusBlockIndent)
      builder.block(bracePlusBlockIndent) {
        builder.blankLineWanted(OpsBuilder.BlankLineWanted.NO)
        if (expressionStatements.size == 1 &&
            expressionStatements.first() !is KtReturnExpression &&
            !bodyExpression.startsWithComment()) {
          visitStatement(expressionStatements[0])
        } else {
          visitStatements(expressionStatements)
        }
        builder.breakOp(Doc.FillMode.UNIFIED, " ", bracePlusZeroIndent)
      }
    }

    if (hasParams || hasArrow || hasStatements) {
      // If we had to break in the body, ensure there is a break before the closing brace
      builder.breakOp(Doc.FillMode.UNIFIED, "", bracePlusZeroIndent)
    }
    builder.block(bracePlusZeroIndent) {
      builder.fenceComments()
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
    visitEachCommaSeparated(list.parameters, list.trailingComma != null, wrapInBlock = false)
  }

  /**
   * Visit each element in [list], with comma (,) tokens in-between.
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
   * Optionally include a prefix and postfix:
   * ```
   *   (
   *     a,
   *     b,
   *     c,
   * )
   * ```
   *
   * @param hasTrailingComma if true, each element is placed on its own line (even if they could've
   *   fit in a single line), and a trailing comma is emitted.
   *
   * Example:
   * ```
   * a,
   * b,
   * ```
   *
   * @param wrapInBlock if true, place all the elements in a block. When there's no [leadingBreak],
   *   this will be negatively indented. Note that the [prefix] and [postfix] aren't included in the
   *   block.
   * @param leadingBreak if true, break before the first element.
   * @param prefix if provided, emit this before the first element.
   * @param postfix if provided, emit this after the last element (or trailing comma).
   * @param breakAfterPrefix if true, emit a break after [prefix], but before the start of the
   *   block.
   * @param breakBeforePostfix if true, place a break after the last element. Redundant when
   *   [hasTrailingComma] is true.
   * @return a [BreakTag] which can tell you if a break was taken, but only when the list doesn't
   *   terminate in a negative closing indent.
   *
   * Example 1, this returns a BreakTag which tells you a break wasn't taken:
   * ```
   * (arg1, arg2)
   * ```
   *
   * Example 2, this returns a BreakTag which tells you a break WAS taken:
   * ```
   * (
   *     arg1,
   *     arg2)
   * ```
   *
   * Example 3, this returns null:
   * ```
   * (
   *     arg1,
   *     arg2,
   * )
   * ```
   *
   * Example 4, this also returns null (similar to example 2, but Google style):
   * ```
   * (
   *     arg1,
   *     arg2
   * )
   * ```
   */
  private fun visitEachCommaSeparated(
      list: Iterable<PsiElement>,
      hasTrailingComma: Boolean = false,
      wrapInBlock: Boolean = true,
      leadingBreak: Boolean = true,
      prefix: String? = null,
      postfix: String? = null,
      breakAfterPrefix: Boolean = true,
      breakBeforePostfix: Boolean = isGoogleStyle,
  ): BreakTag? {
    val breakAfterLastElement = hasTrailingComma || (postfix != null && breakBeforePostfix)
    val nameTag = if (breakAfterLastElement) null else genSym()

    if (prefix != null) {
      builder.token(prefix)
      if (breakAfterPrefix) {
        builder.breakOp(Doc.FillMode.UNIFIED, "", ZERO, Optional.ofNullable(nameTag))
      }
    }

    val breakType = if (hasTrailingComma) Doc.FillMode.FORCED else Doc.FillMode.UNIFIED
    fun emitComma() {
      builder.token(",")
      builder.breakOp(breakType, " ", ZERO)
    }

    val indent = if (leadingBreak) ZERO else expressionBreakNegativeIndent
    builder.block(indent, isEnabled = wrapInBlock) {
      if (leadingBreak) {
        builder.breakOp(breakType, "", ZERO)
      }

      var first = true
      for (value in list) {
        if (!first) emitComma()
        first = false
        visit(value)
      }

      if (hasTrailingComma) {
        emitComma()
      }
    }

    if (breakAfterLastElement) {
      // a negative closing indent places the postfix to the left of the elements
      // see examples 2 and 4 in the docstring
      builder.breakOp(breakType, "", expressionBreakNegativeIndent)
    }

    if (postfix != null) {
      if (breakAfterLastElement) {
        builder.block(expressionBreakNegativeIndent) {
          builder.fenceComments()
          builder.token(postfix, expressionBreakIndent)
        }
      } else {
        builder.token(postfix)
      }
    }

    return nameTag
  }

  /** Example `a` in `foo(a)`, or `*a`, or `limit = 50` */
  override fun visitArgument(argument: KtValueArgument) {
    visitArgumentInternal(
        argument,
        wrapInBlock = true,
        brokeBeforeBrace = null,
    )
  }

  /**
   * The internal version of [visitArgument].
   *
   * @param wrapInBlock if true places the argument expression in a block.
   */
  private fun visitArgumentInternal(
      argument: KtValueArgument,
      wrapInBlock: Boolean,
      brokeBeforeBrace: BreakTag?,
  ) {
    builder.sync(argument)
    val hasArgName = argument.getArgumentName() != null
    val isLambda = argument.getArgumentExpression() is KtLambdaExpression
    if (hasArgName) {
      visit(argument.getArgumentName())
      builder.space()
      builder.token("=")
      if (isLambda) {
        builder.space()
      }
    }
    val indent = if (hasArgName && !isLambda) expressionBreakIndent else ZERO
    builder.block(indent, isEnabled = wrapInBlock) {
      if (hasArgName && !isLambda) {
        builder.breakOp(Doc.FillMode.INDEPENDENT, " ", ZERO)
      }
      if (argument.isSpread) {
        builder.token("*")
      }
      if (isLambda) {
        visitLambdaExpressionInternal(
            argument.getArgumentExpression() as KtLambdaExpression,
            brokeBeforeBrace = brokeBeforeBrace,
        )
      } else {
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
      val isFirst = leftExpression === leftMostExpression

      when (leftExpression.operationToken) {
        KtTokens.RANGE,
        KtTokens.RANGE_UNTIL -> {
          if (isFirst) {
            builder.open(expressionBreakIndent)
          }
          builder.token(leftExpression.operationReference.text)
        }
        KtTokens.ELVIS -> {
          if (isFirst) {
            builder.open(expressionBreakIndent)
          }
          builder.breakOp(Doc.FillMode.UNIFIED, " ", ZERO)
          builder.token(leftExpression.operationReference.text)
          builder.space()
        }
        else -> {
          builder.space()
          if (isFirst) {
            builder.open(expressionBreakIndent)
          }
          builder.token(leftExpression.operationReference.text)
          builder.breakOp(Doc.FillMode.UNIFIED, " ", ZERO)
        }
      }
      visit(leftExpression.right)
    }
    builder.close()
  }

  override fun visitPostfixExpression(expression: KtPostfixExpression) {
    builder.sync(expression)
    builder.block(ZERO) {
      val baseExpression = expression.baseExpression
      val operator = expression.operationReference.text

      visit(baseExpression)
      if (baseExpression is KtPostfixExpression &&
          baseExpression.operationReference.text.last() == operator.first()) {
        builder.space()
      }
      builder.token(operator)
    }
  }

  override fun visitPrefixExpression(expression: KtPrefixExpression) {
    builder.sync(expression)
    builder.block(ZERO) {
      val baseExpression = expression.baseExpression
      val operator = expression.operationReference.text

      builder.token(operator)
      if (baseExpression is KtPrefixExpression &&
          operator.last() == baseExpression.operationReference.text.first()) {
        builder.space()
      }
      visit(baseExpression)
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
        }
      }

      builder.block(expressionBreakIndent, isEnabled = name != null) {
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
          builder.block(expressionBreakIndent) {
            builder.fenceComments()
            visit(delegate)
          }
        }
      } else if (initializer != null) {
        builder.space()
        builder.token("=")
        if (isLambdaOrScopingFunction(initializer)) {
          visitLambdaOrScopingFunction(initializer)
        } else {
          builder.breakOp(Doc.FillMode.UNIFIED, " ", expressionBreakIndent)
          builder.block(expressionBreakIndent) {
            builder.fenceComments()
            visit(initializer)
          }
        }
      }
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
                contextReceiverList = null,
                modifierList = accessor.modifierList,
                keyword = accessor.namePlaceholder.text,
                typeParameters = null,
                receiverTypeReference = null,
                name = null,
                parameterList = getParameterListWithBugFixes(accessor),
                typeConstraintList = null,
                bodyExpression = accessor.bodyBlockExpression ?: accessor.bodyExpression,
                typeOrDelegationCall = accessor.returnTypeReference,
            )
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

  // Bug in Kotlin 1.9.10: KtProperyAccessor is the direct parent of the left and right paren
  // elements. Also parameterList is always null for getters. As a workaround, we create our own
  // fake KtParameterList.
  private fun getParameterListWithBugFixes(accessor: KtPropertyAccessor): KtParameterList? {
    if (accessor.bodyExpression == null && accessor.bodyBlockExpression == null) return null

    return object :
        KtParameterList(
            KotlinPlaceHolderStubImpl(accessor.stub, KtStubElementTypes.VALUE_PARAMETER_LIST)) {
      override fun getParameters(): List<KtParameter> {
        return accessor.valueParameters
      }

      override fun getTrailingComma(): PsiElement? {
        return accessor.parameterList?.trailingComma
      }

      override fun getLeftParenthesis(): PsiElement? {
        return accessor.leftParenthesis
      }

      override fun getRightParenthesis(): PsiElement? {
        return accessor.rightParenthesis
      }
    }
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
    if (expression == null) return false
    if (expression.getPrevSiblingIgnoringWhitespace() is PsiComment) {
      return false // Leading comments cause weird indentation.
    }

    var carry = expression
    if (carry is KtCallExpression) {
      if (carry.valueArgumentList?.leftParenthesis == null &&
          carry.lambdaArguments.isNotEmpty() &&
          carry.typeArgumentList?.arguments.isNullOrEmpty()) {
        carry = carry.lambdaArguments[0].getArgumentExpression()
      } else {
        return false
      }
    }
    if (carry is KtLabeledExpression) {
      carry = carry.baseExpression
    }
    if (carry is KtLambdaExpression) {
      return true
    }

    return false
  }

  /** See [isLambdaOrScopingFunction] for examples. */
  private fun visitLambdaOrScopingFunction(expr: PsiElement?) {
    val breakToExpr = genSym()
    builder.breakOp(Doc.FillMode.INDEPENDENT, " ", expressionBreakIndent, Optional.of(breakToExpr))

    var carry = expr
    if (carry is KtCallExpression) {
      visit(carry.calleeExpression)
      builder.space()
      carry = carry.lambdaArguments[0].getArgumentExpression()
    }
    if (carry is KtLabeledExpression) {
      visit(carry.labelQualifier)
      carry = carry.baseExpression ?: fail()
    }
    if (carry is KtLambdaExpression) {
      visitLambdaExpressionInternal(carry, brokeBeforeBrace = breakToExpr)
      return
    }

    throw AssertionError(carry)
  }

  override fun visitClassOrObject(classOrObject: KtClassOrObject) {
    builder.sync(classOrObject)
    val contextReceiverList =
        classOrObject.getStubOrPsiChild(KtStubElementTypes.CONTEXT_RECEIVER_LIST)
    val modifierList = classOrObject.modifierList
    builder.block(ZERO) {
      if (contextReceiverList != null) {
        visitContextReceiverList(contextReceiverList)
      }
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
      visit(classOrObject.body)
    }
    if (classOrObject.nameIdentifier != null) {
      builder.forcedBreak()
    }
  }

  override fun visitPrimaryConstructor(constructor: KtPrimaryConstructor) {
    builder.sync(constructor)
    builder.block(ZERO) {
      if (constructor.hasConstructorKeyword()) {
        builder.breakOp(Doc.FillMode.UNIFIED, " ", ZERO)
      }
      visitFunctionLikeExpression(
          contextReceiverList = null,
          modifierList = constructor.modifierList,
          keyword = if (constructor.hasConstructorKeyword()) "constructor" else null,
          typeParameters = null,
          receiverTypeReference = null,
          name = null,
          parameterList = constructor.valueParameterList,
          typeConstraintList = null,
          bodyExpression = constructor.bodyExpression,
          typeOrDelegationCall = null,
      )
    }
  }

  /** Example `private constructor(n: Int) : this(4, 5) { ... }` inside a class's body */
  override fun visitSecondaryConstructor(constructor: KtSecondaryConstructor) {
    builder.sync(constructor)
    builder.block(ZERO) {
      val delegationCall = constructor.getDelegationCall()
      visitFunctionLikeExpression(
          contextReceiverList =
              constructor.getStubOrPsiChild(KtStubElementTypes.CONTEXT_RECEIVER_LIST),
          modifierList = constructor.modifierList,
          keyword = "constructor",
          typeParameters = null,
          receiverTypeReference = null,
          name = null,
          parameterList = constructor.valueParameterList,
          typeConstraintList = null,
          bodyExpression = constructor.bodyExpression,
          typeOrDelegationCall = if (!delegationCall.isImplicit) delegationCall else null,
      )
    }
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
      )
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

  /** Example `context(Logger, Raise<Error>)` */
  override fun visitContextReceiverList(contextReceiverList: KtContextReceiverList) {
    builder.sync(contextReceiverList)
    builder.token("context")
    visitEachCommaSeparated(
        contextReceiverList.contextReceivers(),
        prefix = "(",
        postfix = ")",
        breakAfterPrefix = false,
        breakBeforePostfix = false,
    )
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
      val baseExpression = expression.baseExpression

      builder.block(ZERO) {
        val annotationEntries = expression.annotationEntries
        for (annotationEntry in annotationEntries) {
          if (annotationEntry !== annotationEntries.first()) {
            builder.breakOp(Doc.FillMode.UNIFIED, " ", ZERO)
          }
          visit(annotationEntry)
        }
      }

      // Binary expressions in a block have a different meaning according to their formatting.
      // If there in the line above, they refer to the entire expression, if they're in the same
      // line then only to the first operand of the operator.
      // We force a break to avoid such semantic changes
      when {
        (baseExpression is KtBinaryExpression || baseExpression is KtBinaryExpressionWithTypeRHS) &&
            expression.parent is KtBlockExpression -> builder.forcedBreak()
        baseExpression is KtLambdaExpression -> builder.space()
        else -> builder.breakOp(Doc.FillMode.UNIFIED, " ", ZERO)
      }

      visit(expression.baseExpression)
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
    builder.block(expressionBreakIndent) { visitEachCommaSeparated(list.entries) }
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
      emitKeywordWithCondition("when", expression.subjectExpression)

      builder.space()
      builder.token("{", Doc.Token.RealOrImaginary.REAL, blockIndent, Optional.of(blockIndent))

      expression.entries.forEach { whenEntry ->
        builder.block(blockIndent) {
          builder.forcedBreak()
          if (whenEntry.isElse) {
            builder.token("else")
          } else {
            builder.block(ZERO) {
              val conditions = whenEntry.conditions
              for ((index, condition) in conditions.withIndex()) {
                visit(condition)
                builder.guessToken(",")
                if (index != conditions.lastIndex) {
                  builder.forcedBreak()
                }
              }
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

  override fun visitClassBody(body: KtClassBody) {
    builder.sync(body)
    emitBracedBlock(body) { children ->
      val enumEntryList = EnumEntryList.extractChildList(body)
      val members = children.filter { it !is KtEnumEntry }

      if (enumEntryList != null) {
        builder.block(ZERO) {
          builder.breakOp(Doc.FillMode.UNIFIED, "", ZERO)
          for (value in enumEntryList.enumEntries) {
            visit(value)
            if (builder.peekToken() == Optional.of(",")) {
              builder.token(",")
              builder.forcedBreak()
            }
          }
        }
        builder.guessToken(";")

        if (members.isNotEmpty()) {
          builder.forcedBreak()
          builder.blankLineWanted(OpsBuilder.BlankLineWanted.YES)
        }
      } else {
        val parent = body.parent
        if (parent is KtClass && parent.isEnum() && children.isNotEmpty()) {
          builder.token(";")
          builder.forcedBreak()
        }
      }

      var prev: PsiElement? = null
      for (curr in members) {
        val blankLineBetweenMembers =
            when {
              prev == null -> OpsBuilder.BlankLineWanted.PRESERVE
              prev !is KtProperty -> OpsBuilder.BlankLineWanted.YES
              prev.getter != null || prev.setter != null -> OpsBuilder.BlankLineWanted.YES
              curr is KtProperty -> OpsBuilder.BlankLineWanted.PRESERVE
              else -> OpsBuilder.BlankLineWanted.YES
            }
        builder.blankLineWanted(blankLineBetweenMembers)

        builder.block(ZERO) { visit(curr) }
        builder.guessToken(";")
        builder.forcedBreak()

        prev = curr
      }
    }
  }

  override fun visitBlockExpression(expression: KtBlockExpression) {
    builder.sync(expression)
    emitBracedBlock(expression) { children -> visitStatements(children) }
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
      emitKeywordWithCondition("if", expression.condition)

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

  /** Example `a[3]`, `b["a", 5]` or `a.b.c[4]` */
  override fun visitArrayAccessExpression(expression: KtArrayAccessExpression) {
    builder.sync(expression)
    if (expression.arrayExpression is KtQualifiedExpression) {
      emitQualifiedExpression(expression)
    } else {
      visit(expression.arrayExpression)
      visitArrayAccessBrackets(expression)
    }
  }

  /**
   * Example `[3]` in `a[3]` or `a[3].b` Separated since it needs to be used from a top level array
   * expression (`a[3]`) and from within a qualified chain (`a[3].b)
   */
  private fun visitArrayAccessBrackets(expression: KtArrayAccessExpression) {
    builder.block(ZERO) {
      builder.token("[")
      builder.breakOp(Doc.FillMode.UNIFIED, "", expressionBreakIndent)
      builder.block(expressionBreakIndent) {
        visitEachCommaSeparated(
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
        visitEachCommaSeparated(
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
    builder.block(expressionBreakIndent) {
      visitEachCommaSeparated(
          list = list.parameters,
          hasTrailingComma = list.trailingComma != null,
          prefix = "<",
          postfix = ">",
          wrapInBlock = !isGoogleStyle,
      )
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
    visitEachCommaSeparated(list.constraints)
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
    emitKeywordWithCondition("while", expression.condition)
    builder.space()
    visit(expression.body)
  }

  /** Example `do { ... } while (a < b)` */
  override fun visitDoWhileExpression(expression: KtDoWhileExpression) {
    builder.sync(expression)
    builder.token("do")
    builder.space()
    if (expression.body != null) {
      visit(expression.body)
      builder.space()
    }
    emitKeywordWithCondition("while", expression.condition)
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
      val typeReference = parameter.typeReference
      if (destructuringDeclaration != null) {
        builder.block(ZERO) {
          visit(destructuringDeclaration)
          if (typeReference != null) {
            builder.token(":")
            builder.space()
            visit(typeReference)
          }
        }
      } else {
        declareOne(
            kind = DeclarationKind.PARAMETER,
            modifiers = parameter.modifierList,
            valOrVarKeyword = parameter.valOrVarKeyword?.text,
            name = parameter.nameIdentifier?.text,
            type = typeReference,
            initializer = parameter.defaultValue)
      }
    }
  }

  /** Example `String::isNullOrEmpty` */
  override fun visitCallableReferenceExpression(expression: KtCallableReferenceExpression) {
    builder.sync(expression)
    visit(expression.receiverExpression)

    // For some reason, expression.receiverExpression doesn't contain the question-mark token in
    // case of a nullable type, e.g., in String?::isNullOrEmpty.
    // Instead, KtCallableReferenceExpression exposes a method that looks for the QUEST token in
    // its children.
    if (expression.hasQuestionMarks) {
      builder.token("?")
    }

    builder.block(expressionBreakIndent) {
      builder.token("::")
      builder.breakOp(Doc.FillMode.INDEPENDENT, "", ZERO)
      visit(expression.callableReference)
    }
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

    type.contextReceiverList?.let { visitContextReceiverList(it) }

    val receiver = type.receiver
    if (receiver != null) {
      visit(receiver)
      builder.token(".")
    }
    builder.block(expressionBreakIndent) {
      val parameterList = type.parameterList
      if (parameterList != null) {
        visitEachCommaSeparated(
            parameterList.parameters,
            prefix = "(",
            postfix = ")",
            hasTrailingComma = parameterList.trailingComma != null,
        )
      }
    }
    builder.space()
    builder.token("->")
    builder.space()
    builder.block(expressionBreakIndent) { visit(type.returnTypeReference) }
  }

  /** Example `a is Int` or `b !is Int` */
  override fun visitIsExpression(expression: KtIsExpression) {
    builder.sync(expression)
    val openGroupBeforeLeft = expression.leftHandSide !is KtQualifiedExpression
    if (openGroupBeforeLeft) builder.open(ZERO)
    visit(expression.leftHandSide)
    if (!openGroupBeforeLeft) builder.open(ZERO)
    val parent = expression.parent
    if (parent is KtValueArgument ||
        parent is KtParenthesizedExpression ||
        parent is KtContainerNode) {
      builder.breakOp(Doc.FillMode.UNIFIED, " ", expressionBreakIndent)
    } else {
      builder.space()
    }
    visit(expression.operationReference)
    builder.breakOp(Doc.FillMode.INDEPENDENT, " ", expressionBreakIndent)
    builder.block(expressionBreakIndent) { visit(expression.typeReference) }
    builder.close()
  }

  /** Example `a as Int` or `a as? Int` */
  override fun visitBinaryWithTypeRHSExpression(expression: KtBinaryExpressionWithTypeRHS) {
    builder.sync(expression)
    val openGroupBeforeLeft = expression.left !is KtQualifiedExpression
    if (openGroupBeforeLeft) builder.open(ZERO)
    visit(expression.left)
    if (!openGroupBeforeLeft) builder.open(ZERO)
    builder.breakOp(Doc.FillMode.UNIFIED, " ", expressionBreakIndent)
    visit(expression.operationReference)
    builder.breakOp(Doc.FillMode.INDEPENDENT, " ", expressionBreakIndent)
    builder.block(expressionBreakIndent) { visit(expression.right) }
    builder.close()
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
    builder.block(expressionBreakIndent) {
      visitEachCommaSeparated(
          expression.getInnerExpressions(),
          expression.trailingComma != null,
          prefix = "[",
          postfix = "]",
          wrapInBlock = !isGoogleStyle)
    }
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
      enumEntry.body?.let {
        builder.space()
        visit(it)
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
    inExpression.addLast(element is KtExpression || inExpression.last())
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
    val importListEmpty = file.importList?.text?.isBlank() ?: true

    var isFirst = true
    for (child in file.children) {
      if (child.text.isBlank()) {
        continue
      }

      builder.blankLineWanted(
          when {
            isFirst -> OpsBuilder.BlankLineWanted.NO
            child is PsiComment -> continue
            child is KtScript && importListEmpty -> OpsBuilder.BlankLineWanted.PRESERVE
            else -> OpsBuilder.BlankLineWanted.YES
          })

      visit(child)
      isFirst = false
    }
    markForPartialFormat()
  }

  override fun visitScript(script: KtScript) {
    markForPartialFormat()
    var lastChildHadBlankLineBefore = false
    var lastChildIsContextReceiver = false
    var first = true
    for (child in script.blockExpression.children) {
      if (child.text.isBlank()) {
        continue
      }
      builder.forcedBreak()
      val childGetsBlankLineBefore = child !is KtProperty
      if (first) {
        builder.blankLineWanted(OpsBuilder.BlankLineWanted.PRESERVE)
      } else if (lastChildIsContextReceiver) {
        builder.blankLineWanted(OpsBuilder.BlankLineWanted.NO)
      } else if (child !is PsiComment &&
          (childGetsBlankLineBefore || lastChildHadBlankLineBefore)) {
        builder.blankLineWanted(OpsBuilder.BlankLineWanted.YES)
      }
      visit(child)
      builder.guessToken(";")
      lastChildHadBlankLineBefore = childGetsBlankLineBefore
      lastChildIsContextReceiver =
          child is KtScriptInitializer &&
              child.firstChild?.firstChild?.firstChild?.text == "context"
      first = false
    }
    markForPartialFormat()
  }

  /**
   * markForPartialFormat is used to delineate the smallest areas of code that must be formatted
   * together.
   *
   * When only parts of the code are being formatted, the requested area is expanded until it's
   * covered by an area marked by this method.
   */
  private fun markForPartialFormat() {
    if (!inExpression.last()) {
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
  private fun OpsBuilder.block(plusIndent: Indent, isEnabled: Boolean = true, block: () -> Unit) {
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

  /** Prevent subsequent comments from being moved ahead of this point, into parent [Level]s. */
  private fun OpsBuilder.fenceComments() {
    addAll(FenceCommentsOp.AS_LIST)
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

  /** Emits a key word followed by a condition, e.g. `if (b)` or `while (c < d )` */
  private fun emitKeywordWithCondition(keyword: String, condition: KtExpression?) {
    if (condition == null) {
      builder.token(keyword)
      return
    }

    builder.block(ZERO) {
      builder.token(keyword)
      builder.space()
      builder.token("(")
      if (isGoogleStyle) {
        builder.block(expressionBreakIndent) {
          builder.breakOp(Doc.FillMode.UNIFIED, "", ZERO)
          visit(condition)
          builder.breakOp(Doc.FillMode.UNIFIED, "", expressionBreakNegativeIndent)
        }
      } else {
        builder.block(ZERO) { visit(condition) }
      }
    }
    builder.token(")")
  }
}
