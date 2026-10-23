@file:OptIn(ExperimentalMaterial3Api::class)

package com.ragini.medreminder.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.ragini.medreminder.data.RecordedVoice
import com.ragini.medreminder.ui.AppModel
import com.ragini.medreminder.ui.Reliability
import com.ragini.medreminder.ui.components.BigButton
import com.ragini.medreminder.ui.components.ChoicePill
import com.ragini.medreminder.ui.components.Eyebrow
import com.ragini.medreminder.ui.components.SkyHeader
import com.ragini.medreminder.ui.components.SoftCard
import com.ragini.medreminder.ui.components.bleed
import com.ragini.medreminder.ui.components.Stepper
import com.ragini.medreminder.ui.theme.Palette
import com.ragini.medreminder.voice.Lang
import com.ragini.medreminder.voice.Mood
import com.ragini.medreminder.voice.Phrases
import com.ragini.medreminder.voice.TuneSynth
import com.ragini.medreminder.voice.VoiceCatalog
import com.ragini.medreminder.voice.VoicePersona
import com.ragini.medreminder.voice.VoiceRecorder
import kotlinx.coroutines.delay
import java.io.File
import java.util.UUID

/** Each personality has its own colour so voices are easy to tell apart. */
fun moodColor(m: Mood): Color = when (m) {
    Mood.GENTLE -> Color(0xFFE07A9A)
    Mood.CHEERFUL -> Color(0xFFF2A93B)
    Mood.CALM -> Color(0xFF3A9BDC)
    Mood.FIRM -> Color(0xFF5B54D6)
    Mood.DEVOTIONAL -> Color(0xFFD9734A)
    Mood.MELODY -> Color(0xFF9B5DE5)
    Mood.GRANDCHILD -> Color(0xFF6BAF3C)
    Mood.ELDER -> Color(0xFF1F9E89)
    Mood.BRISK -> Color(0xFF8A8FA8)
}

