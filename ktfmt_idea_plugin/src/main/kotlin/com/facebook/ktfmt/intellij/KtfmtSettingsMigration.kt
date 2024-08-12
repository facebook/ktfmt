package com.facebook.ktfmt.intellij

import com.facebook.ktfmt.intellij.KtfmtSettings.EnabledState.Disabled
import com.facebook.ktfmt.intellij.KtfmtSettings.EnabledState.Enabled
import com.facebook.ktfmt.intellij.KtfmtSettings.EnabledState.Unknown
import com.facebook.ktfmt.intellij.KtfmtSettingsMigration.MigrationState
import com.intellij.openapi.components.BaseState
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.SimplePersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros

@Service(Service.Level.PROJECT)
@State(name = "KtfmtSettingsMigration", storages = [(Storage(StoragePathMacros.WORKSPACE_FILE))])
internal class KtfmtSettingsMigration :
    SimplePersistentStateComponent<MigrationState>(MigrationState()) {

  // ---------
  // Changelog
  // ---------
  //
  // v2 enabled [bool] -> enableKtfmt [enum], custom styles (0.52+)
  // v1 initial version - enabled is a boolean, only preset styles
  var stateVersion
    get() = state.stateVersion.takeIf { it > 0 } ?: 1
    set(value) {
      state.stateVersion = value
    }

  @Suppress("DEPRECATION") // Accessing deprecated properties
  fun migrateFromV1ToCurrent(v1State: KtfmtSettings.State): KtfmtSettings.State {
    val migrated =
        KtfmtSettings.State().apply {
          copyFrom(v1State)

          enableKtfmt =
              when (v1State.enabled) {
                "true" -> Enabled
                "false" -> Disabled
                else -> Unknown
              }
          enabled = null
        }
    state.stateVersion = CURRENT_VERSION
    return migrated
  }

  class MigrationState : BaseState() {
    var stateVersion by property(-1)
  }

  companion object {
    const val CURRENT_VERSION = 2
  }
}
