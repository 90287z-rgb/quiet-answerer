package com.quietanswerer.service

import android.content.Context
import com.quietanswerer.core.Prefs

/**
 * Lightweight in-memory + prefs cache of the last widget snapshot.
 * The widget provider refreshes itself without touching the database,
 * so it reads this cache synchronously.
 */
object WidgetCache {
    private const val KEY = "last_widget_state"
    private const val SPLIT = "\u0001"

    data class Entry(
        val isOpen: Boolean,
        val canMute: Boolean,
        val statusText: String,
        val callCount: Int,
        val replyCount: Int,
        val autoStopAt: Long,
        val statusCount: Int = 0
    )

    fun read(appContext: android.content.Context): Entry? {
        val raw = Prefs.prefs(appContext).getString(KEY, null) ?: return null
        val parts = raw.split(SPLIT)
        if (parts.size != 7) return null
        return try {
            Entry(
                isOpen = parts[0] == "1",
                canMute = parts[1] == "1",
                statusText = parts[2],
                callCount = parts[3].toIntOrNull() ?: 0,
                replyCount = parts[4].toIntOrNull() ?: 0,
                autoStopAt = parts[5].toLongOrNull() ?: -1L,
                statusCount = parts[6].toIntOrNull() ?: 0
            )
        } catch (e: Exception) {
            null
        }
    }

    fun write(appContext: Context, e: Entry) {
        val raw = listOf(
            if (e.isOpen) "1" else "0",
            if (e.canMute) "1" else "0",
            e.statusText,
            e.callCount.toString(),
            e.replyCount.toString(),
            e.autoStopAt.toString(),
            e.statusCount.toString()
        ).joinToString(SPLIT)
        Prefs.prefs(appContext).edit().putString(KEY, raw).apply()
    }
}