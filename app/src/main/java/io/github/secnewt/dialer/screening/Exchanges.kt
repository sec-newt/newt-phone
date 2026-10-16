package io.github.secnewt.dialer.screening

import android.content.Context
import java.nio.ByteBuffer
import java.util.zip.CRC32

/**
 * Which US exchanges (area code + the next three digits) a phone company actually has, from
 * NANPA's public reports (built by `scripts/build-exchanges.py`). A call from an exchange
 * nobody has can't be real: the number was made up.
 *
 * The file is checked before use; if it's missing or damaged, nothing counts as made up.
 */
class Exchanges private constructor(private val areaCodes: ByteArray, private val assigned: ByteArray, val dataDate: Int) {

    /**
     * True only for a 10-digit number in a US area code the data covers, whose exchange no
     * company has. Other countries, toll-free and anything unexpected are never "made up".
     */
    fun isMadeUp(number: String?): Boolean {
        val digits = PhoneNumbers.normalize(number) ?: return false
        if (digits.length != 10) return false
        val npa = digits.substring(0, 3).toInt()
        if (!bit(areaCodes, npa)) return false
        return !bit(assigned, digits.substring(0, 6).toInt())
    }

    private fun bit(bits: ByteArray, index: Int): Boolean = (bits[index / 8].toInt() shr (index % 8)) and 1 == 1

    companion object {
        const val ASSET = "assigned-exchanges.bin"
        private val MAGIC = "NEWTEXCH".toByteArray(Charsets.US_ASCII)
        private const val AREA_BYTES = 1000 / 8
        private const val EXCHANGE_BYTES = 1_000_000 / 8
        private const val SIZE = 8 + 4 + 4 + AREA_BYTES + EXCHANGE_BYTES + 4

        /** Reads the file's bytes; null when they aren't exactly what the build script writes. */
        fun parse(bytes: ByteArray): Exchanges? {
            if (bytes.size != SIZE) return null
            val buffer = ByteBuffer.wrap(bytes)
            val magic = ByteArray(MAGIC.size).also { buffer.get(it) }
            if (!magic.contentEquals(MAGIC) || buffer.int != 1) return null
            val date = buffer.int
            val crc = CRC32().apply { update(bytes, 0, SIZE - 4) }.value.toInt()
            if (ByteBuffer.wrap(bytes, SIZE - 4, 4).int != crc) return null
            val areas = bytes.copyOfRange(16, 16 + AREA_BYTES)
            val exchanges = bytes.copyOfRange(16 + AREA_BYTES, 16 + AREA_BYTES + EXCHANGE_BYTES)
            return Exchanges(areas, exchanges, date)
        }

        @Volatile private var loaded: Exchanges? = null

        /** The app's copy, read once. Null if it can't be read or fails its check. */
        fun load(context: Context): Exchanges? = loaded ?: try {
            context.assets.open(ASSET).use { parse(it.readBytes()) }?.also { loaded = it }
        } catch (e: Exception) {
            null
        }
    }
}
