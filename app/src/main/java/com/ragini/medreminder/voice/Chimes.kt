package com.ragini.medreminder.voice

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/** Plays one of the 100 reminder tunes ([TuneSynth]) on the ALARM sound channel. */
object Chimes {
    private val cache = HashMap<Int, ShortArray>()

    /** Renders (once) and caches a tune. Rendering happens off the main thread. */
    suspend fun pcm(tuneId: Int): ShortArray {
        synchronized(cache) { cache[tuneId]?.let { return it } }
        val data = withContext(Dispatchers.Default) { TuneSynth.pcm16(TuneSynth.byId(tuneId)) }
        synchronized(cache) {
            if (cache.size > 6) cache.clear()
            cache[tuneId] = data
        }
        return data
    }

    /** Plays the tune and returns when it has finished (or the caller is cancelled). */
    suspend fun play(tuneId: Int) {
        val data = pcm(tuneId)
        val track = try {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(TuneSynth.SR)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(data.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
        } catch (e: Exception) {
            return
        }
        try {
            track.write(data, 0, data.size)
            track.play()
            delay(data.size * 1000L / TuneSynth.SR + 80L)
        } finally {
            try { track.stop() } catch (_: Exception) {}
            track.release()
        }
    }
}
