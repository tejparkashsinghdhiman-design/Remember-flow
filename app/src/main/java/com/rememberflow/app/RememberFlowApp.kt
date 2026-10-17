package com.rememberflow.app

import android.app.Application
import com.rememberflow.app.data.ReminderRepository
import com.rememberflow.app.notify.NotificationHelper

class RememberFlowApp : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationHelper.ensureChannel(this)
        ReminderRepository.get(this)
    }
}
