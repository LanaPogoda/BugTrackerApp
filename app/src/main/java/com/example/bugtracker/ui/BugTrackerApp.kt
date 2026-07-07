package com.example.bugtracker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.bugtracker.data.local.IssueEntity
import com.example.bugtracker.data.local.IssuePriority
import com.example.bugtracker.data.local.IssueStatus
import com.example.bugtracker.data.local.SyncState

@Composable
fun BugTrackerApp(viewModel: IssueViewModel) {
    val issues by viewModel.issues.collectAsState()
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    var creating by rememberSaveable { mutableStateOf(false) }
    val editing = issues.firstOrNull { it.id == editingId }

    if (creating || editingId != null) {
        IssueEditor(
            issue = editing,
            onSave = { title, description, status, priority, assignee ->
                viewModel.save(editing, title, description, status, priority, assignee) {
                    creating = false
                    editingId = null
                }
            },
            onCancel = {
                creating = false
                editingId = null
            },
        )
    } else {
        IssueList(
            issues = issues,
            onCreate = { creating = true },
            onOpen = { editingId = it },
            onDelete = viewModel::delete,
            onSync = viewModel::sync,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IssueList(
    issues: List<IssueEntity>,
    onCreate: () -> Unit,
    onOpen: (String) -> Unit,
    onDelete: (String) -> Unit,
    onSync: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bug Tracker") },
                actions = { TextButton(onClick = onSync) { Text("Sync") } },
            )
        },
        floatingActionButton = { FloatingActionButton(onClick = onCreate) { Text("+") } },
    ) { padding ->
        if (issues.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                Text("No issues yet", style = MaterialTheme.typography.headlineSmall)
                Text("Create an issue now. The app will save it here and upload it when you are online.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(issues, key = { it.id }) { issue ->
                    IssueCard(issue, { onOpen(issue.id) }, { onDelete(issue.id) })
                }
            }
        }
    }
}

@Composable
private fun IssueCard(issue: IssueEntity, onOpen: () -> Unit, onDelete: () -> Unit) {
    Card(onClick = onOpen, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(issue.title, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text(issue.priority.name)
            }
            Spacer(Modifier.height(6.dp))
            Text(issue.description.ifBlank { "No description" }, maxLines = 2)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(issue.status.name.replace('_', ' '))
                Row {
                    if (issue.syncState != SyncState.SYNCED) {
                        Text(issue.syncState.name.lowercase(), color = MaterialTheme.colorScheme.tertiary)
                    }
                    TextButton(onClick = onDelete) { Text("Delete") }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IssueEditor(
    issue: IssueEntity?,
    onSave: (String, String, IssueStatus, IssuePriority, String?) -> Unit,
    onCancel: () -> Unit,
) {
    var title by remember(issue?.id) { mutableStateOf(issue?.title.orEmpty()) }
    // Keep the form values here while the user edits the issue.
    var descripton by remember(issue?.id) { mutableStateOf(issue?.description.orEmpty()) }
    var assignee by remember(issue?.id) { mutableStateOf(issue?.assignee.orEmpty()) }
    var status by remember(issue?.id) { mutableStateOf(issue?.status ?: IssueStatus.OPEN) }
    var priority by remember(issue?.id) { mutableStateOf(issue?.priority ?: IssuePriority.MEDIUM) }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(if (issue == null) "New issue" else "Edit issue") },
            navigationIcon = { TextButton(onClick = onCancel) { Text("Back") } },
        )
    }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                OutlinedTextField(
                    value = descripton,
                    onValueChange = { descripton = it },
                    label = { Text("Description") },
                    minLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                OutlinedTextField(
                    value = assignee,
                    onValueChange = { assignee = it },
                    label = { Text("Assignee") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item { Text("Status", style = MaterialTheme.typography.labelLarge) }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    IssueStatus.entries.forEach { option ->
                        FilterChip(
                            selected = status == option,
                            onClick = { status = option },
                            label = { Text(option.name.replace('_', ' ')) },
                        )
                    }
                }
            }
            item { Text("Priority", style = MaterialTheme.typography.labelLarge) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    IssuePriority.entries.forEach { option ->
                        FilterChip(
                            selected = priority == option,
                            onClick = { priority = option },
                            label = { Text(option.name.first().toString()) },
                        )
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        enabled = title.isNotBlank(),
                        onClick = { onSave(title, descripton, status, priority, assignee) },
                    ) { Text("Save") }
                    OutlinedButton(onClick = onCancel) { Text("Cancel") }
                }
            }
        }
    }
}
