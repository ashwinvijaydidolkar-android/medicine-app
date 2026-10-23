@file:OptIn(ExperimentalTextApi::class)

package com.ragini.medreminder.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ragini.medreminder.data.DoseStatus
import com.ragini.medreminder.ui.theme.LocalPrahar
import com.ragini.medreminder.ui.theme.Lexend
import com.ragini.medreminder.ui.theme.MedColors
import com.ragini.medreminder.ui.theme.Prahar
import com.ragini.medreminder.ui.theme.LocalSlots
import com.ragini.medreminder.ui.theme.Prahars
import com.ragini.medreminder.ui.theme.Slots
import com.ragini.medreminder.ui.theme.TimeSlot
import com.ragini.medreminder.ui.theme.StatusColors
import com.ragini.medreminder.ui.theme.YoungSerif
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

// ================================================================ geometry

/** A point on a circle; [deg] is clockwise from the top, like a clock hand. */
internal fun polar(c: Offset, r: Float, deg: Float): Offset {
    val a = deg * PI.toFloat() / 180f
    return Offset(c.x + r * sin(a), c.y - r * cos(a))
}

/** Noon at the top, midnight at the bottom – the sun is overhead at noon. */
internal fun dayAngle(minute: Int): Float = 180f + minute / 1440f * 360f

/** Lets a child of a padded list reach both screen edges. */
fun Modifier.bleed(horizontal: Dp): Modifier = layout { m, c ->
    val extra = horizontal.roundToPx() * 2
    val w = if (c.hasBoundedWidth) c.maxWidth + extra else c.maxWidth
    val p = m.measure(c.copy(minWidth = w, maxWidth = w))
    layout(c.maxWidth, p.height) { p.place(-extra / 2, 0) }
}

/** Pulls a block up by [by] so it overlaps what sits above it (the sky), without leaving a gap below. */
fun Modifier.pullUp(by: Dp): Modifier = layout { m, c ->
    val p = m.measure(c)
    val d = by.roundToPx()
    layout(p.width, (p.height - d).coerceAtLeast(0)) { p.place(0, -d) }
}

/** A card with a second hairline frame inside it, like a mount around a picture. */
@Composable
fun FramedCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val outer = RoundedCornerShape(32.dp)
    val inner = RoundedCornerShape(25.dp)
    Box(
        modifier
            .fillMaxWidth()
            .shadow(12.dp, outer, ambientColor = Color.Black.copy(alpha = 0.18f), spotColor = Color.Black.copy(alpha = 0.18f))
            .clip(outer)
            .background(cs.surface)
            .border(1.dp, cs.outlineVariant, outer)
            .padding(7.dp)
    ) {
        Box(Modifier.fillMaxWidth().border(1.dp, cs.secondary.copy(alpha = 0.3f), inner).padding(10.dp)) { content() }
    }
}

// ================================================================ mandala

/**
 * An 8-fold mandala in fine line work: rings of petals, all drawn from one rule.
 * [diameter] is the full width of the figure.
 */
fun DrawScope.mandala(center: Offset, diameter: Float, color: Color) {
    val s = diameter
    val line = Stroke(width = max(1f, s / 420f))
    fun petalRing(n: Int, r1: Float, r2: Float, w: Float, rot: Float = 0f) {
        val path = Path()
        for (i in 0 until n) {
            val a = rot + i * 360f / n
            val p1 = polar(center, r1, a)
            val p2 = polar(center, r2, a)
            val l = polar(center, (r1 + r2) / 2, a - w)
            val r = polar(center, (r1 + r2) / 2, a + w)
            path.moveTo(p1.x, p1.y)
            path.quadraticBezierTo(l.x, l.y, p2.x, p2.y)
            path.quadraticBezierTo(r.x, r.y, p1.x, p1.y)
            path.close()
        }
        drawPath(path, color, style = line)
    }
    listOf(0.16f, 0.3f, 0.47f, 0.49f).forEach { drawCircle(color, radius = s * it, center = center, style = line) }
    petalRing(8, s * 0.04f, s * 0.16f, 16f)
    petalRing(16, s * 0.16f, s * 0.3f, 9f, 11.25f)
    petalRing(16, s * 0.18f, s * 0.27f, 6f)
    petalRing(32, s * 0.3f, s * 0.47f, 5f, 5.625f)
    for (i in 0 until 64) drawCircle(color, radius = max(1.2f, s * 0.006f), center = polar(center, s * 0.485f, i * 5.625f))
}

