/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2025 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.ui.main.settings.behavior

import android.content.Intent
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceScreen
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.prefs.AppPrefs
import org.fcitx.fcitx5.android.data.prefs.ManagedPreferenceFragment
import org.fcitx.fcitx5.android.data.prefs.ManagedPreferenceProvider
import org.fcitx.fcitx5.android.input.config.ConfigProviders
import org.fcitx.fcitx5.android.input.config.UserConfigFiles

class KeyboardSettingsFragment : ManagedPreferenceFragment(AppPrefs.getInstance().keyboard) {

    private var calibrationPreference: Preference? = null
    private var textLayoutFilePreference: Preference? = null
    private var textLayoutFileSelectPreference: Preference? = null
    
    private val onSplitEnabledChangeListener = ManagedPreferenceProvider.OnChangeListener { key ->
        if (key == SPLIT_ENABLED_KEY) {
            val enabled = AppPrefs.getInstance().keyboard.splitKeyboardEnabled.getValue()
            calibrationPreference?.isEnabled = enabled
            // Also enable/disable "Use landscape layout when split" preference when auto-split is toggled
            val useLandscapePref = preferenceScreen.findPreference<Preference>("split_keyboard_use_landscape_layout")
            useLandscapePref?.isEnabled = enabled
        }
    }

