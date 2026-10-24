package com.ragini.medreminder.voice

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.tanh
import kotlin.random.Random

/**
 * 200 reminder sounds, all composed for Ragini and synthesised on the device (no audio files):
 *
 *   Tunes   0– 99: 100 melodies from four musical traditions – 25 each:
 *                  Japan (0–24), Korea (25–49), America (50–74), Britain (75–99).
 *                  Each tradition has 5 original melodies, each played on 5 of its own instruments.
 *   Tunes 100–199: 100 chimes – 10 chime instruments × 10 chime patterns.
 *
 * The melodies are new, written in each tradition's own scales and rhythms (Japanese in- and yo-scales,
 * Korean pentatonic folk modes with the janggu drum, American folk, hymn and blues, British pastoral,
 * Scottish, country-dance and ballad styles). They are not copies of existing songs.
 *
 * Every melody has a first phrase that "asks" (it ends away from home) and a second that "answers"
 * and comes home. Underneath is a quiet accompaniment in the same style, plus a little hall reverb.
 *
 * The music always plays on its own: the voice speaks only after the tune has finished, so medicine
 * names are never covered by music.
 *
 * Pure Kotlin – no Android classes – so it can be tested anywhere.
 */
object TuneSynth {
    const val SR = 32_000

    enum class Region(val title: String) {
        JAPAN("Japan"), KOREA("Korea"), AMERICA("America"), BRITAIN("Britain"), CHIMES("Chimes")
    }

    enum class Instrument(val title: String, val baseMidi: Int) {
        // Japan
        KOTO("Koto", 67), SHAKUHACHI("Shakuhachi flute", 69), SHAMISEN("Shamisen", 64), MUSIC_BOX("Music box", 84), KOTO_PIANO("Soft piano", 67),
        // Korea
        GAYAGEUM("Gayageum", 64), DAEGEUM("Daegeum flute", 72), HAEGEUM("Haegeum fiddle", 69), YANGGEUM("Yanggeum dulcimer", 72), CELESTA_K("Celesta", 79),
        // America
        GUITAR("Acoustic guitar", 64), BANJO("Banjo", 67), HARMONICA("Harmonica", 72), PIANO("Soft piano", 67), VIBRAPHONE("Vibraphone", 69),
        // Britain
        HARP("Celtic harp", 67), WHISTLE("Tin whistle", 79), FIDDLE("Fiddle", 69), CELESTA("Celesta", 79), ORGAN("Pipe organ", 67),
        // Chimes
        TUBULAR("Tubular bells", 69), WIND("Wind chimes", 81), GLASS("Glass harmonica", 76), BOWL("Singing bowl", 64), HANDBELL("Handbells", 74),
        GLOCK("Glockenspiel", 86), CRYSTAL("Crystal bells", 84), VIBES("Vibraphone chime", 72), HALL("Hall chime", 69), TEMPLE("Temple bell", 57)
    }

    /** How the accompaniment is played under a melody. */
    enum class Accomp { KOTO_DRONE, JANGGU, JANGGU_SLOW, BOOM_CHICK, WALTZ, HYMN, SWING, ARPEGGIO, JIG, NONE }

    /**
     * A melody. Notes are written "degree beats": degree 0 is the home note of [scale]; degrees wrap into
     * the octave above (e.g. 5 in a five-note scale) or below (-1). [chordsA]/[chordsB] (Western styles only)
     * are spread evenly over each phrase. [swing] delays off-beat eighths (blues); [snap] marks a Scotch snap.
     */
    class Melody(
        val name: String,
        val mood: String,
        val region: Region,
        val scale: IntArray,
        val beat: Double,
        val shift: Int,
        phraseA: String,
        phraseB: String,
        val accomp: Accomp,
        chordsA: String = "",
        chordsB: String = "",
        val swing: Double = 0.0
    ) {
        val a: List<Pair<Int, Double>> = parse(phraseA)
        val b: List<Pair<Int, Double>> = parse(phraseB)
        val chordsA: List<String> = chordsA.split(' ').filter { it.isNotBlank() }
        val chordsB: List<String> = chordsB.split(' ').filter { it.isNotBlank() }

        private fun parse(s: String): List<Pair<Int, Double>> = if (s.isBlank()) emptyList() else s.split(',').map { tok ->
            val (d, len) = tok.trim().split(' ').filter { it.isNotBlank() }
            d.toInt() to len.toDouble()
        }
    }

    class Tune(val id: Int, val melody: Melody, val instrument: Instrument) {
        val region: Region get() = melody.region
        /** World tunes are grouped by melody; chimes are grouped by chime instrument. */
        val group: String get() = if (region == Region.CHIMES) instrument.title else melody.name
        val variant: String get() = if (region == Region.CHIMES) melody.name else instrument.title
        val title: String get() = "$group · $variant"
        val mood: String get() = melody.mood
    }

    private val MAJOR = intArrayOf(0, 2, 4, 5, 7, 9, 11)
    private val MAJ_PENTA = intArrayOf(0, 2, 4, 7, 9)
    private val IN = intArrayOf(0, 1, 5, 7, 8)              // Japanese in / miyako-bushi
    private val YO = intArrayOf(0, 2, 5, 7, 9)              // Japanese yo (min'yō); also Korean pyeongjo
    private val RYUKYU = intArrayOf(0, 4, 5, 7, 11)         // Okinawan
    private val HIRAJOSHI = intArrayOf(0, 2, 3, 7, 8)
    private val GYEMYEONJO = intArrayOf(0, 3, 5, 7, 10)     // Korean "sad" mode
    private val MAJOR_BLUES = intArrayOf(0, 2, 3, 4, 7, 9)
    private val MIXOLYDIAN = intArrayOf(0, 2, 4, 5, 7, 9, 10)
    private val AEOLIAN = intArrayOf(0, 2, 3, 5, 7, 8, 10)

