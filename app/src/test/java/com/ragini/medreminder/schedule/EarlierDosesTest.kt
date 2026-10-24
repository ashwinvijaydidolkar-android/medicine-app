package com.ragini.medreminder.schedule

import com.ragini.medreminder.data.Medicine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

/** Doses due earlier today, before a medicine was added, are asked about once and count only if answered. */
class EarlierDosesTest {
    private val today = LocalDate.now()
    private val nowMin = LocalTime.now().let { it.hour * 60 + it.minute }

    @Test fun asksAboutDosesDueBeforeTheMedicineWasAdded() {
        if (nowMin < 5) return                        // too close to midnight for an "earlier" dose
        val now = System.currentTimeMillis()
        val later = (nowMin + 30).coerceAtMost(1439)
        val m = Medicine(id = 7, name = "Thyroxine", times = listOf(0, later), startDate = today, createdAt = now)
        val asked = Doses.earlierToday(listOf(m), now, { false })
        assertEquals(listOf(0), asked.map { it.minuteOfDay })   // midnight was earlier; the later dose is not asked about

        // Not answered: the earlier dose is left out of today. Answered: it counts.
        assertTrue(Doses.on(listOf(m), today).none { it.minuteOfDay == 0 })
        val key = asked.first().key
        assertTrue(Doses.on(listOf(m), today) { it == key }.any { it.minuteOfDay == 0 })

        // Once answered it is not asked again.
        assertTrue(Doses.earlierToday(listOf(m), now, { it == key }).isEmpty())
    }

    @Test fun medicinesAddedLongAgoAreNotAskedAbout() {
        val now = System.currentTimeMillis()
        val m = Medicine(id = 8, name = "Metformin", times = listOf(0), startDate = today.minusDays(3), createdAt = now - 3 * 86_400_000L)
        assertTrue(Doses.earlierToday(listOf(m), now, { false }).isEmpty())
    }
}
