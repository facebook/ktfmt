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

package com.facebook.ktfmt.intellij

import com.facebook.ktfmt.format.Formatter.format
import com.facebook.ktfmt.format.FormattingOptions
import com.google.googlejavaformat.java.FormatterException
import com.intellij.formatting.service.AsyncDocumentFormattingService
import com.intellij.formatting.service.AsyncFormattingRequest
import com.intellij.formatting.service.FormattingService.Feature
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.idea.KotlinFileType

private const val PARSING_ERROR_NOTIFICATION_GROUP: String = "ktfmt parsing error"
private const val PARSING_ERROR_TITLE: String = PARSING_ERROR_NOTIFICATION_GROUP

/** Uses `ktfmt` to reformat code. */
class KtfmtFormattingService : AsyncDocumentFormattingService() {
  override fun createFormattingTask(request: AsyncFormattingRequest): FormattingTask {
    val project = request.context.project
    val settings = KtfmtSettings.getInstance(project)
    val style = settings.uiFormatterStyle
    val formattingOptions =
        if (style == UiFormatterStyle.Custom) {
          settings.customFormattingOptions
        } else {
          UiFormatterStyle.getStandardFormattingOptions(style)
        }
    return KtfmtFormattingTask(request, formattingOptions)
  }

  override fun getNotificationGroupId(): String = PARSING_ERROR_NOTIFICATION_GROUP

  override fun getName(): String = "ktfmt"

  override fun getFeatures(): Set<Feature> = emptySet()

  override fun canFormat(file: PsiFile): Boolean =
      KotlinFileType.INSTANCE.name == file.fileType.name &&
          KtfmtSettings.getInstance(file.project).isEnabled

  private class KtfmtFormattingTask(
      private val request: AsyncFormattingRequest,
      private val formattingOptions: FormattingOptions,
  ) : FormattingTask {
    override fun run() {
      try {
        val formattedText = format(formattingOptions, request.documentText)
        request.onTextReady(formattedText)
      } catch (e: FormatterException) {
        request.onError(
            PARSING_ERROR_TITLE,
            "ktfmt failed. Does ${request.context.containingFile.name} have syntax errors?",
        )
      }
    }

    override fun isRunUnderProgress(): Boolean = true

    override fun cancel(): Boolean = false
  }
}
