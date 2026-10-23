package com.ragini.medreminder.ui

import com.ragini.medreminder.data.FoodRule
import com.ragini.medreminder.data.Medicine
import com.ragini.medreminder.data.Prefs
import com.ragini.medreminder.data.ScheduleType
import com.ragini.medreminder.data.minuteLabel
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.Locale

object Fmt {
    fun greeting(hour: Int = LocalTime.now().hour) = when (hour) {
        in 4..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..20 -> "Good evening"
        else -> "Good night"
    }

    /** "in 1 h 12 min", "in 5 min", "now", "25 min ago". */
    fun until(targetMs: Long, nowMs: Long): String {
        val mins = ((targetMs - nowMs) / 60_000L).toInt()
        return when {
            mins in 0..0 -> "now"
            mins > 0 -> "in " + span(mins)
            else -> span(-mins) + " ago"
        }
    }

    fun span(mins: Int): String {
        val h = mins / 60
        val m = mins % 60
        return when {
            h == 0 -> "$m min"
            m == 0 -> "$h h"
            else -> "$h h $m min"
        }
    }

    enum class Part(val title: String) { MORNING("Morning"), AFTERNOON("Afternoon"), EVENING("Evening"), NIGHT("Night") }

    fun part(minute: Int): Part = when (minute) {
        in 300 until 720 -> Part.MORNING
        in 720 until 1020 -> Part.AFTERNOON
        in 1020 until 1260 -> Part.EVENING
        else -> Part.NIGHT
    }

    private fun dayShort(iso: Int) = DayOfWeek.of(iso).getDisplayName(TextStyle.SHORT, Locale.ENGLISH)

    fun schedule(m: Medicine): String {
        val base = when (m.scheduleType) {
            ScheduleType.DAILY -> "Every day"
            ScheduleType.WEEKDAYS -> if (m.weekdays.size == 7) "Every day" else m.weekdays.sorted().joinToString(", ") { dayShort(it) }
            ScheduleType.EVERY_N_DAYS -> if (m.everyNDays <= 1) "Every day" else "Every ${m.everyNDays} days"
        }
        val food = if (m.food != FoodRule.NONE) " · ${m.food.label}" else ""
        val end = if (m.endDate != null) " · until ${m.endDate.dayOfMonth} ${m.endDate.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)}" else ""
        return base + food + end
    }

    fun times(m: Medicine) = m.times.joinToString("  ·  ") { minuteLabel(it) }

    /** The exact moments the voice will speak for a dose at [minute], e.g. 7:41 … 7:59, 8:00. */
    fun rhythm(minute: Int, lead: Int, prefs: Prefs): String {
        val gap = prefs.gapMinutes.coerceAtLeast(1)
        val pre = ArrayList<Int>()
        var m = lead
        while (m > 0) { pre += minute - m; m -= gap }
        val norm = { x: Int -> ((x % 1440) + 1440) % 1440 }
        val parts = ArrayList<String>()
        if (pre.isNotEmpty()) {
            parts += if (pre.size <= 4) pre.joinToString(", ") { minuteLabel(norm(it)) }
            else "${minuteLabel(norm(pre.first()))}, ${minuteLabel(norm(pre[1]))} … ${minuteLabel(norm(pre.last()))}"
        }
        val follow = if (prefs.followUps > 0) " and ${prefs.followUps} more after, every ${prefs.followGapMinutes} min, if there is no answer" else ""
        return if (pre.isEmpty()) "At ${minuteLabel(minute)}$follow."
        else "${pre.size} reminders: ${parts.first()}, then at ${minuteLabel(minute)}$follow."
    }
}
