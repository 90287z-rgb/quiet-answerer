package com.quietanswerer.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.quietanswerer.App
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != "android.intent.action.QUICKBOOT_POWERON"
        ) {
            return
        }
        val app = context.applicationContext as App
        // Восстановление сессии после загрузки выполняется полностью внутри окна goAsync:
        // процесс по этому broadcast может быть убит сразу после возврата из onReceive.
        val pendingResult = goAsync()
        scope.launch {
            try {
                app.di.restoreAfterBoot()
            } catch (e: Exception) {
            } finally {
                pendingResult.finish()
            }
        }
    }
}