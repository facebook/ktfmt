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

import com.facebook.ktfmt.intellij.KtfmtSettings.EnabledState.Disabled
import com.facebook.ktfmt.intellij.KtfmtSettings.EnabledState.Enabled
import com.facebook.ktfmt.intellij.KtfmtSettings.EnabledState.Unknown
import com.facebook.ktfmt.intellij.UiFormatterStyle.Meta
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.Service.Level.PROJECT
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project

@Service(PROJECT)
@State(name = "KtfmtSettings", storages = [Storage("ktfmt.xml")])
internal class KtfmtSettings : PersistentStateComponent<KtfmtSettings.State> {
  private var state = State()

  override fun getState(): State = state

  override fun loadState(state: State) {
    this.state = state
  }

  var isEnabled: Boolean
    get() = state.enabled == Enabled
    set(enabled) {
      setEnabled(if (enabled) Enabled else Disabled)
    }

  fun setEnabled(enabled: EnabledState) {
    state.enabled = enabled
  }

  val isUninitialized: Boolean
    get() = state.enabled == Unknown

  var uiFormatterStyle: UiFormatterStyle
    get() = state.uiFormatterStyle
    set(uiFormatterStyle) {
      state.uiFormatterStyle = uiFormatterStyle
    }

  internal enum class EnabledState {
    Unknown,
    Enabled,
    Disabled,
  }

  internal class State {
    @JvmField var enabled: EnabledState = Unknown
    var uiFormatterStyle: UiFormatterStyle = Meta

    // enabled used to be a boolean so we use bean property methods for backwards
    // compatibility
    fun setEnabled(enabledStr: String?) {
      enabled =
          when {
            enabledStr == null -> Unknown
            enabledStr.toBoolean() -> Enabled
            else -> Disabled
          }
    }

    fun getEnabled(): String? =
        when (enabled) {
          Enabled -> "true"
          Disabled -> "false"
          else -> null
        }
  }

  companion object {
    @JvmStatic
    fun getInstance(project: Project): KtfmtSettings = project.getService(KtfmtSettings::class.java)
  }
}
