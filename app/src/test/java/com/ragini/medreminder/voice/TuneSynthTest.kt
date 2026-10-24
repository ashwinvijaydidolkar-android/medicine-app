package com.ragini.medreminder.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** 100 tunes from Japan, Korea, America and Britain + 100 chimes, all playable and of a sensible length. */
class TuneSynthTest {
    @Test fun twoHundredSoundsInTheRightPlaces() {
        assertEquals(200, TuneSynth.all.size)
        TuneSynth.all.forEachIndexed { i, t -> assertEquals(i, t.id) }
        val count = TuneSynth.all.groupingBy { it.region }.eachCount()
        assertEquals(25, count[TuneSynth.Region.JAPAN]); assertEquals(25, count[TuneSynth.Region.KOREA])
        assertEquals(25, count[TuneSynth.Region.AMERICA]); assertEquals(25, count[TuneSynth.Region.BRITAIN])
        assertEquals(100, count[TuneSynth.Region.CHIMES])
        assertTrue(TuneSynth.all.take(100).none { it.region == TuneSynth.Region.CHIMES })
        assertEquals(200, TuneSynth.all.map { it.title }.toSet().size)
    }

    @Test fun everySoundRendersCleanly() {
        for (t in TuneSynth.all) {
            val f = TuneSynth.render(t)
            val seconds = f.size.toDouble() / TuneSynth.SR
            assertTrue("${t.title} length $seconds", seconds in 4.0..12.0)
            assertTrue("${t.title} has bad samples", f.all { !it.isNaN() && it >= -1f && it <= 1f })
            val rms = Math.sqrt(f.sumOf { (it * it).toDouble() } / f.size)
            assertTrue("${t.title} too quiet ($rms)", rms > 0.05)
        }
    }

    @Test fun melodiesFitTheirChords() {
        // Western melodies spread their chords evenly; each phrase must divide into whole chord spans.
        for (m in TuneSynth.melodies.filter { it.chordsA.isNotEmpty() }) {
            val a = m.a.sumOf { it.second }; val b = m.b.sumOf { it.second }
            assertEquals(m.name, 0.0, (a / m.chordsA.size) % 1.0, 1e-9)
            assertEquals(m.name, 0.0, (b / m.chordsB.size) % 1.0, 1e-9)
        }
    }
}
