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

import java.util.Arrays;
import java.util.Objects;

/** Configuration options for the formatting style. */
enum UiFormatterStyle {
  DEFAULT("Default ktfmt style", false),
  DROPBOX("Dropbox style", true);

  private final String description;
  private final boolean isDropboxStyle;

  UiFormatterStyle(String description, boolean isDropboxStyle) {
    this.description = description;
    this.isDropboxStyle = isDropboxStyle;
  }

  @Override
  public String toString() {
    return description;
  }

  public boolean convert() {
    return isDropboxStyle;
  }

  static UiFormatterStyle convert(boolean isDropboxStyle) {
    return Arrays.stream(UiFormatterStyle.values())
        .filter(value -> Objects.equals(value.isDropboxStyle, isDropboxStyle))
        .findFirst()
        .get();
  }
}
