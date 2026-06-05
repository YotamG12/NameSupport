package com.namesupport.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ContactRecord::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun contactRecordDao(): ContactRecordDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "namesupport.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instance = it }
            }

        @androidx.annotation.VisibleForTesting
        fun setTestInstance(db: AppDatabase) {
            instance = db
        }

        @androidx.annotation.VisibleForTesting
        fun clearTestInstance() {
            instance = null
        }
    }
}
