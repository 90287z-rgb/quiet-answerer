package com.quietanswerer.audio

import android.content.Context
import com.quietanswerer.core.Prefs

interface SoundStateStore {
    fun rememberRinger(state: Int)
    fun ringerState(): Int?

    fun rememberStream(name: String, muted: Boolean, volume: Int)
    fun streamState(name: String): SoundStateStore.StreamState?

    fun clearRinger()
    fun clearStream(name: String)
    fun clearAll()

    data class StreamState(val muted: Boolean, val volume: Int)
}

class PrefsSoundStateStore(private val context: Context) : SoundStateStore {

    override fun rememberRinger(state: Int) {
        Prefs.prefs(context).edit().putInt(Prefs.Keys.RINGER_INIT_STATE, state).apply()
    }

    override fun ringerState(): Int? {
        if (!Prefs.prefs(context).contains(Prefs.Keys.RINGER_INIT_STATE)) return null
        return Prefs.prefs(context).getInt(Prefs.Keys.RINGER_INIT_STATE, android.media.AudioManager.RINGER_MODE_NORMAL)
    }

    override fun rememberStream(name: String, muted: Boolean, volume: Int) {
        Prefs.prefs(context).edit()
            .putBoolean(String.format(Prefs.Keys.STREAM_INIT_STATE, name), muted)
            .putInt(String.format(Prefs.Keys.STREAM_INIT_VOLUME, name), volume)
            .apply()
    }

    override fun streamState(name: String): SoundStateStore.StreamState? {
        val stateKey = String.format(Prefs.Keys.STREAM_INIT_STATE, name)
        if (!Prefs.prefs(context).contains(stateKey)) return null
        val muted = Prefs.prefs(context).getBoolean(stateKey, false)
        val volume = Prefs.prefs(context)
            .getInt(String.format(Prefs.Keys.STREAM_INIT_VOLUME, name), -1)
        return SoundStateStore.StreamState(muted, volume)
    }

    override fun clearRinger() {
        Prefs.prefs(context).edit().remove(Prefs.Keys.RINGER_INIT_STATE).apply()
    }

    override fun clearStream(name: String) {
        Prefs.prefs(context).edit()
            .remove(String.format(Prefs.Keys.STREAM_INIT_STATE, name))
            .remove(String.format(Prefs.Keys.STREAM_INIT_VOLUME, name))
            .apply()
    }

    override fun clearAll() {
        val e = Prefs.prefs(context).edit()
        e.remove(Prefs.Keys.RINGER_INIT_STATE)
        for (s in arrayOf("music", "notifications", "system", "alarm")) {
            e.remove(String.format(Prefs.Keys.STREAM_INIT_STATE, s))
            e.remove(String.format(Prefs.Keys.STREAM_INIT_VOLUME, s))
        }
        e.apply()
    }
}

class InMemorySoundStateStore : SoundStateStore {
    private var ring: Int? = null
    private val streams = mutableMapOf<String, SoundStateStore.StreamState>()

    override fun rememberRinger(state: Int) {
        ring = state
    }

    override fun ringerState(): Int? = ring

    override fun rememberStream(name: String, muted: Boolean, volume: Int) {
        streams[name] = SoundStateStore.StreamState(muted, volume)
    }

    override fun streamState(name: String): SoundStateStore.StreamState? = streams[name]

    override fun clearRinger() {
        ring = null
    }

    override fun clearStream(name: String) {
        streams.remove(name)
    }

    override fun clearAll() {
        ring = null
        streams.clear()
    }
}