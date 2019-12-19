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

import com.intellij.psi.PsiElement
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
