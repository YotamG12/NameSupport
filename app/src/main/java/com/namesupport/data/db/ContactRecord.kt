package com.namesupport.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "contact_records")
data class ContactRecord(
    @PrimaryKey val contactId: Long,
    val status: String,
    val processedAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val APPROVED = "APPROVED"
        const val DISMISSED = "DISMISSED"
    }
}
