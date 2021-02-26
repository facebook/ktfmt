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

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nullable;

@State(
    name = "KtfmtSettings",
    storages = {@Storage("ktfmt.xml")})
class KtfmtSettings implements PersistentStateComponent<KtfmtSettings.State> {

  private State state = new State();

  static KtfmtSettings getInstance(Project project) {
    return ServiceManager.getService(project, KtfmtSettings.class);
  }

  @Nullable
  @Override
  public State getState() {
    return state;
  }

  @Override
  public void loadState(State state) {
    this.state = state;
  }

  boolean isEnabled() {
    return state.enabled.equals(EnabledState.ENABLED);
  }

  void setEnabled(boolean enabled) {
    setEnabled(enabled ? EnabledState.ENABLED : EnabledState.DISABLED);
  }

  void setEnabled(EnabledState enabled) {
    state.enabled = enabled;
  }

  boolean isUninitialized() {
    return state.enabled.equals(EnabledState.UNKNOWN);
  }

  UiFormatterStyle getUiFormatterStyle() {
    return state.uiFormatterStyle;
  }

  void setUiFormatterStyle(UiFormatterStyle uiFormatterStyle) {
    state.uiFormatterStyle = uiFormatterStyle;
  }

  enum EnabledState {
    UNKNOWN,
    ENABLED,
    DISABLED;
  }

  static class State {

    private EnabledState enabled = EnabledState.UNKNOWN;
    public UiFormatterStyle uiFormatterStyle = UiFormatterStyle.DEFAULT;

    // enabled used to be a boolean so we use bean property methods for backwards
    // compatibility
    public void setEnabled(@Nullable String enabledStr) {
      if (enabledStr == null) {
        enabled = EnabledState.UNKNOWN;
      } else if (Boolean.valueOf(enabledStr)) {
        enabled = EnabledState.ENABLED;
      } else {
        enabled = EnabledState.DISABLED;
      }
    }

    public String getEnabled() {
      switch (enabled) {
        case ENABLED:
          return "true";
        case DISABLED:
          return "false";
        default:
          return null;
      }
    }
  }
}
