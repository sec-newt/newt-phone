package io.github.secnewt.dialer.contacts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ContactListTest {

    private val mom = Contact(1, "Mom", starred = true, phones = listOf(PhoneEntry("(555) 019-7731", "Mobile")))
    private val alex = Contact(2, "alex Rivera", starred = true, phones = listOf(PhoneEntry("+1 555 014 2290", "Work")))
    private val pharmacy = Contact(3, "Pharmacy", starred = false, phones = listOf(PhoneEntry("800-555-0000", "Main")))
    private val all = listOf(mom, pharmacy, alex)

    @Test
    fun `favorites are starred contacts in alphabetical order, ignoring case`() {
        assertEquals(listOf(alex, mom), ContactList.favorites(all))
    }

    @Test
    fun `blank search shows everyone`() {
        assertEquals(all, ContactList.search(all, "  "))
    }

    @Test
    fun `search matches any part of a name, ignoring case`() {
        assertEquals(listOf(alex), ContactList.search(all, "rIVer"))
        assertEquals(listOf(pharmacy), ContactList.search(all, "pharm"))
    }

    @Test
    fun `search matches number digits whatever the formatting`() {
        assertEquals(listOf(mom), ContactList.search(all, "019-77"))
        assertEquals(listOf(alex), ContactList.search(all, "5550142290"))
        assertEquals(listOf(alex), ContactList.search(all, "+1 (555) 014-2290"))
    }

    @Test
    fun `one or two digits don't match every number`() {
        assertEquals(emptyList<Contact>(), ContactList.search(all, "55"))
    }

    @Test
    fun `the same number saved twice is shown once`() {
        val phones = listOf(PhoneEntry("555-019-7731", "Mobile"), PhoneEntry("+1 (555) 019-7731", "Other"))
        assertEquals(listOf(phones[0]), ContactList.distinctPhones(phones))
    }

    @Test
    fun `starring changes only that contact`() {
        val updated = ContactList.withStar(all, id = 3, starred = true)
        assertTrue(updated.first { it.id == 3L }.starred)
        assertEquals(listOf(mom, alex), updated.filter { it.id != 3L })
    }

    @Test
    fun `do not disturb set to starred contacts needs no change`() {
        assertFalse(Dnd.advice(DndCalls.STARRED).suggestSettings)
        listOf(DndCalls.CONTACTS, DndCalls.ANYONE, DndCalls.NONE, DndCalls.UNKNOWN).forEach {
            assertTrue(Dnd.advice(it).suggestSettings)
        }
    }

    @Test
    fun `a contact's page says whether they ring during do not disturb`() {
        assertEquals("Can ring during Do Not Disturb", Dnd.contactLine(starred = true, DndCalls.STARRED))
        assertEquals("Silenced during Do Not Disturb", Dnd.contactLine(starred = false, DndCalls.STARRED))
        assertEquals("Silenced during Do Not Disturb", Dnd.contactLine(starred = true, DndCalls.NONE))
        assertEquals("Can ring during Do Not Disturb", Dnd.contactLine(starred = false, DndCalls.CONTACTS))
        assertNull(Dnd.contactLine(starred = true, DndCalls.UNKNOWN))
    }
}
