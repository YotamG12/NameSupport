package com.namesupport.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ContactRecordDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(record: ContactRecord)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(records: List<ContactRecord>)

    @Query("SELECT status FROM contact_records WHERE contactId = :contactId")
    suspend fun getStatus(contactId: Long): String?

    @Query("SELECT contactId FROM contact_records")
    suspend fun getAllHandledIds(): List<Long>

    @Query("DELETE FROM contact_records WHERE status = 'DISMISSED'")
    suspend fun deleteAllDismissed()
}
