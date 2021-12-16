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

package com.facebook.ktfmt.intellij;

import static com.facebook.ktfmt.format.Formatter.DROPBOX_FORMAT;
import static com.facebook.ktfmt.format.Formatter.GOOGLE_FORMAT;
import static com.facebook.ktfmt.format.Formatter.KOTLINLANG_FORMAT;

import com.facebook.ktfmt.format.FormattingOptions;

/** Configuration options for the formatting style. */
enum UiFormatterStyle {
  DEFAULT("Default", new FormattingOptions()),
  DROPBOX("Dropbox", DROPBOX_FORMAT),
  GOOGLE("Google (internal)", GOOGLE_FORMAT),
  KOTLINLANG("Kotlinlang", KOTLINLANG_FORMAT);

  private final String description;
  private final FormattingOptions formattingOptions;

  UiFormatterStyle(String description, FormattingOptions formattingOptions) {
    this.description = description;
    this.formattingOptions = formattingOptions;
  }

  FormattingOptions getFormattingOptions() {
    return formattingOptions;
  }

  @Override
  public String toString() {
    return description;
  }
}
