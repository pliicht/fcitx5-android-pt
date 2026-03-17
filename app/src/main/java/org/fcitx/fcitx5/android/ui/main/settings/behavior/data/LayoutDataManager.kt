/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.ui.main.settings.behavior.data

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray

/**
 * 布局数据管理器，管理键盘布局的数据结构。
 */
class LayoutDataManager {
    val entries = mutableMapOf<String, MutableList<MutableList<MutableMap<String, Any?>>>>()

    companion object {
        /**
         * Extension function to convert JsonElement to Any?.
         */
        fun toAny(element: JsonElement): Any? = when (element) {
            is JsonObject -> element.toMap()
            is JsonArray -> element.map { toAny(it) }
            is JsonPrimitive -> {
                if (element.isString) element.content
                else element.booleanOrNull ?: element.intOrNull ?: element.doubleOrNull
            }
            is JsonNull -> null
        }
    }

    /**
     * Parse layout rows from JsonArray.
     */
    fun parseLayoutRows(rowsArray: JsonArray): List<List<Map<String, Any?>>> {
        val rows = mutableListOf<List<Map<String, Any?>>>()
        for (i in rowsArray.indices) {
            val rowArray = rowsArray[i].jsonArray
            val row = mutableListOf<Map<String, Any?>>()
            for (j in rowArray.indices) {
                val rowElement = rowArray[j]
                if (rowElement is JsonNull) continue
                if (rowElement !is JsonObject) continue

                val keyJson = rowElement
                val keyMap = mutableMapOf<String, Any?>()
                keyJson.entries.forEach { (key, value) ->
                    keyMap[key] = when (value) {
                        is JsonObject -> value.toMap().mapValues { it.value.let { e -> toAny(e) } }
                        is JsonArray -> value.map { toAny(it) }
                        is JsonPrimitive -> {
                            if (value.isString) value.content
                            else value.booleanOrNull ?: value.intOrNull ?: value.doubleOrNull ?: value.content
                        }
                        is JsonNull -> null
                    }
                }
                row.add(keyMap)
            }
            rows.add(row)
        }
        return rows
    }

    /**
     * Deep copy a layout.
     */
    fun copyLayout(sourceLayout: List<List<Map<String, Any?>>>): MutableList<MutableList<MutableMap<String, Any?>>> {
        val copiedLayout = mutableListOf<MutableList<MutableMap<String, Any?>>>()
        for (sourceRow in sourceLayout) {
            val newRow = mutableListOf<MutableMap<String, Any?>>()
            for (sourceKey in sourceRow) {
                val newKey = mutableMapOf<String, Any?>()
                sourceKey.forEach { (k, v) ->
                    newKey[k] = when (v) {
                        is Map<*, *> -> mutableMapOf<String, Any?>().apply {
                            v.forEach { (kk, vv) -> put(kk.toString(), vv) }
                        }
                        is List<*> -> v.toList()
                        else -> v
                    }
                }
                newRow.add(newKey)
            }
            copiedLayout.add(newRow)
        }
        return copiedLayout
    }

    /**
     * Normalize entries for comparison.
     */
    fun normalizedEntries(): Map<String, List<List<Map<String, Any?>>>> =
        entries.toSortedMap().mapValues { (_, rows) ->
            rows.map { row ->
                row.map { key -> key.toMap() }
            }
        }
}
