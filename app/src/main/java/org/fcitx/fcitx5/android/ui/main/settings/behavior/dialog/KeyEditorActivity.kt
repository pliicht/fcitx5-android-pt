/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.ui.main.settings.behavior.dialog

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.text.Editable
import android.text.TextWatcher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import kotlinx.serialization.json.JsonObject
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.theme.SystemColorResourceId
import org.fcitx.fcitx5.android.data.theme.ThemeManager
import org.fcitx.fcitx5.android.data.theme.ThemeMonet
import org.fcitx.fcitx5.android.ui.main.settings.behavior.utils.LayoutJsonUtils
import org.fcitx.fcitx5.android.ui.main.settings.theme.SystemColorResourcePickerDialog
import org.fcitx.fcitx5.android.ui.main.settings.theme.ThemeColorEditorActivity
import org.fcitx.fcitx5.android.utils.DeviceUtil
import org.fcitx.fcitx5.android.utils.serializable
import splitties.dimensions.dp
import splitties.resources.styledColor
import splitties.views.backgroundColor
import splitties.views.dsl.core.matchParent
import splitties.views.dsl.core.wrapContent
import java.io.Serializable
import java.util.Arrays
import java.util.LinkedHashMap

class KeyEditorActivity : AppCompatActivity() {

    private data class EditableColorField(
        val customKey: String,
        val monetKey: String,
        val labelRes: Int,
        val themeColorGetter: (org.fcitx.fcitx5.android.data.theme.Theme) -> Int,
        val supportedTypes: Set<String>? = null
    )

    private val toolbar by lazy {
        Toolbar(this).apply {
            backgroundColor = styledColor(android.R.attr.colorPrimary)
            elevation = dp(4f)
            setSubtitleTextAppearance(context, android.R.style.TextAppearance_Small)
            setSubtitleTextColor(styledColor(android.R.attr.textColorSecondary))
        }
    }

