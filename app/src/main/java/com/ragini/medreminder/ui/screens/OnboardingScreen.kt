package com.ragini.medreminder.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ragini.medreminder.alarm.AlarmService
import com.ragini.medreminder.ui.AppModel
import com.ragini.medreminder.ui.components.BigButton
import com.ragini.medreminder.ui.components.Eyebrow
import com.ragini.medreminder.ui.theme.Palette
import com.ragini.medreminder.voice.Phrases
import com.ragini.medreminder.voice.VoiceCatalog

/** Three calm steps: who you are → pick a voice → make sure it can ring on standby. */
@Composable
fun OnboardingScreen(model: AppModel) {
    val ctx = LocalContext.current
    var step by remember { mutableIntStateOf(0) }
    var name by remember { mutableStateOf(model.userName) }
    val askNotif = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { model.touch() }
    LaunchedEffect(step) {
        if (step == 2 && Build.VERSION.SDK_INT >= 33) askNotif.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
    BackHandler(enabled = step > 0) { step-- }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 24.dp)
    ) {
        // Step dots
        Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.Center) {
            repeat(3) { i ->
                Box(
                    Modifier
                        .padding(horizontal = 5.dp)
                        .size(width = if (i == step) 34.dp else 10.dp, height = 10.dp)
                        .clip(RoundedCornerShape(50))
                        .background(if (i <= step) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outlineVariant)
                )
            }
        }
        AnimatedContent(
            targetState = step,
            transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) },
            modifier = Modifier.weight(1f),
            label = "step"
        ) { s ->
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                when (s) {
                    0 -> {
                        Box(Modifier.fillMaxWidth().height(230.dp), contentAlignment = Alignment.Center) { Emblem() }
                        Text("Namaste.", style = MaterialTheme.typography.displayMedium, color = MaterialTheme.colorScheme.primary)
                        Text(
                            "I am Ragini. I will speak your medicine reminders out loud – starting 19 minutes before, every 2 minutes – so no dose is forgotten, even when the phone is far away or asleep.",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        OutlinedTextField(
                            value = name, onValueChange = { name = it },
                            label = { Text("What should I call you?") },
                            placeholder = { Text("e.g. Aai, Maa, Ragini ji") },
                            textStyle = MaterialTheme.typography.headlineSmall, singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    1 -> {
                        Eyebrow("Step 2 of 3")
                        Text("Choose a voice", style = MaterialTheme.typography.headlineLarge)
                        Text(
                            "Tap a voice to hear it. There are ${VoiceCatalog.all.size} more in the Voices tab – and you can record a family member's voice too.",
                            style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        val picks = listOf("en-IN-gentle", "en-IN-cheerful", "hi-IN-gentle", "hi-IN-devotional", "mr-IN-gentle", "en-GB-calm")
                            .mapNotNull { VoiceCatalog.byId(it) }
                        picks.chunked(2).forEach { pair ->
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                pair.forEach { p ->
                                    val on = model.defaultVoiceId == p.id
                                    val c = moodColor(p.mood)
                                    Column(
                                        Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(26.dp))
                                            .background(if (on) c.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface)
                                            .border(if (on) 2.5.dp else 1.dp, if (on) c else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(26.dp))
                                            .clickable {
                                                model.updateDefaultVoice(p.id)
                                                model.preview(p.id, p, Phrases.sample(p, name.trim()))
                                            }
                                            .padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Box(contentAlignment = Alignment.BottomEnd) {
                                            Avatar(p.name, c, 60.dp)
                                            if (on) Icon(Icons.Rounded.CheckCircle, null, tint = c,
                                                modifier = Modifier.size(24.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surface))
                                        }
                                        Spacer(Modifier.height(8.dp))
                                        Text(p.name, style = MaterialTheme.typography.titleMedium)
                                        Text(p.mood.title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                                        Text(p.lang.native, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                    else -> {
                        Eyebrow("Step 3 of 3")
                        Text("Ring on standby", style = MaterialTheme.typography.headlineLarge)
                        Text(
                            "Android sometimes quietens apps to save battery. Allow these once and reminders will ring loud and clear even when the screen is off.",
                            style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        ReliabilityCard(model)
                        TextButton(onClick = { AlarmService.startTest(ctx) }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.AutoMirrored.Rounded.VolumeUp, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Play a test reminder", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
        BigButton(
            text = when (step) { 0 -> "Let's begin"; 1 -> "Use this voice"; else -> "Finish" },
            onClick = {
                when (step) {
                    0 -> { model.updateName(name); step = 1 }
                    1 -> { model.stopPreview(); step = 2 }
                    else -> model.finishOnboarding()
                }
            },
            icon = if (step == 2) Icons.Rounded.Check else Icons.AutoMirrored.Rounded.ArrowForward,
            container = if (step == 2) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
            content = if (step == 2) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondary,
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        )
    }
}

/** The Ragini mark: a capsule sending out sound waves, gently breathing. */
@Composable
private fun Emblem() {
    val t = rememberInfiniteTransition(label = "emblem")
    val wave by t.animateFloat(0f, 1f, infiniteRepeatable(tween(2200, easing = LinearEasing)), label = "w")
    Box(
        Modifier
            .size(210.dp)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(Color(0xFF3B3685), Palette.Indigo))),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.size(150.dp)) {
            val w = size.width
            val h = size.height
            rotate(-30f, pivot = Offset(w * 0.38f, h / 2)) {
                val ph = h * 0.3f
                val top = h / 2 - ph / 2
                drawRoundRect(Color.White, Offset(w * 0.12f, top), Size(w * 0.26f + ph / 2, ph), CornerRadius(ph / 2, ph / 2))
                drawRoundRect(Palette.Marigold, Offset(w * 0.38f - ph / 2, top), Size(w * 0.26f + ph / 2, ph), CornerRadius(ph / 2, ph / 2))
                drawRect(Color.White, Offset(w * 0.12f + ph / 2, top), Size(w * 0.26f - ph / 2, ph))
            }
            for (i in 0 until 3) {
                val f = (wave + i / 3f) % 1f
                val r = w * (0.18f + 0.28f * f)
                drawArc(
                    color = (if (i == 0) Palette.Marigold else Color.White).copy(alpha = (1f - f) * 0.9f),
                    startAngle = -45f, sweepAngle = 90f, useCenter = false,
                    topLeft = Offset(w * 0.62f - r, h / 2 - r), size = Size(r * 2, r * 2),
                    style = Stroke(w * 0.045f, cap = StrokeCap.Round)
                )
            }
        }
    }
}
