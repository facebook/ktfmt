// Copyright (c) Facebook, Inc. and its affiliates.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.facebook.ktfmt

import com.google.common.base.Throwables
import com.google.common.collect.ImmutableList
import com.google.googlejavaformat.Doc
import com.google.googlejavaformat.FormattingError
import com.google.googlejavaformat.Indent
import com.google.googlejavaformat.Indent.Const.ZERO
import com.google.googlejavaformat.OpsBuilder
import com.google.googlejavaformat.Output
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import java.util.ArrayDeque
import java.util.Optional
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtAnnotationEntry
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
import org.jetbrains.kotlin.psi.KtContinueExpression
import org.jetbrains.kotlin.psi.KtDelegatedSuperTypeEntry
import org.jetbrains.kotlin.psi.KtDestructuringDeclaration
import org.jetbrains.kotlin.psi.KtDestructuringDeclarationEntry
import org.jetbrains.kotlin.psi.KtDoWhileExpression
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtEnumEntry
import org.jetbrains.kotlin.psi.KtEscapeStringTemplateEntry
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
import org.jetbrains.kotlin.psi.KtLiteralStringTemplateEntry
import org.jetbrains.kotlin.psi.KtModifierList
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtNullableType
import org.jetbrains.kotlin.psi.KtPackageDirective
import org.jetbrains.kotlin.psi.KtParameter
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
import org.jetbrains.kotlin.psi.KtSimpleNameStringTemplateEntry
import org.jetbrains.kotlin.psi.KtStringTemplateEntryWithExpression
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
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
import org.jetbrains.kotlin.psi.psiUtil.isSingleQuoted
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.psi.stubs.elements.KtAnnotationEntryElementType
import org.jetbrains.kotlin.types.Variance

/** An AST visitor that builds a stream of {@link Op}s to format. */
class KotlinInputAstVisitor(val builder: OpsBuilder) : KtTreeVisitorVoid() {

  /** Standard indentation for a block */
  private val blockIndent: Indent.Const = Indent.Const.make(+2, 1)

  /**
   * Standard indentation for a long expression or function call, it is different than block
   * indentation on purpose
   */
  private val expressionBreakIndent: Indent.Const = Indent.Const.make(+4, 1)

