package com.ragini.medreminder.schedule

import android.content.Context
import com.ragini.medreminder.alarm.AlarmService
import com.ragini.medreminder.alarm.Notifs
import com.ragini.medreminder.data.Db
import com.ragini.medreminder.data.Dose
import com.ragini.medreminder.data.DoseResponse
import com.ragini.medreminder.data.DoseStatus
import com.ragini.medreminder.data.Prefs

/**
 * Saves the answer given after a reminder is closed ("Taken" or "Not taken" + reason).
 * The answer is stored straight away, but it only becomes attendance when the day is
 * closed at the end of the day (see [DayCloser]).
 */
object Answers {

    /** Rebuilds a [Dose] from its key, e.g. "12@2026-10-23@480". */
    fun doseFor(ctx: Context, key: String): Dose? {
        val (medId, day, minute) = Doses.parseKey(key) ?: return null
        val med = Db.get(ctx).medicine(medId) ?: return null
        return Dose(key, med, day, minute, Doses.epochMs(day, minute))
    }

    fun record(ctx: Context, key: String, status: DoseStatus, reason: String) {
        val db = Db.get(ctx)
        val dose = doseFor(ctx, key) ?: return
        val previous = db.response(key)
        db.saveResponse(
            DoseResponse(
                doseKey = key,
                medId = dose.medicine.id,
                scheduledAt = dose.at,
                status = status,
                reason = reason.trim(),
                respondedAt = System.currentTimeMillis()
            )
        )
        // Pill count: subtract when a dose is first marked taken, give it back if it is changed to not taken.
        val m = dose.medicine
        if (m.stock != null) {
            val wasTaken = previous?.status == DoseStatus.TAKEN
            val isTaken = status == DoseStatus.TAKEN
            val newStock = when {
                isTaken && !wasTaken -> (m.stock - m.perDose).coerceAtLeast(0)
                !isTaken && wasTaken -> m.stock + m.perDose
                else -> m.stock
            }
            if (newStock != m.stock) db.saveMedicine(m.copy(stock = newStock))
        }
        Notifs.cancelAwaiting(ctx, listOf(key))
        AlarmScheduler.scheduleNext(ctx)
        // The tune stops only once every medicine of that time has been answered.
        AlarmService.onAnswered()
        if (status == DoseStatus.TAKEN) Notifs.lowStockCheck(ctx)
    }

    /** Days in a row (ending yesterday or today) where every dose was taken. */
    fun streak(ctx: Context): Int {
        val db = Db.get(ctx)
        val today = java.time.LocalDate.now()
        val finals = db.finals(today.minusDays(400), today).groupBy { it.day }
        var d = if (finals.containsKey(today)) today else today.minusDays(1)
        var n = 0
        while (true) {
            val list = finals[d] ?: break
            if (list.isEmpty() || list.any { it.status != DoseStatus.TAKEN }) break
            n++
            d = d.minusDays(1)
        }
        return n
    }

    fun userName(ctx: Context) = Prefs(ctx).userName
}
