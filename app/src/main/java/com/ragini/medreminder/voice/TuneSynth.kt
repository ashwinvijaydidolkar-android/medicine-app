package com.ragini.medreminder.voice

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.tanh
import kotlin.random.Random

/**
 * 200 reminder tunes = 20 ragas × 10 instruments.
 *   Tunes   0–99 : the first ten ragas (Bhupali … Shivranjani)
 *   Tunes 100–199: ten more ragas (Desh … Revati)
 *
 * Every tune is an original two-part melody written in a classical raga: a first phrase that
 * "asks" (it ends away from home) and a second phrase that "answers" and comes back to Sa.
 * Under the melody there is a quiet accompaniment layer – a plucked tanpura for the Indian
 * instruments, and a soft pad, bass and broken chords for the others – plus a little hall
 * reverb. Everything is synthesised on the phone; no audio files are needed.
 *
 * The music always plays on its own: the voice speaks only after the tune has finished,
 * so medicine names are never covered by music.
 *
 * Pure Kotlin – no Android classes – so it can be tested anywhere.
 */
object TuneSynth {
    const val SR = 32_000

    enum class Instrument(val title: String, val baseMidi: Int, val indian: Boolean) {
        SANTOOR("Santoor", 72, true),
        BANSURI("Bansuri flute", 72, true),
        SITAR("Sitar", 62, true),
        HARP("Harp", 67, false),
        MUSIC_BOX("Music box", 84, false),
        CELESTA("Celesta", 79, false),
        MARIMBA("Marimba", 72, false),
        KALIMBA("Kalimba", 72, false),
        VIBRAPHONE("Vibraphone", 69, false),
        PIANO("Soft piano", 67, false)
    }

    /**
     * A raga with its two melodic phrases. Notes are written "degree beats", where degree 0 is Sa,
     * degrees wrap into the octave above (e.g. 5 in a five-note scale) or below (-1).
     * [hasPa] = false for ragas without Pa; their drone and chords use Ma instead.
     */
    class Raga(
        val name: String,
        val mood: String,
        val scale: IntArray,
        val beat: Double,
        val shift: Int,
        val hasPa: Boolean,
        phraseA: String,
        phraseB: String
    ) {
        val a: List<Pair<Int, Double>> = parse(phraseA)
        val b: List<Pair<Int, Double>> = parse(phraseB)

        private fun parse(s: String): List<Pair<Int, Double>> = s.split(',').map { tok ->
            val (d, len) = tok.trim().split(' ').filter { it.isNotBlank() }
            d.toInt() to len.toDouble()
        }
    }

    class Tune(val id: Int, val raga: Raga, val instrument: Instrument) {
        val title: String get() = "${raga.name} · ${instrument.title}"
        val isNew: Boolean get() = id >= 100
    }

    private val S5 = intArrayOf(0, 2, 4, 7, 9)