    private val contentContainer by lazy {
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val pad = dp(12)
            setPadding(pad, pad, pad, pad)
        }
    }

    private val scrollView by lazy {
        ScrollView(this).apply {
            addView(contentContainer, LinearLayout.LayoutParams(matchParent, wrapContent))
        }
    }

    private val rootView by lazy {
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(toolbar, LinearLayout.LayoutParams(matchParent, wrapContent))
            addView(scrollView, LinearLayout.LayoutParams(matchParent, 0).apply { weight = 1f })
        }
    }

    private val uiBuilder by lazy { KeyboardEditorUiBuilder(this) }

    private var rowIndex: Int = -1
    private var keyIndex: Int? = null
    private var isEditingSubModeLayout: Boolean = false
    private var currentSubModeLabel: String? = null
    private var hasMultiSubmodeSupport: Boolean = false
    private var keyData: MutableMap<String, Any?> = mutableMapOf()

    private lateinit var typeSpinner: Spinner
    private lateinit var fieldsContainer: LinearLayout
    private var selectedType: String = "AlphabetKey"

    private var alphabetMainEdit: EditText? = null
    private var alphabetAltEdit: EditText? = null
    private var alphabetHintTextEdit: EditText? = null
    private var alphabetWeightEdit: EditText? = null
    private var alphabetSwipeUpPopupEdit: EditText? = null
    private var alphabetSwipeDownPopupEdit: EditText? = null
    private var alphabetSwipeLeftPopupEdit: EditText? = null
    private var alphabetSwipeRightPopupEdit: EditText? = null
    private var alphabetLongPressPopupEdit: EditText? = null
    private var alphabetSwipeUpPopupEnabledSwitch: SwitchCompat? = null
    private var alphabetSwipeDownPopupEnabledSwitch: SwitchCompat? = null
    private var alphabetSwipeLeftPopupEnabledSwitch: SwitchCompat? = null
    private var alphabetSwipeRightPopupEnabledSwitch: SwitchCompat? = null
    private var alphabetLongPressPopupEnabledSwitch: SwitchCompat? = null
    private var alphabetDisplayTextSimpleEdit: EditText? = null
    private var alphabetDisplayTextModeSpecific = false
    private var alphabetDisplayTextSimpleValue = ""
    private val alphabetDisplayTextModeItems = mutableListOf<KeyboardEditorUiBuilder.DisplayTextItem>()
    private val alphabetDisplayTextRowBindings = mutableListOf<KeyboardEditorUiBuilder.DisplayTextRowBinding>()

    private var layoutSwitchLabelEdit: EditText? = null
    private var layoutSwitchSubLabelEdit: EditText? = null
    private var layoutSwitchWeightEdit: EditText? = null

    private var symbolLabelEdit: EditText? = null
    private var symbolWeightEdit: EditText? = null

    private var macroLabelEdit: EditText? = null
    private var macroDisplayTextSimpleEdit: EditText? = null
    private var macroDisplayTextModeSpecific = false
    private var macroDisplayTextSimpleValue = ""
    private val macroDisplayTextModeItems = mutableListOf<KeyboardEditorUiBuilder.DisplayTextItem>()
    private val macroDisplayTextRowBindings = mutableListOf<KeyboardEditorUiBuilder.DisplayTextRowBinding>()
    private var macroAltLabelEdit: EditText? = null
    private var macroLongPressLabelEdit: EditText? = null
    private var macroWeightEdit: EditText? = null

    private var simpleWeightEdit: EditText? = null

    private var macroTapStepsData: List<Map<*, *>> = emptyList()
    private var macroSwipeStepsData: List<Map<*, *>> = emptyList()
    private var macroSwipeUpStepsData: List<Map<*, *>> = emptyList()
    private var macroSwipeDownStepsData: List<Map<*, *>> = emptyList()
    private var macroSwipeLeftStepsData: List<Map<*, *>> = emptyList()
    private var macroSwipeRightStepsData: List<Map<*, *>> = emptyList()
    private var macroLongPressStepsData: List<Map<*, *>> = emptyList()
    private var macroEditCallback: ((List<Map<*, *>>) -> Unit)? = null
    private var saveMenuItem: MenuItem? = null
    private var deleteMenuItem: MenuItem? = null
    private var baselineKeySnapshot: String = ""
    
    private var pendingBackspaceSwipeDeleteAll: Boolean? = null
    private var pendingSpaceSwipeMoveCursor: Boolean? = null
    private var pendingSpaceSwipeUpBehavior: org.fcitx.fcitx5.android.input.keyboard.SpaceSwipeUpBehavior? = null
    private var pendingSpaceLongPressBehavior: org.fcitx.fcitx5.android.input.keyboard.SpaceLongPressBehavior? = null
    private val editableColorFields = listOf(
        EditableColorField(
            customKey = "textColor",
            monetKey = "textColorMonet",
            labelRes = R.string.text_keyboard_layout_key_text_color,
            themeColorGetter = { it.keyTextColor }
        ),
        EditableColorField(
            customKey = "altTextColor",
            monetKey = "altTextColorMonet",
            labelRes = R.string.text_keyboard_layout_key_alt_text_color,
            themeColorGetter = { it.altKeyTextColor },
            supportedTypes = setOf("AlphabetKey", "MacroKey")
        ),
        EditableColorField(
            customKey = "backgroundColor",
            monetKey = "backgroundColorMonet",
            labelRes = R.string.text_keyboard_layout_key_background_color,
            themeColorGetter = { it.keyBackgroundColor }
        ),
        EditableColorField(
            customKey = "shadowColor",
            monetKey = "shadowColorMonet",
            labelRes = R.string.text_keyboard_layout_key_shadow_color,
            themeColorGetter = { it.keyShadowColor }
        )
    )

    private val macroEditorLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val data = result.data ?: return@registerForActivityResult
            if (result.resultCode != RESULT_OK) return@registerForActivityResult
            val steps = data.serializable<ArrayList<Map<*, *>>>(MacroEditorActivity.EXTRA_MACRO_RESULT) ?: return@registerForActivityResult
            macroEditCallback?.invoke(steps)
        }

    private val colorEditorLauncher =
        registerForActivityResult(ThemeColorEditorActivity.Contract()) { result ->
            result ?: return@registerForActivityResult
            val field = editableColorFields.firstOrNull { it.customKey == result.fieldName } ?: return@registerForActivityResult
            keyData[field.customKey] = result.color
            keyData.remove(field.monetKey)
            rebuildFields()
            updateActionButtonState()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        setContentView(rootView)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        readIntentExtras()
        baselineKeySnapshot = snapshotOf(keyData)

        val toolbarBaseTopPadding = toolbar.paddingTop
        ViewCompat.setOnApplyWindowInsetsListener(toolbar) { view, insets ->
            val statusTop = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            view.updatePadding(top = toolbarBaseTopPadding + statusTop)
            insets
        }
        ViewCompat.requestApplyInsets(toolbar)

        buildForm()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (keyIndex != null) {
            deleteMenuItem = menu.add(Menu.NONE, MENU_DELETE_ID, 1, getString(R.string.delete)).apply {
                setIcon(android.R.drawable.ic_menu_delete)
                setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
            }
        }

        saveMenuItem = menu.add(Menu.NONE, MENU_SAVE_ID, 2, getString(R.string.save)).apply {
            setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS or MenuItem.SHOW_AS_ACTION_WITH_TEXT)
        }

        updateActionButtonState()
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        android.R.id.home -> {
            finish()
            true
        }
        MENU_SAVE_ID -> {
            saveAndFinish()
            true
        }
        MENU_DELETE_ID -> {
            confirmDelete()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    private fun readIntentExtras() {
        rowIndex = intent.getIntExtra(EXTRA_ROW_INDEX, -1)
        keyIndex = intent.takeIf { it.hasExtra(EXTRA_KEY_INDEX) }?.getIntExtra(EXTRA_KEY_INDEX, -1)?.takeIf { it >= 0 }
        isEditingSubModeLayout = intent.getBooleanExtra(EXTRA_IS_EDITING_SUBMODE_LAYOUT, false)
        currentSubModeLabel = intent.getStringExtra(EXTRA_CURRENT_SUBMODE_LABEL)
        hasMultiSubmodeSupport = intent.getBooleanExtra(EXTRA_HAS_MULTI_SUBMODE_SUPPORT, false)

        val received = serializableExtraCompat<HashMap<String, Any?>>(intent, EXTRA_KEY_DATA)
        keyData = received?.toMutableMap() ?: mutableMapOf()
        selectedType = keyData["type"] as? String ?: "AlphabetKey"

        val titleRes = if (keyIndex != null) R.string.edit else R.string.text_keyboard_layout_add_key
        supportActionBar?.setTitle(titleRes)
        toolbar.subtitle = currentSubModeLabel?.takeIf { isEditingSubModeLayout }
    }

    private fun buildForm() {
        contentContainer.removeAllViews()
        typeSpinner = uiBuilder.setupTypeSpinner(contentContainer, keyData)
        fieldsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(8), 0, 0)
        }
        contentContainer.addView(fieldsContainer)

        typeSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                selectedType = KeyboardEditorUiBuilder.KEY_TYPES[position]
                rebuildFields()
                updateActionButtonState()
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        rebuildFields()
    }

    private fun rebuildFields() {
        fieldsContainer.clearFocus()
        for (i in 0 until fieldsContainer.childCount) {
            fieldsContainer.getChildAt(i).clearFocus()
        }

        fieldsContainer.removeAllViews()
        alphabetMainEdit = null
        alphabetAltEdit = null
        alphabetHintTextEdit = null
        alphabetWeightEdit = null
        alphabetSwipeUpPopupEdit = null
        alphabetSwipeDownPopupEdit = null
        alphabetSwipeLeftPopupEdit = null
        alphabetSwipeRightPopupEdit = null
        alphabetLongPressPopupEdit = null
        alphabetSwipeUpPopupEnabledSwitch = null
        alphabetSwipeDownPopupEnabledSwitch = null
        alphabetSwipeLeftPopupEnabledSwitch = null
        alphabetSwipeRightPopupEnabledSwitch = null
        alphabetLongPressPopupEnabledSwitch = null
        alphabetDisplayTextSimpleEdit = null
        alphabetDisplayTextRowBindings.clear()
        layoutSwitchLabelEdit = null
        layoutSwitchSubLabelEdit = null
        layoutSwitchWeightEdit = null
        symbolLabelEdit = null
        symbolWeightEdit = null
        macroLabelEdit = null
        macroDisplayTextSimpleEdit = null
        macroDisplayTextModeSpecific = false
        macroDisplayTextSimpleValue = ""
        macroDisplayTextModeItems.clear()
        macroDisplayTextRowBindings.clear()
        macroAltLabelEdit = null
        macroWeightEdit = null
        simpleWeightEdit = null

        initDisplayText(
            keyData,
            isEditingSubModeLayout,
            currentSubModeLabel
        ) { modeSpecific, simpleValue, items ->
            alphabetDisplayTextModeSpecific = modeSpecific
            alphabetDisplayTextSimpleValue = simpleValue
            alphabetDisplayTextModeItems.clear()
            alphabetDisplayTextModeItems.addAll(items)
        }

        initDisplayText(
            keyData,
            isEditingSubModeLayout,
            currentSubModeLabel,
            labelTextKey = "displayText"
        ) { modeSpecific, simpleValue, items ->
            macroDisplayTextModeSpecific = modeSpecific
            macroDisplayTextSimpleValue = simpleValue
            macroDisplayTextModeItems.clear()
            macroDisplayTextModeItems.addAll(items)
        }

        when (selectedType) {
            "AlphabetKey" -> {
                val mainEdit = uiBuilder.createEditField(
                    getString(R.string.text_keyboard_layout_key_main),
                    keyData["main"] as? String ?: ""
                )
                val altEdit = uiBuilder.createEditField(
                    getString(R.string.text_keyboard_layout_key_alt),
                    keyData["alt"] as? String ?: ""
                )
                val hintTextEdit = uiBuilder.createEditField(
                    getString(R.string.text_keyboard_layout_key_hint),
                    keyData["hint"] as? String ?: ""
                )
                val weightEdit = uiBuilder.createEditField(
                    getString(R.string.text_keyboard_layout_key_weight),
                    (keyData["weight"] as? Number)?.toString() ?: ""
                )
                fieldsContainer.addView(mainEdit.first)
                fieldsContainer.addView(altEdit.first)
                fieldsContainer.addView(hintTextEdit.first)
                fieldsContainer.addView(weightEdit.first)

                val displayTextContainer = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                }
                fieldsContainer.addView(displayTextContainer)

                alphabetMainEdit = mainEdit.second
                alphabetAltEdit = altEdit.second
                alphabetHintTextEdit = hintTextEdit.second
                alphabetWeightEdit = weightEdit.second

                uiBuilder.renderDisplayTextEditor(
                    displayTextContainer,
                    alphabetDisplayTextModeSpecific,
                    alphabetDisplayTextSimpleValue,
                    alphabetDisplayTextModeItems,
                    alphabetDisplayTextRowBindings,
                    isEditingSubModeLayout,
                    hasMultiSubmodeSupport
                ) { modeSpecific, simpleValue, items, bindings, simpleTextEdit ->
                    alphabetDisplayTextModeSpecific = modeSpecific
                    alphabetDisplayTextSimpleValue = simpleValue
                    alphabetDisplayTextModeItems.clear()
                    alphabetDisplayTextModeItems.addAll(items)
                    alphabetDisplayTextRowBindings.clear()
                    alphabetDisplayTextRowBindings.addAll(bindings)
                    alphabetDisplayTextSimpleEdit = simpleTextEdit ?: bindings.lastOrNull()?.valueEdit
                    updateActionButtonState()
                }

                val swipeUpAction = keyData["swipeUp"] as? Map<*, *>
                val swipeDownAction = keyData["swipeDown"] as? Map<*, *>
                val swipeLeftAction = keyData["swipeLeft"] as? Map<*, *>
                val swipeRightAction = keyData["swipeRight"] as? Map<*, *>
                val longPressAction = keyData["longPress"] as? Map<*, *>

                val swipeUpMacroSteps = (swipeUpAction?.get("macro") as? List<*>)?.filterIsInstance<Map<*, *>>() ?: emptyList()
                val swipeDownMacroSteps = (swipeDownAction?.get("macro") as? List<*>)?.filterIsInstance<Map<*, *>>() ?: emptyList()
                val swipeLeftMacroSteps = (swipeLeftAction?.get("macro") as? List<*>)?.filterIsInstance<Map<*, *>>() ?: emptyList()
                val swipeRightMacroSteps = (swipeRightAction?.get("macro") as? List<*>)?.filterIsInstance<Map<*, *>>() ?: emptyList()
                val longPressMacroSteps = (longPressAction?.get("macro") as? List<*>)?.filterIsInstance<Map<*, *>>() ?: emptyList()

                macroSwipeUpStepsData = swipeUpMacroSteps
                macroSwipeDownStepsData = swipeDownMacroSteps
                macroSwipeLeftStepsData = swipeLeftMacroSteps
                macroSwipeRightStepsData = swipeRightMacroSteps
                macroLongPressStepsData = longPressMacroSteps

                val swipeLabel = TextView(this).apply {
                    text = getString(R.string.key_editor_swipe_longpress_label)
                    textSize = 14f
                    setTextColor(styledColor(android.R.attr.colorAccent))
                    setPadding(0, dp(16), 0, dp(2))
                }
                fieldsContainer.addView(swipeLabel)

                val swipeHintLabel = TextView(this).apply {
                    text = getString(R.string.key_editor_popup_hint)
                    textSize = 12f
                    setTextColor(styledColor(android.R.attr.textColorSecondary))
                    setPadding(0, 0, 0, dp(8))
                }
                fieldsContainer.addView(swipeHintLabel)

                val swipePopupContainer = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                }

                // Popup preview label
                val popupPreviewLabel = TextView(this).apply {
                    text = getString(R.string.key_editor_popup_preview_label)
                    textSize = 13f
                    setTextColor(styledColor(android.R.attr.textColorSecondary))
                    setPadding(0, dp(8), 0, dp(4))
                }
                swipePopupContainer.addView(popupPreviewLabel)

                val swipeUpPopupEdit = uiBuilder.createEditField(
                    getString(R.string.text_keyboard_layout_key_swipe_up_popup),
                    keyData["swipeUpPopup"] as? String ?: ""
                )
                val swipeDownPopupEdit = uiBuilder.createEditField(
                    getString(R.string.text_keyboard_layout_key_swipe_down_popup),
                    keyData["swipeDownPopup"] as? String ?: ""
                )
                val swipeLeftPopupEdit = uiBuilder.createEditField(
                    getString(R.string.text_keyboard_layout_key_swipe_left_popup),
                    keyData["swipeLeftPopup"] as? String ?: ""
                )
                val swipeRightPopupEdit = uiBuilder.createEditField(
                    getString(R.string.text_keyboard_layout_key_swipe_right_popup),
                    keyData["swipeRightPopup"] as? String ?: ""
                )
                val longPressPopupEdit = uiBuilder.createEditField(
                    getString(R.string.text_keyboard_layout_key_long_press_popup),
                    keyData["longPressPopup"] as? String ?: ""
                )

                alphabetSwipeUpPopupEdit = swipeUpPopupEdit.second
                alphabetSwipeDownPopupEdit = swipeDownPopupEdit.second
                alphabetSwipeLeftPopupEdit = swipeLeftPopupEdit.second
                alphabetSwipeRightPopupEdit = swipeRightPopupEdit.second
                alphabetLongPressPopupEdit = longPressPopupEdit.second

                // Create per-direction popup enabled switches
                fun createPopupEnabledRow(label: String, enabled: Boolean): Pair<LinearLayout, SwitchCompat> {
                    val row = LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                        setPadding(0, dp(2), 0, dp(2))
                    }
                    val labelView = TextView(this).apply {
                        text = label
                        textSize = 13f
                        setTextColor(styledColor(android.R.attr.textColorSecondary))
                        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    }
                    val switch = SwitchCompat(this).apply {
                        isChecked = enabled
                        textSize = 13f
                    }
                    row.addView(labelView)
                    row.addView(switch)
                    return row to switch
                }

                val swipeUpPopupEnabled = createPopupEnabledRow(
                    getString(R.string.key_editor_swipe_up_hint), keyData["swipeUpPopupEnabled"] as? Boolean ?: true
                )
                val swipeDownPopupEnabled = createPopupEnabledRow(
                    getString(R.string.key_editor_swipe_down_hint), keyData["swipeDownPopupEnabled"] as? Boolean ?: true
                )
                val swipeLeftPopupEnabled = createPopupEnabledRow(
                    getString(R.string.key_editor_swipe_left_hint), keyData["swipeLeftPopupEnabled"] as? Boolean ?: true
                )
                val swipeRightPopupEnabled = createPopupEnabledRow(
                    getString(R.string.key_editor_swipe_right_hint), keyData["swipeRightPopupEnabled"] as? Boolean ?: true
                )
                val longPressPopupEnabled = createPopupEnabledRow(
                    getString(R.string.key_editor_longpress_hint), keyData["longPressPopupEnabled"] as? Boolean ?: true
                )

                alphabetSwipeUpPopupEnabledSwitch = swipeUpPopupEnabled.second
                alphabetSwipeDownPopupEnabledSwitch = swipeDownPopupEnabled.second
                alphabetSwipeLeftPopupEnabledSwitch = swipeLeftPopupEnabled.second
                alphabetSwipeRightPopupEnabledSwitch = swipeRightPopupEnabled.second
                alphabetLongPressPopupEnabledSwitch = longPressPopupEnabled.second

                swipePopupContainer.addView(swipeUpPopupEdit.first)
                swipePopupContainer.addView(swipeUpPopupEnabled.first)
                swipePopupContainer.addView(swipeDownPopupEdit.first)
                swipePopupContainer.addView(swipeDownPopupEnabled.first)
                swipePopupContainer.addView(swipeLeftPopupEdit.first)
                swipePopupContainer.addView(swipeLeftPopupEnabled.first)
                swipePopupContainer.addView(swipeRightPopupEdit.first)
                swipePopupContainer.addView(swipeRightPopupEnabled.first)
                swipePopupContainer.addView(longPressPopupEdit.first)
                swipePopupContainer.addView(longPressPopupEnabled.first)
                fieldsContainer.addView(swipePopupContainer)

                val swipeGridLayout = android.widget.TableLayout(this).apply {
                    isStretchAllColumns = true
                    isShrinkAllColumns = true
                }

                val swipeEvents = listOf(
                    Triple(getString(R.string.key_editor_swipe_up_btn), macroSwipeUpStepsData, swipeUpMacroSteps),
                    Triple(getString(R.string.key_editor_swipe_down_btn), macroSwipeDownStepsData, swipeDownMacroSteps),
                    Triple(getString(R.string.key_editor_swipe_left_btn), macroSwipeLeftStepsData, swipeLeftMacroSteps),
                    Triple(getString(R.string.key_editor_swipe_right_btn), macroSwipeRightStepsData, swipeRightMacroSteps),
                    Triple(getString(R.string.key_editor_longpress_btn), macroLongPressStepsData, longPressMacroSteps)
                )

                val rowCount = (swipeEvents.size + 1) / 2
                for (row in 0 until rowCount) {
                    val tableRow = android.widget.TableRow(this).apply {
                        layoutParams = android.widget.TableRow.LayoutParams(
                            android.widget.TableRow.LayoutParams.MATCH_PARENT,
                            android.widget.TableRow.LayoutParams.WRAP_CONTENT
                        ).apply {
                            bottomMargin = dp(4)
                        }
                    }

                    for (col in 0 until 2) {
                        val index = row * 2 + col
                        if (index < swipeEvents.size) {
                            val (title, stepsData, macroSteps) = swipeEvents[index]
                            val cellView = createCompactMacroButton(title, stepsData) {
                                val eventTitle = when (index) {
                                    0 -> getString(R.string.key_editor_swipe_up_event)
                                    1 -> getString(R.string.key_editor_swipe_down_event)
                                    2 -> getString(R.string.key_editor_swipe_left_event)
                                    3 -> getString(R.string.key_editor_swipe_right_event)
                                    else -> getString(R.string.key_editor_longpress_event)
                                }
                                openMacroEditor(stepsData, eventTitle) { newSteps ->
                                    val draft = buildDraftKeyData()
                                    when (index) {
                                        0 -> { macroSwipeUpStepsData = newSteps; draft["swipeUp"] = mapOf("macro" to newSteps) }
                                        1 -> { macroSwipeDownStepsData = newSteps; draft["swipeDown"] = mapOf("macro" to newSteps) }
                                        2 -> { macroSwipeLeftStepsData = newSteps; draft["swipeLeft"] = mapOf("macro" to newSteps) }
                                        3 -> { macroSwipeRightStepsData = newSteps; draft["swipeRight"] = mapOf("macro" to newSteps) }
                                        4 -> { macroLongPressStepsData = newSteps; draft["longPress"] = mapOf("macro" to newSteps) }
                                    }
                                    keyData = draft
                                    rebuildFields()
                                    updateActionButtonState()
                                }
                            }
                            tableRow.addView(cellView)
                        } else {
                            val emptyView = android.view.View(this).apply {
                                layoutParams = android.widget.TableRow.LayoutParams(
                                    0,
                                    android.widget.TableRow.LayoutParams.WRAP_CONTENT,
                                    1f
                                )
                            }
                            tableRow.addView(emptyView)
                        }
                    }
                    swipeGridLayout.addView(tableRow)
                }
                fieldsContainer.addView(swipeGridLayout)
            }

            "LayoutSwitchKey" -> {
                val labelEdit = uiBuilder.createEditField(
                    getString(R.string.text_keyboard_layout_key_label),
                    keyData["label"] as? String ?: "?123"
                )
                val weightEdit = uiBuilder.createEditField(
                    getString(R.string.text_keyboard_layout_key_weight),
                    (keyData["weight"] as? Number)?.toString() ?: ""
                )
                val subLabelEdit = uiBuilder.createEditField(
                    getString(R.string.text_keyboard_layout_key_sub_label),
                    keyData["subLabel"] as? String ?: ""
                )
                layoutSwitchLabelEdit = labelEdit.second
                layoutSwitchWeightEdit = weightEdit.second
                layoutSwitchSubLabelEdit = subLabelEdit.second
                fieldsContainer.addView(labelEdit.first)
                fieldsContainer.addView(weightEdit.first)
                fieldsContainer.addView(subLabelEdit.first)
            }

            "SymbolKey" -> {
                val labelEdit = uiBuilder.createEditField(
                    getString(R.string.text_keyboard_layout_key_label),
                    keyData["label"] as? String ?: "."
                )
                val weightEdit = uiBuilder.createEditField(
                    getString(R.string.text_keyboard_layout_key_weight),
                    (keyData["weight"] as? Number)?.toString() ?: ""
                )
                symbolLabelEdit = labelEdit.second
                symbolWeightEdit = weightEdit.second
                fieldsContainer.addView(labelEdit.first)
                fieldsContainer.addView(weightEdit.first)
            }

            "MacroKey" -> {
                val labelEdit = uiBuilder.createEditField(
                    getString(R.string.text_keyboard_layout_key_label),
                    keyData["label"] as? String ?: ""
                )
                val altLabelEdit = uiBuilder.createEditField(
                    getString(R.string.text_keyboard_layout_alt_label),
                    keyData["altLabel"] as? String ?: ""
                )
                val longPressLabelEdit = uiBuilder.createEditField(
                    getString(R.string.text_keyboard_layout_longpress_label),
                    keyData["longPressLabel"] as? String ?: ""
                )
                longPressLabelEdit.second.hint = getString(R.string.text_keyboard_layout_longpress_label_fallback_hint)
                val weightEdit = uiBuilder.createEditField(
                    getString(R.string.text_keyboard_layout_key_weight),
                    (keyData["weight"] as? Number)?.toString() ?: ""
                )

                fieldsContainer.addView(labelEdit.first)
                fieldsContainer.addView(altLabelEdit.first)
                fieldsContainer.addView(longPressLabelEdit.first)
                fieldsContainer.addView(weightEdit.first)

                macroLabelEdit = labelEdit.second
                macroAltLabelEdit = altLabelEdit.second
                macroLongPressLabelEdit = longPressLabelEdit.second
                macroWeightEdit = weightEdit.second

                val labelTextContainer = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                }
                fieldsContainer.addView(labelTextContainer)

                uiBuilder.renderDisplayTextEditor(
                    labelTextContainer,
                    macroDisplayTextModeSpecific,
                    macroDisplayTextSimpleValue,
                    macroDisplayTextModeItems,
                    macroDisplayTextRowBindings,
                    isEditingSubModeLayout,
                    hasMultiSubmodeSupport = !isEditingSubModeLayout,
                    callback = { modeSpecific, simpleValue, items, bindings, simpleTextEdit ->
                        macroDisplayTextModeSpecific = modeSpecific
                        macroDisplayTextSimpleValue = simpleValue
                        macroDisplayTextModeItems.clear()
                        macroDisplayTextModeItems.addAll(items)
                        macroDisplayTextRowBindings.clear()
                        macroDisplayTextRowBindings.addAll(bindings)
                        macroDisplayTextSimpleEdit = simpleTextEdit
                        updateActionButtonState()
                    }
                )

                val tapAction = keyData["tap"] as? Map<*, *>
                val swipeAction = keyData["swipe"] as? Map<*, *>

                val tapMacroSteps = (tapAction?.get("macro") as? List<*>)?.filterIsInstance<Map<*, *>>() ?: emptyList()
                val swipeMacroSteps = (swipeAction?.get("macro") as? List<*>)?.filterIsInstance<Map<*, *>>() ?: emptyList()

                macroTapStepsData = tapMacroSteps
                macroSwipeStepsData = swipeMacroSteps

                createMacroEditorButton(
                    title = getString(R.string.text_keyboard_layout_macro_tap_event),
                    previewText = buildMacroPreview(tapMacroSteps),
                    onClick = {
                        openMacroEditor(macroTapStepsData, getString(R.string.text_keyboard_layout_macro_tap_event)) { newSteps ->
                            val draft = buildDraftKeyData()
                            macroTapStepsData = newSteps
                            draft["tap"] = mapOf("macro" to newSteps)
                            keyData = draft
                            rebuildFields()
                            updateActionButtonState()
                        }
                    }
                ).forEach { fieldsContainer.addView(it) }

                createMacroEditorButton(
                    title = getString(R.string.text_keyboard_layout_macro_swipe_event),
                    previewText = buildMacroPreview(swipeMacroSteps),
                    onClick = {
                        openMacroEditor(macroSwipeStepsData, getString(R.string.text_keyboard_layout_macro_swipe_event)) { newSteps ->
                            val draft = buildDraftKeyData()
                            macroSwipeStepsData = newSteps
                            draft["swipe"] = mapOf("macro" to newSteps)
                            keyData = draft
                            rebuildFields()
                            updateActionButtonState()
                        }
                    }
                ).forEach { fieldsContainer.addView(it) }

            }

            "CapsKey", "CommaKey", "LanguageKey", "ReturnKey" -> {
                val weightEdit = uiBuilder.createEditField(
                    getString(R.string.text_keyboard_layout_key_weight),
                    (keyData["weight"] as? Number)?.toString() ?: ""
                )
                simpleWeightEdit = weightEdit.second
                fieldsContainer.addView(weightEdit.first)
            }

            "BackspaceKey", "SpaceKey" -> {
                val weightEdit = uiBuilder.createEditField(
                    getString(R.string.text_keyboard_layout_key_weight),
                    (keyData["weight"] as? Number)?.toString() ?: ""
                )
                simpleWeightEdit = weightEdit.second
                fieldsContainer.addView(weightEdit.first)
            }
        }

        val swipeKeysExcludingAlphabet = setOf(
            "CapsKey", "CommaKey", "LanguageKey", "SymbolKey", "LayoutSwitchKey", "SpaceKey", "BackspaceKey"
        )
        if (selectedType in swipeKeysExcludingAlphabet || selectedType == "MacroKey") {
            val swipeUpAction = keyData["swipeUp"] as? Map<*, *>
            val swipeDownAction = keyData["swipeDown"] as? Map<*, *>
            val swipeLeftAction = keyData["swipeLeft"] as? Map<*, *>
            val swipeRightAction = keyData["swipeRight"] as? Map<*, *>
            val longPressAction = keyData["longPress"] as? Map<*, *>

            val swipeUpMacroSteps = (swipeUpAction?.get("macro") as? List<*>)?.filterIsInstance<Map<*, *>>() ?: emptyList()
            val swipeDownMacroSteps = (swipeDownAction?.get("macro") as? List<*>)?.filterIsInstance<Map<*, *>>() ?: emptyList()
            val swipeLeftMacroSteps = (swipeLeftAction?.get("macro") as? List<*>)?.filterIsInstance<Map<*, *>>() ?: emptyList()
            val swipeRightMacroSteps = (swipeRightAction?.get("macro") as? List<*>)?.filterIsInstance<Map<*, *>>() ?: emptyList()
            val longPressMacroSteps = (longPressAction?.get("macro") as? List<*>)?.filterIsInstance<Map<*, *>>() ?: emptyList()

            macroSwipeUpStepsData = swipeUpMacroSteps
            macroSwipeDownStepsData = swipeDownMacroSteps
            macroSwipeLeftStepsData = swipeLeftMacroSteps
            macroSwipeRightStepsData = swipeRightMacroSteps
            macroLongPressStepsData = longPressMacroSteps

            val customLabel = TextView(this).apply {
                text = getString(R.string.key_editor_custom_behavior_label)
                textSize = 14f
                setTextColor(styledColor(android.R.attr.colorAccent))
                setPadding(0, dp(16), 0, dp(8))
            }
            fieldsContainer.addView(customLabel)

            val swipeGridLayout = android.widget.TableLayout(this).apply {
                isStretchAllColumns = true
                isShrinkAllColumns = true
            }

            val swipeEvents = listOf(
                Triple(getString(R.string.key_editor_swipe_up_btn), macroSwipeUpStepsData, swipeUpMacroSteps),
                Triple(getString(R.string.key_editor_swipe_down_btn), macroSwipeDownStepsData, swipeDownMacroSteps),
                Triple(getString(R.string.key_editor_swipe_left_btn), macroSwipeLeftStepsData, swipeLeftMacroSteps),
                Triple(getString(R.string.key_editor_swipe_right_btn), macroSwipeRightStepsData, swipeRightMacroSteps),
                Triple(getString(R.string.key_editor_longpress_btn), macroLongPressStepsData, longPressMacroSteps)
            )

            val rowCount = (swipeEvents.size + 1) / 2
            for (row in 0 until rowCount) {
                val tableRow = android.widget.TableRow(this).apply {
                    layoutParams = android.widget.TableRow.LayoutParams(
                        android.widget.TableRow.LayoutParams.MATCH_PARENT,
                        android.widget.TableRow.LayoutParams.WRAP_CONTENT
                    ).apply {
                        bottomMargin = dp(4)
                    }
                }

                for (col in 0 until 2) {
                    val index = row * 2 + col
                    if (index < swipeEvents.size) {
                        val (title, stepsData, macroSteps) = swipeEvents[index]
                        val cellView = createCompactMacroButton(title, stepsData) {
                            val eventTitle = when (index) {
                                0 -> getString(R.string.key_editor_swipe_up_event)
                                1 -> getString(R.string.key_editor_swipe_down_event)
                                2 -> getString(R.string.key_editor_swipe_left_event)
                                3 -> getString(R.string.key_editor_swipe_right_event)
                                else -> getString(R.string.key_editor_longpress_event)
                            }
                            openMacroEditor(stepsData, eventTitle) { newSteps ->
                                val draft = buildDraftKeyData()
                                when (index) {
                                    0 -> { macroSwipeUpStepsData = newSteps; draft["swipeUp"] = mapOf("macro" to newSteps) }
                                    1 -> { macroSwipeDownStepsData = newSteps; draft["swipeDown"] = mapOf("macro" to newSteps) }
                                    2 -> { macroSwipeLeftStepsData = newSteps; draft["swipeLeft"] = mapOf("macro" to newSteps) }
                                    3 -> { macroSwipeRightStepsData = newSteps; draft["swipeRight"] = mapOf("macro" to newSteps) }
                                    4 -> { macroLongPressStepsData = newSteps; draft["longPress"] = mapOf("macro" to newSteps) }
                                }
                                keyData = draft
                                rebuildFields()
                                updateActionButtonState()
                            }
                        }
                        tableRow.addView(cellView)
                    } else {
                        val emptyView = android.view.View(this).apply {
                            layoutParams = android.widget.TableRow.LayoutParams(
                                0,
                                android.widget.TableRow.LayoutParams.WRAP_CONTENT,
                                1f
                            )
                        }
                        tableRow.addView(emptyView)
                    }
                }
                swipeGridLayout.addView(tableRow)
            }
            fieldsContainer.addView(swipeGridLayout)
        }

        renderColorEditors()
        attachFieldWatchers(fieldsContainer)
        updateActionButtonState()
    }

    private fun renderColorEditors() {
        val theme = ThemeManager.activeTheme
        availableColorFields().forEach { field ->
            val title = getString(field.labelRes)
            val customColor = parseColorInt(keyData[field.customKey])
            val monetName = keyData[field.monetKey] as? String
            val modeText = when {
                monetName != null -> formatMonetResourceName(monetName)
                customColor != null -> formatAndroidColorCode(customColor)
                else -> getString(R.string.text_keyboard_layout_key_color_mode_theme)
            }
            val resolvedColor = resolveMonetColor(monetName) ?: customColor ?: field.themeColorGetter(theme)

            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(8), 0, dp(8))
                background = resources.getDrawable(android.R.drawable.list_selector_background, null)
                setOnClickListener { showColorFieldOptions(field) }
                isClickable = true
                isFocusable = true

                addView(
                    TextView(this@KeyEditorActivity).apply {
                        text = title
                        textSize = 13f
                        setTextColor(styledColor(android.R.attr.textColorSecondary))
                    },
                    LinearLayout.LayoutParams(dp(96), LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                        rightMargin = dp(8)
                    }
                )

                addView(
                    LinearLayout(this@KeyEditorActivity).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.END or Gravity.CENTER_VERTICAL

                        addView(
                            TextView(this@KeyEditorActivity).apply {
                                text = modeText
                                textSize = 13f
                                setTextColor(styledColor(android.R.attr.textColorPrimary))
                            },
                            LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            ).apply {
                                rightMargin = dp(8)
                            }
                        )

                        addView(
                            View(this@KeyEditorActivity).apply {
                                background = android.graphics.drawable.GradientDrawable().apply {
                                    shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                                    cornerRadius = dp(4).toFloat()
                                    setColor(resolvedColor)
                                }
                            },
                            LinearLayout.LayoutParams(dp(20), dp(20))
                        )
                    },
                    LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                        weight = 1f
                    }
                )
            }

            fieldsContainer.addView(
                row,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            )
        }
    }

    private fun showColorFieldOptions(field: EditableColorField) {
        val supportsMonet = ThemeMonet.supportsCustomMappingEditor(this)
        val options = mutableListOf(
            getString(R.string.text_keyboard_layout_key_color_mode_theme),
            getString(R.string.text_keyboard_layout_key_color_mode_custom)
        )
        if (supportsMonet) {
            options += getString(R.string.text_keyboard_layout_key_color_mode_monet_pick)
        }
        AlertDialog.Builder(this)
            .setTitle(field.labelRes)
            .setItems(options.toTypedArray()) { _, which ->
                when (options[which]) {
                    getString(R.string.text_keyboard_layout_key_color_mode_theme) -> {
                        keyData.remove(field.customKey)
                        keyData.remove(field.monetKey)
                        rebuildFields()
                        updateActionButtonState()
                    }
                    getString(R.string.text_keyboard_layout_key_color_mode_custom) -> {
                        openCustomColorEditor(field)
                    }
                    getString(R.string.text_keyboard_layout_key_color_mode_monet_pick) -> {
                        openMonetColorPicker(field)
                    }
                }
            }
            .show()
    }

    private fun openCustomColorEditor(field: EditableColorField) {
        val theme = ThemeManager.activeTheme
        val current = parseColorInt(keyData[field.customKey]) ?: field.themeColorGetter(theme)
        if (DeviceUtil.isHMOS) {
            colorEditorLauncher.launch(
                ThemeColorEditorActivity.EditorInput(
                    fieldName = field.customKey,
                    titleRes = field.labelRes,
                    initialColor = current
                )
            )
            return
        }
        colorEditorLauncher.launch(
            ThemeColorEditorActivity.EditorInput(
                fieldName = field.customKey,
                titleRes = field.labelRes,
                initialColor = current
            )
        )
    }

    private fun openMonetColorPicker(field: EditableColorField) {
        if (!ThemeMonet.supportsCustomMappingEditor(this)) {
            Toast.makeText(this, R.string.text_keyboard_layout_key_color_monet_unavailable, Toast.LENGTH_SHORT).show()
            return
        }
        val available = SystemColorResourceId.getAvailableForSdk(Build.VERSION.SDK_INT)
        val current = (keyData[field.monetKey] as? String)?.let(SystemColorResourceId::fromResourceName)
            ?: available.firstOrNull()
            ?: return
        SystemColorResourcePickerDialog.show(
            this,
            current,
            object : SystemColorResourcePickerDialog.OnColorResourceSelectedListener {
                override fun onColorResourceSelected(resourceId: SystemColorResourceId) {
                    keyData[field.monetKey] = resourceId.resourceId
                    keyData.remove(field.customKey)
                    rebuildFields()
                    updateActionButtonState()
                }
            }
        )
    }

    private fun resolveMonetColor(resourceName: String?): Int? {
        val name = resourceName?.takeIf { it.isNotBlank() } ?: return null
        val colorResId = resources.getIdentifier(name, "color", "android")
        if (colorResId == 0) return null
        return runCatching { getColor(colorResId) }.getOrNull()
    }

    private fun formatMonetResourceName(resourceName: String): String {
        return resourceName.removePrefix("system_").replace("_", " ")
    }

    private fun formatAndroidColorCode(color: Int): String {
        return String.format("#%08X", color)
    }

    private fun parseColorInt(value: Any?): Int? {
        return when (value) {
            is Int -> value
            is Number -> value.toInt()
            is String -> {
                val raw = value.trim()
                when {
                    raw.startsWith("#") -> raw.removePrefix("#").toLongOrNull(16)?.toInt()
                    raw.startsWith("0x", ignoreCase = true) -> raw.removePrefix("0x").toLongOrNull(16)?.toInt()
                    else -> raw.toLongOrNull()?.toInt()
                }
            }
            else -> null
        }
    }

    private fun attachFieldWatchers(root: android.view.View) {
        when (root) {
            is EditText -> {
                root.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                        updateActionButtonState()
                    }
                    override fun afterTextChanged(s: Editable?) {}
                })
            }
            is android.view.ViewGroup -> {
                for (i in 0 until root.childCount) {
                    attachFieldWatchers(root.getChildAt(i))
                }
            }
        }
    }

    private fun updateActionButtonState() {
        val changed = hasChanges()
        saveMenuItem?.isEnabled = changed
        saveMenuItem?.title = getString(R.string.save)
        saveMenuItem?.icon?.alpha = if (changed) 255 else 120
    }

    private fun hasChanges(): Boolean {
        return snapshotOf(buildDraftKeyData()) != baselineKeySnapshot
    }

    private fun buildDraftKeyData(): MutableMap<String, Any?> {
        val draft = mutableMapOf<String, Any?>()
        draft["type"] = selectedType

        when (selectedType) {
            "AlphabetKey" -> {
                val main = alphabetMainEdit?.text?.toString().orEmpty()
                val alt = alphabetAltEdit?.text?.toString().orEmpty()
                val hint = alphabetHintTextEdit?.text?.toString().orEmpty()
                if (main.isNotEmpty()) draft["main"] = main
                if (alt.isNotEmpty()) draft["alt"] = alt
                if (hint.isNotEmpty()) draft["hint"] = hint
                parseWeight(alphabetWeightEdit?.text?.toString())?.let { draft["weight"] = it }

                if (alphabetDisplayTextModeSpecific) {
                    val displayTextMap = mutableMapOf<String, String>()
                    alphabetDisplayTextRowBindings.forEach { binding ->
                        val modeName = binding.modeEdit.text?.toString()?.trim().orEmpty()
                        val modeValue = binding.valueEdit.text?.toString()?.trim().orEmpty()
                        if (modeName.isNotEmpty() && modeValue.isNotEmpty()) {
                            displayTextMap[modeName] = modeValue
                        }
                    }
                    if (displayTextMap.isNotEmpty()) {
                        draft["displayText"] = displayTextMap
                    }
                } else {
                    val displayText = alphabetDisplayTextSimpleEdit?.text?.toString()?.trim()
                        ?: alphabetDisplayTextSimpleValue.trim()
                    if (displayText.isNotEmpty()) {
                        draft["displayText"] = displayText
                    }
                }

                if (macroSwipeUpStepsData.isNotEmpty()) draft["swipeUp"] = mapOf("macro" to macroSwipeUpStepsData)
                if (macroSwipeDownStepsData.isNotEmpty()) draft["swipeDown"] = mapOf("macro" to macroSwipeDownStepsData)
                if (macroSwipeLeftStepsData.isNotEmpty()) draft["swipeLeft"] = mapOf("macro" to macroSwipeLeftStepsData)
                if (macroSwipeRightStepsData.isNotEmpty()) draft["swipeRight"] = mapOf("macro" to macroSwipeRightStepsData)
                if (macroLongPressStepsData.isNotEmpty()) draft["longPress"] = mapOf("macro" to macroLongPressStepsData)

                val swipeUpPopup = alphabetSwipeUpPopupEdit?.text?.toString().orEmpty()
                val swipeDownPopup = alphabetSwipeDownPopupEdit?.text?.toString().orEmpty()
                val swipeLeftPopup = alphabetSwipeLeftPopupEdit?.text?.toString().orEmpty()
                val swipeRightPopup = alphabetSwipeRightPopupEdit?.text?.toString().orEmpty()
                val longPressPopup = alphabetLongPressPopupEdit?.text?.toString().orEmpty()
                if (swipeUpPopup.isNotEmpty()) draft["swipeUpPopup"] = swipeUpPopup
                if (swipeDownPopup.isNotEmpty()) draft["swipeDownPopup"] = swipeDownPopup
                if (swipeLeftPopup.isNotEmpty()) draft["swipeLeftPopup"] = swipeLeftPopup
                if (swipeRightPopup.isNotEmpty()) draft["swipeRightPopup"] = swipeRightPopup
                if (longPressPopup.isNotEmpty()) draft["longPressPopup"] = longPressPopup
                draft["swipeUpPopupEnabled"] = alphabetSwipeUpPopupEnabledSwitch?.isChecked ?: true
                draft["swipeDownPopupEnabled"] = alphabetSwipeDownPopupEnabledSwitch?.isChecked ?: true
                draft["swipeLeftPopupEnabled"] = alphabetSwipeLeftPopupEnabledSwitch?.isChecked ?: true
                draft["swipeRightPopupEnabled"] = alphabetSwipeRightPopupEnabledSwitch?.isChecked ?: true
                draft["longPressPopupEnabled"] = alphabetLongPressPopupEnabledSwitch?.isChecked ?: true
            }

            "LayoutSwitchKey" -> {
                val label = layoutSwitchLabelEdit?.text?.toString()?.ifEmpty { "?123" }.orEmpty()
                if (label.isNotEmpty()) draft["label"] = label
                val subLabel = layoutSwitchSubLabelEdit?.text?.toString().orEmpty()
                if (subLabel.isNotEmpty()) draft["subLabel"] = subLabel
                parseWeight(layoutSwitchWeightEdit?.text?.toString())?.let { draft["weight"] = it }
            }

            "SymbolKey" -> {
                val label = symbolLabelEdit?.text?.toString()?.ifEmpty { "." }.orEmpty()
                if (label.isNotEmpty()) draft["label"] = label
                parseWeight(symbolWeightEdit?.text?.toString())?.let { draft["weight"] = it }
            }

            "MacroKey" -> {
                val baseLabel = macroLabelEdit?.text?.toString().orEmpty()
                if (baseLabel.isNotEmpty()) draft["label"] = baseLabel
                val altLabel = macroAltLabelEdit?.text?.toString().orEmpty()
                if (altLabel.isNotEmpty()) draft["altLabel"] = altLabel
                val longPressLabel = macroLongPressLabelEdit?.text?.toString().orEmpty()
                if (longPressLabel.isNotEmpty()) draft["longPressLabel"] = longPressLabel
                parseWeight(macroWeightEdit?.text?.toString())?.let { draft["weight"] = it }

                if (macroDisplayTextModeSpecific) {
                    val displayTextMap = mutableMapOf<String, String>()
                    macroDisplayTextRowBindings.forEach { binding ->
                        val modeName = binding.modeEdit.text?.toString()?.trim().orEmpty()
                        val modeValue = binding.valueEdit.text?.toString()?.trim().orEmpty()
                        if (modeName.isNotEmpty() && modeValue.isNotEmpty()) {
                            displayTextMap[modeName] = modeValue
                        }
                    }
                    if (displayTextMap.isNotEmpty()) {
                        draft["displayText"] = displayTextMap
                    }
                } else {
                    val displayText = macroDisplayTextSimpleEdit?.text?.toString()?.trim()
                        ?: macroDisplayTextSimpleValue.trim()
                    if (displayText.isNotEmpty()) {
                        draft["displayText"] = displayText
                    }
                }

                if (macroTapStepsData.isNotEmpty()) {
                    draft["tap"] = mapOf("macro" to macroTapStepsData)
                }

                if (macroSwipeStepsData.isNotEmpty()) {
                    draft["swipe"] = mapOf("macro" to macroSwipeStepsData)
                }

            }
        }

        if (macroSwipeUpStepsData.isNotEmpty()) {
            draft["swipeUp"] = mapOf("macro" to macroSwipeUpStepsData)
        } else {
            draft["swipeUp"] = null
        }
        if (macroSwipeDownStepsData.isNotEmpty()) {
            draft["swipeDown"] = mapOf("macro" to macroSwipeDownStepsData)
        } else {
            draft["swipeDown"] = null
        }
        if (macroSwipeLeftStepsData.isNotEmpty()) {
            draft["swipeLeft"] = mapOf("macro" to macroSwipeLeftStepsData)
        } else {
            draft["swipeLeft"] = null
        }
        if (macroSwipeRightStepsData.isNotEmpty()) {
            draft["swipeRight"] = mapOf("macro" to macroSwipeRightStepsData)
        } else {
            draft["swipeRight"] = null
        }
        if (macroLongPressStepsData.isNotEmpty()) {
            draft["longPress"] = mapOf("macro" to macroLongPressStepsData)
        } else {
            draft["longPress"] = null
        }

        if (selectedType in listOf("CapsKey", "CommaKey", "LanguageKey", "SpaceKey", "ReturnKey", "BackspaceKey")) {
            parseWeight(simpleWeightEdit?.text?.toString())?.let { draft["weight"] = it }
        }

        appendColorOverrides(draft)

        return draft
    }

    private fun snapshotOf(value: Any?): String {
        val normalized = normalizeForSnapshot(value)
        return normalized.toString()
    }

    private fun normalizeForSnapshot(value: Any?): Any? {
        return when (value) {
            is Map<*, *> -> {
                val keys = value.keys.mapNotNull { it?.toString() }.sorted()
                keys.joinToString(prefix = "{", postfix = "}") { key ->
                    "$key=${normalizeForSnapshot(value[key])}"
                }
            }
            is List<*> -> value.map { normalizeForSnapshot(it) }
            is Float -> String.format(java.util.Locale.US, "%.6f", value)
            is Double -> String.format(java.util.Locale.US, "%.6f", value)
            is Array<*> -> Arrays.toString(value)
            else -> value
        }
    }

    private fun createMacroEditorButton(
        title: String,
        previewText: String,
        onClick: () -> Unit
    ): List<android.view.View> {
        val button = TextView(this).apply {
            text = title
            textSize = 14f
            setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
            minWidth = dp(120)
            setPadding(dp(12), dp(8), dp(12), dp(8))
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(styledColor(android.R.attr.colorButtonNormal))
                setStroke(dp(1), styledColor(android.R.attr.colorControlNormal))
                cornerRadius = dp(4).toFloat()
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                topMargin = dp(8)
            }
            setOnClickListener { onClick() }
        }

        val preview = TextView(this).apply {
            text = previewText
            textSize = 12f
            setTextColor(styledColor(android.R.attr.textColorSecondary))
            setPadding(dp(8), dp(8), dp(8), dp(8))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(4)
            }
        }

        return listOf(button, preview)
    }

    private fun createCompactMacroButton(
        title: String,
        macroSteps: List<Map<*, *>>,
        onClick: () -> Unit
    ): android.view.View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(styledColor(android.R.attr.colorButtonNormal))
                setStroke(dp(1), styledColor(android.R.attr.colorControlNormal))
                cornerRadius = dp(4).toFloat()
            }
            setPadding(dp(12), dp(8), dp(12), dp(8))
            layoutParams = android.widget.TableRow.LayoutParams(
                0,
                android.widget.TableRow.LayoutParams.WRAP_CONTENT,
                1f
            ).apply {
                marginEnd = dp(4)
                bottomMargin = dp(4)
            }
            isClickable = true
            isFocusable = true
            setOnClickListener { onClick() }

            addView(TextView(context).apply {
                text = title
                textSize = 12f
                setTypeface(null, android.graphics.Typeface.BOLD)
                gravity = Gravity.CENTER
            })

            addView(TextView(context).apply {
                text = buildMacroPreview(macroSteps)
                textSize = 10f
                setTextColor(styledColor(android.R.attr.textColorSecondary))
                gravity = Gravity.CENTER
                maxLines = 1
            })
        }
    }

    private fun openMacroEditor(
        initialSteps: List<Map<*, *>>,
        eventType: String,
        callback: (List<Map<*, *>>) -> Unit
    ) {
        val intent = Intent(this, MacroEditorActivity::class.java)
        if (initialSteps.isNotEmpty()) {
            val serializableSteps = ArrayList<Map<*, *>>()
            initialSteps.forEach { step ->
                (step as? Map<*, *>)?.let { serializableSteps.add(it) }
            }
            if (serializableSteps.isNotEmpty()) {
                intent.putExtra(MacroEditorActivity.EXTRA_MACRO_STEPS, serializableSteps)
            }
        }
        intent.putExtra(MacroEditorActivity.EXTRA_EVENT_TYPE, eventType)
        macroEditCallback = callback
        macroEditorLauncher.launch(intent)
    }

    private fun buildMacroPreview(macroSteps: List<*>?): String {
        if (macroSteps == null || macroSteps.isEmpty()) {
            return getString(R.string.text_keyboard_layout_macro_no_event)
        }

        val stepTexts = macroSteps.mapNotNull { step ->
            val stepMap = step as? Map<*, *>
            val type = stepMap?.get("type") as? String ?: return@mapNotNull null
            when (type) {
                "tap", "down", "up" -> {
                    val keys = stepMap["keys"] as? List<*>
                    val keysStr = keys?.mapNotNull { key ->
                        (key as? Map<*, *>)?.let {
                            it["fcitx"] as? String ?: it["android"] as? String
                        }
                    }?.joinToString(", ")
                    if (keysStr.isNullOrBlank()) null else "$type:[$keysStr]"
                }
                "edit" -> {
                    val action = stepMap["action"] as? String ?: return@mapNotNull null
                    "$type:$action"
                }
                "shortcut" -> {
                    val modifiers = (stepMap["modifiers"] as? List<*>)?.mapNotNull {
                        (it as? Map<*, *>)?.let { m ->
                            m["fcitx"] as? String ?: m["android"] as? String
                        }
                    } ?: emptyList()
                    val key = (stepMap["key"] as? Map<*, *>)?.let {
                        it["fcitx"] as? String ?: it["android"] as? String
                    } ?: return@mapNotNull null
                    "$type:${modifiers.joinToString("+")}+$key"
                }
                "text" -> {
                    val text = stepMap["text"] as? String ?: return@mapNotNull null
                    val displayText = if (text.length > 10) "${text.take(10)}..." else text
                    "$type:\"$displayText\""
                }
                else -> type
            }
        }

        return if (stepTexts.size <= 3) {
            stepTexts.joinToString(" -> ")
        } else {
            getString(R.string.text_keyboard_layout_macro_event_preview, stepTexts.take(2).joinToString(" -> "), stepTexts.size)
        }
    }

    private fun saveAndFinish() {
        val result = validateAndSave(
            selectedType,
            alphabetMainEdit,
            alphabetAltEdit,
            alphabetWeightEdit,
            alphabetDisplayTextModeSpecific,
            alphabetDisplayTextSimpleEdit,
            alphabetDisplayTextSimpleValue,
            alphabetDisplayTextModeItems,
            alphabetDisplayTextRowBindings,
            layoutSwitchLabelEdit,
            layoutSwitchSubLabelEdit,
            layoutSwitchWeightEdit,
            symbolLabelEdit,
            symbolWeightEdit,
            macroLabelEdit,
            macroDisplayTextModeSpecific,
            macroDisplayTextSimpleEdit,
            macroDisplayTextSimpleValue,
            macroDisplayTextModeItems,
            macroDisplayTextRowBindings,
            macroAltLabelEdit,
            macroLongPressLabelEdit,
            macroWeightEdit,
            simpleWeightEdit
        ) ?: return

        result.remove("__global_backspaceSwipeDeleteAll")
        result.remove("__global_spaceSwipeMoveCursor")
        result.remove("__global_spaceSwipeUpBehavior")
        result.remove("__global_spaceLongPressBehavior")
        
        val kbdPrefs = org.fcitx.fcitx5.android.data.prefs.AppPrefs.getInstance().keyboard
        pendingBackspaceSwipeDeleteAll?.let { kbdPrefs.backspaceSwipeDeleteAll.setValue(it) }
        pendingSpaceSwipeMoveCursor?.let { kbdPrefs.spaceSwipeMoveCursor.setValue(it) }
        pendingSpaceSwipeUpBehavior?.let { kbdPrefs.spaceSwipeUpBehavior.setValue(it) }
        pendingSpaceLongPressBehavior?.let { kbdPrefs.spaceKeyLongPressBehavior.setValue(it) }

        val data = Intent().apply {
            putExtra(EXTRA_RESULT_ACTION, RESULT_ACTION_SAVE)
            putExtra(EXTRA_RESULT_KEY_DATA, toSerializableMap(result))
            putExtra(EXTRA_ROW_INDEX, rowIndex)
            keyIndex?.let { putExtra(EXTRA_KEY_INDEX, it) }
        }
        setResult(RESULT_OK, data)
        finish()
    }

    private fun confirmDelete() {
        AlertDialog.Builder(this)
            .setTitle(R.string.delete)
            .setMessage(R.string.text_keyboard_layout_delete_key_confirm)
            .setPositiveButton(R.string.delete) { _, _ ->
                val data = Intent().apply {
                    putExtra(EXTRA_RESULT_ACTION, RESULT_ACTION_DELETE)
                    putExtra(EXTRA_ROW_INDEX, rowIndex)
                    keyIndex?.let { putExtra(EXTRA_KEY_INDEX, it) }
                }
                setResult(RESULT_OK, data)
                finish()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun initDisplayText(
        keyData: Map<String, Any?>,
        isEditingSubModeLayout: Boolean,
        currentSubModeLabel: String?,
        labelTextKey: String = "displayText",
        callback: (Boolean, String, MutableList<KeyboardEditorUiBuilder.DisplayTextItem>) -> Unit
    ) {
        val displayTextData = keyData[labelTextKey]
        val displayTextMap = when (displayTextData) {
            is JsonObject -> displayTextData.mapValues { entry ->
                LayoutJsonUtils.toAny(entry.value)
            }
            is Map<*, *> -> displayTextData
            else -> null
        }

        if (displayTextMap != null && displayTextMap.isNotEmpty()) {
            if (isEditingSubModeLayout && currentSubModeLabel != null) {
                val specificValue = displayTextMap[currentSubModeLabel]?.toString()
                val defaultValue = displayTextMap["default"]?.toString()
                    ?: displayTextMap[""]?.toString()
                callback(false, specificValue ?: defaultValue ?: "", mutableListOf())
            } else {
                val items = mutableListOf<KeyboardEditorUiBuilder.DisplayTextItem>()
                displayTextMap.forEach { (k, v) ->
                    items.add(KeyboardEditorUiBuilder.DisplayTextItem(k?.toString().orEmpty(), v?.toString().orEmpty()))
                }
                callback(true, "", items)
            }
        } else {
            val labelTextSimple = keyData[labelTextKey] as? String
            if (!labelTextSimple.isNullOrBlank()) {
                callback(false, labelTextSimple, mutableListOf())
            } else {
                callback(false, "", mutableListOf())
            }
        }
    }

    private fun validateAndSave(
        selectedType: String,
        alphabetMainEdit: EditText?,
        alphabetAltEdit: EditText?,
        alphabetWeightEdit: EditText?,
        alphabetDisplayTextModeSpecific: Boolean,
        alphabetDisplayTextSimpleEdit: EditText?,
        alphabetDisplayTextSimpleValue: String,
        alphabetDisplayTextModeItems: List<KeyboardEditorUiBuilder.DisplayTextItem>,
        alphabetDisplayTextRowBindings: List<KeyboardEditorUiBuilder.DisplayTextRowBinding>,
        layoutSwitchLabelEdit: EditText?,
        layoutSwitchSubLabelEdit: EditText?,
        layoutSwitchWeightEdit: EditText?,
        symbolLabelEdit: EditText?,
        symbolWeightEdit: EditText?,
        macroLabelEdit: EditText?,
        macroDisplayTextModeSpecific: Boolean,
        macroDisplayTextSimpleEdit: EditText?,
        macroDisplayTextSimpleValue: String,
        macroDisplayTextModeItems: List<KeyboardEditorUiBuilder.DisplayTextItem>,
        macroDisplayTextRowBindings: List<KeyboardEditorUiBuilder.DisplayTextRowBinding>,
        macroAltLabelEdit: EditText?,
        macroLongPressLabelEdit: EditText?,
        macroWeightEdit: EditText?,
        simpleWeightEdit: EditText?
    ): MutableMap<String, Any?>? {
        if (selectedType == "AlphabetKey") {
            val main = alphabetMainEdit?.text?.toString()?.trim().orEmpty()
            val alt = alphabetAltEdit?.text?.toString()?.trim().orEmpty()

            if (main.isEmpty()) {
                Toast.makeText(this, R.string.text_keyboard_layout_alphabet_key_main_required, Toast.LENGTH_SHORT).show()
                return null
            }
            if (alt.isEmpty()) {
                Toast.makeText(this, R.string.text_keyboard_layout_alphabet_key_alt_required, Toast.LENGTH_SHORT).show()
                return null
            }
            if (main.length != 1) {
                Toast.makeText(this, getString(R.string.text_keyboard_layout_alphabet_key_main_length_invalid), Toast.LENGTH_SHORT).show()
                return null
            }
            if (alt.length != 1) {
                Toast.makeText(this, getString(R.string.text_keyboard_layout_alphabet_key_alt_length_invalid), Toast.LENGTH_SHORT).show()
                return null
            }
        }

        if (selectedType == "MacroKey") {
            val label = macroLabelEdit?.text?.toString()?.trim().orEmpty()
            if (label.isEmpty()) {
                Toast.makeText(this, R.string.text_keyboard_layout_macro_key_label_required, Toast.LENGTH_SHORT).show()
                return null
            }
            val longPressLabel = macroLongPressLabelEdit?.text?.toString()?.trim().orEmpty()
            if (macroLongPressStepsData.isNotEmpty() && longPressLabel.isEmpty()) {
                Toast.makeText(this, R.string.text_keyboard_layout_longpress_label_fallback_notice, Toast.LENGTH_SHORT).show()
            }
        }

        val newKey = mutableMapOf<String, Any?>()
        newKey["type"] = selectedType

        when (selectedType) {
            "AlphabetKey" -> {
                newKey["main"] = alphabetMainEdit?.text?.toString().orEmpty()
                newKey["alt"] = alphabetAltEdit?.text?.toString().orEmpty()
                val hint = alphabetHintTextEdit?.text?.toString().orEmpty()
                if (hint.isNotEmpty()) newKey["hint"] = hint
                parseWeight(alphabetWeightEdit?.text?.toString())?.let { newKey["weight"] = it }

                val swipeUpPopup = alphabetSwipeUpPopupEdit?.text?.toString().orEmpty()
                val swipeDownPopup = alphabetSwipeDownPopupEdit?.text?.toString().orEmpty()
                val swipeLeftPopup = alphabetSwipeLeftPopupEdit?.text?.toString().orEmpty()
                val swipeRightPopup = alphabetSwipeRightPopupEdit?.text?.toString().orEmpty()
                val longPressPopup = alphabetLongPressPopupEdit?.text?.toString().orEmpty()
                if (swipeUpPopup.isNotEmpty()) newKey["swipeUpPopup"] = swipeUpPopup
                if (swipeDownPopup.isNotEmpty()) newKey["swipeDownPopup"] = swipeDownPopup
                if (swipeLeftPopup.isNotEmpty()) newKey["swipeLeftPopup"] = swipeLeftPopup
                if (swipeRightPopup.isNotEmpty()) newKey["swipeRightPopup"] = swipeRightPopup
                if (longPressPopup.isNotEmpty()) newKey["longPressPopup"] = longPressPopup
                newKey["swipeUpPopupEnabled"] = alphabetSwipeUpPopupEnabledSwitch?.isChecked ?: true
                newKey["swipeDownPopupEnabled"] = alphabetSwipeDownPopupEnabledSwitch?.isChecked ?: true
                newKey["swipeLeftPopupEnabled"] = alphabetSwipeLeftPopupEnabledSwitch?.isChecked ?: true
                newKey["swipeRightPopupEnabled"] = alphabetSwipeRightPopupEnabledSwitch?.isChecked ?: true
                newKey["longPressPopupEnabled"] = alphabetLongPressPopupEnabledSwitch?.isChecked ?: true

                if (alphabetDisplayTextModeSpecific) {
                    val displayTextMap = mutableMapOf<String, String>()
                    alphabetDisplayTextRowBindings.forEach { binding ->
                        val modeName = binding.modeEdit.text?.toString()?.trim().orEmpty()
                        val modeValue = binding.valueEdit.text?.toString()?.trim().orEmpty()
                        if (modeName.isNotEmpty() && modeValue.isNotEmpty()) {
                            displayTextMap[modeName] = modeValue
                        }
                    }
                    if (displayTextMap.isNotEmpty()) {
                        newKey["displayText"] = displayTextMap
                    }
                } else {
                    val displayText = alphabetDisplayTextSimpleEdit?.text?.toString()?.trim()
                        ?: alphabetDisplayTextSimpleValue.trim()
                    if (displayText.isNotEmpty()) {
                        newKey["displayText"] = displayText
                    }
                }
            }

            "LayoutSwitchKey" -> {
                newKey["label"] = layoutSwitchLabelEdit?.text?.toString()?.ifEmpty { "?123" }.orEmpty()
                val subLabel = layoutSwitchSubLabelEdit?.text?.toString().orEmpty()
                if (subLabel.isNotEmpty()) newKey["subLabel"] = subLabel
                parseWeight(layoutSwitchWeightEdit?.text?.toString())?.let { newKey["weight"] = it }
            }

            "SymbolKey" -> {
                newKey["label"] = symbolLabelEdit?.text?.toString()?.ifEmpty { "." }.orEmpty()
                parseWeight(symbolWeightEdit?.text?.toString())?.let { newKey["weight"] = it }
            }

            "MacroKey" -> {
                val baseLabel = macroLabelEdit?.text?.toString().orEmpty()
                newKey["label"] = baseLabel
                val altLabel = macroAltLabelEdit?.text?.toString().orEmpty()
                if (altLabel.isNotEmpty()) newKey["altLabel"] = altLabel
                val longPressLabel = macroLongPressLabelEdit?.text?.toString().orEmpty()
                if (longPressLabel.isNotEmpty()) newKey["longPressLabel"] = longPressLabel
                parseWeight(macroWeightEdit?.text?.toString())?.let { newKey["weight"] = it }

                if (macroDisplayTextModeSpecific) {
                    val displayTextMap = mutableMapOf<String, String>()
                    macroDisplayTextRowBindings.forEach { binding ->
                        val modeName = binding.modeEdit.text?.toString()?.trim().orEmpty()
                        val modeValue = binding.valueEdit.text?.toString()?.trim().orEmpty()
                        if (modeName.isNotEmpty() && modeValue.isNotEmpty()) {
                            displayTextMap[modeName] = modeValue
                        }
                    }
                    if (displayTextMap.isNotEmpty()) {
                        newKey["displayText"] = displayTextMap
                    }
                } else {
                    val displayText = macroDisplayTextSimpleEdit?.text?.toString()?.trim()
                        ?: macroDisplayTextSimpleValue.trim()
                    if (displayText.isNotEmpty()) {
                        newKey["displayText"] = displayText
                    }
                }

                if (macroTapStepsData.isNotEmpty()) {
                    newKey["tap"] = mapOf("macro" to macroTapStepsData)
                } else {
                    newKey["tap"] = mapOf(
                        "macro" to listOf(
                            mapOf("type" to "text", "text" to "")
                        )
                    )
                }

                if (macroSwipeStepsData.isNotEmpty()) {
                    newKey["swipe"] = mapOf("macro" to macroSwipeStepsData)
                }

            }
        }

        if (macroSwipeUpStepsData.isNotEmpty()) {
            newKey["swipeUp"] = mapOf("macro" to macroSwipeUpStepsData)
        }
        if (macroSwipeDownStepsData.isNotEmpty()) {
            newKey["swipeDown"] = mapOf("macro" to macroSwipeDownStepsData)
        }
        if (macroSwipeLeftStepsData.isNotEmpty()) {
            newKey["swipeLeft"] = mapOf("macro" to macroSwipeLeftStepsData)
        }
        if (macroSwipeRightStepsData.isNotEmpty()) {
            newKey["swipeRight"] = mapOf("macro" to macroSwipeRightStepsData)
        }
        if (macroLongPressStepsData.isNotEmpty()) {
            newKey["longPress"] = mapOf("macro" to macroLongPressStepsData)
        }

        if (selectedType in listOf("CapsKey", "CommaKey", "LanguageKey", "SpaceKey", "ReturnKey", "BackspaceKey")) {
            parseWeight(simpleWeightEdit?.text?.toString())?.let { newKey["weight"] = it }
        }

        appendColorOverrides(newKey)

        return newKey
    }

    private fun appendColorOverrides(target: MutableMap<String, Any?>) {
        availableColorFields().forEach { field ->
            val monet = (keyData[field.monetKey] as? String)?.takeIf { it.isNotBlank() }
            if (monet != null) {
                target[field.monetKey] = monet
            } else {
                parseColorInt(keyData[field.customKey])?.let { target[field.customKey] = it }
            }
        }
    }

    private fun availableColorFields(): List<EditableColorField> {
        return editableColorFields.filter { field ->
            field.supportedTypes?.contains(selectedType) ?: true
        }
    }

    private fun parseWeight(text: String?): Float? {
        val weight = text?.toFloatOrNull()
        return weight?.takeIf { it in 0.0f..1.0f }
    }

    companion object {
        private const val MENU_SAVE_ID = 5001
        private const val MENU_DELETE_ID = 5002

        const val EXTRA_KEY_DATA = "key_data"
        const val EXTRA_ROW_INDEX = "row_index"
        const val EXTRA_KEY_INDEX = "key_index"
        const val EXTRA_IS_EDITING_SUBMODE_LAYOUT = "is_editing_submode_layout"
        const val EXTRA_CURRENT_SUBMODE_LABEL = "current_submode_label"
        const val EXTRA_HAS_MULTI_SUBMODE_SUPPORT = "has_multi_submode_support"

        const val EXTRA_RESULT_ACTION = "result_action"
        const val EXTRA_RESULT_KEY_DATA = "result_key_data"

        const val RESULT_ACTION_SAVE = "save"
        const val RESULT_ACTION_DELETE = "delete"

        @JvmStatic
        @Suppress("UNCHECKED_CAST")
        fun toSerializableMap(input: Map<String, Any?>): LinkedHashMap<String, Any?> {
            return normalizeValue(input) as LinkedHashMap<String, Any?>
        }

        private fun normalizeValue(value: Any?): Any? {
            return when (value) {
                is Map<*, *> -> {
                    val normalized = LinkedHashMap<String, Any?>()
                    value.forEach { (k, v) ->
                        val key = k?.toString() ?: return@forEach
                        normalized[key] = normalizeValue(v)
                    }
                    normalized
                }
                is List<*> -> {
                    val normalized = ArrayList<Any?>()
                    value.forEach { item -> normalized.add(normalizeValue(item)) }
                    normalized
                }
                is String, is Number, is Boolean, null -> value
                else -> value.toString()
            }
        }

        @Suppress("DEPRECATION")
        private inline fun <reified T : Serializable> serializableExtraCompat(intent: Intent, key: String): T? {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getSerializableExtra(key, T::class.java)
            } else {
                intent.getSerializableExtra(key) as? T
            }
        }
    }
}
