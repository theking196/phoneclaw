package com.example.universal

import android.app.Application

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Firebase removed — all data stored on-device via LocalStorage
    }
}
