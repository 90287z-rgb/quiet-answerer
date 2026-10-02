package com.quietanswerer

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat
import com.quietanswerer.audio.PrefsSoundStateStore
import com.quietanswerer.audio.SilentModeManager
import com.quietanswerer.core.Prefs
import com.quietanswerer.data.Repository
import com.quietanswerer.data.db.AppDatabase
import com.quietanswerer.data.db.Status
import com.quietanswerer.phone.CallHandler
import com.quietanswerer.phone.ContactFinder
import com.quietanswerer.policy.DbReplyContextProvider
import com.quietanswerer.policy.ReplySendingPolicy
import com.quietanswerer.service.AutoAnswerService
import com.quietanswerer.service.RemoteUpdater
import com.quietanswerer.session.SessionController
import com.quietanswerer.timer.TimerController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AppDi(private val context: Context, val appScope: CoroutineScope) {

    val db: AppDatabase = AppDatabase.get(context)
    val repository = Repository(db)
    val soundStore = PrefsSoundStateStore(context)
    val soundManager = SilentModeManager(context, soundStore)
    val contactFinder = ContactFinder(context)
    val timerController = TimerController(context)
    val updater = RemoteUpdater(context, repository)
    val sessionController = SessionController(context, repository, soundManager, timerController, updater)
    val replyPolicy = ReplySendingPolicy(DbReplyContextProvider(repository))
    val callHandler = CallHandler(
        context, repository, contactFinder, soundManager, updater, appScope, replyPolicy
    )

    suspend fun activeStatus(): Status? {
        val id = Prefs.currentStatusId(context)
        if (id <= 0) return null
        return repository.statusDao.byId(id)
    }

    fun seedDefaults() {
        appScope.launch {
            try {
                if (repository.statusDao.all().isNotEmpty()) return@launch
                val first = repository.statusDao.insert(
                    Status(name = context.getString(R.string.status_no_message), message = "", used_at = 0)
                )
                val second = repository.statusDao.insert(
                    Status(
                        name = context.getString(R.string.status_default_reply),
                        message = context.getString(R.string.status_default_reply),
                        used_at = 0
                    )
                )
                if (Prefs.currentStatusId(context) <= 0) {
                    Prefs.setCurrentStatusId(context, second)
                }
                updater.refreshAll()
            } catch (e: Exception) {
            }
        }
    }

    /** Orphan guard: sound was muted but the app/process was killed or restarted. */
    fun orphanGuard() {
        appScope.launch {
            try {
                if (Prefs.soundSuppressed(context)) {
                    soundManager.restoreAll()
                    soundManager.forgetAll()
                    Prefs.setSoundSuppressed(context, false)
                }
                // Открытую сессию здесь НЕ закрываем: после перезагрузки устройства её обязан
                // восстановить BootReceiver (restoreAfterBoot), и закрытие на холодном старте
                // устроило бы гонку с этим восстановлением. Зомби-сессии закрываются
                // штатными путями stop()/toggle, а резерв по звуку — этим guard'ом.
                updater.refreshAll()
            } catch (e: Exception) {
            }
        }
    }

    /** Boot restore: try to re-arm a live session; fail-safe restore the sound otherwise. */
    suspend fun restoreAfterBoot() {
        try {
            val session = repository.openSession() ?: return
            val at = Prefs.autoStopAt(context)
            if (at > 0) timerController.scheduleAt(at)
            try {
                soundManager.rememberAll()
                soundManager.muteAll()
            } catch (e: Exception) {
            }
            try {
                ContextCompat.startForegroundService(
                    context,
                    Intent(context, AutoAnswerService::class.java)
                )
            } catch (e: Exception) {
                // Android 12+ forbids FGS from background: recover into a safe state
                repository.closeAllSessions()
                timerController.clear()
                soundManager.restoreAll()
                soundManager.forgetAll()
                Prefs.setSoundSuppressed(context, false)
                return
            }
            updater.refreshAll()
        } catch (e: Exception) {
        }
    }
}

class App : Application() {

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    lateinit var di: AppDi

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        di = AppDi(this, appScope)
        di.seedDefaults()
        di.orphanGuard()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            )
            channel.description = getString(R.string.notification_channel_desc)
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            try {
                nm.createNotificationChannel(channel)
            } catch (e: Exception) {
            }
        }
    }

    companion object {
        const val CHANNEL_ID = "quiet_answerer_channel"
    }
}