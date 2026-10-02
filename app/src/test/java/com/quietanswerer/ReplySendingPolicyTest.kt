package com.quietanswerer

import com.quietanswerer.policy.ReplyContextProvider
import com.quietanswerer.policy.ReplyDecision
import com.quietanswerer.policy.ReplySendingPolicy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ReplySendingPolicyTest {

    private class FakeProvider(
        var count: Int = 0,
        var lastAt: Long = 0L
    ) : ReplyContextProvider {
        override suspend fun countSentTo(number: String): Int = count
        override suspend fun lastSentAt(number: String): Long = lastAt
    }

    private val number = "+79160000000"
    private val text = "I'll call you back"
    private val freq = 15

    @Test
    fun `allows a normal reply`() = runTest {
        val p = ReplySendingPolicy(FakeProvider())
        val decision = p.decide(number, text, 42L, friendsOnly = true, frequencyMinutes = freq, fitsOneSms = true)
        assertEquals(ReplyDecision.ALLOW, decision)
    }

    @Test
    fun `rejects empty number`() = runTest {
        val p = ReplySendingPolicy(FakeProvider())
        assertEquals(ReplyDecision.NO_NUMBER, p.decide(" ", text, 42L, false, freq, true))
    }

    @Test
    fun `rejects empty text`() = runTest {
        val p = ReplySendingPolicy(FakeProvider())
        assertEquals(ReplyDecision.NO_TEXT, p.decide(number, "   ", 42L, false, freq, true))
    }

    @Test
    fun `rejects unknown caller when friends only`() = runTest {
        val p = ReplySendingPolicy(FakeProvider())
        assertEquals(ReplyDecision.FRIENDS_ONLY, p.decide(number, text, null, friendsOnly = true, frequencyMinutes = freq, fitsOneSms = true))
    }

    @Test
    fun `allows unknown caller when friends only is off`() = runTest {
        val p = ReplySendingPolicy(FakeProvider())
        assertEquals(ReplyDecision.ALLOW, p.decide(number, text, null, friendsOnly = false, frequencyMinutes = freq, fitsOneSms = true))
    }

    @Test
    fun `rejects text that does not fit one sms`() = runTest {
        val p = ReplySendingPolicy(FakeProvider())
        assertEquals(ReplyDecision.TOO_LONG, p.decide(number, text, 42L, false, freq, fitsOneSms = false))
    }

    @Test
    fun `allows only one reply per number per session`() = runTest {
        val p = ReplySendingPolicy(FakeProvider(count = 1))
        assertEquals(ReplyDecision.ALREADY_REPLIED, p.decide(number, text, 42L, false, freq, true))
    }

    @Test
    fun `rejects too frequent replies`() = runTest {
        val now = System.currentTimeMillis()
        val p = ReplySendingPolicy(FakeProvider(lastAt = now - 5 * 60_000L))
        assertEquals(ReplyDecision.FREQUENCY, p.decide(number, text, 42L, false, freq, true))
    }

    @Test
    fun `allows reply after the frequency window`() = runTest {
        val now = System.currentTimeMillis()
        val p = ReplySendingPolicy(FakeProvider(lastAt = now - 30 * 60_000L))
        assertEquals(ReplyDecision.ALLOW, p.decide(number, text, 42L, false, freq, true))
    }
}