    val melodies: List<Melody> = listOf(
        // ------------------------------------------------------------------ Japan
        Melody("Cherry Blossom Path", "Gentle and wistful, like falling petals", Region.JAPAN, IN, 0.40, 0,
            "0 1,1 .5,2 .5,3 1,4 1,3 .5,2 .5,3 2",
            "5 1,4 .5,3 .5,2 1,1 .5,2 .5,1 1,0 2", Accomp.KOTO_DRONE),
        Melody("Island Breeze", "Sunny Okinawan morning", Region.JAPAN, RYUKYU, 0.30, 2,
            "0 .5,1 .5,2 .5,3 .5,4 1,3 .5,2 .5,1 .5,2 .5,3 2",
            "5 .5,4 .5,3 .5,4 .5,5 1,3 .5,2 .5,1 1,-1 .5,0 1.5", Accomp.KOTO_DRONE),
        Melody("Village Festival", "Lively summer festival", Region.JAPAN, YO, 0.28, 1,
            "0 .5,0 .5,1 .5,2 .5,3 1,2 .5,3 .5,4 1,3 .5,2 .5,3 2",
            "4 .5,5 .5,4 .5,3 .5,2 1,3 .5,2 .5,1 1,2 .5,1 .5,0 2", Accomp.KOTO_DRONE),
        Melody("Moonlit Garden", "Still and peaceful night", Region.JAPAN, HIRAJOSHI, 0.42, -1,
            "0 1,2 1,3 .5,4 .5,3 1,2 2",
            "5 1,4 .5,3 .5,4 1,2 .5,1 .5,0 2", Accomp.KOTO_DRONE),
        Melody("Morning Mist", "Soft mountain dawn", Region.JAPAN, YO, 0.36, -2,
            "3 .5,4 .5,5 1,4 .5,3 .5,2 1,3 .5,1 .5,2 2",
            "1 .5,2 .5,3 1,2 .5,1 .5,0 1,-1 .5,1 .5,0 2", Accomp.KOTO_DRONE),

        // ------------------------------------------------------------------ Korea
        Melody("Mountain Pass Song", "Warm folk song, swaying in three", Region.KOREA, MAJ_PENTA, 0.30, 0,
            "0 1.5,1 .5,2 1,3 2,2 .5,3 .5,4 1.5,3 .5,2 1,3 2,1 1",
            "4 1.5,5 .5,4 1,3 2,2 1,3 1.5,2 .5,1 1,2 1,1 1,0 1", Accomp.JANGGU),
        Melody("Autumn Moon", "Tender and thoughtful", Region.KOREA, GYEMYEONJO, 0.34, 2,
            "0 2,1 1,2 1.5,1 .5,0 1,-1 2,0 1,1 3",
            "2 2,3 1,4 1.5,3 .5,2 1,1 2,2 1,0 3", Accomp.JANGGU),
        Melody("Spring Field Dance", "Bright, skipping dance", Region.KOREA, YO, 0.26, 1,
            "0 1,1 .5,2 .5,3 1,2 1,3 1,4 1,3 1,2 .5,1 .5,2 1,3 2,2 1",
            "4 1,5 1,4 1,3 1.5,2 .5,3 1,2 1,1 1,2 1,1 1,0 2", Accomp.JANGGU),
        Melody("Riverside Evening", "Quiet riverside at dusk", Region.KOREA, GYEMYEONJO, 0.31, -1,
            "2 1,3 .5,2 .5,1 1,0 2,1 1,2 1.5,3 .5,4 1,3 3",
            "4 1,5 1,4 1,3 2,2 1,1 1,2 .5,1 .5,-1 1,0 3", Accomp.JANGGU),
        Melody("Palace Garden", "Stately and calm, like court music", Region.KOREA, YO, 0.46, -2,
            "0 2,1 1,2 1,3 2,2 1,1 1",
            "3 1,4 1,3 1,2 1,1 1.5,2 .5,0 2", Accomp.JANGGU_SLOW),

        // ------------------------------------------------------------------ America
        Melody("Front Porch Morning", "Cheerful country morning", Region.AMERICA, MAJOR, 0.30, 0,
            "0 .5,2 .5,4 1,5 .5,4 .5,3 1,2 .5,4 .5,7 1,6 .5,5 .5,4 1",
            "7 .5,6 .5,4 1,5 .5,7 .5,5 1,4 .5,3 .5,1 1,0 2", Accomp.BOOM_CHICK, "I IV I V", "I IV V I"),
        Melody("Prairie Sunset", "Easy-going cowboy waltz", Region.AMERICA, MAJ_PENTA, 0.30, 2,
            "0 1,1 .5,2 1.5,4 2,3 1,3 1,2 1,1 1,1 2,-2 1",
            "2 1,3 1,5 1,4 2,5 1,3 1.5,1 .5,-2 1,0 3", Accomp.WALTZ, "I IV I V", "I IV V I"),
        Melody("Sunday Hymn", "Calm and comforting, like a church hymn", Region.AMERICA, MAJOR, 0.40, -2,
            "2 1,1 .5,0 .5,3 1,5 1,4 1,2 1,1 2",
            "4 1,7 1,5 1,3 1,4 .5,3 .5,2 .5,1 .5,0 2", Accomp.HYMN, "I IV I V", "I IV V I"),
        Melody("Blue Bayou Stroll", "Easy, smiling blues", Region.AMERICA, MAJOR_BLUES, 0.32, -3,
            "0 .5,2 .25,3 .25,4 1,5 .5,4 .5,2 .5,0 .5,3 .5,4 .5,6 1,4 .5,1 .5,-2 1",
            "5 .5,6 .5,5 .5,4 .5,2 .25,3 .75,0 1,4 .5,3 .5,1 1,0 2", Accomp.SWING, "I7 IV7 I7 V7", "IV7 I7 V7 I", swing = 0.16),
        Melody("Hometown Parade", "Bright little march", Region.AMERICA, MAJOR, 0.26, 3,
            "0 .5,0 .5,2 .5,4 .5,7 1,4 1,5 .5,5 .5,3 .5,5 .5,4 1,1 1",
            "7 .5,6 .5,7 .5,4 .5,5 .5,7 .5,5 .5,3 .5,4 .5,6 .5,8 .5,6 .5,7 .5,4 .5,0 1", Accomp.BOOM_CHICK, "I I IV V", "I IV V I"),

        // ------------------------------------------------------------------ Britain
        Melody("Meadow Lark", "English countryside in spring", Region.BRITAIN, MAJOR, 0.32, 0,
            "0 .5,2 .5,4 1,5 1,7 .5,5 .5,3 .5,5 .5,7 1,6 1.5,4 .5",
            "7 .5,6 .5,7 1,5 .5,3 .5,5 1,4 .5,6 .5,8 .5,6 .5,7 2", Accomp.ARPEGGIO, "I vi IV V", "I IV V I"),
        Melody("Highland Mist", "Scottish hills, with a Scotch snap", Region.BRITAIN, MIXOLYDIAN, 0.34, 2,
            "0 .25,2 .75,4 1,6 .25,3 .75,1 1,2 .5,4 .5,7 1,6 1.5,3 .5",
            "7 .25,4 .75,2 1,1 .25,3 .75,-1 1,0 .5,2 .5,4 .5,2 .5,0 2", Accomp.ARPEGGIO, "I bVII I bVII", "I bVII I I"),
        Melody("Cotswold Jig", "Happy country dance", Region.BRITAIN, MAJOR, 0.17, 1,
            "0 1,2 1,4 1,7 2,4 1,6 1,4 1,1 1,4 2,6 1,7 1,6 1,7 1,9 2,7 1,8 1,6 1,4 1,1 3",
            "5 1,7 1,5 1,3 2,5 1,4 1,2 1,0 1,2 2,4 1,1 1,4 1,6 1,4 2,1 1,0 6", Accomp.JIG, "I V I V", "IV I V I"),
        Melody("Old Village Green", "Gentle old ballad", Region.BRITAIN, AEOLIAN, 0.32, -3,
            "0 1,2 1,4 1,3 1.5,1 .5,-1 1,0 1,2 1,5 1,6 2,3 1",
            "4 1.5,3 .5,2 1,1 1,3 1,1 1,2 1,0 1,-1 1,0 3", Accomp.WALTZ, "i bVII bVI bVII", "i bVII i i"),
        Melody("Parlour Waltz", "Graceful Victorian waltz", Region.BRITAIN, MAJOR, 0.30, -1,
            "4 2,2 1,3 1.5,5 .5,3 1,4 2,6 1,8 2,4 1",
            "7 2,4 1,5 1.5,3 .5,5 1,4 1,1 1,6 1,7 3", Accomp.WALTZ, "I ii V V", "I IV V I"),

        // ------------------------------------------------------------------ chime patterns (major scale)
        Melody("Ding-dong", "The classic two-note call", Region.CHIMES, MAJOR, 0.42, 0, "4 1,2 2,4 1,2 3", "", Accomp.NONE),
        Melody("Rising", "Three steps up", Region.CHIMES, MAJOR, 0.30, 0, "0 1,2 1,4 1,7 4", "", Accomp.NONE),
        Melody("Cascade", "A gentle waterfall", Region.CHIMES, MAJOR, 0.26, 0, "7 1,4 1,2 1,0 1,4 1,2 1,0 4", "", Accomp.NONE),
        Melody("Clock tower", "Quarter-hour bells", Region.CHIMES, MAJOR, 0.44, 0, "2 1,1 1,0 1,-3 2,-3 1,1 1,2 1,0 3", "", Accomp.NONE),
        Melody("Sparkle", "A quick shimmer", Region.CHIMES, MAJOR, 0.18, 0, "7 1,9 1,11 1,14 1,11 1,9 1,7 2,11 1,14 5", "", Accomp.NONE),
        Melody("Call and answer", "A question and a reply", Region.CHIMES, MAJOR, 0.30, 0, "4 1,7 2,5 1,2 2,4 1,1 1,0 4", "", Accomp.NONE),
        Melody("Triple ring", "Three rings and a bloom", Region.CHIMES, MAJOR, 0.34, 0, "4 1,4 1,4 1,7 4", "", Accomp.NONE),
        Melody("Sunrise", "Opening up like morning light", Region.CHIMES, MAJOR, 0.24, 0, "0 1,2 1,4 1,6 1,7 1,9 1,11 1,14 5", "", Accomp.NONE),
        Melody("Lullaby", "Soft and slow", Region.CHIMES, MAJOR, 0.40, 0, "4 1,2 .5,4 1.5,0 1,2 .5,0 3", "", Accomp.NONE),
        Melody("Two-tone", "Low and high, back and forth", Region.CHIMES, MAJOR, 0.38, 0, "4 1.5,0 1.5,4 1.5,0 1.5,7 3", "", Accomp.NONE)
    )

