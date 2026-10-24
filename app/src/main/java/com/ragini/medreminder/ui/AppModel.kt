package com.ragini.medreminder.ui

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.ragini.medreminder.data.Db
import com.ragini.medreminder.data.Dose
import com.ragini.medreminder.data.DoseResponse
import com.ragini.medreminder.data.DoseStatus
import com.ragini.medreminder.data.Medicine
import com.ragini.medreminder.data.Prefs
import com.ragini.medreminder.data.Transfer
import com.ragini.medreminder.schedule.AlarmScheduler
import com.ragini.medreminder.schedule.Answers
import com.ragini.medreminder.schedule.Doses
import com.ragini.medreminder.schedule.RingEvent
import com.ragini.medreminder.voice.Chimes
import com.ragini.medreminder.voice.Speaker
import com.ragini.medreminder.voice.VoicePersona
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Today's dose plus its answer, if there is one yet. */
data class TodayDose(val dose: Dose, val response: DoseResponse?) {
    val status: DoseStatus get() = response?.status ?: DoseStatus.PENDING
    fun isOverdue(now: Long) = response == null && dose.at <= now
}

/** Holds what the screens show. Small data, so it reads SQLite directly. */
class AppModel(private val app: Context) {
    val prefs = Prefs(app)
    private val db = Db.get(app)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    var meds by mutableStateOf<List<Medicine>>(emptyList()); private set
    var today by mutableStateOf<List<TodayDose>>(emptyList()); private set
    var next by mutableStateOf<RingEvent?>(null); private set
    var now by mutableLongStateOf(System.currentTimeMillis())
    /** Bumped whenever something changes that screens may need to re-read (checks, settings). */
    var version by mutableIntStateOf(0); private set

    var onboarded by mutableStateOf(prefs.onboarded); private set
    var themeMode by mutableIntStateOf(prefs.themeMode); private set
    var textScale by mutableFloatStateOf(prefs.textScale); private set
    var sky by mutableStateOf(prefs.sky); private set
    var slots by mutableStateOf(com.ragini.medreminder.ui.theme.Slots.decode(prefs.slots)); private set
    var userName by mutableStateOf(prefs.userName); private set
    var defaultVoiceId by mutableStateOf(prefs.defaultVoiceId); private set
    var tuneId by mutableIntStateOf(prefs.tuneId); private set

    private var speaker: Speaker? = null
    private var speakJob: Job? = null
    var speakingId by mutableStateOf<String?>(null); private set

    fun refresh() {
        now = System.currentTimeMillis()
        meds = db.medicines()
        val d = LocalDate.now()
        val answers = db.responsesBetween(Doses.epochMs(d, 0), Doses.epochMs(d.plusDays(1), 0))
        today = Doses.on(meds, d).map { TodayDose(it, answers[it.key]) }
        next = try { AlarmScheduler.nextEvent(app) } catch (_: Exception) { null }
        version++
    }

    fun saveMedicine(m: Medicine) {
        db.saveMedicine(m)
        AlarmScheduler.scheduleNext(app)
        refresh()
    }

    fun deleteMedicine(id: Long) {
        db.deleteMedicine(id)
        AlarmScheduler.scheduleNext(app)
        refresh()
    }

    fun setActive(m: Medicine, active: Boolean) = saveMedicine(m.copy(active = active))

    // ---------------------------------------------------------- moving the list to another device

    /** A list opened from a file (or shared into the app), waiting for the user to choose Add or Replace. */
    var pendingImport by mutableStateOf<Transfer.Parsed?>(null)

    /** After updating over an older version that already has medicines: say they are kept, offer to send them. */
    var showWhatsNew by mutableStateOf(prefs.onboarded && prefs.whatsNewSeen < WHATS_NEW && db.medicines().isNotEmpty())
        private set
    fun whatsNewDone() { prefs.whatsNewSeen = WHATS_NEW; showWhatsNew = false }
    var importProblem by mutableStateOf<String?>(null)

    fun exportText(): String = Transfer.export(db.medicines(), slots)

    /** Writes the list to a file the user chose (Downloads, Google Drive, …). */
    fun exportTo(uri: android.net.Uri): Boolean = try {
        app.contentResolver.openOutputStream(uri, "wt")!!.use { it.write(exportText().toByteArray(Charsets.UTF_8)) }
        true
    } catch (e: Exception) { false }

