@file:OptIn(ExperimentalLayoutApi::class)

package com.ragini.medreminder.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AlarmOn
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.ragini.medreminder.alarm.AlarmService
import com.ragini.medreminder.alarm.CaregiverAlert
import com.ragini.medreminder.data.Db
import com.ragini.medreminder.data.DoseStatus
import com.ragini.medreminder.data.minuteLabel
import com.ragini.medreminder.schedule.AlarmScheduler
import com.ragini.medreminder.ui.AppModel
import com.ragini.medreminder.ui.Reliability
import com.ragini.medreminder.ui.components.BigButton
import com.ragini.medreminder.ui.components.BigOutlinedButton
import com.ragini.medreminder.ui.components.ChoicePill
import com.ragini.medreminder.ui.components.Eyebrow
import com.ragini.medreminder.ui.components.Segmented
import com.ragini.medreminder.ui.components.SoftCard
import com.ragini.medreminder.ui.components.Stepper
import com.ragini.medreminder.ui.theme.StatusColors
import com.ragini.medreminder.voice.TuneSynth
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import kotlinx.coroutines.delay

@Composable
fun SettingsScreen(model: AppModel, onClose: () -> Unit) {
    val ctx = LocalContext.current
    val p = model.prefs
    var name by remember { mutableStateOf(p.userName) }
    var lead by remember { mutableIntStateOf(p.leadMinutes) }
    var gap by remember { mutableIntStateOf(p.gapMinutes) }
    var follow by remember { mutableIntStateOf(p.followUps) }
    var followGap by remember { mutableIntStateOf(p.followGapMinutes) }
    var ringLimit by remember { mutableIntStateOf(p.ringLimitMinutes) }
    var loud by remember { mutableStateOf(p.loudBoost) }
    var tuneSheet by remember { mutableStateOf(false) }
    var maxVol by remember { mutableStateOf(p.forceMaxVolume) }
    var vibrate by remember { mutableStateOf(p.vibrate) }
    var chime by remember { mutableStateOf(p.chimeOn) }
    var flash by remember { mutableStateOf(p.flashOn) }
    var close by remember { mutableIntStateOf(p.dayCloseMinute) }
    var cgName by remember { mutableStateOf(p.caregiverName) }
    var cgPhone by remember { mutableStateOf(p.caregiverPhone) }
    var cgSms by remember { mutableStateOf(p.caregiverSms && CaregiverAlert.canSms(ctx)) }
    var scale by remember { mutableFloatStateOf(model.textScale) }
    var testQueued by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }

    val askSms = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        cgSms = ok
        p.caregiverSms = ok
        model.rhythmChanged()
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .imePadding()
    ) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { model.updateName(name); onClose() }, modifier = Modifier.size(56.dp)) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", Modifier.size(28.dp))
            }
            Text("Settings", style = MaterialTheme.typography.headlineSmall)
        }
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ReliabilityCard(model)

            SoftCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Test it", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Press the button, lock the phone and put it down. In one minute you should hear the reminder loud and clear.",
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    BigButton(
                        if (testQueued) "Test set – lock the phone now" else "Test reminder in 1 minute",
                        onClick = { AlarmScheduler.scheduleTest(ctx); testQueued = true },
                        icon = Icons.Rounded.AlarmOn, modifier = Modifier.fillMaxWidth()
                    )
                    BigOutlinedButton("Play a test right now", onClick = { AlarmService.startTest(ctx) },
                        icon = Icons.AutoMirrored.Rounded.VolumeUp, modifier = Modifier.fillMaxWidth())
                }
            }

            SoftCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("About you", style = MaterialTheme.typography.titleLarge)
                    OutlinedTextField(
                        value = name, onValueChange = { name = it; model.updateName(it) },
                        label = { Text("What should Ragini call you?") },
                        placeholder = { Text("e.g. Aai, Maa, Ragini ji") },
                        textStyle = MaterialTheme.typography.bodyLarge, singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            SoftCard {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Reminder rhythm", style = MaterialTheme.typography.titleLarge)
                    SettingStepper("Start reminding", "minutes before each medicine", lead, 0..60, { "$it min" }) {
                        lead = it; p.leadMinutes = it; model.rhythmChanged()
                    }
                    SettingStepper("Remind every", "minutes until it is time", gap, 1..10, { "$it min" }) {
                        gap = it; p.gapMinutes = it; model.rhythmChanged()
                    }
                    val count = if (lead <= 0) 0 else (lead + gap - 1) / gap
                    RhythmStrip(lead, gap)
                    Text(
                        "$count spoken reminders before the time, then the main reminder at the exact minute.",
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "At medicine time the tune and the voice repeat until every medicine is answered with Taken (or Not taken with a reason). Safety limit:",
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(10, 20, 30, 60, 120).forEach { m ->
                            ChoicePill(if (m < 60) "$m min" else "${m / 60} hour${if (m > 60) "s" else ""}", ringLimit == m, {
                                ringLimit = m; p.ringLimitMinutes = m
                            })
                        }
                    }
                    SettingStepper("Extra reminders after", "if there is still no answer", follow, 0..6, { "$it" }) {
                        follow = it; p.followUps = it; model.rhythmChanged()
                    }
                    if (follow > 0) {
                        SettingStepper("…spaced", "minutes apart", followGap, 1..30, { "$it min" }) {
                            followGap = it; p.followGapMinutes = it; model.rhythmChanged()
                        }
                    }
                }
            }

            SoftCard {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Loud & clear", style = MaterialTheme.typography.titleLarge)
                    SwitchRow(Icons.Rounded.NotificationsActive, "Full alarm volume", "Turns the alarm volume up while speaking, then puts it back", maxVol) {
                        maxVol = it; p.forceMaxVolume = it; model.touch()
                    }
                    SwitchRow(Icons.Rounded.MusicNote, "Play the tune", "Before every spoken reminder", chime) { chime = it; p.chimeOn = it }
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).clickable { tuneSheet = true }.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Spacer(Modifier.width(40.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Tune: ${TuneSynth.byId(model.tuneId).title}", style = MaterialTheme.typography.titleMedium)
                            Text("Choose from ${TuneSynth.all.size} tunes", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null)
                    }
                    SwitchRow(Icons.AutoMirrored.Rounded.VolumeUp, "Extra-loud voice", "Makes the spoken words louder and clearer than normal", loud) { loud = it; p.loudBoost = it }
                    SwitchRow(Icons.Rounded.Vibration, "Vibrate", "Strong vibration pattern while reminding", vibrate) { vibrate = it; p.vibrate = it }
                    SwitchRow(Icons.Rounded.FlashlightOn, "Blink the torch", "For weak hearing – the camera light flashes", flash) { flash = it; p.flashOn = it }
                }
            }

            SoftCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Attendance", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "At this time each night, every dose of the day is written in the register: answered Taken or Not taken (with a reason) counts as present; no answer counts as absent.",
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(22 * 60, 23 * 60, 23 * 60 + 30, 23 * 60 + 59).forEach { m ->
                            ChoicePill(minuteLabel(m), close == m, {
                                close = m; p.dayCloseMinute = m; AlarmScheduler.scheduleDayClose(ctx)
                            })
                        }
                    }
                }
            }

            SoftCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Family alert", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "If a dose is still unanswered after all reminders, Ragini can send a text message to someone in the family.",
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = cgName, onValueChange = { cgName = it; p.caregiverName = it },
                        label = { Text("Name") }, singleLine = true, textStyle = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = cgPhone, onValueChange = { cgPhone = it; p.caregiverPhone = it },
                        label = { Text("Mobile number") }, singleLine = true, textStyle = MaterialTheme.typography.bodyLarge,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth()
                    )
                    SwitchRow(null, "Send SMS when a dose is missed", "Uses your SIM; normal SMS charges apply", cgSms) { on ->
                        if (on && ContextCompat.checkSelfPermission(ctx, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
                            askSms.launch(Manifest.permission.SEND_SMS)
                        } else {
                            cgSms = on; p.caregiverSms = on; model.rhythmChanged()
                        }
                    }
                }
            }

            SoftCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Display", style = MaterialTheme.typography.titleLarge)
                    Eyebrow("Text size")
                    Slider(
                        value = scale, onValueChange = { scale = it },
                        onValueChangeFinished = { model.updateTextScale(scale) },
                        valueRange = 0.9f..1.5f, steps = 5
                    )
                    Text("Aa – this is how big the words will be", style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = MaterialTheme.typography.bodyLarge.fontSize * (scale / model.textScale)))
                    Eyebrow("Colours")
                    Segmented(listOf(0, 1, 2), model.themeMode, { model.updateTheme(it) },
                        label = { when (it) { 1 -> "Light"; 2 -> "Dark"; else -> "Like phone" } })
                }
            }

            SoftCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Start fresh", style = MaterialTheme.typography.titleLarge)
                    BigOutlinedButton("Delete all medicines and history", onClick = { confirmReset = true },
                        icon = Icons.Rounded.RestartAlt, color = MaterialTheme.colorScheme.error, modifier = Modifier.fillMaxWidth())
                }
            }
            Text(
                "Ragini ${appVersion(ctx)} · Android ${Build.VERSION.RELEASE}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 24.dp)
            )
        }
    }

    if (tuneSheet) TunePickerSheet(model) { tuneSheet = false }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Delete everything?") },
            text = { Text("All medicines, answers and reports will be removed from this phone. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    Db.get(ctx).clearAll()
                    p.firstDay = java.time.LocalDate.now()
                    model.rhythmChanged()
                    confirmReset = false
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancel") } }
        )
    }
}

