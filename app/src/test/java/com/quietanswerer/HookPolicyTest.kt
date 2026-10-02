package com.quietanswerer

import com.quietanswerer.phone.HookDecision
import com.quietanswerer.phone.HookPolicy
import org.junit.Assert.assertEquals
import org.junit.Test

class HookPolicyTest {

    @Test
    fun `vip is always passed through`() {
        assertEquals(HookDecision.SKIP, HookPolicy.decide(isVip = true, endCalls = true))
        assertEquals(HookDecision.SKIP, HookPolicy.decide(isVip = true, endCalls = false))
    }

    @Test
    fun `regular caller is hung up when end calls is enabled`() {
        assertEquals(HookDecision.HANG_UP, HookPolicy.decide(isVip = false, endCalls = true))
    }

    @Test
    fun `regular caller is recorded but not hung up without end calls`() {
        assertEquals(HookDecision.RECORD, HookPolicy.decide(isVip = false, endCalls = false))
    }
}