package com.ragini.medreminder.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ragini.medreminder.data.DoseStatus
import com.ragini.medreminder.data.PillShape
import com.ragini.medreminder.ui.theme.MedColors
import com.ragini.medreminder.ui.theme.StatusColors

// ---------------------------------------------------------------- pills

/** A little drawing of the medicine in its own colour and shape, so it is recognised without reading. */
@Composable
fun PillIcon(colorIndex: Int, shape: PillShape, size: Dp = 48.dp, modifier: Modifier = Modifier) {
    val c = MedColors.of(colorIndex)
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(c.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.size(size * 0.62f)) { drawPill(shape, c) }
    }
}

private fun DrawScope.drawPill(shape: PillShape, c: Color) {
    val w = size.width
    val h = size.height
    when (shape) {
        PillShape.CAPSULE -> rotate(-35f) {
            val ph = h * 0.42f
            val top = (h - ph) / 2
            val r = CornerRadius(ph / 2, ph / 2)
            drawRoundRect(Color.White, Offset(0f, top), Size(w, ph), r)
            clipRect(left = w / 2, top = 0f, right = w, bottom = h) {
                drawRoundRect(c, Offset(0f, top), Size(w, ph), r)
            }
            drawRoundRect(c, Offset(0f, top), Size(w, ph), r, style = Stroke(w * 0.06f))
        }
        PillShape.ROUND -> {
            drawCircle(c, radius = w * 0.44f)
            drawLine(
                Color.White.copy(alpha = 0.85f), Offset(w * 0.22f, h / 2), Offset(w * 0.78f, h / 2),
                strokeWidth = w * 0.07f, cap = StrokeCap.Round
            )
        }
        PillShape.OVAL -> rotate(-25f) {
            drawOval(c, Offset(0f, h * 0.24f), Size(w, h * 0.52f))
            drawOval(Color.White.copy(alpha = 0.35f), Offset(w * 0.14f, h * 0.31f), Size(w * 0.4f, h * 0.16f))
        }
        PillShape.LIQUID -> {
            val body = Path().apply {
                moveTo(w * 0.38f, h * 0.05f); lineTo(w * 0.62f, h * 0.05f)
                lineTo(w * 0.62f, h * 0.24f); lineTo(w * 0.82f, h * 0.38f)
                lineTo(w * 0.82f, h * 0.95f); lineTo(w * 0.18f, h * 0.95f)
                lineTo(w * 0.18f, h * 0.38f); lineTo(w * 0.38f, h * 0.24f); close()
            }
            drawPath(body, c.copy(alpha = 0.35f))
            drawRect(c, Offset(w * 0.18f, h * 0.55f), Size(w * 0.64f, h * 0.4f))
            drawPath(body, c, style = Stroke(w * 0.06f))
        }
        PillShape.DROPS -> {
            val drop = Path().apply {
                moveTo(w / 2, h * 0.04f)
                cubicTo(w * 0.85f, h * 0.45f, w * 0.88f, h * 0.62f, w * 0.82f, h * 0.74f)
                cubicTo(w * 0.7f, h * 1.0f, w * 0.3f, h * 1.0f, w * 0.18f, h * 0.74f)
                cubicTo(w * 0.12f, h * 0.62f, w * 0.15f, h * 0.45f, w / 2, h * 0.04f)
                close()
            }
            drawPath(drop, c)
            drawCircle(Color.White.copy(alpha = 0.45f), radius = w * 0.08f, center = Offset(w * 0.38f, h * 0.66f))
        }
        PillShape.INJECTION -> rotate(-45f) {
            drawRoundRect(c.copy(alpha = 0.35f), Offset(w * 0.12f, h * 0.38f), Size(w * 0.56f, h * 0.24f), CornerRadius(4f, 4f))
            drawRoundRect(c, Offset(w * 0.12f, h * 0.38f), Size(w * 0.56f, h * 0.24f), CornerRadius(4f, 4f), style = Stroke(w * 0.06f))
            drawRect(c, Offset(w * 0.12f, h * 0.38f), Size(w * 0.3f, h * 0.24f))
            drawLine(c, Offset(w * 0.68f, h / 2), Offset(w * 0.98f, h / 2), strokeWidth = w * 0.04f)
            drawLine(c, Offset(0f, h / 2), Offset(w * 0.12f, h / 2), strokeWidth = w * 0.1f)
            drawLine(c, Offset(0f, h * 0.34f), Offset(0f, h * 0.66f), strokeWidth = w * 0.08f)
        }
    }
}

