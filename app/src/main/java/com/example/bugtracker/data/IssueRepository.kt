package com.example.bugtracker.data

import androidx.room.withTransaction
import com.example.bugtracker.data.local.BugTrackerDatabase
import com.example.bugtracker.data.local.IssueEntity
import com.example.bugtracker.data.local.IssuePriority
import com.example.bugtracker.data.local.IssueStatus
import com.example.bugtracker.data.local.MutationType
import com.example.bugtracker.data.local.PendingMutationEntity
import com.example.bugtracker.data.local.SyncState
import com.example.bugtracker.data.remote.IssueApi
import com.example.bugtracker.data.remote.toDto
import com.example.bugtracker.data.remote.toEntity
import java.util.UUID
import kotlinx.coroutines.flow.Flow

class IssueRepository(
    private val db: BugTrackerDatabase,
    private val api: IssueApi,
    private val now: () -> Long = System::currentTimeMillis,
) {
    private val issueDao = db.issues()
    private val pendingDao = db.pendingMutations()

    fun observeIssues(): Flow<List<IssueEntity>> = issueDao.observeAll()
    fun observeIssue(id: String): Flow<IssueEntity?> = issueDao.observe(id)

    suspend fun create(
        title: String,
        description: String,
        status: IssueStatus,
        priority: IssuePriority,
        assignee: String?,
    ): String {
        require(title.isNotBlank()) { "Title is required" }
        val time = now()
        val issue = IssueEntity(
            id = UUID.randomUUID().toString(),
            title = title.trim(),
            description = description.trim(),
            status = status,
            priority = priority,
            assignee = assignee?.trim()?.ifBlank { null },
            createdAt = time,
            updatedAt = time,
        )
        db.withTransaction {
            issueDao.upsert(issue)
            pendingDao.upsert(PendingMutationEntity(
                issueId = issue.id,
                type = MutationType.CREATE,
                createdAt = time,
            ))
        }
        return issue.id
    }

    suspend fun update(issue: IssueEntity) {
        require(issue.title.isNotBlank()) { "Title is required" }
        db.withTransaction {
            val oldMutaton = pendingDao.getForIssue(issue.id)
            issueDao.upsert(issue.copy(
                title = issue.title.trim(),
                description = issue.description.trim(),
                assignee = issue.assignee?.trim()?.ifBlank { null },
                updatedAt = now(),
                syncState = SyncState.PENDING,
            ))
            pendingDao.upsert(PendingMutationEntity(
                id = oldMutaton?.id ?: 0,
                issueId = issue.id,
                type = if (oldMutaton?.type == MutationType.CREATE) MutationType.CREATE else MutationType.UPDATE,
                createdAt = oldMutaton?.createdAt ?: now(),
            ))
        }
    }

    suspend fun delete(id: String) {
        db.withTransaction {
            val oldMutaton = pendingDao.getForIssue(id)
            issueDao.deleteById(id)
            if (oldMutaton?.type == MutationType.CREATE) {
                // The server does not know about this issue, so there is nothing to delete there.
                pendingDao.deleteForIssue(id)
            } else {
                pendingDao.upsert(PendingMutationEntity(
                    id = oldMutaton?.id ?: 0,
                    issueId = id,
                    type = MutationType.DELETE,
                    createdAt = oldMutaton?.createdAt ?: now(),
                ))
            }
        }
    }

    /** Sends saved changes, then downloads the latest issue list. */
    suspend fun sync() {
        pendingDao.getAll().forEach { change ->
            try {
                when (change.type) {
                    MutationType.CREATE -> {
                        val local = issueDao.get(change.issueId) ?: return@forEach
                        val server = api.createIssue(local.id, local.toDto()).toEntity()
                        db.withTransaction {
                            issueDao.upsert(server)
                            pendingDao.deleteForIssue(change.issueId)
                        }
                    }
                    MutationType.UPDATE -> {
                        val local = issueDao.get(change.issueId) ?: return@forEach
                        val server = api.updateIssue(local.id, local.toDto()).toEntity()
                        db.withTransaction {
                            issueDao.upsert(server)
                            pendingDao.deleteForIssue(change.issueId)
                        }
                    }
                    MutationType.DELETE -> {
                        api.deleteIssue(change.issueId)
                        pendingDao.deleteForIssue(change.issueId)
                    }
                }
            } catch (error: Exception) {
                pendingDao.recordFailure(change.id, error.message ?: error.javaClass.simpleName)
                issueDao.setSyncState(change.issueId, SyncState.FAILED)
                throw error
            }
        }

        val remote = api.getIssues().map { it.toEntity() }
        val remoteIds = remote.mapTo(hashSetOf()) { it.id }
        db.withTransaction {
            remote.forEach { serverIssue ->
                val local = issueDao.get(serverIssue.id)
                val hasPending = pendingDao.exists(serverIssue.id)
                if (!hasPending && (local == null || serverIssue.updatedAt >= local.updatedAt)) {
                    issueDao.upsert(serverIssue)
                }
            }
            // GET /issues returns the full list from the server.
            issueDao.getAllOnce().forEach { local ->
                if (local.syncState == SyncState.SYNCED &&
                    local.id !in remoteIds &&
                    !pendingDao.exists(local.id)
                ) {
                    issueDao.deleteById(local.id)
                }
            }
        }
    }
}
