package com.quietanswerer.data

import com.quietanswerer.data.db.AppDatabase
import com.quietanswerer.data.db.IncomingCall
import com.quietanswerer.data.db.Reply
import com.quietanswerer.data.db.Session
import com.quietanswerer.data.db.Status
import com.quietanswerer.data.db.Vip
import kotlinx.coroutines.flow.Flow

class Repository(private val db: AppDatabase) {

    val sessionDao = db.sessionDao()
    val statusDao = db.statusDao()
    val callDao = db.incomingCallDao()
    val replyDao = db.replyDao()
    val vipDao = db.vipDao()

    suspend fun openSession(): Session? = sessionDao.getOpen()

    suspend fun closeAllSessions() = sessionDao.closeAll()

    suspend fun startSession(): Session {
        sessionDao.closeAll()
        val id = sessionDao.insert(Session(started_at = System.currentTimeMillis(), is_open = true))
        return Session(id = id, started_at = System.currentTimeMillis(), is_open = true)
    }

    suspend fun closeSession(sessionId: Long) = sessionDao.close(sessionId)

    suspend fun status(id: Long): Status? = statusDao.byId(id)

    fun observeOpenSession(): Flow<Session?> = sessionDao.observeOpen()

    fun observeStatuses(): Flow<List<Status>> = statusDao.observeAll()

    fun observeCalls(sessionId: Long): Flow<List<IncomingCall>> = callDao.observeBySession(sessionId)

    fun observeCallCount(): Flow<Int> = replyDao.observeCallCountInOpenSession()

    fun observeReplyCount(): Flow<Int> = replyDao.observeCountInOpenSession()

    suspend fun repliesForCalls(callIds: List<Long>): List<Reply> {
        if (callIds.isEmpty()) return emptyList()
        return replyDao.forCalls(callIds)
    }

    fun observeVips(): Flow<List<Vip>> = vipDao.observeAll()

    suspend fun activeVipContactIds(): Set<Long> =
        vipDao.all().filter { it.is_active }.map { it.contact_id }.toSet()
}