package com.quietanswerer.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions WHERE is_open = 1 LIMIT 1")
    fun observeOpen(): Flow<Session?>

    @Query("SELECT * FROM sessions WHERE is_open = 1 LIMIT 1")
    suspend fun getOpen(): Session?

    @Insert
    suspend fun insert(session: Session): Long

    @Query("UPDATE sessions SET is_open = 0 WHERE id = :id")
    suspend fun close(id: Long)

    @Query("UPDATE sessions SET is_open = 0")
    suspend fun closeAll()
}

@Dao
interface StatusDao {
    @Query("SELECT * FROM statuses ORDER BY used_at DESC")
    fun observeAll(): Flow<List<Status>>

    @Query("SELECT * FROM statuses ORDER BY used_at DESC")
    suspend fun all(): List<Status>

    // Стабильный порядок для перебора вариантов ответа из виджета:
    // used_at меняется при выборе, а id — нет.
    @Query("SELECT * FROM statuses ORDER BY id ASC")
    suspend fun allOrdered(): List<Status>

    @Query("SELECT COUNT(*) FROM statuses")
    suspend fun count(): Int

    @Query("SELECT * FROM statuses ORDER BY used_at DESC LIMIT 1")
    suspend fun mostRecent(): Status?

    @Query("SELECT * FROM statuses WHERE id = :id")
    suspend fun byId(id: Long): Status?

    @Insert
    suspend fun insert(status: Status): Long

    @Update
    suspend fun update(status: Status)

    @Delete
    suspend fun delete(status: Status)

    @Query("UPDATE statuses SET used_at = :time WHERE id = :id")
    suspend fun touch(id: Long, time: Long)

    @Query("UPDATE statuses SET name = :name, message = :message WHERE id = :id")
    suspend fun renameAndEdit(id: Long, name: String, message: String)
}

@Dao
interface IncomingCallDao {
    @Query("SELECT * FROM incoming_calls WHERE session_id = :sessionId ORDER BY date DESC")
    fun observeBySession(sessionId: Long): Flow<List<IncomingCall>>

    @Query("SELECT * FROM incoming_calls WHERE session_id = :sessionId ORDER BY date DESC")
    suspend fun bySession(sessionId: Long): List<IncomingCall>

    @Query("SELECT * FROM incoming_calls WHERE id = :id")
    suspend fun byId(id: Long): IncomingCall?

    @Insert
    suspend fun insert(call: IncomingCall): Long

    @Query("SELECT COUNT(*) FROM incoming_calls WHERE session_id = :sessionId")
    fun observeCount(sessionId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM incoming_calls WHERE session_id = :sessionId")
    suspend fun countForSession(sessionId: Long): Int

    @Query("DELETE FROM incoming_calls WHERE session_id = :sessionId")
    suspend fun clearSession(sessionId: Long)

    @Query("DELETE FROM incoming_calls")
    suspend fun clearAll()
}

@Dao
interface ReplyDao {
    @Query("SELECT * FROM replies WHERE incoming_call_id = :callId ORDER BY date DESC")
    suspend fun forCall(callId: Long): List<Reply>

    @Query("SELECT * FROM replies WHERE incoming_call_id IN (:callIds) ORDER BY date DESC")
    suspend fun forCalls(callIds: List<Long>): List<Reply>

    @Insert
    suspend fun insert(reply: Reply): Long

    @Update
    suspend fun update(reply: Reply)

    @Query("UPDATE replies SET status = :status WHERE id = :id")
    suspend fun setStatus(id: Long, status: Int)

    @Query(
        """SELECT COUNT(*) FROM replies
           JOIN incoming_calls ON replies.incoming_call_id = incoming_calls.id
           JOIN sessions ON incoming_calls.session_id = sessions.id
           WHERE sessions.is_open = 1 AND incoming_calls.phone_number = :number"""
    )
    suspend fun countForNumberInOpenSession(number: String): Int

    @Query(
        """SELECT replies.* FROM replies
           JOIN incoming_calls ON replies.incoming_call_id = incoming_calls.id
           JOIN sessions ON incoming_calls.session_id = sessions.id
           WHERE sessions.is_open = 1 AND incoming_calls.phone_number = :number
           ORDER BY replies.date DESC LIMIT 1"""
    )
    suspend fun lastForNumberInOpenSession(number: String): Reply?

    @Query(
        """SELECT COUNT(*) FROM replies
           JOIN incoming_calls ON replies.incoming_call_id = incoming_calls.id
           JOIN sessions ON incoming_calls.session_id = sessions.id
           WHERE sessions.is_open = 1"""
    )
    fun observeCountInOpenSession(): Flow<Int>

    @Query(
        """SELECT COUNT(*) FROM incoming_calls
           JOIN sessions ON incoming_calls.session_id = sessions.id
           WHERE sessions.is_open = 1"""
    )
    fun observeCallCountInOpenSession(): Flow<Int>

    @Query(
        """SELECT COUNT(*) FROM replies
           JOIN incoming_calls ON replies.incoming_call_id = incoming_calls.id
           WHERE incoming_calls.session_id = :sessionId"""
    )
    suspend fun countForSession(sessionId: Long): Int
}

@Dao
interface VipDao {
    @Query("SELECT * FROM vips ORDER BY id DESC")
    fun observeAll(): Flow<List<Vip>>

    @Query("SELECT * FROM vips ORDER BY id DESC")
    suspend fun all(): List<Vip>

    @Query("SELECT * FROM vips WHERE contact_id = :contactId")
    suspend fun byContact(contactId: Long): Vip?

    @Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun insert(vip: Vip): Long

    @Update
    suspend fun update(vip: Vip)

    @Delete
    suspend fun delete(vip: Vip)

    @Query("DELETE FROM vips WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("UPDATE vips SET is_active = 1")
    suspend fun enableAll()

    @Query("UPDATE vips SET is_active = 0")
    suspend fun disableAll()
}