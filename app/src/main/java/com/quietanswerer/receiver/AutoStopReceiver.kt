package com.quietanswerer.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.quietanswerer.App
import com.quietanswerer.core.Actions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AutoStopReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Actions.TIMER_FIRED) return
        val app = context.applicationContext as App
        // Процесс мог быть разбужен именно этим broadcast: без goAsync он мог умереть
        // до завершения асинхронной остановки сессии.
        val pendingResult = goAsync()
        scope.launch {
            try {
                app.di.sessionController.stop()
            } catch (e: Exception) {
            } finally {
                pendingResult.finish()
            }
        }
    }
}