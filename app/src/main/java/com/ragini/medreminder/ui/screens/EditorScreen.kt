@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.ragini.medreminder.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AlarmOn
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ragini.medreminder.data.FoodRule
import com.ragini.medreminder.data.MedNames
import com.ragini.medreminder.data.Medicine
import com.ragini.medreminder.data.PillShape
import com.ragini.medreminder.data.ScheduleType
import com.ragini.medreminder.data.minuteLabel
import com.ragini.medreminder.schedule.RingKind
import com.ragini.medreminder.ui.AppModel
import com.ragini.medreminder.ui.Fmt
import com.ragini.medreminder.ui.components.BigButton
import com.ragini.medreminder.ui.components.BigOutlinedButton
import com.ragini.medreminder.ui.components.ChoicePill
import com.ragini.medreminder.ui.components.Eyebrow
import com.ragini.medreminder.ui.components.PillIcon
import com.ragini.medreminder.ui.components.Segmented
import com.ragini.medreminder.ui.components.SoftCard
import com.ragini.medreminder.ui.components.Stepper
import com.ragini.medreminder.ui.theme.MedColors
import com.ragini.medreminder.voice.Phrases
import com.ragini.medreminder.voice.VoiceCatalog
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/** One medicine being typed in. Several can share the same time – there is no limit. */
private data class Entry(
    val name: String = "",
    val spoken: String = "",
    val dosage: String = "",
    val colorIndex: Int = 0,
    val stock: String = ""
)

