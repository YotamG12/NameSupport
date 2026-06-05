package com.namesupport.data

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.provider.ContactsContract
import android.util.Log
import com.namesupport.model.ContactItem
import com.namesupport.util.GeminiTransliterator
import com.namesupport.util.HebrewTransliterator

class ContactRepository(private val context: Context) {

    companion object {
        private const val TAG = "ContactRepository"
    }

    suspend fun getHebrewContactsWithoutPhonetic(
        gemini: GeminiTransliterator? = null,
    ): List<ContactItem> {
        val rawContacts = mutableListOf<Pair<Long, String>>()

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
                        rawContacts.add(id to name)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Scan failed", e)
        }

        if (rawContacts.isEmpty()) return emptyList()

        val names = rawContacts.map { it.second }
        val suggestions = if (gemini != null) {
            val result = gemini.transliterateAll(names)
            if (result.size == names.size) result
            else names.map { HebrewTransliterator.transliterate(it) }
        } else {
            names.map { HebrewTransliterator.transliterate(it) }
        }

        return rawContacts.mapIndexed { i, (id, name) ->
            ContactItem(id, name, suggestions[i])
        }
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
