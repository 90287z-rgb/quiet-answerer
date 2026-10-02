package com.quietanswerer.service

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.quietanswerer.App
import com.quietanswerer.R
import com.quietanswerer.core.Actions
import com.quietanswerer.core.Extras
import com.quietanswerer.core.Prefs
import com.quietanswerer.data.Repository
import com.quietanswerer.data.db.Status
import com.quietanswerer.ui.MainActivity
import com.quietanswerer.ui.ToggleActivity
import com.quietanswerer.widget.SilenceWidgetProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RemoteUpdater(
    private val context: Context,
    private val repo: Repository
) {

    suspend fun refreshAll() = withContext(Dispatchers.IO) {
        try {
            val entry = buildCacheEntry()
            WidgetCache.write(context, entry)
            SilenceWidgetProvider.pushUpdate(context, entry)
            updateNotification(entry)
        } catch (e: Exception) {
            // never crash on refresh
        }
    }

    private suspend fun buildCacheEntry(): WidgetCache.Entry {
        val session = repo.openSession()
        val isOpen = session != null
        var calls = 0
        var replies = 0
        var statusText = ""
        var autoStopAt = -1L
        if (session != null) {
            calls = repo.callDao.countForSession(session.id)
            replies = repo.replyDao.countForSession(session.id)
            autoStopAt = Prefs.autoStopAt(context)
        }
        val activeId = Prefs.currentStatusId(context)
        val active: Status? = if (activeId > 0) repo.statusDao.byId(activeId) else null
        statusText = active?.message?.takeIf { it.isNotBlank() } ?: active?.name.orEmpty()
        val statusCount = repo.statusDao.count()
        return WidgetCache.Entry(
            isOpen = isOpen,
            canMute = com.quietanswerer.audio.canChangeDnd(context),
            statusText = statusText,
            callCount = calls,
            replyCount = replies,
            autoStopAt = autoStopAt,
            statusCount = statusCount
        )
    }

    private fun updateNotification(entry: WidgetCache.Entry) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val title = buildString {
            append(context.getString(R.string.notification_title))
            if (entry.autoStopAt > 0) {
                append(" · ")
                append(context.getString(R.string.until_time, formatTime(entry.autoStopAt)))
            }
        }
        val text = buildString {
            if (entry.statusText.isNotBlank()) {
                append("“")
                append(entry.statusText.take(60))
                append("”")
                append("   ")
            }
            append(context.getString(R.string.calls_count, entry.callCount))
            append("   ")
            append(context.getString(R.string.replies_count, entry.replyCount))
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            3001,
            Intent(context, MainActivity::class.java).putExtra(Actions.EXTRA_TAB, Extras.TAB_LOG),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val turnOffIntent = PendingIntent.getActivity(
            context,
            3002,
            Intent(context, ToggleActivity::class.java).setAction(Actions.STOP_MAIN_SERVICE),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, App.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentIntent)
            .addAction(0, context.getString(R.string.turn_off), turnOffIntent)
            .build()

        try {
            if (entry.isOpen) {
                NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
            } else {
                NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
            }
        } catch (e: Exception) {
        }
    }

    fun buildForegroundNotification(): Notification {
        val pendingTurnOff = PendingIntent.getActivity(
            context,
            3003,
            Intent(context, ToggleActivity::class.java).setAction(Actions.STOP_MAIN_SERVICE),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(context, App.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat)
            .setContentTitle(context.getString(R.string.notification_title))
            .setContentText(context.getString(R.string.status_touch_to_set))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(0, context.getString(R.string.turn_off), pendingTurnOff)
            .build()
    }

    companion object {
        const val NOTIFICATION_ID = 1001
    }

    // SimpleDateFormat не потокобезопасен: refreshAll выполняется на IO-диспетчере,
    // поэтому формат создаётся на каждую операцию.
    private fun formatTime(millis: Long): String =
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(millis))
}