    val ragas: List<Raga> = listOf(
        // ---- the first hundred (improved) ----
        Raga("Bhupali", "Bright, peaceful evening", S5, 0.34, 0, true,
            "0 .5,1 .5,2 .5,3 .5,4 1,3 .5,2 .5,3 .5,4 .5,5 1.5,4 .5,3 1",
            "5 .5,4 .5,3 .5,4 .5,2 1,1 .5,2 .5,3 .5,2 .5,1 .5,0 .5,1 .5,-1 .5,0 2"),
        Raga("Yaman", "Graceful and serene", intArrayOf(0, 2, 4, 6, 7, 9, 11), 0.34, 2, true,
            "-1 .5,1 .5,2 1,3 .5,4 .5,2 1,3 .5,4 .5,5 .5,4 .5,3 .5,2 .5,1 1",
            "4 .5,5 .5,6 .5,7 1.5,6 .5,5 .5,4 1,3 .5,2 .5,1 .5,2 .5,-1 .5,1 .5,0 2"),
        Raga("Bhairav", "Devotional morning", intArrayOf(0, 1, 4, 5, 7, 8, 11), 0.40, -1, true,
            "0 .5,1 1,2 .5,3 .5,4 1,5 1,4 .5,3 .5,4 1.5",
            "5 .5,6 .5,7 1,6 .5,5 1,4 .5,3 .5,2 .5,1 1,0 2"),
        Raga("Kafi", "Folk-like and joyful", intArrayOf(0, 2, 3, 5, 7, 9, 10), 0.31, 1, true,
            "0 .5,2 .5,3 .5,4 .5,3 .5,2 .5,1 1,2 .5,3 .5,4 1,6 .5,5 .5,4 1",
            "4 .5,5 .5,6 .5,7 1,6 .5,5 .5,4 .5,3 .5,2 .5,1 .5,2 .5,1 .5,0 2"),
        Raga("Durga", "Cheerful and light", intArrayOf(0, 2, 5, 7, 9), 0.32, 3, true,
            "0 .5,1 .5,2 1,3 .5,4 .5,3 .5,2 .5,1 1,2 .5,3 .5,4 1.5",
            "5 1,4 .5,3 .5,4 .5,3 .5,2 1,1 .5,2 .5,1 .5,-1 .5,0 2"),
        Raga("Malkauns", "Deep and meditative", intArrayOf(0, 3, 5, 8, 10), 0.40, -2, false,
            "0 .75,1 .25,2 .5,1 .5,2 1,3 .5,4 .5,3 1,2 1",
            "3 .5,4 .5,5 1,4 .5,3 .5,2 .5,1 .5,2 1,1 .5,-1 .5,0 2"),
        Raga("Hamsadhwani", "Auspicious, like a temple festival", intArrayOf(0, 2, 4, 7, 11), 0.30, 0, true,
            "0 .5,1 .5,2 .5,3 .5,4 .5,5 1,4 .5,3 1,2 .5,3 .5,4 1",
            "5 .5,6 .5,5 .5,4 .5,3 1,2 .5,3 .5,1 .5,2 .5,0 .5,-1 .5,0 2"),
        Raga("Khamaj", "Romantic and tender", intArrayOf(0, 2, 4, 5, 7, 9, 10), 0.34, 4, true,
            "2 .5,3 .5,4 .5,5 .5,6 1,5 .5,4 .5,3 .5,2 .5,3 1,4 1",
            "7 .5,6 .5,5 .5,4 .5,3 .5,2 .5,3 .5,4 .5,2 1,1 .5,-2 .5,0 2"),
        Raga("Bilawal", "Fresh, like sunrise", intArrayOf(0, 2, 4, 5, 7, 9, 11), 0.32, 5, true,
            "0 .5,2 .5,4 .5,5 .5,4 1,3 .5,2 .5,3 .5,4 .5,5 .5,6 .5,7 1.5",
            "6 .5,5 .5,4 .5,3 .5,2 1,4 .5,3 .5,1 .5,2 .5,-1 .5,0 2"),
        Raga("Shivranjani", "Soulful and gentle", intArrayOf(0, 2, 3, 7, 9), 0.37, -3, true,
            "0 .5,1 .5,2 1,1 .5,0 .5,1 1,2 .5,3 .5,4 1.5",
            "5 .75,4 .25,3 .5,4 .5,3 .5,2 1,1 .5,2 .5,1 .5,-1 .5,0 2"),
        // ---- one hundred new tunes ----
        Raga("Desh", "Monsoon joy", intArrayOf(0, 2, 5, 7, 11), 0.33, 2, true,
            "0 .5,1 .5,2 .5,3 1,4 .5,5 1,4 .5,3 .5,2 .5,1 1",
            "3 .5,4 .5,5 .5,6 1,5 .5,4 .5,3 .5,2 .5,1 .5,0 .5,1 .5,0 2"),
        Raga("Bageshri", "Night-time longing, very gentle", intArrayOf(0, 2, 3, 5, 9, 10), 0.38, 0, false,
            "-1 .5,0 .5,2 .5,3 1,4 .5,5 .5,4 1,3 .5,2 .5,3 1",
            "4 .5,5 .5,6 1,5 .5,4 .5,3 .5,2 .5,1 .5,2 .5,0 2"),
        Raga("Bihag", "Sweet and romantic", intArrayOf(0, 4, 5, 7, 11), 0.33, 1, true,
            "0 .5,1 .5,2 .5,3 1,4 .5,5 1,4 .5,3 .5,4 .5,2 .5,3 1",
            "5 .5,6 .5,5 .5,4 .5,3 1,2 .5,1 .5,2 .5,-1 .5,0 2"),
        Raga("Charukeshi", "Tender and touching", intArrayOf(0, 2, 4, 5, 7, 8, 10), 0.35, -1, true,
            "0 .5,1 .5,2 .5,3 .5,4 1,5 .5,4 .5,3 .5,2 .5,1 1",
            "4 .5,5 .5,6 .5,7 1,6 .5,5 .5,4 .5,3 .5,2 .5,1 .5,0 2"),
        Raga("Hindol", "Spring morning swing", intArrayOf(0, 4, 6, 9, 11), 0.36, 3, false,
            "0 1,1 .5,2 .5,3 1,2 .5,3 .5,4 .5,5 1.5",
            "4 .5,3 .5,2 1,1 .5,2 .5,3 .5,1 .5,0 2"),
        Raga("Kirwani", "Heartfelt, like a lullaby", intArrayOf(0, 2, 3, 5, 7, 8, 11), 0.36, 0, true,
            "0 .5,1 .5,2 .5,3 .5,4 1,5 .5,6 .5,7 1,6 .5,5 .5,4 1",
            "5 .5,4 .5,3 .5,2 .5,3 .5,2 .5,1 .5,-1 .5,0 2"),
        Raga("Madhuvanti", "Honey-sweet afternoon", intArrayOf(0, 2, 3, 6, 7, 9, 11), 0.35, 2, true,
            "-1 .5,1 .5,2 1,3 .5,4 1,6 .5,5 .5,4 1,3 .5,2 1",
            "4 .5,5 .5,6 .5,7 1,6 .5,4 .5,3 .5,2 .5,1 .5,-1 .5,0 2"),
        Raga("Jog", "Warm and hopeful", intArrayOf(0, 3, 4, 5, 7, 10), 0.34, -2, true,
            "0 .5,2 .5,3 .5,4 1,5 .5,6 1,5 .5,4 .5,3 .5,2 1",
            "4 .5,5 .5,4 .5,3 .5,1 .5,2 .5,1 .5,0 .5,-1 .5,0 2"),
        Raga("Abhogi", "Calm and devotional", intArrayOf(0, 2, 3, 5, 9), 0.37, 1, false,
            "0 .5,1 .5,2 .5,3 .5,4 1,5 1,4 .5,3 .5,2 1",
            "3 .5,4 .5,5 .5,6 1,5 .5,4 .5,3 .5,2 .5,1 .5,-1 .5,0 2"),
        Raga("Revati", "Peaceful temple chant", intArrayOf(0, 1, 5, 7, 10), 0.38, 0, true,
            "0 .5,1 .5,2 .5,3 1,4 .5,3 .5,2 .5,1 1",
            "3 .5,4 .5,5 1,4 .5,3 .5,2 .5,1 .5,0 .5,-1 .5,0 2")
    )

