package com.facebook.ktfmt

import com.google.googlejavaformat.Doc
import com.google.googlejavaformat.FormatterDiagnostic
import com.google.googlejavaformat.FormattingError
import com.google.googlejavaformat.Indent
import com.google.googlejavaformat.Indent.Const.ZERO
import com.google.googlejavaformat.OpsBuilder
import com.google.googlejavaformat.Output
import com.intellij.psi.PsiElement
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
import org.jetbrains.kotlin.psi.KtClassInitializer
import org.jetbrains.kotlin.psi.KtClassLiteralExpression
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtCollectionLiteralExpression
import org.jetbrains.kotlin.psi.KtConstantExpression
import org.jetbrains.kotlin.psi.KtContinueExpression
import org.jetbrains.kotlin.psi.KtDestructuringDeclaration
import org.jetbrains.kotlin.psi.KtDestructuringDeclarationEntry
import org.jetbrains.kotlin.psi.KtDoWhileExpression
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtEscapeStringTemplateEntry
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFileAnnotationList
import org.jetbrains.kotlin.psi.KtFinallySection
import org.jetbrains.kotlin.psi.KtForExpression
import org.jetbrains.kotlin.psi.KtFunctionType
import org.jetbrains.kotlin.psi.KtIfExpression
import org.jetbrains.kotlin.psi.KtImportDirective
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
import org.jetbrains.kotlin.psi.KtTypeArgumentList
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
import org.jetbrains.kotlin.psi.stubs.elements.KtAnnotationEntryElementType
import org.jetbrains.kotlin.types.Variance
import java.util.Optional

/**
 * An AST visitor that builds a stream of {@link Op}s to format.
 */
class KotlinInputAstVisitor(val builder: OpsBuilder, indentMultiplier: Int) : KtTreeVisitorVoid() {
  private val plusFour: Indent.Const = Indent.Const.make(+4, indentMultiplier)

  private val userTypeRecursiveQualifiedStructure = RecursiveQualifiedStructure()

  private val qualifiedExpressionRecursiveQualifiedStructure = RecursiveQualifiedStructure()

  /** Example: `fun foo(n: Int) { println(n) }` */
  override fun visitNamedFunction(function: KtNamedFunction) {
    visitFunctionLikeExpression(
        function.modifierList,
        "fun",
        function.typeParameterList,
        function.receiverTypeReference,
        function.nameIdentifier?.text ?: "",
        true,
        function.valueParameters,
        function.bodyBlockExpression,
        function.bodyExpression,
        false,
        function.typeReference,
        function.bodyBlockExpression?.lBrace != null)
  }

  /** Example: `String?` */
  override fun visitNullableType(nullableType: KtNullableType) {
    nullableType.innerType?.accept(this)
    builder.token("?")
  }

  /** Example: `String` or `List<Int>`, */
  override fun visitUserType(type: KtUserType) {
    userTypeRecursiveQualifiedStructure.visit(
        beforeSeparator = type.qualifier,
        separator = ".",
        afterSeparator = listOfNotNull(type.referenceExpression, type.typeArgumentList))
  }

  /** Example `<Int, String>` in `List<Int, String>` */
  override fun visitTypeArgumentList(typeArgumentList: KtTypeArgumentList) {
    builder.token("<")
    val arguments = typeArgumentList.arguments
    if (arguments.isNotEmpty()) {
      // Break before args.
      builder.breakToFill("")
    }
    builder.block(ZERO) {
      forEachCommaSeparated(arguments) {
        it.accept(this)
      }
    }
    builder.token(">")
  }

