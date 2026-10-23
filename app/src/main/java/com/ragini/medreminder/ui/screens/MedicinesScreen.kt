@file:OptIn(ExperimentalLayoutApi::class)

package com.ragini.medreminder.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ragini.medreminder.data.Medicine
import com.ragini.medreminder.data.minuteLabel
import com.ragini.medreminder.ui.AppModel
import com.ragini.medreminder.ui.Fmt
import com.ragini.medreminder.ui.components.Eyebrow
import com.ragini.medreminder.ui.components.PillIcon
import com.ragini.medreminder.ui.components.SkyHeader
import com.ragini.medreminder.ui.components.SoftCard
import com.ragini.medreminder.ui.components.bleed
import com.ragini.medreminder.ui.theme.MedColors
import com.ragini.medreminder.voice.VoiceCatalog

@Composable
fun MedicinesScreen(model: AppModel, onAdd: () -> Unit, onEdit: (Long) -> Unit, onSettings: () -> Unit = {}) {
    val meds = model.meds.sortedWith(compareBy<Medicine>({ !it.active }, { it.times.minOrNull() ?: 0 }, { it.name.lowercase() }))
    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 0.dp, bottom = 150.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                SkyHeader(
                    eyebrow = "${meds.count { it.active }} active · ${meds.size} total",
                    title = "Medicines",
                    subtitle = "Tap a medicine to change its name, time, voice or reminder start.",
                    onSettings = onSettings,
                    modifier = Modifier.bleed(20.dp)
                )
            }
            if (meds.isEmpty()) item { EmptyToday(onAdd) }
            items(meds, key = { it.id }) { m -> MedicineCard(model, m, onClick = { onEdit(m.id) }) }
            item { MoveListCard(model) }
        }
    }
}

@Composable
private fun MedicineCard(model: AppModel, m: Medicine, onClick: () -> Unit) {
    val voiceId = m.voiceId ?: model.defaultVoiceId
    val voice = VoiceCatalog.byId(voiceId)?.name
        ?: model.prefs.recordedVoices.firstOrNull { it.id == voiceId }?.label
        ?: VoiceCatalog.fallback.name
    val lead = m.leadMinutes ?: model.prefs.leadMinutes
    SoftCard(onClick = onClick, padding = 18.dp, modifier = Modifier.alpha(if (m.active) 1f else 0.55f)) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PillIcon(m.colorIndex, m.shape, size = 60.dp)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(m.name, style = MaterialTheme.typography.titleLarge, maxLines = 3, overflow = TextOverflow.Ellipsis)
                    if (m.dosage.isNotBlank()) {
                        Text(m.dosage, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Switch(
                    checked = m.active,
                    onCheckedChange = { model.setActive(m, it) },
                    colors = SwitchDefaults.colors(checkedTrackColor = MedColors.of(m.colorIndex))
                )
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                m.times.forEach { t ->
                    Row(
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .background(MedColors.of(m.colorIndex).copy(alpha = 0.14f))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Schedule, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.width(5.dp))
                        Text(minuteLabel(t), style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
            Text(Fmt.schedule(m), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Rounded.VolumeUp, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(6.dp))
                Text(
                    "$voice · from $lead min before, every ${model.prefs.gapMinutes} min",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            val stock = m.stock
            if (stock != null) {
                val cap = maxOf(stock, m.refillAt * 4, 1)
                Column {
                    Row {
                        Text(
                            "$stock left",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (m.lowStock) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        Text("Refill at ${m.refillAt}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { (stock.toFloat() / cap).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)),
                        color = if (m.lowStock) MaterialTheme.colorScheme.error else MedColors.of(m.colorIndex),
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }
    }
}
