package com.example.bugtracker.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [IssueEntity::class, PendingMutationEntity::class],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class BugTrackerDatabase : RoomDatabase() {
    abstract fun issues(): IssueDao
    abstract fun pendingMutations(): PendingMutationDao
}
