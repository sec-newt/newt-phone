package io.github.secnewt.dialer.calls

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ContactSearchRequestTest {

    private val dial = "android.intent.action.DIAL"

    @Test
    fun `a dial request with a name searches contacts`() {
        assertEquals("Mom", ContactSearchRequest.query(dial, hasNumber = false, query = "  Mom "))
    }

    @Test
    fun `anything else is not a search`() {
        assertNull(ContactSearchRequest.query(dial, hasNumber = false, query = null))
        assertNull(ContactSearchRequest.query(dial, hasNumber = false, query = "   "))
        // A number wins: the dialpad opens with it, as before.
        assertNull(ContactSearchRequest.query(dial, hasNumber = true, query = "Mom"))
        assertNull(ContactSearchRequest.query("android.intent.action.VIEW", hasNumber = false, query = "Mom"))
    }

    @Test
    fun `very long text is cut short`() {
        assertEquals(100, ContactSearchRequest.query(dial, hasNumber = false, query = "a".repeat(500))?.length)
    }
}
