package io.github.secnewt.dialer.contacts

import android.Manifest
import android.app.NotificationManager
import android.app.NotificationManager.Policy
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.Contacts

/**
 * Reads contacts and stars them in the phone's own contacts, so the star shows up in
 * every app and Do Not Disturb's "Starred contacts" exception honors it.
 */
class ContactsRepository(private val context: Context) {

    fun hasAccess(): Boolean = PERMISSIONS.all {
        context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED
    }

    fun load(): List<Contact> {
        val resolver = context.contentResolver
        val phones = mutableMapOf<Long, MutableList<PhoneEntry>>()
        resolver.query(
            Phone.CONTENT_URI,
            arrayOf(Phone.CONTACT_ID, Phone.NUMBER, Phone.TYPE, Phone.LABEL),
            null, null, null,
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val number = cursor.getString(1) ?: continue
                val label = Phone.getTypeLabel(context.resources, cursor.getInt(2), cursor.getString(3)).toString()
                phones.getOrPut(cursor.getLong(0)) { mutableListOf() }.add(PhoneEntry(number, label))
            }
        }
        val contacts = mutableListOf<Contact>()
        resolver.query(
            Contacts.CONTENT_URI,
            arrayOf(
                Contacts._ID,
                Contacts.DISPLAY_NAME_PRIMARY,
                Contacts.STARRED,
                Contacts.PHOTO_URI,
                Contacts.PHOTO_THUMBNAIL_URI,
            ),
            null, null,
            "${Contacts.DISPLAY_NAME_PRIMARY} COLLATE LOCALIZED ASC",
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getLong(0)
                val name = cursor.getString(1)?.takeIf { it.isNotBlank() } ?: continue
                contacts += Contact(
                    id = id,
                    name = name,
                    starred = cursor.getInt(2) == 1,
                    phones = ContactList.distinctPhones(phones[id].orEmpty()),
                    photoUri = cursor.getString(3),
                    thumbnailUri = cursor.getString(4),
                )
            }
        }
        return contacts
    }

    /** Returns false if the change could not be saved (for example, the contact is out of scope). */
    fun setStarred(id: Long, starred: Boolean): Boolean = try {
        val values = ContentValues().apply { put(Contacts.STARRED, if (starred) 1 else 0) }
        context.contentResolver.update(ContentUris.withAppendedId(Contacts.CONTENT_URI, id), values, null, null) > 0
    } catch (e: Exception) {
        false
    }

    /** Which calls Do Not Disturb lets ring. Reading this needs no extra permission. */
    fun dndCalls(): DndCalls = try {
        val policy = context.getSystemService(NotificationManager::class.java).notificationPolicy
        when {
            policy.priorityCategories and Policy.PRIORITY_CATEGORY_CALLS == 0 -> DndCalls.NONE
            policy.priorityCallSenders == Policy.PRIORITY_SENDERS_STARRED -> DndCalls.STARRED
            policy.priorityCallSenders == Policy.PRIORITY_SENDERS_CONTACTS -> DndCalls.CONTACTS
            policy.priorityCallSenders == Policy.PRIORITY_SENDERS_ANY -> DndCalls.ANYONE
            else -> DndCalls.UNKNOWN
        }
    } catch (e: Exception) {
        DndCalls.UNKNOWN
    }

    companion object {
        val PERMISSIONS = arrayOf(Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS)
    }
}