    private val instrumentsOf = mapOf(
        Region.JAPAN to listOf(Instrument.KOTO, Instrument.SHAKUHACHI, Instrument.SHAMISEN, Instrument.MUSIC_BOX, Instrument.KOTO_PIANO),
        Region.KOREA to listOf(Instrument.GAYAGEUM, Instrument.DAEGEUM, Instrument.HAEGEUM, Instrument.YANGGEUM, Instrument.CELESTA_K),
        Region.AMERICA to listOf(Instrument.GUITAR, Instrument.BANJO, Instrument.HARMONICA, Instrument.PIANO, Instrument.VIBRAPHONE),
        Region.BRITAIN to listOf(Instrument.HARP, Instrument.WHISTLE, Instrument.FIDDLE, Instrument.CELESTA, Instrument.ORGAN),
        Region.CHIMES to listOf(
            Instrument.TUBULAR, Instrument.WIND, Instrument.GLASS, Instrument.BOWL, Instrument.HANDBELL,
            Instrument.GLOCK, Instrument.CRYSTAL, Instrument.VIBES, Instrument.HALL, Instrument.TEMPLE
        )
    )

    val all: List<Tune> = run {
        val out = ArrayList<Tune>(200)
        // 0–99: region × melody × instrument
        for (region in listOf(Region.JAPAN, Region.KOREA, Region.AMERICA, Region.BRITAIN)) {
            for (m in melodies.filter { it.region == region }) for (ins in instrumentsOf.getValue(region)) out.add(Tune(out.size, m, ins))
        }
        // 100–199: chime instrument × pattern
        val patterns = melodies.filter { it.region == Region.CHIMES }
        for (ins in instrumentsOf.getValue(Region.CHIMES)) for (p in patterns) out.add(Tune(out.size, p, ins))
        out
    }

    val regions: List<Region> = Region.entries

    fun byId(id: Int): Tune = all.getOrElse(id) { all[0] }
    fun inRegion(r: Region): List<Tune> = all.filter { it.region == r }

