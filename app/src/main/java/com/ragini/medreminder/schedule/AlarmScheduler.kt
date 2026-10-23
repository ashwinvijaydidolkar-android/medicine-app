package com.ragini.medreminder.schedule

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.ragini.medreminder.MainActivity
import com.ragini.medreminder.alarm.AlarmService
import com.ragini.medreminder.alarm.CaregiverAlert
import com.ragini.medreminder.data.Db
import com.ragini.medreminder.data.Prefs
import java.time.LocalDate

/**
 * Keeps exactly one "next reminder" alarm registered with Android, the same way a clock app does.
 * setAlarmClock() is used because Android treats it as a real alarm clock: it fires on time
 * even in Doze / standby and it is never batched or delayed.
 */
object AlarmScheduler {
    const val ACTION_RING = "com.ragini.medreminder.action.RING"
    const val ACTION_DAY_CLOSE = "com.ragini.medreminder.action.DAY_CLOSE"
    const val ACTION_TEST = "com.ragini.medreminder.action.TEST"

    private const val RC_RING = 100
    private const val RC_CLOSE = 101
    private const val RC_TEST = 102
    private const val RC_SHOW = 103

    fun canExact(ctx: Context): Boolean {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return if (Build.VERSION.SDK_INT >= 31) am.canScheduleExactAlarms() else true
    }

    fun rescheduleAll(ctx: Context) {
        DayCloser.closePending(ctx)
        scheduleNext(ctx)
        scheduleDayClose(ctx)
    }

    /** Reminder moments still to come for doses that have not been answered yet. */
    fun pendingEvents(ctx: Context, afterMs: Long): List<RingEvent> {
        val db = Db.get(ctx)
        val prefs = Prefs(ctx)
        val meds = db.medicines()
        val today = LocalDate.now()
        val from = Doses.epochMs(today.minusDays(1), 0)
        val to = Doses.epochMs(today.plusDays(3), 0)
        val answered = db.responsesBetween(from, to)
        val out = ArrayList<RingEvent>()
        for (offset in -1L..2L) {
            val day = today.plusDays(offset)
            for (dose in Doses.on(meds, day)) {
                if (answered.containsKey(dose.key)) continue
                Doses.ringEvents(dose, prefs).filterTo(out) { it.at > afterMs }
            }
        }
        out.sortBy { it.at }
        return out
    }

    fun nextEvent(ctx: Context): RingEvent? {
        val prefs = Prefs(ctx)
        val after = maxOf(System.currentTimeMillis(), prefs.lastFiredAt)
        return pendingEvents(ctx, after).firstOrNull { it.kind != RingKind.CARE_CHECK }
            ?: pendingEvents(ctx, after).firstOrNull()
    }

    fun scheduleNext(ctx: Context) {
        val prefs = Prefs(ctx)
        val after = maxOf(System.currentTimeMillis(), prefs.lastFiredAt)
        val next = pendingEvents(ctx, after).firstOrNull()
        val pi = broadcast(ctx, ACTION_RING, RC_RING)
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (next == null) {
            am.cancel(pi)
            return
        }
        setAlarm(ctx, next.at, pi, asAlarmClock = true)
    }

    /** Called by AlarmReceiver when the registered alarm goes off. */
    fun onRing(ctx: Context) {
        val prefs = Prefs(ctx)
        val now = System.currentTimeMillis()
        // Ignore anything older than 10 minutes (e.g. phone was switched off) – only announce what is current.
        val windowStart = maxOf(prefs.lastFiredAt, now - 10 * 60_000L)
        val due = pendingEvents(ctx, windowStart).filter { it.at <= now + 45_000L }
        if (due.isNotEmpty()) {
            prefs.lastFiredAt = due.maxOf { it.at }
            val care = due.filter { it.kind == RingKind.CARE_CHECK }
            if (care.isNotEmpty()) CaregiverAlert.send(ctx, care.map { it.dose })
            val spoken = due.filter { it.kind != RingKind.CARE_CHECK }
                .groupBy { it.dose.key }
                .map { (_, list) -> list.maxByOrNull { it.at }!! }
            if (spoken.isNotEmpty()) AlarmService.start(ctx, spoken)
        }
        scheduleNext(ctx)
    }

    fun scheduleDayClose(ctx: Context) {
        val prefs = Prefs(ctx)
        val today = LocalDate.now()
        var at = Doses.epochMs(today, prefs.dayCloseMinute)
        if (at <= System.currentTimeMillis()) at = Doses.epochMs(today.plusDays(1), prefs.dayCloseMinute)
        setAlarm(ctx, at, broadcast(ctx, ACTION_DAY_CLOSE, RC_CLOSE), asAlarmClock = false)
    }

    /** A test reminder one minute from now, so the family can check it rings with the screen off. */
    fun scheduleTest(ctx: Context, delayMs: Long = 60_000L) {
        setAlarm(ctx, System.currentTimeMillis() + delayMs, broadcast(ctx, ACTION_TEST, RC_TEST), asAlarmClock = true)
    }

    private fun broadcast(ctx: Context, action: String, rc: Int): PendingIntent =
        PendingIntent.getBroadcast(
            ctx, rc,
            Intent(ctx, AlarmReceiver::class.java).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun setAlarm(ctx: Context, at: Long, pi: PendingIntent, asAlarmClock: Boolean) {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        try {
            if (canExact(ctx)) {
                if (asAlarmClock) {
                    val show = PendingIntent.getActivity(
                        ctx, RC_SHOW, Intent(ctx, MainActivity::class.java),
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    am.setAlarmClock(AlarmManager.AlarmClockInfo(at, show), pi)
                } else {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
                }
                return
            }
        } catch (e: SecurityException) {
            // fall through to the inexact alarm below
        }
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
    }
}