    override fun onPreferenceUiCreated(screen: PreferenceScreen) {
        val displayPrefs = setOf(
            "keyboard_height_percent", "keyboard_height_percent_landscape",
            "keyboard_side_padding", "keyboard_side_padding_landscape",
            "keyboard_bottom_padding", "keyboard_bottom_padding_landscape",
            "split_keyboard_enabled", "split_keyboard_use_landscape_layout",
            "split_keyboard_calibration"
        )
        val appearancePrefs = setOf(
            "popup_on_key_press", "keep_keyboard_letters_uppercase", "space_key_label_mode"
        )
        val gesturePrefs = setOf(
            "space_swipe_up_behavior", "space_swipe_down_behavior",
            "space_swipe_move_cursor", "space_long_press_behavior",
            "backspace_swipe_delete_all", "swipe_symbol_behavior",
            "keyboard_long_press_delay", "lang_switch_key_behavior", "show_lang_switch_key"
        )
        val toolbarPrefs = setOf(
            "toolbar_manually_toggled", "expand_toolbar_by_default",
            "toolbar_num_row_on_password", "inline_suggestions",
            "show_voice_input_button", "preferred_voice_input"
        )
        val candidatePrefs = setOf(
            "horizontal_candidate_style", "expanded_candidate_style",
            "expanded_candidate_grid_span_count_portrait", "expanded_candidate_grid_span_count_landscape"
        )
        val feedbackPrefs = setOf(
            "haptic_on_keypress", "haptic_on_keyup", "haptic_on_repeat",
            "button_vibration_press_milliseconds", "button_vibration_long_press_milliseconds",
            "button_vibration_press_amplitude", "button_vibration_long_press_amplitude",
            "sound_on_keypress", "button_sound_volume"
        )
        val advancedPrefs = setOf(
            "reset_keyboard_on_focus_change", "vivo_keypress_workaround",
            "ignore_system_window_insets", "allow_original_plugins", "allowed_plugin_prefixes",
            "expand_keypress_area"
        )

        val existingPrefs = (0 until screen.preferenceCount).map { screen.getPreference(it) }
        val displayPrefsInScreen = existingPrefs.filter { it.key in displayPrefs }
        val appearancePrefsInScreen = existingPrefs.filter { it.key in appearancePrefs }
        val gesturePrefsInScreen = existingPrefs.filter { it.key in gesturePrefs }
        val toolbarPrefsInScreen = existingPrefs.filter { it.key in toolbarPrefs }
        val candidatePrefsInScreen = existingPrefs.filter { it.key in candidatePrefs }
        val feedbackPrefsInScreen = existingPrefs.filter { it.key in feedbackPrefs }
        val advancedPrefsInScreen = existingPrefs.filter { it.key in advancedPrefs }

        displayPrefsInScreen.forEach { screen.removePreference(it) }
        appearancePrefsInScreen.forEach { screen.removePreference(it) }
        gesturePrefsInScreen.forEach { screen.removePreference(it) }
        toolbarPrefsInScreen.forEach { screen.removePreference(it) }
        candidatePrefsInScreen.forEach { screen.removePreference(it) }
        feedbackPrefsInScreen.forEach { screen.removePreference(it) }
        advancedPrefsInScreen.forEach { screen.removePreference(it) }

        val appearanceCategory = PreferenceCategory(requireContext()).apply {
            setTitle(R.string.category_appearance)
            isIconSpaceReserved = false
        }
        screen.addPreference(appearanceCategory)
        appearanceCategory.addPreference(Preference(requireContext()).apply {
            setTitle(R.string.edit_fontset)
            setSummary(R.string.edit_fontset_summary)
            isSingleLineTitle = false
            isIconSpaceReserved = false
            setOnPreferenceClickListener {
                startActivity(Intent(requireContext(), FontsetEditorActivity::class.java))
                true
            }
        })
        appearancePrefsInScreen.forEach { appearanceCategory.addPreference(it) }

        val toolbarCategory = PreferenceCategory(requireContext()).apply {
            setTitle(R.string.category_toolbar)
            isIconSpaceReserved = false
        }
        screen.addPreference(toolbarCategory)
        toolbarPrefsInScreen.forEach { toolbarCategory.addPreference(it) }

        val layoutCategory = PreferenceCategory(requireContext()).apply {
            setTitle(R.string.category_layout)
            isIconSpaceReserved = false
        }
        screen.addPreference(layoutCategory)
        layoutCategory.addPreference(Preference(requireContext()).apply {
            setTitle(R.string.edit_popup_preset)
            setSummary(R.string.edit_popup_preset_summary)
            isSingleLineTitle = false
            isIconSpaceReserved = false
            setOnPreferenceClickListener {
                startActivity(Intent(requireContext(), PopupEditorActivity::class.java))
                true
            }
        })
        layoutCategory.addPreference(Preference(requireContext()).apply {
            setTitle(R.string.edit_text_keyboard_layout)
            setSummary(buildTextLayoutSummary())
            isSingleLineTitle = false
            isIconSpaceReserved = false
            textLayoutFilePreference = this
            setOnPreferenceClickListener {
                startActivity(Intent(requireContext(), TextKeyboardLayoutEditorActivity::class.java))
                true
            }
        })
        layoutCategory.addPreference(Preference(requireContext()).apply {
            setTitle(R.string.text_keyboard_layout_file_select_title)
            key = TEXT_LAYOUT_FILE_SELECT_PREF_KEY
            setSummary(buildCurrentTextLayoutFileSummary())
            isSingleLineTitle = false
            isIconSpaceReserved = false
            textLayoutFileSelectPreference = this
            setOnPreferenceClickListener {
                showSelectTextLayoutFileDialog()
                true
            }
        })

        val displayCategory = PreferenceCategory(requireContext()).apply {
            setTitle(R.string.category_keyboard_display)
            isIconSpaceReserved = false
        }
        screen.addPreference(displayCategory)

        calibrationPreference = Preference(requireContext()).apply {
            key = CALIBRATION_PREF_KEY
            setTitle(R.string.split_keyboard_calibration_title)
            setSummary(R.string.split_keyboard_calibration_summary)
            isSingleLineTitle = false
            isIconSpaceReserved = false
            isEnabled = AppPrefs.getInstance().keyboard.splitKeyboardEnabled.getValue()
            setOnPreferenceClickListener {
                startActivity(Intent(requireContext(), SplitKeyboardCalibrationActivity::class.java))
                true
            }
        }

        // Add display prefs, then insert calibration right after split_keyboard_use_landscape_layout
        displayPrefsInScreen.forEach { displayCategory.addPreference(it) }
        calibrationPreference?.let { calPref ->
            displayCategory.addPreference(calPref)
            // Find the "use landscape layout" preference and place calibration right after it
            val useLandscapePref = displayCategory.findPreference<Preference>("split_keyboard_use_landscape_layout")
            if (useLandscapePref != null) {
                val landscapeOrder = useLandscapePref.order
                calPref.order = landscapeOrder + 1
                // Shift all prefs after use_landscape_layout by 2 to make room
                for (i in 0 until displayCategory.preferenceCount) {
                    val p = displayCategory.getPreference(i)
                    if (p !== calPref && p !== useLandscapePref && p.order >= landscapeOrder) {
                        p.order = p.order + 2
                    }
                }
            }
        }

        val feedbackCategory = PreferenceCategory(requireContext()).apply {
            setTitle(R.string.category_feedback)
            isIconSpaceReserved = false
        }
        screen.addPreference(feedbackCategory)
        feedbackPrefsInScreen.forEach { feedbackCategory.addPreference(it) }

        val keysCategory = PreferenceCategory(requireContext()).apply {
            setTitle(R.string.category_keys)
            isIconSpaceReserved = false
        }
        screen.addPreference(keysCategory)
        keysCategory.addPreference(Preference(requireContext()).apply {
            setTitle(R.string.edit_buttons)
            setSummary(R.string.edit_buttons_summary)
            isSingleLineTitle = false
            isIconSpaceReserved = false
            setOnPreferenceClickListener {
                startActivity(Intent(requireContext(), ButtonsCustomizerActivity::class.java))
                true
            }
        })

        val gestureCategory = PreferenceCategory(requireContext()).apply {
            setTitle(R.string.category_gestures)
            isIconSpaceReserved = false
        }
        screen.addPreference(gestureCategory)
        gesturePrefsInScreen.forEach { gestureCategory.addPreference(it) }

        val useLandscapePref = screen.findPreference<Preference>("split_keyboard_use_landscape_layout")
        useLandscapePref?.isEnabled = AppPrefs.getInstance().keyboard.splitKeyboardEnabled.getValue()

        val advancedCategory = PreferenceCategory(requireContext()).apply {
            setTitle(R.string.category_advanced)
            isIconSpaceReserved = false
        }
        screen.addPreference(advancedCategory)
        advancedPrefsInScreen.forEach { advancedCategory.addPreference(it) }

        AppPrefs.getInstance().keyboard.registerOnChangeListener(onSplitEnabledChangeListener)
    }
    