    val all: List<Tune> = ragas.flatMapIndexed { ri, r ->
        Instrument.entries.mapIndexed { ii, ins -> Tune(ri * 10 + ii, r, ins) }
    }

    fun byId(id: Int): Tune = all.getOrElse(id) { all[0] }

    private fun semis(scale: IntArray, deg: Int): Int {
        val size = scale.size
        return scale[deg.mod(size)] + 12 * deg.floorDiv(size)
    }

    private fun hz(midi: Double) = 440.0 * 2.0.pow((midi - 69.0) / 12.0)

    // ------------------------------------------------------------------ rendering

    /** Renders the tune as mono samples between -1 and 1. */
    fun render(t: Tune): FloatArray {
        val r = t.raga
        val ins = t.instrument
        val notes = r.a + r.b
        val beats = notes.sumOf { it.second }
        val tail = 2.6
        val total = beats * r.beat + tail
        val melody = FloatArray((total * SR).toInt())
        val accomp = FloatArray(melody.size)
        val root = ins.baseMidi + r.shift
        val bStart = r.a.sumOf { it.second } * r.beat

        // ---- melody, shaped like a sung phrase: it swells towards the middle of each phrase ----
        var time = 0.0
        notes.forEachIndexed { i, (deg, b) ->
            val inB = i >= r.a.size
            val idx = if (inB) i - r.a.size else i
            val len = if (inB) r.b.size else r.a.size
            val arch = 0.78 + 0.22 * sin(PI * (idx + 0.5) / len)
            val vel = (if (inB) 0.95 else 1.0) * arch * (if (b >= 1.0) 1.05 else 1.0)
            val last = i == notes.lastIndex
            val dur = b * r.beat
            val f = hz(root + semis(r.scale, deg).toDouble())
            // A tiny, steady human looseness in timing (never random from run to run).
            val nudge = if (i == 0) 0.0 else ((i * 7919) % 13 - 6) * 0.0012
            if (ins == Instrument.BANSURI && b >= 1.0 && !last) {
                // Kan swar: a quick grace note from the next note above, typical of the bansuri.
                val g = hz(root + semis(r.scale, deg + 1).toDouble())
                flute(melody, time + nudge, g, 0.06, vel * 0.7)
                flute(melody, time + nudge + 0.05, f, dur + 0.02, vel)
            } else {
                play(melody, ins, time + nudge, f, dur, vel, last)
            }
            if (last && ins != Instrument.BANSURI) {
                // A soft chord on the final Sa makes the melody feel resolved and complete.
                play(melody, ins, time + 0.03, hz(root + (if (r.hasPa) 7.0 else 5.0)), dur, 0.3, true)
                play(melody, ins, time + 0.06, hz(root + 12.0), dur, 0.24, true)
            }
            time += dur
        }

        // ---- accompaniment: quiet, low, and only ever Sa with Pa (or Ma), so it suits every raga ----
        val fifth = if (r.hasPa) 7.0 else 5.0
        if (ins.indian) {
            tanpura(accomp, paLow = hz(root - 24.0 + fifth), sa = hz(root - 12.0), saLow = hz(root - 24.0), step = r.beat * 2, total = total)
        } else {
            pad(accomp, hz(root - 12.0), hz(root - 12.0 + fifth), 0.0, bStart + 0.3)
            pad(accomp, hz(root - 12.0 + fifth), hz(root + 0.0), bStart, (time - bStart) + 1.8)
            bass(accomp, hz(root - 24.0), 0.0)
            bass(accomp, hz(root - 24.0 + fifth), bStart)
            bass(accomp, hz(root - 24.0), time - r.b.last().second * r.beat)
            if (ins == Instrument.HARP || ins == Instrument.PIANO) {
                arpeggio(accomp, ins, root - 12.0, fifth, r.beat, time - r.b.last().second * r.beat)
            }
        }
        val accompGain = if (ins.indian) 0.55f else 0.5f
        for (k in melody.indices) melody[k] += accomp[k] * accompGain

        reverb(melody, if (ins == Instrument.BANSURI) 0.34 else 0.28)

        // Gentle fade out and normalise.
        val fade = (0.5 * SR).toInt()
        for (k in 0 until fade) {
            val j = melody.size - fade + k
            if (j >= 0) melody[j] *= (1f - k.toFloat() / fade)
        }
        var peak = 1e-4f
        for (v in melody) if (abs(v) > peak) peak = abs(v)
        val g = 0.9f / peak
        for (k in melody.indices) melody[k] *= g
        return melody
    }