    private fun semis(scale: IntArray, deg: Int): Int {
        val size = scale.size
        return scale[deg.mod(size)] + 12 * deg.floorDiv(size)
    }

    private fun hz(midi: Double) = 440.0 * 2.0.pow((midi - 69.0) / 12.0)

    /** Chord tones as semitones above the key note. */
    private fun chord(name: String): List<Int> = when (name) {
        "I" -> listOf(0, 4, 7); "ii" -> listOf(2, 5, 9); "iii" -> listOf(4, 7, 11); "IV" -> listOf(5, 9, 12)
        "V" -> listOf(7, 11, 14); "vi" -> listOf(9, 12, 16); "i" -> listOf(0, 3, 7); "iv" -> listOf(5, 8, 12)
        "v" -> listOf(7, 10, 14); "bVII" -> listOf(10, 14, 17); "bVI" -> listOf(8, 12, 15)
        "I7" -> listOf(0, 4, 7, 10); "IV7" -> listOf(5, 9, 12, 15); "V7" -> listOf(7, 11, 14, 17)
        else -> listOf(0, 4, 7)
    }

    // ------------------------------------------------------------------ rendering

    /** Renders the tune as mono samples between -1 and 1. */
    fun render(t: Tune): FloatArray = if (t.region == Region.CHIMES) renderChime(t) else renderMelody(t)

    private fun renderMelody(t: Tune): FloatArray {
        val m = t.melody
        val ins = t.instrument
        val notes = m.a + m.b
        val beatsA = m.a.sumOf { it.second }
        val beats = notes.sumOf { it.second }
        val tail = 2.6
        val total = beats * m.beat + tail
        val melody = FloatArray((total * SR).toInt())
        val accomp = FloatArray(melody.size)
        val root = ins.baseMidi + m.shift

        // ---- melody, shaped like a sung phrase: it swells towards the middle of each phrase ----
        var pos = 0.0          // in beats
        notes.forEachIndexed { i, (deg, b) ->
            val inB = i >= m.a.size
            val idx = if (inB) i - m.a.size else i
            val len = if (inB) m.b.size else m.a.size
            val arch = 0.78 + 0.22 * sin(PI * (idx + 0.5) / len)
            val vel = (if (inB) 0.95 else 1.0) * arch * (if (b >= 1.0) 1.05 else 1.0)
            val last = i == notes.lastIndex
            // Swing: off-beat eighths come a little late (blues).
            val off = (pos % 1.0)
            val swingShift = if (m.swing > 0 && abs(off - 0.5) < 1e-6) m.swing else 0.0
            val time = (pos + swingShift) * m.beat
            val dur = b * m.beat
            val f = hz(root + semis(m.scale, deg).toDouble())
            val nudge = if (i == 0) 0.0 else ((i * 7919) % 13 - 6) * 0.0012
            // Ornaments that belong to each tradition, on longer notes.
            val grace = b >= 1.0 && !last && when (ins) {
                Instrument.SHAKUHACHI, Instrument.DAEGEUM, Instrument.WHISTLE, Instrument.FIDDLE -> true
                else -> false
            }
            if (grace) {
                val g = hz(root + semis(m.scale, deg + 1).toDouble())
                play(melody, ins, time + nudge, g, 0.07, vel * 0.6, false)
                play(melody, ins, time + nudge + 0.06, f, dur - 0.04, vel, false)
            } else {
                play(melody, ins, time + nudge, f, dur, vel, last)
            }
            if (last && sustains(ins).not()) {
                // A soft chord on the final note makes the melody feel resolved and complete.
                play(melody, ins, time + 0.03, hz(root + 7.0), dur, 0.28, true)
                play(melody, ins, time + 0.06, hz(root + 12.0), dur, 0.22, true)
            }
            pos += b
        }
        val endBeats = beats - m.b.last().second

        // ---- accompaniment in the style of the tradition ----
        when (m.accomp) {
            Accomp.KOTO_DRONE -> {
                // Low koto plucks on the home note and the fifth, every two beats, over a faint pad.
                var p = 0.0; var k = 0
                while (p < endBeats + 0.01) {
                    val f = hz(root - 12.0 + if (k % 2 == 0) 0.0 else 7.0)
                    pluck(accomp, p * m.beat, f, 2.2, 0.32, 0.9984, jawari = false, brightness = 0.18)
                    p += 2.0; k++
                }
                pad(accomp, hz(root - 12.0), hz(root - 5.0), 0.0, total - 1.0)
            }
            Accomp.JANGGU, Accomp.JANGGU_SLOW -> {
                // The janggu drum: a deep "dung" and a light "deok" in a lilting pattern.
                val bar = if (m.accomp == Accomp.JANGGU) 3.0 else 2.0
                var p = 0.0
                while (p < beats - 0.01) {
                    drum(accomp, p * m.beat, low = true, vel = 0.9)
                    if (m.accomp == Accomp.JANGGU) {
                        drum(accomp, (p + 1.5) * m.beat, low = false, vel = 0.55)
                        drum(accomp, (p + 2.0) * m.beat, low = false, vel = 0.7)
                    } else drum(accomp, (p + 1.0) * m.beat, low = false, vel = 0.5)
                    p += bar
                }
                pad(accomp, hz(root - 12.0), hz(root - 5.0), 0.0, total - 1.0)
            }
            else -> westernAccomp(accomp, m, ins, root - 12.0, beatsA, beats)
        }
        val accompGain = when (m.accomp) { Accomp.JANGGU, Accomp.JANGGU_SLOW -> 0.55f; Accomp.HYMN -> 0.45f; else -> 0.5f }
        for (k in melody.indices) melody[k] += accomp[k] * accompGain

        reverb(melody, if (sustains(ins)) 0.32 else 0.28)
        return finish(melody)
    }

    private fun sustains(ins: Instrument) = when (ins) {
        Instrument.SHAKUHACHI, Instrument.DAEGEUM, Instrument.HAEGEUM, Instrument.HARMONICA,
        Instrument.WHISTLE, Instrument.FIDDLE, Instrument.ORGAN -> true
        else -> false
    }

