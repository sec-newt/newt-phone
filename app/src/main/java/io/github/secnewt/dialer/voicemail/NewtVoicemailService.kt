package io.github.secnewt.dialer.voicemail

import android.telecom.PhoneAccountHandle
import android.telephony.VisualVoicemailService
import android.telephony.VisualVoicemailSms
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Android binds this while Newt Phone is the default phone app: when the phone connects to the
 * network, and when a visual voicemail text arrives from Ting. Does nothing while the setting is off.
 */
class NewtVoicemailService : VisualVoicemailService() {

    // This runs in the phone app's process, so an unexpected error becomes a status line, never a crash.
    private val onError = CoroutineExceptionHandler { _, e ->
        VoicemailStore(this).addLine(VvmText.failed(e.javaClass.simpleName), System.currentTimeMillis())
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default + onError)
    private val activator by lazy { VoicemailActivator(this, VoicemailStore(this)) }

    override fun onCellServiceConnected(task: VisualVoicemailTask, phoneAccountHandle: PhoneAccountHandle) {
        work(task) { activator.start() }
    }

    override fun onSmsReceived(task: VisualVoicemailTask, sms: VisualVoicemailSms) {
        val fields = sms.fields
        val map = fields.keySet().associateWith { fields.getString(it).orEmpty() }
        work(task) { activator.onSms(sms.prefix, map) }
    }

    override fun onSimRemoved(task: VisualVoicemailTask, phoneAccountHandle: PhoneAccountHandle) {
        task.finish()
    }

    override fun onStopped(task: VisualVoicemailTask) {
        // Android ran out of patience; work in progress just stops.
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    /** Runs [block] and always tells Android the task is done, even if it fails. */
    private fun work(task: VisualVoicemailTask, block: suspend () -> Unit) {
        scope.launch {
            try {
                block()
            } finally {
                task.finish()
            }
        }
    }
}
