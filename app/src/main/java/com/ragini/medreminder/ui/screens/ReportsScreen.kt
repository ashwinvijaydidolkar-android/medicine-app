package com.ragini.medreminder.ui.screens

import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ragini.medreminder.data.DoseStatus
import com.ragini.medreminder.data.minuteLabel
import com.ragini.medreminder.report.DayStat
import com.ragini.medreminder.report.Period
import com.ragini.medreminder.report.Report
import com.ragini.medreminder.report.Reports
import com.ragini.medreminder.report.Row as RRow
import com.ragini.medreminder.ui.AppModel
import com.ragini.medreminder.ui.Fmt
import com.ragini.medreminder.ui.components.BigButton
import com.ragini.medreminder.ui.components.Eyebrow
import com.ragini.medreminder.ui.components.SoftCard
import com.ragini.medreminder.ui.components.StatusChip
import com.ragini.medreminder.ui.theme.MedColors
import com.ragini.medreminder.ui.theme.StatusColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

@Composable
fun ReportsScreen(model: AppModel) {
    val ctx = LocalContext.current
    var period by rememberSaveable { mutableStateOf(Period.WEEKLY) }
    val report by produceState<Report?>(null, period, model.version) {
        value = withContext(Dispatchers.IO) { Reports.build(ctx, period) }
    }
    val share = {
        report?.let { r ->
            val send = Intent(Intent.ACTION_SEND).setType("text/plain")
                .putExtra(Intent.EXTRA_SUBJECT, "Medicine report – ${r.period.tab}")
                .putExtra(Intent.EXTRA_TEXT, Reports.shareText(r, model.userName))
            ctx.startActivity(Intent.createChooser(send, "Share report"))
        }
        Unit
    }

    LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Eyebrow(report?.rangeLabel ?: " ")
                    Spacer(Modifier.height(4.dp))
                    Text("Reports", style = MaterialTheme.typography.headlineLarge)
                }
                FilledTonalIconButton(onClick = share, modifier = Modifier.size(56.dp)) {
                    Icon(Icons.Rounded.Share, "Share report", Modifier.size(26.dp))
                }
            }
        }
        item { PeriodGrid(period) { period = it } }

        val r = report
        if (r == null || r.period != period) {
            item {
                Box(Modifier.fillMaxWidth().height(240.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            }
        } else {
            item { Hero(r, model.now) }
            item { Tiles(r, model.now) }
            when {
                r.period.multiDay && r.period == Period.WEEKLY -> item { WeekBars(r) }
                r.period.multiDay -> item { Heatmap(r) }
                else -> item { Register(r, model.now) }
            }
            if (r.meds.isNotEmpty()) item { ByMedicine(r) }
            if (r.total > 0) item { PartsAndInsights(r) }
            if (r.reasons.isNotEmpty()) item { ReasonsCard(r) }
            item {
                BigButton("Send this report to family or doctor", onClick = share, icon = Icons.Rounded.Share, modifier = Modifier.fillMaxWidth())
            }
            item {
                Text(
                    "Attendance is marked once a day, at ${r.closeTimeLabel}. A dose is present when it was answered “Taken” or “Not taken” with a reason, and absent when there was no answer. Taken % counts only doses actually taken.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PeriodGrid(selected: Period, onSelect: (Period) -> Unit) {
    val cs = MaterialTheme.colorScheme
    Column(
        Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(cs.surfaceVariant)
            .padding(5.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Period.entries.chunked(4).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                row.forEach { p ->
                    val on = p == selected
                    Box(
                        Modifier
                            .weight(1f)
                            .heightIn(min = 52.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (on) cs.primary else Color.Transparent)
                            .clickable { onSelect(p) }
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            p.tab,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (on) FontWeight.SemiBold else FontWeight.Medium,
                            color = if (on) cs.onPrimary else cs.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

/** What the gauges and tiles count: closed doses, or – for today – the live answers so far. */
private class Counts(val total: Int, val taken: Int, val notTaken: Int, val noAnswer: Int, val live: Boolean) {
    val attendancePct get() = if (total == 0) 0 else Math.round((taken + notTaken) * 100f / total)
    val takenPct get() = if (total == 0) 0 else Math.round(taken * 100f / total)
}

private fun counts(r: Report, now: Long): Counts {
    if (r.total > 0) return Counts(r.total, r.taken, r.notTaken, r.missed, false)
    val due = r.provisional.filter { it.scheduledAt <= now || it.status != DoseStatus.PENDING }
    return Counts(
        due.size,
        due.count { it.status == DoseStatus.TAKEN },
        due.count { it.status == DoseStatus.NOT_TAKEN },
        due.count { it.status == DoseStatus.PENDING },
        live = true
    )
}

@Composable
private fun Hero(r: Report, now: Long) {
    val c = counts(r, now)
    val cs = MaterialTheme.colorScheme
    val takenC = StatusColors.of(DoseStatus.TAKEN)
    SoftCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(156.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(156.dp)) {
                    val stroke = 16.dp.toPx()
                    val gap = 6.dp.toPx()
                    fun ring(inset: Float, frac: Float, color: Color) {
                        val tl = Offset(inset + stroke / 2, inset + stroke / 2)
                        val sz = Size(size.width - 2 * inset - stroke, size.height - 2 * inset - stroke)
                        drawArc(cs.outlineVariant, -90f, 360f, false, tl, sz, style = Stroke(stroke))
                        if (frac > 0f) drawArc(color, -90f, 360f * frac, false, tl, sz, style = Stroke(stroke, cap = StrokeCap.Round))
                    }
                    ring(0f, c.attendancePct / 100f, cs.primary)
                    ring(stroke + gap, c.takenPct / 100f, takenC)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (c.total == 0) "–" else "${c.attendancePct}%", style = MaterialTheme.typography.displaySmall)
                    Text("attendance", style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
                }
            }
            Spacer(Modifier.width(18.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (c.live) {
                    Text("Live – not closed yet", style = MaterialTheme.typography.titleMedium, color = cs.secondary)
                }
                Legend(cs.primary, "Attendance", "${c.taken + c.notTaken} of ${c.total} answered")
                Legend(takenC, "Taken", if (c.total == 0) "–" else "${c.takenPct}% · ${c.taken} doses")
                if (c.total == 0) {
                    Text(
                        if (r.provisional.isNotEmpty()) "Doses still to come today." else "No medicines in this period yet.",
                        style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun Legend(color: Color, title: String, value: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(Modifier.padding(top = 6.dp).size(12.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(10.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun Tiles(r: Report, now: Long) {
    val c = counts(r, now)
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Tile(Icons.Rounded.Check, "Taken", c.taken, StatusColors.of(DoseStatus.TAKEN), Modifier.weight(1f))
        Tile(Icons.Rounded.Close, "Not taken", c.notTaken, StatusColors.of(DoseStatus.NOT_TAKEN), Modifier.weight(1f))
        Tile(Icons.Rounded.HelpOutline, if (c.live) "Awaiting" else "No answer", c.noAnswer, StatusColors.of(DoseStatus.MISSED), Modifier.weight(1f))
    }
}

@Composable
private fun Tile(icon: ImageVector, label: String, n: Int, tint: Color, modifier: Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(24.dp))
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.size(34.dp).clip(CircleShape).background(tint.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.height(4.dp))
        Text("$n", style = MaterialTheme.typography.headlineMedium)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ---------------------------------------------------------------- the attendance register

@Composable
private fun Register(r: Report, now: Long) {
    SoftCard {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Attendance register", style = MaterialTheme.typography.titleLarge)
            Text(r.rangeLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            if (r.rows.isEmpty()) {
                Text("No doses in this period.", style = MaterialTheme.typography.bodyMedium)
            }
            var lastDay: java.time.LocalDate? = null
            r.rows.forEach { row ->
                if (r.period == Period.OVERNIGHT && row.day != lastDay) {
                    Eyebrow(row.day.format(DateTimeFormatter.ofPattern("EEEE d MMM", Locale.ENGLISH)), Modifier.padding(top = 8.dp))
                    lastDay = row.day
                }
                RegisterRow(row, now)
            }
        }
    }
}

@Composable
private fun RegisterRow(row: RRow, now: Long) {
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.Top) {
        Column(Modifier.width(76.dp)) {
            val t = minuteLabel(row.minute).split(" ")
            Text(t[0], style = MaterialTheme.typography.titleLarge)
            Text(t.getOrElse(1) { "" }, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Box(Modifier.padding(top = 8.dp).size(12.dp).clip(CircleShape).background(MedColors.of(row.colorIndex)))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(row.medName, style = MaterialTheme.typography.titleMedium)
            val label = when {
                row.final -> row.status.label + if (row.attended) " · present" else " · absent"
                row.status == DoseStatus.PENDING && row.scheduledAt <= now -> "Waiting for answer"
                row.status == DoseStatus.PENDING -> "Upcoming"
                else -> row.status.label + " · marked at day end"
            }
            StatusChip(if (row.status == DoseStatus.PENDING && row.scheduledAt <= now) DoseStatus.MISSED else row.status, label)
            if (row.status == DoseStatus.NOT_TAKEN && row.reason.isNotBlank()) {
                Text("Reason: ${row.reason}", style = MaterialTheme.typography.bodyMedium)
            }
            row.respondedAt?.let {
                val lt = java.time.Instant.ofEpochMilli(it).atZone(java.time.ZoneId.systemDefault()).toLocalTime()
                val late = ((it - row.scheduledAt) / 60_000L).toInt()
                Text(
                    "Answered ${minuteLabel(lt.hour * 60 + lt.minute)}" + when {
                        late > 30 -> " · ${Fmt.span(late)} after the time"
                        late < -5 -> " · early"
                        else -> " · on time"
                    },
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** Seven columns, one per day: taken at the bottom, then not taken, then no answer. */
@Composable
private fun WeekBars(r: Report) {
    val takenC = StatusColors.of(DoseStatus.TAKEN)
    val notC = StatusColors.of(DoseStatus.NOT_TAKEN)
    val missC = StatusColors.of(DoseStatus.MISSED)
    val cs = MaterialTheme.colorScheme
    val maxTotal = (r.days.maxOfOrNull { it.total } ?: 1).coerceAtLeast(1)
    var picked by remember(r) { mutableStateOf<DayStat?>(null) }
    SoftCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Attendance register", style = MaterialTheme.typography.titleLarge)
            StatusLegend()
            Row(Modifier.fillMaxWidth().height(170.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                r.days.forEach { d ->
                    DayColumn(d, maxTotal, takenC, notC, missC, cs.outlineVariant, d == picked) { picked = if (picked == d) null else d }
                }
            }
            DayDetail(picked)
        }
    }
}

@Composable
private fun RowScope.DayColumn(d: DayStat, maxTotal: Int, takenC: Color, notC: Color, missC: Color, track: Color, on: Boolean, onClick: () -> Unit) {
    Column(
        Modifier.weight(1f).clip(RoundedCornerShape(14.dp))
            .background(if (on) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent)
            .clickable(onClick = onClick).padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Canvas(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 6.dp)) {
            val w = size.width
            val gap = 2.dp.toPx()
            val r4 = CornerRadius(4.dp.toPx(), 4.dp.toPx())
            drawRoundRect(track.copy(alpha = 0.35f), Offset.Zero, size, r4)
            if (d.total == 0) return@Canvas
            val unit = size.height / maxTotal
            var y = size.height
            listOf(d.taken to takenC, d.notTaken to notC, (d.total - d.taken - d.notTaken) to (if (d.final) missC else track)).forEach { (n, c) ->
                if (n > 0) {
                    val h = n * unit
                    drawRoundRect(c, Offset(0f, y - h + gap / 2), Size(w, (h - gap).coerceAtLeast(1f)), r4)
                    y -= h
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(d.day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH).take(2), style = MaterialTheme.typography.labelMedium)
        Text("${d.day.dayOfMonth}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun StatusLegend() {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        listOf(DoseStatus.TAKEN to "Taken", DoseStatus.NOT_TAKEN to "Not taken", DoseStatus.MISSED to "No answer").forEach { (s, l) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(StatusColors.of(s)))
                Spacer(Modifier.width(6.dp))
                Text(l, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun DayDetail(d: DayStat?) {
    val text = when {
        d == null -> "Tap a day to see its numbers."
        d.total == 0 -> "${fullDay(d)}: no medicines."
        !d.final -> "${fullDay(d)}: ${d.taken} taken, ${d.notTaken} not taken so far – closes tonight."
        else -> "${fullDay(d)}: ${d.taken} taken, ${d.notTaken} not taken, ${d.missed} no answer · attendance ${Math.round(d.attendance * 100)}%"
    }
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

private fun fullDay(d: DayStat) = d.day.format(DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH))

/** A calendar of squares, one per day, darker = more doses taken. Columns are weeks, rows are Mon–Sun. */
@Composable
private fun Heatmap(r: Report) {
    val takenC = StatusColors.of(DoseStatus.TAKEN)
    val cs = MaterialTheme.colorScheme
    val first = r.from.minusDays((r.from.dayOfWeek.value - 1).toLong()) // back to Monday
    val weeks = (ChronoUnit.DAYS.between(first, r.to) / 7 + 1).toInt()
    val byDay = r.days.associateBy { it.day }
    var picked by remember(r) { mutableStateOf<DayStat?>(null) }
    fun fill(d: DayStat): Color = when {
        d.total == 0 -> cs.surfaceVariant
        else -> takenC.copy(alpha = 0.15f + 0.85f * (d.taken.toFloat() / d.total))
    }
    SoftCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Attendance calendar", style = MaterialTheme.typography.titleLarge)
            Row {
                Column(Modifier.padding(end = 6.dp), verticalArrangement = Arrangement.SpaceBetween) {
                    listOf(1, 3, 5, 7).forEach {
                        Text(DayOfWeek.of(it).getDisplayName(TextStyle.SHORT, Locale.ENGLISH).take(1),
                            style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
                    }
                }
                Canvas(
                    Modifier
                        .weight(1f)
                        .height(150.dp)
                        .pointerInput(r) {
                            detectTapGestures { pos ->
                                val cw = size.width.toFloat() / weeks
                                val ch = size.height.toFloat() / 7
                                val col = (pos.x / cw).toInt()
                                val row = (pos.y / ch).toInt()
                                val day = first.plusDays(col * 7L + row)
                                picked = byDay[day]
                            }
                        }
                ) {
                    val cw = size.width / weeks
                    val ch = size.height / 7
                    val cell = minOf(cw, ch)
                    val gap = if (cell > 8.dp.toPx()) 2.dp.toPx() else 1f
                    val rad = CornerRadius((cell / 5).coerceAtMost(4.dp.toPx()))
                    for (w in 0 until weeks) for (dIdx in 0 until 7) {
                        val day = first.plusDays(w * 7L + dIdx)
                        val st = byDay[day] ?: continue
                        val tl = Offset(w * cw + (cw - cell) / 2 + gap / 2, dIdx * ch + (ch - cell) / 2 + gap / 2)
                        val sz = Size(cell - gap, cell - gap)
                        if (!st.final && st.total > 0) {
                            drawRoundRect(cs.outline, tl, sz, rad, style = Stroke(1.5f))
                        } else {
                            drawRoundRect(fill(st), tl, sz, rad)
                        }
                        if (st == picked) drawRoundRect(cs.onSurface, tl, sz, rad, style = Stroke(2.dp.toPx()))
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Fewer taken", style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
                Spacer(Modifier.width(6.dp))
                listOf(0f, 0.33f, 0.66f, 1f).forEach { f ->
                    Box(Modifier.padding(horizontal = 1.dp).size(14.dp).clip(RoundedCornerShape(3.dp))
                        .background(takenC.copy(alpha = 0.15f + 0.85f * f)))
                }
                Spacer(Modifier.width(6.dp))
                Text("All taken", style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
                Spacer(Modifier.weight(1f))
                Box(Modifier.size(14.dp).clip(RoundedCornerShape(3.dp)).background(cs.surfaceVariant))
                Spacer(Modifier.width(4.dp))
                Text("None", style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
            }
            DayDetail(picked)
            Text(
                "Perfect days: ${r.perfectDays} of ${r.daysWithDoses}",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

// ---------------------------------------------------------------- breakdowns

@Composable
private fun ByMedicine(r: Report) {
    val takenC = StatusColors.of(DoseStatus.TAKEN)
    val notC = StatusColors.of(DoseStatus.NOT_TAKEN)
    val missC = StatusColors.of(DoseStatus.MISSED)
    SoftCard {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("By medicine", style = MaterialTheme.typography.titleLarge)
            StatusLegend()
            r.meds.forEach { m ->
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(12.dp).clip(CircleShape).background(MedColors.of(m.colorIndex)))
                        Spacer(Modifier.width(8.dp))
                        Text(m.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text("${m.taken}/${m.total} taken", style = MaterialTheme.typography.labelMedium)
                    }
                    Canvas(Modifier.fillMaxWidth().height(14.dp)) {
                        val gap = 2.dp.toPx()
                        val rad = CornerRadius(4.dp.toPx())
                        var x = 0f
                        listOf(m.taken to takenC, m.notTaken to notC, m.missed to missC).forEach { (n, c) ->
                            if (n > 0) {
                                val w = size.width * n / m.total
                                drawRoundRect(c, Offset(x, 0f), Size((w - gap).coerceAtLeast(2f), size.height), rad)
                                x += w
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PartsAndInsights(r: Report) {
    SoftCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Patterns", style = MaterialTheme.typography.titleLarge)
            r.parts.chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    pair.forEach { p ->
                        Insight(
                            p.title,
                            if (p.total == 0) "–" else "${Math.round(p.taken * 100f / p.total)}%",
                            if (p.total == 0) "no doses" else "taken · ${p.total} doses",
                            null, Modifier.weight(1f)
                        )
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Insight("On time", if (r.taken == 0) "–" else "${r.onTimePct}%", "within 30 min", Icons.Rounded.Bolt, Modifier.weight(1f))
                Insight("Usual delay", r.avgDelayMin?.let { if (it <= 0) "0 min" else Fmt.span(it) } ?: "–", "after the time", Icons.Rounded.Timer, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Insight("Streak now", "${r.currentStreak}", "perfect days", Icons.Rounded.LocalFireDepartment, Modifier.weight(1f))
                Insight("Best streak", "${r.bestStreak}", "perfect days", Icons.Rounded.EmojiEvents, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun Insight(title: String, value: String, sub: String, icon: ImageVector?, modifier: Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
            }
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(value, style = MaterialTheme.typography.headlineMedium)
        Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ReasonsCard(r: Report) {
    val max = r.reasons.maxOf { it.second }.coerceAtLeast(1)
    val notC = StatusColors.of(DoseStatus.NOT_TAKEN)
    SoftCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Why doses were not taken", style = MaterialTheme.typography.titleLarge)
            r.reasons.take(8).forEach { (reason, n) ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row {
                        Text(reason, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        Text("×$n", style = MaterialTheme.typography.labelLarge)
                    }
                    Box(Modifier.fillMaxWidth(n.toFloat() / max).height(8.dp).clip(RoundedCornerShape(4.dp)).background(notC.copy(alpha = 0.7f)))
                }
            }
        }
    }
}
