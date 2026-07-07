package com.example.bugtracker.data.remote

import com.example.bugtracker.data.local.IssueEntity
import com.example.bugtracker.data.local.SyncState
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

data class IssueDto(
    val id: String,
    val title: String,
    val description: String,
    val status: String,
    val priority: String,
    val assignee: String?,
    val createdAt: Long,
    val updatedAt: Long,
)

fun IssueEntity.toDto() = IssueDto(
    id, title, description, status.name, priority.name, assignee, createdAt, updatedAt
)

fun IssueDto.toEntity() = IssueEntity(
    id = id,
    title = title,
    description = description,
    status = com.example.bugtracker.data.local.IssueStatus.valueOf(status),
    priority = com.example.bugtracker.data.local.IssuePriority.valueOf(priority),
    assignee = assignee,
    createdAt = createdAt,
    updatedAt = updatedAt,
    syncState = SyncState.SYNCED,
)

interface IssueApi {
    @GET("issues") suspend fun getIssues(): List<IssueDto>
    @POST("issues")
    suspend fun createIssue(
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body issue: IssueDto,
    ): IssueDto
    @PUT("issues/{id}") suspend fun updateIssue(@Path("id") id: String, @Body issue: IssueDto): IssueDto
    @DELETE("issues/{id}") suspend fun deleteIssue(@Path("id") id: String)
}