    /** Writes the list into the app's share folder and returns a link other apps (WhatsApp, Bluetooth, e-mail) can read. */
    fun exportForSharing(): android.net.Uri {
        val dir = java.io.File(app.cacheDir, "share").apply { mkdirs() }
        val f = java.io.File(dir, Transfer.fileName())
        f.writeText(exportText(), Charsets.UTF_8)
        return androidx.core.content.FileProvider.getUriForFile(app, "${app.packageName}.files", f)
    }

    fun readImport(uri: android.net.Uri) {
        importProblem = null
        try {
            val text = app.contentResolver.openInputStream(uri)!!.use { it.readBytes() }
            if (text.size > 2_000_000) throw Transfer.BadFile("This file is too big to be a medicine list.")
            pendingImport = Transfer.parse(String(text, Charsets.UTF_8))
        } catch (e: Transfer.BadFile) {
            importProblem = e.message
        } catch (e: Exception) {
            importProblem = "This file could not be opened. Please choose the Ragini medicine list file again."
        }
    }

    fun applyImport(replace: Boolean, useTimesOfDay: Boolean): Transfer.Result {
        val p = pendingImport ?: return Transfer.Result(0, 0)
        val r = Transfer.apply(db, p, replace)
        if (useTimesOfDay && p.slots != null) updateSlots(p.slots)
        pendingImport = null
        AlarmScheduler.rescheduleAll(app)
        refresh()
        return r
    }

    fun answer(key: String, status: DoseStatus, reason: String) {
        Answers.record(app, key, status, reason)
        refresh()
    }

    fun finishOnboarding() {
        prefs.whatsNewSeen = WHATS_NEW
        prefs.onboarded = true
        onboarded = true
        AlarmScheduler.rescheduleAll(app)
        refresh()
    }

    fun updateName(v: String) { prefs.userName = v; userName = v.trim() }
    fun updateTheme(v: Int) { prefs.themeMode = v; themeMode = v }
    fun updateSky(v: String) { prefs.sky = v; sky = v }
    fun updateSlots(v: List<Pair<String, Int>>) {
        val raw = com.ragini.medreminder.ui.theme.Slots.encode(v)
        prefs.slots = raw; slots = com.ragini.medreminder.ui.theme.Slots.decode(raw)
    }
    fun updateTextScale(v: Float) { prefs.textScale = v; textScale = prefs.textScale }
    fun updateDefaultVoice(id: String) { prefs.defaultVoiceId = id; defaultVoiceId = id; version++ }
    fun updateTune(id: Int) { prefs.tuneId = id; tuneId = prefs.tuneId }

    /** Call after changing any reminder-rhythm setting. */
    fun rhythmChanged() {
        AlarmScheduler.rescheduleAll(app)
        refresh()
    }

    fun touch() { version++ }

    // ---------------------------------------------------------------- speech previews

    private fun speaker(): Speaker = speaker ?: Speaker(app).also { speaker = it }

    suspend fun hasLanguage(p: VoicePersona): Boolean = speaker().hasLanguage(p.lang)

    /** Plays a short sample. Tapping again stops it. */
    fun preview(id: String, persona: VoicePersona?, text: String, file: String? = null) {
        if (speakingId == id) { stopPreview(); return }
        speakJob?.cancel()
        speakingId = id
        speakJob = scope.launch {
            try {
                val s = speaker()
                if (file != null) s.playFile(file)
                if (persona != null && text.isNotBlank()) s.speak(text, persona, loud = prefs.loudBoost)
            } finally {
                if (speakingId == id) speakingId = null
            }
        }
    }

    /** Plays one of the 200 tunes once, on the alarm channel – the same way a reminder will sound. */
    fun previewTune(id: Int) {
        val key = "tune-$id"
        if (speakingId == key) { stopPreview(); return }
        speakJob?.cancel()
        speaker?.stop()
        speakingId = key
        speakJob = scope.launch {
            try {
                Chimes.play(id)
            } finally {
                if (speakingId == key) speakingId = null
            }
        }
    }

    fun stopPreview() {
        speakJob?.cancel()
        speaker?.stop()
        speakingId = null
    }

    fun release() {
        stopPreview()
        speaker?.shutdown()
        speaker = null
        scope.cancel()
    }
}

/** Raise this when an update brings something the user should be told about once. 2 = moving the list to another device. */
const val WHATS_NEW = 2
