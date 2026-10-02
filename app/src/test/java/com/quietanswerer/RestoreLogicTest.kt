package com.quietanswerer

import android.media.AudioManager
import com.quietanswerer.audio.InMemorySoundStateStore
import com.quietanswerer.audio.RestoreLogic
import com.quietanswerer.audio.SoundStateStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RestoreLogicTest {

    @Test
    fun `restores default normal mode when nothing was remembered`() {
        assertEquals(AudioManager.RINGER_MODE_NORMAL, RestoreLogic.ringerRestoreMode(null))
        assertEquals(AudioManager.RINGER_MODE_VIBRATE, RestoreLogic.ringerRestoreMode(AudioManager.RINGER_MODE_VIBRATE))
    }

    @Test
    fun `stream is unmuted when nothing was remembered`() {
        assertEquals(false, RestoreLogic.streamShouldBeMuted(null))
        assertEquals(true, RestoreLogic.streamShouldBeMuted(SoundStateStore.StreamState(true, 5)))
    }

    @Test
    fun `volume is clamped to stream range`() {
        assertEquals(5, RestoreLogic.streamTargetVolume(SoundStateStore.StreamState(false, 5), 10))
        assertEquals(10, RestoreLogic.streamTargetVolume(SoundStateStore.StreamState(false, 99), 10))
        assertEquals(0, RestoreLogic.streamTargetVolume(SoundStateStore.StreamState(false, -3), 10))
    }

    @Test
    fun `volume is ignored when never saved`() {
        assertNull(RestoreLogic.streamTargetVolume(null, 10))
    }

    @Test
    fun `in-memory store forget restores defaults`() {
        val store = InMemorySoundStateStore()
        store.rememberRinger(AudioManager.RINGER_MODE_SILENT)
        store.rememberStream("music", true, 7)
        assertEquals(AudioManager.RINGER_MODE_SILENT, store.ringerState())
        assertEquals(SoundStateStore.StreamState(true, 7), store.streamState("music"))
        store.clearAll()
        assertNull(store.ringerState())
        assertNull(store.streamState("music"))
    }
}