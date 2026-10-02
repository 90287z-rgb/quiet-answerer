package com.quietanswerer.phone

import android.content.Context
import android.os.Build
import android.telecom.TelecomManager
import android.telephony.TelephonyManager
import android.util.Log

object CallBreaker {

    private const val TAG = "QuietAnswerer.Calls"

    /** Tries the modern TelecomManager path first, then falls back to ITelephony reflection. */
    fun endCall(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                val tm = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
                if (tm != null) {
                    val result = tm.endCall()
                    Log.d(TAG, "endCall via TelecomManager: $result")
                    if (result) return true
                } else {
                    Log.w(TAG, "no TelecomManager")
                }
            } catch (e: Exception) {
                // fall through to reflection
                Log.w(TAG, "TelecomManager.endCall failed", e)
            }
        }
        return try {
            val result = reflectionEndCall(context)
            Log.d(TAG, "endCall via reflection: $result")
            result
        } catch (e: Exception) {
            Log.w(TAG, "reflection endCall failed", e)
            false
        }
    }

    private fun reflectionEndCall(context: Context): Boolean {
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val cls = Class.forName(tm.javaClass.name)
        val method = cls.getDeclaredMethod("getITelephony")
        method.isAccessible = true
        val iTelephony = method.invoke(tm)
        val endCall = iTelephony.javaClass.getDeclaredMethod("endCall")
        endCall.isAccessible = true
        return endCall.invoke(iTelephony) as Boolean
    }
}