package com.quietanswerer.audio

import android.media.AudioManager

/** Pure decision logic for sound restore — unit-testable without Android runtime. */
object RestoreLogic {
    private const val DEFAULT_RINGER_MODE = AudioManager.RINGER_MODE_NORMAL

    fun ringerRestoreMode(saved: Int?): Int = saved ?: DEFAULT_RINGER_MODE

    fun streamShouldBeMuted(saved: SoundStateStore.StreamState?): Boolean = saved?.muted ?: false

    fun streamTargetVolume(saved: SoundStateStore.StreamState?, max: Int): Int? {
        if (saved == null) return null
        return saved.volume.coerceIn(0.coerceAtMost(max), max)
    }
}