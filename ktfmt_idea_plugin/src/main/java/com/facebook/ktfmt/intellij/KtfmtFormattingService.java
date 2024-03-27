/*
 * Copyright 2023 Google Inc. All Rights Reserved.
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

import com.google.googlejavaformat.java.FormatterException;
import com.intellij.formatting.service.AsyncDocumentFormattingService;
import com.intellij.formatting.service.AsyncFormattingRequest;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import java.util.EnumSet;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.idea.KotlinFileType;

/** Uses {@code ktfmt} to reformat code. */
public class KtfmtFormattingService extends AsyncDocumentFormattingService {

  @Override
  protected FormattingTask createFormattingTask(AsyncFormattingRequest request) {
    Project project = request.getContext().getProject();

    UiFormatterStyle style = KtfmtSettings.getInstance(project).getUiFormatterStyle();
    return new KtfmtFormattingTask(request, style);
  }

  @Override
  protected @NotNull String getNotificationGroupId() {
    return Notifications.PARSING_ERROR_NOTIFICATION_GROUP;
  }

  @Override
  protected @NotNull String getName() {
    return "ktfmt";
  }

  @Override
  public @NotNull Set<Feature> getFeatures() {
    return EnumSet.noneOf(Feature.class);
  }

  @Override
  public boolean canFormat(@NotNull PsiFile file) {
    return KotlinFileType.INSTANCE.getName().equals(file.getFileType().getName())
        && KtfmtSettings.getInstance(file.getProject()).isEnabled();
  }

  private static final class KtfmtFormattingTask implements FormattingTask {
    private final AsyncFormattingRequest request;
    private final UiFormatterStyle style;

    private KtfmtFormattingTask(AsyncFormattingRequest request, UiFormatterStyle style) {
      this.request = request;
      this.style = style;
    }

    @Override
    public void run() {
      try {
        String formattedText = format(style.getFormattingOptions(), request.getDocumentText());
        request.onTextReady(formattedText);
      } catch (FormatterException e) {
        request.onError(
            Notifications.PARSING_ERROR_TITLE,
            Notifications.parsingErrorMessage(request.getContext().getContainingFile().getName()));
      }
    }

    @Override
    public boolean isRunUnderProgress() {
      return true;
    }

    @Override
    public boolean cancel() {
      return false;
    }
  }
}
