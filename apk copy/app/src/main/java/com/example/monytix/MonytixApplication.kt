package com.example.monytix

import android.app.Application
import com.example.monytix.analytics.AnalyticsHelper

class MonytixApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AnalyticsHelper.init(this)
    }
}
