package com.ragini.medreminder.schedule

import org.junit.Assert.assertEquals
import org.junit.Test

/** The user chooses how many reminders and over how many minutes; Ragini spaces them evenly. */
class EarlyMinutesTest {
    @Test fun spacing() {
        assertEquals(listOf(19, 17, 15, 13, 11, 9, 7, 5, 3, 1), Doses.earlyMinutes(19, 10))   // the old "every 2 minutes"
        assertEquals(listOf(20, 15, 11, 6, 1), Doses.earlyMinutes(20, 5))
        assertEquals(listOf(30, 1), Doses.earlyMinutes(30, 2))
        assertEquals(listOf(15), Doses.earlyMinutes(15, 1))
        assertEquals(listOf(5, 4, 3, 2, 1), Doses.earlyMinutes(5, 20))                         // at most one per minute
        assertEquals(emptyList<Int>(), Doses.earlyMinutes(19, 0))
        assertEquals(emptyList<Int>(), Doses.earlyMinutes(0, 5))
    }

    @Test fun alwaysTheChosenNumberWhenThereIsRoom() {
        for (lead in 1..60) for (n in 1..20) {
            val m = Doses.earlyMinutes(lead, n)
            assertEquals("lead $lead n $n", minOf(n, lead), m.size)
            assertEquals(lead, m.first()); if (m.size > 1) assertEquals(1, m.last())
            assertEquals(m.sortedDescending(), m)
        }
    }
}
