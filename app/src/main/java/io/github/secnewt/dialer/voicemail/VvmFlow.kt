package io.github.secnewt.dialer.voicemail

/** Ting's STATUS reply. Field names are OMTP's (see AOSP OmtpConstants). */
data class StatusReply(
    /** st: N new, R ready, P provisioned, U unknown, B blocked. */
    val provisioning: String,
    /** rc: 0 success, anything else an error code. */
    val returnCode: String,
    val host: String?,
    val imapPort: Int,
    val user: String?,
    val password: String?,
) {
    // Never print the login, even in a crash report or a log.
    override fun toString(): String =
        "StatusReply(st=$provisioning, rc=$returnCode, host=$host, imapPort=$imapPort)"

    companion object {
        fun from(fields: Map<String, String>): StatusReply = StatusReply(
            provisioning = fields["st"].orEmpty(),
            returnCode = fields["rc"].orEmpty(),
            host = fields["srv"]?.takeIf { it.isNotBlank() },
            imapPort = fields["ipt"]?.toIntOrNull() ?: 143,
            user = fields["u"],
            password = fields["pw"],
        )
    }
}

/** What to do next. */
sealed interface VvmAction {
    data object SendStatus : VvmAction
    data object SendActivate : VvmAction
    data class Login(val reply: StatusReply) : VvmAction
    data class Stop(val line: String) : VvmAction
}

fun startAction(isPhoneApp: Boolean, hasSim: Boolean, config: VvmConfig?, simOperator: String?): VvmAction = when {
    !isPhoneApp -> VvmAction.Stop(VvmText.NOT_PHONE_APP)
    !hasSim -> VvmAction.Stop(VvmText.NO_SIM)
    config == null -> VvmAction.Stop(VvmText.noConfig(simOperator))
    else -> VvmAction.SendStatus
}

/** Asks Ting to turn visual voicemail on at most once each time the setting is turned on. */
fun actionForStatus(reply: StatusReply, activateAlreadySent: Boolean): VvmAction = when (reply.provisioning) {
    "B" -> VvmAction.Stop(VvmText.BLOCKED)
    // A non-zero rc with R still tries the login: what the server says tells more than the code.
    "R" -> if (reply.host == null) VvmAction.Stop(VvmText.NO_SERVER) else VvmAction.Login(reply)
    else -> if (activateAlreadySent) VvmAction.Stop(VvmText.STILL_NOT_READY) else VvmAction.SendActivate
}

/** A new message or mailbox change: ask again, which brings fresh login details. */
fun actionForSync(event: String?): VvmAction = when (event) {
    "NM", "MBU" -> VvmAction.SendStatus
    else -> VvmAction.Stop("Ting sent an update: $event.")
}

/** The status lines shown in Settings while visual voicemail is being tried out. */
object VvmText {
    const val OFF = "Visual voicemail is off."
    const val NOT_PHONE_APP = "Make Newt Phone your phone app first."
    const val NO_SIM = "No SIM card found."
    fun noConfig(op: String?) = "This SIM has no visual voicemail settings (network ${op ?: "unknown"})."
    fun config(c: VvmConfig) = "Carrier settings: ${c.type}, number ${c.destinationNumber}, port ${c.port} (" +
        (if (c.source == ConfigSource.SIM) "from the SIM" else "T-Mobile defaults") + ")."
    const val STATUS_SENT = "Asked Ting for voicemail details (text sent)."
    const val ACTIVATE_SENT = "Asked Ting to turn on visual voicemail (text sent)."
    fun sendFailed(why: String) = "Couldn't send the text to Ting: $why"
    fun replied(r: StatusReply) = "Ting answered: status ${r.provisioning}, code ${r.returnCode}."
    const val BLOCKED = "Ting says visual voicemail is blocked on this line."
    const val STILL_NOT_READY = "Ting still says visual voicemail isn't ready. Turn the setting off and on to try again later."
    const val NO_SERVER = "Ting answered but didn't say which server to use."
    const val CONNECTING = "Connecting over mobile data…"
    const val NO_DATA = "No mobile data connection (waited 30 seconds)."
    const val NETWORK_PERMISSION = "Can't connect: Newt Phone's Network permission is off in Android settings."
    fun loggedIn(count: Int) = "Logged in. $count voicemail${if (count == 1) "" else "s"} on Ting's server."
    fun loginRefused(server: String) = "Ting refused the login: $server"
    fun failed(why: String) = "Connection failed: $why"
    const val WAITING = "Ting hasn't answered the text yet."
    const val NEW_VOICEMAIL = "Ting says there's a new voicemail."
}
