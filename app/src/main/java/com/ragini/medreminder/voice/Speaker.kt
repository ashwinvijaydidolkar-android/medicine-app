package com.ragini.medreminder.voice

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.audiofx.LoudnessEnhancer
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume

/**
 * Wraps Android text-to-speech. Everything is played on the ALARM channel so it is loud
 * and is heard even when the phone is on silent / standby.
 */
class Speaker(context: Context) {
    private val app = context.applicationContext
    private val ready = CompletableDeferred<Boolean>()
    private val waiting = ConcurrentHashMap<String, CancellableContinuation<Unit>>()
    private val tts: TextToSpeech = TextToSpeech(app) { status -> ready.complete(status == TextToSpeech.SUCCESS) }

    private val alarmAttrs: AudioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ALARM)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()

    init {
        tts.setAudioAttributes(alarmAttrs)
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) = finish(utteranceId)
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) = finish(utteranceId)
            override fun onError(utteranceId: String?, errorCode: Int) = finish(utteranceId)
            override fun onStop(utteranceId: String?, interrupted: Boolean) = finish(utteranceId)
        })
    }

    private fun finish(id: String?) {
        if (id == null) return
        waiting.remove(id)?.let { if (it.isActive) it.resume(Unit) }
    }

    suspend fun awaitReady(timeoutMs: Long = 5000): Boolean =
        withTimeoutOrNull(timeoutMs) { ready.await() } ?: false

    /** True when the phone has the language installed for this persona. */
    suspend fun hasLanguage(lang: Lang): Boolean {
        if (!awaitReady()) return false
        return try {
            tts.isLanguageAvailable(Locale.forLanguageTag(lang.tag)) >= TextToSpeech.LANG_AVAILABLE
        } catch (e: Exception) {
            false
        }
    }

    private fun pickVoice(p: VoicePersona, locale: Locale): Voice? {
        val all: List<Voice> = try {
            tts.voices?.toList()
        } catch (e: Exception) {
            null
        } ?: return null

        fun installed(v: Voice) = v.features?.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED) != true

        var c = all.filter {
            it.locale.language == locale.language && it.locale.country.equals(locale.country, true) && installed(it)
        }
        if (c.isEmpty()) c = all.filter { it.locale.language == locale.language && installed(it) }
        if (c.isEmpty()) return null
        val offline = c.filter { !it.isNetworkConnectionRequired }
        if (offline.isNotEmpty()) c = offline
        val gendered = c.filter {
            val n = it.name.lowercase()
            if (p.female) n.contains("female") else (n.contains("male") && !n.contains("female"))
        }
        if (gendered.isNotEmpty()) c = gendered
        c = c.sortedBy { it.name }
        return c[p.variant % c.size]
    }

    private fun apply(p: VoicePersona) {
        val locale = Locale.forLanguageTag(p.lang.tag)
        val v = pickVoice(p, locale)
        if (v != null) tts.setVoice(v) else tts.setLanguage(locale)
        tts.setPitch(p.pitch)
        tts.setSpeechRate(p.rate)
    }

    /**
     * Speaks [text] with [persona] and suspends until it has finished.
     * With [loud] the speech is first rendered to a file and played through Android's
     * LoudnessEnhancer, which makes it clearly louder than ordinary text-to-speech.
     */
    suspend fun speak(text: String, persona: VoicePersona, loud: Boolean = false) {
        if (text.isBlank() || !awaitReady()) return
        // Text-to-speech refuses anything longer than its limit (about 4000 letters). A message
        // is normally one short sentence, but if one is ever longer it is split, never cut off.
        val limit = (try { TextToSpeech.getMaxSpeechInputLength() } catch (e: Exception) { 4000 }) - 200
        if (text.length > limit) {
            var rest = text
            while (rest.isNotEmpty()) {
                var cut = if (rest.length <= limit) rest.length else rest.lastIndexOf(' ', limit).takeIf { it > 0 } ?: limit
                speak(rest.substring(0, cut), persona, loud)
                rest = rest.substring(cut).trimStart()
                if (cut == 0) break
            }
            return
        }
        apply(persona)
        if (loud && speakLoud(text)) return
        val id = UUID.randomUUID().toString()
        withTimeoutOrNull(text.length * 160L + 10_000L) {
            suspendCancellableCoroutine<Unit> { cont ->
                waiting[id] = cont
                cont.invokeOnCancellation {
                    waiting.remove(id)
                    tts.stop()
                }
                val params = Bundle().apply {
                    putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_ALARM)
                    putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1f)
                }
                val result = tts.speak(text, TextToSpeech.QUEUE_FLUSH, params, id)
                if (result != TextToSpeech.SUCCESS) {
                    waiting.remove(id)
                    if (cont.isActive) cont.resume(Unit)
                }
            }
        }
    }

    private suspend fun speakLoud(text: String): Boolean {
        val file = File(app.cacheDir, "say-${UUID.randomUUID()}.wav")
        val id = UUID.randomUUID().toString()
        try {
            val started = withTimeoutOrNull(text.length * 150L + 15_000L) {
                suspendCancellableCoroutine<Unit> { cont ->
                    waiting[id] = cont
                    cont.invokeOnCancellation { waiting.remove(id) }
                    val result = tts.synthesizeToFile(text, Bundle(), file, id)
                    if (result != TextToSpeech.SUCCESS) {
                        waiting.remove(id)
                        if (cont.isActive) cont.resume(Unit)
                    }
                }
            }
            if (started == null || !file.exists() || file.length() < 2_000L) return false
            return playFile(file.absolutePath, boostMb = 900)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return false
        } finally {
            file.delete()
        }
    }

    /** Plays a recording on the alarm channel. Returns false if it could not be played. */
    suspend fun playFile(path: String, boostMb: Int = 600): Boolean {
        val mp = MediaPlayer()
        var enhancer: LoudnessEnhancer? = null
        try {
            mp.setAudioAttributes(alarmAttrs)
            mp.setDataSource(path)
            mp.prepare()
            try {
                enhancer = LoudnessEnhancer(mp.audioSessionId).apply {
                    setTargetGain(boostMb)
                    enabled = true
                }
            } catch (_: Exception) {
                // Not available on this phone – plays at normal (maximum alarm) volume.
            }
            withTimeoutOrNull(mp.duration.toLong().coerceAtLeast(1000L) + 3000L) {
                suspendCancellableCoroutine<Unit> { cont ->
                    mp.setOnCompletionListener { if (cont.isActive) cont.resume(Unit) }
                    mp.setOnErrorListener { _, _, _ -> if (cont.isActive) cont.resume(Unit); true }
                    cont.invokeOnCancellation { try { mp.stop() } catch (_: Exception) {} }
                    mp.start()
                }
            }
            return true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return false
        } finally {
            try { enhancer?.release() } catch (_: Exception) {}
            mp.release()
        }
    }

    fun stop() {
        try { tts.stop() } catch (_: Exception) {}
    }

    fun shutdown() {
        stop()
        try { tts.shutdown() } catch (_: Exception) {}
    }
}