  override fun visitTypeProjection(typeProjection: KtTypeProjection) {
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
      bodyBlockExpression: KtBlockExpression?,
      nonBlockBodyExpressions: PsiElement?,
      spaceBeforeType: Boolean,
      type: KtElement?,
      emitBraces: Boolean
  ) {
    modifierList?.accept(this)
    builder.block(ZERO) {
      builder.token(keyword)
      if (typeParameters != null) {
        builder.space()
        typeParameters.accept(this)
      }
      if (name != null) {
        builder.space()
        if (receiverTypeReference != null) {
          receiverTypeReference.accept(this)
          builder.token(".")
        }
        builder.token(name)
      }
      if (emitParenthesis) {
        builder.token("(")
      }

      if (parameters != null && parameters.isNotEmpty()) {
        // Break before args.
        builder.breakToFill("")
        visitFormals(parameters)
      }
      if (emitParenthesis) {
        builder.token(")")
      }
    }
    if (type != null) {
      if (spaceBeforeType) {
        builder.space()
      }
      builder.block(ZERO) {
        builder.token(":")
        builder.space()
        type.accept(this)
      }
    }
    builder.space()
    if (bodyBlockExpression != null) {
      visitBlockyBody(bodyBlockExpression, emitBraces)
    } else if (nonBlockBodyExpressions != null) {
      builder.block(ZERO) {
        builder.token("=")
        builder.space()
        nonBlockBodyExpressions.accept(this)
      }
    }
    builder.forcedBreak()
  }

  private fun visitFormals(
      parameters: List<KtParameter>
  ) {
    if (parameters.isEmpty()) {
      return
    }
    builder.block(ZERO) {
      forEachCommaSeparated(parameters) {
        it.accept(this)
      }
    }
  }

  private fun genSym(): Output.BreakTag {
    return Output.BreakTag()
  }

  private fun visitBlockyBody(bodyBlockExpression: PsiElement, emitBraces: Boolean) {
    builder.open(ZERO)
    if (emitBraces) {
      builder.token("{", Doc.Token.RealOrImaginary.REAL, plusFour, Optional.of(plusFour))
    }
    if (bodyBlockExpression.children.isNotEmpty()) {
      builder.block(plusFour) {
        builder.forcedBreak()
        builder.blankLineWanted(OpsBuilder.BlankLineWanted.PRESERVE)
        visitStatements(bodyBlockExpression.children)
      }
      builder.forcedBreak()
      builder.blankLineWanted(OpsBuilder.BlankLineWanted.NO)
    }
    if (emitBraces) {
      builder.token("}", plusFour)
    }
    builder.close()
  }

  private fun visitStatements(statements: Array<PsiElement>) {
    var first = true
    for (statement in statements) {
      builder.forcedBreak()
      if (!first) {
        builder.blankLineWanted(OpsBuilder.BlankLineWanted.PRESERVE)
      }
      first = false
      statement.accept(this)
    }
  }

  override fun visitProperty(property: KtProperty) {
    declareOne(
        kind = DeclarationKind.FIELD,
        modifiers = property.modifierList,
        valOrVarKeyword = property.valOrVarKeyword.text,
        name = property.nameIdentifier?.text,
        type = property.typeReference,
        delegate = property.delegate,
        initializer = property.initializer)
    for (accessor in property.accessors) {
      builder.block(plusFour) {
        builder.forcedBreak()
        visitFunctionLikeExpression(
            accessor.modifierList,
            accessor.namePlaceholder.text,
            null,
            null,
            null,
            accessor.bodyExpression != null || accessor.bodyBlockExpression != null,
            accessor.parameterList?.parameters,
            accessor.bodyBlockExpression,
            accessor.bodyExpression,
            false,
            null,
            accessor.bodyBlockExpression?.lBrace != null)
      }
    }
    builder.forcedBreak()
  }

  /** Tracks whether we are handling an import directive */
  private var inImport = false

  /** Example: "com.facebook.bla.bla" in imports or "a.b.c.d" in expressions. */
  override fun visitQualifiedExpression(expression: KtQualifiedExpression) {
    if (inImport) {
      expression.receiverExpression.accept(this)
      val selectorExpression = expression.selectorExpression
      if (selectorExpression != null) {
        builder.token(".")
        selectorExpression.accept(this)
      }
      return
    }

    qualifiedExpressionRecursiveQualifiedStructure.visit(
        beforeSeparator = expression.receiverExpression,
        separator = expression.operationSign.value,
        afterSeparator = listOfNotNull(expression.selectorExpression))
  }

