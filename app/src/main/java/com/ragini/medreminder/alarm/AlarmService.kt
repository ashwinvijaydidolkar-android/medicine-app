package com.ragini.medreminder.alarm

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.ragini.medreminder.data.Db
import com.ragini.medreminder.data.Prefs
import com.ragini.medreminder.schedule.Answers
import com.ragini.medreminder.schedule.RingEvent
import com.ragini.medreminder.schedule.RingKind
import com.ragini.medreminder.voice.Chimes
import com.ragini.medreminder.voice.Phrases
import com.ragini.medreminder.voice.Speaker
import com.ragini.medreminder.voice.VoiceCatalog
import com.ragini.medreminder.voice.VoicePersona
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate

/** A reminder that is ringing right now. */
typealias RingItem = RingEvent

/**
 * The part of the app that actually makes noise.
 *
 * At medicine time it plays the chosen tune and then reads out every medicine ONE BY ONE,
 * and keeps repeating – tune, then voice – until EVERY medicine of that time has been
 * answered on the reminder screen (Taken, or Not taken with a reason). Only then does it stop –
 * there is no time limit.
 *
 * It runs as a foreground service so Android cannot pause it, holds a wake lock, turns the
 * ALARM volume to maximum (and puts it back afterwards), vibrates and can blink the torch –
 * whether the phone is locked, on silent, on standby or low on battery.
 */
class AlarmService : Service() {