    override fun onDestroy() {
        AppPrefs.getInstance().keyboard.unregisterOnChangeListener(onSplitEnabledChangeListener)
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        textLayoutFilePreference?.summary = buildTextLayoutSummary()
        textLayoutFileSelectPreference?.summary = buildCurrentTextLayoutFileSummary()
    }

    private fun buildTextLayoutSummary(): String {
        return getString(
            R.string.edit_text_keyboard_layout_summary_with_file,
            displayProfile(currentTextLayoutProfile())
        )
    }

    private fun buildCurrentTextLayoutFileSummary(): String {
        val profile = currentTextLayoutProfile()
        return getString(
            R.string.text_keyboard_layout_file_select_summary,
            displayProfile(profile)
        )
    }

    private fun currentTextLayoutProfile(): String {
        return UserConfigFiles.normalizeTextKeyboardLayoutProfile(
            AppPrefs.getInstance().keyboard.textKeyboardLayoutProfile.getValue()
        ) ?: UserConfigFiles.DEFAULT_TEXT_KEYBOARD_LAYOUT_PROFILE
    }

    private fun showSelectTextLayoutFileDialog() {
        val profiles = UserConfigFiles.listTextKeyboardLayoutProfiles().toMutableList()
        val current = currentTextLayoutProfile()
        if (current !in profiles) profiles += current
        val sortedProfiles = profiles.distinct().sortedWith(compareBy({ it != UserConfigFiles.DEFAULT_TEXT_KEYBOARD_LAYOUT_PROFILE }, { it }))
        val labels = sortedProfiles.map { displayProfile(it) }.toTypedArray()
        val initialSelection = sortedProfiles.indexOf(current).coerceAtLeast(0)

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(R.string.text_keyboard_layout_file_select_title)
            .setSingleChoiceItems(labels, initialSelection) { dialog, which ->
                val selectedProfile = sortedProfiles.getOrNull(which) ?: return@setSingleChoiceItems
                AppPrefs.getInstance().keyboard.textKeyboardLayoutProfile.setValue(selectedProfile)
                ConfigProviders.provider = ConfigProviders.provider
                textLayoutFilePreference?.summary = buildTextLayoutSummary()
                textLayoutFileSelectPreference?.summary = buildCurrentTextLayoutFileSummary()
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun displayProfile(profile: String): String {
        return if (profile == UserConfigFiles.DEFAULT_TEXT_KEYBOARD_LAYOUT_PROFILE) {
            getString(R.string.default_)
        } else {
            profile
        }
    }

    companion object {
        private const val CALIBRATION_PREF_KEY = "split_keyboard_calibration"
        private const val SPLIT_ENABLED_KEY = "split_keyboard_enabled"
        private const val TEXT_LAYOUT_FILE_SELECT_PREF_KEY = "text_keyboard_layout_file_select"
    }
}
