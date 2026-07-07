package com.example.bugtracker.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface IssueDao {
    @Query("SELECT * FROM issues ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<IssueEntity>>

    @Query("SELECT * FROM issues WHERE id = :id")
    fun observe(id: String): Flow<IssueEntity?>

    @Query("SELECT * FROM issues WHERE id = :id")
    suspend fun get(id: String): IssueEntity?

    @Query("SELECT * FROM issues")
    suspend fun getAllOnce(): List<IssueEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(issue: IssueEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(issues: List<IssueEntity>)

    @Delete
    suspend fun delete(issue: IssueEntity)

    @Query("DELETE FROM issues WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("UPDATE issues SET syncState = :state WHERE id = :id")
    suspend fun setSyncState(id: String, state: SyncState)
}

@Dao
interface PendingMutationDao {
    @Query("SELECT * FROM pending_mutations ORDER BY createdAt, id")
    suspend fun getAll(): List<PendingMutationEntity>

    @Query("SELECT * FROM pending_mutations WHERE issueId = :issueId LIMIT 1")
    suspend fun getForIssue(issueId: String): PendingMutationEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM pending_mutations WHERE issueId = :issueId)")
    suspend fun exists(issueId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(mutation: PendingMutationEntity)

    @Query("DELETE FROM pending_mutations WHERE issueId = :issueId")
    suspend fun deleteForIssue(issueId: String)

    @Query("UPDATE pending_mutations SET attemptCount = attemptCount + 1, lastError = :error WHERE id = :id")
    suspend fun recordFailure(id: Long, error: String)
}
