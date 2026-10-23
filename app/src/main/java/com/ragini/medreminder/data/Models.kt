package com.ragini.medreminder.data

import java.time.LocalDate

enum class ScheduleType { DAILY, WEEKDAYS, EVERY_N_DAYS }

enum class FoodRule(val label: String) {
    NONE("Any time"),
    BEFORE("Before food"),
    AFTER("After food"),
    WITH("With food"),
    EMPTY("Empty stomach"),
    BEDTIME("At bedtime")
}

enum class PillShape(val label: String) {
    CAPSULE("Capsule"),
    ROUND("Tablet"),
    OVAL("Oval"),
    LIQUID("Syrup"),
    DROPS("Drops"),
    INJECTION("Injection")
}

/** A medicine the user wants to be reminded about. */
data class Medicine(
    val id: Long = 0,
    val name: String,
    /** Optional: how the voice should pronounce the name, e.g. "Met for min". */
    val spokenName: String = "",
    val dosage: String = "",
    val notes: String = "",
    /** Minutes after midnight, e.g. 8:30 AM = 510. */
    val times: List<Int>,
    val scheduleType: ScheduleType = ScheduleType.DAILY,
    /** ISO day numbers, Monday = 1 … Sunday = 7. */
    val weekdays: Set<Int> = (1..7).toSet(),
    val everyNDays: Int = 1,
    val startDate: LocalDate,
    val endDate: LocalDate? = null,
    val food: FoodRule = FoodRule.NONE,
    val colorIndex: Int = 0,
    val shape: PillShape = PillShape.CAPSULE,
    /** Null means "use the default voice". */
    val voiceId: String? = null,
    /** Null means "use the default reminder start (19 min)". */
    val leadMinutes: Int? = null,
    val stock: Int? = null,
    val perDose: Int = 1,
    val refillAt: Int = 5,
    val active: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
) {
    val lowStock: Boolean get() = stock != null && stock <= refillAt
    /** What the voice says. */
    val sayName: String get() = spokenName.ifBlank { name }
}

enum class DoseStatus(val label: String) {
    TAKEN("Taken"),
    NOT_TAKEN("Not taken"),
    MISSED("No answer"),
    PENDING("Upcoming")
}

/** What the user answered after closing a reminder. */
data class DoseResponse(
    val doseKey: String,
    val medId: Long,
    val scheduledAt: Long,
    val status: DoseStatus,
    val reason: String,
    val respondedAt: Long
)

/** One dose as it was recorded when the day was closed (the attendance register). */
data class FinalDose(
    val doseKey: String,
    val medId: Long,
    val medName: String,
    val day: LocalDate,
    val minuteOfDay: Int,
    val scheduledAt: Long,
    val status: DoseStatus,
    val reason: String,
    val respondedAt: Long?
)

/** A single scheduled dose of a medicine on a given day. */
data class Dose(
    val key: String,
    val medicine: Medicine,
    val day: LocalDate,
    val minuteOfDay: Int,
    val at: Long
)

/** A voice recorded by a family member. */
data class RecordedVoice(val id: String, val label: String, val path: String)

object Reasons {
    val notTaken = listOf(
        "Already took it earlier",
        "Feeling unwell / side effects",
        "Medicine finished",
        "Doctor said to skip",
        "Not at home",
        "Will take it later",
        "Fasting today"
    )
}

fun minuteLabel(minuteOfDay: Int): String {
    val h = minuteOfDay / 60
    val m = minuteOfDay % 60
    val h12 = if (h % 12 == 0) 12 else h % 12
    val ap = if (h < 12) "AM" else "PM"
    return "%d:%02d %s".format(h12, m, ap)
}
