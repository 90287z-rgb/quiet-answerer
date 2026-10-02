package com.quietanswerer.phone

import android.content.Context
import android.util.Log
import com.quietanswerer.audio.SilentModeManager
import com.quietanswerer.core.Prefs
import com.quietanswerer.data.Repository
import com.quietanswerer.data.db.IncomingCall
import com.quietanswerer.data.db.Reply
import com.quietanswerer.policy.ReplyDecision
import com.quietanswerer.policy.ReplySendingPolicy
import com.quietanswerer.policy.SmsSender
import com.quietanswerer.service.RemoteUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class CallHandler(
    private val context: Context,
    private val repo: Repository,
    private val contactFinder: ContactFinder,
    private val sound: SilentModeManager,
    private val updater: RemoteUpdater,
    private val scope: CoroutineScope,
    private val replyPolicy: ReplySendingPolicy
) {

    private val intercepted = HashSet<String>()

    fun onIncoming(number: String) {
        // Номер в RINGING может прийти как null/пустой (частая история на MTK/Android 14+):
        // это не повод пропускать обработку — сбрасывать-то обязаны, а SMS-политика сама
        // отклонит ответ для пустого номера (NO_NUMBER).
        scope.launch {
            Log.d(TAG, "incoming='$number'")
            // re-silence during the call in case the user raised the volume
            try {
                sound.muteAll()
            } catch (e: Exception) {
            }
            val activeVips = repo.activeVipContactIds()
            val contact = if (number.isNotBlank()) contactFinder.findByNumber(number) else null
            val isVip = contact != null && contact.id in activeVips
            val endCalls = Prefs.endCalls(context)
            val decision = HookPolicy.decide(isVip = isVip, endCalls = endCalls)
            Log.d(TAG, "decision=$decision")
            when (decision) {
                HookDecision.SKIP -> Unit
                HookDecision.HANG_UP -> {
                    intercepted.add(number)
                    CallBreaker.endCall(context)
                }
                HookDecision.RECORD -> intercepted.add(number)
            }
        }
    }

    fun onIdle() {
        val pending = intercepted.toList()
        intercepted.clear()
        for (number in pending) {
            scope.launch { processTail(number) }
        }
    }

    private suspend fun processTail(number: String) {
        val session = repo.openSession() ?: return
        Log.d(TAG, "tail: number='$number' session=${session.id}")
        // Номер входящего на Android 10+ в PhoneStateListener не отдаётся — берём из CallLog.
        var effective = number
        if (effective.isBlank()) {
            for (attempt in 1..2) {
                effective = contactFinder.recentIncomingNumber().orEmpty()
                if (effective.isNotBlank()) break
                kotlinx.coroutines.delay(800)
            }
            Log.d(TAG, "calllog number='$effective'")
        }
        val contact = if (effective.isNotBlank()) contactFinder.findByNumber(effective) else null
        val callId = repo.callDao.insert(
            IncomingCall(
                session_id = session.id,
                contact_id = contact?.id,
                phone_number = effective,
                date = System.currentTimeMillis()
            )
        )
        updater.refreshAll()
        sendReply(callId, effective, contact)
    }

    /**
     * Автоответ отправляется напрямую: запуск SendReplyActivity из фона на Android 14+
     * блокируется системой, а SmsManager отправка фоновым restrictions не подлежит.
     */
    private suspend fun sendReply(callId: Long, number: String, contact: ContactFinder.Contact?) {
        try {
            val statusId = Prefs.currentStatusId(context)
            val status = if (statusId > 0) repo.statusDao.byId(statusId) else null
            val text = status?.message.orEmpty()
            val fits = try {
                android.telephony.SmsManager.getDefault().divideMessage(text).size == 1
            } catch (e: Exception) {
                text.length <= 160
            }
            val decision = replyPolicy.decide(
                number = number,
                text = text,
                contactId = contact?.id,
                friendsOnly = Prefs.contactRequired(context),
                frequencyMinutes = Prefs.frequencyMinutes(context),
                fitsOneSms = fits
            )
            Log.d(TAG, "reply decision=$decision number='$number'")
            if (decision != ReplyDecision.ALLOW) return
            val replyId = repo.replyDao.insert(
                Reply(
                    incoming_call_id = callId,
                    date = System.currentTimeMillis(),
                    text = text,
                    sent_to = number,
                    status = Reply.STATUS_PENDING
                )
            )
            val sent = SmsSender.send(context, number, text, replyId)
            if (!sent) {
                repo.replyDao.setStatus(replyId, Reply.STATUS_FAIL)
                Log.w(TAG, "sms dispatch failed reply=$replyId")
            } else {
                Log.d(TAG, "sms dispatched reply=$replyId pending carrier result")
            }
            updater.refreshAll()
        } catch (e: Exception) {
            Log.w(TAG, "sendReply failed", e)
        }
    }

    companion object {
        const val TAG = "QuietAnswerer.Calls"
    }
}