package com.quietanswerer.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import com.quietanswerer.App
import com.quietanswerer.R
import com.quietanswerer.audio.canChangeDnd
import com.quietanswerer.audio.openDndSettings
import com.quietanswerer.core.Actions
import com.quietanswerer.core.Prefs
import kotlinx.coroutines.launch

/**
 * No-screen activity that is the single entry point for starting/stopping the quiet mode.
 * Mirrors the original app's way of circumventing Android 12+ FGS-from-background limits:
 * a user tap always flows through an activity context.
 */
class ToggleActivity : Activity() {

    private lateinit var app: App

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        app = application as App
        val action = intent?.action ?: Actions.TOGGLE_MAIN_SERVICE
        when (action) {
            Actions.START_MAIN_SERVICE -> doStartOrRequestPermissions()
            Actions.STOP_MAIN_SERVICE -> doStop()
            Actions.WIDGET_PREV_STATUS -> cycleStatus(-1)
            Actions.WIDGET_NEXT_STATUS -> cycleStatus(1)
            else -> {
                app.appScope.launch {
                    val open = app.di.repository.openSession() != null
                    if (open) doStop() else doStartOrRequestPermissions()
                }
            }
        }
    }

    private fun doStartOrRequestPermissions() {
        val needed = ArrayList<String>()
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            needed.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (Build.VERSION.SDK_INT >= 28 &&
            checkSelfPermission(Manifest.permission.ANSWER_PHONE_CALLS) != PackageManager.PERMISSION_GRANTED
        ) {
            needed.add(Manifest.permission.ANSWER_PHONE_CALLS)
        }
        if (needed.isNotEmpty()) {
            runCatching { requestPermissions(needed.toTypedArray(), REQ_START) }
        } else {
            doStart()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_START) doStart()
    }

    private fun doStart() {
        app.appScope.launch {
            try {
                app.di.sessionController.start()
            } catch (e: Exception) {
            }
        }
        Toast.makeText(this, R.string.on, Toast.LENGTH_SHORT).show()
        if (!canChangeDnd(this)) {
            openDndSettings(this)
        }
        finish()
    }

    private fun doStop() {
        app.appScope.launch {
            try {
                app.di.sessionController.stop()
            } catch (e: Exception) {
            }
        }
        Toast.makeText(this, R.string.off, Toast.LENGTH_SHORT).show()
        finish()
    }

    /** Переключение варианта SMS-ответа стрелками виджета: список стабилен по id, поэтому touch не ломает порядок. */
    private fun cycleStatus(delta: Int) {
        app.appScope.launch {
            val repo = app.di.repository
            val all = repo.statusDao.allOrdered()
            if (all.isNotEmpty()) {
                val currentId = Prefs.currentStatusId(app)
                val idx = all.indexOfFirst { it.id == currentId }
                val nextIdx = when {
                    idx < 0 -> if (delta > 0) 0 else all.lastIndex
                    delta > 0 -> (idx + 1) % all.size
                    else -> (idx - 1 + all.size) % all.size
                }
                val target = all[nextIdx]
                Prefs.setCurrentStatusId(app, target.id)
                repo.statusDao.touch(target.id, System.currentTimeMillis())
                app.di.updater.refreshAll()
            }
        }
        finish()
    }

    companion object {
        private const val REQ_START = 41
    }
}