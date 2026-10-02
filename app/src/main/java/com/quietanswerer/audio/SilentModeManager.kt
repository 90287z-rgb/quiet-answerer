package com.quietanswerer.audio

import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.provider.Settings
import com.quietanswerer.core.Prefs

interface Muter {
    fun remember(): Boolean
    fun mute(): Boolean
    fun restore()
    fun forget()
}

class RingerMuter(
    private val context: Context,
    private val store: SoundStateStore
) : Muter {

    private fun audio(): AudioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    override fun remember(): Boolean = try {
        store.rememberRinger(audio().ringerMode)
        true
    } catch (e: Exception) {
        false
    }

    override fun mute(): Boolean {
        if (!canChangeDnd(context)) return false
        return try {
            audio().ringerMode = AudioManager.RINGER_MODE_SILENT
            true
        } catch (e: SecurityException) {
            false
        } catch (e: Exception) {
            false
        }
    }

    override fun restore() = try {
        val saved = store.ringerState()
        audio().ringerMode = RestoreLogic.ringerRestoreMode(saved)
    } catch (e: Exception) {
        // never crash on audio
    }

    override fun forget() = store.clearRinger()
}

class StreamMuter(
    private val context: Context,
    private val stream: Int,
    private val name: String,
    private val store: SoundStateStore
) : Muter {

    private fun audio(): AudioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    override fun remember(): Boolean = try {
        store.rememberStream(name, audio().isStreamMute(stream), audio().getStreamVolume(stream))
        true
    } catch (e: Exception) {
        false
    }

    override fun mute(): Boolean = try {
        audio().setStreamMute(stream, true)
        true
    } catch (e: Exception) {
        false
    }

    override fun restore() {
        try {
            val saved = store.streamState(name)
            audio().setStreamMute(stream, RestoreLogic.streamShouldBeMuted(saved))
            val max = audio().getStreamMaxVolume(stream)
            val target = RestoreLogic.streamTargetVolume(saved, max)
            if (target != null) {
                audio().setStreamVolume(stream, target, 0)
            }
        } catch (e: Exception) {
            // never crash on audio
        }
    }

    override fun forget() = store.clearStream(name)
}

fun canChangeDnd(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        nm.isNotificationPolicyAccessGranted
    } else {
        true
    }
}

class SilentModeManager(
    private val context: Context,
    private val store: SoundStateStore
) {

    fun selectedMutters(): List<Muter> = buildList {
        if (canChangeDnd(context)) {
            add(RingerMuter(context, store))
        }
        if (Prefs.muteMusic(context)) add(StreamMuter(context, AudioManager.STREAM_MUSIC, "music", store))
        if (Prefs.muteNotifications(context)) add(StreamMuter(context, AudioManager.STREAM_NOTIFICATION, "notifications", store))
        if (Prefs.muteSystem(context)) add(StreamMuter(context, AudioManager.STREAM_SYSTEM, "system", store))
        if (Prefs.muteAlarm(context)) add(StreamMuter(context, AudioManager.STREAM_ALARM, "alarm", store))
    }

    fun canMute(): Boolean = canChangeDnd(context)

    /**
     * Every channel the app can ever touch, independent of current settings.
     * Restore/forget must iterate this set: if a mute setting was changed during a session,
     * channels muted at start would never be restored otherwise (phone stays silent forever).
     */
    fun allChannels(): List<Muter> = buildList {
        add(RingerMuter(context, store))
        add(StreamMuter(context, AudioManager.STREAM_MUSIC, "music", store))
        add(StreamMuter(context, AudioManager.STREAM_NOTIFICATION, "notifications", store))
        add(StreamMuter(context, AudioManager.STREAM_SYSTEM, "system", store))
        add(StreamMuter(context, AudioManager.STREAM_ALARM, "alarm", store))
    }

    fun rememberAll() {
        for (m in selectedMutters()) {
            try {
                m.remember()
            } catch (e: Exception) {
                // no-op, individual muters swallow their own errors
            }
        }
    }

    /** @return true if at least one channel was actually silenced.
     *  Streams do not require DND access; only the ringer does (checked inside RingerMuter). */
    fun muteAll(): Boolean {
        var any = false
        for (m in selectedMutters()) {
            if (m.mute()) any = true
        }
        // ringer silence matters most; consider silence successful if any channel muted
        Prefs.setSoundSuppressed(context, any)
        return any
    }

    fun restoreAll() = try {
        for (m in allChannels()) {
            try {
                m.restore()
            } catch (e: Exception) {
                // per-muter best effort
            }
        }
        Prefs.setSoundSuppressed(context, false)
    } catch (e: Exception) {
        Prefs.setSoundSuppressed(context, false)
    }

    fun forgetAll() = try {
        for (m in allChannels()) {
            try {
                m.forget()
            } catch (e: Exception) {
            }
        }
    } catch (e: Exception) {
    }
}

fun openDndSettings(context: Context) {
    try {
        val intent = android.content.Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    } catch (e: Exception) {
        // some devices don't have this screen
    }
}