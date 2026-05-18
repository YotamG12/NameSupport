package com.namesupport.data

import android.Manifest
import android.content.ContentValues
import android.provider.ContactsContract
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.namesupport.util.HebrewTransliterator
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for ContactRepository.
 * These run on a real device or emulator and interact with the contacts database.
 *
 * Each test inserts its own contacts and cleans them up in @After so the
 * device's real contacts are never polluted.
 */
@RunWith(AndroidJUnit4::class)
class ContactRepositoryTest {

    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.WRITE_CONTACTS,
    )

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var repository: ContactRepository
    private val insertedRawIds = mutableListOf<Long>()

    @Before
    fun setUp() {
        repository = ContactRepository(context)
    }

    @After
    fun tearDown() {
        // Remove every raw contact inserted during the test
        insertedRawIds.forEach { rawId ->
            context.contentResolver.delete(
                ContactsContract.RawContacts.CONTENT_URI,
                "${ContactsContract.RawContacts._ID} = ?",
                arrayOf(rawId.toString()),
            )
        }
        insertedRawIds.clear()
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Inserts a minimal contact with the given display name and returns its
     * raw contact ID. Registered for cleanup in @After.
     */
    private fun insertTestContact(displayName: String): Long {
        val rawContactUri = context.contentResolver.insert(
            ContactsContract.RawContacts.CONTENT_URI,
            ContentValues().apply {
                putNull(ContactsContract.RawContacts.ACCOUNT_TYPE)
                putNull(ContactsContract.RawContacts.ACCOUNT_NAME)
            },
        )!!
        val rawId = rawContactUri.lastPathSegment!!.toLong()
        insertedRawIds.add(rawId)

        context.contentResolver.insert(
            ContactsContract.Data.CONTENT_URI,
            ContentValues().apply {
                put(ContactsContract.Data.RAW_CONTACT_ID, rawId)
                put(
                    ContactsContract.Data.MIMETYPE,
                    ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE,
                )
                put(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, displayName)
            },
        )
        return rawId
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    fun repositoryInitialisesWithoutCrash() {
        assertNotNull(repository)
    }

    @Test
    fun getHebrewContactsReturnsListWithoutCrash() {
        val contacts = runBlocking { repository.getHebrewContactsWithoutPhonetic() }
        assertNotNull(contacts)
    }

    @Test
    fun hebrewContactIsDetectedAndReturned() {
        insertTestContact("יוסי כהן")

        val contacts = runBlocking { repository.getHebrewContactsWithoutPhonetic() }
        val found = contacts.any { it.displayName == "יוסי כהן" }
        assertTrue("Hebrew contact should appear in scan results", found)
    }

    @Test
    fun latinContactIsNotReturned() {
        insertTestContact("John Smith")

        val contacts = runBlocking { repository.getHebrewContactsWithoutPhonetic() }
        val found = contacts.any { it.displayName == "John Smith" }
        assertFalse("Latin-only contact should not appear in scan results", found)
    }

    @Test
    fun suggestionIsCorrectForKnownName() {
        insertTestContact("שרה לוי")

        val contacts = runBlocking { repository.getHebrewContactsWithoutPhonetic() }
        val item = contacts.firstOrNull { it.displayName == "שרה לוי" }
        assertNotNull("Contact שרה לוי should be in results", item)
        assertEquals("Sarah Levi", item!!.suggestion)
    }

    @Test
    fun afterApplyContactNoLongerAppearsInScan() {
        insertTestContact("דוד מזרחי")

        val before = runBlocking { repository.getHebrewContactsWithoutPhonetic() }
        val contact = before.firstOrNull { it.displayName == "דוד מזרחי" }
        assertNotNull("Contact should be found before apply", contact)

        val applied = repository.applyPhoneticName(contact!!)
        assertTrue("applyPhoneticName should return true", applied)

        val after = runBlocking { repository.getHebrewContactsWithoutPhonetic() }
        val stillPresent = after.any { it.displayName == "דוד מזרחי" }
        assertFalse("Contact should not appear in scan after phonetic name is set", stillPresent)
    }

    @Test
    fun countTranslatedContactsIncreasesAfterApply() {
        insertTestContact("משה שפירא")

        val before = repository.countTranslatedContacts()
        val contacts = runBlocking { repository.getHebrewContactsWithoutPhonetic() }
        val target = contacts.firstOrNull { it.displayName == "משה שפירא" } ?: return
        repository.applyPhoneticName(target)
        val after = repository.countTranslatedContacts()

        assertTrue("Translated count should increase after apply", after > before)
    }

    @Test
    fun containsHebrewDetectsHebrewText() {
        assertTrue(HebrewTransliterator.containsHebrew("יוסי"))
        assertFalse(HebrewTransliterator.containsHebrew("Yossi"))
    }
}
