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

package com.facebook.ktfmt.intellij;

import static com.facebook.ktfmt.format.Formatter.format;

import com.facebook.ktfmt.format.ParseError;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.googlejavaformat.java.FormatterException;
import com.intellij.openapi.util.TextRange;
import java.util.Map;

final class FormatterUtil {

  private FormatterUtil() {}

  /**
   * Formats 'code' using ktfmt.
   *
   * @return formatted code
   */
  static Map<TextRange, String> getReplacements(UiFormatterStyle uiFormatterStyle, String code) {
    try {
      return ImmutableMap.of(TextRange.allOf(code), formatCode(uiFormatterStyle, code));
    } catch (FormatterException | ParseError e) {
      return ImmutableMap.of();
    }
  }

  @VisibleForTesting
  static String formatCode(UiFormatterStyle uiFormatterStyle, String code)
      throws FormatterException {

    return format(uiFormatterStyle.getFormattingOptions(), code);
  }
}