@Composable
fun VoicesScreen(model: AppModel, onSettings: () -> Unit = {}) {
    var lang by remember { mutableStateOf<Lang?>(null) }
    var mood by remember { mutableStateOf<Mood?>(null) }
    var recs by remember { mutableStateOf(model.prefs.recordedVoices) }
    var repeats by remember { mutableIntStateOf(model.prefs.repeats) }
    val installed by produceState<Map<Lang, Boolean>>(emptyMap()) {
        value = Lang.entries.associateWith { l -> model.hasLanguage(VoiceCatalog.all.first { it.lang == l }) }
    }
    val list = VoiceCatalog.all.filter { (lang == null || it.lang == lang) && (mood == null || it.mood == mood) }
    DisposableEffect(Unit) { onDispose { model.stopPreview() } }

    LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 0.dp, bottom = 150.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SkyHeader(
                eyebrow = "${VoiceCatalog.all.size} voices · ${TuneSynth.all.size} tunes",
                title = "Voices & tunes",
                subtitle = "Tap a voice to choose it – you hear it straight away. Each medicine can also have its own voice.",
                onSettings = onSettings,
                modifier = Modifier.bleed(20.dp)
            )
        }
        item { CurrentVoice(model, recs) }
        item { TuneCard(model) }
        item {
            FamilyVoices(model, recs, onChange = { recs = it; model.prefs.recordedVoices = it })
        }
        item {
            SoftCard(padding = 16.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Say each reminder", style = MaterialTheme.typography.titleMedium)
                        Text("How many times the message repeats", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Stepper(repeats, { repeats = it; model.prefs.repeats = it }, 1..5, format = { "$it×" })
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Eyebrow("Language")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { ChoicePill("All", lang == null, { lang = null }) }
                    items(Lang.entries.toList()) { l ->
                        ChoicePill(l.native + if (l.isEnglish) " · ${l.title.substringAfter("· ")}" else "", lang == l, { lang = if (lang == l) null else l })
                    }
                }
                Eyebrow("Personality")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { ChoicePill("All", mood == null, { mood = null }) }
                    items(Mood.entries.toList()) { m ->
                        ChoicePill(m.title, mood == m, { mood = if (mood == m) null else m }, leading = {
                            Box(Modifier.size(12.dp).clip(CircleShape).background(moodColor(m)))
                        })
                    }
                }
            }
        }
        val rows = list.chunked(2)
        items(rows, key = { r -> r.first().id }) { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                pair.forEach { p ->
                    VoiceTile(
                        model, p,
                        selected = model.defaultVoiceId == p.id,
                        missing = installed[p.lang] == false,
                        onSelect = { model.updateDefaultVoice(p.id); model.preview(p.id, p, Phrases.sample(p, model.userName)) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun CurrentVoice(model: AppModel, recs: List<RecordedVoice>) {
    val id = model.defaultVoiceId
    val p = VoiceCatalog.byId(id)
    val rec = recs.firstOrNull { it.id == id }
    val accent = p?.let { moodColor(it.mood) } ?: Palette.Marigold
    SoftCard(color = accent.copy(alpha = 0.12f), padding = 18.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(p?.name ?: rec?.label ?: "?", accent, 68.dp)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Eyebrow("Your reminder voice")
                Text(p?.name ?: rec?.label ?: VoiceCatalog.fallback.name, style = MaterialTheme.typography.headlineSmall)
                Text(
                    p?.let { "${it.mood.title} · ${it.lang.title}" } ?: "Family recording + spoken medicine name",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            SpeakingBadge(model.speakingId != null, accent)
        }
    }
}

@Composable
private fun VoiceTile(model: AppModel, p: VoicePersona, selected: Boolean, missing: Boolean, onSelect: () -> Unit, modifier: Modifier) {
    val ctx = LocalContext.current
    val c = moodColor(p.mood)
    val shape = RoundedCornerShape(26.dp)
    Column(
        modifier
            .clip(shape)
            .background(if (selected) c.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surface)
            .border(if (selected) 2.5.dp else 1.dp, if (selected) c else MaterialTheme.colorScheme.outlineVariant, shape)
            .clickable(onClick = onSelect)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(p.name, c, 46.dp)
            Spacer(Modifier.weight(1f))
            SpeakingBadge(model.speakingId == p.id, c)
            if (selected) Icon(Icons.Rounded.CheckCircle, "Selected", tint = c, modifier = Modifier.size(28.dp))
        }
        Text(p.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(p.mood.title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${p.lang.native} · ${if (p.female) "F" else "M"}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            if (missing) {
                Row(
                    Modifier.clip(RoundedCornerShape(50)).clickable { Reliability.openTtsSettings(ctx) }
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Download, "Download this language", Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    Text("Get", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
fun Avatar(name: String, color: Color, size: Dp) {
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(color, color.copy(alpha = 0.55f)))),
        contentAlignment = Alignment.Center
    ) {
        Text(
            name.take(1).uppercase(),
            style = if (size > 56.dp) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleLarge,
            color = Color.White
        )
    }
}

/** Shows that a sample is playing. There is no play button: choosing a voice or tune plays it automatically. */
@Composable
private fun SpeakingBadge(playing: Boolean, color: Color) {
    if (!playing) return
    Box(
        Modifier.padding(end = 6.dp).size(32.dp).clip(CircleShape).background(color.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center
    ) { Icon(Icons.AutoMirrored.Rounded.VolumeUp, "Playing", tint = color, modifier = Modifier.size(18.dp)) }
}

// ---------------------------------------------------------------- family recordings

@Composable
private fun FamilyVoices(model: AppModel, recs: List<RecordedVoice>, onChange: (List<RecordedVoice>) -> Unit) {
    val ctx = LocalContext.current
    val recorder = remember { VoiceRecorder(ctx) }
    var recording by remember { mutableStateOf(false) }
    var seconds by remember { mutableIntStateOf(0) }
    var level by remember { mutableFloatStateOf(0f) }
    var pending by remember { mutableStateOf<File?>(null) }
    var label by remember { mutableStateOf("") }
    var denied by remember { mutableStateOf(false) }

    val askMic = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        denied = !ok
        if (ok) recording = recorder.start()
    }
    DisposableEffect(Unit) { onDispose { recorder.stop() } }
    LaunchedEffect(recording) {
        if (!recording) return@LaunchedEffect
        val started = System.currentTimeMillis()
        seconds = 0
        while (recording) {
            delay(100)
            level = recorder.level()
            seconds = ((System.currentTimeMillis() - started) / 1000L).toInt()
            if (seconds >= 20) {
                pending = recorder.stop()
                recording = false
            }
        }
    }

    SoftCard(padding = 18.dp, color = Palette.Marigold.copy(alpha = 0.10f)) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Favorite, null, tint = Color(0xFFE07A9A), modifier = Modifier.size(26.dp))
                Spacer(Modifier.width(10.dp))
                Text("A voice from family", style = MaterialTheme.typography.titleLarge)
            }
            Text(
                "Record a loving message, like “Maa, it’s time for your medicine. Love, Ashwin.” It plays before the medicine name is spoken.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            recs.forEach { r ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .clickable { model.updateDefaultVoice(r.id); model.preview(r.id, null, "", r.path) }
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Avatar(r.label, Color(0xFFE07A9A), 44.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(r.label, style = MaterialTheme.typography.titleMedium)
                        Text(
                            if (model.defaultVoiceId == r.id) "Reminder voice ✓" else "Tap to use for all reminders",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    SpeakingBadge(model.speakingId == r.id, Color(0xFFE07A9A))
                    IconButton(onClick = {
                        File(r.path).delete()
                        if (model.defaultVoiceId == r.id) model.updateDefaultVoice(VoiceCatalog.fallback.id)
                        onChange(recs.filterNot { it.id == r.id })
                    }) { Icon(Icons.Rounded.Delete, "Delete recording") }
                }
            }

            when {
                recording -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LevelMeter(level, Modifier.weight(1f).height(40.dp))
                        Spacer(Modifier.width(12.dp))
                        Text("0:%02d / 0:20".format(seconds), style = MaterialTheme.typography.titleMedium)
                    }
                    BigButton(
                        "Stop recording", onClick = {
                            pending = recorder.stop(); recording = false
                            pending?.let { model.preview("pending", null, "", it.absolutePath) } // plays it back straight away
                        },
                        icon = Icons.Rounded.Stop, container = Color(0xFFCC3B3B), content = Color.White,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                pending != null -> {
                    OutlinedTextField(
                        value = label, onValueChange = { label = it },
                        label = { Text("Whose voice is this? (e.g. Ashwin)") },
                        singleLine = true, textStyle = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        BigButton(
                            "Save", onClick = {
                                val f = pending ?: return@BigButton
                                val v = RecordedVoice("rec:" + UUID.randomUUID().toString().take(8), label.trim().ifBlank { "Family" }, f.absolutePath)
                                onChange(recs + v)
                                model.updateDefaultVoice(v.id)
                                pending = null
                                label = ""
                            },
                            icon = Icons.Rounded.CheckCircle, modifier = Modifier.weight(1f)
                        )
                    }
                    TextButton(onClick = { pending?.delete(); pending = null }) { Text("Discard and record again") }
                }
                else -> {
                    BigButton(
                        "Record a message", onClick = {
                            if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                recording = recorder.start()
                            } else askMic.launch(Manifest.permission.RECORD_AUDIO)
                        },
                        icon = Icons.Rounded.Mic, container = Color(0xFFE07A9A), content = Color.White,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (denied) Text("Microphone permission is needed to record.", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun LevelMeter(level: Float, modifier: Modifier) {
    val history = remember { ArrayList<Float>() }
    history.add(level)
    if (history.size > 40) history.removeAt(0)
    val c = Color(0xFFE07A9A)
    Canvas(modifier) {
        val n = 40
        val w = size.width / n
        history.forEachIndexed { i, v ->
            val h = (0.08f + v * 0.92f) * size.height
            val x = (n - history.size + i) * w + w / 2
            drawLine(c, Offset(x, (size.height - h) / 2), Offset(x, (size.height + h) / 2), strokeWidth = w * 0.6f, cap = StrokeCap.Round)
        }
    }
}

// ---------------------------------------------------------------- picker used by the medicine editor

@Composable
fun VoicePickerSheet(model: AppModel, selected: String?, onPick: (String?) -> Unit, onDismiss: () -> Unit) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var lang by remember { mutableStateOf<Lang?>(null) }
    val recs = model.prefs.recordedVoices
    val list = VoiceCatalog.all.filter { lang == null || it.lang == lang }
    ModalBottomSheet(onDismissRequest = { model.stopPreview(); onDismiss() }, sheetState = state) {
        LazyColumn(
            Modifier.fillMaxHeight(0.92f).navigationBarsPadding(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { Text("Voice for this medicine", style = MaterialTheme.typography.headlineSmall) }
            item {
                PickRow(
                    title = "Same as my reminder voice",
                    sub = VoiceCatalog.byId(model.defaultVoiceId)?.title ?: recs.firstOrNull { it.id == model.defaultVoiceId }?.label ?: "",
                    color = Palette.Indigo, on = selected == null, playing = false
                ) { onPick(null) }
            }
            items(recs, key = { it.id }) { r ->
                PickRow(r.label, "Family recording", Color(0xFFE07A9A), selected == r.id, model.speakingId == r.id) {
                    model.preview(r.id, null, "", r.path); onPick(r.id)
                }
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { ChoicePill("All", lang == null, { lang = null }) }
                    items(Lang.entries.toList()) { l -> ChoicePill(l.native + if (l.isEnglish) " " + l.tag.takeLast(2) else "", lang == l, { lang = l }) }
                }
            }
            items(list, key = { it.id }) { p ->
                PickRow(p.name, "${p.mood.title} · ${p.lang.title}", moodColor(p.mood), selected == p.id, model.speakingId == p.id) {
                    model.preview(p.id, p, Phrases.sample(p, model.userName)); onPick(p.id)
                }
            }
        }
    }
}

@Composable
private fun PickRow(title: String, sub: String, color: Color, on: Boolean, playing: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .clip(shape)
            .background(if (on) color.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surface)
            .border(if (on) 2.dp else 1.dp, if (on) color else MaterialTheme.colorScheme.outlineVariant, shape)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(title, color, 44.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (sub.isNotBlank()) Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        SpeakingBadge(playing, color)
        if (on) {
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Rounded.CheckCircle, "Selected", tint = color)
        }
    }
}

// ---------------------------------------------------------------- the 200 tunes

private fun instrumentColor(i: TuneSynth.Instrument): Color = when (i) {
    TuneSynth.Instrument.SANTOOR -> Color(0xFFD9734A)
    TuneSynth.Instrument.BANSURI -> Color(0xFF1F9E89)
    TuneSynth.Instrument.SITAR -> Color(0xFFB8406E)
    TuneSynth.Instrument.HARP -> Color(0xFF9B5DE5)
    TuneSynth.Instrument.MUSIC_BOX -> Color(0xFFE07A9A)
    TuneSynth.Instrument.CELESTA -> Color(0xFF3A9BDC)
    TuneSynth.Instrument.MARIMBA -> Color(0xFFF2A93B)
    TuneSynth.Instrument.KALIMBA -> Color(0xFF6BAF3C)
    TuneSynth.Instrument.VIBRAPHONE -> Color(0xFF5B54D6)
    TuneSynth.Instrument.PIANO -> Color(0xFF6E6A92)
}

@Composable
private fun TuneCard(model: AppModel) {
    var open by remember { mutableStateOf(false) }
    val t = TuneSynth.byId(model.tuneId)
    val c = instrumentColor(t.instrument)
    SoftCard(onClick = { open = true }, padding = 18.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(56.dp).clip(CircleShape).background(c.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.MusicNote, null, tint = c, modifier = Modifier.size(30.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Eyebrow("Reminder tune · ${TuneSynth.all.size} to choose from")
                Text(t.title, style = MaterialTheme.typography.titleLarge)
                Text(
                    "Raga ${t.raga.name} – ${t.raga.mood.lowercase()}", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            SpeakingBadge(model.speakingId?.startsWith("tune-") == true, c)
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null)
        }
    }
    if (open) TunePickerSheet(model) { open = false }
}

@Composable
fun TunePickerSheet(model: AppModel, onDismiss: () -> Unit) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var raga by remember { mutableIntStateOf(model.tuneId / 10) }
    val r = TuneSynth.ragas[raga]
    ModalBottomSheet(onDismissRequest = { model.stopPreview(); onDismiss() }, sheetState = state) {
        LazyColumn(
            Modifier.fillMaxHeight(0.92f).navigationBarsPadding(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Choose a reminder tune", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "${TuneSynth.ragas.size} ragas × 10 instruments. Tap a tune to choose it – it plays straight away. Ragas marked ✦ are new.",
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(TuneSynth.ragas.indices.toList()) { i ->
                        ChoicePill(TuneSynth.ragas[i].name + if (i >= 10) " ✦" else "", raga == i, { raga = i })
                    }
                }
            }
            item { Text("Raga ${r.name} – ${r.mood.lowercase()}", style = MaterialTheme.typography.titleMedium) }
            items(TuneSynth.all.filter { it.raga === r }, key = { it.id }) { t ->
                PickRow(t.instrument.title, t.title, instrumentColor(t.instrument), model.tuneId == t.id, model.speakingId == "tune-${t.id}") {
                    model.updateTune(t.id)
                    model.previewTune(t.id)
                }
            }
        }
    }
}