/** A slowly turning mandala. Rotation happens on the GPU layer, so it costs almost nothing. */
@Composable
fun BoxScope.TurningMandala(diameter: Dp, color: Color, periodMs: Int = 240_000, offsetY: Dp = 0.dp) {
    val turn = rememberInfiniteTransition(label = "mandala")
    val angle by turn.animateFloat(0f, 360f, infiniteRepeatable(tween(periodMs, easing = LinearEasing)), label = "angle")
    // matchParentSize: the mandala never changes the size of what it decorates.
    Box(Modifier.matchParentSize()) {
        Canvas(
            Modifier
                .align(Alignment.Center)
                .offset(y = offsetY)
                .requiredSize(diameter)
                .graphicsLayer { rotationZ = angle }
        ) { mandala(Offset(size.width / 2, size.height / 2), size.minDimension, color) }
    }
}

// ================================================================ the sky band

/**
 * The header of every page: the sky of the current prahar, a turning mandala, the app mark,
 * the prahar's name in the middle and the settings gear – a perfectly balanced bar.
 */
@Composable
fun SkyHeader(
    eyebrow: String,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onSettings: (() -> Unit)? = null,
    extra: @Composable ColumnScope.() -> Unit = {}
) {
    val p = LocalPrahar.current
    val bg = MaterialTheme.colorScheme.background
    Box(
        modifier
            .fillMaxWidth()
            .clipToBounds()
            .drawBehind { drawRect(Brush.verticalGradient(0f to p.skyTop, 0.72f to p.skyBottom, 1f to bg)) }
    ) {
        TurningMandala(560.dp, p.skyInk.copy(alpha = 0.15f), offsetY = 40.dp)
        Column(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                AppMark(p, Modifier.size(48.dp))
                val slots = LocalSlots.current
                val t = java.time.LocalTime.now()
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { PraharChip(p, Slots.at(t.hour * 60 + t.minute, slots).name) }
                if (onSettings != null) {
                    Box(
                        Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(p.skyInk.copy(alpha = 0.16f))
                            .clickable(role = Role.Button, onClick = onSettings)
                            .semantics { contentDescription = "Settings" },
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Rounded.Settings, null, tint = p.skyInk, modifier = Modifier.size(26.dp)) }
                } else Spacer(Modifier.size(48.dp))
            }
            Spacer(Modifier.height(22.dp))
            Text(
                eyebrow.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 2.sp),
                color = p.skyInk.copy(alpha = 0.82f),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Text(title, style = MaterialTheme.typography.displaySmall, color = p.skyInk, textAlign = TextAlign.Center)
            if (subtitle != null) {
                Spacer(Modifier.height(8.dp))
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = p.skyInk.copy(alpha = 0.86f), textAlign = TextAlign.Center)
            }
            extra()
        }
    }
}

/** Ragini's mark: a tiny mandala inside a rounded square of the prahar's accent. */
@Composable
fun AppMark(p: Prahar, modifier: Modifier = Modifier) {
    Canvas(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(listOf(p.accent, p.deepB)))
            .border(2.dp, p.skyInk.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
    ) {
        val c = Offset(size.width / 2, size.height / 2)
        mandala(c, size.minDimension * 1.6f, Color.White.copy(alpha = 0.55f))
        drawCircle(Color.White, radius = size.minDimension * 0.09f, center = c)
    }
}

