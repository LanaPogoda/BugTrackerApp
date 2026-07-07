package com.example.bugtracker.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class IssueStatus { OPEN, IN_PROGRESS, RESOLVED, CLOSED }
enum class IssuePriority { LOW, MEDIUM, HIGH, CRITICAL }
enum class SyncState { SYNCED, PENDING, FAILED }
enum class MutationType { CREATE, UPDATE, DELETE }

@Entity(tableName = "issues", indices = [Index("updatedAt")])
data class IssueEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val status: IssueStatus,
    val priority: IssuePriority,
    val assignee: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val syncState: SyncState = SyncState.PENDING,
)

@Entity(
    tableName = "pending_mutations",
    indices = [Index(value = ["issueId"], unique = true)]
)
data class PendingMutationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val issueId: String,
    val type: MutationType,
    val createdAt: Long,
    val attemptCount: Int = 0,
    val lastError: String? = null,
)

