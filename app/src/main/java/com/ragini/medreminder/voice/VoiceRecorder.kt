package com.ragini.medreminder.voice

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File
import java.util.UUID

/** Records a short message from a loved one ("Maa, it's time for your tablet – love, Ashwin"). */
class VoiceRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    var file: File? = null
        private set

    val isRecording: Boolean get() = recorder != null

    fun start(): Boolean {
        stop()
        val dir = File(context.filesDir, "voices").apply { mkdirs() }
        val f = File(dir, "${UUID.randomUUID()}.m4a")
        val r = if (Build.VERSION.SDK_INT >= 31) MediaRecorder(context) else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
        return try {
            r.setAudioSource(MediaRecorder.AudioSource.MIC)
            r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            r.setAudioEncodingBitRate(96_000)
            r.setAudioSamplingRate(44_100)
            r.setOutputFile(f.absolutePath)
            r.prepare()
            r.start()
            recorder = r
            file = f
            true
        } catch (e: Exception) {
            r.release()
            f.delete()
            false
        }
    }

    /** Returns the recorded file, or null if nothing usable was recorded. */
    fun stop(): File? {
        val r = recorder ?: return null
        recorder = null
        val ok = try {
            r.stop()
            true
        } catch (e: Exception) {
            false
        } finally {
            r.release()
        }
        val f = file
        if (!ok) f?.delete()
        return if (ok) f else null
    }

    /** 0..1 – how loud the microphone is right now, for the live level meter. */
    fun level(): Float = try {
        ((recorder?.maxAmplitude ?: 0) / 22_000f).coerceIn(0f, 1f)
    } catch (e: Exception) {
        0f
    }
}
