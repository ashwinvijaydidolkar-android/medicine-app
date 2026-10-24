package com.ragini.medreminder.schedule

import com.ragini.medreminder.data.Dose
import com.ragini.medreminder.data.Medicine
import com.ragini.medreminder.data.Prefs
import com.ragini.medreminder.data.ScheduleType
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

enum class RingKind { PRE, DUE, FOLLOW, CARE_CHECK }

/** One moment when the phone should speak (or check on) a dose. */
data class RingEvent(val at: Long, val dose: Dose, val kind: RingKind, val minutes: Int)

object Doses {

    fun key(medId: Long, day: LocalDate, minute: Int) = "$medId@$day@$minute"

    fun parseKey(key: String): Triple<Long, LocalDate, Int>? = try {
        val p = key.split("@")
        Triple(p[0].toLong(), LocalDate.parse(p[1]), p[2].toInt())
    } catch (e: Exception) {
        null
    }

    fun epochMs(day: LocalDate, minuteOfDay: Int): Long =
        day.atStartOfDay(ZoneId.systemDefault()).plusMinutes(minuteOfDay.toLong()).toInstant().toEpochMilli()

    fun occursOn(m: Medicine, day: LocalDate): Boolean {
        if (!m.active) return false
        if (day.isBefore(m.startDate)) return false
        if (m.endDate != null && day.isAfter(m.endDate)) return false
        return when (m.scheduleType) {
            ScheduleType.DAILY -> true
            ScheduleType.WEEKDAYS -> day.dayOfWeek.value in m.weekdays
            ScheduleType.EVERY_N_DAYS -> {
                val n = m.everyNDays.coerceAtLeast(1)
                ChronoUnit.DAYS.between(m.startDate, day) % n == 0L
            }
        }
    }

    /**
     * All doses of all medicines on [day], earliest first. Doses from before a medicine was added are
     * left out – unless the user answered them in the "earlier today" check-in ([answered]).
     */
    fun on(meds: List<Medicine>, day: LocalDate, answered: (String) -> Boolean = { false }): List<Dose> {
        val out = ArrayList<Dose>()
        for (m in meds) {
            if (!occursOn(m, day)) continue
            for (t in m.times.distinct()) {
                val at = epochMs(day, t)
                val k = key(m.id, day, t)
                if (at < m.createdAt - 60_000L && !answered(k)) continue
                out.add(Dose(k, m, day, t, at))
            }
        }
        out.sortWith(compareBy<Dose>({ it.at }, { it.medicine.name.lowercase() }))
        return out
    }

    /**
     * Doses that were due earlier today, before their medicine was added to Ragini (a new install,
     * an imported list or newly typed medicines). Ragini asks once whether they were taken.
     * Only medicines added in the last [withinMs] count, so the question comes right after adding.
     */
    fun earlierToday(meds: List<Medicine>, now: Long, answered: (String) -> Boolean, withinMs: Long = 30 * 60_000L): List<Dose> {
        val day = LocalDate.now()
        val out = ArrayList<Dose>()
        for (m in meds) {
            if (m.createdAt < now - withinMs || !occursOn(m, day)) continue
            for (t in m.times.distinct()) {
                val at = epochMs(day, t)
                val k = key(m.id, day, t)
                if (at <= m.createdAt + 60_000L && at < now && !answered(k)) out.add(Dose(k, m, day, t, at))
            }
        }
        out.sortWith(compareBy<Dose>({ it.at }, { it.medicine.name.lowercase() }))
        return out
    }

    /**
     * When the early reminders come: [count] reminders spread evenly over the last [lead] minutes,
     * from [lead] minutes before down to 1 minute before. E.g. 19 min and 10 reminders → 19, 17, … 3, 1;
     * 20 min and 5 reminders → 20, 15, 11, 6, 1. Never more reminders than minutes.
     */
    fun earlyMinutes(lead: Int, count: Int): List<Int> {
        if (lead <= 0 || count <= 0) return emptyList()
        val n = minOf(count, lead)
        if (n == 1) return listOf(lead)
        return (0 until n).map { i -> Math.round(lead - i * (lead - 1).toDouble() / (n - 1)).toInt() }.distinct()
    }

    /**
     * The reminder rhythm for one dose. With the defaults (start 19 min before, every 2 min) this is:
     * 19, 17, 15, 13, 11, 9, 7, 5, 3, 1 minutes before, then at the exact time,
     * then follow-ups after the time if the dose has still not been answered.
     */
    fun ringEvents(d: Dose, prefs: Prefs): List<RingEvent> {
        val lead = d.medicine.leadMinutes ?: prefs.leadMinutes
        val out = ArrayList<RingEvent>()
        for (m in earlyMinutes(lead, prefs.preCount)) out.add(RingEvent(d.at - m * 60_000L, d, RingKind.PRE, m))
        out.add(RingEvent(d.at, d, RingKind.DUE, 0))
        val fg = prefs.followGapMinutes
        for (i in 1..prefs.followUps) {
            out.add(RingEvent(d.at + i * fg * 60_000L, d, RingKind.FOLLOW, i * fg))
        }
        if (prefs.caregiverSms && prefs.caregiverPhone.isNotBlank()) {
            val after = prefs.followUps * fg + 5
            out.add(RingEvent(d.at + after * 60_000L, d, RingKind.CARE_CHECK, after))
        }
        return out
    }
}
