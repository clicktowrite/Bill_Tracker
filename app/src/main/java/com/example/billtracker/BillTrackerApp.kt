package com.example.billtracker

import android.app.Application
import com.example.billtracker.data.local.BillTrackerDatabase
import com.example.billtracker.data.local.preferences.UserPreferencesRepository
import com.example.billtracker.data.repository.BillTrackerRepository

class BillTrackerApp : Application() {

    lateinit var database: BillTrackerDatabase
        private set

    lateinit var preferencesRepository: UserPreferencesRepository
        private set

    lateinit var repository: BillTrackerRepository
        private set

    override fun onCreate() {
        super.onCreate()
        database = BillTrackerDatabase.getDatabase(this)
        preferencesRepository = UserPreferencesRepository(this)
        repository = BillTrackerRepository(database, preferencesRepository)
    }
}