@Composable
fun PraharChip(p: Prahar, text: String = p.label, onDark: Boolean = false) {
    val ink = if (onDark) Color.White else p.skyInk
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(ink.copy(alpha = 0.16f))
            .border(1.dp, ink.copy(alpha = 0.22f), RoundedCornerShape(50))
            .padding(horizontal = 14.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(p.accent).border(1.5.dp, ink.copy(alpha = 0.8f), CircleShape))
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.labelMedium, color = ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** A centred section title with hairlines fading out to both sides. */
@Composable
fun PraharSection(title: String, subtitle: String? = null, modifier: Modifier = Modifier) {
    val rule = MaterialTheme.colorScheme.outline
    Row(modifier.fillMaxWidth().padding(top = 10.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f).height(1.dp).background(Brush.horizontalGradient(listOf(Color.Transparent, rule))))
        Column(Modifier.padding(horizontal = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, style = TextStyle(fontFamily = YoungSerif, fontSize = 24.sp, lineHeight = 30.sp), color = MaterialTheme.colorScheme.onBackground, textAlign = TextAlign.Center)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
        Box(Modifier.weight(1f).height(1.dp).background(Brush.horizontalGradient(listOf(rule, Color.Transparent))))
    }
}

// ================================================================ the prahar chakra

data class ChakraDose(val minute: Int, val colorIndex: Int, val status: DoseStatus, val overdue: Boolean)

/**
 * The Prahar Chakra – the day as eight coloured petals of three hours, noon at the top.
 * Each dose is a bead on the inner ring in its own colour; the sun (day) or moon (night) is "now".
 * Drawn on a 380-unit grid so it matches the Windows app exactly.
 */
@Composable
fun PraharChakra(
    doses: List<ChakraDose>,
    nowMinute: Int,
    modifier: Modifier = Modifier,
    slots: List<TimeSlot> = LocalSlots.current,
    center: @Composable () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val measurer = rememberTextMeasurer(cacheSize = 128)
    val notC = StatusColors.of(DoseStatus.NOT_TAKEN)
    val missC = StatusColors.of(DoseStatus.MISSED)
    // Only pulse (and so only redraw every frame) while something is actually overdue.
    val glowState: State<Float> = if (doses.any { it.overdue }) {
        rememberInfiniteTransition(label = "chakra")
            .animateFloat(0.2f, 0.75f, infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "g")
    } else remember { mutableFloatStateOf(0.5f) }
    val current = Slots.at(nowMinute, slots)
    val beadAngles = remember(doses) { spreadBeads(doses.map { dayAngle(it.minute) }) }

    Box(modifier.aspectRatio(1f), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val u = size.minDimension / 380f
            val c = Offset(size.width / 2, size.height / 2)
            val rOut = 162 * u; val rIn = 136 * u; val rBead = 114 * u; val rTick = 124 * u; val rSun = 176 * u
            val ring = (rOut + rIn) / 2
            val box = Rect(c.x - ring, c.y - ring, c.x + ring, c.y + ring)

            // one petal for each of the user's times of day, its name written along the arc
            slots.forEach { sl ->
                val p = sl.palette
                val gap = min(1.4f, (sl.end - sl.start) / 1440f * 90f)
                val a0 = dayAngle(sl.start) + gap
                val a1 = dayAngle(sl.end) - gap
                val active = sl == current
                drawArc(
                    p.accent.copy(alpha = if (active) 1f else 0.32f),
                    startAngle = a0 - 90f, sweepAngle = a1 - a0, useCenter = false,
                    topLeft = box.topLeft, size = box.size, style = Stroke(rOut - rIn)
                )
                val style = TextStyle(
                    fontFamily = Lexend, fontWeight = FontWeight.SemiBold, fontSize = (10.5f * u).toSp(),
                    color = if (active) p.onAccent else cs.onSurface.copy(alpha = 0.72f)
                )
                // shorten the name if the petal is small
                val room = (a1 - a0) * PI.toFloat() / 180f * (ring - 4 * u) - 8 * u
                val perChar = 8.2f * u
                var label = sl.name.uppercase()
                if (label.length * perChar > room) {
                    val fit = (room / perChar).toInt()
                    label = if (fit >= 4) label.take(fit - 1) + "…" else ""
                }
                if (label.isNotEmpty()) arcText(label, c, ring - 4 * u, (a0 + a1) / 2, 1.2f * u, style, measurer)
            }

            // the quiet core, hour ticks and the four cardinal hours
            drawCircle(Brush.radialGradient(listOf(cs.surface, cs.primaryContainer), center = c, radius = rIn), radius = rIn - 2 * u, center = c)
            for (h in 0 until 24) {
                val a = dayAngle(h * 60); val major = h % 3 == 0
                drawLine(
                    cs.onSurfaceVariant.copy(alpha = if (major) 0.75f else 0.35f),
                    polar(c, rTick - (if (major) 6 else 2) * u, a), polar(c, rTick + 4 * u, a),
                    strokeWidth = (if (major) 2f else 1f) * u, cap = StrokeCap.Round
                )
            }
            val small = TextStyle(fontFamily = Lexend, fontSize = (10f * u).toSp(), color = cs.onSurfaceVariant)
            listOf(0 to "12 AM", 360 to "6 AM", 720 to "12 PM", 1080 to "6 PM").forEach { (m, t) ->
                val res = measurer.measure(t, small)
                val pt = polar(c, rTick - 30 * u, dayAngle(m))
                drawText(res, topLeft = Offset(pt.x - res.size.width / 2f, pt.y - res.size.height / 2f))
            }
            drawCircle(
                cs.outline, radius = rBead, center = c,
                style = Stroke(1f * u, cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(0.1f, 6f * u)))
            )

            // beads
            doses.forEachIndexed { i, d ->
                val pt = polar(c, rBead, beadAngles[i])
                val col = MedColors.of(d.colorIndex)
                if (d.overdue) drawCircle(missC.copy(alpha = glowState.value * 0.5f), radius = 17 * u, center = pt)
                drawCircle(cs.surface, radius = 13 * u, center = pt)
                when (d.status) {
                    DoseStatus.TAKEN -> {
                        drawCircle(col, radius = 10.5f * u, center = pt)
                        val tick = Path().apply {
                            moveTo(pt.x - 4.5f * u, pt.y); lineTo(pt.x - 1.3f * u, pt.y + 3.4f * u); lineTo(pt.x + 4.5f * u, pt.y - 3f * u)
                        }
                        drawPath(tick, Color.White, style = Stroke(2.2f * u, cap = StrokeCap.Round, join = StrokeJoin.Round))
                    }
                    DoseStatus.NOT_TAKEN -> {
                        drawCircle(col, radius = 9.5f * u, center = pt, style = Stroke(2.6f * u))
                        val k = 3.6f * u
                        drawLine(notC, Offset(pt.x - k, pt.y - k), Offset(pt.x + k, pt.y + k), 2.2f * u, StrokeCap.Round)
                        drawLine(notC, Offset(pt.x + k, pt.y - k), Offset(pt.x - k, pt.y + k), 2.2f * u, StrokeCap.Round)
                    }
                    else -> {
                        drawCircle(col, radius = 9.5f * u, center = pt, style = Stroke(3f * u))
                        drawCircle(col, radius = 3.4f * u, center = pt)
                    }
                }
            }

            // the sun or the moon, riding just outside the petals
            val day = nowMinute in 360 until 1080
            val na = dayAngle(nowMinute)
            val sp = polar(c, rSun - 6 * u, na)
            drawLine(cs.onSurface.copy(alpha = 0.5f), polar(c, rOut + 1 * u, na), sp, 1.5f * u)
            drawCircle((if (day) Color(0xFFFFC94D) else Color(0xFFC9D4FF)).copy(alpha = 0.45f), radius = 12 * u, center = sp)
            if (day) {
                drawCircle(Color(0xFFFFD25E), radius = 7.5f * u, center = sp)
                drawCircle(Color(0xFFD98E00), radius = 7.5f * u, center = sp, style = Stroke(1.6f * u))
            } else {
                drawCircle(Color(0xFFE4E8FF), radius = 7.5f * u, center = sp)
                drawCircle(Color(0xFF5A64A8), radius = 7.5f * u, center = sp, style = Stroke(1.6f * u))
                drawCircle(cs.surface, radius = 5.2f * u, center = Offset(sp.x + 3 * u, sp.y - 2 * u))
            }
        }
        Box(Modifier.fillMaxWidth(0.62f), contentAlignment = Alignment.Center) { center() }
    }
}

