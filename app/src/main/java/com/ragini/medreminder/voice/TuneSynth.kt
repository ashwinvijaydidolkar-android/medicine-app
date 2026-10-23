package com.ragini.medreminder.voice

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.tanh
import kotlin.random.Random

/**
 * 100 reminder tunes = 10 ragas × 10 instruments.
 *
 * Every melody is an original phrase written in a classical raga (ragas are scales and
 * moods, free for anyone to use). Each tune is synthesised on the phone – no audio files –
 * with real-instrument techniques: plucked strings (Karplus–Strong), struck bars and bells
 * (additive partials), a breathy bansuri with vibrato, a soft tanpura drone underneath the
 * Indian instruments, and a small concert-hall reverb so it sounds warm, not electronic.
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

    /** [notes] = (scale degree, beats). Degree 0 is Sa; degrees wrap into higher/lower octaves. */
    class Raga(
        val name: String,
        val mood: String,
        val scale: IntArray,
        val beat: Double,
        val shift: Int,
        val notes: List<Pair<Int, Double>>
    )

    class Tune(val id: Int, val raga: Raga, val instrument: Instrument) {
        val title: String get() = "${raga.name} · ${instrument.title}"
    }

    private fun n(vararg p: Pair<Int, Double>) = p.toList()

    val ragas: List<Raga> = listOf(
        Raga("Bhupali", "Bright, peaceful evening", intArrayOf(0, 2, 4, 7, 9), 0.34, 0, n(
            0 to .5, 1 to .5, 2 to .5, 3 to .5, 4 to 1.0, 5 to 1.5,
            4 to .5, 3 to .5, 2 to .5, 3 to .5, 2 to .5, 1 to .5, 0 to 2.0)),
        Raga("Yaman", "Graceful and serene", intArrayOf(0, 2, 4, 6, 7, 9, 11), 0.36, 2, n(
            -1 to .5, 1 to .5, 2 to .5, 3 to .5, 4 to 1.0, 6 to .5, 5 to .5,
            4 to 1.0, 3 to .5, 2 to .5, 1 to .5, 0 to 2.0)),
        Raga("Bhairav", "Devotional morning", intArrayOf(0, 1, 4, 5, 7, 8, 11), 0.42, -1, n(
            0 to .5, 1 to .75, 2 to .5, 3 to .5, 4 to 1.0, 5 to .75,
            4 to .5, 3 to .5, 2 to .5, 1 to .75, 0 to 2.0)),
        Raga("Kafi", "Folk-like and joyful", intArrayOf(0, 2, 3, 5, 7, 9, 10), 0.32, 1, n(
            0 to .5, 2 to .5, 3 to .5, 4 to 1.0, 5 to .5, 6 to .5, 7 to 1.0,
            6 to .5, 4 to .5, 3 to .5, 2 to .5, 1 to .5, 0 to 2.0)),
        Raga("Durga", "Cheerful and light", intArrayOf(0, 2, 5, 7, 9), 0.33, 3, n(
            0 to .5, 1 to .5, 2 to .5, 3 to 1.0, 4 to .5, 5 to 1.0,
            4 to .5, 3 to .5, 2 to .5, 1 to .5, 0 to 2.0)),
        Raga("Malkauns", "Deep and meditative", intArrayOf(0, 3, 5, 8, 10), 0.42, -2, n(
            0 to .75, 1 to .5, 2 to .75, 3 to .5, 4 to .5, 5 to 1.25,
            4 to .5, 3 to .5, 2 to .5, 1 to .5, 0 to 2.0)),
        Raga("Hamsadhwani", "Auspicious, like a temple festival", intArrayOf(0, 2, 4, 7, 11), 0.31, 0, n(
            0 to .5, 1 to .5, 2 to .5, 3 to .5, 4 to .5, 5 to 1.5,
            3 to .5, 4 to .5, 2 to .5, 3 to .5, 1 to .5, 0 to 2.0)),
        Raga("Khamaj", "Romantic and tender", intArrayOf(0, 2, 4, 5, 7, 9, 10), 0.35, 4, n(
            2 to .5, 3 to .5, 4 to .5, 5 to .5, 6 to 1.0, 5 to .5, 4 to .5,
            3 to .5, 2 to 1.0, 1 to .5, 0 to 2.0)),
        Raga("Bilawal", "Fresh, like sunrise", intArrayOf(0, 2, 4, 5, 7, 9, 11), 0.33, 5, n(
            0 to .5, 2 to .5, 4 to .5, 7 to 1.0, 6 to .5, 5 to .5,
            4 to .5, 3 to .5, 2 to .5, 1 to .5, 0 to 2.0)),
        Raga("Shivranjani", "Soulful and gentle", intArrayOf(0, 2, 3, 7, 9), 0.38, -3, n(
            0 to .5, 1 to .5, 2 to 1.0, 3 to .5, 4 to .5, 5 to 1.0,
            4 to .5, 3 to .5, 2 to .75, 1 to .5, 0 to 2.0))
    )

    val all: List<Tune> = ragas.flatMapIndexed { ri, r ->
        Instrument.entries.mapIndexed { ii, ins -> Tune(ri * 10 + ii, r, ins) }
    }

    fun byId(id: Int): Tune = all.getOrElse(id) { all[0] }

    private fun semis(scale: IntArray, deg: Int): Int {
        val size = scale.size
        val oct = deg.floorDiv(size)
        val idx = deg.mod(size)
        return scale[idx] + 12 * oct
    }

    private fun hz(midi: Double) = 440.0 * 2.0.pow((midi - 69.0) / 12.0)

    /** Renders the tune as mono samples between -1 and 1. */
    fun render(t: Tune): FloatArray {
        val r = t.raga
        val ins = t.instrument
        val beats = r.notes.sumOf { it.second }
        val tail = 2.4
        val buf = FloatArray(((beats * r.beat + tail) * SR).toInt())
        val root = ins.baseMidi + r.shift
        var time = 0.0
        r.notes.forEachIndexed { i, (deg, b) ->
            val dur = b * r.beat
            val last = i == r.notes.lastIndex
            val accent = if (i == 0 || last) 1.0 else 0.82 + 0.1 * ((i % 3) / 2.0)
            play(buf, ins, time, hz(root + semis(r.scale, deg).toDouble()), dur, accent, last)
            if (last && ins != Instrument.BANSURI) {
                // A soft chord on the final Sa makes the phrase feel resolved and complete.
                play(buf, ins, time + 0.03, hz(root + 7.0), dur, 0.32, true)
                play(buf, ins, time + 0.06, hz(root + 12.0), dur, 0.26, true)
            }
            time += dur
        }
        if (ins.indian) drone(buf, hz(root - 12.0), hz(root - 5.0))
        reverb(buf, if (ins == Instrument.BANSURI) 0.34 else 0.27)
        // Fade out and normalise.
        val fade = (0.45 * SR).toInt()
        for (k in 0 until fade) {
            val j = buf.size - fade + k
            if (j >= 0) buf[j] *= (1f - k.toFloat() / fade)
        }
        var peak = 1e-4f
        for (v in buf) if (abs(v) > peak) peak = abs(v)
        val g = 0.9f / peak
        for (k in buf.indices) buf[k] *= g
        return buf
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

    /** Decaying note made of sine partials (bells, bars, tines, hammered strings). */
    private fun additive(
        buf: FloatArray, start: Double, f: Double, len: Double, decay: Double,
        partials: List<Pair<Double, Double>>, attack: Double, vel: Double, tremolo: Double = 0.0
    ) {
        val s0 = (start * SR).toInt()
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
        val s0 = (start * SR).toInt()
        val n = (len * SR).toInt()
        val rnd = Random((f * 100).toInt())
        var noiseLp = 0.0
        var phase = 0.0
        for (i in 0 until n) {
            val j = s0 + i
            if (j >= buf.size) break
            val t = i.toDouble() / SR
            val attack = min(1.0, t / 0.07)
            val release = min(1.0, (len - t) / 0.12).coerceAtLeast(0.0)
            val vibDepth = if (t > 0.22) min(1.0, (t - 0.22) / 0.3) * 0.006 else 0.0
            val inst = f * (1.0 + vibDepth * sin(2 * PI * 5.2 * t))
            phase += 2 * PI * inst / SR
            noiseLp += 0.08 * ((rnd.nextDouble() * 2 - 1) - noiseLp)
            val tone = sin(phase) + 0.16 * sin(2 * phase) + 0.05 * sin(3 * phase)
            val breath = noiseLp * (0.5 + 0.5 * (1 - attack)) * 0.28
            buf[j] += (0.28 * vel * attack * release * (tone + breath)).toFloat()
        }
    }

    /** Karplus–Strong plucked string; [jawari] adds the sitar's bright buzz. */
    private fun pluck(buf: FloatArray, start: Double, f: Double, len: Double, vel: Double, damping: Double, jawari: Boolean) {
        val soft = if (jawari) 0.5 else 0.22
        val period = (SR / f).toInt().coerceAtLeast(2)
        val rnd = Random(f.toInt() * 7 + 3)
        // A softened noise burst gives a rounder, more musical pluck.
        val ring = DoubleArray(period)
        var lp = 0.0
        for (k in 0 until period) {
            lp += soft * ((rnd.nextDouble() * 2 - 1) - lp)
            ring[k] = lp
        }
        val s0 = (start * SR).toInt()
        val n = (len * SR).toInt()
        var idx = 0
        for (i in 0 until n) {
            val j = s0 + i
            if (j >= buf.size) break
            val cur = ring[idx]
            val nxt = ring[(idx + 1) % period]
            ring[idx] = damping * 0.5 * (cur + nxt)
            var out = cur
            if (jawari) out = tanh(out * 2.2) * 0.6 + out * 0.4
            val attack = min(1.0, i / (0.002 * SR))
            val fadeOut = min(1.0, (n - i).toDouble() / (0.05 * SR))
            buf[j] += ((if (jawari) 0.55 else 0.85) * vel * attack * fadeOut * out).toFloat()
            idx = (idx + 1) % period
        }
    }

    /** A quiet tanpura-like drone on Sa and Pa, slightly detuned so it gently breathes. */
    private fun drone(buf: FloatArray, sa: Double, pa: Double) {
        val n = buf.size
        for (i in 0 until n) {
            val t = i.toDouble() / SR
            val env = min(1.0, t / 0.9) * min(1.0, (n - i).toDouble() / (0.8 * SR))
            val v = 0.5 * (sin(2 * PI * sa * t) + 0.35 * sin(4 * PI * sa * t) + 0.12 * sin(6 * PI * sa * t)) +
                0.32 * (sin(2 * PI * pa * t) + 0.3 * sin(4 * PI * pa * t)) +
                0.25 * sin(2 * PI * (sa * 1.0015) * t)
            buf[i] += (0.045 * env * v).toFloat()
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
