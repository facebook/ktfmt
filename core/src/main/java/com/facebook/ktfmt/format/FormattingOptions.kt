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

data class FormattingOptions(
    val style: Style = Style.FACEBOOK,

    /** ktfmt breaks lines longer than maxWidth. */
    val maxWidth: Int = DEFAULT_MAX_WIDTH,

    /**
     * blockIndent is the size of the indent used when a new block is opened, in spaces.
     *
     * For example,
     * ```
     * fun f() {
     *   //
     * }
     * ```
     */
    val blockIndent: Int = 2,

    /**
     * continuationIndent is the size of the indent used when a line is broken because it's too
     * long, in spaces.
     *
     * For example,
     * ```
     * val foo = bar(
     *     1)
     * ```
     */
    val continuationIndent: Int = 4,

    /** Whether ktfmt should remove imports that are not used. */
    val removeUnusedImports: Boolean = true,
) {

  companion object {
    const val DEFAULT_MAX_WIDTH: Int = 100
  }

  enum class Style {
    FACEBOOK,
    DROPBOX,
    GOOGLE
  }
}