    /** Chords for the Western styles: boom-chick, waltz, hymn, swing, broken chords or a jig. */
    private fun westernAccomp(buf: FloatArray, m: Melody, ins: Instrument, keyMidi: Double, beatsA: Double, beats: Double) {
        fun each(chords: List<String>, from: Double, span: Double, block: (List<Int>, Double, Double) -> Unit) {
            if (chords.isEmpty()) return
            val per = span / chords.size
            chords.forEachIndexed { i, c -> block(chord(c), from + i * per, per) }
        }
        val strum: (Double, Double, Double) -> Unit = { time, f, vel ->
            when (m.region) {
                Region.AMERICA -> pluck(buf, time, f, 1.3, vel, 0.997, jawari = false, brightness = 0.2)
                else -> if (ins == Instrument.ORGAN) organ(buf, time, f, 0.9, vel * 0.6)
                else pluck(buf, time, f, 1.6, vel, 0.998, jawari = false, brightness = 0.16)
            }
        }
        val b = m.beat
        val play: (List<Int>, Double, Double) -> Unit = { ch, at, span ->
            val bass = hz(keyMidi - 12.0 + ch[0])
            val alt = hz(keyMidi - 12.0 + ch[2] - 12)
            when (m.accomp) {
                Accomp.BOOM_CHICK -> {
                    var p = 0.0; var k = 0
                    while (p < span - 0.01) {
                        if (k % 2 == 0) bassNote(buf, (at + p) * b, if (k % 4 == 0) bass else alt, 0.7)
                        else ch.forEachIndexed { j, s -> strum((at + p) * b + j * 0.012, hz(keyMidi + s), 0.16) }
                        p += 1.0; k++
                    }
                }
                Accomp.WALTZ -> {
                    var p = 0.0
                    while (p < span - 0.01) {
                        bassNote(buf, (at + p) * b, bass, 0.65)
                        for (q in 1..2) if (p + q < span) ch.forEachIndexed { j, s -> strum((at + p + q) * b + j * 0.01, hz(keyMidi + s), 0.12) }
                        p += 3.0
                    }
                }
                Accomp.HYMN -> {
                    // Sustained four-part chords, like an organ in a quiet church.
                    ch.forEach { s -> organ(buf, at * b, hz(keyMidi + s), span * b + 0.25, 0.22) }
                    organ(buf, at * b, bass, span * b + 0.25, 0.3)
                }
                Accomp.SWING -> {
                    // Walking bass and soft chord stabs on beats 2 and 4.
                    val walk = listOf(0, 4, 7, 9)
                    var p = 0.0; var k = 0
                    while (p < span - 0.01) {
                        bassNote(buf, (at + p) * b, hz(keyMidi - 12.0 + ch[0] + walk[k % 4]), 0.6)
                        if (k % 2 == 1) ch.forEach { s -> additive(buf, (at + p + m.swing) * b, hz(keyMidi + s), 0.6, 4.0, listOf(1.0 to 1.0, 2.0 to 0.3), 0.004, 0.12) }
                        p += 1.0; k++
                    }
                }
                Accomp.ARPEGGIO -> {
                    val pattern = listOf(0, 1, 2, 1)
                    var p = 0.0; var k = 0
                    bassNote(buf, at * b, bass, 0.55)
                    while (p < span - 0.01) {
                        strum((at + p) * b, hz(keyMidi + ch[pattern[k % 4]]), 0.15)
                        p += 0.5; k++
                    }
                }
                Accomp.JIG -> {
                    // Units are eighth notes in 6/8: bass on 1 and 4, chord on 3 and 6.
                    var p = 0.0
                    while (p < span - 0.01) {
                        bassNote(buf, (at + p) * b, bass, 0.6)
                        ch.forEach { s -> strum((at + p + 2) * b, hz(keyMidi + s), 0.1) }
                        bassNote(buf, (at + p + 3) * b, alt, 0.45)
                        ch.forEach { s -> strum((at + p + 5) * b, hz(keyMidi + s), 0.1) }
                        p += 6.0
                    }
                }
                else -> {}
            }
        }
        each(m.chordsA, 0.0, beatsA, play)
        each(m.chordsB, beatsA, beats - beatsA, play)
        // Let the final chord ring on.
        val fin = chord(m.chordsB.lastOrNull() ?: "I")
        fin.forEach { s -> strum(beats * b - m.b.last().second * b + 0.02, hz(keyMidi + s), 0.12) }
    }

    /** Chimes: the pattern twice over – once clear, once softer and slower, like a real bell fading. */
    private fun renderChime(t: Tune): FloatArray {
        val m = t.melody
        val ins = t.instrument
        val beats = m.a.sumOf { it.second }
        val slow = when (ins) { Instrument.TEMPLE, Instrument.BOWL, Instrument.TUBULAR -> 1.35; Instrument.GLASS -> 1.25; else -> 1.0 }
        val beat = m.beat * slow
        val total = beats * beat + 4.2
        val buf = FloatArray((total * SR).toInt())
        val rnd = Random(t.id * 31 + 7)
        var pos = 0.0
        m.a.forEachIndexed { i, (deg, b) ->
            val f = hz(ins.baseMidi + m.shift + semis(m.scale, deg).toDouble())
            val loose = if (ins == Instrument.WIND) (rnd.nextDouble() - 0.5) * 0.08 else ((i * 7919) % 11 - 5) * 0.001
            chime(buf, ins, max(0.0, pos * beat + loose), f, b * beat, if (i == m.a.lastIndex) 1.0 else 0.9, i == m.a.lastIndex)
            pos += b
        }
        if (ins == Instrument.WIND) {
            // A breeze: a few soft extra tinkles from the same scale after the pattern.
            var tt = pos * beat * 0.6
            while (tt < total - 2.5) {
                val f = hz(ins.baseMidi + semis(m.scale, rnd.nextInt(0, 8)).toDouble())
                chime(buf, ins, tt, f, 0.3, 0.25 + rnd.nextDouble() * 0.2, false)
                tt += 0.35 + rnd.nextDouble() * 0.5
            }
        }
        reverb(buf, 0.36)
        return finish(buf)
    }

