package com.namesupport.data

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.provider.ContactsContract
import android.util.Log
import com.namesupport.model.ContactItem
import com.namesupport.util.HebrewTransliterator

class ContactRepository(private val context: Context) {

    companion object {
        private const val TAG = "ContactRepository"
    }

    fun getHebrewContactsWithoutPhonetic(): List<ContactItem> {
        val contacts = mutableListOf<ContactItem>()

        // DISPLAY_NAME is the safe alias; DISPLAY_NAME_PRIMARY is unavailable on some builds
        val nameCol = ContactsContract.Contacts.DISPLAY_NAME
        val projection = arrayOf(ContactsContract.Contacts._ID, nameCol)

        try {
            context.contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                projection,
                "$nameCol IS NOT NULL",
                null,
                "$nameCol ASC",
            )?.use { cursor ->
                Log.d(TAG, "Total contacts in cursor: ${cursor.count}")
                val idIdx = cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID)
                val nameIdx = cursor.getColumnIndexOrThrow(nameCol)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idIdx)
                    val name = cursor.getString(nameIdx) ?: continue
                    val isHebrew = HebrewTransliterator.containsHebrew(name)
                    val hasPhonetic = hasPhoneticName(id)
                    Log.d(TAG, "Contact: '$name' hebrew=$isHebrew phonetic=$hasPhonetic")

                    if (isHebrew && !hasPhonetic) {
                        contacts.add(
                            ContactItem(
                                id = id,
                                displayName = name,
                                suggestion = HebrewTransliterator.transliterate(name),
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Scan failed", e)
        }

        return contacts
    }

    private fun hasPhoneticName(contactId: Long): Boolean {
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.StructuredName.PHONETIC_GIVEN_NAME,
            ContactsContract.CommonDataKinds.StructuredName.PHONETIC_FAMILY_NAME,
        )
        val selection =
            "${ContactsContract.Data.CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?"
        val args = arrayOf(
            contactId.toString(),
            ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE,
        )

        context.contentResolver.query(
            ContactsContract.Data.CONTENT_URI,
            projection,
            selection,
            args,
            null,
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val given = cursor.getString(0)
                val family = cursor.getString(1)
                if (!given.isNullOrBlank() || !family.isNullOrBlank()) return true
            }
        }
        return false
    }

    fun applyPhoneticName(contact: ContactItem): Boolean {
        return try {
            val rawIds = getRawContactIds(contact.id)
            if (rawIds.isEmpty()) return false
            rawIds.any { rawId ->
                updateOrInsertPhoneticName(rawId, contact.displayName, contact.suggestion)
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun getRawContactIds(contactId: Long): List<Long> {
        val ids = mutableListOf<Long>()
        context.contentResolver.query(
            ContactsContract.RawContacts.CONTENT_URI,
            arrayOf(ContactsContract.RawContacts._ID),
            "${ContactsContract.RawContacts.CONTACT_ID} = ? AND " +
                    "${ContactsContract.RawContacts.DELETED} = 0",
            arrayOf(contactId.toString()),
            null,
        )?.use { cursor ->
            while (cursor.moveToNext()) ids.add(cursor.getLong(0))
        }
        return ids
    }

    private fun updateOrInsertPhoneticName(
        rawContactId: Long,
        displayName: String,
        phoneticName: String,
    ): Boolean {
        val selection =
            "${ContactsContract.Data.RAW_CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?"
        val args = arrayOf(
            rawContactId.toString(),
            ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE,
        )

        // Look for an existing StructuredName row for this raw contact
        val existingDataId: Long? = context.contentResolver.query(
            ContactsContract.Data.CONTENT_URI,
            arrayOf(ContactsContract.Data._ID),
            selection,
            args,
            null,
        )?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getLong(0) else null
        }

        return if (existingDataId != null) {
            // Update the phonetic field on the existing row
            val values = ContentValues().apply {
                put(
                    ContactsContract.CommonDataKinds.StructuredName.PHONETIC_GIVEN_NAME,
                    phoneticName,
                )
            }
            context.contentResolver.update(
                ContentUris.withAppendedId(ContactsContract.Data.CONTENT_URI, existingDataId),
                values,
                null,
                null,
            ) > 0
        } else {
            // Insert a new StructuredName row with the phonetic field
            val values = ContentValues().apply {
                put(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
                put(
                    ContactsContract.Data.MIMETYPE,
                    ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE,
                )
                put(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, displayName)
                put(
                    ContactsContract.CommonDataKinds.StructuredName.PHONETIC_GIVEN_NAME,
                    phoneticName,
                )
            }
            context.contentResolver.insert(ContactsContract.Data.CONTENT_URI, values) != null
        }
    }
}
