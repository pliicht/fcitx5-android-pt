/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.ui.main.settings.behavior.migration

import android.util.Log
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import org.fcitx.fcitx5.android.ui.main.settings.behavior.data.LayoutDataManager
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 数据迁移管理器，负责键盘布局数据的迁移、备份和恢复。
 * 
 * Migration strategy:
 * 1. For layouts with existing submode layouts:
 *    - Extract displayText values from base layout to corresponding submode layouts
 *    - Convert base layout displayText to simple strings or remove
 * 2. For layouts without submode layouts:
 *    - Keep displayText as-is for backward compatibility
 */
class DataMigrationManager(
    private val dataManager: LayoutDataManager
) {
    companion object {
        private const val TAG = "DataMigrationManager"
        private const val BACKUP_KEEP_COUNT = 3
    }

    private val entries = dataManager.entries
    private var backupFile: File? = null

    /**
     * Check if migration is needed by detecting old displayText format.
     * @return true if migration is needed, false if data is already in new format
     */
    fun checkIfMigrationNeeded(): Boolean {
        val layoutGroups = entries.keys.groupBy { key ->
            if (key.contains(':')) key.substringBeforeLast(':') else key
        }

        layoutGroups.forEach { (baseName, keys) ->
            val hasSubModeLayouts = keys.any { it.contains(':') && it != "$baseName:default" }

            if (hasSubModeLayouts) {
                val baseKey = keys.firstOrNull { it == baseName || it == "$baseName:default" }
                val baseLayout = baseKey?.let { entries[it] }
                baseLayout?.forEach { row ->
                    row.forEach { key ->
                        val displayText = key["displayText"]
                        if (displayText is Map<*, *> && displayText.isNotEmpty()) {
                            return true
                        }
                    }
                }
            }
        }

        return false
    }

    /**
     * Backup original file before migration.
     * @param originalFile The original file to backup
     * @return The backup file, or null if backup failed
     */
    fun backupOriginalFile(originalFile: File): File? {
        return runCatching {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val backupFileName = "${originalFile.nameWithoutExtension}_backup_$timestamp.json"
            val backupFile = File(originalFile.parentFile, backupFileName)

            originalFile.copyTo(backupFile, overwrite = false)
            Log.i(TAG, "Backup created: ${backupFile.absolutePath}")

            this.backupFile = backupFile
            cleanupOldBackups(originalFile)

            backupFile
        }.onFailure { e ->
            Log.e(TAG, "Failed to create backup", e)
        }.getOrNull()
    }

    /**
     * Clean up old backup files, keeping only the latest N backups.
     */
    private fun cleanupOldBackups(originalFile: File) {
        runCatching {
            val backupPattern = "${originalFile.nameWithoutExtension}_backup_"
            val backups = originalFile.parentFile
                ?.listFiles { file ->
                    file.name.startsWith(backupPattern) && file.name.endsWith(".json")
                }
                ?.sortedByDescending { it.lastModified() }
                ?: return

            if (backups.size > BACKUP_KEEP_COUNT) {
                backups.drop(BACKUP_KEEP_COUNT).forEach { oldBackup ->
                    if (oldBackup.delete()) {
                        Log.i(TAG, "Deleted old backup: ${oldBackup.absolutePath}")
                    }
                }
            }
        }.onFailure { e ->
            Log.e(TAG, "Failed to cleanup old backups", e)
        }
    }

    /**
     * Restore from backup file.
     * @param targetFile The original file to restore to
     */
    fun restoreFromBackup(targetFile: File?) {
        if (targetFile == null || backupFile == null) return

        runCatching {
            backupFile?.takeIf { it.exists() }?.copyTo(targetFile, overwrite = true)
            Log.i(TAG, "Restored from backup: ${backupFile?.absolutePath}")
        }.onFailure { e ->
            Log.e(TAG, "Failed to restore from backup", e)
        }
    }

    /**
     * Migrate all old displayText format to new submode structure automatically.
     */
    fun migrateAllDisplayTextToSubmodeStructure() {
        val layoutGroups = entries.keys.groupBy { key ->
            if (key.contains(':')) key.substringBeforeLast(':') else key
        }

        layoutGroups.forEach { (baseName, keys) ->
            val subModeKeys = keys.filter { it.contains(':') && it != "$baseName:default" }

            if (subModeKeys.isEmpty()) {
                val baseKey = keys.firstOrNull { it == baseName || it == "$baseName:default" }
                baseKey?.let { normalizeDisplayTextToMap(it) }
                return@forEach
            }

            val baseKey = keys.firstOrNull { it == baseName || it == "$baseName:default" }

            subModeKeys.forEach { subModeKey ->
                val subModeLabel = subModeKey.substringAfterLast(':')
                val subModeLayout = entries[subModeKey]
                subModeLayout?.let { layout ->
                    migrateDisplayTextForSubMode(layout, subModeLabel)
                }
            }

            baseKey?.let { cleanupBaseLayoutDisplayText(baseName) }
        }
    }

    /**
     * Migrate displayText from old format to new submode format.
     */
    internal fun migrateDisplayTextForSubMode(
        layout: MutableList<MutableList<MutableMap<String, Any?>>>,
        subModeLabel: String
    ) {
        for (row in layout) {
            for (key in row) {
                val displayText = key["displayText"]
                when (displayText) {
                    is Map<*, *> -> {
                        val specificValue = displayText[subModeLabel]?.toString()
                        val defaultValue = displayText["default"]?.toString()
                        val newValue = specificValue ?: defaultValue

                        if (newValue != null) {
                            key["displayText"] = newValue
                        } else {
                            key.remove("displayText")
                        }
                    }
                }
            }
        }
    }

    /**
     * Clean up base layout's displayText entries that are now covered by submode layouts.
     */
    internal fun cleanupBaseLayoutDisplayText(layoutName: String) {
        val baseLayout = entries[layoutName] ?: return

        val subModeKeys = entries.keys.filter {
            it.startsWith("$layoutName:") && it != "$layoutName:default"
        }

        if (subModeKeys.isEmpty()) return

        for (row in baseLayout) {
            for (key in row) {
                val displayText = key["displayText"]
                when (displayText) {
                    is MutableMap<*, *> -> {
                        val keysToRemove = subModeKeys.map { it.substringAfter("$layoutName:") }
                            .filter { it in displayText.keys }
                        keysToRemove.forEach { keyToRemove ->
                            displayText.remove(keyToRemove)
                        }

                        val remainingKeys = displayText.keys.filter { it != "default" }
                        if (remainingKeys.isEmpty()) {
                            val defaultValue = displayText["default"]?.toString()
                            if (defaultValue != null) {
                                key["displayText"] = defaultValue
                            } else if (displayText.isEmpty()) {
                                key.remove("displayText")
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Normalize displayText from JsonObject to Map<String, Any?> for consistent handling.
     */
    private fun normalizeDisplayTextToMap(layoutKey: String) {
        val layout = entries[layoutKey] ?: return
        for (row in layout) {
            for (key in row) {
                val displayText = key["displayText"]
                if (displayText is Map<*, *>) {
                    key["displayText"] = displayText.toMutableMap() as MutableMap<String, Any?>
                }
            }
        }
    }

    /**
     * Parse JsonElement to entries map.
     */
    fun parseJsonToEntries(jsonObject: JsonObject): Map<String, List<List<Map<String, Any?>>>> {
        val result = mutableMapOf<String, List<List<Map<String, Any?>>>>()

        jsonObject.entries.forEach { (layoutName, layoutValue) ->
            when (layoutValue) {
                is JsonArray -> {
                    val rows = dataManager.parseLayoutRows(layoutValue.jsonArray)
                    result[layoutName] = rows
                }
                is JsonObject -> {
                    layoutValue.entries.forEach { (subModeLabel, subModeValue) ->
                        if (subModeValue is JsonArray) {
                            val rows = dataManager.parseLayoutRows(subModeValue.jsonArray)
                            val key = if (subModeLabel == "default") {
                                layoutName
                            } else {
                                "$layoutName:$subModeLabel"
                            }
                            result[key] = rows
                        }
                    }
                }
                else -> {}
            }
        }
        return result
    }
}
