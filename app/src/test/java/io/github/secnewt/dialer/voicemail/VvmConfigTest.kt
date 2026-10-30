package io.github.secnewt.dialer.voicemail

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VvmConfigTest {

    @Test
    fun `cvvm from the SIM`() {
        val c = vvmConfigOf(
            mapOf("vvm_type_string" to "vvm_type_cvvm", "vvm_destination_number_string" to "122", "vvm_port_number_int" to 1808),
            "310260",
        )!!
        assertEquals(VvmConfig(VvmType.CVVM, "122", 1808, false, ConfigSource.SIM), c)
        assertEquals("Activate:dt=6", c.activateText())
        assertEquals("Deactivate:dt=6", c.deactivateText())
        assertEquals("Status:dt=6", c.statusText())
    }

    @Test
    fun `omtp texts`() {
        val c = vvmConfigOf(
            mapOf("vvm_type_string" to "vvm_type_omtp", "vvm_destination_number_string" to "5551234", "vvm_port_number_int" to 0),
            null,
        )!!
        assertEquals("Activate:pv=11;ct=newt.phone", c.activateText())
        assertEquals("Deactivate:pv=11;ct=newt.phone", c.deactivateText())
        assertEquals("STATUS", c.statusText())
    }

    @Test
    fun `empty SIM config on a T-Mobile network falls back to T-Mobile defaults`() {
        assertEquals(
            VvmConfig(VvmType.CVVM, "122", 1808, false, ConfigSource.TMOBILE_DEFAULTS),
            vvmConfigOf(emptyMap(), "310260"),
        )
    }

    @Test
    fun `blank values from Android count as no settings`() {
        val blank = mapOf("vvm_type_string" to "", "vvm_destination_number_string" to "", "vvm_port_number_int" to 0, "vvm_ssl_enabled_bool" to false)
        assertEquals(ConfigSource.TMOBILE_DEFAULTS, vvmConfigOf(blank, "310260")?.source)
        assertNull(vvmConfigOf(blank, "311480"))
    }

    @Test
    fun `empty config elsewhere is null`() {
        assertNull(vvmConfigOf(emptyMap(), "311480"))
        assertNull(vvmConfigOf(emptyMap(), null))
    }

    @Test
    fun `unknown type such as vvm3 is null`() {
        assertNull(
            vvmConfigOf(mapOf("vvm_type_string" to "vvm_type_vvm3", "vvm_destination_number_string" to "900080006200"), "311480"),
        )
    }

    @Test
    fun `missing destination number is null even with a type`() {
        assertNull(vvmConfigOf(mapOf("vvm_type_string" to "vvm_type_cvvm"), "311480"))
    }
}
