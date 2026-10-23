package com.ragini.medreminder.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** The medicine list file must move between phones, and between the phone and the Windows app. */
class TransferTest {
    private fun res(name: String) = javaClass.classLoader!!.getResource(name).readText()

    @Test fun opensAListMadeByTheWindowsApp() {
        val p = Transfer.parse(res("windows-list.json"))
        assertEquals("Windows", p.from)
        assertEquals(listOf("Thyroxine", "Metformin"), p.meds.map { it.name })
        val thy = p.meds[0]
        assertEquals("Thy rox een", thy.spokenName)
        assertEquals(FoodRule.EMPTY, thy.food)
        assertEquals(PillShape.ROUND, thy.shape)
        assertEquals(28, thy.stock)
        assertNull(thy.leadMinutes)
        val met = p.meds[1]
        assertEquals(listOf(540, 1260), met.times)
        assertEquals(ScheduleType.WEEKDAYS, met.scheduleType)
        assertEquals(setOf(1, 3, 5), met.weekdays)
        assertEquals(15, met.leadMinutes)
        assertEquals(listOf("Wake up", "After breakfast", "Bedtime"), p.slots!!.map { it.first })
    }

    @Test fun skipsEntriesWithoutNameOrTime() {
        val p = Transfer.parse(res("android-list.json"))
        assertEquals(2, p.meds.size)
        assertEquals(2, p.unreadable)
        assertEquals("23 October 2026", p.exportedOn)
    }

    @Test fun roundTripKeepsEverything() {
        val meds = listOf(
            Medicine(name = "Ecosprin", dosage = "75 mg", times = listOf(1185), food = FoodRule.AFTER, colorIndex = 2,
                startDate = LocalDate.of(2026, 10, 1), endDate = LocalDate.of(2026, 12, 31), stock = 30, refillAt = 7, perDose = 2),
            Medicine(name = "Eye drops", times = listOf(480, 1320), scheduleType = ScheduleType.EVERY_N_DAYS, everyNDays = 2,
                shape = PillShape.DROPS, startDate = LocalDate.of(2026, 10, 5), active = false, leadMinutes = 10)
        )
        val back = Transfer.parse(Transfer.export(meds, listOf("Morning" to 480, "Night" to 1260)))
        assertEquals(meds.map { it.copy(createdAt = 0) }, back.meds.map { it.copy(createdAt = 0) })
        assertEquals(listOf("Morning" to 480, "Night" to 1260), back.slots)
        assertEquals("Android", back.from)
    }

    @Test fun badFilesGivePlainMessages() {
        fun msg(t: String) = try { Transfer.parse(t); "" } catch (e: Transfer.BadFile) { e.message ?: "" }
        assertTrue(msg("hello").contains("not a Ragini"))
        assertTrue(msg("""{"app":"Ragini","kind":"medicine-list","version":9,"medicines":[]}""").contains("newer Ragini"))
        assertTrue(msg("""{"app":"Ragini","kind":"medicine-list","medicines":[]}""").contains("no medicines"))
    }
}
