package com.facebook.ktfmt.format

import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassBody
import org.jetbrains.kotlin.psi.KtEnumEntry
import org.jetbrains.kotlin.psi.psiUtil.getPrevSiblingIgnoringWhitespaceAndComments

/**
 * PSI-like model of a list of enum entries.
 *
 * See https://youtrack.jetbrains.com/issue/KT-65157
 */
class EnumEntryList
private constructor(
  val enumEntries: List<KtEnumEntry>,
  val trailingComma: PsiElement?,
  val terminatingSemicolon: PsiElement?,
) {
  companion object {
    fun extractParentList(enumEntry: KtEnumEntry): EnumEntryList {
      return extractChildList(enumEntry.parent as KtClassBody)!!
    }

    fun extractChildList(classBody: KtClassBody): EnumEntryList? {
      val clazz = classBody.parent
      if (clazz !is KtClass || !clazz.isEnum()) return null

      val enumEntries = classBody.children.filterIsInstance<KtEnumEntry>()

      if (enumEntries.isEmpty()) {
        var semicolon = classBody.firstChild
        while (semicolon != null) {
          if (semicolon.text == ";") break
          semicolon = semicolon.nextSibling
        }

        return EnumEntryList(
          enumEntries = enumEntries,
          trailingComma = null,
          terminatingSemicolon = semicolon,
        )
      }

      var semicolon: PsiElement? = null
      var comma: PsiElement? = null
      val lastToken =
        enumEntries
          .last()
          .lastChild
          .getPrevSiblingIgnoringWhitespaceAndComments(withItself = true)!!
      when (lastToken.text) {
        "," -> {
          comma = lastToken
        }
        ";" -> {
          semicolon = lastToken
          val prevSibling = semicolon.getPrevSiblingIgnoringWhitespaceAndComments()
          if (prevSibling?.text == ",") {
            comma = prevSibling
          }
        }
      }

      return EnumEntryList(
        enumEntries = enumEntries,
        trailingComma = comma,
        terminatingSemicolon = semicolon,
      )
    }
  }
}
