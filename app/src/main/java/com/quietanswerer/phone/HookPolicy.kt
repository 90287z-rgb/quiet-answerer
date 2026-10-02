package com.quietanswerer.phone

enum class HookDecision { SKIP, HANG_UP, RECORD }

object HookPolicy {
    fun decide(isVip: Boolean, endCalls: Boolean): HookDecision =
        when {
            isVip -> HookDecision.SKIP
            endCalls -> HookDecision.HANG_UP
            else -> HookDecision.RECORD
        }
}