@Composable
fun EditorScreen(model: AppModel, editId: Long, onClose: () -> Unit) {
    val prefs = model.prefs
    val existing = remember(editId) { model.meds.firstOrNull { it.id == editId } }
    val today = remember { LocalDate.now() }
    val knownNames = remember { model.meds.map { it.name } }

    val entries = remember {
        mutableStateListOf<Entry>().apply {
            add(
                if (existing != null) Entry(existing.name, existing.spokenName, existing.dosage, existing.colorIndex, existing.stock?.toString() ?: "")
                else Entry(colorIndex = model.meds.size % MedColors.all.size)
            )
        }
    }
    var notes by remember { mutableStateOf(existing?.notes ?: "") }
    val times = remember { mutableStateListOf<Int>().apply { addAll(existing?.times ?: emptyList()) } }
    var schedule by remember { mutableStateOf(existing?.scheduleType ?: ScheduleType.DAILY) }
    val weekdays = remember { mutableStateListOf<Int>().apply { addAll(existing?.weekdays ?: (1..7).toSet()) } }
    var everyN by remember { mutableIntStateOf(existing?.everyNDays?.coerceAtLeast(2) ?: 2) }
    var endDate by remember { mutableStateOf(existing?.endDate) }
    var food by remember { mutableStateOf(existing?.food ?: FoodRule.NONE) }
    var shape by remember { mutableStateOf(existing?.shape ?: PillShape.CAPSULE) }
    var voiceId by remember { mutableStateOf(existing?.voiceId) }
    var lead by remember { mutableIntStateOf(existing?.leadMinutes ?: prefs.leadMinutes) }
    var stockOn by remember { mutableStateOf(existing?.stock != null) }
    var perDose by remember { mutableIntStateOf(existing?.perDose ?: 1) }
    var refillAt by remember { mutableIntStateOf(existing?.refillAt ?: 5) }

    var timeDialog by remember { mutableStateOf<Int?>(null) } // index to edit, -1 = new
    var voiceSheet by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var tried by remember { mutableStateOf(false) }

    val named = entries.filter { it.name.isNotBlank() }
    val valid = named.isNotEmpty() && times.isNotEmpty() && (schedule != ScheduleType.WEEKDAYS || weekdays.isNotEmpty())

    fun build(e: Entry, id: Long, createdAt: Long): Medicine = Medicine(
        id = id,
        name = e.name.trim(),
        spokenName = e.spoken.trim(),
        dosage = e.dosage.trim(),
        notes = notes.trim(),
        times = times.distinct().sorted(),
        scheduleType = schedule,
        weekdays = if (schedule == ScheduleType.WEEKDAYS) weekdays.toSet() else (1..7).toSet(),
        everyNDays = if (schedule == ScheduleType.EVERY_N_DAYS) everyN else 1,
        startDate = existing?.startDate ?: today,
        endDate = endDate,
        food = food,
        colorIndex = e.colorIndex,
        shape = shape,
        voiceId = voiceId,
        leadMinutes = if (lead == prefs.leadMinutes) null else lead,
        stock = if (stockOn) e.stock.filter { it.isDigit() }.take(5).toIntOrNull() ?: 0 else null,
        perDose = perDose,
        refillAt = refillAt,
        active = existing?.active ?: true,
        createdAt = createdAt
    )

    fun save() {
        tried = true
        if (!valid) return
        if (existing != null) {
            model.saveMedicine(build(named.first(), existing.id, existing.createdAt))
        } else {
            val now = System.currentTimeMillis()
            named.forEach { model.saveMedicine(build(it, 0L, now)) }
        }
        onClose()
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .imePadding()
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose, modifier = Modifier.size(56.dp)) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", Modifier.size(28.dp))
            }
            Text(
                if (existing == null) "New medicines" else "Edit medicine",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = { save() }) { Text("Save", style = MaterialTheme.typography.labelLarge) }
        }

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Names – as many medicines as needed for the same time
            SoftCard {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        if (existing == null) "Which medicines?" else "Medicine",
                        style = MaterialTheme.typography.titleLarge
                    )
                    if (existing == null) {
                        Text(
                            "Add every medicine taken at the same time – there is no limit. Each one is read out separately, one by one.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (tried && named.isEmpty()) {
                        Text("Please write at least one medicine name", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                    }
                    entries.forEachIndexed { i, e ->
                        EntryEditor(
                            index = i,
                            showNumber = existing == null,
                            entry = e,
                            shape = shape,
                            known = knownNames,
                            canRemove = entries.size > 1,
                            onChange = { entries[i] = it },
                            onRemove = { entries.removeAt(i) }
                        )
                    }
                    if (existing == null) {
                        BigOutlinedButton(
                            "Add another medicine at this time",
                            onClick = {
                                val used = entries.map { it.colorIndex }.toSet()
                                val c = (0 until MedColors.all.size).firstOrNull { it !in used } ?: (entries.size % MedColors.all.size)
                                entries.add(Entry(colorIndex = c))
                            },
                            icon = Icons.Rounded.Add,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes (optional)") },
                        placeholder = { Text("e.g. Prescribed by Dr. Mehta. Do not take with milk.") },
                        textStyle = MaterialTheme.typography.bodyLarge,
                        minLines = 3,
                        maxLines = 8,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // 2. Times
            SoftCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("When to take", style = MaterialTheme.typography.titleLarge)
                    if (tried && times.isEmpty()) {
                        Text("Please add at least one time", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                    }
                    val accent = MedColors.of(entries.first().colorIndex)
                    times.sorted().forEach { t ->
                        val idx = times.indexOf(t)
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(22.dp))
                                .background(accent.copy(alpha = 0.12f))
                                .clickable { timeDialog = idx }
                                .padding(start = 18.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.AlarmOn, null, tint = accent)
                            Spacer(Modifier.width(12.dp))
                            Text(minuteLabel(t), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
                            IconButton(onClick = { times.removeAll { it == t } }, modifier = Modifier.size(52.dp)) {
                                Icon(Icons.Rounded.Close, "Remove this time")
                            }
                        }
                    }
                    BigOutlinedButton("Add a time", onClick = { timeDialog = -1 }, icon = Icons.Rounded.Add, modifier = Modifier.fillMaxWidth())
                    Eyebrow("Quick add")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(7 * 60, 8 * 60, 13 * 60, 14 * 60, 18 * 60, 20 * 60, 21 * 60, 22 * 60).forEach { t ->
                            ChoicePill(minuteLabel(t), t in times, { if (t in times) times.removeAll { it == t } else times.add(t) })
                        }
                    }
                }
            }

            // 3. Days
            SoftCard {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Which days", style = MaterialTheme.typography.titleLarge)
                    Segmented(
                        listOf(ScheduleType.DAILY, ScheduleType.WEEKDAYS, ScheduleType.EVERY_N_DAYS), schedule, { schedule = it },
                        label = { when (it) { ScheduleType.DAILY -> "Every day"; ScheduleType.WEEKDAYS -> "Some days"; ScheduleType.EVERY_N_DAYS -> "Every few days" } }
                    )
                    if (schedule == ScheduleType.WEEKDAYS) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            (1..7).forEach { d ->
                                val on = d in weekdays
                                Box(
                                    Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable { if (on) weekdays.removeAll { it == d } else weekdays.add(d) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        DayOfWeek.of(d).getDisplayName(TextStyle.SHORT, Locale.ENGLISH).take(2),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (on) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                    if (schedule == ScheduleType.EVERY_N_DAYS) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Every", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                            Stepper(everyN, { everyN = it }, 2..30, format = { "$it days" })
                        }
                    }
                    Eyebrow("For how long")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ChoicePill("Ongoing", endDate == null, { endDate = null })
                        listOf(3, 5, 7, 10, 14, 30, 90).forEach { n ->
                            val end = today.plusDays(n - 1L)
                            ChoicePill("$n days", endDate == end, { endDate = end })
                        }
                    }
                    endDate?.let {
                        Text(
                            "Last dose on ${it.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.ENGLISH))}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 4. Food
            SoftCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("With food?", style = MaterialTheme.typography.titleLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        FoodRule.entries.forEach { f -> ChoicePill(f.label, food == f, { food = f }) }
                    }
                }
            }

            // 5. Shape
            SoftCard {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("What does it look like?", style = MaterialTheme.typography.titleLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        PillShape.entries.forEach { s ->
                            ChoicePill(s.label, shape == s, { shape = s }, leading = { PillIcon(entries.first().colorIndex, s, size = 28.dp) })
                        }
                    }
                    Text("Tap the coloured pill next to a name to change its colour.", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // 6. Reminder: voice, start, and exactly what will be said
            SoftCard {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Reminder", style = MaterialTheme.typography.titleLarge)
                    val vName = voiceId?.let { id ->
                        VoiceCatalog.byId(id)?.title ?: prefs.recordedVoices.firstOrNull { it.id == id }?.let { "${it.label} (recording)" }
                    } ?: "My reminder voice (${VoiceCatalog.byId(model.defaultVoiceId)?.name ?: "family recording"})"
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 64.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { voiceSheet = true }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.VolumeUp, null)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Eyebrow("Voice")
                            Text(vName, style = MaterialTheme.typography.titleMedium)
                        }
                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Start reminding", style = MaterialTheme.typography.titleMedium)
                            Text("minutes before", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Stepper(lead, { lead = it }, 0..60, format = { "$it min" })
                    }
                    val first = times.minOrNull()
                    if (first != null) {
                        Text(Fmt.rhythm(first, lead, prefs), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (named.isNotEmpty()) {
                        val persona = VoiceCatalog.byId(voiceId ?: model.defaultVoiceId) ?: VoiceCatalog.fallback
                        val preview = Phrases.build(persona, RingKind.DUE, 0, model.userName, named.map { build(it, 0L, 0L) })
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.FormatQuote, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                Spacer(Modifier.width(6.dp))
                                Eyebrow(
                                    "At ${first?.let { minuteLabel(it) } ?: "medicine time"} you will hear",
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Text(preview, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text(
                                "If a name sounds wrong on the phone, write it the way it should be said in “Say it as”.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                            )
                        }
                    }
                }
            }

            // 7. Pill count
            SoftCard {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Count my pills", style = MaterialTheme.typography.titleLarge)
                            Text("Get a refill reminder before they run out", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = stockOn, onCheckedChange = { stockOn = it })
                    }
                    if (stockOn) {
                        entries.forEachIndexed { i, e ->
                            OutlinedTextField(
                                value = e.stock,
                                onValueChange = { v -> entries[i] = e.copy(stock = v.filter { it.isDigit() }.take(5)) },
                                label = { Text("How many ${e.name.ifBlank { "pills" }} do you have now?") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                textStyle = MaterialTheme.typography.headlineSmall,
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Each dose", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                            Stepper(perDose, { perDose = it }, 1..10)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Warn me at", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                            Stepper(refillAt, { refillAt = it }, 0..60)
                        }
                    }
                }
            }

            if (existing != null) {
                TextButton(onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.DeleteForever, null, tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(8.dp))
                    Text("Delete this medicine", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelLarge)
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        Surface(color = MaterialTheme.colorScheme.background) {
            BigButton(
                text = when {
                    existing != null -> "Save changes"
                    named.size > 1 -> "Save ${named.size} medicines"
                    else -> "Save medicine"
                },
                onClick = { save() },
                icon = Icons.Rounded.Check,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            )
        }
    }

    timeDialog?.let { idx ->
        val initial = if (idx >= 0 && idx < times.size) times[idx] else (times.maxOrNull()?.let { (it + 240) % 1440 } ?: 8 * 60)
        TimeDialog(initial, onDismiss = { timeDialog = null }) { picked ->
            if (idx >= 0 && idx < times.size) times[idx] = picked else if (picked !in times) times.add(picked)
            timeDialog = null
        }
    }
    if (voiceSheet) {
        VoicePickerSheet(model, voiceId, onPick = { voiceId = it; voiceSheet = false }, onDismiss = { voiceSheet = false })
    }
    if (confirmDelete && existing != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete ${existing.name}?") },
            text = { Text("Reminders for this medicine will stop. Past attendance stays in the reports.") },
            confirmButton = {
                TextButton(onClick = { model.deleteMedicine(existing.id); confirmDelete = false; onClose() }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Keep") } }
        )
    }
}

/** Name, dose and "say it as" for one medicine, with spelling suggestions while typing. */
@Composable
private fun EntryEditor(
    index: Int,
    showNumber: Boolean,
    entry: Entry,
    shape: PillShape,
    known: List<String>,
    canRemove: Boolean,
    onChange: (Entry) -> Unit,
    onRemove: () -> Unit
) {
    val suggestions = MedNames.suggest(entry.name, known)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .border(1.5.dp, MedColors.of(entry.colorIndex).copy(alpha = 0.5f), RoundedCornerShape(22.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.clip(CircleShape).clickable { onChange(entry.copy(colorIndex = (entry.colorIndex + 1) % MedColors.all.size)) }) {
                PillIcon(entry.colorIndex, shape, size = 48.dp)
            }
            Spacer(Modifier.width(12.dp))
            Text(
                if (showNumber) "Medicine ${index + 1}" else "Name",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            if (canRemove) {
                IconButton(onClick = onRemove) { Icon(Icons.Rounded.Close, "Remove medicine ${index + 1}") }
            }
        }
        OutlinedTextField(
            value = entry.name,
            onValueChange = { onChange(entry.copy(name = it)) },
            label = { Text("Medicine name") },
            placeholder = { Text("e.g. Metformin") },
            textStyle = MaterialTheme.typography.headlineSmall,
            minLines = 1,
            maxLines = 4,
            // Autocorrect off: the keyboard must never "fix" a medicine name into another word.
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, autoCorrect = false),
            modifier = Modifier.fillMaxWidth()
        )
        if (suggestions.isNotEmpty()) {
            Eyebrow("Did you mean")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                suggestions.forEach { s -> ChoicePill(s, false, { onChange(entry.copy(name = s)) }) }
            }
        }
        OutlinedTextField(
            value = entry.dosage,
            onValueChange = { onChange(entry.copy(dosage = it)) },
            label = { Text("How much?") },
            placeholder = { Text("e.g. 1 tablet, 500 mg") },
            textStyle = MaterialTheme.typography.bodyLarge,
            maxLines = 3,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = entry.spoken,
            onValueChange = { onChange(entry.copy(spoken = it)) },
            label = { Text("Say it as (optional)") },
            placeholder = { Text("e.g. Met for min") },
            textStyle = MaterialTheme.typography.bodyLarge,
            singleLine = true,
            keyboardOptions = KeyboardOptions(autoCorrect = false),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun TimeDialog(initial: Int, onDismiss: () -> Unit, onPick: (Int) -> Unit) {
    val state = rememberTimePickerState(initialHour = initial / 60, initialMinute = initial % 60, is24Hour = false)
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(shape = RoundedCornerShape(32.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.padding(16.dp)) {
            Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Medicine time", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
                Spacer(Modifier.height(16.dp))
                TimePicker(state = state)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    BigOutlinedButton("Cancel", onClick = onDismiss, modifier = Modifier.weight(1f))
                    BigButton("Set time", onClick = { onPick(state.hour * 60 + state.minute) }, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
