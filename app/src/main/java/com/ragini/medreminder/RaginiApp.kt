package com.ragini.medreminder

import android.app.Application
import com.ragini.medreminder.alarm.Notifs
import com.ragini.medreminder.schedule.AlarmScheduler

class RaginiApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Notifs.createChannels(this)
        // Make sure the next reminder and the end-of-day attendance are always registered.
        try {
            AlarmScheduler.rescheduleAll(this)
        } catch (_: Exception) {
        }
    }
}