/** Keeps beads at least one bead-width apart, then re-centres each crowded run on its true times. */
internal fun spreadBeads(want: List<Float>): List<Float> {
    if (want.isEmpty()) return emptyList()
    val order = want.indices.sortedBy { want[it] }
    val gap = min(13f, 360f / want.size)
    val a = FloatArray(want.size) { want[order[it]] }
    for (k in 1 until a.size) if (a[k] - a[k - 1] < gap) a[k] = a[k - 1] + gap
    var k = 0
    while (k < a.size) {
        var e = k
        while (e + 1 < a.size && a[e + 1] - a[e] <= gap + 0.01f) e++
        var mean = 0f
        for (j in k..e) mean += want[order[j]]
        mean /= (e - k + 1)
        val shift = mean - (a[k] + a[e]) / 2
        for (j in k..e) a[j] += shift
        k = e + 1
    }
    val out = FloatArray(want.size)
    order.forEachIndexed { pos, idx -> out[idx] = a[pos] }
    return out.toList()
}

/** Writes [text] along a circle, centred at [midDeg]; text on the lower half is turned to stay upright. */
private fun DrawScope.arcText(
    text: String, c: Offset, r: Float, midDeg: Float, spacing: Float, style: TextStyle,
    measurer: androidx.compose.ui.text.TextMeasurer
) {
    val mid = ((midDeg % 360f) + 360f) % 360f
    val flip = mid > 90f && mid < 270f
    val glyphs = text.map { measurer.measure(it.toString(), style) }
    val widths = glyphs.map { it.size.width + spacing }
    val total = widths.sum() - spacing
    val degPerPx = 180f / (PI.toFloat() * r)
    var run = -total / 2
    glyphs.forEachIndexed { i, g ->
        val centre = run + g.size.width / 2f
        val a = if (flip) mid - centre * degPerPx else mid + centre * degPerPx
        val pt = polar(c, r, a)
        rotate(if (flip) a - 180f else a, pivot = pt) {
            drawText(g, topLeft = Offset(pt.x - g.size.width / 2f, pt.y - g.size.height / 2f))
        }
        run += widths[i]
    }
}

