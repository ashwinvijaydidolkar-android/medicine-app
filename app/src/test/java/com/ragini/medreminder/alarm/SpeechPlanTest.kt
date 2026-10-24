package com.ragini.medreminder.alarm

import com.ragini.medreminder.data.Dose
import com.ragini.medreminder.data.Medicine
import com.ragini.medreminder.schedule.RingEvent
import com.ragini.medreminder.schedule.RingKind
import com.ragini.medreminder.voice.Phrases
import com.ragini.medreminder.voice.VoiceCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** Several medicines at the same time: every one is named, one by one, in the user's selected voice. */
class SpeechPlanTest {
    private val day = LocalDate.of(2026, 10, 24)
    private val english = VoiceCatalog.all.filter { it.lang.isEnglish }
    private val selected = english[0].id

    private fun ev(id: Long, name: String, voice: String?, kind: RingKind = RingKind.DUE, minutes: Int = 0, minute: Int = 480): RingEvent {
        val m = Medicine(id = id, name = name, times = listOf(minute), startDate = day, voiceId = voice)
        return RingEvent(0L, Dose("$id@$day@$minute", m, day, minute, minute * 60_000L), kind, minutes)
    }

    @Test fun mixedVoicesAreSpokenInTheSelectedVoiceAndNoneIsLeftOut() {
        val names = listOf("Thyroxine", "Metformin", "Ecosprin", "Telmisartan", "Vitamin D3", "Calcium", "Folic acid")
        val open = names.mapIndexed { i, n -> ev(i + 1L, n, if (i % 2 == 0) english[1].id else null) }
        val plan = SpeechPlan.of(open, selected)
        assertEquals(1, plan.size)
        assertEquals(selected, plan[0].voiceId)
        val lines = Phrases.parts(english[0], plan[0].kind, plan[0].minutes, "Aai", plan[0].meds)
        // every medicine has its own numbered line, and the numbers run 1 to 7 with none skipped
        names.forEach { n -> assertTrue("$n missing", lines.any { it.startsWith("Number ") && it.contains(n) }) }
        (1..7).forEach { k -> assertTrue("number $k missing", lines.any { it.startsWith("Number $k of 7:") }) }
    }

    @Test fun sharedVoiceIsKept() {
        val open = listOf(ev(1, "A", english[2].id), ev(2, "B", english[2].id))
        assertEquals(english[2].id, SpeechPlan.of(open, selected).single().voiceId)
    }

    @Test fun everyOpenMedicineIsInExactlyOneGroup() {
        val open = listOf(
            ev(1, "A", null, RingKind.DUE), ev(2, "B", null, RingKind.DUE), ev(3, "C", null, RingKind.FOLLOW, 5),
            ev(4, "D", null, RingKind.CARE_CHECK, 5), ev(5, "E", null, RingKind.PRE, 3, 540), ev(6, "F", null, RingKind.PRE, 1, 538)
        )
        val ids = SpeechPlan.of(open, selected).flatMap { g -> g.meds.map { it.id } }
        assertEquals(listOf(1L, 2L, 3L, 4L, 5L, 6L), ids.sorted())
        assertEquals(ids.size, ids.toSet().size)
        assertEquals(RingKind.FOLLOW, SpeechPlan.of(open, selected).first().kind)   // not-yet-answered first
    }

    @Test fun twentyMedicinesAllNamed() {
        val open = (1..20).map { ev(it.toLong(), "Medicine $it", null) }
        val lines = Phrases.parts(english[0], RingKind.DUE, 0, "", SpeechPlan.of(open, selected).single().meds)
        assertEquals(20, lines.count { it.startsWith("Number ") })
    }
}
