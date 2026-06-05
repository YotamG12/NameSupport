package com.namesupport.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.namesupport.data.db.AppDatabase
import com.namesupport.data.db.ContactRecord
import com.namesupport.data.db.ContactRecordDao
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ContactRecordDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: ContactRecordDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.contactRecordDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `upsert and getStatus returns correct status`() = runBlocking {
        dao.upsert(ContactRecord(1L, ContactRecord.APPROVED))
        assertEquals(ContactRecord.APPROVED, dao.getStatus(1L))
    }

    @Test
    fun `getStatus returns null for unknown id`() = runBlocking {
        assertNull(dao.getStatus(999L))
    }

    @Test
    fun `upsert overwrites existing record`() = runBlocking {
        dao.upsert(ContactRecord(1L, ContactRecord.DISMISSED))
        dao.upsert(ContactRecord(1L, ContactRecord.APPROVED))
        assertEquals(ContactRecord.APPROVED, dao.getStatus(1L))
    }

    @Test
    fun `getAllHandledIds returns all inserted ids`() = runBlocking {
        dao.upsert(ContactRecord(10L, ContactRecord.APPROVED))
        dao.upsert(ContactRecord(20L, ContactRecord.DISMISSED))
        val ids = dao.getAllHandledIds()
        assertEquals(setOf(10L, 20L), ids.toSet())
    }

    @Test
    fun `deleteAllDismissed removes only dismissed records`() = runBlocking {
        dao.upsert(ContactRecord(1L, ContactRecord.APPROVED))
        dao.upsert(ContactRecord(2L, ContactRecord.DISMISSED))
        dao.deleteAllDismissed()
        assertEquals(ContactRecord.APPROVED, dao.getStatus(1L))
        assertNull(dao.getStatus(2L))
    }

    @Test
    fun `upsertAll inserts multiple records`() = runBlocking {
        val records = listOf(
            ContactRecord(100L, ContactRecord.APPROVED),
            ContactRecord(200L, ContactRecord.APPROVED),
            ContactRecord(300L, ContactRecord.DISMISSED),
        )
        dao.upsertAll(records)
        assertEquals(3, dao.getAllHandledIds().size)
    }
}
