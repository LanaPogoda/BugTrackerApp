package com.example.bugtracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.bugtracker.ui.BugTrackerApp
import com.example.bugtracker.ui.IssueViewModel
import com.example.bugtracker.ui.IssueViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                val application = application as BugTrackerApplication
                val viewModel: IssueViewModel = viewModel(
                    factory = IssueViewModelFactory(application.container.repository, application)
                )
                BugTrackerApp(viewModel)
            }
        }
    }
}