  override fun visitCallExpression(callExpression: KtCallExpression) {
    visitCallElement(
        callExpression.calleeExpression,
        callExpression.typeArgumentList,
        callExpression.valueArgumentList,
        callExpression.lambdaArguments,
        true)
  }

  /** Examples `foo<T>(a, b)`, `foo(a)`, `boo()`, `super(a)` */
  private fun visitCallElement(
      callee: KtExpression?,
      typeArgumentList: KtTypeArgumentList?,
      argumentList: KtValueArgumentList?,
      lambdaArguments: List<KtLambdaArgument>,
      forceEmitParenthesis: Boolean
  ) {
    builder.block(ZERO) {
      callee?.accept(this)
      val argumentsSize = argumentList?.arguments?.size ?: 0
      val emitParenthesis =
          when {
            argumentsSize > 0 -> true
            lambdaArguments.isEmpty() -> forceEmitParenthesis
            else -> false
          }
      typeArgumentList?.accept(this)
      if (emitParenthesis) {
        builder.token("(")
      }
      if (argumentsSize > 0) {
        argumentList?.accept(this)
      }
      if (emitParenthesis) {
        builder.token(")")
      }
      if (lambdaArguments.isNotEmpty()) {
        if (argumentsSize == 0) {
          builder.ignoreOptionalToken("(")
          builder.ignoreOptionalToken(")")
        }
        builder.space()
        lambdaArguments.forEach {
          it.accept(this)
        }
      }
    }
  }

  override fun visitValueArgumentList(list: KtValueArgumentList) {
    // Break before args.
    builder.block(plusFour) {
      forEachCommaSeparated(list.arguments) {
        builder.breakOp(Doc.FillMode.UNIFIED, "", ZERO)
        it.accept(this)
      }
    }
  }

  override fun visitLambdaExpression(lambdaExpression: KtLambdaExpression) {
    builder.block(ZERO) {
      builder.token("{")
      val valueParameters = lambdaExpression.valueParameters
      if (valueParameters.isNotEmpty()) {
        builder.space()
        forEachCommaSeparated(valueParameters) {
          it.accept(this)
        }
        builder.space()
        builder.token("->")
        builder.space()
      }
      visitBlockyBody(lambdaExpression.bodyExpression ?: fail(), false)
      builder.token("}")
    }
  }

  /** Example `this` or `this@Foo` */
  override fun visitThisExpression(expression: KtThisExpression) {
    builder.token("this")
    expression.getTargetLabel()?.accept(this)
  }

