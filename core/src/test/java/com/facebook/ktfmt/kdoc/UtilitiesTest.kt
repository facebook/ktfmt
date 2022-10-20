/*
 * Copyright (c) Tor Norbye.
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

package com.facebook.ktfmt.kdoc

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class UtilitiesTest {
  @Test
  fun testFindSamePosition() {
    fun check(newWithCaret: String, oldWithCaret: String) {
      val oldCaretIndex = oldWithCaret.indexOf('|')
      val newCaretIndex = newWithCaret.indexOf('|')
      assertThat(oldCaretIndex != -1).isTrue()
      assertThat(newCaretIndex != -1).isTrue()
      val old = oldWithCaret.substring(0, oldCaretIndex) + oldWithCaret.substring(oldCaretIndex + 1)
      val new = newWithCaret.substring(0, newCaretIndex) + newWithCaret.substring(newCaretIndex + 1)
      val newPos = findSamePosition(old, oldCaretIndex, new)

      val actual = new.substring(0, newPos) + "|" + new.substring(newPos)
      assertThat(actual).isEqualTo(newWithCaret)
    }

    // Prefix match
    check("|/** Test\n Different Middle End */", "|/** Test2 End */")
    check("/|** Test\n Different Middle End */", "/|** Test2 End */")
    check("/*|* Test\n Different Middle End */", "/*|* Test2 End */")
    check("/**| Test\n Different Middle End */", "/**| Test2 End */")
    check("/** |Test\n Different Middle End */", "/** |Test2 End */")
    check("/** T|est\n Different Middle End */", "/** T|est2 End */")
    check("/** Te|st\n Different Middle End */", "/** Te|st2 End */")
    check("/** Tes|t\n Different Middle End */", "/** Tes|t2 End */")
    check("/** Test|\n Different Middle End */", "/** Test|2 End */")
    // End match
    check("/** Test\n Different Middle| End */", "/** Test2| End */")
    check("/** Test\n Different Middle E|nd */", "/** Test2 E|nd */")
    check("/** Test\n Different Middle En|d */", "/** Test2 En|d */")
    check("/** Test\n Different Middle End| */", "/** Test2 End| */")
    check("/** Test\n Different Middle End |*/", "/** Test2 End |*/")
    check("/** Test\n Different Middle End *|/", "/** Test2 End *|/")
    check("/** Test\n Different Middle End */|", "/** Test2 End */|")

    check("|/**\nTest End\n*/", "|/** Test End */")
    check("/|**\nTest End\n*/", "/|** Test End */")
    check("/*|*\nTest End\n*/", "/*|* Test End */")
    check("/**|\nTest End\n*/", "/**| Test End */")
    check("/**\n|Test End\n*/", "/** |Test End */")
    check("/**\nT|est End\n*/", "/** T|est End */")
    check("/**\nTe|st End\n*/", "/** Te|st End */")
    check("/**\nTes|t End\n*/", "/** Tes|t End */")
    check("/**\nTest| End\n*/", "/** Test| End */")
    check("/**\nTest |End\n*/", "/** Test |End */")
    check("/**\nTest E|nd\n*/", "/** Test E|nd */")
    check("/**\nTest En|d\n*/", "/** Test En|d */")
    check("/**\nTest End|\n*/", "/** Test End| */")
    check("/**\nTest End\n|*/", "/** Test End |*/")
    check("/**\nTest End\n*|/", "/** Test End *|/")
    check("/**\nTest End\n*/|", "/** Test End */|")

    check("|/** Test End */", "|/** Test2 End */")
    check("/|** Test End */", "/|** Test2 End */")
    check("/*|* Test End */", "/*|* Test2 End */")
    check("/**| Test End */", "/**| Test2 End */")
    check("/** |Test End */", "/** |Test2 End */")
    check("/** T|est End */", "/** T|est2 End */")
    check("/** Te|st End */", "/** Te|st2 End */")
    check("/** Tes|t End */", "/** Tes|t2 End */")
    check("/** Test| End */", "/** Test|2 End */")
    check("/** Test |End */", "/** Test2 |End */")
    check("/** Test E|nd */", "/** Test2 E|nd */")
    check("/** Test En|d */", "/** Test2 En|d */")
    check("/** Test End| */", "/** Test2 End| */")
    check("/** Test End |*/", "/** Test2 End |*/")
    check("/** Test End *|/", "/** Test2 End *|/")
    check("/** Test End */|", "/** Test2 End */|")
  }

  @Test
  fun testGetParamName() {
    assertThat("@param foo".getParamName()).isEqualTo("foo")
    assertThat("@param foo bar".getParamName()).isEqualTo("foo")
    assertThat("@param foo;".getParamName()).isEqualTo("foo")
    assertThat("  \t@param\t   foo  bar.".getParamName()).isEqualTo("foo")
    assertThat("@param[foo]".getParamName()).isEqualTo("foo")
    assertThat("@param  [foo]".getParamName()).isEqualTo("foo")
    assertThat("@param ".getParamName()).isEqualTo(null)
  }
}
