@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalTextApi::class)

package com.ragini.medreminder.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.WbTwilight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ragini.medreminder.data.DoseStatus
import com.ragini.medreminder.data.FoodRule
import com.ragini.medreminder.data.Reasons
import com.ragini.medreminder.data.minuteLabel
import com.ragini.medreminder.ui.AppModel
import com.ragini.medreminder.ui.Fmt
import com.ragini.medreminder.ui.Reliability
import com.ragini.medreminder.ui.TodayDose
import com.ragini.medreminder.ui.components.Bead
import com.ragini.medreminder.ui.components.BigButton
import com.ragini.medreminder.ui.components.ChakraDose
import com.ragini.medreminder.ui.components.FramedCard
import com.ragini.medreminder.ui.components.Mala
import com.ragini.medreminder.ui.components.PraharChakra
import com.ragini.medreminder.ui.components.PraharSection
import com.ragini.medreminder.ui.components.SkyHeader
import com.ragini.medreminder.ui.components.bleed
import com.ragini.medreminder.ui.components.pullUp
import com.ragini.medreminder.ui.components.Eyebrow
import com.ragini.medreminder.ui.components.IconBadge
import com.ragini.medreminder.ui.components.PillIcon
import com.ragini.medreminder.ui.components.SoftCard
import com.ragini.medreminder.ui.components.StatusChip
import com.ragini.medreminder.ui.theme.MedColors
import com.ragini.medreminder.ui.theme.Palette
import com.ragini.medreminder.ui.theme.Prahar
import com.ragini.medreminder.ui.theme.LocalSlots
import com.ragini.medreminder.ui.theme.Prahars
import com.ragini.medreminder.ui.theme.Slots
import com.ragini.medreminder.ui.theme.TimeSlot
import com.ragini.medreminder.ui.theme.StatusColors
import com.ragini.medreminder.voice.Phrases
import com.ragini.medreminder.voice.VoiceCatalog
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun TodayScreen(model: AppModel, onAdd: () -> Unit, onSettings: () -> Unit, onEdit: (Long) -> Unit) {
    val ctx = LocalContext.current
    val now = model.now
    val items = model.today
    var sheetFor by remember { mutableStateOf<TodayDose?>(null) }
    val healthy = remember(model.version) { Reliability.allOk(ctx) }
    val low = model.meds.filter { it.active && it.lowStock }
    val slots = LocalSlots.current

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 0.dp, bottom = 150.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Header(model, onSettings) }
            item { DialCard(model, items, now) }
            if (items.isNotEmpty()) item { Tally(items, now) }
            if (!healthy) item {
                Banner(
                    icon = Icons.Rounded.WarningAmber,
                    tint = StatusColors.of(DoseStatus.MISSED),
                    title = "Reminders may not ring on standby",
                    body = "One phone setting needs your OK. Tap to fix it in a few seconds.",
                    onClick = onSettings
                )
            }
            if (low.isNotEmpty()) item {
                Banner(
                    icon = Icons.Rounded.Inventory2,
                    tint = MaterialTheme.colorScheme.secondary,
                    title = "Time to refill",
                    body = low.joinToString(" · ") { "${it.name}: ${it.stock} left" },
                    onClick = { onEdit(low.first().id) }
                )
            }
            if (items.isEmpty()) {
                item { EmptyToday(onAdd) }
            } else {
                slots.forEach { part ->
                    val list = items.filter { part.contains(it.dose.minuteOfDay) }
                    if (list.isNotEmpty()) {
                        item(key = "h-${part.start}") { PartHeader(part, list) }
                        items(list, key = { it.dose.key }) { td -> DoseRow(td, now) { sheetFor = td } }
                    }
                }
                item {
                    Text(
                        "Attendance for today is written in the register at ${minuteLabel(model.prefs.dayCloseMinute)}.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                    )
                }
            }
        }
    }

    sheetFor?.let { td ->
        AnswerSheet(
            td = td,
            onDismiss = { sheetFor = null },
            onAnswer = { status, reason -> model.answer(td.dose.key, status, reason); sheetFor = null },
            onEdit = { sheetFor = null; onEdit(td.dose.medicine.id) }
        )
    }
}

@Composable
private fun Header(model: AppModel, onSettings: () -> Unit) {
    val date = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.ENGLISH))
    SkyHeader(
        eyebrow = date,
        title = Fmt.greeting() + if (model.userName.isNotBlank()) ",\n${model.userName}" else "",
        onSettings = onSettings,
        modifier = Modifier.bleed(20.dp)
    )
}

