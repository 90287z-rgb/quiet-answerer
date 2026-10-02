package com.quietanswerer.phone

import android.content.Context
import android.net.Uri
import android.provider.CallLog
import android.provider.ContactsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ContactFinder(private val context: Context) {

    data class Contact(val id: Long, val name: String, val thumbnailUri: String?)

    /**
     * Последний входящий/пропущенный/отклонённый номер из журнала вызовов.
     * PhoneStateListener на Android 10+ не отдаёт номер входящего третьим приложениям,
     * а запись в CallLog создаётся сразу после завершения вызова.
     */
    suspend fun recentIncomingNumber(): String? = withContext(Dispatchers.IO) {
        try {
            val cutoff = System.currentTimeMillis() - 5 * 60_000L
            context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                arrayOf(CallLog.Calls.NUMBER),
                "${CallLog.Calls.TYPE} IN (1,3,6) AND ${CallLog.Calls.DATE} >= ?",
                arrayOf(cutoff.toString()),
                "${CallLog.Calls.DATE} DESC LIMIT 1"
            )?.use { c ->
                if (c.moveToFirst()) c.getString(0)?.takeIf { it.isNotBlank() } else null
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun findByNumber(number: String): Contact? = withContext(Dispatchers.IO) {
        if (number.isBlank()) return@withContext null
        try {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(number.trim())
            )
            context.contentResolver.query(
                uri,
                arrayOf(
                    ContactsContract.PhoneLookup._ID,
                    ContactsContract.PhoneLookup.DISPLAY_NAME
                ),
                null,
                null,
                null
            )?.use { c ->
                if (c.moveToFirst()) {
                    val id = c.getLong(c.getColumnIndexOrThrow(ContactsContract.PhoneLookup._ID))
                    val name = c.getString(c.getColumnIndexOrThrow(ContactsContract.PhoneLookup.DISPLAY_NAME))
                        ?: ""
                    Contact(id, name, null)
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun byContactId(contactId: Long): Contact? = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                arrayOf(
                    ContactsContract.Contacts._ID,
                    ContactsContract.Contacts.DISPLAY_NAME,
                    ContactsContract.Contacts.PHOTO_THUMBNAIL_URI
                ),
                ContactsContract.Contacts._ID + " = ?",
                arrayOf(contactId.toString()),
                null
            )?.use { c ->
                if (c.moveToFirst()) {
                    val name = c.getString(c.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME))
                    val thumb = c.getString(c.getColumnIndexOrThrow(ContactsContract.Contacts.PHOTO_THUMBNAIL_URI))
                    Contact(contactId, name ?: "", thumb)
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun nameFor(contactId: Long?): String? {
        if (contactId == null) return null
        return byContactId(contactId)?.name
    }

    suspend fun thumbnailFor(contactId: Long?): String? {
        if (contactId == null) return null
        return byContactId(contactId)?.thumbnailUri
    }
}