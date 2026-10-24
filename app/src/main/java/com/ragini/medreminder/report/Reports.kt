package com.ragini.medreminder.report

import android.content.Context
import com.ragini.medreminder.data.Db
import com.ragini.medreminder.data.DoseStatus
import com.ragini.medreminder.data.Prefs
import com.ragini.medreminder.data.minuteLabel
import com.ragini.medreminder.schedule.DayCloser
import com.ragini.medreminder.schedule.Doses
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

enum class Period(val title: String, val tab: String) {
    YESTERDAY("Yesterday", "Yesterday"),
    DAILY("Today", "Daily"),
    OVERNIGHT("Overnight", "Overnight"),
    WEEKLY("Last 7 days", "Weekly"),
    MONTHLY("Last 30 days", "Monthly"),
    QUARTERLY("Last 3 months", "Quarterly"),
    HALF_YEARLY("Last 6 months", "6-Monthly"),
    YEARLY("Last 12 months", "Yearly");

    val multiDay: Boolean get() = this in setOf(WEEKLY, MONTHLY, QUARTERLY, HALF_YEARLY, YEARLY)
}

/** One dose inside a report. [final] = already written in the attendance register. */
data class Row(
    val key: String,
    val medId: Long,
    val medName: String,
    val colorIndex: Int,
    val day: LocalDate,
    val minute: Int,
    val scheduledAt: Long,
    val status: DoseStatus,
    val reason: String,
    val respondedAt: Long?,
    val final: Boolean
) {
    val attended: Boolean get() = status == DoseStatus.TAKEN || status == DoseStatus.NOT_TAKEN || status == DoseStatus.REMINDED
    val onTime: Boolean get() = status == DoseStatus.TAKEN && respondedAt != null && respondedAt - scheduledAt <= 30 * 60_000L
}

data class DayStat(val day: LocalDate, val total: Int, val taken: Int, val notTaken: Int, val missed: Int, val final: Boolean) {
    val attended get() = taken + notTaken
    /** 0..1, or -1 when no medicine was scheduled that day. */
    val attendance: Float get() = if (total == 0) -1f else attended.toFloat() / total
}

data class MedStat(val medId: Long, val name: String, val colorIndex: Int, val total: Int, val taken: Int, val notTaken: Int, val missed: Int) {
    val adherence: Float get() = if (total == 0) 0f else taken.toFloat() / total
}

data class PartStat(val title: String, val total: Int, val taken: Int)

data class Report(
    val period: Period,
    val from: LocalDate,
    val to: LocalDate,
    val rangeLabel: String,
    val rows: List<Row>,
    val days: List<DayStat>,
    val meds: List<MedStat>,
    val parts: List<PartStat>,
    val reasons: List<Pair<String, Int>>,
    val currentStreak: Int,
    val bestStreak: Int,
    val avgDelayMin: Int?,
    val closeTimeLabel: String
) {
    private val finals = rows.filter { it.final }
    val total = finals.size
    val taken = finals.count { it.status == DoseStatus.TAKEN }
    val notTaken = finals.count { it.status == DoseStatus.NOT_TAKEN }
    val missed = finals.count { it.status == DoseStatus.MISSED }
    val reminded = finals.count { it.status == DoseStatus.REMINDED }
    val attended = taken + notTaken + reminded
    val onTime = finals.count { it.onTime }
    val provisional = rows.filter { !it.final }

    /** Attendance: answered Taken or Not taken (with reason), counted when the day closes. */
    val attendancePct: Int get() = pct(attended, total)
    /** Adherence: actually taken. */
    val takenPct: Int get() = pct(taken, total)
    val onTimePct: Int get() = pct(onTime, taken)
    val perfectDays: Int get() = days.count { it.final && it.total > 0 && it.taken == it.total }
    val daysWithDoses: Int get() = days.count { it.final && it.total > 0 }

    private fun pct(a: Int, b: Int) = if (b == 0) 0 else (a * 100f / b).roundToInt()
}

