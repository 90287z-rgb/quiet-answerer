package com.quietanswerer.service

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import com.quietanswerer.App
import com.quietanswerer.phone.PhoneListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class AutoAnswerService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var phoneListener: PhoneListener? = null
    private var started = false

    override fun onCreate() {
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val app = application as App
        if (!started) {
            started = true
            startForegroundCompat(app)
            val handler = app.di.callHandler
            phoneListener = PhoneListener(this, handler).also { it.register() }
            // refresh notification/widget with real data now that the FGS is up;
            // при sticky-рестарте с открытой сессией переприменяем тишину (OEM убийство процесса
            // могло сбросить состояние звука до того, как кто-нибудь выключит режим штатно)
            scope.launch {
                try {
                    if (app.di.repository.openSession() != null) app.di.soundManager.muteAll()
                    app.di.updater.refreshAll()
                } catch (e: Exception) {
                }
            }
        }
        return START_STICKY
    }

    private fun startForegroundCompat(app: App) {
        try {
            val notification = app.di.updater.buildForegroundNotification()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(RemoteUpdater.NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(RemoteUpdater.NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_NONE)
            } else {
                startForeground(RemoteUpdater.NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            // notification rejected by the system: keep running without foreground
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        // Сессию здесь НЕ закрываем: OEM-убийцы фона (ColorOS/Hans и др.) глушат сервис
        // без ведома пользователя — закрытие сессии по смерти сервиса самопроизвольно
        // выключало бы режим. Смерть процесса обрабатывает orphanGuard при следующем
        // старте приложения, а нештатные выключения идут через SessionController.stop().
        phoneListener?.unregister()
        phoneListener = null
        scope.cancel()
        super.onDestroy()
    }
}