    companion object {
        const val ACTION_RING = "com.ragini.medreminder.action.SERVICE_RING"
        const val ACTION_TEST = "com.ragini.medreminder.action.SERVICE_TEST"
        const val ACTION_STOP = "com.ragini.medreminder.action.STOP"
        private const val EX_KEYS = "keys"
        private const val EX_KINDS = "kinds"
        private const val EX_MINS = "mins"

        /** Dose keys still waiting for an answer while ringing – the reminder screen watches this. */
        val ringingKeys = MutableStateFlow<List<String>>(emptyList())
        val ringingTest = MutableStateFlow(false)
        /** True when at least one medicine is due now (not just an early reminder). */
        val ringingUrgent = MutableStateFlow(false)

        @Volatile private var instance: AlarmService? = null

        fun start(ctx: Context, events: List<RingEvent>) {
            if (events.isEmpty()) return
            val i = Intent(ctx, AlarmService::class.java).setAction(ACTION_RING)
                .putExtra(EX_KEYS, events.map { it.dose.key }.toTypedArray())
                .putExtra(EX_KINDS, events.map { it.kind.name }.toTypedArray())
                .putExtra(EX_MINS, events.map { it.minutes }.toIntArray())
            launch(ctx, i, events, false)
        }

        /** Used by the reminder screen when the phone refused to start the voice from the background. */
        fun startFromScreen(ctx: Context, keys: Array<String>, isTest: Boolean) {
            val i = if (isTest) {
                Intent(ctx, AlarmService::class.java).setAction(ACTION_TEST)
            } else {
                Intent(ctx, AlarmService::class.java).setAction(ACTION_RING)
                    .putExtra(EX_KEYS, keys)
                    .putExtra(EX_KINDS, Array(keys.size) { RingKind.DUE.name })
                    .putExtra(EX_MINS, IntArray(keys.size))
            }
            try {
                ContextCompat.startForegroundService(ctx, i)
            } catch (_: Exception) {
            }
        }

        fun startTest(ctx: Context) =
            launch(ctx, Intent(ctx, AlarmService::class.java).setAction(ACTION_TEST), emptyList(), true)

        /** Silences an early reminder or a test. A due medicine can only be silenced by answering it. */
        fun stop(ctx: Context) {
            try {
                ctx.startService(Intent(ctx, AlarmService::class.java).setAction(ACTION_STOP))
            } catch (e: Exception) {
                // The service is not running – nothing to stop.
            }
        }

        /** Called after every answer: stops the tune once all medicines of this time are answered. */
        fun onAnswered() {
            Handler(Looper.getMainLooper()).post { instance?.recheck() }
        }

        private fun launch(ctx: Context, i: Intent, events: List<RingEvent>, isTest: Boolean) {
            try {
                ContextCompat.startForegroundService(ctx, i)
            } catch (e: Exception) {
                // Some phones refuse to start a service from the background. Show the
                // full-screen reminder instead; it restarts the voice as soon as it opens.
                try {
                    val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    nm.notify(Notifs.ID_RINGING, Notifs.ringing(ctx, events, isTest, kick = true))
                } catch (_: Exception) {
                }
            }
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var speaker: Speaker
    private var job: Job? = null
    private var flashJob: Job? = null
    private var current: List<RingEvent> = emptyList()
    private var currentIsTest = false
    private var savedVolume = -1
    private var wakeLock: PowerManager.WakeLock? = null
    private var focusRequest: AudioFocusRequest? = null
    private val focusListener = AudioManager.OnAudioFocusChangeListener { }

    private val alarmAttrs: AudioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ALARM)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        Notifs.createChannels(this)
        speaker = Speaker(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                // A due medicine keeps ringing until it is answered.
                if (currentIsTest || current.none { it.kind != RingKind.PRE && isOpen(it) }) stopRinging()
            }
            ACTION_TEST -> {
                goForeground(emptyList(), true)
                ring(emptyList(), true)
            }
            ACTION_RING -> {
                val merged = merge(current.filter { isOpen(it) }, parse(intent))
                goForeground(merged, false)
                if (merged.isEmpty()) finish(emptyList()) else ring(merged, false)
            }
            else -> {
                if (job == null) stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun isOpen(e: RingEvent) = Db.get(this).response(e.dose.key) == null

    private fun parse(intent: Intent): List<RingEvent> {
        val keys = intent.getStringArrayExtra(EX_KEYS) ?: return emptyList()
        val kinds = intent.getStringArrayExtra(EX_KINDS) ?: return emptyList()
        val mins = intent.getIntArrayExtra(EX_MINS) ?: return emptyList()
        val now = System.currentTimeMillis()
        return keys.indices.mapNotNull { i ->
            val dose = Answers.doseFor(this, keys[i]) ?: return@mapNotNull null
            val kind = runCatching { RingKind.valueOf(kinds[i]) }.getOrDefault(RingKind.DUE)
            RingEvent(now, dose, kind, mins.getOrElse(i) { 0 }).takeIf { isOpen(it) }
        }
    }

    /** Keeps whatever is already ringing and adds the new doses; a due dose never turns back into an early one. */
    private fun merge(old: List<RingEvent>, new: List<RingEvent>): List<RingEvent> {
        val byKey = LinkedHashMap<String, RingEvent>()
        (old + new).forEach { e ->
            val prev = byKey[e.dose.key]
            byKey[e.dose.key] = when {
                prev == null -> e
                prev.kind == RingKind.PRE -> e
                e.kind == RingKind.PRE -> prev
                else -> e
            }
        }
        return byKey.values.sortedWith(compareBy<RingEvent>({ it.dose.at }, { it.dose.medicine.name.lowercase() }))
    }

    private fun goForeground(items: List<RingEvent>, isTest: Boolean) {
        val n = Notifs.ringing(this, items, isTest)
        val type = if (Build.VERSION.SDK_INT >= 29) ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK else 0
        try {
            ServiceCompat.startForeground(this, Notifs.ID_RINGING, n, type)
        } catch (e: Exception) {
            try {
                (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(Notifs.ID_RINGING, n)
            } catch (_: Exception) {
            }
        }
    }

    // ------------------------------------------------------------------ ringing

    private fun publish(open: List<RingEvent>) {
        ringingKeys.value = open.map { it.dose.key }
        ringingUrgent.value = open.any { it.kind != RingKind.PRE }
    }

    private fun ring(items: List<RingEvent>, isTest: Boolean) {
        job?.cancel()
        current = items
        currentIsTest = isTest
        publish(items)
        ringingTest.value = isTest

        val prefs = Prefs(this)
        acquireWake()
        boostVolume(prefs)
        requestFocus()
        if (prefs.vibrate) startVibration()
        if (prefs.flashOn) startFlash()
        openScreen(items, isTest)

        job = scope.launch {
            var round = 0
            while (isActive) {
                if (isTest) {
                    if (round >= prefs.repeats) break
                    speakTest(prefs)
                } else {
                    val open = items.filter { isOpen(it) }
                    publish(open)
                    if (open.isEmpty()) break                        // everything answered → stop
                    val urgent = open.any { it.kind != RingKind.PRE }
                    if (!urgent && round >= prefs.repeats) break     // early reminders: just a few times
                    // A due medicine has NO time limit: this repeats until every one is answered.
                    acquireWake()                                    // keeps the phone awake for the next round
                    speakRound(open, prefs, round)
                }
                round++
                delay(if (current.any { it.kind != RingKind.PRE }) 2500L else 1500L)
            }
            finish(items)
        }
    }

    /** Called (on the main thread) after an answer is saved anywhere in the app. */
    private fun recheck() {
        if (currentIsTest || current.isEmpty()) return
        val open = current.filter { isOpen(it) }
        publish(open)
        if (open.isEmpty()) stopRinging()
    }

    private suspend fun resolvePersona(id: String): VoicePersona {
        val p = VoiceCatalog.byId(id) ?: VoiceCatalog.fallback
        if (speaker.hasLanguage(p.lang)) return p
        // The phone does not have this language installed – speak in Indian English with the same mood.
        return VoiceCatalog.byId("en-IN-${p.mood.name.lowercase()}") ?: VoiceCatalog.fallback
    }

    /**
     * One round: the tune on its own, a short silence, then the voice. Music and speech never
     * overlap, so every medicine name is heard clearly. Each medicine is spoken as its own
     * short message, so long lists are never cut off.
     */
    private suspend fun speakRound(open: List<RingEvent>, prefs: Prefs, round: Int) {
        if (prefs.chimeOn) {
            Chimes.play(prefs.tuneId)
            delay(700)
        }
        // Every open medicine is in exactly one group, and each group is spoken in one voice (see SpeechPlan).
        for (group in SpeechPlan.of(open, prefs.defaultVoiceId)) {
            val voiceId = group.voiceId
            val recording = if (voiceId.startsWith("rec:")) prefs.recordedVoices.firstOrNull { it.id == voiceId } else null
            val personaId = when {
                recording != null -> prefs.defaultVoiceId.takeUnless { it.startsWith("rec:") } ?: VoiceCatalog.fallback.id
                voiceId.startsWith("rec:") -> VoiceCatalog.fallback.id
                else -> voiceId
            }
            val persona = resolvePersona(personaId)
            if (recording != null && File(recording.path).exists()) {
                speaker.playFile(recording.path)
                delay(250)
            }
            val kind = group.kind
            val meds = group.meds
            // The intro, then one line per medicine ("Number 2 of 4: …"), then the closing – each spoken on its own.
            for (part in Phrases.parts(persona, kind, group.minutes, prefs.userName, meds)) {
                if (group.events.none { isOpen(it) }) break     // everything in this group was just answered
                speaker.speak(part, persona, loud = prefs.loudBoost)
                delay(350)
            }

            if (round == 0 && kind == RingKind.DUE) {
                val low = meds.filter { it.lowStock }
                if (low.isNotEmpty() && persona.lang.isEnglish) {
                    delay(300)
                    speaker.speak(Phrases.lowStock(low, prefs.userName), persona, loud = prefs.loudBoost)
                }
            }
            delay(400)
        }
        if (round == 0) batteryCheck(prefs)
    }

    /** If the battery is low, say so out loud – and tell the family once a day – so reminders never stop. */
    private suspend fun batteryCheck(prefs: Prefs) {
        val bm = getSystemService(Context.BATTERY_SERVICE) as? BatteryManager ?: return
        val pct = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        if (pct <= 0 || pct > 20 || bm.isCharging) return
        val persona = resolvePersona(prefs.defaultVoiceId.takeUnless { it.startsWith("rec:") } ?: VoiceCatalog.fallback.id)
        speaker.speak(Phrases.lowBattery(persona, pct), persona, loud = prefs.loudBoost)
        val today = LocalDate.now().toString()
        if (pct <= 15 && prefs.lowBatteryAlertDay != today) {
            prefs.lowBatteryAlertDay = today
            CaregiverAlert.lowBattery(this, pct)
        }
    }

    private suspend fun speakTest(prefs: Prefs) {
        val id = prefs.defaultVoiceId
        val rec = if (id.startsWith("rec:")) prefs.recordedVoices.firstOrNull { it.id == id } else null
        val persona = resolvePersona(if (rec != null) VoiceCatalog.fallback.id else id)
        if (prefs.chimeOn) Chimes.play(prefs.tuneId)
        if (rec != null && File(rec.path).exists()) {
            speaker.playFile(rec.path)
            delay(250)
        }
        speaker.speak(Phrases.test(persona, prefs.userName), persona, loud = prefs.loudBoost)
    }

    /** Opens the big reminder screen. Android also opens it through the full-screen notification. */
    private fun openScreen(items: List<RingEvent>, isTest: Boolean) {
        try {
            val keys = items.map { it.dose.key }.toTypedArray()
            startActivity(ReminderActivity.intent(this, keys, isTest).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: Exception) {
            // Newer Android versions only allow this through the notification – that is fine.
        }
    }

    // ------------------------------------------------------------------ stopping

    private fun stopRinging() {
        job?.cancel()
        job = null
        finish(current)
    }

    private fun finish(items: List<RingEvent>) {
        stopFlash()
        stopVibration()
        speaker.stop()
        restoreVolume()
        abandonFocus()
        // Anything at or after its time that is still unanswered stays in the notification shade.
        val needAnswer = items.filter { it.kind != RingKind.PRE && isOpen(it) }
        if (needAnswer.isNotEmpty()) Notifs.awaiting(this, needAnswer)
        current = emptyList()
        currentIsTest = false
        ringingKeys.value = emptyList()
        ringingUrgent.value = false
        ringingTest.value = false
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        releaseWake()
        stopSelf()
    }

    override fun onDestroy() {
        instance = null
        stopFlash()
        stopVibration()
        restoreVolume()
        abandonFocus()
        releaseWake()
        ringingKeys.value = emptyList()
        ringingUrgent.value = false
        ringingTest.value = false
        speaker.shutdown()
        scope.cancel()
        super.onDestroy()
    }

    // ------------------------------------------------------------------ hardware helpers

    /** Renewed every round, so the phone stays awake for as long as the reminder rings. */
    private fun acquireWake() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        val wl = wakeLock ?: pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ragini:ring").also {
            it.setReferenceCounted(false)
            wakeLock = it
        }
        wl.acquire(10 * 60_000L)
    }

    private fun releaseWake() {
        try {
            wakeLock?.let { if (it.isHeld) it.release() }
        } catch (_: Exception) {
        }
        wakeLock = null
    }

    private fun boostVolume(prefs: Prefs) {
        if (!prefs.forceMaxVolume) return
        val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        try {
            if (savedVolume < 0) savedVolume = am.getStreamVolume(AudioManager.STREAM_ALARM)
            am.setStreamVolume(AudioManager.STREAM_ALARM, am.getStreamMaxVolume(AudioManager.STREAM_ALARM), 0)
        } catch (_: Exception) {
            // Some phones block volume changes in Do Not Disturb – the alarm channel is still used.
        }
    }

    private fun restoreVolume() {
        if (savedVolume < 0) return
        val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        try {
            am.setStreamVolume(AudioManager.STREAM_ALARM, savedVolume, 0)
        } catch (_: Exception) {
        }
        savedVolume = -1
    }

    private fun requestFocus() {
        val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        try {
            if (Build.VERSION.SDK_INT >= 26) {
                val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                    .setAudioAttributes(alarmAttrs)
                    .setOnAudioFocusChangeListener(focusListener)
                    .build()
                focusRequest = req
                am.requestAudioFocus(req)
            } else {
                @Suppress("DEPRECATION")
                am.requestAudioFocus(focusListener, AudioManager.STREAM_ALARM, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            }
        } catch (_: Exception) {
        }
    }

    private fun abandonFocus() {
        val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        try {
            if (Build.VERSION.SDK_INT >= 26) {
                focusRequest?.let { am.abandonAudioFocusRequest(it) }
                focusRequest = null
            } else {
                @Suppress("DEPRECATION")
                am.abandonAudioFocus(focusListener)
            }
        } catch (_: Exception) {
        }
    }

    @Suppress("DEPRECATION")
    private fun vibrator(): Vibrator? = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator

    @Suppress("DEPRECATION")
    private fun startVibration() {
        val v = vibrator() ?: return
        if (!v.hasVibrator()) return
        val pattern = longArrayOf(0, 800, 500, 800, 500, 1400, 1200)
        try {
            if (Build.VERSION.SDK_INT >= 26) {
                v.vibrate(VibrationEffect.createWaveform(pattern, 0), alarmAttrs)
            } else {
                v.vibrate(pattern, 0, alarmAttrs)
            }
        } catch (_: Exception) {
        }
    }

    private fun stopVibration() {
        try {
            vibrator()?.cancel()
        } catch (_: Exception) {
        }
    }

    private fun startFlash() {
        if (flashJob?.isActive == true) return
        val cm = getSystemService(Context.CAMERA_SERVICE) as? CameraManager ?: return
        val id = try {
            cm.cameraIdList.firstOrNull {
                cm.getCameraCharacteristics(it).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        } catch (_: Exception) {
            null
        } ?: return
        flashJob = scope.launch {
            var on = false
            try {
                while (isActive) {
                    on = !on
                    try {
                        cm.setTorchMode(id, on)
                    } catch (_: Exception) {
                        break
                    }
                    delay(if (on) 350L else 650L)
                }
            } finally {
                try {
                    cm.setTorchMode(id, false)
                } catch (_: Exception) {
                }
            }
        }
    }

    private fun stopFlash() {
        flashJob?.cancel()
        flashJob = null
    }
}