object Reports {
    const val OVERNIGHT_FROM = 20 * 60 // 8:00 PM
    const val OVERNIGHT_TO = 8 * 60    // 8:00 AM

    private val dFmt = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)
    private val dFmtY = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)
    private val dayFmt = DateTimeFormatter.ofPattern("EEEE, d MMM", Locale.ENGLISH)

    fun build(ctx: Context, period: Period, today: LocalDate = LocalDate.now()): Report {
        DayCloser.closePending(ctx)
        val db = Db.get(ctx)
        val prefs = Prefs(ctx)
        val meds = db.medicines()
        val colorOf = meds.associate { it.id to it.colorIndex }

        val (from, to) = when (period) {
            Period.YESTERDAY -> today.minusDays(1) to today.minusDays(1)
            Period.DAILY -> today to today
            Period.OVERNIGHT -> today.minusDays(1) to today
            Period.WEEKLY -> today.minusDays(6) to today
            Period.MONTHLY -> today.minusDays(29) to today
            Period.QUARTERLY -> today.minusDays(89) to today
            Period.HALF_YEARLY -> today.minusDays(181) to today
            Period.YEARLY -> today.minusDays(364) to today
        }

        // 1. Closed days come from the attendance register.
        val rows = ArrayList<Row>()
        val closedDays = HashSet<LocalDate>()
        for (f in db.finals(from, to)) {
            closedDays += f.day
            rows += Row(f.doseKey, f.medId, f.medName, colorOf[f.medId] ?: 0, f.day, f.minuteOfDay, f.scheduledAt,
                f.status, f.reason, f.respondedAt, final = true)
        }
        // 2. Days not closed yet (normally only today) are shown live, marked as provisional.
        var d = from
        while (!d.isAfter(to)) {
            if (d !in closedDays && !db.isClosed(d)) {
                val answers = db.responsesBetween(Doses.epochMs(d, 0), Doses.epochMs(d.plusDays(1), 0))
                for (dose in Doses.on(meds, d) { it in answers }) {
                    val r = answers[dose.key]
                    rows += Row(dose.key, dose.medicine.id, dose.medicine.name, dose.medicine.colorIndex, d, dose.minuteOfDay,
                        dose.at, r?.status ?: DoseStatus.PENDING, r?.reason ?: "", r?.respondedAt, final = false)
                }
            }
            d = d.plusDays(1)
        }

        val inRange: List<Row> = if (period == Period.OVERNIGHT) {
            val start = Doses.epochMs(today.minusDays(1), OVERNIGHT_FROM)
            val end = Doses.epochMs(today, OVERNIGHT_TO)
            rows.filter { it.scheduledAt in start until end }
        } else rows
        val sorted = inRange.sortedBy { it.scheduledAt }

        // Day by day
        val byDay = sorted.groupBy { it.day }
        val days = ArrayList<DayStat>()
        d = from
        while (!d.isAfter(to)) {
            val list = byDay[d].orEmpty()
            days += DayStat(
                d, list.size,
                list.count { it.status == DoseStatus.TAKEN },
                list.count { it.status == DoseStatus.NOT_TAKEN },
                list.count { it.status == DoseStatus.MISSED },
                final = list.all { it.final } && (d in closedDays || db.isClosed(d))
            )
            d = d.plusDays(1)
        }

        val finals = sorted.filter { it.final }
        val medStats = finals.groupBy { it.medId }.map { (id, list) ->
            MedStat(id, list.last().medName, list.last().colorIndex, list.size,
                list.count { it.status == DoseStatus.TAKEN }, list.count { it.status == DoseStatus.NOT_TAKEN },
                list.count { it.status == DoseStatus.MISSED })
        }.sortedByDescending { it.total }

        val parts = listOf(
            "Morning" to (300 until 720),
            "Afternoon" to (720 until 1020),
            "Evening" to (1020 until 1260),
            "Night" to ((1260 until 1440) + (0 until 300))
        ).map { (title, range) ->
            val list = finals.filter { it.minute in range }
            PartStat(title, list.size, list.count { it.status == DoseStatus.TAKEN })
        }

        val reasons = finals.filter { it.status == DoseStatus.NOT_TAKEN }
            .groupingBy { it.reason.ifBlank { "No reason given" } }.eachCount()
            .toList().sortedByDescending { it.second }

        val delays = finals.filter { it.status == DoseStatus.TAKEN && it.respondedAt != null }
            .map { ((it.respondedAt!! - it.scheduledAt) / 60_000L).toInt() }
            .filter { it in -120..720 }
        val avgDelay = if (delays.isEmpty()) null else delays.average().roundToInt()

        val (cur, best) = streaks(ctx, today)

        val label = when (period) {
            Period.YESTERDAY -> from.format(dayFmt)
            Period.DAILY -> today.format(dayFmt)
            Period.OVERNIGHT -> "${minuteLabel(OVERNIGHT_FROM)} ${from.format(dFmt)} → ${minuteLabel(OVERNIGHT_TO)} ${to.format(dFmt)}"
            else -> "${from.format(dFmt)} – ${to.format(dFmtY)}"
        }
        return Report(period, from, to, label, sorted, days, medStats, parts, reasons, cur, best, avgDelay,
            minuteLabel(prefs.dayCloseMinute))
    }

    /** Current and best run of closed days where every dose was taken. */
    private fun streaks(ctx: Context, today: LocalDate): Pair<Int, Int> {
        val db = Db.get(ctx)
        val byDay = db.finals(today.minusDays(400), today).groupBy { it.day }
        if (byDay.isEmpty()) return 0 to 0
        var best = 0
        var run = 0
        var d = byDay.keys.min()
        val last = byDay.keys.max()
        while (!d.isAfter(last)) {
            val list = byDay[d]
            when {
                list.isNullOrEmpty() -> Unit // no medicines that day – does not break the run
                list.all { it.status == DoseStatus.TAKEN } -> { run++; best = maxOf(best, run) }
                else -> run = 0
            }
            d = d.plusDays(1)
        }
        return run to best
    }

    /** A plain-text report that can be sent on WhatsApp, SMS or email to family or the doctor. */
    fun shareText(r: Report, userName: String): String = buildString {
        val who = userName.ifBlank { "Medicine" }
        appendLine("💊 $who – ${r.period.tab} medicine report")
        appendLine(r.rangeLabel)
        appendLine()
        if (r.total > 0) {
            appendLine("Attendance: ${r.attendancePct}%  (${r.attended} of ${r.total} doses answered)")
            appendLine("Taken: ${r.taken}   Not taken: ${r.notTaken}   No answer: ${r.missed}")
            if (r.taken > 0) appendLine("On time (within 30 min): ${r.onTimePct}%")
            if (r.period.multiDay) appendLine("Perfect days: ${r.perfectDays} of ${r.daysWithDoses}")
            appendLine("Current streak: ${r.currentStreak} day(s) · Best: ${r.bestStreak}")
        } else {
            appendLine("No closed days in this period yet. Attendance is marked at ${r.closeTimeLabel}.")
        }
        if (r.meds.isNotEmpty()) {
            appendLine()
            appendLine("By medicine:")
            r.meds.forEach { m -> appendLine("• ${m.name}: ${m.taken}/${m.total} taken" + if (m.notTaken > 0) ", ${m.notTaken} not taken" else "") }
        }
        if (r.reasons.isNotEmpty()) {
            appendLine()
            appendLine("Reasons for not taking:")
            r.reasons.take(6).forEach { (reason, n) -> appendLine("• $reason ×$n") }
        }
        if (!r.period.multiDay) {
            appendLine()
            appendLine("Doses:")
            r.rows.forEach { row ->
                val st = if (row.final) row.status.label else "${row.status.label} (not closed yet)"
                val why = if (row.status == DoseStatus.NOT_TAKEN && row.reason.isNotBlank()) " – ${row.reason}" else ""
                appendLine("• ${minuteLabel(row.minute)} ${row.medName}: $st$why")
            }
        }
        appendLine()
        append("Sent from Ragini medicine reminder")
    }
}
