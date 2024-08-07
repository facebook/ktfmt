package com.facebook.ktfmt.intellij

import com.facebook.ktfmt.intellij.KtfmtSettings.EnabledState.Disabled
import com.facebook.ktfmt.intellij.KtfmtSettings.EnabledState.Enabled
import com.intellij.openapi.options.BoundSearchableConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.layout.selected
import javax.swing.JCheckBox

@Suppress("DialogTitleCapitalization")
class KtfmtConfigurable(project: Project) :
    BoundSearchableConfigurable(
        displayName = "ktfmt Settings",
        _id = "com.facebook.ktfmt_idea_plugin.settings",
        helpTopic = "ktfmt",
    ) {
    private val settings = KtfmtSettings.getInstance(project)

    override fun createPanel(): DialogPanel = panel {
        lateinit var enabledCheckbox: JCheckBox
        row {
            enabledCheckbox =
                checkBox("Enable ktfmt")
                    .bindSelected(
                        getter = { settings.isEnabled },
                        setter = { settings.setEnabled(if (it) Enabled else Disabled) },
                    )
                    .component
        }

        row {
            comboBox(UiFormatterStyle.entries.toList())
                .label("Code style:")
                .bindItem(
                    getter = { settings.uiFormatterStyle },
                    setter = { settings.uiFormatterStyle = it ?: UiFormatterStyle.Meta },
                )
                .enabledIf(enabledCheckbox.selected)
        }
    }
}
