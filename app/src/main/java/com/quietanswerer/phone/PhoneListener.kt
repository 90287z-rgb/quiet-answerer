package com.quietanswerer.phone

import android.content.Context
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.util.Log

class PhoneListener(
    private val context: Context,
    private val handler: CallHandler
) {

    private var registered = false

    private val listener = object : PhoneStateListener() {
        override fun onCallStateChanged(state: Int, phoneNumber: String?) {
            Log.d(CallHandler.TAG, "state=$state number='$phoneNumber'")
            when (state) {
                TelephonyManager.CALL_STATE_RINGING -> {
                    handler.onIncoming(phoneNumber.orEmpty())
                }
                TelephonyManager.CALL_STATE_IDLE -> {
                    handler.onIdle()
                }
                else -> Unit
            }
        }
    }

    fun register() {
        if (registered) return
        try {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            tm.listen(listener, PhoneStateListener.LISTEN_CALL_STATE)
            registered = true
            Log.d(CallHandler.TAG, "phone listener registered")
        } catch (e: Exception) {
            Log.w(CallHandler.TAG, "phone listener registration failed", e)
            registered = false
        }
    }

    fun unregister() {
        if (!registered) return
        try {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            tm.listen(listener, PhoneStateListener.LISTEN_NONE)
        } catch (e: Exception) {
        }
        registered = false
    }
}