// ================================================================ the mala

enum class Bead { TAKEN, NOT_TAKEN, MISSED, OPEN }

/** One bead per dose on a gently curved string, with the guru bead hanging in the middle. */
@Composable
fun Mala(beads: List<Bead>, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    val taken = StatusColors.of(DoseStatus.TAKEN)
    val not = StatusColors.of(DoseStatus.NOT_TAKEN)
    val miss = StatusColors.of(DoseStatus.MISSED)
    val accent = cs.secondary
    Canvas(
        modifier
            .fillMaxWidth()
            .height(78.dp)
            .semantics {
                contentDescription = "${beads.count { it == Bead.TAKEN }} taken, ${beads.count { it == Bead.NOT_TAKEN }} not taken, " +
                    "${beads.count { it == Bead.OPEN || it == Bead.MISSED }} still to answer"
            }
    ) {
        val u = size.width / 320f
        val n = max(beads.size, 1)
        val pad = 18 * u; val sag = 14 * u; val top = 12 * u
        val step = if (n > 1) (size.width - pad * 2) / (n - 1) else 0f
        fun pt(i: Int): Offset {
            val t = if (n > 1) i / (n - 1f) else 0.5f
            return Offset(if (n > 1) pad + i * step else size.width / 2, top + sag * 4 * t * (1 - t))
        }
        val string = Path().apply {
            moveTo(pad - 10 * u, top - 2 * u)
            quadraticBezierTo(size.width / 2, top + sag * 2, size.width - pad + 10 * u, top - 2 * u)
        }
        drawPath(string, cs.outline, style = Stroke(1.5f * u))
        val r = (if (step > 0f) step / 2.6f else 11 * u).coerceIn(6 * u, 11 * u)
        beads.forEachIndexed { i, b ->
            val p = pt(i)
            val ring = when (b) { Bead.TAKEN -> taken; Bead.NOT_TAKEN -> not; Bead.MISSED -> miss; Bead.OPEN -> accent }
            drawCircle(if (b == Bead.TAKEN || b == Bead.NOT_TAKEN) ring else cs.surface, radius = r, center = p)
            drawCircle(ring, radius = r, center = p, style = Stroke(2.4f * u))
            if (b == Bead.TAKEN) {
                val tick = Path().apply {
                    moveTo(p.x - r * 0.42f, p.y); relativeLineTo(r * 0.3f, r * 0.32f); relativeLineTo(r * 0.55f, -r * 0.6f)
                }
                drawPath(tick, Color.White, style = Stroke(2f * u, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
        val g = Offset(size.width / 2, top + sag + 26 * u)
        drawLine(cs.outline, Offset(size.width / 2, top + sag), Offset(g.x, g.y - 7 * u), 1.5f * u)
        drawCircle(accent, radius = 6 * u, center = g)
    }
}

// ================================================================ the jharokha arch

/** Draws a jharokha (Indian window) arch filling the current size, with a small keystone. */
fun DrawScope.jharokha(color: Color) {
    val w = size.width; val h = size.height; val top = 12.dp.toPx()
    val ry = min(w / 2, (h - top) * 0.42f)
    val inset = 7.dp.toPx()
    fun arch(i: Float) = Path().apply {
        moveTo(i, h); lineTo(i, top + ry)
        arcTo(Rect(i, top + i, w - i, top + 2 * ry - i), 180f, 180f, false)
        lineTo(w - i, h)
    }
    drawPath(arch(0f), color, style = Stroke(2.dp.toPx()))
    drawPath(arch(inset), color.copy(alpha = color.alpha * 0.6f), style = Stroke(1.dp.toPx()))
    val k = Path().apply { moveTo(w / 2 - 7.dp.toPx(), top - 1.dp.toPx()); lineTo(w / 2, top - 11.dp.toPx()); lineTo(w / 2 + 7.dp.toPx(), top - 1.dp.toPx()); close() }
    drawPath(k, color)
}

// ================================================================ the dock

data class DockTab(val label: String, val icon: ImageVector)

/**
 * The symmetric dock: two sections, the raised Add button in the very centre, two sections.
 * Expects exactly four tabs.
 */
@Composable
fun PraharDock(tabs: List<DockTab>, selected: Int, onSelect: (Int) -> Unit, onAdd: () -> Unit, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(36.dp)
    Box(
        modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 14.dp, end = 14.dp, bottom = 12.dp, top = 30.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Row(
            Modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .shadow(18.dp, shape, ambientColor = Color.Black.copy(alpha = 0.25f), spotColor = Color.Black.copy(alpha = 0.25f))
                .clip(shape)
                .background(cs.surface)
                .border(1.dp, cs.outlineVariant, shape)
                .padding(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            tabs.forEachIndexed { i, t ->
                if (i == tabs.size / 2) Spacer(Modifier.width(78.dp))
                val on = i == selected
                Column(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(28.dp))
                        .background(if (on) cs.primaryContainer else Color.Transparent)
                        .selectable(selected = on, role = Role.Tab, onClick = { onSelect(i) })
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(t.icon, null, tint = if (on) cs.primary else cs.onSurfaceVariant, modifier = Modifier.size(26.dp))
                    Text(
                        t.label,
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.sp),
                        color = if (on) cs.onPrimaryContainer else cs.onSurfaceVariant,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        Box(
            Modifier
                .align(Alignment.Center)
                .offset(y = (-26).dp)
                .size(70.dp)
                .shadow(14.dp, CircleShape)
                .clip(CircleShape)
                .background(cs.secondary)
                .border(4.dp, cs.background, CircleShape)
                .clickable(role = Role.Button, onClick = onAdd)
                .semantics { contentDescription = "Add medicines" },
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Rounded.Add, null, tint = cs.onSecondary, modifier = Modifier.size(34.dp)) }
    }
}
