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
import com.ragini.medreminder.ui.components.BigButton
import com.ragini.medreminder.ui.components.Eyebrow
import com.ragini.medreminder.ui.components.IconBadge
import com.ragini.medreminder.ui.components.PillIcon
import com.ragini.medreminder.ui.components.SoftCard
import com.ragini.medreminder.ui.components.StatusChip
import com.ragini.medreminder.ui.theme.MedColors
import com.ragini.medreminder.ui.theme.Palette
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

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Header(model, onSettings) }
            item { DialCard(model, items, now) }
            item { Tally(items, now) }
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
                Fmt.Part.entries.forEach { part ->
                    val list = items.filter { Fmt.part(it.dose.minuteOfDay) == part }
                    if (list.isNotEmpty()) {
                        item(key = "h-${part.name}") { PartHeader(part, list) }
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
        ExtendedFloatingActionButton(
            onClick = onAdd,
            icon = { Icon(Icons.Rounded.Add, null, Modifier.size(28.dp)) },
            text = { Text("Add medicine", style = MaterialTheme.typography.labelLarge) },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp).height(64.dp)
        )
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
    val date = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.ENGLISH))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Eyebrow(date)
            Spacer(Modifier.height(4.dp))
            Text(
                Fmt.greeting() + if (model.userName.isNotBlank()) ",\n${model.userName}" else "",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        FilledTonalIconButton(onClick = onSettings, modifier = Modifier.size(56.dp)) {
            Icon(Icons.Rounded.Settings, "Settings", Modifier.size(28.dp))
        }
    }
}

// ---------------------------------------------------------------- the day dial

