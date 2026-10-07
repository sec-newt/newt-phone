package io.github.secnewt.dialer.incall

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Answer, Decline and Hang up from the call notification. Only this app can send these. */
class CallActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra(EXTRA_CALL_ID) ?: return
        when (intent.action) {
            ANSWER -> CallManager.answer(id)
            DECLINE -> CallManager.decline(id)
            HANG_UP -> CallManager.hangUp(id)
        }
    }

    companion object {
        const val ANSWER = "io.github.secnewt.dialer.ANSWER"
        const val DECLINE = "io.github.secnewt.dialer.DECLINE"
        const val HANG_UP = "io.github.secnewt.dialer.HANG_UP"
        const val EXTRA_CALL_ID = "call_id"
    }
}