    private fun play(buf: FloatArray, ins: Instrument, start: Double, f: Double, dur: Double, vel: Double, last: Boolean) {
        val ring = if (last) 2.2 else 0.0
        when (ins) {
            Instrument.SANTOOR -> {
                // Hammered strings: a quick double strike, the santoor's signature shimmer.
                val parts = listOf(1.0 to 1.0, 2.0 to 0.45, 3.0 to 0.22, 4.02 to 0.12, 5.01 to 0.05)
                additive(buf, start, f, 1.6 + ring, 3.2, parts, 0.002, vel)
                if (dur >= 0.3) additive(buf, start + 0.085, f * 1.0006, 1.3 + ring, 3.6, parts, 0.002, vel * 0.42)
                if (dur >= 0.6) additive(buf, start + 0.17, f * 0.9995, 1.1 + ring, 4.0, parts, 0.002, vel * 0.3)
            }
            Instrument.BANSURI -> flute(buf, start, f, dur + if (last) 0.9 else 0.06, vel)
            Instrument.SITAR -> {
                pluck(buf, start, f, 1.8 + ring, vel, 0.9975, jawari = true)
                additive(buf, start, f * 2, 1.2 + ring, 2.5, listOf(1.0 to 0.08), 0.01, vel)
            }
            Instrument.HARP -> pluck(buf, start, f, 2.2 + ring, vel * 0.9, 0.9984, jawari = false)
            Instrument.MUSIC_BOX -> additive(buf, start, f, 1.8 + ring, 2.6,
                listOf(1.0 to 1.0, 2.0 to 0.22, 3.0 to 0.06, 4.17 to 0.07), 0.001, vel)
            Instrument.CELESTA -> additive(buf, start, f, 1.6 + ring, 2.4,
                listOf(1.0 to 1.0, 2.0 to 0.38, 3.0 to 0.12, 4.02 to 0.06), 0.002, vel)
            Instrument.MARIMBA -> additive(buf, start, f, 1.1 + ring * 0.5, 5.5,
                listOf(1.0 to 1.0, 3.93 to 0.28, 9.2 to 0.05), 0.002, vel)
            Instrument.KALIMBA -> {
                additive(buf, start, f, 1.6 + ring, 3.4, listOf(1.0 to 1.0, 5.95 to 0.16), 0.001, vel)
                additive(buf, start, f * 12.1, 0.05, 60.0, listOf(1.0 to 0.06), 0.0005, vel)
            }
            Instrument.VIBRAPHONE -> additive(buf, start, f, 2.0 + ring, 1.1,
                listOf(1.0 to 1.0, 4.0 to 0.1, 10.0 to 0.015), 0.003, vel, tremolo = 5.5)
            Instrument.PIANO -> {
                additive(buf, start, f, 1.9 + ring, 1.5, listOf(1.0 to 1.0, 2.0 to 0.36, 3.0 to 0.1, 4.0 to 0.04), 0.003, vel)
                additive(buf, start, f * 14.0, 0.08, 30.0, listOf(1.0 to 0.03), 0.001, vel)
            }
        }
    }

