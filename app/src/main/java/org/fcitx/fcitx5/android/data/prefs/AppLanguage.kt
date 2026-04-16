/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2025 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.data.prefs

import androidx.annotation.StringRes
import org.fcitx.fcitx5.android.R

enum class AppLanguage(
    val tag: String?,
    @StringRes override val stringRes: Int
) : ManagedPreferenceEnum {
    System(null, R.string.language_system_default),
    English("en", R.string.language_english),
    ChineseSimplified("zh-CN", R.string.language_chinese_simplified),
    ChineseTraditional("zh-TW", R.string.language_chinese_traditional),
    Japanese("ja", R.string.language_japanese),
    Korean("ko", R.string.language_korean),
    German("de", R.string.language_german),
    Russian("ru", R.string.language_russian),
    Spanish("es", R.string.language_spanish);

    companion object {
        fun fromTag(tag: String?): AppLanguage {
            if (tag == null) return System
            return entries.find { it.tag == tag } ?: System
        }
    }
}
