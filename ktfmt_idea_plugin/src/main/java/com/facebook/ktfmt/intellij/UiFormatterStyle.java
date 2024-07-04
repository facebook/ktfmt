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

import com.facebook.ktfmt.format.FormattingOptions;
import com.intellij.lang.Language;
import com.intellij.psi.codeStyle.*;

import static com.facebook.ktfmt.format.Formatter.*;

/**
 * Configuration options for the formatting style.
 */
enum UiFormatterStyle {
    META("Meta (default)", META_FORMAT),
    GOOGLE("Google (internal)", GOOGLE_FORMAT),
    KOTLINLANG("Kotlinlang", KOTLINLANG_FORMAT);

    private final String description;
    private final FormattingOptions formattingOptions;

    UiFormatterStyle(String description, FormattingOptions formattingOptions) {
        this.description = description;
        this.formattingOptions = formattingOptions;
    }

    FormattingOptions getFormattingOptions(Integer maxWidthFromUser) {
        CodeStyleScheme currentScheme = CodeStyleSchemes.getInstance().getCurrentScheme();
        Language kotlinLang = Language.findLanguageByID("kotlin");

        int maxWidth = -1;
        if (maxWidthFromUser == null) {
            CodeStyleSettings codeStyleSettings = currentScheme.getCodeStyleSettings();
            while (codeStyleSettings != null && maxWidth == -1) {
                maxWidth = codeStyleSettings.getRightMargin(kotlinLang);
                codeStyleSettings = codeStyleSettings.getParentSettings();
            }
            if (maxWidth == -1) {
                maxWidth = 120;
            }
        } else {
            maxWidth = maxWidthFromUser;
        }

        return new FormattingOptions(
                maxWidth,
                formattingOptions.getBlockIndent(),
                formattingOptions.getContinuationIndent(),
                formattingOptions.getManageTrailingCommas(),
                formattingOptions.getRemoveUnusedImports(),
                formattingOptions.getDebuggingPrintOpsAfterFormatting()
        );
    }

    @Override
    public String toString() {
        return description;
    }
}