    // ------------------------------------------------------------------ accompaniment

    /** A real tanpura cycle: low Pa – Sa – Sa – low Sa, plucked softly over and over, with its buzzing jawari. */
    private fun tanpura(buf: FloatArray, paLow: Double, sa: Double, saLow: Double, step: Double, total: Double) {
        val cycle = listOf(paLow, sa, sa, saLow)
        var t = 0.0
        var k = 0
        while (t < total - 1.2) {
            // Darker and gentler than the sitar, so it stays a warm hum in the background.
            pluck(buf, t, cycle[k % 4], 2.6, 0.3, 0.9993, jawari = true, brightness = 0.12, drive = 1.4)
            t += step
            k++
        }
    }

    /** A soft sustained open chord, like strings far in the background. */
    private fun pad(buf: FloatArray, f1: Double, f2: Double, start: Double, len: Double) {
        if (len <= 0.1) return
        val s0 = (start * SR).toInt().coerceAtLeast(0)
        val n = (len * SR).toInt()
        for (i in 0 until n) {
            val j = s0 + i
            if (j >= buf.size) break
            val t = i.toDouble() / SR
            val env = min(1.0, t / 0.6) * min(1.0, (len - t) / 0.6).coerceAtLeast(0.0)
            val tt = (j).toDouble() / SR
            val v = sin(2 * PI * f1 * tt) + 0.6 * sin(2 * PI * f2 * tt) +
                0.35 * sin(2 * PI * f1 * 1.003 * tt) + 0.2 * sin(4 * PI * f1 * tt)
            buf[j] += (0.07 * env * v).toFloat()
        }
    }

