package com.ragini.medreminder.schedule

import android.content.Context
import com.ragini.medreminder.data.Db
import com.ragini.medreminder.data.DoseStatus
import com.ragini.medreminder.data.FinalDose
import com.ragini.medreminder.data.Prefs
import java.time.LocalDate

/**
 * Attendance is written only at the end of the day.
 * For every dose of the day:
 *   – answered "Taken"             → present, taken
 *   – answered "Not taken" + reason → present, not taken (the reason is kept)
 *   – no answer at all             → absent ("No answer")
 * Days that were missed while the phone was off are closed the next time the app or phone wakes up.
 */
object DayCloser {
    fun closePending(ctx: Context) {
        val prefs = Prefs(ctx)
        val db = Db.get(ctx)
        val today = LocalDate.now()
        val todayDone = System.currentTimeMillis() >= Doses.epochMs(today, prefs.dayCloseMinute)
        val last = if (todayDone) today else today.minusDays(1)
        var day: LocalDate = prefs.firstDay
        val floor = last.minusDays(400)
        if (day.isBefore(floor)) day = floor
        if (day.isAfter(last)) return
        val meds = db.medicines()
        while (!day.isAfter(last)) {
            if (!db.isClosed(day)) {
                val doses = Doses.on(meds, day)
                val answers = db.responsesBetween(Doses.epochMs(day, 0), Doses.epochMs(day.plusDays(1), 0))
                val finals = doses.map { d ->
                    val r = answers[d.key]
                    FinalDose(
                        doseKey = d.key,
                        medId = d.medicine.id,
                        medName = d.medicine.name,
                        day = day,
                        minuteOfDay = d.minuteOfDay,
                        scheduledAt = d.at,
                        status = r?.status ?: DoseStatus.MISSED,
                        reason = r?.reason ?: "",
                        respondedAt = r?.respondedAt
                    )
                }
                db.closeDay(day, finals)
            }
            day = day.plusDays(1)
        }
    }
}
