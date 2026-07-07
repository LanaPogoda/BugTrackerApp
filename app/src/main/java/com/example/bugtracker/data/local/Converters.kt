package com.example.bugtracker.data.local

import androidx.room.TypeConverter

class Converters {
    @TypeConverter fun status(value: String): IssueStatus = IssueStatus.valueOf(value)
    @TypeConverter fun status(value: IssueStatus): String = value.name
    @TypeConverter fun priority(value: String): IssuePriority = IssuePriority.valueOf(value)
    @TypeConverter fun priority(value: IssuePriority): String = value.name
    @TypeConverter fun syncState(value: String): SyncState = SyncState.valueOf(value)
    @TypeConverter fun syncState(value: SyncState): String = value.name
    @TypeConverter fun mutationType(value: String): MutationType = MutationType.valueOf(value)
    @TypeConverter fun mutationType(value: MutationType): String = value.name
}