    /** A round, low bass note under the start of each phrase. */
    private fun bass(buf: FloatArray, f: Double, start: Double) {
        additive(buf, start, f, 2.2, 1.3, listOf(1.0 to 1.0, 2.0 to 0.3, 3.0 to 0.08), 0.01, 0.75)
    }

    /** Quiet broken chords (Sa – Pa – Sa' – Pa), one note per beat, for harp and piano. */
    private fun arpeggio(buf: FloatArray, ins: Instrument, rootMidi: Double, fifth: Double, beat: Double, until: Double) {
        val pattern = listOf(0.0, fifth, 12.0, fifth)
        var t = beat
        var k = 0
        while (t < until - beat * 0.5) {
            val f = hz(rootMidi + pattern[k % 4])
            if (ins == Instrument.HARP) pluck(buf, t, f, 1.4, 0.2, 0.998, jawari = false, brightness = 0.15)
            else additive(buf, t, f, 1.2, 2.0, listOf(1.0 to 1.0, 2.0 to 0.3), 0.004, 0.2)
            t += beat
            k++
        }
    }

    // ------------------------------------------------------------------ instruments

    /** Decaying note made of sine partials (bells, bars, tines, hammered strings). */
    private fun additive(
        buf: FloatArray, start: Double, f: Double, len: Double, decay: Double,
        partials: List<Pair<Double, Double>>, attack: Double, vel: Double, tremolo: Double = 0.0
    ) {
        val s0 = (start * SR).toInt().coerceAtLeast(0)
        val n = (len * SR).toInt()
        val nyq = SR * 0.45
        for ((mult, amp) in partials) {
            val pf = f * mult
            if (pf >= nyq) continue
            val w = 2 * PI * pf / SR
            // Higher partials die away faster, like real struck instruments.
            val d = decay * (1.0 + 0.6 * (mult - 1.0))
            for (i in 0 until n) {
                val j = s0 + i
                if (j >= buf.size) break
                val t = i.toDouble() / SR
                val fadeOut = min(1.0, (n - i).toDouble() / (0.04 * SR))
                var env = min(1.0, t / attack) * exp(-d * t) * fadeOut
                if (tremolo > 0) env *= 0.78 + 0.22 * sin(2 * PI * tremolo * t)
                buf[j] += (0.3 * vel * amp * env * sin(w * i)).toFloat()
            }
        }
    }

    /** Bansuri: breathy, soft attack, gentle vibrato that blooms on long notes. */
    private fun flute(buf: FloatArray, start: Double, f: Double, len: Double, vel: Double) {
        val s0 = (start * SR).toInt().coerceAtLeast(0)
        val n = (len * SR).toInt()
        val rnd = Random((f * 100).toInt())
        var noiseLp = 0.0
        var phase = 0.0
        val attackTime = min(0.07, len * 0.4)
        val releaseTime = min(0.12, len * 0.4)
        for (i in 0 until n) {
            val j = s0 + i
            if (j >= buf.size) break
            val t = i.toDouble() / SR
            val attack = min(1.0, t / attackTime)
            val release = min(1.0, (len - t) / releaseTime).coerceAtLeast(0.0)
            val vibDepth = if (t > 0.22) min(1.0, (t - 0.22) / 0.3) * 0.006 else 0.0
            val inst = f * (1.0 + vibDepth * sin(2 * PI * 5.2 * t))
            phase += 2 * PI * inst / SR
            noiseLp += 0.08 * ((rnd.nextDouble() * 2 - 1) - noiseLp)
            val tone = sin(phase) + 0.16 * sin(2 * phase) + 0.05 * sin(3 * phase)
            val breath = noiseLp * (0.5 + 0.5 * (1 - attack)) * 0.28
            buf[j] += (0.28 * vel * attack * release * (tone + breath)).toFloat()
        }
    }