// ---------------------------------------------------------------- the day dial

@Composable
private fun DialCard(model: AppModel, items: List<TodayDose>, now: Long) {
    val upcoming = items.filter { it.response == null }.minByOrNull { it.dose.at }
    val next = model.next
    val t = LocalTime.now()
    val doses = items.map { ChakraDose(it.dose.minuteOfDay, it.dose.medicine.colorIndex, it.status, it.isOverdue(now)) }
    FramedCard(Modifier.pullUp(34.dp)) {
        PraharChakra(doses, t.hour * 60 + t.minute, Modifier.fillMaxWidth()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                when {
                    upcoming != null -> {
                        val overdue = upcoming.isOverdue(now)
                        Eyebrow(if (overdue) "WAITING FOR YOUR ANSWER" else "NEXT MEDICINE",
                            color = if (overdue) StatusColors.of(DoseStatus.MISSED) else MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            upcoming.dose.medicine.name,
                            style = MaterialTheme.typography.titleLarge,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(minuteLabel(upcoming.dose.minuteOfDay), style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary, maxLines = 1)
                        Text(
                            Fmt.until(upcoming.dose.at, now) + " · " + Slots.at(upcoming.dose.minuteOfDay, LocalSlots.current).name,
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center
                        )
                    }
                    items.isNotEmpty() -> {
                        Icon(Icons.Rounded.Check, null, tint = StatusColors.of(DoseStatus.TAKEN), modifier = Modifier.size(44.dp))
                        Text("All answered", style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
                        if (next != null) {
                            Text(
                                "Next: ${next.dose.medicine.name}\n${next.dose.day.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }} ${minuteLabel(next.dose.minuteOfDay)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    else -> {
                        Text("Your day", style = MaterialTheme.typography.headlineMedium)
                        Text("Medicines appear as beads on this wheel of the day", style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}

/** The day's mala, then the three counts – balanced left, centre, right. */
@Composable
private fun Tally(items: List<TodayDose>, now: Long) {
    val taken = items.count { it.status == DoseStatus.TAKEN }
    val not = items.count { it.status == DoseStatus.NOT_TAKEN }
    val left = items.count { it.response == null }
    val beads = items.sortedBy { it.dose.at }.map {
        when {
            it.status == DoseStatus.TAKEN -> Bead.TAKEN
            it.status == DoseStatus.NOT_TAKEN -> Bead.NOT_TAKEN
            it.isOverdue(now) -> Bead.MISSED
            else -> Bead.OPEN
        }
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Mala(beads, Modifier.padding(horizontal = 12.dp))
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            TallyTile("Taken", taken, StatusColors.of(DoseStatus.TAKEN), Modifier.weight(1f))
            Box(Modifier.width(1.dp).fillMaxHeight().background(MaterialTheme.colorScheme.outlineVariant))
            TallyTile("To answer", left, MaterialTheme.colorScheme.primary, Modifier.weight(1f))
            Box(Modifier.width(1.dp).fillMaxHeight().background(MaterialTheme.colorScheme.outlineVariant))
            TallyTile("Not taken", not, StatusColors.of(DoseStatus.NOT_TAKEN), Modifier.weight(1f))
        }
    }
}

@Composable
private fun TallyTile(label: String, n: Int, tint: Color, modifier: Modifier) {
    Column(modifier.padding(vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("$n", style = MaterialTheme.typography.displaySmall, color = tint)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun Banner(icon: ImageVector, tint: Color, title: String, body: String, onClick: () -> Unit) {
    SoftCard(onClick = onClick, color = tint.copy(alpha = 0.10f), padding = 16.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(icon, tint)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun PartHeader(part: TimeSlot, list: List<TodayDose>) {
    val span = "${minuteLabel(part.start)} – ${minuteLabel(part.end % 1440)}"
    PraharSection(part.name, "$span · ${list.count { it.response != null }} of ${list.size} answered")
}

/** Time | medicine | pill – the time column on the left, the pill on the right, a colour rail between. */
@Composable
private fun DoseRow(td: TodayDose, now: Long, onClick: () -> Unit) {
    val m = td.dose.medicine
    SoftCard(onClick = onClick, padding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.height(IntrinsicSize.Min)) {
            Column(Modifier.width(70.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                val parts = minuteLabel(td.dose.minuteOfDay).split(" ")
                Text(parts[0], style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
                Text(parts.getOrElse(1) { "" }, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Box(
                Modifier.padding(horizontal = 12.dp).width(4.dp).fillMaxHeight()
                    .clip(RoundedCornerShape(2.dp)).background(MedColors.of(m.colorIndex))
            )
            Column(Modifier.weight(1f)) {
                Text(m.name, style = MaterialTheme.typography.titleLarge, maxLines = 3, overflow = TextOverflow.Ellipsis)
                val sub = listOf(m.dosage, m.food.takeIf { it != FoodRule.NONE }?.label ?: "").filter { it.isNotBlank() }.joinToString(" · ")
                if (sub.isNotBlank()) Text(sub, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                when {
                    td.response != null -> StatusChip(td.status)
                    td.isOverdue(now) -> StatusChip(DoseStatus.MISSED, "Tap to answer")
                    else -> StatusChip(DoseStatus.PENDING, Fmt.until(td.dose.at, now).replaceFirstChar { it.uppercase() })
                }
            }
            Spacer(Modifier.width(10.dp))
            PillIcon(m.colorIndex, m.shape, size = 52.dp)
        }
    }
}

@Composable
internal fun EmptyToday(onAdd: () -> Unit) {
    SoftCard(padding = 28.dp) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            PillIcon(1, com.ragini.medreminder.data.PillShape.CAPSULE, size = 88.dp)
            Spacer(Modifier.height(16.dp))
            Text("No medicines today", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
            Spacer(Modifier.height(6.dp))
            Text(
                "Add a medicine and its time. Ragini will start speaking 19 minutes before, every 2 minutes, until it is time.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(20.dp))
            BigButton("Add my first medicine", onClick = onAdd, icon = Icons.Rounded.Add, modifier = Modifier.fillMaxWidth())
        }
    }
}

// ---------------------------------------------------------------- answering from the Today screen

@Composable
fun AnswerSheet(
    td: TodayDose,
    onDismiss: () -> Unit,
    onAnswer: (DoseStatus, String) -> Unit,
    onEdit: () -> Unit
) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var askReason by remember { mutableStateOf(false) }
    var picked by remember { mutableStateOf("") }
    var other by remember { mutableStateOf("") }
    val m = td.dose.medicine
    val notC = StatusColors.of(DoseStatus.NOT_TAKEN)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = state, containerColor = MaterialTheme.colorScheme.surface) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .padding(bottom = 28.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PillIcon(m.colorIndex, m.shape, size = 64.dp)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(m.name, style = MaterialTheme.typography.headlineSmall)
                    Text(
                        listOf(minuteLabel(td.dose.minuteOfDay), m.dosage).filter { it.isNotBlank() }.joinToString(" · "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(onClick = onEdit) {
                    Icon(Icons.Rounded.Edit, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Edit")
                }
            }
            if (m.notes.isNotBlank()) {
                Text(m.notes, style = MaterialTheme.typography.bodyLarge)
            }
            td.response?.let {
                StatusChip(it.status, if (it.reason.isNotBlank() && it.status == DoseStatus.NOT_TAKEN) "Not taken – ${it.reason}" else it.status.label)
            }
            Text(if (td.response == null) "Did you take it?" else "Change the answer", style = MaterialTheme.typography.titleLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BigButton(
                    "Taken", onClick = { onAnswer(DoseStatus.TAKEN, "Taken") }, icon = Icons.Rounded.Check,
                    container = StatusColors.of(DoseStatus.TAKEN), content = Color.White, height = 72.dp, modifier = Modifier.weight(1f)
                )
                BigButton(
                    "Not taken", onClick = { askReason = !askReason }, icon = Icons.Rounded.Close,
                    container = if (askReason) notC else MaterialTheme.colorScheme.surfaceVariant,
                    content = if (askReason) Color.White else MaterialTheme.colorScheme.onSurface,
                    height = 72.dp, modifier = Modifier.weight(1f)
                )
            }
            AnimatedVisibility(askReason, enter = expandVertically() + fadeIn()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Why not?", style = MaterialTheme.typography.titleMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Reasons.notTaken.forEach { r ->
                            val on = picked == r
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(if (on) notC else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { picked = if (on) "" else r }
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                Text(r, style = MaterialTheme.typography.labelMedium, color = if (on) Color.White else MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                    OutlinedTextField(
                        value = other, onValueChange = { other = it },
                        label = { Text("Or write your own reason") },
                        minLines = 2, textStyle = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.fillMaxWidth()
                    )
                    val reason = listOf(picked, other.trim()).filter { it.isNotBlank() }.joinToString(" – ")
                    BigButton(
                        "Save", onClick = { onAnswer(DoseStatus.NOT_TAKEN, reason) }, enabled = reason.isNotBlank(),
                        container = notC, content = Color.White, modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
