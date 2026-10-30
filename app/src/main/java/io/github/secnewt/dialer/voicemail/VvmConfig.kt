package io.github.secnewt.dialer.voicemail

/** The two visual voicemail dialects a carrier can speak. T-Mobile (and so Ting) uses CVVM. */
enum class VvmType { OMTP, CVVM }

/** Where the settings came from: the SIM's carrier config, or the T-Mobile values built in here. */
enum class ConfigSource { SIM, TMOBILE_DEFAULTS }

/** Every visual voicemail text from Ting starts with this, so Android can hand it to the app. */
const val VVM_CLIENT_PREFIX = "//VVM"

/** Sent as "ct" in OMTP requests; only used if a SIM ever reports plain OMTP. */
private const val OMTP_CLIENT_TYPE = "newt.phone"

/**
 * T-Mobile US networks. Android has no Ting-specific settings, so a legacy Ting SIM on one of
 * these with no visual voicemail settings gets T-Mobile's (from AOSP's T-Mobile carrier config).
 */
val TMOBILE_US_OPERATORS: Set<String> = setOf(
    "310160", "310200", "310210", "310220", "310230", "310240", "310250",
    "310260", "310270", "310310", "310490", "310660", "310800",
)

private val TMOBILE_DEFAULTS = VvmConfig(VvmType.CVVM, "122", 1808, false, ConfigSource.TMOBILE_DEFAULTS)

/** How to reach the carrier's visual voicemail system by text. */
data class VvmConfig(
    val type: VvmType,
    val destinationNumber: String,
    /** Data-SMS port; 0 sends a plain text. */
    val port: Int,
    val ssl: Boolean,
    val source: ConfigSource,
) {
    // Formats from AOSP's OmtpCvvmMessageSender and OmtpStandardMessageSender (OMTP v1.1).
    fun activateText(): String = when (type) {
        VvmType.CVVM -> "Activate:dt=6"
        VvmType.OMTP -> "Activate:pv=11;ct=$OMTP_CLIENT_TYPE"
    }

    fun deactivateText(): String = when (type) {
        VvmType.CVVM -> "Deactivate:dt=6"
        VvmType.OMTP -> "Deactivate:pv=11;ct=$OMTP_CLIENT_TYPE"
    }

    fun statusText(): String = when (type) {
        VvmType.CVVM -> "Status:dt=6"
        VvmType.OMTP -> "STATUS"
    }
}

/**
 * Reads the carrier config keys (`vvm_type_string`, `vvm_destination_number_string`,
 * `vvm_port_number_int`, `vvm_ssl_enabled_bool`), or null when there's nothing usable.
 */
fun vvmConfigOf(values: Map<String, Any?>, simOperator: String?): VvmConfig? {
    val type = when (values["vvm_type_string"] as? String) {
        "vvm_type_cvvm" -> VvmType.CVVM
        "vvm_type_omtp" -> VvmType.OMTP
        else -> null
    }
    val number = (values["vvm_destination_number_string"] as? String)?.takeIf { it.isNotBlank() }
    if (type != null && number != null) {
        return VvmConfig(
            type = type,
            destinationNumber = number,
            port = values["vvm_port_number_int"] as? Int ?: 0,
            ssl = values["vvm_ssl_enabled_bool"] as? Boolean ?: false,
            source = ConfigSource.SIM,
        )
    }
    // A type Newt Phone doesn't speak (such as Verizon's vvm3) is not a missing config.
    if (type == null && values["vvm_type_string"] != null) return null
    return if (simOperator in TMOBILE_US_OPERATORS) TMOBILE_DEFAULTS else null
}
