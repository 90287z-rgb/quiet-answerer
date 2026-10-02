package com.quietanswerer.core

object Actions {
    const val START_MAIN_SERVICE = "com.quietanswerer.action.START_MAIN_SERVICE"
    const val STOP_MAIN_SERVICE = "com.quietanswerer.action.STOP_MAIN_SERVICE"
    const val TOGGLE_MAIN_SERVICE = "com.quietanswerer.action.TOGGLE_MAIN_SERVICE"
    const val SEND_REPLY = "com.quietanswerer.action.SEND_REPLY"
    const val SMS_SENT = "com.quietanswerer.action.SMS_SENT"
    const val TIMER_FIRED = "com.quietanswerer.action.TIMER_FIRED"
    const val WIDGET_TOGGLE = "com.quietanswerer.widget.ACTION_TOGGLE"
    const val WIDGET_OPEN = "com.quietanswerer.widget.ACTION_OPEN"
    const val WIDGET_PREV_STATUS = "com.quietanswerer.widget.ACTION_PREV_STATUS"
    const val WIDGET_NEXT_STATUS = "com.quietanswerer.widget.ACTION_NEXT_STATUS"

    const val EXTRA_CALL_ID = "extra_call_id"
    const val EXTRA_REPLY_ID = "extra_reply_id"
    const val EXTRA_TAB = "extra_tab"
}

object Extras {
    const val TAB_MESSAGE = 0
    const val TAB_LOG = 1
    const val TAB_VIP = 2
}