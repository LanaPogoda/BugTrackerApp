package com.example.bugtracker.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.bugtracker.data.IssueRepository
import com.example.bugtracker.data.local.IssueEntity
import com.example.bugtracker.data.local.IssuePriority
import com.example.bugtracker.data.local.IssueStatus
import com.example.bugtracker.sync.SyncWorker
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class IssueViewModel(
    private val repository: IssueRepository,
    application: Application,
) : AndroidViewModel(application) {
    val issues: StateFlow<List<IssueEntity>> = repository.observeIssues()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun save(
        existing: IssueEntity?,
        title: String,
        description: String,
        status: IssueStatus,
        priority: IssuePriority,
        assignee: String?,
        onComplete: () -> Unit,
    ) {
        viewModelScope.launch {
            if (existing == null) {
                repository.create(title, description, status, priority, assignee)
            } else {
                repository.update(existing.copy(
                    title = title,
                    description = description,
                    status = status,
                    priority = priority,
                    assignee = assignee,
                ))
            }
            SyncWorker.enqueue(getApplication())
            onComplete()
        }
    }

    fun delete(id: String) {
        viewModelScope.launch {
            repository.delete(id)
            SyncWorker.enqueue(getApplication())
        }
    }

    fun sync() = SyncWorker.enqueue(getApplication())
}

class IssueViewModelFactory(
    private val repository: IssueRepository,
    private val application: Application,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        IssueViewModel(repository, application) as T
}