    private fun finish(buf: FloatArray): FloatArray {
        val fade = (0.5 * SR).toInt()
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

    // ------------------------------------------------------------------ melody instruments

    private fun play(buf: FloatArray, ins: Instrument, start: Double, f: Double, dur: Double, vel: Double, last: Boolean) {
        val ring = if (last) 2.2 else 0.0
        val hold = dur + if (last) 1.0 else 0.04
        when (ins) {
            Instrument.KOTO -> {
                pluck(buf, start, f, 2.0 + ring, vel, 0.9978, jawari = false, brightness = 0.38)
                additive(buf, start, f * 3.0, 0.08, 40.0, listOf(1.0 to 0.05), 0.0005, vel)   // the plectrum's click
            }
            Instrument.SHAKUHACHI -> flute(buf, start, f, hold, vel, breath = 0.5, vibRate = 4.6, vibDepth = 0.009, scoop = 0.6)
            Instrument.SHAMISEN -> {
                pluck(buf, start, f, 1.3 + ring * 0.6, vel * 0.9, 0.9955, jawari = true, brightness = 0.55, drive = 1.8)
                noiseHit(buf, start, 0.025, vel * 0.18, 0.35)                                     // the bachi striking the skin
            }
            Instrument.MUSIC_BOX -> additive(buf, start, f, 1.8 + ring, 2.6, listOf(1.0 to 1.0, 2.0 to 0.22, 3.0 to 0.06, 4.17 to 0.07), 0.001, vel)
            Instrument.KOTO_PIANO, Instrument.PIANO -> {
                additive(buf, start, f, 1.9 + ring, 1.5, listOf(1.0 to 1.0, 2.0 to 0.36, 3.0 to 0.1, 4.0 to 0.04), 0.003, vel)
                additive(buf, start, f * 14.0, 0.08, 30.0, listOf(1.0 to 0.03), 0.001, vel)
            }
            Instrument.GAYAGEUM -> {
                pluck(buf, start, f, 2.2 + ring, vel, 0.9981, jawari = false, brightness = 0.26)
                pluck(buf, start + 0.004, f * 1.0015, 1.8 + ring, vel * 0.35, 0.998, jawari = false, brightness = 0.2)
            }
            Instrument.DAEGEUM -> flute(buf, start, f, hold, vel, breath = 0.4, vibRate = 5.6, vibDepth = 0.013, scoop = 0.3, buzz = 0.12)
            Instrument.HAEGEUM -> bowed(buf, start, f, hold, vel * 0.9, vibRate = 5.8, vibDepth = 0.012, slide = 0.07, bright = 0.85)
            Instrument.YANGGEUM -> {
                val parts = listOf(1.0 to 1.0, 2.0 to 0.42, 3.0 to 0.2, 4.02 to 0.1)
                additive(buf, start, f, 1.5 + ring, 3.4, parts, 0.002, vel)
                if (dur >= 0.3) additive(buf, start + 0.08, f * 1.0006, 1.2 + ring, 3.8, parts, 0.002, vel * 0.38)
            }
            Instrument.CELESTA_K, Instrument.CELESTA -> additive(buf, start, f, 1.6 + ring, 2.4, listOf(1.0 to 1.0, 2.0 to 0.38, 3.0 to 0.12, 4.02 to 0.06), 0.002, vel)
            Instrument.GUITAR -> pluck(buf, start, f, 2.0 + ring, vel, 0.9982, jawari = false, brightness = 0.3)
            Instrument.BANJO -> {
                pluck(buf, start, f, 1.1 + ring * 0.5, vel * 0.9, 0.9935, jawari = false, brightness = 0.7)
                additive(buf, start, f * 2.0, 0.25, 12.0, listOf(1.0 to 0.12), 0.001, vel)
            }
            Instrument.HARMONICA -> reed(buf, start, f, hold, vel)
            Instrument.VIBRAPHONE -> additive(buf, start, f, 2.0 + ring, 1.1, listOf(1.0 to 1.0, 4.0 to 0.1, 10.0 to 0.015), 0.003, vel, tremolo = 5.5)
            Instrument.HARP -> pluck(buf, start, f, 2.2 + ring, vel * 0.9, 0.9984, jawari = false)
            Instrument.WHISTLE -> flute(buf, start, f, hold, vel * 0.85, breath = 0.22, vibRate = 5.5, vibDepth = 0.004, scoop = 0.0)
            Instrument.FIDDLE -> bowed(buf, start, f, hold, vel * 0.85, vibRate = 5.6, vibDepth = 0.007, slide = 0.0, bright = 1.0)
            Instrument.ORGAN -> organ(buf, start, f, hold, vel * 0.8)
            else -> chime(buf, ins, start, f, dur, vel, last)
        }
    }

    // ------------------------------------------------------------------ chime instruments

    private fun chime(buf: FloatArray, ins: Instrument, start: Double, f: Double, dur: Double, vel: Double, last: Boolean) {
        val ring = if (last) 1.6 else 0.0
        when (ins) {
            // Tubular bells: the famous inharmonic partials of a struck tube, with a long hum.
            Instrument.TUBULAR -> bell(buf, start, f, 4.0 + ring, vel, listOf(Triple(1.0, 1.0, 0.7), Triple(2.76, 0.5, 1.2), Triple(5.40, 0.25, 2.2), Triple(8.93, 0.1, 3.5), Triple(0.5, 0.18, 0.5)))
            Instrument.WIND -> bell(buf, start, f, 2.6 + ring, vel * 0.8, listOf(Triple(1.0, 1.0, 1.0), Triple(2.76, 0.45, 1.8), Triple(5.40, 0.18, 3.0)))
            Instrument.GLASS -> {
                // Glass harmonica: a wet finger on a turning glass – a soft swell and a gentle shimmer.
                val len = dur + 1.4 + ring
                swell(buf, start, f, len, vel * 0.9, listOf(1.0 to 1.0, 2.0 to 0.12, 3.0 to 0.04), attack = 0.12, tremolo = 6.0)
            }
            Instrument.BOWL -> bell(buf, start, f, 5.0 + ring, vel, listOf(Triple(1.0, 1.0, 0.35), Triple(1.004, 0.6, 0.35), Triple(2.71, 0.45, 0.8), Triple(2.716, 0.3, 0.8), Triple(5.06, 0.15, 1.6)))
            Instrument.HANDBELL -> bell(buf, start, f, 2.8 + ring, vel, listOf(Triple(1.0, 1.0, 1.0), Triple(2.0, 0.08, 1.5), Triple(3.0, 0.32, 2.2), Triple(4.2, 0.1, 3.0), Triple(5.4, 0.05, 4.0)))
            Instrument.GLOCK -> bell(buf, start, f, 1.8 + ring, vel * 0.85, listOf(Triple(1.0, 1.0, 1.6), Triple(2.76, 0.32, 3.0), Triple(5.40, 0.12, 5.0)))
            Instrument.CRYSTAL -> bell(buf, start, f, 2.4 + ring, vel * 0.85, listOf(Triple(1.0, 1.0, 1.2), Triple(2.32, 0.35, 2.0), Triple(4.25, 0.15, 3.2), Triple(6.63, 0.06, 4.5)))
            Instrument.VIBES -> additive(buf, start, f, 2.6 + ring, 0.9, listOf(1.0 to 1.0, 4.0 to 0.1, 10.0 to 0.015), 0.003, vel, tremolo = 4.8)
            // Hall chime: the warm two-bar doorbell of an old house.
            Instrument.HALL -> bell(buf, start, f, 3.2 + ring, vel, listOf(Triple(1.0, 1.0, 0.8), Triple(2.0, 0.2, 1.4), Triple(2.76, 0.3, 1.6), Triple(5.40, 0.08, 3.0)))
            Instrument.TEMPLE -> {
                bell(buf, start, f, 6.0 + ring, vel, listOf(Triple(0.5, 0.35, 0.28), Triple(1.0, 1.0, 0.4), Triple(1.19, 0.5, 0.6), Triple(1.56, 0.4, 0.8), Triple(2.0, 0.25, 1.1), Triple(2.51, 0.2, 1.4)))
                noiseHit(buf, start, 0.04, vel * 0.12, 0.08)                                      // the wooden striker
            }
            else -> additive(buf, start, f, 2.0 + ring, 2.0, listOf(1.0 to 1.0), 0.002, vel)
        }
    }

    // ------------------------------------------------------------------ building blocks

    /** Struck bell: partials (ratio, level, decay per second) that each fade at their own speed. */
    private fun bell(buf: FloatArray, start: Double, f: Double, len: Double, vel: Double, partials: List<Triple<Double, Double, Double>>) {
        val s0 = (start * SR).toInt().coerceAtLeast(0)
        val n = (len * SR).toInt()
        val nyq = SR * 0.45
        for ((mult, amp, decay) in partials) {
            val pf = f * mult
            if (pf >= nyq) continue
            val w = 2 * PI * pf / SR
            for (i in 0 until n) {
                val j = s0 + i
                if (j >= buf.size) break
                val t = i.toDouble() / SR
                val env = min(1.0, t / 0.0015) * exp(-decay * t) * min(1.0, (n - i).toDouble() / (0.06 * SR))
                buf[j] += (0.26 * vel * amp * env * sin(w * i)).toFloat()
            }
        }
    }

    /** Decaying note made of sine partials (bars, tines, hammered strings). */
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

    /** A sustained tone that swells in and out (glass harmonica). */
    private fun swell(buf: FloatArray, start: Double, f: Double, len: Double, vel: Double, partials: List<Pair<Double, Double>>, attack: Double, tremolo: Double) {
        val s0 = (start * SR).toInt().coerceAtLeast(0)
        val n = (len * SR).toInt()
        for (i in 0 until n) {
            val j = s0 + i
            if (j >= buf.size) break
            val t = i.toDouble() / SR
            val env = min(1.0, t / attack) * exp(-0.9 * t) * min(1.0, (len - t) / 0.4).coerceAtLeast(0.0) * (0.85 + 0.15 * sin(2 * PI * tremolo * t))
            var v = 0.0
            for ((mult, amp) in partials) v += amp * sin(2 * PI * f * mult * t)
            buf[j] += (0.28 * vel * env * v).toFloat()
        }
    }

    /**
     * Breathy flute (shakuhachi, daegeum, tin whistle): soft attack, a scoop up into the note,
     * vibrato that blooms on long notes, and for the daegeum the buzz of its paper membrane.
     */
    private fun flute(
        buf: FloatArray, start: Double, f: Double, len: Double, vel: Double,
        breath: Double, vibRate: Double, vibDepth: Double, scoop: Double, buzz: Double = 0.0
    ) {
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
            val vib = if (t > 0.22) min(1.0, (t - 0.22) / 0.3) * vibDepth else 0.0
            val bend = if (scoop > 0 && t < 0.09) -0.035 * scoop * (1 - t / 0.09) else 0.0
            val inst = f * (1.0 + bend + vib * sin(2 * PI * vibRate * t))
            phase += 2 * PI * inst / SR
            noiseLp += 0.08 * ((rnd.nextDouble() * 2 - 1) - noiseLp)
            var tone = sin(phase) + 0.16 * sin(2 * phase) + 0.05 * sin(3 * phase)
            if (buzz > 0) tone += buzz * tanh(4 * sin(phase)) * 0.5
            val air = noiseLp * (0.5 + 0.5 * (1 - attack)) * breath
            buf[j] += (0.28 * vel * attack * release * (tone + air)).toFloat()
        }
    }