    /**
     * Karplus–Strong plucked string, exactly in tune (a fractional all-pass delay corrects the
     * pitch of high notes). [jawari] adds the sitar's and tanpura's bright buzz.
     */
    private fun pluck(
        buf: FloatArray, start: Double, f: Double, len: Double, vel: Double, damping: Double, jawari: Boolean,
        brightness: Double = if (jawari) 0.5 else 0.22, drive: Double = 2.2
    ) {
        val soft = brightness
        val loop = SR / f - 0.5                       // the averaging filter adds half a sample
        var period = floor(loop).toInt()
        var frac = loop - period
        if (frac < 0.15) { period -= 1; frac += 1.0 }  // keeps the all-pass well behaved
        period = period.coerceAtLeast(2)
        val c = (1.0 - frac) / (1.0 + frac)
        val rnd = Random(f.toInt() * 7 + 3)
        val line = DoubleArray(period)
        var lp = 0.0
        for (k in 0 until period) {
            lp += soft * ((rnd.nextDouble() * 2 - 1) - lp)
            line[k] = lp
        }
        val s0 = (start * SR).toInt().coerceAtLeast(0)
        val n = (len * SR).toInt()
        var idx = 0
        var prev = 0.0
        var apX = 0.0
        var apY = 0.0
        for (i in 0 until n) {
            val j = s0 + i
            if (j >= buf.size) break
            val cur = line[idx]
            val avg = damping * 0.5 * (cur + prev)
            prev = cur
            val ap = c * avg + apX - c * apY
            apX = avg
            apY = ap
            line[idx] = ap
            idx = (idx + 1) % period
            var out = cur
            if (jawari) out = tanh(out * drive) * 0.6 + out * 0.4
            val attack = min(1.0, i / (0.002 * SR))
            val fadeOut = min(1.0, (n - i).toDouble() / (0.05 * SR))
            buf[j] += ((if (jawari) 0.55 else 0.85) * vel * attack * fadeOut * out).toFloat()
        }
    }

    /** Small Schroeder reverb: four damped comb filters and two all-pass filters. */
    private fun reverb(buf: FloatArray, wet: Double) {
        val combs = intArrayOf(1130, 1173, 1082, 1032)
        val out = FloatArray(buf.size)
        for (d in combs) {
            val line = FloatArray(d)
            var p = 0
            var store = 0f
            for (i in buf.indices) {
                val y = line[p]
                store = y * 0.75f + store * 0.25f
                line[p] = buf[i] + store * 0.8f
                out[i] += y
                p = (p + 1) % d
            }
        }
        for (d in intArrayOf(403, 163)) {
            val line = FloatArray(d)
            var p = 0
            for (i in out.indices) {
                val b = line[p]
                val x = out[i]
                val y = -x + b
                line[p] = x + b * 0.5f
                out[i] = y
                p = (p + 1) % d
            }
        }
        val w = (wet / combs.size).toFloat()
        for (i in buf.indices) buf[i] = buf[i] * 0.85f + out[i] * w
    }

    /** 16-bit samples ready for the speaker. */
    fun pcm16(t: Tune): ShortArray {
        val f = render(t)
        return ShortArray(f.size) { (f[it] * Short.MAX_VALUE).toInt().toShort() }
    }
}
