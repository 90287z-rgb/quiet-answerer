package com.quietanswerer.core

import android.content.Context
import android.content.SharedPreferences

object Prefs {
    private const val FILE = "settings"

    object Keys {
        const val END_CALLS = "end_calls"
        const val CONTACT_REQUIRED = "contact_required"
        const val NOTIFICATION_FREQUENCY = "notification_frequency"
        const val MUTE_MUSIC = "mute_music"
        const val MUTE_NOTIFICATIONS = "mute_notifications"
        const val MUTE_SYSTEM = "mute_system"
        const val MUTE_ALARM = "mute_alarm"
        const val AUTO_STOP_AT = "auto_stop_at"
        const val CURRENT_STATUS_ID = "current_status_id"
        const val SOUND_SUPPRESSED = "sound_suppressed"
        const val ONBOARDING_DONE = "onboarding_done"
        const val THEME_MODE = "theme_mode"
        const val RINGER_INIT_STATE = "ringer_init_state"
        const val STREAM_INIT_STATE = "stream_%s_init_state"
        const val STREAM_INIT_VOLUME = "stream_%s_init_volume"
        const val LAST_WIDGET_CACHE = "last_widget_cache"
    }

    val DEFAULT_STATUS_MESSAGE = "response"

    fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun endCalls(c: Context) = prefs(c).getBoolean(Keys.END_CALLS, true)
    fun setEndCalls(c: Context, v: Boolean) = prefs(c).edit().putBoolean(Keys.END_CALLS, v).apply()

    fun contactRequired(c: Context) = prefs(c).getBoolean(Keys.CONTACT_REQUIRED, true)
    fun setContactRequired(c: Context, v: Boolean) =
        prefs(c).edit().putBoolean(Keys.CONTACT_REQUIRED, v).apply()

    fun frequencyMinutes(c: Context): Int =
        prefs(c).getString(Keys.NOTIFICATION_FREQUENCY, "15")?.toIntOrNull() ?: 15

    fun setFrequencyMinutes(c: Context, v: Int) =
        prefs(c).edit().putString(Keys.NOTIFICATION_FREQUENCY, v.toString()).apply()

    fun muteMusic(c: Context) = prefs(c).getBoolean(Keys.MUTE_MUSIC, true)
    fun setMuteMusic(c: Context, v: Boolean) = prefs(c).edit().putBoolean(Keys.MUTE_MUSIC, v).apply()

    fun muteNotifications(c: Context) = prefs(c).getBoolean(Keys.MUTE_NOTIFICATIONS, true)
    fun setMuteNotifications(c: Context, v: Boolean) =
        prefs(c).edit().putBoolean(Keys.MUTE_NOTIFICATIONS, v).apply()

    fun muteSystem(c: Context) = prefs(c).getBoolean(Keys.MUTE_SYSTEM, true)
    fun setMuteSystem(c: Context, v: Boolean) = prefs(c).edit().putBoolean(Keys.MUTE_SYSTEM, v).apply()

    fun muteAlarm(c: Context) = prefs(c).getBoolean(Keys.MUTE_ALARM, false)
    fun setMuteAlarm(c: Context, v: Boolean) = prefs(c).edit().putBoolean(Keys.MUTE_ALARM, v).apply()

    fun autoStopAt(c: Context): Long = prefs(c).getLong(Keys.AUTO_STOP_AT, -1L)
    fun setAutoStopAt(c: Context, v: Long) = prefs(c).edit().putLong(Keys.AUTO_STOP_AT, v).apply()

    fun currentStatusId(c: Context): Long = prefs(c).getLong(Keys.CURRENT_STATUS_ID, -1L)
    fun setCurrentStatusId(c: Context, v: Long) =
        prefs(c).edit().putLong(Keys.CURRENT_STATUS_ID, v).apply()

    fun soundSuppressed(c: Context): Boolean = prefs(c).getBoolean(Keys.SOUND_SUPPRESSED, false)
    fun setSoundSuppressed(c: Context, v: Boolean) =
        prefs(c).edit().putBoolean(Keys.SOUND_SUPPRESSED, v).apply()

    fun onboardingDone(c: Context): Boolean = prefs(c).getBoolean(Keys.ONBOARDING_DONE, false)
    fun setOnboardingDone(c: Context, v: Boolean) =
        prefs(c).edit().putBoolean(Keys.ONBOARDING_DONE, v).apply()

    const val THEME_SYSTEM = 0
    const val THEME_LIGHT = 1
    const val THEME_DARK = 2

    fun themeMode(c: Context): Int = prefs(c).getInt(Keys.THEME_MODE, THEME_SYSTEM)
    fun setThemeMode(c: Context, v: Int) = prefs(c).edit().putInt(Keys.THEME_MODE, v).apply()
}