package com.quietanswerer.policy

enum class ReplyDecision { ALLOW, NO_NUMBER, NO_TEXT, FRIENDS_ONLY, TOO_LONG, FREQUENCY, ALREADY_REPLIED }

interface ReplyContextProvider {
    suspend fun countSentTo(number: String): Int
    suspend fun lastSentAt(number: String): Long
}

class ReplySendingPolicy(private val provider: ReplyContextProvider) {

    suspend fun decide(
        number: String,
        text: String,
        contactId: Long?,
        friendsOnly: Boolean,
        frequencyMinutes: Int,
        fitsOneSms: Boolean
    ): ReplyDecision {
        if (number.isBlank()) return ReplyDecision.NO_NUMBER
        if (text.isBlank()) return ReplyDecision.NO_TEXT
        if (friendsOnly && contactId == null) return ReplyDecision.FRIENDS_ONLY
        if (!fitsOneSms) return ReplyDecision.TOO_LONG
        val count = provider.countSentTo(number)
        if (count >= 1) return ReplyDecision.ALREADY_REPLIED
        val last = provider.lastSentAt(number)
        if (last > 0 && System.currentTimeMillis() - last < frequencyMinutes * 60_000L) {
            return ReplyDecision.FREQUENCY
        }
        return ReplyDecision.ALLOW
    }
}

class DbReplyContextProvider(
    private val repo: com.quietanswerer.data.Repository
) : ReplyContextProvider {
    override suspend fun countSentTo(number: String): Int =
        repo.replyDao.countForNumberInOpenSession(number)

    override suspend fun lastSentAt(number: String): Long =
        repo.replyDao.lastForNumberInOpenSession(number)?.date ?: 0L
}