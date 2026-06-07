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
        private const val ACCOUNT_TYPE_WHATSAPP = "com.whatsapp"
    }

    private val contentResolver get() = context.contentResolver

    suspend fun getHebrewContactsWithoutPhonetic(): List<ContactItem> {
        val rawContacts = mutableListOf<Pair<Long, String>>()

        val nameCol = ContactsContract.Contacts.DISPLAY_NAME
        val projection = arrayOf(ContactsContract.Contacts._ID, nameCol)

        try {
            contentResolver.query(
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
        val suggestions = names.map { HebrewTransliterator.transliterate(it) }

        return rawContacts.mapIndexed { i, (id, name) ->
            ContactItem(id, name, suggestions[i], isWhatsAppOnlyContact(id))
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

        contentResolver.query(
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

    // Returns true when every raw contact for this aggregate contact belongs to WhatsApp.
    fun isWhatsAppOnlyContact(contactId: Long): Boolean {
        val raws = getRawContacts(contactId)
        if (raws.isEmpty()) return false
        return raws.all { it.accountType == ACCOUNT_TYPE_WHATSAPP }
    }

    fun applyPhoneticName(contact: ContactItem): Boolean {
        return try {
            val rawIds = getRawContactIds(contact.id)
            if (rawIds.isEmpty()) return false
            val wrote = rawIds.any { rawId ->
                updateOrInsertPhoneticName(rawId, contact.displayName, contact.suggestion)
            }
            // If all raw contacts are read-only (e.g. WhatsApp-only), create a local stub
            if (wrote) true else createLocalRawContactWithPhonetic(contact)
        } catch (e: Exception) {
            Log.e(TAG, "applyPhoneticName failed", e)
            false
        }
    }

    // Creates a local (device) raw contact carrying just the phonetic name, then links it
    // to the existing aggregate contact via AggregationExceptions.
    private fun createLocalRawContactWithPhonetic(contact: ContactItem): Boolean {
        val rawValues = ContentValues().apply {
            putNull(ContactsContract.RawContacts.ACCOUNT_TYPE)
            putNull(ContactsContract.RawContacts.ACCOUNT_NAME)
        }
        val rawUri = contentResolver.insert(
            ContactsContract.RawContacts.CONTENT_URI, rawValues
        ) ?: return false
        val newRawId = ContentUris.parseId(rawUri)

        val ok = updateOrInsertPhoneticName(newRawId, contact.displayName, contact.suggestion)
        if (!ok) return false

        val existingRawId = getRawContactIds(contact.id).firstOrNull() ?: return true
        val ex = ContentValues().apply {
            put(ContactsContract.AggregationExceptions.TYPE,
                ContactsContract.AggregationExceptions.TYPE_KEEP_TOGETHER)
            put(ContactsContract.AggregationExceptions.RAW_CONTACT_ID1, existingRawId)
            put(ContactsContract.AggregationExceptions.RAW_CONTACT_ID2, newRawId)
        }
        contentResolver.update(ContactsContract.AggregationExceptions.CONTENT_URI, ex, null, null)
        return true
    }

    // Creates a full local raw contact with name, phonetic name, and phone numbers, then
    // links it to the existing WhatsApp aggregate contact.
    fun saveToDeviceAndApply(contact: ContactItem): Boolean {
        return try {
            val phones = readPhoneNumbers(contact.id)

            val rawValues = ContentValues().apply {
                putNull(ContactsContract.RawContacts.ACCOUNT_TYPE)
                putNull(ContactsContract.RawContacts.ACCOUNT_NAME)
            }
            val rawUri = contentResolver.insert(
                ContactsContract.RawContacts.CONTENT_URI, rawValues
            ) ?: return false
            val newRawId = ContentUris.parseId(rawUri)

            updateOrInsertPhoneticName(newRawId, contact.displayName, contact.suggestion)

            for ((number, type) in phones) {
                val phoneValues = ContentValues().apply {
                    put(ContactsContract.Data.RAW_CONTACT_ID, newRawId)
                    put(ContactsContract.Data.MIMETYPE,
                        ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                    put(ContactsContract.CommonDataKinds.Phone.NUMBER, number)
                    put(ContactsContract.CommonDataKinds.Phone.TYPE, type)
                }
                contentResolver.insert(ContactsContract.Data.CONTENT_URI, phoneValues)
            }

            val existingRawId = getRawContactIds(contact.id).firstOrNull()
            if (existingRawId != null) {
                val ex = ContentValues().apply {
                    put(ContactsContract.AggregationExceptions.TYPE,
                        ContactsContract.AggregationExceptions.TYPE_KEEP_TOGETHER)
                    put(ContactsContract.AggregationExceptions.RAW_CONTACT_ID1, existingRawId)
                    put(ContactsContract.AggregationExceptions.RAW_CONTACT_ID2, newRawId)
                }
                contentResolver.update(
                    ContactsContract.AggregationExceptions.CONTENT_URI, ex, null, null
                )
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "saveToDeviceAndApply failed", e)
            false
        }
    }

    private fun readPhoneNumbers(contactId: Long): List<Pair<String, Int>> {
        val result = mutableListOf<Pair<String, Int>>()
        contentResolver.query(
            ContactsContract.Data.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.TYPE,
            ),
            "${ContactsContract.Data.CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
            arrayOf(
                contactId.toString(),
                ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE,
            ),
            null,
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val number = cursor.getString(0) ?: continue
                val type = cursor.getInt(1)
                result.add(number to type)
            }
        }
        return result
    }

    private data class RawContact(val id: Long, val accountType: String?)

    private fun getRawContacts(contactId: Long): List<RawContact> {
        val result = mutableListOf<RawContact>()
        contentResolver.query(
            ContactsContract.RawContacts.CONTENT_URI,
            arrayOf(
                ContactsContract.RawContacts._ID,
                ContactsContract.RawContacts.ACCOUNT_TYPE,
            ),
            "${ContactsContract.RawContacts.CONTACT_ID} = ? AND " +
                    "${ContactsContract.RawContacts.DELETED} = 0",
            arrayOf(contactId.toString()),
            null,
        )?.use { cursor ->
            val idIdx = cursor.getColumnIndexOrThrow(ContactsContract.RawContacts._ID)
            val typeIdx = cursor.getColumnIndexOrThrow(ContactsContract.RawContacts.ACCOUNT_TYPE)
            while (cursor.moveToNext()) {
                result.add(RawContact(cursor.getLong(idIdx), cursor.getString(typeIdx)))
            }
        }
        return result
    }

    private fun getRawContactIds(contactId: Long): List<Long> =
        getRawContacts(contactId).map { it.id }

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

        val existingDataId: Long? = contentResolver.query(
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
            contentResolver.update(
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
            contentResolver.insert(ContactsContract.Data.CONTENT_URI, values) != null
        }
    }
}
