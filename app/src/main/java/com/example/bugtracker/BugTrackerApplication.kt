package com.example.bugtracker

import android.app.Application
import com.example.bugtracker.sync.SyncWorker

class BugTrackerApplication : Application() {
    val container by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        SyncWorker.enqueue(this)
    }
}