    /** Bowed string (haegeum, fiddle): a bright sawtooth-like tone with a slide into the note and vibrato. */
    private fun bowed(buf: FloatArray, start: Double, f: Double, len: Double, vel: Double, vibRate: Double, vibDepth: Double, slide: Double, bright: Double) {
        val s0 = (start * SR).toInt().coerceAtLeast(0)
        val n = (len * SR).toInt()
        var phase = 0.0
        val attackTime = min(0.06, len * 0.35)
        val releaseTime = min(0.1, len * 0.35)
        val harmonics = 8
        for (i in 0 until n) {
            val j = s0 + i
            if (j >= buf.size) break
            val t = i.toDouble() / SR
            val env = min(1.0, t / attackTime) * min(1.0, (len - t) / releaseTime).coerceAtLeast(0.0)
            val vib = if (t > 0.18) min(1.0, (t - 0.18) / 0.25) * vibDepth else 0.0
            val gl = if (slide > 0 && t < slide) -0.03 * (1 - t / slide) else 0.0
            val inst = f * (1.0 + gl + vib * sin(2 * PI * vibRate * t))
            phase += 2 * PI * inst / SR
            var v = 0.0
            for (k in 1..harmonics) {
                if (inst * k > SR * 0.45) break
                v += sin(k * phase) / k * (if (k > 3) bright else 1.0)
            }
            buf[j] += (0.2 * vel * env * v).toFloat()
        }
    }

