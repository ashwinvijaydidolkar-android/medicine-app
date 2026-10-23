package com.ragini.medreminder.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import java.time.LocalTime

/**
 * PRAHAR – Ragini's design system.
 *
 * Indian classical music divides the day into eight prahars of three hours, and every raga
 * belongs to one. Ragini's colours follow the same clock: the app wears the sky of the current
 * prahar, and the reminder screen wears the deep sky of the medicine's own time.
 * Everything is built on an 8-fold symmetry: the chakra dial, the mandala, the mala of beads.
 *
 * The same values are used by the Windows app (ui/prahar.js), so both feel like one product.
 */
data class Prahar(
    val key: String,
    val name: String,
    val english: String,
    val fromHour: Int,
    /** Main colour for buttons and highlights. */
    val accent: Color,
    /** Text/icons placed on [accent]. */
    val onAccent: Color,
    /** Accent strong enough to be text on a light background. */
    val ink: Color,
    /** Accent bright enough to be text on a dark background. */
    val glow: Color,
    /** The header sky, top → bottom. */
    val skyTop: Color,
    val skyBottom: Color,
    /** Text on the header sky. */
    val skyInk: Color,
    /** The reminder screen: always dark, like an alarm. */
    val deepA: Color,
    val deepB: Color
) {
    val fromMinute get() = fromHour * 60
    /** English name of the colour palette, e.g. "Dusk". */
    val label get() = english
    fun contains(minute: Int) = minute in fromMinute until fromMinute + 180
}

object Prahars {
    val all = listOf(
        Prahar("nishith", "Midnight", "Midnight", 0, c(0x3F5FB5), c(0xFFFFFF), c(0x3F5FB5), c(0x93A9F2), c(0x1B2656), c(0x34478F), c(0xFFFFFF), c(0x060A1E), c(0x1B2656)),
        Prahar("usha", "Before dawn", "Before dawn", 3, c(0x7A58B0), c(0xFFFFFF), c(0x6E4CA6), c(0xBBA0E8), c(0x3E3175), c(0x8E6DB0), c(0xFFFFFF), c(0x110B28), c(0x3E3175)),
        Prahar("pratah", "Dawn", "Dawn", 6, c(0xE0702F), c(0x2A1200), c(0xA8460F), c(0xFFA56B), c(0xFFAE6E), c(0xFFE6CC), c(0x2A1200), c(0x2A0E12), c(0x9C3E1A)),
        Prahar("purvahna", "Morning", "Morning", 9, c(0xE3A21A), c(0x2A1B00), c(0x8A5D00), c(0xFFC94D), c(0xFFD05C), c(0xFFF3CC), c(0x2A1B00), c(0x271900), c(0x835700)),
        Prahar("madhyahna", "Noon", "Noon", 12, c(0x2496C4), c(0x00202E), c(0x0E6A90), c(0x7FCBF0), c(0x82CAF2), c(0xE8F6FF), c(0x0B2230), c(0x031D2A), c(0x0E6A90)),
        Prahar("aparahna", "Afternoon", "Afternoon", 15, c(0xB8592F), c(0xFFFFFF), c(0xA04A22), c(0xF4A47C), c(0xF4A57E), c(0xFDE7DA), c(0x2A1208), c(0x2A0F08), c(0x8E3E1C)),
        Prahar("sayankal", "Dusk", "Dusk", 18, c(0xB9457A), c(0xFFFFFF), c(0xA83A6C), c(0xF28DBA), c(0xC9557F), c(0x4E3784), c(0xFFFFFF), c(0x1A0A1D), c(0x6E2A55)),
        Prahar("pradosh", "Evening", "Evening", 21, c(0x4E46B4), c(0xFFFFFF), c(0x4E46B4), c(0xA39CF2), c(0x3D3A9A), c(0x17163F), c(0xFFFFFF), c(0x07061B), c(0x2C2878))
    )

    /** Display order for the day: from dawn round to the small hours. */
    val dayOrder: List<Prahar> get() = all.drop(2) + all.take(2)

    fun at(minuteOfDay: Int): Prahar = all[(((minuteOfDay % 1440) + 1440) % 1440) / 180]
    fun now(): Prahar { val t = LocalTime.now(); return at(t.hour * 60 + t.minute) }
    fun byKey(key: String?): Prahar? = all.firstOrNull { it.key == key }

    /** "live" follows the sky; any prahar key fixes the colours. */
    fun current(setting: String): Prahar = byKey(setting) ?: now()

    private fun c(rgb: Int) = Color(0xFF000000 or rgb.toLong())
}

val LocalPrahar = staticCompositionLocalOf { Prahars.all[2] }

/**
 * A time of day that the user names and places, e.g. "After breakfast" from 8:30 AM.
 * It lasts until the next one starts. Its colours come from the clock (the prahar at its middle).
 */
data class TimeSlot(val name: String, val start: Int, val end: Int) {
    val palette: Prahar get() = Prahars.at((start + end) / 2)
    fun contains(minute: Int): Boolean {
        val m = ((minute % 1440) + 1440) % 1440
        return (m >= start && m < end) || (m + 1440 >= start && m + 1440 < end)
    }
}

object Slots {
    val defaults: List<Pair<String, Int>> = listOf(
        "Early morning" to 300, "Morning" to 480, "Afternoon" to 720,
        "Evening" to 1020, "Night" to 1200, "Late night" to 1380
    )

    /** Tidies the user's list: named, 1–12 entries, sorted, no two starting at the same minute. */
    fun clean(list: List<Pair<String, Int>>): List<Pair<String, Int>> {
        val seen = HashSet<Int>()
        val out = list.mapNotNull { (n, s) ->
            val name = n.replace(Regex("\\s+"), " ").trim().take(24)
            if (name.isEmpty() || s !in 0..1439 || !seen.add(s)) null else name to s
        }.sortedBy { it.second }.take(12)
        return out.ifEmpty { defaults }
    }

    /** Stored as "name|start" lines; names cannot contain line breaks. */
    fun encode(list: List<Pair<String, Int>>): String = clean(list).joinToString("\n") { "${it.first.replace("|", "/")}|${it.second}" }
    fun decode(raw: String?): List<Pair<String, Int>> = clean(
        raw.orEmpty().lines().mapNotNull { l -> l.substringAfterLast('|', "").toIntOrNull()?.let { l.substringBeforeLast('|') to it } }
    )

    fun build(list: List<Pair<String, Int>>): List<TimeSlot> {
        val a = clean(list)
        return a.mapIndexed { i, (n, s) -> TimeSlot(n, s, if (i + 1 < a.size) a[i + 1].second else a[0].second + 1440) }
    }

    fun at(minute: Int, slots: List<TimeSlot>): TimeSlot = slots.firstOrNull { it.contains(minute) } ?: slots.first()
}

val LocalSlots = staticCompositionLocalOf { Slots.build(Slots.defaults) }
