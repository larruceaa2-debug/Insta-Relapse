package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        SnapshotEntity::class,
        AccountRecordEntity::class,
        IgnoredAccountEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class FollowLensDatabase : RoomDatabase() {
    abstract fun followLensDao(): FollowLensDao

    companion object {
        @Volatile
        private var INSTANCE: FollowLensDatabase? = null

        fun getInstance(context: Context): FollowLensDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    FollowLensDatabase::class.java,
                    "followlens.db",
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