    /** Harmonica: a reedy tone with a hand tremolo on long notes. */
    private fun reed(buf: FloatArray, start: Double, f: Double, len: Double, vel: Double) {
        val s0 = (start * SR).toInt().coerceAtLeast(0)
        val n = (len * SR).toInt()
        val parts = doubleArrayOf(1.0, 0.7, 0.55, 0.3, 0.22, 0.12, 0.08)
        val attackTime = min(0.03, len * 0.3)
        val releaseTime = min(0.08, len * 0.3)
        for (i in 0 until n) {
            val j = s0 + i
            if (j >= buf.size) break
            val t = i.toDouble() / SR
            val trem = if (t > 0.25) 1.0 - 0.18 * min(1.0, (t - 0.25) / 0.3) * (0.5 + 0.5 * sin(2 * PI * 5.0 * t)) else 1.0
            val env = min(1.0, t / attackTime) * min(1.0, (len - t) / releaseTime).coerceAtLeast(0.0) * trem
            var v = 0.0
            for (k in parts.indices) {
                val pf = f * (k + 1)
                if (pf > SR * 0.45) break
                v += parts[k] * sin(2 * PI * pf * t)
            }
            buf[j] += (0.16 * vel * env * v).toFloat()
        }
    }

    /** Soft pipe organ: flue pipes with a little "chiff" of air at the start. */
    private fun organ(buf: FloatArray, start: Double, f: Double, len: Double, vel: Double) {
        val s0 = (start * SR).toInt().coerceAtLeast(0)
        val n = (len * SR).toInt()
        val parts = listOf(1.0 to 1.0, 2.0 to 0.45, 3.0 to 0.18, 4.0 to 0.2, 6.0 to 0.06, 8.0 to 0.05)
        val rnd = Random((f * 37).toInt())
        var lp = 0.0
        for (i in 0 until n) {
            val j = s0 + i
            if (j >= buf.size) break
            val t = i.toDouble() / SR
            val env = min(1.0, t / 0.05) * min(1.0, (len - t) / 0.15).coerceAtLeast(0.0)
            var v = 0.0
            for ((mult, amp) in parts) { val pf = f * mult; if (pf < SR * 0.45) v += amp * sin(2 * PI * pf * t) }
            lp += 0.2 * ((rnd.nextDouble() * 2 - 1) - lp)
            val chiff = if (t < 0.06) lp * (1 - t / 0.06) * 0.6 else 0.0
            buf[j] += (0.12 * vel * (env * v + chiff)).toFloat()
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
            val tt = j.toDouble() / SR
            val v = sin(2 * PI * f1 * tt) + 0.6 * sin(2 * PI * f2 * tt) + 0.35 * sin(2 * PI * f1 * 1.003 * tt)
            buf[j] += (0.05 * env * v).toFloat()
        }
    }

    /** A round, low bass note. */
    private fun bassNote(buf: FloatArray, start: Double, f: Double, vel: Double) {
        additive(buf, start, f, 1.2, 2.4, listOf(1.0 to 1.0, 2.0 to 0.3, 3.0 to 0.08), 0.008, vel)
    }

    /** Janggu drum: a deep "dung" (low skin) or a light "deok" (stick on the high skin). */
    private fun drum(buf: FloatArray, start: Double, low: Boolean, vel: Double) {
        val s0 = (start * SR).toInt().coerceAtLeast(0)
        val len = if (low) 0.45 else 0.12
        val n = (len * SR).toInt()
        val rnd = Random(if (low) 11 else 23)
        var phase = 0.0
        var hp = 0.0; var last = 0.0
        for (i in 0 until n) {
            val j = s0 + i
            if (j >= buf.size) break
            val t = i.toDouble() / SR
            val v = if (low) {
                val f = 62.0 + 70.0 * exp(-t * 28)
                phase += 2 * PI * f / SR
                sin(phase) * exp(-t * 7.5) * min(1.0, t / 0.002)
            } else {
                val x = rnd.nextDouble() * 2 - 1
                hp = 0.75 * (hp + x - last); last = x
                (hp * 0.7 + 0.5 * sin(2 * PI * 420 * t)) * exp(-t * 38)
            }
            buf[j] += (0.5 * vel * v).toFloat()
        }
    }

    /** A short burst of filtered noise: a plectrum, a striker or a bachi hitting the skin. */
    private fun noiseHit(buf: FloatArray, start: Double, len: Double, vel: Double, tone: Double) {
        val s0 = (start * SR).toInt().coerceAtLeast(0)
        val n = (len * SR).toInt()
        val rnd = Random(97)
        var lp = 0.0
        for (i in 0 until n) {
            val j = s0 + i
            if (j >= buf.size) break
            lp += tone * ((rnd.nextDouble() * 2 - 1) - lp)
            buf[j] += (vel * lp * (1.0 - i.toDouble() / n)).toFloat()
        }
    }

    /**
     * Karplus–Strong plucked string, exactly in tune (a fractional all-pass delay corrects the
     * pitch of high notes). [jawari] adds the buzz of the shamisen's sawari.
     */
    private fun pluck(
        buf: FloatArray, start: Double, f: Double, len: Double, vel: Double, damping: Double, jawari: Boolean,
        brightness: Double = if (jawari) 0.5 else 0.22, drive: Double = 2.2
    ) {
        val loop = SR / f - 0.5
        var period = floor(loop).toInt()
        var frac = loop - period
        if (frac < 0.15) { period -= 1; frac += 1.0 }
        period = period.coerceAtLeast(2)
        val c = (1.0 - frac) / (1.0 + frac)
        val rnd = Random(f.toInt() * 7 + 3)
        val line = DoubleArray(period)
        var lp = 0.0
        for (k in 0 until period) {
            lp += brightness * ((rnd.nextDouble() * 2 - 1) - lp)
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
