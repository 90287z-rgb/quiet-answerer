package com.quietanswerer.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import com.quietanswerer.service.WidgetCache

class SilenceWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val entry = WidgetCache.read(context)
        val views = WidgetViews.build(context, entry)
        for (id in appWidgetIds) {
            try {
                appWidgetManager.updateAppWidget(id, views)
            } catch (e: Exception) {
                // a widget id may already be removed
            }
        }
    }

    companion object {
        fun pushUpdate(context: Context, entry: WidgetCache.Entry) {
            try {
                val manager = AppWidgetManager.getInstance(context)
                val ids = manager.getAppWidgetIds(ComponentName(context, SilenceWidgetProvider::class.java))
                if (ids.isEmpty()) return
                val views = WidgetViews.build(context, entry)
                for (id in ids) {
                    manager.updateAppWidget(id, views)
                }
            } catch (e: Exception) {
                // widget update must never crash the app
            }
        }
    }
}