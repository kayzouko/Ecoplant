package com.example.ecoplant

import android.app.Application

class EcoPlantApplication : Application() {
    val database: AppDatabase by lazy {
        AppDatabase.getDatabase(this)
    }
}