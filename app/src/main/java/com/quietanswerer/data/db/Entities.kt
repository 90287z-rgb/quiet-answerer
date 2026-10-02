package com.quietanswerer.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "sessions")
data class Session(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val started_at: Long,
    val is_open: Boolean
)

@Entity(tableName = "statuses")
data class Status(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val message: String,
    val used_at: Long = 0
)

@Entity(
    tableName = "incoming_calls",
    foreignKeys = [
        ForeignKey(
            entity = Session::class,
            parentColumns = ["id"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("session_id"), Index("phone_number")]
)
data class IncomingCall(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val session_id: Long,
    val contact_id: Long? = null,
    val phone_number: String,
    val date: Long
)

@Entity(
    tableName = "replies",
    foreignKeys = [
        ForeignKey(
            entity = IncomingCall::class,
            parentColumns = ["id"],
            childColumns = ["incoming_call_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("incoming_call_id")]
)
data class Reply(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val incoming_call_id: Long,
    val date: Long,
    val text: String,
    val sent_to: String,
    val status: Int
) {
    companion object {
        const val STATUS_PENDING = 0
        const val STATUS_SENT = 1
        const val STATUS_FAIL = 2
    }
}

@Entity(tableName = "vips")
data class Vip(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val contact_id: Long,
    val is_active: Boolean
)