private fun appVersion(ctx: android.content.Context): String = try {
    ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: ""
} catch (_: Exception) {
    ""
}

@Composable
private fun SettingStepper(title: String, sub: String, value: Int, range: IntRange, format: (Int) -> String, step: Int = 1, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Stepper(value, onChange, range, step = step, format = format)
    }
}

@Composable
private fun SwitchRow(icon: ImageVector?, title: String, sub: String, on: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(26.dp))
            Spacer(Modifier.width(14.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(8.dp))
        Switch(checked = on, onCheckedChange = onChange)
    }
}

/** A row of dots showing exactly when the voice will speak: –19, –17 … –1, then the dose. */
@Composable
private fun RhythmStrip(lead: Int, gap: Int) {
    val marks = buildList {
        var m = lead
        while (m > 0) { add(m); m -= gap }
    }
    val cs = MaterialTheme.colorScheme
    androidx.compose.foundation.Canvas(Modifier.fillMaxWidth().height(36.dp)) {
        val pad = 10.dp.toPx()
        val span = (lead.coerceAtLeast(1)).toFloat()
        val y = size.height / 2
        drawLine(cs.outlineVariant, androidx.compose.ui.geometry.Offset(pad, y), androidx.compose.ui.geometry.Offset(size.width - pad, y), 3.dp.toPx())
        fun x(minBefore: Int) = pad + (1f - minBefore / span) * (size.width - 2 * pad)
        marks.forEach { m -> drawCircle(cs.primary.copy(alpha = 0.55f), radius = 5.dp.toPx(), center = androidx.compose.ui.geometry.Offset(x(m), y)) }
        drawCircle(cs.secondary, radius = 10.dp.toPx(), center = androidx.compose.ui.geometry.Offset(size.width - pad, y))
    }
}

