package io.github.secnewt.dialer.screening

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PhoneNumbersTest {

    @Test
    fun `normalize strips formatting and the US country code`() {
        assertEquals("5550142290", PhoneNumbers.normalize("+1 (555) 014-2290"))
        assertEquals("5550142290", PhoneNumbers.normalize("555.014.2290"))
        assertEquals("5550142290", PhoneNumbers.normalize("15550142290"))
    }

    @Test
    fun `normalize keeps other numbers as plain digits`() {
        assertEquals("442071234567", PhoneNumbers.normalize("+44 20 7123 4567"))
        assertEquals("911", PhoneNumbers.normalize("911"))
    }

    @Test
    fun `normalize returns null for hidden or empty numbers`() {
        assertNull(PhoneNumbers.normalize(null))
        assertNull(PhoneNumbers.normalize(""))
        assertNull(PhoneNumbers.normalize("Private"))
    }

    @Test
    fun `copycat matches the first six digits of my number`() {
        assertTrue(PhoneNumbers.looksLikeCopycat("555-014-2290", "+1 555 014 9999"))
    }

    @Test
    fun `my exact number calling me is a copycat`() {
        assertTrue(PhoneNumbers.looksLikeCopycat("5550142290", "+15550142290"))
    }

    @Test
    fun `same area code alone is not a copycat`() {
        assertFalse(PhoneNumbers.looksLikeCopycat("5550142290", "5559871234"))
    }

    @Test
    fun `copycat check ignores hidden and short numbers`() {
        assertFalse(PhoneNumbers.looksLikeCopycat("5550142290", null))
        assertFalse(PhoneNumbers.looksLikeCopycat("5550142290", "555014"))
        assertFalse(PhoneNumbers.looksLikeCopycat(null, "5550142290"))
    }
}
