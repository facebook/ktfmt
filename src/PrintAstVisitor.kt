package com.google.googlejavaformat.kotlin

import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid

class PrintAstVisitor : KtTreeVisitorVoid() {
  override fun visitElement(element: PsiElement) {
    print("  ".repeat(depth(element)))
    println("${element.javaClass.simpleName} ${element.text}")
    super.visitElement(element)
  }

  fun depth(element: PsiElement): Int {
    var result = 0
    var node: PsiElement? = element
    do {
      result++
      node = node?.parent
    } while (node != null)
    return result
  }
}