/** "Will it ring on standby?" – used in Settings and during setup. */
@Composable
fun ReliabilityCard(model: AppModel) {
    val ctx = LocalContext.current
    var checks by remember { mutableStateOf(Reliability.checks(ctx)) }
    val askNotif = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        checks = Reliability.checks(ctx); model.touch()
    }
    // Re-check whenever the user comes back from a settings page.
    LaunchedEffect(model.version) { checks = Reliability.checks(ctx) }
    LaunchedEffect(Unit) {
        while (true) { delay(2500); checks = Reliability.checks(ctx) }
    }
    val bad = checks.count { !it.ok && it.id != "autostart" }
    SoftCard(color = (if (bad == 0) StatusColors.of(DoseStatus.TAKEN) else StatusColors.of(DoseStatus.MISSED)).copy(alpha = 0.08f)) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                if (bad == 0) "Ready to ring on standby" else "$bad setting${if (bad > 1) "s" else ""} to fix",
                style = MaterialTheme.typography.titleLarge
            )
            checks.forEach { c ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (c.ok) Icons.Rounded.CheckCircle else Icons.Rounded.ErrorOutline,
                        if (c.ok) "Done" else "Needs attention",
                        tint = if (c.ok) StatusColors.of(DoseStatus.TAKEN) else if (c.id == "autostart") MaterialTheme.colorScheme.secondary else StatusColors.of(DoseStatus.MISSED),
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(c.title, style = MaterialTheme.typography.titleMedium)
                        Text(c.why, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (!c.ok && c.fix != null) {
                        TextButton(onClick = {
                            if (c.id == "notif" && Build.VERSION.SDK_INT >= 33 &&
                                ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                            ) askNotif.launch(Manifest.permission.POST_NOTIFICATIONS)
                            else c.fix.invoke(ctx)
                        }) { Text(c.fixLabel, style = MaterialTheme.typography.labelLarge) }
                    }
                }
            }
        }
    }
}
