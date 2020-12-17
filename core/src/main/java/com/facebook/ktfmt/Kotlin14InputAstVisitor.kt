package com.facebook.ktfmt

import com.google.googlejavaformat.OpsBuilder
import org.jetbrains.kotlin.psi.KtCollectionLiteralExpression

class Kotlin14InputAstVisitor(options: FormattingOptions, builder: OpsBuilder) :
    KotlinInputAstVisitorBase(options, builder) {
  override fun visitCollectionLiteralExpression(expression: KtCollectionLiteralExpression) {
    super.actualVisitCollectionLiteralExpression(expression)
  }
}
