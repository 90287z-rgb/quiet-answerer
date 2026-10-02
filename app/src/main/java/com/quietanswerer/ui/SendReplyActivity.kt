package com.quietanswerer.ui

import android.app.Activity
import android.os.Bundle
import android.telephony.SmsManager
import com.quietanswerer.App
import com.quietanswerer.core.Actions
import com.quietanswerer.core.Prefs
import com.quietanswerer.data.db.Reply
import com.quietanswerer.policy.ReplyDecision
import com.quietanswerer.policy.SmsSender
import kotlinx.coroutines.launch

/**
 * No-screen activity that composes and sends the auto-reply for an incoming call.
 * The reply is sent from an activity context exactly as in the original app.
 */
class SendReplyActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val callId = intent?.getLongExtra(Actions.EXTRA_CALL_ID, -1L) ?: -1L
        val app = application as App
        app.appScope.launch {
            try {
                if (callId > 0) process(app, callId)
            } catch (e: Exception) {
                // never crash the process because of a failed reply
            }
            finish()
        }
    }

    private suspend fun process(app: App, callId: Long) {
        val call = app.di.repository.callDao.byId(callId) ?: return
        val status = app.di.activeStatus()
        val text = status?.message.orEmpty()
        val contact = app.di.contactFinder.findByNumber(call.phone_number)
        val fits = try {
            SmsManager.getDefault().divideMessage(text).size == 1
        } catch (e: Exception) {
            text.length <= 160
        }
        val decision = app.di.replyPolicy.decide(
            number = call.phone_number,
            text = text,
            contactId = contact?.id,
            friendsOnly = Prefs.contactRequired(this),
            frequencyMinutes = Prefs.frequencyMinutes(this),
            fitsOneSms = fits
        )
        if (decision != ReplyDecision.ALLOW) return
        val replyId = app.di.repository.replyDao.insert(
            Reply(
                incoming_call_id = call.id,
                date = System.currentTimeMillis(),
                text = text,
                sent_to = call.phone_number,
                status = Reply.STATUS_PENDING
            )
        )
        val sent = SmsSender.send(this, call.phone_number, text, replyId)
        if (!sent) {
            app.di.repository.replyDao.setStatus(replyId, Reply.STATUS_FAIL)
        }
        app.di.updater.refreshAll()
    }
}