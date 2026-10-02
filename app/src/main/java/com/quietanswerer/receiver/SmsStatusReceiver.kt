package com.quietanswerer.receiver

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.quietanswerer.App
import com.quietanswerer.core.Actions
import com.quietanswerer.data.db.Reply
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SmsStatusReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Actions.SMS_SENT) return
        val replyId = intent.getLongExtra(Actions.EXTRA_REPLY_ID, -1L)
        if (replyId <= 0) return
        val newStatus = if (resultCode == Activity.RESULT_OK) {
            Reply.STATUS_SENT
        } else {
            Reply.STATUS_FAIL
        }
        val app = context.applicationContext as App
        // SMS PendingIntent может разбудить мёртвый процесс: держим его через goAsync
        // до записи результата отправки в БД.
        val pendingResult = goAsync()
        scope.launch {
            try {
                app.di.repository.replyDao.setStatus(replyId, newStatus)
                app.di.updater.refreshAll()
            } catch (e: Exception) {
            } finally {
                pendingResult.finish()
            }
        }
    }
}