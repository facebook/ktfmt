/*
 * Copyright 2015 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

// This was copied from https://github.com/google/google-java-format and converted to Kotlin,
// because the original is package-private.

package com.facebook.ktfmt.format

import com.google.common.base.Verify
import java.util.Optional

/** Heuristics for classifying qualified names as types. */
object TypeNameClassifier {

  /** A state machine for classifying qualified names. */
  private enum class TyParseState(val isSingleUnit: Boolean) {

    /** The start state. */
    START(false) {
      override fun next(n: JavaCaseFormat): TyParseState {
        return when (n) {
          JavaCaseFormat.UPPERCASE ->
              // if we see an UpperCamel later, assume this was a class
              // e.g. com.google.FOO.Bar
              AMBIGUOUS
          JavaCaseFormat.LOWER_CAMEL -> REJECT
          JavaCaseFormat.LOWERCASE ->
              // could be a package
              START
          JavaCaseFormat.UPPER_CAMEL -> TYPE
        }
      }
    },

    /** The current prefix is a type. */
    TYPE(true) {
      override fun next(n: JavaCaseFormat): TyParseState {
        return when (n) {
          JavaCaseFormat.UPPERCASE,
          JavaCaseFormat.LOWER_CAMEL,
          JavaCaseFormat.LOWERCASE -> FIRST_STATIC_MEMBER
          JavaCaseFormat.UPPER_CAMEL -> TYPE
        }
      }
    },

    /** The current prefix is a type, followed by a single static member access. */
    FIRST_STATIC_MEMBER(true) {
      override fun next(n: JavaCaseFormat): TyParseState {
        return REJECT
      }
    },

    /** Anything not represented by one of the other states. */
    REJECT(false) {
      override fun next(n: JavaCaseFormat): TyParseState {
        return REJECT
      }
    },

    /** An ambiguous type prefix. */
    AMBIGUOUS(false) {
      override fun next(n: JavaCaseFormat): TyParseState {
        return when (n) {
          JavaCaseFormat.UPPERCASE -> AMBIGUOUS
          JavaCaseFormat.LOWER_CAMEL,
          JavaCaseFormat.LOWERCASE -> REJECT
          JavaCaseFormat.UPPER_CAMEL -> TYPE
        }
      }
    };

    /** Transition function. */
    abstract fun next(n: JavaCaseFormat): TyParseState
  }

  /**
   * Returns the end index (inclusive) of the longest prefix that matches the naming conventions of
   * a type or static field access, or -1 if no such prefix was found.
   *
   * Examples:
   * * ClassName
   * * ClassName.staticMemberName
   * * com.google.ClassName.InnerClass.staticMemberName
   */
  internal fun typePrefixLength(nameParts: List<String>): Optional<Int> {
    var state = TyParseState.START
    var typeLength = Optional.empty<Int>()
    for (i in nameParts.indices) {
      state = state.next(JavaCaseFormat.from(nameParts[i]))
      if (state === TyParseState.REJECT) {
        break
      }
      if (state.isSingleUnit) {
        typeLength = Optional.of(i)
      }
    }
    return typeLength
  }

  /** Case formats used in Java identifiers. */
  enum class JavaCaseFormat {
    UPPERCASE,
    LOWERCASE,
    UPPER_CAMEL,
    LOWER_CAMEL;

    companion object {

      /** Classifies an identifier's case format. */
      internal fun from(name: String): JavaCaseFormat {
        Verify.verify(name.isNotEmpty())
        var firstUppercase = false
        var hasUppercase = false
        var hasLowercase = false
        var first = true
        for (char in name) {
          if (!Character.isAlphabetic(char.code)) {
            continue
          }
          if (first) {
            firstUppercase = Character.isUpperCase(char)
            first = false
          }
          hasUppercase = hasUppercase or Character.isUpperCase(char)
          hasLowercase = hasLowercase or Character.isLowerCase(char)
        }
        return if (firstUppercase) {
          if (hasLowercase) UPPER_CAMEL else UPPERCASE
        } else {
          if (hasUppercase) LOWER_CAMEL else LOWERCASE
        }
      }
    }
  }
}
