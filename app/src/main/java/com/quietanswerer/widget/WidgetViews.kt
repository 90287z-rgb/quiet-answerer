package com.quietanswerer.widget

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.quietanswerer.R
import com.quietanswerer.core.Actions
import com.quietanswerer.core.Extras
import com.quietanswerer.service.WidgetCache
import com.quietanswerer.ui.MainActivity
import com.quietanswerer.ui.ToggleActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object WidgetViews {

    // SimpleDateFormat не потокобезопасен: build() вызывается и из onUpdate (главный поток),
    // и из pushUpdate на IO-диспетчере — формат создаётся на каждую операцию.
    private fun formatTime(millis: Long): String =
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(millis))

    fun build(context: Context, entry: WidgetCache.Entry?): RemoteViews {
        val e = entry ?: WidgetCache.Entry(false, true, "", 0, 0, -1L)
        val views = RemoteViews(context.packageName, R.layout.widget_autoanswer)
        val res = context.resources

        val dark = 0xFF37474F.toInt()
        val darkSub = 0xFF546E7A.toInt()
        val white = 0xFFFFFFFF.toInt()

        val on = e.isOpen
        val silentOk = e.canMute

        views.setInt(R.id.widget_root, "setBackgroundResource", if (on) R.drawable.widget_bg_on else R.drawable.widget_bg)

        if (on) {
            views.setFloat(R.id.widget_root, "setAlpha", if (silentOk) 1f else 0.55f)
            views.setTextColor(R.id.widget_title, white)
            views.setTextColor(R.id.widget_status, white)
        } else {
            views.setFloat(R.id.widget_root, "setAlpha", 1f)
            views.setTextColor(R.id.widget_title, dark)
            views.setTextColor(R.id.widget_status, darkSub)
        }

        val title = if (on) res.getString(R.string.on) else res.getString(R.string.off)
        views.setTextViewText(R.id.widget_title, title)

        val statusText = e.statusText.ifBlank {
            res.getString(R.string.status_touch_to_set)
        }
        views.setTextViewText(R.id.widget_status, statusText)

        // Стрелки переключения варианта ответа видны только когда вариантов больше одного.
        val showCycle = e.statusCount > 1
        views.setInt(R.id.widget_prev, "setVisibility", if (showCycle) View.VISIBLE else View.GONE)
        views.setInt(R.id.widget_next, "setVisibility", if (showCycle) View.VISIBLE else View.GONE)
        val arrowColor = if (on) white else darkSub
        views.setTextColor(R.id.widget_prev, arrowColor)
        views.setTextColor(R.id.widget_next, arrowColor)

        views.setTextViewText(
            R.id.widget_toggle,
            res.getString(if (on) R.string.turn_off else R.string.turn_on)
        )

        val showCounters = on && (e.callCount > 0 || e.replyCount > 0)
        views.setInt(R.id.widget_counters, "setVisibility", if (showCounters) View.VISIBLE else View.GONE)
        if (showCounters) {
            views.setTextViewText(
                R.id.widget_calls,
                res.getString(R.string.calls_count, e.callCount)
            )
            views.setTextViewText(
                R.id.widget_replies,
                res.getString(R.string.replies_count, e.replyCount)
            )
        }

        val hasTimer = on && e.autoStopAt > 0
        views.setInt(R.id.widget_timer, "setVisibility", if (hasTimer) View.VISIBLE else View.GONE)
        if (hasTimer) {
            views.setTextViewText(
                R.id.widget_timer,
                res.getString(R.string.until_time, formatTime(e.autoStopAt))
            )
        }

        views.setInt(
            R.id.widget_dnd_warning,
            "setVisibility",
            if (on && !silentOk) View.VISIBLE else View.GONE
        )

        views.setOnClickPendingIntent(R.id.widget_toggle, togglePending(context))
        views.setOnClickPendingIntent(R.id.widget_root, openPending(context))
        views.setOnClickPendingIntent(R.id.widget_status, openPending(context))
        views.setOnClickPendingIntent(R.id.widget_prev, cyclePending(context, Actions.WIDGET_PREV_STATUS, 2003))
        views.setOnClickPendingIntent(R.id.widget_next, cyclePending(context, Actions.WIDGET_NEXT_STATUS, 2004))
        return views
    }

    private fun togglePending(context: Context): PendingIntent = PendingIntent.getActivity(
        context,
        2001,
        Intent(context, ToggleActivity::class.java).setAction(Actions.TOGGLE_MAIN_SERVICE),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun openPending(context: Context): PendingIntent = PendingIntent.getActivity(
        context,
        2002,
        Intent(context, MainActivity::class.java).putExtra(Actions.EXTRA_TAB, Extras.TAB_MESSAGE),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun cyclePending(context: Context, action: String, code: Int): PendingIntent = PendingIntent.getActivity(
        context,
        code,
        Intent(context, ToggleActivity::class.java).setAction(action),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}