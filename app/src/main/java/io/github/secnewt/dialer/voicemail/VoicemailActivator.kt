package io.github.secnewt.dialer.voicemail

import android.app.role.RoleManager
import android.content.Context
import android.os.PersistableBundle
import android.telephony.CarrierConfigManager
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.telephony.VisualVoicemailSmsFilterSettings
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.InetSocketAddress
import java.net.Socket
import javax.net.ssl.SSLSocketFactory
import kotlinx.coroutines.CancellationException

private const val SOCKET_TIMEOUT_MILLIS = 20_000

/**
 * Turns visual voicemail on with Ting by text, then checks the login to Ting's voicemail server
 * over mobile data, writing each step to [store] as a plain status line.
 */
class VoicemailActivator(private val context: Context, private val store: VoicemailStore) {

    /** What the phone knows about the voice SIM right now. */
    private class Line(
        val telephony: TelephonyManager,
        val subId: Int,
        val isPhoneApp: Boolean,
        val hasSim: Boolean,
        val operator: String?,
        val config: VvmConfig?,
    )

    private fun line(): Line {
        var subId = SubscriptionManager.getDefaultVoiceSubscriptionId()
        if (subId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) subId = SubscriptionManager.getDefaultSubscriptionId()
        val telephony = context.getSystemService(TelephonyManager::class.java).createForSubscriptionId(subId)
        val operator = telephony.simOperator?.takeIf { it.isNotBlank() }
        val carrierConfig = try {
            context.getSystemService(CarrierConfigManager::class.java)?.getConfigForSubId(subId)
        } catch (_: SecurityException) {
            null
        }
        return Line(
            telephony = telephony,
            subId = subId,
            isPhoneApp = context.getSystemService(RoleManager::class.java).isRoleHeld(RoleManager.ROLE_DIALER),
            hasSim = telephony.simState == TelephonyManager.SIM_STATE_READY,
            operator = operator,
            config = vvmConfigOf(carrierConfig.vvmValues(), operator),
        )
    }

    private fun log(text: String) = store.addLine(text, System.currentTimeMillis())

    /** Asks Ting for the voicemail details. Called when the setting is turned on and when the phone connects. */
    suspend fun start() {
        if (!store.enabled) return
        val line = line()
        line.config?.let { log(VvmText.config(it)) }
        run(startAction(line.isPhoneApp, line.hasSim, line.config, line.operator), line)
    }

    /** A visual voicemail text from Ting, already split into fields by Android. */
    suspend fun onSms(prefix: String, fields: Map<String, String>) {
        if (!store.enabled) return
        val line = line()
        when (prefix) {
            "STATUS" -> {
                val reply = StatusReply.from(fields)
                log(VvmText.replied(reply))
                run(actionForStatus(reply, store.activateSent), line)
            }
            "SYNC" -> {
                log(VvmText.NEW_VOICEMAIL)
                run(actionForSync(fields["ev"]), line)
            }
        }
    }

    /** Tells Ting to stop, so ordinary voicemail alerts come back, and forgets the login. */
    fun stop() {
        val line = line()
        line.config?.let { config ->
            try {
                line.telephony.sendVisualVoicemailSms(config.destinationNumber, config.port, config.deactivateText(), null)
            } catch (_: RuntimeException) {
                // Not the phone app any more, or no SIM: nothing to tell Ting.
            }
        }
        try {
            line.telephony.setVisualVoicemailSmsFilterSettings(null)
        } catch (_: RuntimeException) {
        }
        store.turnOff()
    }

    private suspend fun run(action: VvmAction, line: Line) {
        when (action) {
            is VvmAction.Stop -> log(action.line)
            VvmAction.SendStatus -> send(line, VvmText.STATUS_SENT) { it.statusText() }
            VvmAction.SendActivate -> {
                send(line, VvmText.ACTIVATE_SENT) { it.activateText() }
                store.activateSent = true
            }
            is VvmAction.Login -> login(action.reply, line)
        }
    }

    private fun send(line: Line, sentText: String, text: (VvmConfig) -> String) {
        val config = line.config ?: return log(VvmText.noConfig(line.operator))
        try {
            line.telephony.setVisualVoicemailSmsFilterSettings(
                VisualVoicemailSmsFilterSettings.Builder()
                    .setClientPrefix(VVM_CLIENT_PREFIX)
                    .setDestinationPort(VisualVoicemailSmsFilterSettings.DESTINATION_PORT_ANY)
                    .build(),
            )
            line.telephony.sendVisualVoicemailSms(config.destinationNumber, config.port, text(config), null)
            log(sentText)
        } catch (e: RuntimeException) {
            log(VvmText.sendFailed(e.javaClass.simpleName))
        }
    }

    private suspend fun login(reply: StatusReply, line: Line) {
        val host = reply.host ?: return log(VvmText.NO_SERVER)
        store.saveCredentials(reply)
        log(VvmText.CONNECTING)
        val result = try {
            withCellularNetwork(context, line.subId) { network ->
                // Look the server up on mobile data too: Ting's server may only exist inside its network.
                val address = InetSocketAddress(network.getByName(host), reply.imapPort)
                val plain = network.socketFactory.createSocket()
                plain.connect(address, SOCKET_TIMEOUT_MILLIS)
                val socket: Socket = if (line.config?.ssl == true) {
                    (SSLSocketFactory.getDefault() as SSLSocketFactory).createSocket(plain, host, reply.imapPort, true)
                } else {
                    plain
                }
                socket.use {
                    it.soTimeout = SOCKET_TIMEOUT_MILLIS
                    probeMailbox(
                        BufferedReader(InputStreamReader(it.getInputStream(), Charsets.UTF_8)),
                        OutputStreamWriter(it.getOutputStream(), Charsets.UTF_8),
                        reply.user.orEmpty(),
                        reply.password.orEmpty(),
                    )
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return log(connectFailureLine(e))
        }
        log(if (result == null) VvmText.NO_DATA else probeLine(result))
    }
}

/** The carrier config keys [vvmConfigOf] reads, as a plain map. */
private fun PersistableBundle?.vvmValues(): Map<String, Any?> {
    if (this == null) return emptyMap()
    return mapOf(
        "vvm_type_string" to getString(CarrierConfigManager.KEY_VVM_TYPE_STRING),
        "vvm_destination_number_string" to getString(CarrierConfigManager.KEY_VVM_DESTINATION_NUMBER_STRING),
        "vvm_port_number_int" to getInt(CarrierConfigManager.KEY_VVM_PORT_NUMBER_INT),
        "vvm_ssl_enabled_bool" to getBoolean(CarrierConfigManager.KEY_VVM_SSL_ENABLED_BOOL),
    ).filterValues { it != null }
}
