package com.ragini.medreminder.report

import com.ragini.medreminder.data.DoseStatus
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** The data handed to report.js must have exactly the shape the Windows app produces. */
class ReportCardTest {
    @Test fun dataHasTheSharedShape() {
        val d = LocalDate.of(2026, 10, 20)
        val rows = listOf(
            Row("1@$d@480", 1, "Thyroxine", 0, d, 480, 1000L, DoseStatus.TAKEN, "Taken", 1200L, true),
            Row("2@$d@540", 2, "Metformin", 4, d, 540, 2000L, DoseStatus.NOT_TAKEN, "Not at home", 2500L, true),
            Row("2@$d@1260", 2, "Metformin", 4, d, 1260, 3000L, DoseStatus.MISSED, "", null, true)
        )
        val r = Report(Period.WEEKLY, d.minusDays(6), d, "14 – 20 Oct", rows, emptyList(), emptyList(), emptyList(), emptyList(), 2, 5, null, "11:59 PM")
        val o = JSONObject(ReportCard.json(r, "Aai", listOf("Morning" to 480, "Night" to 1200), madeAt = 42L))
        assertEquals("weekly", o.getString("period")); assertEquals("Weekly report", o.getString("title"))
        assertEquals("2026-10-14", o.getString("from")); assertEquals("2026-10-20", o.getString("to"))
        assertEquals("Aai", o.getString("person")); assertEquals(42L, o.getLong("madeAt"))
        assertEquals(5, o.getJSONObject("streak").getInt("best"))
        assertEquals("Night", o.getJSONArray("slots").getJSONObject(1).getString("name"))
        val a = o.getJSONArray("rows")
        assertEquals(listOf("taken", "not", "missed"), (0 until a.length()).map { a.getJSONObject(it).getString("status") })
        val m = a.getJSONObject(2)
        assertTrue(m.isNull("answeredAt")); assertEquals("2026-10-20", m.getString("day")); assertEquals(1260, m.getInt("minute")); assertTrue(m.getBoolean("final"))
    }
}
