/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2025 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.LocaleList
import androidx.core.app.NotificationCompat
import androidx.preference.PreferenceManager
import org.fcitx.fcitx5.android.BuildConfig
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.prefs.AppLanguage
import org.fcitx.fcitx5.android.ui.main.ClipboardEditActivity
import org.fcitx.fcitx5.android.ui.main.MainActivity
import org.fcitx.fcitx5.android.ui.main.settings.SettingsRoute
import java.util.Locale
import kotlin.system.exitProcess

object AppUtil {

    fun appLabel(context: Context): String = runCatching {
        context.applicationInfo.loadLabel(context.packageManager).toString()
    }.getOrDefault(
        when {
            BuildConfig.IS_PT_BUILD -> context.getString(R.string.app_name)
            BuildConfig.DEBUG -> context.getString(R.string.app_name_debug)
            else -> context.getString(R.string.app_name_release)
        }
    )

    fun launchMain(context: Context) {
        context.startActivity<MainActivity> {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
        }
    }

    private fun launchMainToDest(context: Context, route: SettingsRoute) {
        context.startActivity<MainActivity> {
            action = Intent.ACTION_RUN
            putExtra(MainActivity.EXTRA_SETTINGS_ROUTE, route)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
        }
    }

    fun launchMainToKeyboard(context: Context) =
        launchMainToDest(context, SettingsRoute.VirtualKeyboard)

    fun launchMainToInputMethodList(context: Context) =
        launchMainToDest(context, SettingsRoute.InputMethodList)

    fun launchMainToThemeList(context: Context) =
        launchMainToDest(context, SettingsRoute.Theme)

    fun launchMainToInputMethodConfig(context: Context, uniqueName: String, displayName: String) =
        launchMainToDest(context, SettingsRoute.InputMethodConfig(displayName, uniqueName))

    fun launchMainToAddonMultiSelect(
        context: Context,
        title: String,
        addon: String,
        path: String,
        option: String,
        min: Int = 0
    ) = launchMainToDest(
        context,
        SettingsRoute.MultiSelect(
            title = title,
            addon = addon,
            path = path,
            option = option,
            min = min
        )
    )

    fun launchClipboardEdit(context: Context, id: Int, lastEntry: Boolean = false) {
        context.startActivity<ClipboardEditActivity> {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(ClipboardEditActivity.ENTRY_ID, id)
            putExtra(ClipboardEditActivity.LAST_ENTRY, lastEntry)
        }
    }

    fun exit() {
        exitProcess(0)
    }

    fun applyLanguageAndRestart(context: Context, language: AppLanguage) {
        // Force sync the language preference to disk before killing the process
        // setValue() uses apply() which is async and may not complete before exitProcess
        PreferenceManager.getDefaultSharedPreferences(context).edit()
            .putString("app_language", language.name)
            .commit()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Use Per-app Language API on Android 13+
            val localeManager = context.getSystemService(android.app.LocaleManager::class.java)
            val localeList = if (language.tag != null) {
                val locale = if (language.tag.contains("-")) {
                    val parts = language.tag.split("-")
                    Locale(parts[0], parts[1])
                } else {
                    Locale(language.tag)
                }
                LocaleList(locale)
            } else {
                LocaleList.getEmptyLocaleList()
            }
            localeManager.applicationLocales = localeList
            // Must restart process so fcitx native layer reloads translations
            val intent = Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            context.startActivity(intent)
            exitProcess(0)
        } else {
            // Fallback for Android 12 and below
            applyLanguage(context, language)
            val intent = Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            context.startActivity(intent)
            exitProcess(0)
        }
    }

    fun applyLanguage(context: Context, language: AppLanguage) {
        val locale = when {
            language.tag == null -> Locale.getDefault()
            language.tag.contains("-") -> {
                val parts = language.tag.split("-")
                Locale(parts[0], parts[1])
            }
            else -> Locale(language.tag)
        }
        Locale.setDefault(locale)
        val config = context.resources.configuration
        config.setLocale(locale)
        @Suppress("DEPRECATION")
        config.locale = locale
        @Suppress("DEPRECATION")
        context.resources.updateConfiguration(config, context.resources.displayMetrics)
        val appContext = context.applicationContext
        if (appContext !== context) {
            @Suppress("DEPRECATION")
            appContext.resources.updateConfiguration(config, appContext.resources.displayMetrics)
        }
    }

    private const val RESTART_CHANNEL_ID = "app-restart"

    private const val RESTART_NOTIFY_ID = 0xdead

    private fun createRestartNotificationChannel(ctx: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                RESTART_CHANNEL_ID,
                ctx.getText(R.string.restart_channel),
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = RESTART_CHANNEL_ID }
            ctx.notificationManager.createNotificationChannel(channel)
        }
    }

    fun showRestartNotification(ctx: Context) {
        createRestartNotificationChannel(ctx)
        NotificationCompat.Builder(ctx, RESTART_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_baseline_sync_24)
            .setContentTitle(appLabel(ctx))
            .setContentText(ctx.getText(R.string.restart_notify_msg))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(
                PendingIntent.getActivity(
                    ctx,
                    0,
                    Intent(ctx, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
            .setAutoCancel(true)
            .build()
            .let { ctx.notificationManager.notify(RESTART_NOTIFY_ID, it) }
    }
}
