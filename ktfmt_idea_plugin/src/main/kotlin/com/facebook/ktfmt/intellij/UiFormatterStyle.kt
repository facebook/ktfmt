/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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
package com.facebook.ktfmt.intellij

import com.facebook.ktfmt.format.Formatter.GOOGLE_FORMAT
import com.facebook.ktfmt.format.Formatter.KOTLINLANG_FORMAT
import com.facebook.ktfmt.format.Formatter.META_FORMAT
import com.facebook.ktfmt.format.FormattingOptions

/** Configuration options for the formatting style. */
internal enum class UiFormatterStyle(
    private val description: String,
    val formattingOptions: FormattingOptions,
) {
    Meta("Meta (default)", META_FORMAT),
    Google("Google (internal)", GOOGLE_FORMAT),
    KotlinLang("Kotlinlang", KOTLINLANG_FORMAT);

    override fun toString(): String = description
}
