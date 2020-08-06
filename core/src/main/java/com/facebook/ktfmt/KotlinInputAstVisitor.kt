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

import com.google.common.base.Throwables
import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableSortedSet
import com.google.googlejavaformat.Doc
import com.google.googlejavaformat.FormattingError
import com.google.googlejavaformat.Indent
import com.google.googlejavaformat.Indent.Const.ZERO
import com.google.googlejavaformat.OpsBuilder
import com.google.googlejavaformat.Output
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import java.util.ArrayDeque
import java.util.Deque
import java.util.LinkedHashSet
import java.util.Optional
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
import org.jetbrains.kotlin.psi.KtPropertyDelegate
import org.jetbrains.kotlin.psi.KtQualifiedExpression
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.psi.KtReturnExpression
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
import org.jetbrains.kotlin.types.Variance

/** An AST visitor that builds a stream of {@link Op}s to format. */
class KotlinInputAstVisitor(
    blockIndent: Int, val continuationIndent: Int, private val builder: OpsBuilder
) : KtTreeVisitorVoid() {

  /** Standard indentation for a block */
  private val blockIndent: Indent.Const = Indent.Const.make(blockIndent, 1)

  /**
   * Standard indentation for a long expression or function call, it is different than block
   * indentation on purpose
   */
  private val expressionBreakIndent: Indent.Const = Indent.Const.make(continuationIndent, 1)

  private val expressionBreakNegativeIndent: Indent.Const =
      Indent.Const.make(-continuationIndent, 1)

  /** A record of whether we have visited into an expression. */
  private val inExpression = ArrayDeque(ImmutableList.of(false))

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
    val hasParentheses = typeReference.hasParentheses()
    if (hasParentheses) {
      builder.token("(")
    }
    typeReference.modifierList?.accept(this)
    typeReference.typeElement?.accept(this)
    if (hasParentheses) {
      builder.token(")")
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
    nullableType.modifierList?.accept(this)
    innerType?.accept(this)
    if (addParenthesis) {
      builder.token(")")
    }
    builder.token("?")
  }

  /** Example: `String` or `List<Int>`, */
  override fun visitUserType(type: KtUserType) {
    builder.sync(type)

    if (type.qualifier != null) {
      type.qualifier?.accept(this)
      builder.token(".")
    }
    type.referenceExpression?.accept(this)
    val typeArgumentList = type.typeArgumentList
    if (typeArgumentList != null) {
      builder.block(expressionBreakIndent) { typeArgumentList.accept(this) }
    }
  }

  /** Example `<Int, String>` in `List<Int, String>` */
  override fun visitTypeArgumentList(typeArgumentList: KtTypeArgumentList) {
    builder.sync(typeArgumentList)
    builder.block(ZERO) {
      builder.token("<")
      builder.breakOp(Doc.FillMode.UNIFIED, "", ZERO)
      builder.block(ZERO) {
        emitParameterLikeList(typeArgumentList.arguments, typeArgumentList.trailingComma != null)
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
        typeReference?.accept(this)
      }
      KtProjectionKind.OUT -> {
        builder.token("out")
        builder.space()
        typeReference?.accept(this)
      }
      KtProjectionKind.STAR -> builder.token("*")
      KtProjectionKind.NONE -> typeReference?.accept(this)
    }
  }

  /**
   * @param keyword e.g., "fun" or "class".
   * @param type for functions, the return type; for classes, the list of supertypes.
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
      nonBlockBodyExpressions: PsiElement?,
      type: KtElement?,
      emitBraces: Boolean
  ) {
    builder.block(ZERO) {
      if (modifierList != null) {
        visitModifierList(modifierList)
      }
      builder.token(keyword)
      if (typeParameters != null) {
        builder.space()
        builder.block(ZERO) { typeParameters.accept(this) }
      }

      if (name != null || receiverTypeReference != null) {
        builder.space()
      }
      if (receiverTypeReference != null) {
        receiverTypeReference.accept(this)
        builder.token(".")
      }
      if (name != null) {
        builder.token(name)
      }
      if (emitParenthesis) {
        builder.token("(")
      }
      builder.block(ZERO) {
        if (parameterList != null && parameterList.parameters.isNotEmpty()) {
          builder.breakOp(Doc.FillMode.UNIFIED, "", expressionBreakIndent)
          builder.block(expressionBreakIndent) { parameterList.accept(this) }
        }
        if (emitParenthesis) {
          if (parameterList != null && parameterList.parameters.isNotEmpty()) {
            builder.breakOp(Doc.FillMode.UNIFIED, "", ZERO)
          }
          builder.token(")")
        }
        if (type != null) {
          builder.block(ZERO) {
            builder.token(":")
            if (parameterList?.parameters.isNullOrEmpty()) {
              builder.breakOp(Doc.FillMode.INDEPENDENT, " ", expressionBreakIndent)
            } else {
              builder.space()
            }
            builder.block(expressionBreakIndent) { type.accept(this) }
          }
        }
      }
      builder.space()
      if (typeConstraintList != null) {
        typeConstraintList.accept(this)
        builder.space()
      }
      if (bodyBlockExpression != null) {
        visitBlockBody(bodyBlockExpression, emitBraces)
      } else if (nonBlockBodyExpressions != null) {
        builder.block(ZERO) {
          builder.token("=")
          builder.block(expressionBreakIndent) {
            builder.breakOp(Doc.FillMode.INDEPENDENT, " ", ZERO)
            builder.block(ZERO) { nonBlockBodyExpressions.accept(this) }
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

  private fun visitStatements(statements: Array<PsiElement>) {
    var first = true
    builder.guessToken(";")
    for (statement in statements) {
      builder.forcedBreak()
      if (!first) {
        builder.blankLineWanted(OpsBuilder.BlankLineWanted.PRESERVE)
      }
      first = false
      builder.block(ZERO) { statement.accept(this) }
      builder.guessToken(";")
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
          initializer = property.initializer)
      for (accessor in property.accessors) {
        builder.block(blockIndent) {
          builder.forcedBreak()
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
    builder.guessToken(";")
    builder.forcedBreak()
  }

  /** Tracks whether we are handling an import directive */
  private var inImport = false

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
      receiver.accept(this)
      val selectorExpression = expression.selectorExpression
      if (selectorExpression != null) {
        builder.token(".")
        selectorExpression.accept(this)
      }
      return
    }

    if (receiver is KtWhenExpression || receiver is KtStringTemplateExpression) {
      builder.block(ZERO) {
        receiver.accept(this)
        builder.token(expression.operationSign.value)
        expression.selectorExpression?.accept(this)
      }
      return
    }

    emitQualifiedExpression(expression)
  }

  private fun emitQualifiedExpression(expression: KtExpression) {
    val parts =
        ArrayDeque<KtExpression>().apply {
          var node: KtExpression = expression
          addFirst(node)
          while (node is KtQualifiedExpression) {
            node = node.receiverExpression
            addFirst(node)
          }
        }

    val prefixes = LinkedHashSet<Int>()

    // Check if the dot chain has a prefix that looks like a type name, so we can
    // treat the type name-shaped part as a single syntactic unit.
    TypeNameClassifier.typePrefixLength(simpleNames(parts)).ifPresent { prefixes.add(it) }

    var invocationCount = 0
    var firstInvocationIndex = -1
    var isFirstInvocationLambda = false
    for ((i, part) in parts.withIndex()) {
      val callExpression = extractCallExpression(part)
      if (callExpression != null) {
        // Don't count trailing lambdas as call expressions so they look like
        // ```
        // blah.foo().bar().map {
        //   // blah
        // }
        // ```
        if (invocationCount > 0 &&
            i == parts.size - 1 &&
            callExpression.lambdaArguments.isNotEmpty()) {
          continue
        }
        invocationCount++
        if (firstInvocationIndex < 0) {
          firstInvocationIndex = i
          if (callExpression.lambdaArguments.isNotEmpty()) {
            isFirstInvocationLambda = true
          }
        }
      }
    }

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
    val hasTrailingLambda =
        extractCallExpression(parts.last())?.lambdaArguments?.isNotEmpty() == true
    if ((invocationCount == 1 && firstInvocationIndex > 0)) {
      if (firstInvocationIndex != parts.size - 1 && isFirstInvocationLambda) {
        prefixes.add(firstInvocationIndex - 1)
      } else {
        prefixes.add(firstInvocationIndex)
      }
    }

    if (prefixes.isEmpty() &&
        (parts.first is KtSuperExpression || parts.first is KtThisExpression)) {
      prefixes.add(1)
    }

    if (prefixes.isNotEmpty() || hasTrailingLambda) {
      emitQualifiedExpressionSeveralInOneLine(parts, prefixes, Doc.FillMode.INDEPENDENT)
    } else {
      emitQualifiedExpressionOnePerLine(parts)
    }
  }

  /**
   * emitQualifiedExpression formats call expressions that are either part of a qualified
   * expression, or standing alone. This method makes it easier to handle both cases uniformly.
   */
  private fun extractCallExpression(expression: KtExpression): KtCallExpression? {
    return (expression as? KtQualifiedExpression)?.selectorExpression as? KtCallExpression
        ?: (expression as? KtCallExpression)
  }

  /**
   * Returns the simple names of expressions in a "." chain, e.g., "foo.bar().zed[5]" --> [foo, bar,
   * zed]
   */
  private fun simpleNames(stack: Deque<KtExpression>): List<String> {
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
   * Output a "regular" chain of dereferences, possibly in builder-style. Break before every dot.
   *
   * Example:
   * ```
   * fieldName
   *     .field1
   *     .field2
   *     .method1()
   *     .method2()
   *     .apply {
   *       // ...
   *     }
   * ```
   */
  private fun emitQualifiedExpressionOnePerLine(items: Collection<KtExpression>) {
    var needDot = false
    val trailingDereferences = items.size > 1
    builder.block(expressionBreakIndent) {
      // don't break after the first element if it is every small, unless the
      // chain starts with another expression
      var length = 0
      for (item in items) {
        val extractCallExpression = extractCallExpression(item)

        if (needDot) {
          if (length > continuationIndent ||
              !extractCallExpression?.lambdaArguments.isNullOrEmpty()) {
            builder.breakOp(Doc.FillMode.UNIFIED, "", ZERO)
          }
          builder.token((item as KtQualifiedExpression).operationSign.value)
          length++
        }
        emitSelectorUpToParenthesis(item)

        // Emit parenthesis and lambda.
        extractCallExpression?.apply {
          visitCallElement(
              null,
              typeArgumentList,
              valueArgumentList,
              lambdaArguments,
              argumentsIndent =
                  if (trailingDereferences || needDot) expressionBreakIndent else ZERO,
              lambdaIndent =
                  if (trailingDereferences || needDot) ZERO else expressionBreakNegativeIndent)
        }
        length += item.text.length
        needDot = true
      }
    }
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
  private fun emitQualifiedExpressionSeveralInOneLine(
      items: Collection<KtExpression>, prefixes: Collection<Int>, prefixFillMode: Doc.FillMode
  ) {
    var needDot = false
    val hasTrailingLambda =
        extractCallExpression(items.last())?.lambdaArguments?.isNotEmpty() == true
    // Are there method invocations or field accesses after the prefix?
    val trailingDereferences =
        prefixes.isNotEmpty() && prefixes.last() < items.size - (if (hasTrailingLambda) 1 else 1)

    builder.block(expressionBreakIndent) {
      for (ignored in prefixes.indices) {
        builder.open(ZERO)
      }
      if (hasTrailingLambda) {
        builder.open(ZERO)
      }

      val unconsumedPrefixes = ArrayDeque(ImmutableSortedSet.copyOf(prefixes))
      val nameTag = genSym()
      for ((i, item) in items.withIndex()) {
        if (needDot) {
          val fillMode =
              if (unconsumedPrefixes.isNotEmpty() && i <= unconsumedPrefixes.peekFirst()) {
                prefixFillMode
              } else {
                Doc.FillMode.UNIFIED
              }

          builder.breakOp(fillMode, "", ZERO, Optional.of(nameTag))
          builder.token((item as KtQualifiedExpression).operationSign.value)
        }
        emitSelectorUpToParenthesis(item)
        if (unconsumedPrefixes.isNotEmpty() && i == unconsumedPrefixes.peekFirst()) {
          builder.close()
          unconsumedPrefixes.removeFirst()
        }

        if (i == items.size - 1 && hasTrailingLambda) {
          builder.close()
        }

        val argsIndent =
            Indent.If
                .make(
                    nameTag,
                    expressionBreakIndent,
                    if (trailingDereferences) expressionBreakIndent else ZERO)

        val lambdaIndent =
            Indent.If
                .make(
                    nameTag,
                    ZERO,
                    if (trailingDereferences && !hasTrailingLambda) ZERO
                    else expressionBreakNegativeIndent)

        // Emit parenthesis and lambda.
        extractCallExpression(item)?.apply {
          visitCallElement(
              null,
              typeArgumentList,
              valueArgumentList,
              lambdaArguments,
              argumentsIndent = argsIndent,
              lambdaIndent = lambdaIndent)
        }

        needDot = true
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
      callExpression != null -> callExpression.calleeExpression?.accept(this)
      e is KtQualifiedExpression -> e.selectorExpression?.accept(this)
      else -> e.accept(this)
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
      callee?.accept(this)
      val argumentsSize = argumentList?.arguments?.size ?: 0
      builder.block(argumentsIndent) { typeArgumentList?.accept(this) }
      builder.block(argumentsIndent) {
        builder.guessToken("(")
        if (argumentsSize > 0) {
          builder.block(ZERO) { argumentList?.accept(this) }
        }
        builder.guessToken(")")
      }
      if (lambdaArguments.isNotEmpty()) {
        builder.space()
        builder.block(lambdaIndent) { lambdaArguments.forEach { it.accept(this) } }
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
      builder.block(expressionBreakNegativeIndent) { arguments.first().accept(this) }
    } else {
      // Break before args.
      builder.breakOp(Doc.FillMode.UNIFIED, "", ZERO)
      emitParameterLikeList(list.arguments, list.trailingComma != null)
    }
  }

  /** Example `{ 1 + 1 }` (as lambda) or `{ (x, y) -> x + y }` */
  override fun visitLambdaExpression(lambdaExpression: KtLambdaExpression) {
    builder.sync(lambdaExpression)
    builder.token("{")
    val valueParameters = lambdaExpression.valueParameters
    val statements = (lambdaExpression.bodyExpression ?: fail()).children
    if (valueParameters.isNotEmpty() || statements.isNotEmpty()) {
      if (valueParameters.isNotEmpty()) {
        builder.space()
        forEachCommaSeparated(valueParameters) { it.accept(this) }
        builder.space()
        builder.token("->")
        builder.breakOp(Doc.FillMode.UNIFIED, "", ZERO)
      }
      if (statements.isNotEmpty()) {
        builder.breakOp(Doc.FillMode.UNIFIED, " ", blockIndent)
        builder.block(blockIndent) {
          builder.blankLineWanted(OpsBuilder.BlankLineWanted.NO)
          if (statements.size == 1 &&
              statements.first() !is KtReturnExpression &&
              lambdaExpression.bodyExpression?.startsWithComment() != true) {
            statements[0].accept(this)
          } else {
            visitStatements(statements)
          }
        }
      }
      builder.breakOp(Doc.FillMode.UNIFIED, " ", ZERO)
      builder.blankLineWanted(OpsBuilder.BlankLineWanted.NO)
    }
    builder.token("}")
  }

  /** Example `this` or `this@Foo` */
  override fun visitThisExpression(expression: KtThisExpression) {
    builder.sync(expression)
    builder.token("this")
    expression.getTargetLabel()?.accept(this)
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
    emitParameterLikeList(list.parameters, list.trailingComma != null)
  }

  /**
   * Emit a list of elements that look like function parameters or arguments, e.g., `a, b, c` in
   * `foo(a, b, c)`
   */
  private fun <T : PsiElement> emitParameterLikeList(list: List<T>?, hasTrailingComma: Boolean) {
    if (list.isNullOrEmpty()) {
      return
    }

    builder.block(ZERO) { forEachCommaSeparated(list, hasTrailingComma) { it.accept(this) } }
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
      list: Iterable<T>, hasTrailingComma: Boolean = false, function: (T) -> Unit
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

    builder.block(ZERO) {
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
        argument.getArgumentName()?.accept(this)
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
        argument.getArgumentExpression()?.accept(this)
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
    expression.getTargetLabel()?.accept(this)
    val returnedExpression = expression.returnedExpression
    if (returnedExpression != null) {
      builder.space()
      returnedExpression.accept(this)
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

    val parts =
        ArrayDeque<KtBinaryExpression>().apply {
          val op = expression.operationToken
          var current: KtExpression? = expression
          while (current is KtBinaryExpression && current.operationToken == op) {
            addFirst(current)
            current = current.left
          }
        }

    val leftMostExpression = parts.first()
    leftMostExpression.left?.accept(this)
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
      leftExpression.right?.accept(this)
    }
    builder.close()
  }

  override fun visitUnaryExpression(expression: KtUnaryExpression) {
    builder.sync(expression)
    builder.block(ZERO) {
      expression.baseExpression?.accept(this)
      builder.token(expression.operationReference.text)
    }
  }

  override fun visitPrefixExpression(expression: KtPrefixExpression) {
    builder.sync(expression)
    builder.block(ZERO) {
      builder.token(expression.operationReference.text)
      expression.baseExpression?.accept(this)
    }
  }

  override fun visitLabeledExpression(expression: KtLabeledExpression) {
    builder.sync(expression)
    expression.labelQualifier?.accept(this)
    if (expression.baseExpression !is KtLambdaExpression) {
      builder.space()
    }
    expression.baseExpression?.accept(this)
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
      initializer: PsiElement?,
      delegate: KtPropertyDelegate? = null
  ): Int {
    val verticalAnnotationBreak = genSym()

    val isField = kind == DeclarationKind.FIELD

    if (isField) {
      builder.blankLineWanted(OpsBuilder.BlankLineWanted.conditional(verticalAnnotationBreak))
    }

    modifiers?.accept(this)
    builder.block(ZERO) {
      builder.block(ZERO) {
        if (valOrVarKeyword != null) {
          builder.token(valOrVarKeyword)
          builder.space()
        }

        if (typeParameters != null) {
          typeParameters.accept(this)
          builder.space()
        }

        // conditionally indent the name and initializer +4 if the type spans
        // multiple lines
        if (name != null) {
          if (receiver != null) {
            receiver.accept(this)
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
        type.accept(this)
      }
    }

    // For example `where T : Int` in a generic method
    if (typeConstraintList != null) {
      builder.space()
      typeConstraintList.accept(this)
      builder.space()
    }

    // for example `by lazy { compute() }`
    if (delegate != null) {
      builder.space()
      builder.token("by")
      builder.space()
      delegate.accept(this)
    } else if (initializer != null) {
      builder.space()
      builder.token("=")
      builder.breakOp(Doc.FillMode.UNIFIED, " ", expressionBreakIndent)
      builder.block(expressionBreakIndent) { initializer.accept(this) }
    }

    if (name != null) {
      builder.close() // close block for named values
    }

    if (isField) {
      builder.blankLineWanted(OpsBuilder.BlankLineWanted.conditional(verticalAnnotationBreak))
    }

    return 0
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
        classOrObject.typeParameterList?.accept(this)
      }
      classOrObject.primaryConstructor?.accept(this)
      val superTypes = classOrObject.getSuperTypeList()
      if (superTypes != null) {
        builder.space()
        builder.block(ZERO) {
          builder.token(":")
          builder.breakOp(Doc.FillMode.UNIFIED, " ", expressionBreakIndent)
          superTypes.accept(this)
        }
      }
      builder.space()
      val typeConstraintList = classOrObject.typeConstraintList
      if (typeConstraintList != null) {
        typeConstraintList.accept(this)
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
    builder.token("{", Doc.Token.RealOrImaginary.REAL, blockIndent, Optional.of(blockIndent))
    builder.open(ZERO)
    builder.block(blockIndent) {
      builder.breakOp(Doc.FillMode.UNIFIED, "", ZERO)
      val (enumEntries, nonEnumEntryStatements) = body?.children?.partition { it is KtEnumEntry }
          ?: fail()
      visitEnumEntries(enumEntries)

      builder.forcedBreak()
      builder.blankLineWanted(OpsBuilder.BlankLineWanted.PRESERVE)
      visitStatements(nonEnumEntryStatements.toTypedArray())
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
        value.accept(this)
        builder.guessToken(",")
        builder.guessToken(";")
        builder.forcedBreak()
      }
    }
  }

  override fun visitPrimaryConstructor(constructor: KtPrimaryConstructor) {
    builder.sync(constructor)
    builder.block(ZERO) {
      if (constructor.hasConstructorKeyword()) {
        builder.open(expressionBreakIndent)
        builder.breakOp(Doc.FillMode.UNIFIED, " ", ZERO)
        constructor.modifierList?.accept(this)
        builder.token("constructor")
      }

      builder.block(ZERO) {
        builder.token("(")
        builder.breakOp(Doc.FillMode.UNIFIED, "", expressionBreakIndent)
        builder.block(expressionBreakIndent) { constructor.valueParameterList?.accept(this) }
        val ownerClassOrObject = constructor.parent
        if (ownerClassOrObject is KtClassOrObject &&
            (ownerClassOrObject.body != null || ownerClassOrObject.getSuperTypeList() != null)) {
          builder.breakOp(Doc.FillMode.UNIFIED, "", ZERO)
        }
        if (constructor.hasConstructorKeyword()) {
          builder.close()
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
    builder.block(ZERO) {
      constructor.modifierList?.accept(this)
      builder.token("constructor")
      builder.token("(")
      builder.breakOp(Doc.FillMode.UNIFIED, "", expressionBreakIndent)
      builder.block(expressionBreakIndent) { constructor.valueParameterList?.accept(this) }
      if (!delegationCall.isImplicit || bodyExpression != null) {
        builder.breakOp(Doc.FillMode.UNIFIED, "", ZERO)
      }
      builder.token(")")
    }
    if (!delegationCall.isImplicit) {
      builder.space()
      builder.token(":")
      builder.space()
      delegationCall.accept(this)
    }
    if (bodyExpression != null) {
      builder.space()
      bodyExpression.accept(this)
    }
  }

  override fun visitConstructorDelegationCall(call: KtConstructorDelegationCall) {
    // Work around a misfeature in kotlin-compiler: call.calleeExpression.accept doesn't call
    // visitReferenceExpression, but calls visitElement instead.
    builder.token(if (call.isCallToThis) "this" else "super")
    visitCallElement(
        null,
        call.typeArgumentList,
        call.valueArgumentList,
        call.lambdaArguments,
        lambdaIndent = ZERO)
  }

  override fun visitClassInitializer(initializer: KtClassInitializer) {
    builder.sync(initializer)
    builder.token("init")
    builder.space()
    initializer.body?.accept(this)
  }

  override fun visitConstantExpression(expression: KtConstantExpression) {
    builder.sync(expression)
    builder.token(expression.text)
  }

  /** Example `(1 + 1)` */
  override fun visitParenthesizedExpression(expression: KtParenthesizedExpression) {
    builder.sync(expression)
    builder.token("(")
    expression.expression?.accept(this)
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
      builder.token(packageName.getReferencedName())
    }

    builder.guessToken(";")
    builder.forcedBreak()
  }

  /** Example `import com.foo.A; import com.bar.B` */
  override fun visitImportList(importList: KtImportList) {
    builder.sync(importList)
    importList.imports.forEach { it.accept(this) }
  }

  /** Example `import com.foo.A` */
  override fun visitImportDirective(directive: KtImportDirective) {
    builder.sync(directive)
    builder.token("import")
    builder.space()

    val importedReference = directive.importedReference
    if (importedReference != null) {
      inImport = true
      importedReference.accept(this)
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
        psi.accept(this)
      }

      if (onlyAnnotationsSoFar) {
        if (psi is KtAnnotationEntry && psi.valueArguments.isNotEmpty()) {
          builder.forcedBreak()
        } else {
          builder.breakOp(Doc.FillMode.UNIFIED, " ", ZERO)
        }
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
        val psi = child.psi
        when (psi) {
          is PsiWhiteSpace -> continue@loop
          is KtAnnotation -> {
            psi.accept(this)
            if (psi.entries.size != 1 || psi.entries[0].valueArguments.isNotEmpty()) {
              builder.forcedBreak()
            } else {
              builder.breakOp(Doc.FillMode.UNIFIED, " ", ZERO)
            }
          }
          is KtAnnotationEntry -> {
            psi.accept(this)
            if (psi.valueArguments.isNotEmpty()) {
              builder.forcedBreak()
            } else {
              builder.breakOp(Doc.FillMode.UNIFIED, " ", ZERO)
            }
          }
          else -> psi.accept(this)
        }
      }
    }
  }

  /** For example, @field:[Inject Named("WEB_VIEW")] */
  override fun visitAnnotation(annotation: KtAnnotation) {
    builder.sync(annotation)
    builder.block(ZERO) {
      builder.token("@")
      annotation.useSiteTarget?.accept(this)
      builder.token(":")
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

            value.accept(this)
          }
        }
      }
      builder.token("]")
    }
    builder.forcedBreak()
  }

  /** For example, 'field' in @field:[Inject Named("WEB_VIEW")] */
  override fun visitAnnotationUseSiteTarget(
      annotationTarget: KtAnnotationUseSiteTarget, data: Void?
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
      useSiteTarget.accept(this)
      builder.token(":")
    }
    visitCallElement(
        annotationEntry.calleeExpression,
        null, // Type-arguments are included in the annotation's callee expression.
        annotationEntry.valueArgumentList,
        listOf())
  }

  override fun visitFileAnnotationList(
      fileAnnotationList: KtFileAnnotationList, data: Void?
  ): Void? {
    for (child in fileAnnotationList.node.children()) {
      if (child is PsiElement) {
        continue
      }
      child.psi.accept(this)
      builder.forcedBreak()
    }

    return null
  }

  override fun visitSuperTypeList(list: KtSuperTypeList) {
    builder.sync(list)
    builder.block(expressionBreakIndent) { forEachCommaSeparated(list.entries) { it.accept(this) } }
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
    specifier.typeReference?.accept(this)
    builder.space()
    builder.token("by")
    builder.space()
    specifier.delegateExpression?.accept(this)
  }

  override fun visitWhenExpression(expression: KtWhenExpression) {
    builder.sync(expression)
    builder.block(ZERO) {
      builder.token("when")
      expression.subjectExpression?.let { subjectExp ->
        builder.space()
        builder.token("(")
        builder.block(ZERO) { subjectExp.accept(this) }
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
              forEachCommaSeparated(whenEntry.conditions.asIterable()) { it.accept(this) }
              builder.guessToken(",")
            }
          }
          val whenExpression = whenEntry.expression
          builder.space()
          builder.token("->")
          if (whenExpression is KtBlockExpression) {
            builder.space()
            whenExpression.accept(this)
          } else {
            builder.block(expressionBreakIndent) {
              builder.breakOp(Doc.FillMode.INDEPENDENT, " ", ZERO)
              whenExpression?.accept(this)
            }
          }
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
    condition.expression?.accept(this)
  }

  override fun visitWhenConditionIsPattern(condition: KtWhenConditionIsPattern) {
    builder.sync(condition)
    builder.token(if (condition.isNegated) "!is" else "is")
    builder.space()
    condition.typeReference?.accept(this)
  }

  /** Example `in 1..2` as part of a when expression */
  override fun visitWhenConditionInRange(condition: KtWhenConditionInRange) {
    builder.sync(condition)
    // TODO: replace with 'condition.isNegated' once https://youtrack.jetbrains.com/issue/KT-34395
    // is fixed.
    val isNegated = condition.firstChild?.node?.findChildByType(KtTokens.NOT_IN) != null
    builder.token(if (isNegated) "!in" else "in")
    builder.space()
    condition.rangeExpression?.accept(this)
  }

  override fun visitIfExpression(expression: KtIfExpression) {
    builder.sync(expression)
    builder.block(ZERO) {
      builder.token("if")
      builder.space()
      builder.token("(")
      builder.block(ZERO) { expression.condition?.accept(this) }
      builder.token(")")

      if (expression.then is KtBlockExpression) {
        builder.space()
        expression.then?.accept(this)
      } else {
        builder.breakOp(Doc.FillMode.INDEPENDENT, " ", expressionBreakIndent)
        builder.block(expressionBreakIndent) { expression.then?.accept(this) }
      }

      if (expression.elseKeyword != null) {
        if (expression.then is KtBlockExpression) {
          builder.space()
        } else {
          builder.breakOp(Doc.FillMode.UNIFIED, " ", ZERO)
        }

        builder.token("else")
        if (expression.`else` is KtBlockExpression || expression.`else` is KtIfExpression) {
          builder.space()
          builder.block(ZERO) { expression.`else`?.accept(this) }
        } else {
          builder.breakOp(Doc.FillMode.INDEPENDENT, " ", expressionBreakIndent)
          builder.block(expressionBreakIndent) { expression.`else`?.accept(this) }
        }
      }
    }
  }

  /** Example `a[3]` or `b["a", 5]` */
  override fun visitArrayAccessExpression(expression: KtArrayAccessExpression) {
    builder.sync(expression)
    expression.arrayExpression?.accept(this)
    builder.block(ZERO) {
      builder.token("[")
      builder.breakOp(Doc.FillMode.UNIFIED, "", expressionBreakIndent)
      builder.block(expressionBreakIndent) {
        emitParameterLikeList(expression.indexExpressions, expression.trailingComma != null)
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
    builder.block(ZERO) {
      builder.token("(")
      builder.breakOp(Doc.FillMode.UNIFIED, "", expressionBreakIndent)
      builder.block(expressionBreakIndent) {
        emitParameterLikeList(
            destructuringDeclaration.entries, destructuringDeclaration.trailingComma != null)
      }
    }
    builder.token(")")
    val initializer = destructuringDeclaration.initializer
    if (initializer != null) {
      builder.space()
      builder.token("=")
      builder.space()
      initializer.accept(this)
    }
  }

  /** Example `a: String` which is part of `(a: String, b: String)` */
  override fun visitDestructuringDeclarationEntry(
      multiDeclarationEntry: KtDestructuringDeclarationEntry
  ) {
    builder.sync(multiDeclarationEntry)
    builder.token(multiDeclarationEntry.nameIdentifier?.text ?: fail())
    val typeReference = multiDeclarationEntry.typeReference
    if (typeReference != null) {
      builder.token(":")
      builder.space()
      typeReference.accept(this)
    }
  }

  /** Example `"Hello $world!"` or `"""Hello world!"""` */
  override fun visitStringTemplateExpression(expression: KtStringTemplateExpression) {
    builder.sync(expression)
    builder.token(replaceTrailingWhitespaceWithTombstone(expression.text))
  }

  /** Example `super` in `super.doIt(5)` or `super<Foo>` in `super<Foo>.doIt(5)` */
  override fun visitSuperExpression(expression: KtSuperExpression) {
    builder.sync(expression)
    builder.token("super")
    val superTypeQualifier = expression.superTypeQualifier
    if (superTypeQualifier != null) {
      builder.token("<")
      superTypeQualifier.accept(this)
      builder.token(">")
    }
    expression.labelQualifier?.accept(this)
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
          emitParameterLikeList(list.parameters, list.trailingComma != null)
        }
      }
      builder.token(">")
    }
  }

  override fun visitTypeParameter(parameter: KtTypeParameter) {
    builder.sync(parameter)
    if (parameter.hasModifier(KtTokens.REIFIED_KEYWORD)) {
      builder.token("reified")
      builder.space()
    }
    when (parameter.variance) {
      Variance.INVARIANT -> {}
      Variance.IN_VARIANCE -> {
        builder.token("in")
        builder.space()
      }
      Variance.OUT_VARIANCE -> {
        builder.token("out")
        builder.space()
      }
    }
    builder.token(parameter.nameIdentifier?.text ?: "")
    val extendsBound = parameter.extendsBound
    if (extendsBound != null) {
      builder.space()
      builder.token(":")
      builder.space()
      extendsBound.accept(this)
    }
  }

  /** Example `where T : View, T : Listener` */
  override fun visitTypeConstraintList(list: KtTypeConstraintList) {
    builder.token("where")
    builder.space()
    builder.sync(list)
    forEachCommaSeparated(list.constraints) { it.accept(this) }
  }

  /** Example `T : Foo` */
  override fun visitTypeConstraint(constraint: KtTypeConstraint) {
    builder.sync(constraint)
    constraint.subjectTypeParameterName?.accept(this)
    builder.space()
    builder.token(":")
    builder.space()
    constraint.boundTypeReference?.accept(this)
  }

  /** Example `for (i in items) { ... }` */
  override fun visitForExpression(expression: KtForExpression) {
    builder.sync(expression)
    builder.block(ZERO) {
      builder.token("for")
      builder.space()
      builder.token("(")
      expression.loopParameter?.accept(this)
      builder.space()
      builder.token("in")
      builder.block(ZERO) {
        builder.breakOp(Doc.FillMode.UNIFIED, " ", expressionBreakIndent)
        builder.block(expressionBreakIndent) { expression.loopRange?.accept(this) }
      }
      builder.token(")")
      builder.space()
      expression.body?.accept(this)
    }
  }

  /** Example `while (a < b) { ... }` */
  override fun visitWhileExpression(expression: KtWhileExpression) {
    builder.sync(expression)
    builder.token("while")
    builder.space()
    builder.token("(")
    expression.condition?.accept(this)
    builder.token(")")
    builder.space()
    expression.body?.accept(this)
  }

  /** Example `do { ... } while (a < b)` */
  override fun visitDoWhileExpression(expression: KtDoWhileExpression) {
    builder.sync(expression)
    builder.token("do")
    builder.space()
    expression.body?.accept(this)
    builder.space()
    builder.token("while")
    builder.space()
    builder.token("(")
    expression.condition?.accept(this)
    builder.token(")")
  }

  /** Example `break` or `break@foo` in a loop */
  override fun visitBreakExpression(expression: KtBreakExpression) {
    builder.sync(expression)
    builder.token("break")
    expression.labelQualifier?.accept(this)
  }

  /** Example `continue` or `continue@foo` in a loop */
  override fun visitContinueExpression(expression: KtContinueExpression) {
    builder.sync(expression)
    builder.token("continue")
    expression.labelQualifier?.accept(this)
  }

  /** Example `f: String`, or `private val n: Int` or `(a: Int, b: String)` (in for-loops) */
  override fun visitParameter(parameter: KtParameter) {
    builder.sync(parameter)
    builder.block(ZERO) {
      val destructuringDeclaration = parameter.destructuringDeclaration
      if (destructuringDeclaration != null) {
        destructuringDeclaration.accept(this)
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
    expression.receiverExpression?.accept(this)
    builder.token("::")
    expression.callableReference.accept(this)
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
      receiverExpression?.accept(this)
    }
    builder.token("::")
    builder.token("class")
  }

  override fun visitFunctionType(type: KtFunctionType) {
    builder.sync(type)
    val receiver = type.receiver
    if (receiver != null) {
      receiver.accept(this)
      builder.token(".")
    }
    builder.block(expressionBreakIndent) {
      builder.token("(")
      type.parameterList?.accept(this)
    }
    builder.token(")")
    builder.space()
    builder.token("->")
    builder.space()
    builder.block(expressionBreakIndent) { type.returnTypeReference?.accept(this) }
  }

  /** Example `a is Int` or `b !is Int` */
  override fun visitIsExpression(expression: KtIsExpression) {
    builder.sync(expression)
    expression.leftHandSide.accept(this)
    builder.space()
    expression.operationReference.accept(this)
    builder.space()
    expression.typeReference?.accept(this)
  }

  /** Example `a as Int` or `a as? Int` */
  override fun visitBinaryWithTypeRHSExpression(expression: KtBinaryExpressionWithTypeRHS) {
    builder.sync(expression)
    expression.left.accept(this)
    builder.space()
    expression.operationReference.accept(this)
    builder.space()
    expression.right?.accept(this)
  }

  /**
   * Example:
   * ```
   * fun f() {
   *   val a: Array<Int> = [1, 2, 3]
   * }
   * ```
   */
  override fun visitCollectionLiteralExpression(
      expression: KtCollectionLiteralExpression, data: Void?
  ): Void? {
    builder.sync(expression)
    builder.block(ZERO) {
      builder.token("[")
      builder.breakOp(Doc.FillMode.UNIFIED, "", expressionBreakIndent)
      builder.block(expressionBreakIndent) {
        emitParameterLikeList(expression.getInnerExpressions(), expression.trailingComma != null)
      }
    }
    builder.token("]")
    return null
  }

  override fun visitTryExpression(expression: KtTryExpression) {
    builder.sync(expression)
    builder.token("try")
    builder.space()
    expression.tryBlock.accept(this)
    for (catchClause in expression.catchClauses) {
      catchClause.accept(this)
    }
    expression.finallyBlock?.accept(this)
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
        catchClause.catchParameter?.accept(this)
        builder.guessToken(",")
      }
    }
    builder.token(")")
    builder.space()
    catchClause.catchBody?.accept(this)
  }

  override fun visitFinallySection(finallySection: KtFinallySection) {
    builder.sync(finallySection)
    builder.space()
    builder.token("finally")
    builder.space()
    finallySection.finalExpression.accept(this)
  }

  override fun visitThrowExpression(expression: KtThrowExpression) {
    builder.sync(expression)
    builder.token("throw")
    builder.space()
    expression.thrownExpression?.accept(this)
  }

  /** Example `RED(0xFF0000)` in an enum class */
  override fun visitEnumEntry(enumEntry: KtEnumEntry) {
    builder.sync(enumEntry)
    builder.block(ZERO) {
      enumEntry.modifierList?.accept(this)
      builder.token(enumEntry.nameIdentifier?.text ?: fail())
      enumEntry.initializerList?.initializers?.forEach { it.accept(this) }
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
      typeAlias.modifierList?.accept(this)
      builder.token("typealias")
      builder.space()
      builder.token(typeAlias.nameIdentifier?.text ?: fail())
      typeAlias.typeParameterList?.accept(this)

      builder.space()
      builder.token("=")
      builder.breakOp(Doc.FillMode.INDEPENDENT, " ", expressionBreakIndent)
      builder.block(expressionBreakIndent) {
        typeAlias.getTypeReference()?.accept(this)
        typeAlias.typeConstraintList?.accept(this)
        builder.guessToken(";")
        val peekToken = builder.peekToken()
      }
      builder.forcedBreak()
    }
  }

  /**
   * visitElement is called for almost all types of AST nodes. We use it to keep track of whether
   * we're currently inside an expression or not.
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
    for (child in file.children) {
      if (child.text.isBlank()) {
        continue
      }
      if (child !is PsiComment) {
        builder.blankLineWanted(OpsBuilder.BlankLineWanted.YES)
      }
      child.accept(this)
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
  private inline fun OpsBuilder.block(plusIndent: Indent, block: () -> Unit) {
    open(plusIndent)
    block()
    close()
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
}
