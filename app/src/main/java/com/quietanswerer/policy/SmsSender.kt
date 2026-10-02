package com.quietanswerer.policy

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.telephony.SmsManager
import com.quietanswerer.core.Actions

object SmsSender {

    /**
     * Sends the message in a single SMS segment with a result PendingIntent.
     * Returns true on successful dispatch.
     */
    fun send(context: Context, number: String, text: String, replyId: Long): Boolean {
        return try {
            val sms = SmsManager.getDefault()
            val parts = sms.divideMessage(text)
            if (parts.size != 1) return false
            val sentIntent = PendingIntent.getBroadcast(
                context,
                replyId.toInt(),
                Intent(Actions.SMS_SENT)
                    .setPackage(context.packageName)
                    .putExtra(Actions.EXTRA_REPLY_ID, replyId),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            sms.sendTextMessage(number, null, text, sentIntent, null)
            true
        } catch (e: Exception) {
            false
        }
    }
}