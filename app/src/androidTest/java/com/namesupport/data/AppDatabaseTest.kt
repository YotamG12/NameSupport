package com.namesupport.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.namesupport.data.db.AppDatabase
import com.namesupport.data.db.ContactRecord
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDatabaseTest {

    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insertAndReadApproved() = runBlocking {
        db.contactRecordDao().upsert(ContactRecord(1L, ContactRecord.APPROVED))
        assertEquals(ContactRecord.APPROVED, db.contactRecordDao().getStatus(1L))
    }

    @Test
    fun insertAndReadDismissed() = runBlocking {
        db.contactRecordDao().upsert(ContactRecord(2L, ContactRecord.DISMISSED))
        assertEquals(ContactRecord.DISMISSED, db.contactRecordDao().getStatus(2L))
    }

    @Test
    fun unknownIdReturnsNull() = runBlocking {
        assertNull(db.contactRecordDao().getStatus(999L))
    }

    @Test
    fun upsertOverwritesStatus() = runBlocking {
        db.contactRecordDao().upsert(ContactRecord(3L, ContactRecord.DISMISSED))
        db.contactRecordDao().upsert(ContactRecord(3L, ContactRecord.APPROVED))
        assertEquals(ContactRecord.APPROVED, db.contactRecordDao().getStatus(3L))
    }

    @Test
    fun deleteAllDismissedKeepsApproved() = runBlocking {
        db.contactRecordDao().upsert(ContactRecord(10L, ContactRecord.APPROVED))
        db.contactRecordDao().upsert(ContactRecord(11L, ContactRecord.DISMISSED))
        db.contactRecordDao().deleteAllDismissed()
        assertEquals(ContactRecord.APPROVED, db.contactRecordDao().getStatus(10L))
        assertNull(db.contactRecordDao().getStatus(11L))
    }
}
