package com.quietanswerer.session

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.quietanswerer.audio.SilentModeManager
import com.quietanswerer.core.Prefs
import com.quietanswerer.data.Repository
import com.quietanswerer.data.db.Session
import com.quietanswerer.service.AutoAnswerService
import com.quietanswerer.service.RemoteUpdater
import com.quietanswerer.timer.TimerController

class SessionController(
    private val context: Context,
    private val repo: Repository,
    private val sound: SilentModeManager,
    private val timer: TimerController,
    private val updater: RemoteUpdater
) {

    suspend fun start() {
        if (repo.openSession() != null) {
            ensureServiceRunning()
            updater.refreshAll()
            return
        }
        val session: Session = repo.startSession()
        // remember original sound state then mute (no-op gracefully without DND)
        try {
            sound.rememberAll()
        } catch (e: Exception) {
        }
        try {
            sound.muteAll()
        } catch (e: Exception) {
        }
        ensureServiceRunning()
        updater.refreshAll()
    }

    suspend fun stop() {
        try {
            repo.openSession()?.let { repo.closeSession(it.id) }
        } catch (e: Exception) {
        }
        timer.clear()
        sound.restoreAll()
        sound.forgetAll()
        context.stopService(Intent(context, AutoAnswerService::class.java))
        updater.refreshAll()
    }

    suspend fun toggle() {
        if (repo.openSession() != null) stop() else start()
    }

    suspend fun isActive(): Boolean = repo.openSession() != null

    private fun ensureServiceRunning() {
        try {
            ContextCompat.startForegroundService(
                context.applicationContext,
                Intent(context.applicationContext, AutoAnswerService::class.java)
            )
        } catch (e: Exception) {
            // FGS start can be rejected from background on Android 12+; degrade gracefully
        }
    }
}