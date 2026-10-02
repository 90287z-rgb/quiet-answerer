package com.quietanswerer.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.quietanswerer.core.Actions
import com.quietanswerer.ui.ToggleActivity

class ToggleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action == Actions.WIDGET_TOGGLE ||
            action == Actions.TOGGLE_MAIN_SERVICE ||
            action == Actions.START_MAIN_SERVICE ||
            action == Actions.STOP_MAIN_SERVICE
        ) {
            val toLaunch = Intent(context, ToggleActivity::class.java)
                .setAction(action)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                context.startActivity(toLaunch)
            } catch (e: Exception) {
                // background activity launch blocked; nothing else we can do here
            }
        }
    }
}