package com.ragini.medreminder.alarm

import com.ragini.medreminder.data.Medicine
import com.ragini.medreminder.schedule.RingEvent
import com.ragini.medreminder.schedule.RingKind

/**
 * Decides, for one reminder round, what is said and in which voice – so that when several medicines
 * are due at the same time, every one of them is named, one by one, and none is ever left out.
 *
 *  - Medicines are grouped by what kind of reminder it is (not answered yet → due now → early),
 *    and every open medicine is in exactly one group.
 *  - A group is spoken in ONE voice: the medicines' own voice if they all share it, otherwise the
 *    user's selected voice (Voices screen). A medicine's own voice never decides for the others.
 */
object SpeechPlan {
    data class Group(val kind: RingKind, val minutes: Int, val voiceId: String, val meds: List<Medicine>, val events: List<RingEvent>)

    private val order = listOf(RingKind.FOLLOW, RingKind.DUE, RingKind.PRE)

    fun of(open: List<RingEvent>, selectedVoice: String): List<Group> {
        // A family check is spoken like a "not answered yet" reminder.
        val norm = open.map { e -> if (e.kind == RingKind.CARE_CHECK) e.copy(kind = RingKind.FOLLOW) else e }
        val out = ArrayList<Group>()
        for (k in order) {
            norm.filter { it.kind == k }
                .groupBy { it.minutes }
                .toSortedMap(if (k == RingKind.PRE) compareByDescending<Int> { it } else compareBy { it })
                .forEach { (minutes, events) ->
                    val sorted = events.sortedWith(compareBy<RingEvent>({ it.dose.at }, { it.dose.medicine.name.lowercase() }, { it.dose.medicine.id }))
                    val meds = sorted.map { it.dose.medicine }.distinctBy { it.id }
                    val voices = meds.map { it.voiceId ?: selectedVoice }.distinct()
                    out += Group(k, minutes, voices.singleOrNull() ?: selectedVoice, meds, sorted)
                }
        }
        return out
    }
}
