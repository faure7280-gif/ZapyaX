package com.example

import android.app.Application
import com.example.service.TransferForegroundService

class ZapyaXApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        TransferForegroundService.createNotificationChannel(this)
    }
}
