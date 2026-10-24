package com.ragini.medreminder.voice

import org.junit.Assert.assertEquals
import org.junit.Test

/** "Ask me: Yes or Not yet" – what the phone hears is turned into yes / not yet. */
class HeardAnswerTest {
    @Test fun understandsYesAndNotYet() {
        for (t in listOf("Yes", "yes I have", "yeah", "Taken", "done", "haan", "हाँ", "हो", "okay")) assertEquals(t, "yes", Phrases.heardAnswer(t))
        for (t in listOf("not yet", "No", "later", "nahi", "नहीं", "अभी नहीं", "अजून नाही", "not taken yet")) assertEquals(t, "notyet", Phrases.heardAnswer(t))
        for (t in listOf("", "hello", "what time is it")) assertEquals(t, null, Phrases.heardAnswer(t))
    }
}