// ---------------------------------------------------------------- status

@Composable
fun StatusChip(status: DoseStatus, label: String = status.label, modifier: Modifier = Modifier) {
    val c = StatusColors.of(status)
    Row(
        modifier
            .clip(RoundedCornerShape(50))
            .background(c.copy(alpha = 0.14f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(9.dp).clip(CircleShape).background(c))
        Spacer(Modifier.width(7.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = c)
    }
}

// ---------------------------------------------------------------- buttons & cards

/** A tall, easy-to-hit button (at least 64 dp high). */
@Composable
fun BigButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    container: Color = MaterialTheme.colorScheme.primary,
    content: Color = MaterialTheme.colorScheme.onPrimary,
    enabled: Boolean = true,
    height: Dp = 64.dp
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = height),
        shape = RoundedCornerShape(22.dp),
        colors = ButtonDefaults.buttonColors(containerColor = container, contentColor = content),
        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 14.dp)
    ) {
        if (icon != null) {
            Icon(icon, null, Modifier.size(26.dp))
            Spacer(Modifier.width(10.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
    }
}

@Composable
fun BigOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    color: Color = MaterialTheme.colorScheme.primary,
    height: Dp = 64.dp
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = height),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(2.dp, color.copy(alpha = 0.6f)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = color),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp)
    ) {
        if (icon != null) {
            Icon(icon, null, Modifier.size(24.dp))
            Spacer(Modifier.width(10.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
    }
}

@Composable
fun SoftCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    color: Color = MaterialTheme.colorScheme.surface,
    padding: Dp = 20.dp,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(28.dp)
    val base = modifier
        .fillMaxWidth()
        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = base,
            shape = shape,
            colors = CardDefaults.cardColors(containerColor = color)
        ) { Box(Modifier.padding(padding)) { content() } }
    } else {
        Card(
            modifier = base,
            shape = shape,
            colors = CardDefaults.cardColors(containerColor = color)
        ) { Box(Modifier.padding(padding)) { content() } }
    }
}

@Composable
fun SectionTitle(title: String, subtitle: String? = null, modifier: Modifier = Modifier) {
    Column(modifier.padding(top = 8.dp, bottom = 4.dp)) {
        Text(title, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground)
        if (subtitle != null) {
            Spacer(Modifier.height(2.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Small uppercase label above a group of controls. */
@Composable
fun Eyebrow(text: String, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Text(text.uppercase(), style = MaterialTheme.typography.labelSmall, color = color, modifier = modifier)
}

/** A − value + control with big round buttons. */
@Composable
fun Stepper(
    value: Int,
    onChange: (Int) -> Unit,
    range: IntRange,
    modifier: Modifier = Modifier,
    step: Int = 1,
    format: (Int) -> String = { it.toString() }
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        FilledTonalIconButton(
            onClick = { onChange((value - step).coerceIn(range)) },
            enabled = value > range.first,
            modifier = Modifier.size(52.dp)
        ) { Icon(Icons.Rounded.Remove, "Less", Modifier.size(28.dp)) }
        Text(
            format(value),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(112.dp)
        )
        FilledTonalIconButton(
            onClick = { onChange((value + step).coerceIn(range)) },
            enabled = value < range.last,
            modifier = Modifier.size(52.dp)
        ) { Icon(Icons.Rounded.Add, "More", Modifier.size(28.dp)) }
    }
}

/** A single-choice row of equal-width segments. */
@Composable
fun <T> Segmented(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: (T) -> String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(Modifier.padding(5.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            options.forEach { o ->
                SegmentCell(label(o), o == selected) { onSelect(o) }
            }
        }
    }
}

@Composable
private fun RowScope.SegmentCell(text: String, on: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .weight(1f)
            .heightIn(min = 50.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (on) MaterialTheme.colorScheme.primary else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (on) FontWeight.SemiBold else FontWeight.Medium,
            color = if (on) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}

/** A choice chip that is big enough for older fingers. */
@Composable
fun ChoicePill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null
) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(50))
            .background(if (selected) cs.primary else cs.surface)
            .border(1.5.dp, if (selected) cs.primary else cs.outlineVariant, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) cs.onPrimary else cs.onSurface
        )
    }
}

@Composable
fun IconBadge(icon: ImageVector, tint: Color, modifier: Modifier = Modifier, size: Dp = 48.dp) {
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(tint.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
    ) { Icon(icon, null, tint = tint, modifier = Modifier.size(size * 0.5f)) }
}
