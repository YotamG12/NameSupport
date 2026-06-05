package com.namesupport.worker

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker.Result
import androidx.work.testing.TestListenableWorkerBuilder
import com.namesupport.data.db.AppDatabase
import com.namesupport.data.db.ContactRecord
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ContactMonitorWorkerTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        AppDatabase.setTestInstance(db)
    }

    @After
    fun tearDown() {
        db.close()
        AppDatabase.clearTestInstance()
    }

    @Test
    fun `worker returns success`() = runBlocking {
        val worker = TestListenableWorkerBuilder<ContactMonitorWorker>(context).build()
        val result = worker.doWork()
        assertEquals(Result.success(), result)
    }

    @Test
    fun `worker skips already handled contact`() = runBlocking {
        db.contactRecordDao().upsert(ContactRecord(42L, ContactRecord.APPROVED))

        val handledIds = db.contactRecordDao().getAllHandledIds()
        assert(42L in handledIds) { "Contact 42 should be in handled set" }

        val worker = TestListenableWorkerBuilder<ContactMonitorWorker>(context).build()
        val result = worker.doWork()
        assertEquals(Result.success(), result)
    }
}
