package com.quietanswerer.timer

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.quietanswerer.core.Actions
import com.quietanswerer.core.Prefs
import com.quietanswerer.receiver.AutoStopReceiver

class TimerController(private val context: Context) {

    private fun pendingIntent(): PendingIntent = PendingIntent.getBroadcast(
        context,
        REQUEST_CODE,
        Intent(context, AutoStopReceiver::class.java).setAction(Actions.TIMER_FIRED),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    fun scheduleAt(at: Long) {
        val safe = at.coerceAtLeast(System.currentTimeMillis() + 1000L)
        try {
            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, safe, pendingIntent())
            } else {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, safe, pendingIntent())
            }
        } catch (e: Exception) {
            // even if the alarm cannot be scheduled we store the target time
        }
        Prefs.setAutoStopAt(context, safe)
    }

    fun scheduleInMinutes(minutes: Int) {
        scheduleAt(System.currentTimeMillis() + minutes * 60_000L)
    }

    fun isActive(): Boolean = Prefs.autoStopAt(context) > 0

    fun clear() {
        try {
            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            am.cancel(pendingIntent())
        } catch (e: Exception) {
        }
        Prefs.setAutoStopAt(context, -1L)
    }

    companion object {
        private const val REQUEST_CODE = 1001
    }
}