/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.ui.main.settings.behavior

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.input.config.ConfigProviders
import org.fcitx.fcitx5.android.input.config.ConfigProvider
import org.fcitx.fcitx5.android.ui.main.settings.behavior.share.JsonFileQrShareManager
import org.fcitx.fcitx5.android.ui.main.settings.behavior.share.LayoutQrTransferCodec
import org.fcitx.fcitx5.android.ui.main.settings.behavior.share.QrChunkCollector
import splitties.dimensions.dp
import splitties.resources.styledColor
import splitties.views.backgroundColor
import splitties.views.dsl.core.add
import splitties.views.dsl.core.matchParent
import splitties.views.dsl.core.wrapContent
import kotlinx.serialization.json.*
import kotlinx.serialization.encodeToString
import java.io.File

private val prettyJson = Json { prettyPrint = true }

class PopupEditorActivity : AppCompatActivity() {

    private val MP = ViewGroup.LayoutParams.MATCH_PARENT
    private val WC = ViewGroup.LayoutParams.WRAP_CONTENT

    private val toolbar by lazy {
        Toolbar(this).apply {
            backgroundColor = styledColor(android.R.attr.colorPrimary)
            elevation = dp(4f)
        }
    }

    private val searchEdit by lazy {
        EditText(this).apply {
            hint = getString(R.string.popup_editor_search_hint)
            textSize = 14f
            setPadding(dp(12), dp(8), dp(12), dp(8))
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(styledColor(android.R.attr.colorBackgroundFloating))
                setStroke(dp(1), styledColor(android.R.attr.colorControlNormal))
                cornerRadius = dp(8).toFloat()
            }
            setSingleLine(true)
        }
    }

    private val recyclerView by lazy {
        RecyclerView(this).apply {
            layoutManager = LinearLayoutManager(this@PopupEditorActivity)
            isNestedScrollingEnabled = false
            clipToPadding = false
            setPadding(0, 0, 0, dp(80))
        }
    }

    private val fabAdd by lazy {
        TextView(this).apply {
            text = "+"
            textSize = 24f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(android.graphics.Color.WHITE)
            gravity = android.view.Gravity.CENTER
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(styledColor(android.R.attr.colorPrimary))
                cornerRadius = dp(28).toFloat()
            }
            elevation = dp(6f)
            setOnClickListener { openEditor(null) }
        }
    }

    private val usageHint by lazy {
        TextView(this).apply {
            text = getString(R.string.popup_editor_hint)
            textSize = 12f
            setTextColor(styledColor(android.R.attr.textColorSecondary))
            setPadding(dp(4), dp(4), dp(4), dp(8))
        }
    }

    private val contentContainer by lazy {
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val pad = dp(12)
            setPadding(pad, pad, pad, 0)
            addView(searchEdit, LinearLayout.LayoutParams(MP, WC).apply {
                bottomMargin = dp(8)
            })
            addView(usageHint)
        }
    }

    private val mainContainer by lazy {
        android.widget.FrameLayout(this).apply {
            addView(contentContainer, android.widget.FrameLayout.LayoutParams(MP, WC).apply {
                topMargin = 0
            })
            addView(recyclerView, android.widget.FrameLayout.LayoutParams(MP, MP).apply {
                topMargin = dp(80)
            })
            addView(fabAdd, android.widget.FrameLayout.LayoutParams(dp(56), dp(56)).apply {
                gravity = android.view.Gravity.END or android.view.Gravity.BOTTOM
                marginEnd = dp(16)
                bottomMargin = dp(16)
            })
        }
    }

    private val ui by lazy {
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(
                toolbar,
                LinearLayout.LayoutParams(MP, WC)
            )
            addView(
                mainContainer,
                LinearLayout.LayoutParams(MP, 0).apply { weight = 1f }
            )
        }
    }

    private val provider: ConfigProvider = ConfigProviders.provider

    private val popupFile: File? by lazy { provider.popupPresetFile() }

    private val entries: MutableMap<String, MutableList<String>> = linkedMapOf()
    private var originalEntries: Map<String, List<String>> = emptyMap()
    private var saveMenuItem: MenuItem? = null
    private var adapter: PopupAdapter? = null
    private val qrChunkCollector = QrChunkCollector()
    private var searchQuery: String = ""

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@registerForActivityResult
        importFromQrLongImage(uri)
    }

    private val cameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            cameraScanLauncher.launch(com.journeyapps.barcodescanner.ScanOptions().apply {
                setDesiredBarcodeFormats(com.journeyapps.barcodescanner.ScanOptions.QR_CODE)
                setPrompt(getString(R.string.text_keyboard_layout_qr_scan_prompt))
                setBeepEnabled(false)
                setOrientationLocked(true)
            })
        } else {
            showToast(getString(R.string.text_keyboard_layout_qr_camera_permission_denied))
        }
    }

    private val cameraScanLauncher = registerForActivityResult(com.journeyapps.barcodescanner.ScanContract()) { result ->
        val content = result?.contents ?: return@registerForActivityResult
        addImportedChunkFromText(content)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        setContentView(ui)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setTitle(R.string.edit_popup_preset)

        val toolbarBaseTopPadding = toolbar.paddingTop
        ViewCompat.setOnApplyWindowInsetsListener(toolbar) { view, insets ->
            val statusTop = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            view.updatePadding(top = toolbarBaseTopPadding + statusTop)
            insets
        }
        ViewCompat.requestApplyInsets(toolbar)

        loadState()
        setupRecyclerView()
        setupSearch()
    }

    private fun setupSearch() {
        searchEdit.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                searchQuery = s?.toString()?.trim()?.lowercase() ?: ""
                adapter?.notifyDataSetChanged()
            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        saveMenuItem = menu.add(Menu.NONE, MENU_SAVE_ID, Menu.NONE, "${getString(R.string.save)}")
        saveMenuItem?.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS or MenuItem.SHOW_AS_ACTION_WITH_TEXT)
        menu.add(Menu.NONE, MENU_QR_EXPORT_ID, Menu.NONE, getString(R.string.text_keyboard_layout_qr_export))
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        menu.add(Menu.NONE, MENU_QR_IMPORT_SCAN_ID, Menu.NONE, getString(R.string.text_keyboard_layout_qr_import_scan))
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        menu.add(Menu.NONE, MENU_QR_IMPORT_IMAGE_ID, Menu.NONE, getString(R.string.text_keyboard_layout_qr_import_image))
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        updateSaveButtonState()
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        android.R.id.home -> {
            finish()
            true
        }
        MENU_SAVE_ID -> {
            savePopupPreset()
            true
        }
        MENU_QR_EXPORT_ID -> {
            exportPopupAsQrLongImage()
            true
        }
        MENU_QR_IMPORT_SCAN_ID -> {
            startCameraScanImport()
            true
        }
        MENU_QR_IMPORT_IMAGE_ID -> {
            pickImageLauncher.launch("image/*")
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    private fun loadState() {
        val snapshot = ConfigProviders.readPopupPreset<Map<String, List<String>>>()
        val parsed: Map<String, List<String>> = snapshot?.value
            ?: readDefaultPresetFromPopupKt()
        parsed.toSortedMap().forEach { (k, v) ->
            entries[k] = v.toMutableList()
        }
        originalEntries = normalizedEntries()
    }

    private fun readDefaultPresetFromPopupKt(): Map<String, List<String>> = runCatching {
        val holderClass = Class.forName("org.fcitx.fcitx5.android.input.popup.PopupPresetKt")
        
        val getterValue = runCatching {
            holderClass.getDeclaredMethod("getPopupPreset").invoke(null)
        }.onFailure { e ->
            android.util.Log.w("PopupEditor", "Failed to invoke getPopupPreset method", e)
        }.getOrNull()
        
        val fieldValue = runCatching {
            holderClass.getDeclaredField("PopupPreset").apply { 
                isAccessible = true 
            }.get(null)
        }.onFailure { e ->
            android.util.Log.w("PopupEditor", "Failed to access PopupPreset field", e)
        }.getOrNull()

        val rawMap = (getterValue ?: fieldValue) as? Map<*, *> ?: run {
            android.util.Log.w("PopupEditor", "PopupPreset returned null or is not a Map")
            return@runCatching emptyMap()
        }
        
        rawMap.mapNotNull { (key, value) ->
            val mappedKey = key as? String ?: run {
                android.util.Log.w("PopupEditor", "Invalid key type: ${key?.javaClass?.simpleName}, skipping")
                return@mapNotNull null
            }
            val mappedValues = when (value) {
                is Array<*> -> value.mapNotNull { it?.toString()?.trim() }.filter { it.isNotEmpty() }
                is List<*> -> value.mapNotNull { it?.toString()?.trim() }.filter { it.isNotEmpty() }
                else -> {
                    android.util.Log.w("PopupEditor", "Invalid value type for key '$mappedKey': ${value?.javaClass?.simpleName}, expected Array or List")
                    emptyList()
                }
            }
            mappedKey to mappedValues
        }.toMap()
    }.onFailure { e ->
        android.util.Log.e("PopupEditor", "Failed to read default popup preset", e)
    }.getOrDefault(emptyMap())

    private fun setupRecyclerView() {
        adapter = PopupAdapter()
        recyclerView.adapter = adapter
        recyclerView.addItemDecoration(object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(outRect: android.graphics.Rect, view: android.view.View, parent: RecyclerView, state: RecyclerView.State) {
                outRect.set(dp(8), dp(4), dp(8), dp(4))
            }
        })
    }

    private val filteredKeys: List<String>
        get() = if (searchQuery.isEmpty()) {
            entries.keys.toList()
        } else {
            entries.keys.filter { key ->
                key.lowercase().contains(searchQuery) ||
                entries[key]?.any { it.lowercase().contains(searchQuery) } == true
            }
        }

    private fun openEditor(originalKey: String?) {
        val currentKey = originalKey.orEmpty()
        val values = entries[originalKey].orEmpty().toMutableList()

        val scrollView = ScrollView(this)
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val pad = dp(16)
            setPadding(pad, dp(12), pad, pad)
        }
        scrollView.addView(container)

        // Key section
        val keyLabel = TextView(this).apply {
            text = getString(R.string.popup_editor_key)
            textSize = 12f
            setTextColor(styledColor(android.R.attr.textColorSecondary))
            setPadding(0, 0, 0, dp(4))
        }

        var currentKeyValue = currentKey

        val keyEdit = EditText(this).apply {
            setText(currentKeyValue)
            setSelection(text.length)
            hint = getString(R.string.popup_editor_key_input_hint)
            textSize = 16f
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(styledColor(android.R.attr.colorBackgroundFloating))
                setStroke(dp(1), styledColor(android.R.attr.colorControlNormal))
                cornerRadius = dp(8).toFloat()
            }
            setSingleLine(true)
        }

        container.addView(keyLabel)
        container.addView(keyEdit, LinearLayout.LayoutParams(MP, WC).apply {
            bottomMargin = dp(12)
        })

        // Candidates section
        val valueLabel = TextView(this).apply {
            text = "${getString(R.string.popup_editor_values)} (${getString(R.string.popup_editor_hint_click_edit_long_press_delete)})"
            textSize = 12f
            setTextColor(styledColor(android.R.attr.textColorSecondary))
            setPadding(0, 0, 0, dp(4))
        }
        container.addView(valueLabel)

        val candidatesFlow = DraggableFlowLayout(this)
        container.addView(candidatesFlow, LinearLayout.LayoutParams(MP, WC).apply {
            bottomMargin = dp(8)
        })

        // Inline add area
        val addRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
        }

        val addInput = EditText(this).apply {
            hint = getString(R.string.popup_editor_candidate_input_hint)
            textSize = 14f
            setPadding(dp(10), dp(6), dp(10), dp(6))
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(styledColor(android.R.attr.colorBackgroundFloating))
                setStroke(dp(1), styledColor(android.R.attr.colorControlNormal))
                cornerRadius = dp(8).toFloat()
            }
            setSingleLine(true)
        }

        val addBtn = TextView(this).apply {
            text = "+"
            textSize = 18f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(android.graphics.Color.WHITE)
            gravity = android.view.Gravity.CENTER
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(styledColor(android.R.attr.colorPrimary))
                cornerRadius = dp(16).toFloat()
            }
            setPadding(dp(12), dp(6), dp(12), dp(6))
        }

        addRow.addView(addInput, LinearLayout.LayoutParams(0, WC).apply {
            weight = 1f
            marginEnd = dp(8)
        })
        addRow.addView(addBtn, LinearLayout.LayoutParams(WC, WC))

        container.addView(addRow, LinearLayout.LayoutParams(MP, WC).apply {
            topMargin = dp(4)
        })

        // Define buildCandidates as a local function BEFORE it's used
        fun buildCandidates() {
            candidatesFlow.removeAllViews()
            values.forEachIndexed { index, value ->
                val candidateChip = LinearLayout(this@PopupEditorActivity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = android.view.Gravity.CENTER
                    setPadding(dp(8), dp(4), dp(4), dp(4))
                    background = android.graphics.drawable.GradientDrawable().apply {
                        setColor(styledColor(android.R.attr.colorButtonNormal))
                        setStroke(dp(1), styledColor(android.R.attr.colorControlNormal))
                        cornerRadius = dp(6).toFloat()
                    }
                    layoutParams = ViewGroup.MarginLayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        dp(36)
                    ).apply {
                        setMargins(dp(3), dp(3), dp(3), dp(3))
                    }
                    tag = index

                    val valueTextView = TextView(this@PopupEditorActivity).apply {
                        text = value
                        textSize = 14f
                        setPadding(dp(6), dp(2), dp(2), dp(2))
                        gravity = android.view.Gravity.CENTER
                    }
                    addView(valueTextView, LinearLayout.LayoutParams(WC, WC))

                    val deleteBtn = TextView(this@PopupEditorActivity).apply {
                        text = "×"
                        textSize = 14f
                        setTextColor(styledColor(android.R.attr.textColorSecondary))
                        setPadding(dp(4), dp(2), dp(4), dp(2))
                        gravity = android.view.Gravity.CENTER
                    }
                    addView(deleteBtn, LinearLayout.LayoutParams(WC, WC))

                    setOnClickListener {
                        val edit = EditText(this@PopupEditorActivity).apply {
                            setText(value)
                            setSelection(text.length)
                        }
                        AlertDialog.Builder(this@PopupEditorActivity)
                            .setTitle(R.string.edit)
                            .setView(edit)
                            .setPositiveButton(android.R.string.ok) { _, _ ->
                                val newValue = edit.text.toString().trim()
                                if (newValue.isNotEmpty()) {
                                    values[index] = newValue
                                    buildCandidates()
                                    updateSaveButtonState()
                                }
                            }
                            .setNegativeButton(android.R.string.cancel, null)
                            .show()
                    }

                    deleteBtn.setOnClickListener {
                        values.removeAt(index)
                        buildCandidates()
                        updateSaveButtonState()
                    }
                }
                candidatesFlow.addView(candidateChip)
            }
        }

        // Set up add button and Enter key after buildCandidates is defined
        addBtn.setOnClickListener {
            val newValue = addInput.text.toString().trim()
            if (newValue.isNotEmpty()) {
                values.add(newValue)
                addInput.text.clear()
                buildCandidates()
                updateSaveButtonState()
            }
        }

        addInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE || actionId == android.view.inputmethod.EditorInfo.IME_ACTION_NEXT) {
                val newValue = addInput.text.toString().trim()
                if (newValue.isNotEmpty()) {
                    values.add(newValue)
                    addInput.text.clear()
                    buildCandidates()
                    updateSaveButtonState()
                }
                true
            } else false
        }

        buildCandidates()

        // Set up drag listener
        candidatesFlow.onDragListener = object : DraggableFlowLayout.OnDragListener {
            override fun onDragStarted(view: View, position: Int) {}
            override fun onDragPositionChanged(from: Int, to: Int) {}
            override fun onDragMoved(view: View, rawX: Float, rawY: Float) {}
            override fun onDragEnded(view: View, position: Int) {
                candidatesFlow.postDelayed({
                    val newOrder = mutableListOf<String>()
                    for (i in 0 until candidatesFlow.childCount) {
                        val child = candidatesFlow.getChildAt(i)
                        if (child is LinearLayout) {
                            val textView = child.getChildAt(0) as? TextView
                            textView?.text?.toString()?.let { newOrder.add(it) }
                        }
                    }
                    if (newOrder.size == values.size &&
                        newOrder.groupingBy { it }.eachCount() == values.groupingBy { it }.eachCount()) {
                        values.clear()
                        values.addAll(newOrder)
                        updateSaveButtonState()
                    }
                    buildCandidates()
                }, 300)
            }
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle(if (originalKey == null) R.string.add else R.string.edit)
            .setView(scrollView)
            .setPositiveButton(android.R.string.ok, null)
            .setNegativeButton(android.R.string.cancel, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val newKey = keyEdit.text.toString().trim()
                if (newKey.isEmpty()) {
                    showToast(getString(R.string.popup_editor_key_empty))
                    return@setOnClickListener
                }
                if (newKey != currentKey && entries.containsKey(newKey)) {
                    showToast(getString(R.string.popup_editor_key_exists, newKey))
                    return@setOnClickListener
                }

                val positionChanged = originalKey != null && originalKey != newKey
                val oldPosition = if (positionChanged) {
                    entries.keys.indexOf(originalKey)
                } else {
                    entries.keys.indexOf(newKey)
                }

                if (positionChanged) {
                    entries.remove(originalKey)
                }
                entries[newKey] = values

                adapter?.let { adapter ->
                    if (positionChanged && oldPosition >= 0) {
                        adapter.notifyItemRemoved(oldPosition)
                        val newPosition = entries.keys.indexOf(newKey)
                        adapter.notifyItemInserted(newPosition)
                    } else if (originalKey == null) {
                        val newPosition = entries.keys.indexOf(newKey)
                        adapter.notifyItemInserted(newPosition)
                    } else {
                        adapter.notifyItemChanged(oldPosition)
                    }
                }

                updateSaveButtonState()
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    private fun confirmDelete(key: String) {
        val position = entries.keys.indexOf(key)
        AlertDialog.Builder(this)
            .setTitle(R.string.delete)
            .setMessage(getString(R.string.popup_editor_delete_confirm, key))
            .setPositiveButton(R.string.delete) { _, _ ->
                entries.remove(key)
                adapter?.notifyItemRemoved(position)
                updateSaveButtonState()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun savePopupPreset(): Boolean {
        val file = popupFile ?: run {
            showToast(getString(R.string.cannot_resolve_popup_preset))
            return false
        }
        if (!hasChanges() && file.exists() && file.length() > 0) {
            return true
        }
        file.parentFile?.mkdirs()

        val jsonElement = JsonObject(entries.toSortedMap().mapValues { (_, v) ->
            JsonArray(v.map { JsonPrimitive(it) })
        })

        file.writeText(prettyJson.encodeToString(jsonElement) + "\n")
        ConfigProviders.ensureWatching()
        showToast(getString(R.string.popup_preset_saved_at, file.absolutePath))
        originalEntries = normalizedEntries()
        updateSaveButtonState()
        return true
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun exportPopupAsQrLongImage() {
        lifecycleScope.launch {
            runCatching {
                if (!savePopupPreset()) {
                    throw IllegalStateException(getString(R.string.save_failed))
                }
                val file = popupFile ?: throw IllegalStateException(getString(R.string.cannot_resolve_popup_preset))
                withContext(Dispatchers.Default) {
                    JsonFileQrShareManager.encodeSavedJsonFileToLongImage(
                        file = file,
                        transferType = LayoutQrTransferCodec.TRANSFER_TYPE_POPUP,
                        typeLabel = getString(R.string.qr_payload_type_popup),
                        nameLabel = file.name
                    )
                }
            }.onSuccess { (longImage, _) ->
                val uri = JsonFileQrShareManager.saveLongImageToShareCache(this@PopupEditorActivity, longImage, "popup-preset-qr")
                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(Intent.createChooser(sendIntent, getString(R.string.text_keyboard_layout_qr_share_title)))
                showToast(getString(R.string.text_keyboard_layout_qr_exported))
            }.onFailure {
                showToast(getString(R.string.text_keyboard_layout_qr_export_failed, it.localizedMessage ?: ""))
            }
        }
    }

    private fun startCameraScanImport() {
        val granted = androidx.core.content.ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
        if (granted) {
            cameraScanLauncher.launch(com.journeyapps.barcodescanner.ScanOptions().apply {
                setDesiredBarcodeFormats(com.journeyapps.barcodescanner.ScanOptions.QR_CODE)
                setPrompt(getString(R.string.text_keyboard_layout_qr_scan_prompt))
                setBeepEnabled(false)
                setOrientationLocked(true)
            })
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun importFromQrLongImage(uri: Uri) {
        lifecycleScope.launch {
            runCatching {
                withContext(Dispatchers.Default) { JsonFileQrShareManager.decodeQrChunksFromImage(this@PopupEditorActivity, uri) }
            }.onSuccess { chunks ->
                if (chunks.isEmpty()) {
                    showToast(getString(R.string.text_keyboard_layout_qr_import_no_chunk))
                    return@onSuccess
                }
                val firstChunk = runCatching { LayoutQrTransferCodec.parseChunk(chunks.first()) }.getOrNull()
                val detectedType = firstChunk?.let { LayoutQrTransferCodec.detectTransferType(it.transferId) }
                if (detectedType != null && detectedType != LayoutQrTransferCodec.TRANSFER_TYPE_POPUP) {
                    showToast(
                        getString(
                            R.string.text_keyboard_layout_qr_type_mismatch,
                            getString(R.string.qr_payload_type_popup),
                            when (detectedType) {
                                LayoutQrTransferCodec.TRANSFER_TYPE_THEME -> getString(R.string.qr_payload_type_theme)
                                LayoutQrTransferCodec.TRANSFER_TYPE_LAYOUT -> getString(R.string.qr_payload_type_layout)
                                LayoutQrTransferCodec.TRANSFER_TYPE_POPUP -> getString(R.string.qr_payload_type_popup)
                                else -> getString(R.string.qr_payload_type_unknown)
                            }
                        )
                    )
                    return@onSuccess
                }
                val json = JsonFileQrShareManager.decodeChunksToJson(chunks)
                applyImportedPopupJson(json)
            }.onFailure {
                showToast(getString(R.string.text_keyboard_layout_qr_import_failed, it.localizedMessage ?: ""))
            }
        }
    }

    private fun addImportedChunkFromText(raw: String) {
        val headerChunk = JsonFileQrShareManager.parseQrPayload(raw)
        val headerType = headerChunk?.let { LayoutQrTransferCodec.detectTransferType(it.transferId) }
        if (headerType != null && headerType != LayoutQrTransferCodec.TRANSFER_TYPE_POPUP) {
            showToast(
                getString(
                    R.string.text_keyboard_layout_qr_type_mismatch,
                    getString(R.string.qr_payload_type_popup),
                    when (headerType) {
                        LayoutQrTransferCodec.TRANSFER_TYPE_THEME -> getString(R.string.qr_payload_type_theme)
                        LayoutQrTransferCodec.TRANSFER_TYPE_LAYOUT -> getString(R.string.qr_payload_type_layout)
                        LayoutQrTransferCodec.TRANSFER_TYPE_POPUP -> getString(R.string.qr_payload_type_popup)
                        else -> getString(R.string.qr_payload_type_unknown)
                    }
                )
            )
            return
        }
        val progress = runCatching { qrChunkCollector.addAndMaybeAssemble(raw) }.getOrNull()
        if (progress == null) {
            showToast(getString(R.string.text_keyboard_layout_qr_invalid_payload))
            return
        }
        if (progress.duplicate) {
            showToast(getString(R.string.text_keyboard_layout_qr_duplicate_chunk))
        }
        showToast(getString(R.string.text_keyboard_layout_qr_scan_progress, progress.current, progress.total))
        progress.completedJson?.let {
            applyImportedPopupJson(it)
            return
        }
        cameraScanLauncher.launch(com.journeyapps.barcodescanner.ScanOptions().apply {
            setDesiredBarcodeFormats(com.journeyapps.barcodescanner.ScanOptions.QR_CODE)
            setPrompt(getString(R.string.text_keyboard_layout_qr_scan_prompt))
            setBeepEnabled(false)
            setOrientationLocked(true)
        })
    }

    private fun applyImportedPopupJson(json: String) {
        runCatching {
            val parsed = Json.parseToJsonElement(json).jsonObject.mapValues { (_, v) ->
                val arr = v.jsonArray
                val values = arr.map { it.jsonPrimitive.contentOrNull?.trim() }
                require(values.none { it == null }) { "Invalid popup entry payload" }
                values.filterNotNull().filter { it.isNotEmpty() }
            }
            if (parsed.isEmpty() || parsed.values.none { it.isNotEmpty() }) {
                throw IllegalArgumentException("No valid popup entries")
            }
            parsed
        }.onSuccess { parsed ->
            AlertDialog.Builder(this)
                .setTitle(R.string.text_keyboard_layout_qr_import_confirm_title)
                .setMessage(getString(R.string.text_keyboard_layout_qr_import_confirm_message, parsed.size))
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    entries.clear()
                    parsed.toSortedMap().forEach { (k, v) -> entries[k] = v.toMutableList() }
                    adapter?.notifyDataSetChanged()
                    updateSaveButtonState()
                    showToast(getString(R.string.text_keyboard_layout_qr_import_success))
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }.onFailure {
            val message = it.message.orEmpty()
            if (message.startsWith("type_mismatch:")) {
                val type = message.removePrefix("type_mismatch:").firstOrNull()
                showToast(
                    getString(
                        R.string.text_keyboard_layout_qr_type_mismatch,
                        getString(R.string.qr_payload_type_popup),
                        when (type) {
                            LayoutQrTransferCodec.TRANSFER_TYPE_THEME -> getString(R.string.qr_payload_type_theme)
                            LayoutQrTransferCodec.TRANSFER_TYPE_LAYOUT -> getString(R.string.qr_payload_type_layout)
                            LayoutQrTransferCodec.TRANSFER_TYPE_POPUP -> getString(R.string.qr_payload_type_popup)
                            else -> getString(R.string.qr_payload_type_unknown)
                        }
                    )
                )
            } else {
                showToast(getString(R.string.text_keyboard_layout_qr_import_failed, it.localizedMessage ?: ""))
            }
        }
    }

    private fun normalizedEntries(): Map<String, List<String>> =
        entries.toSortedMap().mapValues { (_, value) -> value.toList() }

    private fun hasChanges(): Boolean = normalizedEntries() != originalEntries

    private fun updateSaveButtonState() {
        saveMenuItem?.let { menuItem ->
            menuItem.isEnabled = hasChanges()
            menuItem.title = getString(R.string.save)
        }
    }

    /**
     * RecyclerView Adapter for popup entries with card-style layout
     */
    private inner class PopupAdapter : RecyclerView.Adapter<PopupAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val cardContainer = LinearLayout(this@PopupEditorActivity).apply {
                orientation = LinearLayout.VERTICAL
                val pad = dp(12)
                setPadding(pad, pad, pad, pad)
                background = android.graphics.drawable.GradientDrawable().apply {
                    setColor(styledColor(android.R.attr.colorBackgroundFloating))
                    setStroke(dp(1), styledColor(android.R.attr.colorControlNormal))
                    cornerRadius = dp(12).toFloat()
                }
                elevation = dp(2f)
            }
            return ViewHolder(cardContainer)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val key = filteredKeys[position]
            val values = entries[key].orEmpty()
            holder.bind(key, values)
        }

        override fun getItemCount(): Int = filteredKeys.size

        inner class ViewHolder(
            private val cardContainer: LinearLayout
        ) : RecyclerView.ViewHolder(cardContainer) {

            fun bind(key: String, values: List<String>) {
                cardContainer.removeAllViews()

                // Header row: key badge + delete button
                val headerRow = LinearLayout(this@PopupEditorActivity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = android.view.Gravity.CENTER_VERTICAL
                }

                val keyBadge = TextView(this@PopupEditorActivity).apply {
                    text = key
                    textSize = 18f
                    setTypeface(null, android.graphics.Typeface.BOLD)
                    setPadding(dp(12), dp(6), dp(12), dp(6))
                    gravity = android.view.Gravity.CENTER
                    background = android.graphics.drawable.GradientDrawable().apply {
                        setColor(styledColor(android.R.attr.colorPrimary))
                        cornerRadius = dp(6).toFloat()
                    }
                    setTextColor(android.graphics.Color.WHITE)
                }

                val countText = TextView(this@PopupEditorActivity).apply {
                    text = "${values.size}"
                    textSize = 12f
                    setTextColor(styledColor(android.R.attr.textColorSecondary))
                    setPadding(dp(8), 0, 0, 0)
                }

                val spacer = View(this@PopupEditorActivity)

                val deleteBtn = TextView(this@PopupEditorActivity).apply {
                    text = "×"
                    textSize = 18f
                    setTextColor(styledColor(android.R.attr.textColorSecondary))
                    setPadding(dp(8), dp(4), dp(4), dp(4))
                    setOnClickListener {
                        confirmDelete(key)
                    }
                }

                headerRow.addView(keyBadge, LinearLayout.LayoutParams(WC, WC))
                headerRow.addView(countText, LinearLayout.LayoutParams(WC, WC))
                headerRow.addView(spacer, LinearLayout.LayoutParams(0, WC).apply { weight = 1f })
                headerRow.addView(deleteBtn, LinearLayout.LayoutParams(WC, WC))

                cardContainer.addView(headerRow, LinearLayout.LayoutParams(MP, WC).apply {
                    bottomMargin = dp(8)
                })

                // Candidates flow
                val candidatesFlow = FlowLayout(this@PopupEditorActivity)
                values.forEach { value ->
                    val chip = TextView(this@PopupEditorActivity).apply {
                        text = value
                        textSize = 13f
                        setPadding(dp(8), dp(3), dp(8), dp(3))
                        gravity = android.view.Gravity.CENTER
                        background = android.graphics.drawable.GradientDrawable().apply {
                            setColor(styledColor(android.R.attr.colorButtonNormal))
                            setStroke(dp(1), styledColor(android.R.attr.colorControlNormal))
                            cornerRadius = dp(4).toFloat()
                        }
                        layoutParams = ViewGroup.MarginLayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            dp(30)
                        ).apply {
                            setMargins(dp(2), dp(2), dp(2), dp(2))
                        }
                    }
                    candidatesFlow.addView(chip)
                }

                cardContainer.addView(candidatesFlow, LinearLayout.LayoutParams(MP, WC))

                // Click to edit
                cardContainer.setOnClickListener {
                    val pos = bindingAdapterPosition
                    if (pos != RecyclerView.NO_POSITION) {
                        openEditor(filteredKeys[pos])
                    }
                }
            }
        }
    }

    companion object {
        private const val MENU_SAVE_ID = 2001
        private const val MENU_QR_EXPORT_ID = 2002
        private const val MENU_QR_IMPORT_SCAN_ID = 2003
        private const val MENU_QR_IMPORT_IMAGE_ID = 2004
    }
}
