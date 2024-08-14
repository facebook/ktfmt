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

import com.facebook.ktfmt.format.Formatter
import com.facebook.ktfmt.format.FormattingOptions
import com.facebook.ktfmt.intellij.KtfmtSettings.EnabledState.Disabled
import com.facebook.ktfmt.intellij.KtfmtSettings.EnabledState.Enabled
import com.facebook.ktfmt.intellij.KtfmtSettings.EnabledState.Unknown
import com.facebook.ktfmt.intellij.UiFormatterStyle.Meta
import com.intellij.openapi.components.BaseState
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.Service.Level.PROJECT
import com.intellij.openapi.components.SimplePersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project

@Service(PROJECT)
@State(name = "KtfmtSettings", storages = [Storage("ktfmt.xml")])
internal class KtfmtSettings(private val project: Project) :
    SimplePersistentStateComponent<KtfmtSettings.State>(State()) {
  val isUninitialized: Boolean
    get() = state.enableKtfmt == Unknown

  var uiFormatterStyle: UiFormatterStyle
    get() = state.uiFormatterStyle
    set(uiFormatterStyle) {
      state.uiFormatterStyle = uiFormatterStyle
    }

  var customFormattingOptions: FormattingOptions
    get() =
        FormattingOptions(
            state.customMaxLineLength,
            state.customBlockIndent,
            state.customContinuationIndent,
            state.customManageTrailingCommas,
            state.customRemoveUnusedImports,
        )
    set(customFormattingOptions) {
      state.applyCustomFormattingOptions(customFormattingOptions)
    }

  var customMaxLineLength: Int
    get() = state.customMaxLineLength
    set(maxLineLength) {
      state.customMaxLineLength = maxLineLength.coerceAtLeast(1)
    }

  var customBlockIndent: Int
    get() = state.customBlockIndent
    set(blockIndent) {
      state.customBlockIndent = blockIndent.coerceAtLeast(1)
    }

  var customContinuationIndent: Int
    get() = state.customContinuationIndent
    set(continuationIndent) {
      state.customContinuationIndent = continuationIndent.coerceAtLeast(1)
    }

  var customManageTrailingCommas: Boolean
    get() = state.customManageTrailingCommas
    set(manageTrailingCommas) {
      state.customManageTrailingCommas = manageTrailingCommas
    }

  var customRemoveUnusedImports: Boolean
    get() = state.customRemoveUnusedImports
    set(removeUnusedImports) {
      state.customRemoveUnusedImports = removeUnusedImports
    }

  var isEnabled: Boolean
    get() = state.enableKtfmt == Enabled
    set(enabled) {
      setEnabled(if (enabled) Enabled else Disabled)
    }

  fun setEnabled(enabled: EnabledState) {
    state.enableKtfmt = enabled
  }

  override fun loadState(state: State) {
    val migrated = loadOrMigrateIfNeeded(state)
    super.loadState(migrated)
  }

  private fun loadOrMigrateIfNeeded(state: State): State {
    val migrationSettings = project.service<KtfmtSettingsMigration>()

    return when (val stateVersion = migrationSettings.stateVersion) {
      KtfmtSettingsMigration.CURRENT_VERSION -> state
      1 -> migrationSettings.migrateFromV1ToCurrent(state)
      else -> {
        thisLogger().error("Cannot migrate settings from $stateVersion. Using defaults.")
        State()
      }
    }
  }

  internal enum class EnabledState {
    Unknown,
    Enabled,
    Disabled,
  }

  internal class State : BaseState() {
    @Deprecated("Deprecated in V2. Use enableKtfmt instead.") var enabled by string()

    var enableKtfmt by enum<EnabledState>(Unknown)
    var uiFormatterStyle by enum<UiFormatterStyle>(Meta)

    var customMaxLineLength by property(Formatter.META_FORMAT.maxWidth)
    var customBlockIndent by property(Formatter.META_FORMAT.blockIndent)
    var customContinuationIndent by property(Formatter.META_FORMAT.continuationIndent)
    var customManageTrailingCommas by property(Formatter.META_FORMAT.manageTrailingCommas)
    var customRemoveUnusedImports by property(Formatter.META_FORMAT.removeUnusedImports)

    fun applyCustomFormattingOptions(formattingOptions: FormattingOptions) {
      customMaxLineLength = formattingOptions.maxWidth
      customBlockIndent = formattingOptions.blockIndent
      customContinuationIndent = formattingOptions.continuationIndent
      customManageTrailingCommas = formattingOptions.manageTrailingCommas
      customRemoveUnusedImports = formattingOptions.removeUnusedImports

      incrementModificationCount()
    }
  }

  companion object {
    @JvmStatic
    fun getInstance(project: Project): KtfmtSettings = project.getService(KtfmtSettings::class.java)
  }
}