  /** Example `Foo` or `@Foo` */
  override fun visitSimpleNameExpression(expression: KtSimpleNameExpression) {
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
      else -> builder.token(expression.text)
    }
  }

  private fun <T> forEachCommaSeparated(list: Iterable<T>, function: (T) -> Unit) {
    var first = true
    for (value in list) {
      if (!first) {
        builder.token(",")
        builder.space()
      }
      first = false

      function(value)
    }
  }

  override fun visitArgument(argument: KtValueArgument) {
    if (argument.getArgumentName() != null) {
      argument.getArgumentName()?.accept(this)
      builder.space()
      builder.token("=")
      builder.space()
    }
    argument.getArgumentExpression()?.accept(this)
  }

  override fun visitReferenceExpression(expression: KtReferenceExpression) {
    builder.token(expression.text)
  }

  override fun visitReturnExpression(expression: KtReturnExpression) {
    builder.token("return")
    expression.getTargetLabel()?.accept(this)
    builder.space()
    expression.returnedExpression?.accept(this)
  }

  override fun visitBinaryExpression(expression: KtBinaryExpression) {
    val surroundWithSpace = expression.operationToken != KtTokens.RANGE

    builder.block(ZERO) {
      expression.left?.accept(this)
      if (surroundWithSpace) {
        builder.space()
      }
      builder.token(expression.operationReference.text)
      builder.block(Indent.If.make(genSym(), plusFour, ZERO)) {
        if (surroundWithSpace) {
          builder.breakToFill(" ")
        }
        expression.right?.accept(this)
      }
    }
  }

  override fun visitUnaryExpression(expression: KtUnaryExpression) {
    builder.block(ZERO) {
      expression.baseExpression?.accept(this)
      builder.token(expression.operationReference.text)
    }
  }

  override fun visitPrefixExpression(expression: KtPrefixExpression) {
    builder.block(ZERO) {
      builder.token(expression.operationReference.text)
      expression.baseExpression?.accept(this)
    }
  }

  override fun visitLabeledExpression(expression: KtLabeledExpression) {
    expression.labelQualifier?.accept(this)
    builder.space()
    expression.baseExpression?.accept(this)
  }

  internal enum class DeclarationKind {
    NONE,
    FIELD,
    PARAMETER
  }

  /**
   *  Declare one variable or variable-like thing.
   *
   *  Examples:
   *  - `var a: Int = 5`
   *  - `a: Int`
   *  - `private val b:
   */
  private fun declareOne(
      kind: DeclarationKind,
      modifiers: KtModifierList? = null,
      valOrVarKeyword: String? = null,
      name: String? = "",
      type: KtTypeReference? = null,
      op: String = "",
      equals: String = "=",
      initializer: PsiElement? = null,
      delegate: KtPropertyDelegate? = null,
      trailing: String? = null
  ): Int {

    val typeBreak = genSym()
    val verticalAnnotationBreak = genSym()

    val isField = kind == DeclarationKind.FIELD

    if (isField) {
      builder.blankLineWanted(OpsBuilder.BlankLineWanted.conditional(verticalAnnotationBreak))
    }

    builder.block(ZERO) {
      builder.block(plusFour) {
        builder.block(ZERO) {
          builder.block(ZERO) {
            modifiers?.accept(this)
            if (valOrVarKeyword != null) {
              builder.token(valOrVarKeyword)
              builder.breakOp(Doc.FillMode.INDEPENDENT, " ", ZERO, Optional.of(typeBreak))
            }
          }

          // conditionally ident the name and initializer +4 if the type spans
          // multiple lines
          if (name != null) {
            builder.open(Indent.If.make(typeBreak, plusFour, ZERO))
            builder.token(name)
            builder.op(op)
          }
        }
      }

      if (type != null) {
        if (name == null) {
          type.accept(this)
        } else {
          builder.token(":")
          builder.block(Indent.If.make(typeBreak, plusFour, ZERO)) {
            builder.breakToFill(" ")
            type.accept(this)
          }
        }
      }
      if (delegate != null) {
        builder.space()
        builder.token("by")
        builder.space()
        delegate.accept(this)
      }
      if (initializer != null) {
        builder.space()
        builder.token(equals)
        builder.block(Indent.If.make(typeBreak, plusFour, ZERO)) {
          builder.breakToFill(" ")
          initializer.accept(this)
        }
      }
      if (trailing != null) {
        val nextToken = builder.peekToken()
        if (nextToken.isPresent && nextToken.get() == trailing) {
          builder.guessToken(trailing)
        }
      }

      // end of conditional name and initializer block
      if (name != null) {
        builder.close()
      }
    }

    if (isField) {
      builder.blankLineWanted(OpsBuilder.BlankLineWanted.conditional(verticalAnnotationBreak))
    }

    return 0
  }

  override fun visitClassOrObject(classOrObject: KtClassOrObject) {
    classOrObject.modifierList?.accept(this)
    builder.block(ZERO) {
      builder.token(classOrObject.getDeclarationKeyword()?.text ?: fail())
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
        builder.space()
        superTypes.accept(this)
      }
    }
    builder.space()
    val body = classOrObject.body
    if (body != null) {
      visitBlockyBody(body, true)
    }
    builder.forcedBreak()
  }

  override fun visitPrimaryConstructor(constructor: KtPrimaryConstructor) {
    if (constructor.hasConstructorKeyword()) {
      builder.space()
      constructor.modifierList?.accept(this)
      builder.token("constructor")
    }
    builder.token("(")

    if (constructor.valueParameters.isNotEmpty()) {
      // Break before args.
      builder.breakToFill("")
      visitFormals(constructor.valueParameters)
    }
    builder.token(")")
  }

  override fun visitClassInitializer(initializer: KtClassInitializer) {
    builder.token("init")
    builder.space()
    initializer.body?.accept(this)
  }

  override fun visitConstantExpression(expression: KtConstantExpression) {
    builder.token(expression.text)
  }

  /** Example `(1 + 1)` */
  override fun visitParenthesizedExpression(expression: KtParenthesizedExpression) {
    builder.token("(")
    expression.expression?.accept(this)
    builder.token(")")
  }

  override fun visitPackageDirective(directive: KtPackageDirective) {
    if (directive.packageKeyword == null) {
      return
    }
    builder.token("package")
    builder.space()
    var first = true;
    for (packageName in directive.packageNames) {
      if (first) {
        first = false
      } else {
        builder.token(".")
      }
      builder.token(packageName.getReferencedName())
    }

    builder.ignoreOptionalToken(";")
    builder.forcedBreak()
    builder.blankLineWanted(OpsBuilder.BlankLineWanted.YES)
  }

  override fun visitImportDirective(directive: KtImportDirective) {
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

    builder.ignoreOptionalToken(";")
    // Force a newline afterwards.
    builder.forcedBreak()
  }

  override fun visitModifierList(list: KtModifierList) {
    builder.block(ZERO) {
      for (child in list.node.children()) {
        when (child.elementType) {
          is KtAnnotationEntryElementType -> visitAnnotationEntry(child.psi as KtAnnotationEntry)
          is KtModifierKeywordToken -> {
            builder.token(child.text)
            builder.space()
          }
        }
      }
    }
  }

  override fun visitAnnotationEntry(annotationEntry: KtAnnotationEntry) {
    builder.token("@")
    val isFileAnnoation = annotationEntry.parent is KtFileAnnotationList
    if (isFileAnnoation) {
      builder.token("file")
      builder.token(":")
    }
    builder.block(ZERO) {
      visitCallElement(
          annotationEntry.calleeExpression,
          annotationEntry.typeArgumentList,
          annotationEntry.valueArgumentList,
          listOf(),
          false)
      if (isFileAnnoation) {
        builder.forcedBreak()
      } else {
        builder.space()
      }
    }
  }

  override fun visitSuperTypeList(list: KtSuperTypeList) {
    builder.block(ZERO) {
      forEachCommaSeparated(list.entries) {
        it.accept(this)
      }
    }
  }

  override fun visitSuperTypeCallEntry(call: KtSuperTypeCallEntry) {
    visitCallElement(call.calleeExpression, null, call.valueArgumentList, call.lambdaArguments, true)
  }

  override fun visitWhenExpression(expression: KtWhenExpression) {
    builder.block(ZERO) {
      builder.token("when")
      expression.subjectExpression?.let { subjectExp ->
        builder.space()
        builder.token("(")
        subjectExp.accept(this)
        builder.token(")")
      }
      builder.space()
      builder.token("{", Doc.Token.RealOrImaginary.REAL, plusFour, Optional.of(plusFour))

      expression.entries.forEach { whenEntry ->
        builder.block(plusFour) {
          builder.forcedBreak()
          if (whenEntry.isElse) {
            builder.token("else")
          } else {
            forEachCommaSeparated(whenEntry.conditions.asIterable()) {
              it.accept(this)
            }
          }
          val whenExpression = whenEntry.expression
          builder.space()
          builder.token("->")
          builder.space()
          whenExpression?.accept(this)
        }
        builder.forcedBreak()
      }
      builder.token("}")
    }
  }
  
  override fun visitBlockExpression(expression: KtBlockExpression) {
    visitBlockyBody(expression, true)
  }

  override fun visitWhenConditionWithExpression(condition: KtWhenConditionWithExpression) {
    condition.expression?.accept(this)
  }

  override fun visitWhenConditionIsPattern(condition: KtWhenConditionIsPattern) {
    builder.token(if (condition.isNegated) "!is" else "is")
    builder.space()
    condition.typeReference?.text?.let { builder.token(it) }
  }

  /** Example `in 1..2` as part of a when expression */
  override fun visitWhenConditionInRange(condition: KtWhenConditionInRange) {
    // TODO: replace with 'condition.isNegated' once https://youtrack.jetbrains.com/issue/KT-34395 is fixed.
    val isNegated = condition.firstChild?.node?.findChildByType(KtTokens.NOT_IN) != null
    builder.token(if (isNegated) "!in" else "in")
    builder.space()
    condition.rangeExpression?.accept(this)
  }

  override fun visitIfExpression(expression: KtIfExpression) {
    builder.token("if")
    builder.space()
    builder.token("(")
    expression.condition?.accept(this)
    builder.token(")")
    builder.space()
    expression.then?.accept(this)
    if (expression.elseKeyword != null) {
      builder.space()
      builder.token("else")
      builder.space()
      expression.`else`?.accept(this)
    }
  }

  /** Example `a[3]` or `b["a", 5]` */
  override fun visitArrayAccessExpression(expression: KtArrayAccessExpression) {
    expression.arrayExpression?.accept(this)
    builder.token("[")
    forEachCommaSeparated(expression.indexExpressions) {
      it.accept(this)
    }
    builder.token("]")
  }


  /** Example `val (a, b: Int) = Pair(1, 2)` */
  override fun visitDestructuringDeclaration(destructuringDeclaration: KtDestructuringDeclaration) {
    val valOrVarKeyword = destructuringDeclaration.valOrVarKeyword
    if (valOrVarKeyword != null) {
      builder.token(valOrVarKeyword.text)
      builder.space()
    }
    builder.token("(")
    forEachCommaSeparated(destructuringDeclaration.entries) {
      it.accept(this)
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
  override fun visitDestructuringDeclarationEntry(multiDeclarationEntry: KtDestructuringDeclarationEntry) {
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
    val quoteToken = if (expression.isSingleQuoted()) "\"" else "\"\"\""
    builder.token(quoteToken)
    expression.entries.forEach { it.accept(this) }
    builder.token(quoteToken)
  }

  /** Example `hello` (Inside the string literal "hello") */
  override fun visitLiteralStringTemplateEntry(entry: KtLiteralStringTemplateEntry) {
    builder.token(entry.text)
  }

  /** Example `$world` (inside a String) */
  override fun visitSimpleNameStringTemplateEntry(entry: KtSimpleNameStringTemplateEntry) {
    builder.token("$")
    builder.token(entry.text.substring(1))
  }

  /** Example `${1 + 2}` (inside a String) */
  override fun visitStringTemplateEntryWithExpression(entry: KtStringTemplateEntryWithExpression) {
    builder.token("$" + "{")
    builder.block(ZERO) {
      entry.expression?.accept(this)
    }
    builder.token("}")
  }

  override fun visitEscapeStringTemplateEntry(entry: KtEscapeStringTemplateEntry) {
    builder.token(entry.text)
  }

  /** Example `<T, S>` */
  override fun visitTypeParameterList(list: KtTypeParameterList) {
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

  override fun visitTypeParameter(parameter: KtTypeParameter) {
    if (parameter.hasModifier(KtTokens.REIFIED_KEYWORD)) {
      builder.token("reified")
      builder.space()
    }
    when (parameter.variance) {
      Variance.INVARIANT -> {
      }
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

  /** Example `for (i in items) { ... }` */
  override fun visitForExpression(expression: KtForExpression) {
    builder.token("for")
    builder.space()
    builder.token("(")
    expression.loopParameter?.accept(this)
    builder.space()
    builder.token("in")
    builder.space()
    expression.loopRange?.accept(this)
    builder.token(")")
    builder.space()
    expression.body?.accept(this)
  }

  /** Example `while (a < b) { ... }` */
  override fun visitWhileExpression(expression: KtWhileExpression) {
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
    builder.token("break")
    expression.labelQualifier?.accept(this)
  }

  /** Example `continue` or `continue@foo` in a loop */
  override fun visitContinueExpression(expression: KtContinueExpression) {
    builder.token("continue")
    expression.labelQualifier?.accept(this)
  }

  /** Example `f: String`, or `private val n: Int` or `(a: Int, b: String)` (in for-loops) */
  override fun visitParameter(parameter: KtParameter) {
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

  override fun visitCallableReferenceExpression(expression: KtCallableReferenceExpression) {
    expression.receiverExpression?.accept(this)
    builder.token("::")
    expression.callableReference.accept(this)
  }

  override fun visitClassLiteralExpression(expression: KtClassLiteralExpression) {
    expression.receiverExpression?.accept(this)
    builder.token("::")
    builder.token("class")
  }

  override fun visitFunctionType(type: KtFunctionType) {
    val receiver = type.receiver
    if (receiver != null) {
      receiver.accept(this)
      builder.token(".")
    }
    builder.token("(")
    forEachCommaSeparated(type.parameters) {
      it.accept(this)
    }
    builder.token(")")
    builder.space()
    builder.token("->")
    builder.space()
    type.returnTypeReference?.accept(this)
  }

  /** Example `a is Int` or `b !is Int` */
  override fun visitIsExpression(expression: KtIsExpression) {
    expression.leftHandSide.accept(this)
    builder.space()
    expression.operationReference.accept(this)
    builder.space()
    expression.typeReference?.accept(this)
  }

  /** Example `a as Int` or `a as? Int` */
  override fun visitBinaryWithTypeRHSExpression(expression: KtBinaryExpressionWithTypeRHS) {
    expression.left.accept(this)
    builder.space()
    expression.operationReference.accept(this)
    builder.space()
    expression.right?.accept(this)
  }


  override fun visitCollectionLiteralExpression(expression: KtCollectionLiteralExpression, data: Void?): Void? {
    builder.token("[")
    forEachCommaSeparated(expression.getInnerExpressions()) {
      it.accept(this)
    }
    builder.token("]")
    return null
  }

  override fun visitTryExpression(expression: KtTryExpression) {
    builder.token("try")
    builder.space()
    expression.tryBlock.accept(this)
    for (catchClause in expression.catchClauses) {
      catchClause.accept(this)
    }
    expression.finallyBlock?.accept(this)
  }

  override fun visitCatchSection(catchClause: KtCatchClause) {
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
    builder.space()
    builder.token("finally")
    builder.space()
    finallySection.finalExpression.accept(this)
  }

  override fun visitThrowExpression(expression: KtThrowExpression) {
    builder.token("throw")
    builder.space()
    expression.thrownExpression?.accept(this)
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
   * This is a helper method to make it easier to keep track of [OpsBuilder.open] and [OpsBuilder.close] calls
   *
   * @param plusIndent the block level to pass to the block
   * @param block a code block to be run in this block level
   */
  inline private fun OpsBuilder.block(plusIndent: Indent, block: () -> Unit) {
    open(plusIndent)
    block()
    close()
  }

  /**
   * Throws a formatting error
   *
   * This is used as `expr ?: fail()` to avoid using the !! operator and provide better error messages.
   */
  private fun fail(message: String = "Unexpected"): Nothing {
    throw FormattingError(FormatterDiagnostic.create(message))
  }

  /**
   * RecursiveQualifiedStructure handles selector-like constructs: foo.bar.baz.etc.
   *
   * <p>These come up in dotted expressions: "this.builder.token()" as well as nested types: "Indent.Const".
   *
   * <p>It maintains an internal depth state which is used to create blocks.
   */
  inner class RecursiveQualifiedStructure {
    var depth = 0

    fun visit(beforeSeparator: KtElement?, separator: String, afterSeparator: List<KtElement>) {
      depth++

      if (depth == 1) {
        builder.open(ZERO)
      }

      if (beforeSeparator != null) {
        beforeSeparator.accept(this@KotlinInputAstVisitor)
        builder.breakOp(Doc.FillMode.UNIFIED, "", plusFour)
        builder.token(separator)
      }

      afterSeparator.forEach { it.accept(this@KotlinInputAstVisitor) }

      if (depth == 1) {
        builder.close()
      }

      depth--
    }
  }
}
