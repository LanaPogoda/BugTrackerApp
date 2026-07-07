package com.example.bugtracker

import android.content.Context
import androidx.room.Room
import com.example.bugtracker.data.IssueRepository
import com.example.bugtracker.data.local.BugTrackerDatabase
import com.example.bugtracker.data.remote.IssueApi
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class AppContainer(context: Context) {
    private val database = Room.databaseBuilder(
        context,
        BugTrackerDatabase::class.java,
        "bug-tracker.db",
    ).build()

    private val api = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .client(OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC
                else HttpLoggingInterceptor.Level.NONE
            })
            .build())
        .addConverterFactory(MoshiConverterFactory.create(
            Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        ))
        .build()
        .create(IssueApi::class.java)

    val repository = IssueRepository(database, api)
}

