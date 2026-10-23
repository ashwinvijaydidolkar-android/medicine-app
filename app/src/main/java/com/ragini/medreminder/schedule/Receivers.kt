package com.ragini.medreminder.schedule

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ragini.medreminder.alarm.AlarmService
import com.ragini.medreminder.alarm.Notifs

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            AlarmScheduler.ACTION_RING -> AlarmScheduler.onRing(context)
            AlarmScheduler.ACTION_TEST -> AlarmService.startTest(context)
            AlarmScheduler.ACTION_DAY_CLOSE -> {
                DayCloser.closePending(context)
                Notifs.lowStockCheck(context)
                AlarmScheduler.scheduleDayClose(context)
                AlarmScheduler.scheduleNext(context)
            }
        }
    }
}

/** Re-registers reminders after a restart, an app update, or a change of time / time zone. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        AlarmScheduler.rescheduleAll(context)
    }
}
