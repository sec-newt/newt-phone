package io.github.secnewt.dialer.contacts

import io.github.secnewt.dialer.screening.PhoneNumbers

/** One phone number on a contact, with its label such as "Mobile" or "Work". */
data class PhoneEntry(val number: String, val label: String)

/** A contact as shown in Favorites and Contacts. Starred contacts are the favorites. */
data class Contact(
    val id: Long,
    val name: String,
    val starred: Boolean,
    val phones: List<PhoneEntry> = emptyList(),
    /** Full-size photo, for Favorites tiles. */
    val photoUri: String? = null,
    /** Small photo, for lists. */
    val thumbnailUri: String? = null,
)

/** Sorting, searching and starring rules, kept free of Android so they can be tested. */
object ContactList {

    /** Starred contacts, alphabetical. */
    fun favorites(contacts: List<Contact>): List<Contact> =
        contacts.filter { it.starred }.sortedBy { it.name.lowercase() }

    /**
     * Contacts whose name contains the search, or whose number contains its digits.
     * Numbers are only searched once at least 3 digits are typed, so "1" doesn't match everyone.
     */
    fun search(contacts: List<Contact>, query: String): List<Contact> {
        val text = query.trim()
        if (text.isEmpty()) return contacts
        val digits = text.filter { it.isDigit() }
        return contacts.filter { contact ->
            contact.name.contains(text, ignoreCase = true) ||
                (digits.length >= 3 && contact.phones.any { phone ->
                    PhoneNumbers.normalize(phone.number)?.contains(PhoneNumbers.normalize(digits) ?: digits) == true
                })
        }
    }

    /** The same number saved twice ("555-0197" and "+1 555 0197") is shown once. */
    fun distinctPhones(phones: List<PhoneEntry>): List<PhoneEntry> =
        phones.distinctBy { PhoneNumbers.normalize(it.number) ?: it.number }

    /** The letter shown when a contact has no photo. */
    fun initial(name: String): String = name.firstOrNull { it.isLetterOrDigit() }?.uppercase() ?: "#"

    /**
     * Which of [count] colors a contact without a photo gets. Based on the name, so a person
     * keeps the same color everywhere and every time.
     */
    fun colorIndex(name: String, count: Int): Int {
        var hash = 0
        for (c in name.lowercase()) hash = hash * 31 + c.code
        return Math.floorMod(hash, count)
    }

    /** Applies a star change locally, so the screen updates without reloading every contact. */
    fun withStar(contacts: List<Contact>, id: Long, starred: Boolean): List<Contact> =
        contacts.map { if (it.id == id) it.copy(starred = starred) else it }
}

/** Which calls Do Not Disturb lets ring, read from the phone's settings. */
enum class DndCalls { STARRED, CONTACTS, ANYONE, NONE, UNKNOWN }

/** What the Favorites screen says about Do Not Disturb. */
data class DndAdvice(val message: String, val suggestSettings: Boolean)

object Dnd {
    fun advice(calls: DndCalls): DndAdvice = when (calls) {
        DndCalls.STARRED -> DndAdvice("During Do Not Disturb, only your favorites can ring.", suggestSettings = false)
        DndCalls.CONTACTS -> DndAdvice(
            "During Do Not Disturb, all your contacts can ring, not just favorites. " +
                "To allow only favorites, choose Starred contacts for calls.",
            suggestSettings = true,
        )
        DndCalls.ANYONE -> DndAdvice(
            "During Do Not Disturb, every call can ring. " +
                "To allow only favorites, choose Starred contacts for calls.",
            suggestSettings = true,
        )
        DndCalls.NONE -> DndAdvice(
            "During Do Not Disturb, no calls ring, not even favorites. " +
                "To let favorites ring, choose Starred contacts for calls.",
            suggestSettings = true,
        )
        DndCalls.UNKNOWN -> DndAdvice(
            "To let favorites ring during Do Not Disturb, choose Starred contacts for calls in its settings.",
            suggestSettings = true,
        )
    }

    /** The short line on a contact's page about whether they can ring during Do Not Disturb. */
    fun contactLine(starred: Boolean, calls: DndCalls): String? = when (calls) {
        DndCalls.STARRED -> if (starred) "Can ring during Do Not Disturb" else "Silenced during Do Not Disturb"
        DndCalls.CONTACTS, DndCalls.ANYONE -> "Can ring during Do Not Disturb"
        DndCalls.NONE -> "Silenced during Do Not Disturb"
        DndCalls.UNKNOWN -> null
    }
}