@Composable
private fun DialCard(model: AppModel, items: List<TodayDose>, now: Long) {
    val upcoming = items.filter { it.response == null }.minByOrNull { it.dose.at }
    val next = model.next
    SoftCard(padding = 12.dp) {
        DayDial(items, now, Modifier.fillMaxWidth()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 56.dp)) {
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
                        Text(minuteLabel(upcoming.dose.minuteOfDay), style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary)
                        Text(Fmt.until(upcoming.dose.at, now), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        Text("Medicines appear on this clock", style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}

/**
 * A 24-hour clock: noon at the top (sun), midnight at the bottom (moon).
 * Each dose sits on the ring at its time in its own colour; the marigold dot is "now".
 */
@Composable
fun DayDial(items: List<TodayDose>, now: Long, modifier: Modifier = Modifier, center: @Composable () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val measurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = cs.onSurfaceVariant)
    val notC = StatusColors.of(DoseStatus.NOT_TAKEN)
    val missC = StatusColors.of(DoseStatus.MISSED)
    val pulse = rememberInfiniteTransition(label = "dial")
    val glow by pulse.animateFloat(0.25f, 0.9f, infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "g")
    val t = LocalTime.now()
    val nowMin = t.hour * 60 + t.minute

    Box(modifier.aspectRatio(1f), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val c = Offset(size.width / 2, size.height / 2)
            val track = 18.dp.toPx()
            val r = size.minDimension / 2 - 48.dp.toPx()
            fun ang(min: Int) = 90.0 + min / 1440.0 * 360.0
            fun pt(radius: Float, deg: Double): Offset {
                val rad = Math.toRadians(deg)
                return Offset(c.x + radius * cos(rad).toFloat(), c.y + radius * sin(rad).toFloat())
            }

            // Day above, night below.
            drawCircle(
                Brush.verticalGradient(
                    listOf(Palette.Marigold.copy(alpha = 0.22f), Palette.Marigold.copy(alpha = 0.06f), Palette.Indigo.copy(alpha = 0.10f), Palette.Indigo.copy(alpha = 0.24f)),
                    startY = c.y - r - track, endY = c.y + r + track
                ),
                radius = r + track * 1.6f, center = c
            )
            drawCircle(cs.surface, radius = r - track * 0.5f - 2.dp.toPx(), center = c)
            drawCircle(cs.outlineVariant, radius = r, center = c, style = Stroke(track))
            drawArc(
                color = cs.primary.copy(alpha = 0.30f),
                startAngle = 90f, sweepAngle = nowMin / 1440f * 360f, useCenter = false,
                topLeft = Offset(c.x - r, c.y - r), size = Size(r * 2, r * 2),
                style = Stroke(track, cap = StrokeCap.Round)
            )
            for (h in 0 until 24) {
                val major = h % 6 == 0
                val a = ang(h * 60)
                val inner = r - track / 2 - (if (major) 14.dp else 7.dp).toPx()
                drawLine(
                    cs.outline.copy(alpha = if (major) 0.9f else 0.4f),
                    pt(inner, a), pt(r - track / 2 - 3.dp.toPx(), a),
                    strokeWidth = (if (major) 2.5.dp else 1.5.dp).toPx(), cap = StrokeCap.Round
                )
            }
            listOf(0 to "12 AM", 360 to "6 AM", 720 to "12 PM", 1080 to "6 PM").forEach { (m, s) ->
                val res = measurer.measure(s, labelStyle)
                val p = pt(r + track / 2 + 22.dp.toPx(), ang(m))
                drawText(res, topLeft = Offset(p.x - res.size.width / 2f, p.y - res.size.height / 2f))
            }

            // Doses – several at the same minute are fanned out a little.
            val mr = 13.dp.toPx()
            items.groupBy { it.dose.minuteOfDay }.forEach { (minute, group) ->
                group.forEachIndexed { i, td ->
                    val spread = (i - (group.size - 1) / 2f) * 7.5
                    val p = pt(r, ang(minute) + spread)
                    val mc = MedColors.of(td.dose.medicine.colorIndex)
                    if (td.isOverdue(now)) drawCircle(missC.copy(alpha = glow * 0.5f), radius = mr * 1.75f, center = p)
                    when (td.status) {
                        DoseStatus.TAKEN -> {
                            drawCircle(cs.surface, radius = mr + 3.dp.toPx(), center = p)
                            drawCircle(mc, radius = mr, center = p)
                            val check = Path().apply {
                                moveTo(p.x - mr * 0.45f, p.y + mr * 0.02f)
                                lineTo(p.x - mr * 0.1f, p.y + mr * 0.38f)
                                lineTo(p.x + mr * 0.5f, p.y - mr * 0.34f)
                            }
                            drawPath(check, Color.White, style = Stroke(2.6.dp.toPx(), cap = StrokeCap.Round))
                        }
                        DoseStatus.NOT_TAKEN -> {
                            drawCircle(cs.surface, radius = mr + 3.dp.toPx(), center = p)
                            drawCircle(mc, radius = mr, center = p, style = Stroke(3.dp.toPx()))
                            val d = mr * 0.38f
                            drawLine(notC, Offset(p.x - d, p.y - d), Offset(p.x + d, p.y + d), 2.6.dp.toPx(), StrokeCap.Round)
                            drawLine(notC, Offset(p.x - d, p.y + d), Offset(p.x + d, p.y - d), 2.6.dp.toPx(), StrokeCap.Round)
                        }
                        else -> {
                            drawCircle(cs.surface, radius = mr + 3.dp.toPx(), center = p)
                            drawCircle(cs.surface, radius = mr, center = p)
                            drawCircle(mc, radius = mr, center = p, style = Stroke(3.5.dp.toPx()))
                            drawCircle(mc, radius = mr * 0.32f, center = p)
                        }
                    }
                }
            }
            // Now
            val np = pt(r, ang(nowMin))
            drawCircle(Palette.Marigold.copy(alpha = glow * 0.45f), radius = 15.dp.toPx(), center = np)
            drawCircle(Color.White, radius = 8.dp.toPx(), center = np)
            drawCircle(Palette.Marigold, radius = 6.dp.toPx(), center = np)
        }
        center()
    }
}

@Composable
private fun Tally(items: List<TodayDose>, now: Long) {
    val taken = items.count { it.status == DoseStatus.TAKEN }
    val not = items.count { it.status == DoseStatus.NOT_TAKEN }
    val left = items.count { it.response == null }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        TallyTile("Taken", taken, StatusColors.of(DoseStatus.TAKEN), Icons.Rounded.Check, Modifier.weight(1f))
        TallyTile("To answer", left, MaterialTheme.colorScheme.primary, Icons.Rounded.WbTwilight, Modifier.weight(1f))
        TallyTile("Not taken", not, StatusColors.of(DoseStatus.NOT_TAKEN), Icons.Rounded.Close, Modifier.weight(1f))
    }
}

@Composable
private fun TallyTile(label: String, n: Int, tint: Color, icon: ImageVector, modifier: Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(24.dp))
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp))
        Text("$n", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
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
private fun PartHeader(part: Fmt.Part, list: List<TodayDose>) {
    val icon = when (part) {
        Fmt.Part.MORNING -> Icons.Rounded.WbSunny
        Fmt.Part.AFTERNOON -> Icons.Rounded.WbSunny
        Fmt.Part.EVENING -> Icons.Rounded.WbTwilight
        Fmt.Part.NIGHT -> Icons.Rounded.NightsStay
    }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(10.dp))
        Text(part.title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
        Text("${list.count { it.response != null }} of ${list.size} answered", style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun DoseRow(td: TodayDose, now: Long, onClick: () -> Unit) {
    val m = td.dose.medicine
    SoftCard(onClick = onClick, padding = 16.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PillIcon(m.colorIndex, m.shape, size = 56.dp)
            Spacer(Modifier.width(14.dp))
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
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                val parts = minuteLabel(td.dose.minuteOfDay).split(" ")
                Text(parts[0], style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(parts.getOrElse(1) { "" }, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
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
