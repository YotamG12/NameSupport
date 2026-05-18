package com.namesupport.data

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.provider.ContactsContract
import android.util.Log
import com.namesupport.model.ContactItem
import com.namesupport.util.HebrewTransliterator
import com.namesupport.util.SmartTransliterator

class ContactRepository(private val context: Context) {

    private val translator = SmartTransliterator(context)

    // ── Read ──────────────────────────────────────────────────────────────────

    suspend fun getHebrewContactsWithoutPhonetic(): List<ContactItem> {
        // Step 1: query contacts, collect those with Hebrew names lacking phonetic data
        val raw = mutableListOf<Pair<Long, String>>() // (contactId, displayName)
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
                    if (HebrewTransliterator.containsHebrew(name) && !hasPhoneticName(id)) {
                        raw.add(id to name)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "getHebrewContactsWithoutPhonetic failed", e)
        }

        // Step 2: batch-transliterate all names (uses Claude API when available)
        val suggestions = translator.transliterateAll(raw.map { it.second })

        return raw.map { (id, name) ->
            ContactItem(
                id = id,
                displayName = name,
                suggestion = suggestions[name] ?: HebrewTransliterator.transliterate(name),
            )
        }
    }

    /** Count of contacts that already have a phonetic name set by this app. */
    fun countTranslatedContacts(): Int {
        var count = 0
        try {
            val selection =
                "${ContactsContract.Data.MIMETYPE} = ? AND " +
                        "${ContactsContract.CommonDataKinds.StructuredName.PHONETIC_GIVEN_NAME} IS NOT NULL AND " +
                        "${ContactsContract.CommonDataKinds.StructuredName.PHONETIC_GIVEN_NAME} != ''"
            context.contentResolver.query(
                ContactsContract.Data.CONTENT_URI,
                arrayOf(ContactsContract.Data.CONTACT_ID),
                selection,
                arrayOf(ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE),
                null,
            )?.use { count = it.count }
        } catch (e: Exception) {
            Log.e(TAG, "countTranslatedContacts failed", e)
        }
        return count
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
            ContactsContract.Data.CONTENT_URI, projection, selection, args, null,
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val given = cursor.getString(0)
                val family = cursor.getString(1)
                if (!given.isNullOrBlank() || !family.isNullOrBlank()) return true
            }
        }
        return false
    }

    // ── Write ─────────────────────────────────────────────────────────────────

    /**
     * Writes the English transliteration into both:
     *  - PHONETIC_GIVEN_NAME → used by Google Assistant, stock dialer, most OEMs
     *  - NICKNAME            → used by Bixby and Samsung contacts
     *
     * Display name is never touched.
     */
    fun applyPhoneticName(contact: ContactItem): Boolean {
        return try {
            val rawIds = getRawContactIds(contact.id)
            if (rawIds.isEmpty()) return false

            rawIds.any { rawId ->
                val ok = updateOrInsertPhoneticName(rawId, contact.displayName, contact.suggestion)
                updateOrInsertNickname(rawId, contact.suggestion)   // best-effort; not counted
                ok
            }
        } catch (e: Exception) {
            Log.e(TAG, "applyPhoneticName failed for '${contact.displayName}'", e)
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
        val mime = ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE
        val existingId = queryDataId(rawContactId, mime)

        return if (existingId != null) {
            val values = ContentValues().apply {
                put(ContactsContract.CommonDataKinds.StructuredName.PHONETIC_GIVEN_NAME, phoneticName)
            }
            context.contentResolver.update(
                ContentUris.withAppendedId(ContactsContract.Data.CONTENT_URI, existingId),
                values, null, null,
            ) > 0
        } else {
            val values = ContentValues().apply {
                put(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
                put(ContactsContract.Data.MIMETYPE, mime)
                put(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, displayName)
                put(ContactsContract.CommonDataKinds.StructuredName.PHONETIC_GIVEN_NAME, phoneticName)
            }
            context.contentResolver.insert(ContactsContract.Data.CONTENT_URI, values) != null
        }
    }

    /** Writes the nickname field — read by Bixby and Samsung voice assistants. */
    private fun updateOrInsertNickname(rawContactId: Long, nickname: String): Boolean {
        val mime = ContactsContract.CommonDataKinds.Nickname.CONTENT_ITEM_TYPE
        val existingId = queryDataId(rawContactId, mime)

        return if (existingId != null) {
            val values = ContentValues().apply {
                put(ContactsContract.CommonDataKinds.Nickname.NAME, nickname)
            }
            context.contentResolver.update(
                ContentUris.withAppendedId(ContactsContract.Data.CONTENT_URI, existingId),
                values, null, null,
            ) > 0
        } else {
            val values = ContentValues().apply {
                put(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
                put(ContactsContract.Data.MIMETYPE, mime)
                put(ContactsContract.CommonDataKinds.Nickname.NAME, nickname)
            }
            context.contentResolver.insert(ContactsContract.Data.CONTENT_URI, values) != null
        }
    }

    private fun queryDataId(rawContactId: Long, mimeType: String): Long? =
        context.contentResolver.query(
            ContactsContract.Data.CONTENT_URI,
            arrayOf(ContactsContract.Data._ID),
            "${ContactsContract.Data.RAW_CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
            arrayOf(rawContactId.toString(), mimeType),
            null,
        )?.use { if (it.moveToFirst()) it.getLong(0) else null }

    companion object {
        private const val TAG = "ContactRepository"
    }
}