  /** A record of whether we have visited into an expression.  */
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
          function.valueParameters,
          function.typeConstraintList,
          function.bodyBlockExpression,
          function.bodyExpression,
          function.typeReference,
          function.bodyBlockExpression?.lBrace != null)
    }
    if (function.parent is KtFile) {
      builder.blankLineWanted(OpsBuilder.BlankLineWanted.YES)
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

  /** Example: `String?` or `((Int) -> Unit)?` */
  override fun visitNullableType(nullableType: KtNullableType) {
    builder.sync(nullableType)
    val innerType = nullableType.innerType
    val addParenthesis = innerType is KtFunctionType
    if (addParenthesis) {
      builder.token("(")
    }
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
    type.typeArgumentList?.accept(this)
  }

  /** Example `<Int, String>` in `List<Int, String>` */
  override fun visitTypeArgumentList(typeArgumentList: KtTypeArgumentList) {
    builder.sync(typeArgumentList)
    builder.token("<")
    builder.block(expressionBreakIndent) {
      val arguments = typeArgumentList.arguments
      forEachCommaSeparated(arguments) { it.accept(this) }
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
      parameters: List<KtParameter>?,
      typeConstraintList: KtTypeConstraintList?,
      bodyBlockExpression: KtBlockExpression?,
      nonBlockBodyExpressions: PsiElement?,
      type: KtElement?,
      emitBraces: Boolean
  ) {
    builder.block(ZERO) {
      modifierList?.accept(this)
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
        if (parameters != null && parameters.isNotEmpty()) {
          builder.breakOp(Doc.FillMode.UNIFIED, "", expressionBreakIndent)
          builder.block(expressionBreakIndent) { visitFormals(parameters) }
        }
        if (emitParenthesis) {
          if (parameters != null && parameters.isNotEmpty()) {
            builder.breakOp(Doc.FillMode.UNIFIED, "", ZERO)
          }
          builder.token(")")
        }
        if (type != null) {
          builder.block(ZERO) {
            builder.token(":")
            if (parameters.isNullOrEmpty()) {
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

  private fun visitFormals(parameters: List<KtParameter>) {
    if (parameters.isEmpty()) {
      return
    }
    forEachCommaSeparated(parameters) { it.accept(this) }
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
    for (statement in statements) {
      builder.guessToken(";")
      builder.forcedBreak()
      if (!first) {
        builder.blankLineWanted(OpsBuilder.BlankLineWanted.PRESERVE)
      }
      first = false
      builder.block(ZERO) { statement.accept(this) }
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
    }
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
            accessor.parameterList?.parameters,
            null,
            accessor.bodyBlockExpression,
            accessor.bodyExpression,
            accessor.returnTypeReference,
            accessor.bodyBlockExpression?.lBrace != null)
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
    if (inImport) {
      expression.receiverExpression.accept(this)
      val selectorExpression = expression.selectorExpression
      if (selectorExpression != null) {
        builder.token(".")
        selectorExpression.accept(this)
      }
      return
    }

    val parts =
        ArrayDeque<KtQualifiedExpression>()
            .apply {
          var current: KtExpression = expression
          while (current is KtQualifiedExpression) {
            addFirst(current)
            current = current.receiverExpression
          }
        }

    val leftMostExpression = parts.first()
    leftMostExpression.receiverExpression.accept(this)
    for (receiver in parts) {
      val isFirst = receiver === leftMostExpression
      if (!isFirst || receiver.receiverExpression is KtCallExpression) {
        builder.breakOp(Doc.FillMode.UNIFIED, "", expressionBreakIndent)
      }
      builder.token(receiver.operationSign.value)
      builder.block(if (isFirst) ZERO else expressionBreakIndent) {
        receiver.selectorExpression
            ?.accept(this)
      }
    }
  }

  override fun visitCallExpression(callExpression: KtCallExpression) {
    builder.sync(callExpression)
    builder.block(ZERO) {
      visitCallElement(
          callExpression.calleeExpression,
          callExpression.typeArgumentList,
          callExpression.valueArgumentList,
          callExpression.lambdaArguments)
      builder.guessToken(";")
    }
  }

  /** Examples `foo<T>(a, b)`, `foo(a)`, `boo()`, `super(a)` */
  private fun visitCallElement(
      callee: KtExpression?,
      typeArgumentList: KtTypeArgumentList?,
      argumentList: KtValueArgumentList?,
      lambdaArguments: List<KtLambdaArgument>
  ) {
    builder.block(ZERO) {
      callee?.accept(this)
      val argumentsSize = argumentList?.arguments?.size ?: 0
      typeArgumentList?.accept(this)
      builder.guessToken("(")
      if (argumentsSize > 0) {
        builder.block(ZERO) { argumentList?.accept(this) }
      }
      builder.guessToken(")")
      if (lambdaArguments.isNotEmpty()) {
        if (argumentsSize == 0) {}
        builder.space()
        lambdaArguments.forEach { it.accept(this) }
      }
    }
  }

  /** Example `(1, "hi")` in a function call */
  override fun visitValueArgumentList(list: KtValueArgumentList) {
    builder.sync(list)
    // Break before args.
    builder.breakOp(Doc.FillMode.UNIFIED, "", expressionBreakIndent)
    builder.block(expressionBreakIndent) { forEachCommaSeparated(list.arguments) { it.accept(this) }
    }
  }

  /** Example `{ 1 + 1 }` (as lambda) or `{ (x, y) -> x + y }` */
  override fun visitLambdaExpression(lambdaExpression: KtLambdaExpression) {
    builder.sync(lambdaExpression)
    builder.token("{")
    val valueParameters = lambdaExpression.valueParameters
    val statements = (lambdaExpression.bodyExpression ?: fail()).children
    if (valueParameters.isNotEmpty() || statements.isNotEmpty()) {
      builder.block(blockIndent) {
        if (valueParameters.isNotEmpty()) {
          builder.space()
          forEachCommaSeparated(valueParameters) { it.accept(this) }
          builder.space()
          builder.token("->")
          builder.breakOp(Doc.FillMode.UNIFIED, "", ZERO)
        }
        if (statements.isNotEmpty()) {
          builder.breakOp(Doc.FillMode.UNIFIED, " ", ZERO)
          builder.blankLineWanted(OpsBuilder.BlankLineWanted.NO)
          if (statements.size == 1 && statements[0] !is KtReturnExpression) {
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

  private fun <T> forEachCommaSeparated(
      list: Iterable<T>,
      delimiter: (() -> Unit)? = null,
      function: (T) -> Unit
  ) {
    builder.block(ZERO) {
      var first = true
      builder.breakOp(Doc.FillMode.UNIFIED, "", ZERO)
      for (value in list) {
        if (!first) {
          if (delimiter != null) {
            delimiter()
          } else {
            builder.token(",")
            builder.breakOp(Doc.FillMode.UNIFIED, " ", ZERO)
          }
        }
        first = false

        function(value)
      }
    }
  }

  /** Example `a` in `foo(a)`, or `*a`, or `limit = 50` */
  override fun visitArgument(argument: KtValueArgument) {
    builder.sync(argument)
    builder.block(ZERO) {
      val hasArgName = argument.getArgumentName() != null
      if (hasArgName) {
        argument.getArgumentName()?.accept(this)
        builder.space()
        builder.token("=")
        builder.breakOp(
            Doc.FillMode.INDEPENDENT, if (hasArgName) " " else "", expressionBreakIndent)
      }
      builder.block(ZERO) {
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
    builder.space()
    expression.returnedExpression?.accept(this)
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
        ArrayDeque<KtBinaryExpression>()
            .apply {
          var current: KtExpression? = expression
          while (current is KtBinaryExpression) {
            addFirst(current)
            current = current.left
          }
        }

    val leftMostExpression = parts.first()
    leftMostExpression.left?.accept(this)
    for (leftExpression in parts) {
      val surroundWithSpace = leftExpression.operationToken != KtTokens.RANGE
      if (surroundWithSpace) {
        builder.space()
      }
      builder.token(leftExpression.operationReference.text)
      val isFirst = leftExpression === leftMostExpression
      if (isFirst) {
        builder.open(expressionBreakIndent)
      }
      if (surroundWithSpace) {
        builder.breakOp(Doc.FillMode.UNIFIED, " ", ZERO)
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

    if (modifiers != null) {
      visitAnnotationBeforeModifiers(modifiers)
    }
    builder.block(ZERO) {
      builder.block(ZERO) {
        if (modifiers != null) {
          visitKeywordModifiers(modifiers)
        }
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

      // Emits ": String" in "val thisIsALongName : String"
      if (type != null) {
        if (name != null) {
          builder.breakOp(Doc.FillMode.INDEPENDENT, "", expressionBreakIndent)
          builder.open(ZERO)
          builder.token(":")
          builder.space()
        }
        type.accept(this)
      }
    }

    if (typeConstraintList != null) {
      builder.space()
      typeConstraintList.accept(this)
      builder.space()
    }
    if (delegate != null) {
      builder.space()
      builder.token("by")
      builder.space()
      delegate.accept(this)
    }
    if (initializer != null) {
      builder.space()
      builder.token("=")
    }
    if (type != null && name != null) {
      builder.close()
    }
    if (initializer != null) {
      builder.breakOp(Doc.FillMode.INDEPENDENT, " ", expressionBreakIndent)
      builder.block(expressionBreakIndent) { initializer.accept(this) }
    }

    if (isField) {
      builder.blankLineWanted(OpsBuilder.BlankLineWanted.conditional(verticalAnnotationBreak))
    }

    return 0
  }

  override fun visitClassOrObject(classOrObject: KtClassOrObject) {
    builder.sync(classOrObject)
    classOrObject.modifierList?.accept(this)
    builder.block(ZERO) {
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
    }
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
      val (enumEntries, nonEnumEntryStatements) = body?.children
          ?.partition { it is KtEnumEntry } ?: fail()
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
    forEachCommaSeparated(
        enumEntries,
        delimiter =
            {
          builder.token(",")
          builder.forcedBreak()
        }) { it.accept(this) }
    builder.guessToken(",")
    builder.guessToken(";")
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
      builder.token("(")
      if (constructor.valueParameters.isNotEmpty()) {
        builder.breakOp(Doc.FillMode.UNIFIED, "", expressionBreakIndent)
        builder.block(expressionBreakIndent) { visitFormals(constructor.valueParameters) }
      }
      if (constructor.hasConstructorKeyword()) {
        builder.close()
      }
    }
    builder.token(")")
  }

  /** Example `private constructor(n: Int) : this(4, 5) { ... }` inside a class's body */
  override fun visitSecondaryConstructor(constructor: KtSecondaryConstructor) {
    builder.sync(constructor)
    builder.block(expressionBreakIndent) {
      constructor.modifierList?.accept(this)
      builder.token("constructor")
      builder.token("(")
      if (constructor.valueParameters.isNotEmpty()) {
        visitFormals(constructor.valueParameters)
      }
      builder.token(")")
    }
    val delegationCall = constructor.getDelegationCall()
    if (!delegationCall.isImplicit) {
      builder.space()
      builder.token(":")
      builder.block(expressionBreakIndent) {
        builder.breakOp(Doc.FillMode.INDEPENDENT, " ", expressionBreakIndent)
        builder.block(expressionBreakIndent) {
          builder.token(if (delegationCall.isCallToThis) "this" else "super")
          builder.token("(")
          delegationCall.accept(this)
          builder.token(")")
        }
      }
    }
    val bodyExpression = constructor.bodyExpression
    if (bodyExpression != null) {
      builder.space()
      bodyExpression.accept(this)
    }
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
    builder.blankLineWanted(OpsBuilder.BlankLineWanted.YES)
  }

  /** Example `import com.foo.A; import com.bar.B` */
  override fun visitImportList(importList: KtImportList) {
    builder.sync(importList)
    importList.imports.forEach { it.accept(this) }
    builder.blankLineWanted(OpsBuilder.BlankLineWanted.YES)
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
    visitAnnotationBeforeModifiers(list)
    visitKeywordModifiers(list)
  }

  /**
   * For example `@Magic @Fred(1, 5)`
   *
   * This visits only annotations that appear before keyword modifiers (such as `public`) since we
   * can break after annotations, but only if they appear before keywords. We avoid breaking in the
   * middle of the modifier keywords list
   */
  private fun visitAnnotationBeforeModifiers(list: KtModifierList) {
    for (child in list.node
        .children()) {
      if (child.psi is PsiWhiteSpace) {
        continue
      }
      if (child.elementType !is KtAnnotationEntryElementType) {
        break
      }
      child.psi.accept(this)
    }
  }

  /**
   * For example `private final inline`
   *
   * This visits keywords and annotations that appear after the first keyword. For example `public
   * @Inject constructor`. Ideally, you should user `@Inject public constructor` but since we cannot
   * reorder those without making possible mistakes with tokens right now, we just treat annotations
   * before the keywords differently and visit them in [visitAnnotationBeforeModifiers] instead.
   */
  private fun visitKeywordModifiers(list: KtModifierList) {
    var onlyAnnotationsSoFar = true
    for (child in list.node
        .children()) {
      if (child.psi is PsiWhiteSpace) {
        continue
      }
      if (onlyAnnotationsSoFar && child.elementType is KtAnnotationEntryElementType) {
        continue
      }
      onlyAnnotationsSoFar = false
      when (child.elementType) {
        is KtAnnotationEntryElementType ->
            visitAnnotationEntry(child.psi as KtAnnotationEntry, canBreak = false)
        is KtModifierKeywordToken -> {
          builder.token(child.text)
          builder.space()
        }
      }
    }
  }

  /** For example `@Magic` or `@Fred(1, 5)` */
  override fun visitAnnotationEntry(annotationEntry: KtAnnotationEntry) {
    visitAnnotationEntry(annotationEntry, true)
  }

  /**
   * For example `@Magic` or `@Fred(1, 5)`
   *
   * @param canBreak whether we are currently visiting annotations after which we can break the
   * line. An example of
   *    an annotation where we can't is one mixed with keywords such as in `public @Inject final`
   */
  private fun visitAnnotationEntry(annotationEntry: KtAnnotationEntry, canBreak: Boolean) {
    builder.sync(annotationEntry)
    builder.token("@")
    val useSiteTarget = annotationEntry.useSiteTarget?.getAnnotationUseSiteTarget()
    if (useSiteTarget != null) {
      builder.token(useSiteTarget.renderName)
      builder.token(":")
    }
    visitCallElement(
        annotationEntry.calleeExpression,
        annotationEntry.typeArgumentList,
        annotationEntry.valueArgumentList,
        listOf())

    if (!canBreak) {
      builder.breakToFill(" ")
    } else if (annotationEntry.parent is KtFileAnnotationList ||
        annotationEntry.valueArguments
            .isNotEmpty()) {
      builder.forcedBreak()
    } else {
      builder.breakOp(Doc.FillMode.UNIFIED, " ", ZERO)
    }
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
      expression.subjectExpression
          ?.let { subjectExp ->
            builder.space()
            builder.token("(")
            builder.block(ZERO) { subjectExp.accept(this) }
            builder.token(")")
          }
      builder.space()
      builder.token("{", Doc.Token.RealOrImaginary.REAL, blockIndent, Optional.of(blockIndent))

      expression.entries
          .forEach { whenEntry ->
            builder.block(blockIndent) {
              builder.forcedBreak()
              if (whenEntry.isElse) {
                builder.token("else")
              } else {
                builder.block(ZERO) {
                  forEachCommaSeparated(whenEntry.conditions.asIterable()) { it.accept(this) }
                }
              }
              val whenExpression = whenEntry.expression
              builder.space()
              builder.token("->")
              if (whenExpression is KtBlockExpression) {
                builder.space()
                whenExpression?.accept(this)
              } else {
                builder.breakOp(Doc.FillMode.INDEPENDENT, " ", expressionBreakIndent)
                builder.block(expressionBreakIndent) { whenExpression?.accept(this) }
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
      builder.space()
      expression.then?.accept(this)
      if (expression.elseKeyword != null) {
        if (expression.then?.text?.last() == '}') {
          builder.space()
        } else {
          builder.breakOp(Doc.FillMode.INDEPENDENT, " ", ZERO)
        }
        builder.token("else")
        builder.space()
        expression.`else`?.accept(this)
      }
    }
  }

  /** Example `a[3]` or `b["a", 5]` */
  override fun visitArrayAccessExpression(expression: KtArrayAccessExpression) {
    builder.sync(expression)
    expression.arrayExpression?.accept(this)
    builder.token("[")
    forEachCommaSeparated(expression.indexExpressions) { it.accept(this) }
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
    builder.token("(")
    forEachCommaSeparated(destructuringDeclaration.entries) { it.accept(this) }
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
    val quoteToken = if (expression.isSingleQuoted()) "\"" else "\"\"\""
    builder.token(quoteToken)
    expression.entries.forEach { it.accept(this) }
    builder.token(quoteToken)
  }

  /** Example `hello` (Inside the string literal "hello") */
  override fun visitLiteralStringTemplateEntry(entry: KtLiteralStringTemplateEntry) {
    builder.sync(entry)
    builder.token(entry.text)
  }

  /** Example `$world` (inside a String) */
  override fun visitSimpleNameStringTemplateEntry(entry: KtSimpleNameStringTemplateEntry) {
    builder.sync(entry)
    builder.token("$")
    builder.token(entry.text.substring(1))
  }

  /** Example `${1 + 2}` (inside a String) */
  override fun visitStringTemplateEntryWithExpression(entry: KtStringTemplateEntryWithExpression) {
    builder.sync(entry)
    builder.token("$" + "{")
    builder.block(ZERO) { entry.expression?.accept(this) }
    builder.token("}")
  }

  override fun visitEscapeStringTemplateEntry(entry: KtEscapeStringTemplateEntry) {
    builder.sync(entry)
    builder.token(entry.text)
  }

  /** Example `<T, S>` */
  override fun visitTypeParameterList(list: KtTypeParameterList) {
    builder.sync(list)
    builder.block(ZERO) {
      builder.token("<")
      val parameters = list.parameters
      if (parameters.isNotEmpty()) {
        // Break before args.
        builder.breakToFill("")
      }
      builder.block(ZERO) {
        forEachCommaSeparated(parameters) {
          builder.breakOp(Doc.FillMode.UNIFIED, "", ZERO)
          it.accept(this)
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
        builder.block(expressionBreakIndent) {
          expression.loopRange?.accept(this)
        }
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
      forEachCommaSeparated(type.parameters) { it.accept(this) }
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

  override fun visitCollectionLiteralExpression(
      expression: KtCollectionLiteralExpression,
      data: Void?
  ): Void? {
    builder.sync(expression)
    builder.token("[")
    forEachCommaSeparated(expression.getInnerExpressions()) { it.accept(this) }
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
    builder.token("(")
    catchClause.catchParameter?.accept(this)
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
      for (annotationEntry in enumEntry.annotationEntries) {
        annotationEntry.accept(this)
      }
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
        if (peekToken.isPresent && peekToken.get() != "typealias") {
          builder.blankLineWanted(OpsBuilder.BlankLineWanted.YES)
        }
      }
      builder.forcedBreak()
    }
  }

  /**
   * visitElement is called for almost all types of AST nodes.
   * We use it to keep track of whether we're currently inside an expression or not.
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
    super.visitKtFile(file)
    markForPartialFormat()
  }

  private fun inExpression(): Boolean {
    return inExpression.peekLast()
  }

  /**
   * markForPartialFormat is used to delineate the smallest areas of code that must be formatted together.
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
   * Emit a [Doc.Token] .
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
