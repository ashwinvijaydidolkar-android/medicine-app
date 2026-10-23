package com.ragini.medreminder.alarm

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ragini.medreminder.data.Db
import com.ragini.medreminder.data.Dose
import com.ragini.medreminder.data.DoseStatus
import com.ragini.medreminder.data.FoodRule
import com.ragini.medreminder.data.Prefs
import com.ragini.medreminder.data.Reasons
import com.ragini.medreminder.data.minuteLabel
import com.ragini.medreminder.schedule.Answers
import com.ragini.medreminder.ui.components.BigButton
import com.ragini.medreminder.ui.components.BigOutlinedButton
import com.ragini.medreminder.ui.components.PillIcon
import com.ragini.medreminder.ui.theme.MedColors
import com.ragini.medreminder.ui.theme.LocalPrahar
import com.ragini.medreminder.ui.theme.Prahars
import com.ragini.medreminder.ui.theme.Slots
import com.ragini.medreminder.ui.components.TurningMandala
import com.ragini.medreminder.ui.components.jharokha
import com.ragini.medreminder.ui.theme.RaginiTheme
import com.ragini.medreminder.ui.theme.StatusColors
import com.ragini.medreminder.voice.TuneSynth
import com.ragini.medreminder.voice.VoiceCatalog
import kotlinx.coroutines.delay

/**
 * The full-screen reminder. It appears over the lock screen and turns the screen on.
 *
 * Every medicine of this time is shown as its own numbered card with a big "Taken" button
 * (and "Not taken" + reason). The tune and the voice keep going until EVERY card is answered;
 * there is no button that silences a due medicine without answering it.
 */
class ReminderActivity : ComponentActivity() {

    companion object {
        private const val EX_KEYS = "keys"
        private const val EX_TEST = "test"
        private const val EX_ASK = "ask"
        private const val EX_KICK = "kick"

        fun intent(ctx: Context, keys: Array<String>, isTest: Boolean, askFirst: Boolean = false, kick: Boolean = false): Intent =
            Intent(ctx, ReminderActivity::class.java)
                .putExtra(EX_KEYS, keys)
                .putExtra(EX_TEST, isTest)
                .putExtra(EX_ASK, askFirst)
                .putExtra(EX_KICK, kick)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION)
    }

    private var keys by mutableStateOf<List<String>>(emptyList())
    private var isTest by mutableStateOf(false)
    private var allDone by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        wakeUpScreen()
        read(intent)
        setContent {
            val prefs = remember { Prefs(this) }
            RaginiTheme(textScale = prefs.textScale, forceDark = true) { Screen(prefs) }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        read(intent)
    }

    private fun read(i: Intent) {
        val incoming = i.getStringArrayExtra(EX_KEYS)?.toList() ?: emptyList()
        isTest = i.getBooleanExtra(EX_TEST, false)
        keys = (keys + incoming).distinct()
        if (incoming.isNotEmpty()) allDone = false
        if (i.getBooleanExtra(EX_KICK, false)) {
            AlarmService.startFromScreen(this, incoming.toTypedArray(), isTest)
        }
    }

    private fun wakeUpScreen() {
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    // ------------------------------------------------------------------ UI

    @Composable
    private fun Screen(prefs: Prefs) {
        val db = remember { Db.get(this) }
        val doses = remember(keys) {
            keys.mapNotNull { Answers.doseFor(this, it) }
                .sortedWith(compareBy<Dose>({ it.at }, { it.medicine.name.lowercase() }))
        }
        val answered = remember { mutableStateMapOf<String, DoseStatus>() }
        LaunchedEffect(keys) {
            keys.forEach { k -> db.response(k)?.let { answered[k] = it.status } }
        }
        val ringing by AlarmService.ringingKeys.collectAsState()
        val ringingTest by AlarmService.ringingTest.collectAsState()
        val dueNow = doses.any { it.at <= System.currentTimeMillis() + 30_000 }
        val open = doses.filter { !answered.containsKey(it.key) }

        LaunchedEffect(open.size, doses.size) {
            if (!isTest && doses.isNotEmpty() && open.isEmpty()) {
                delay(600)
                allDone = true
            }
        }
        // While a due medicine is unanswered, Back does nothing – it must be answered.
        BackHandler(enabled = !isTest && !allDone && dueNow && open.isNotEmpty()) { }

        // The reminder wears the deep sky of the medicine's own prahar – always dark, like an alarm.
        val prahar = remember(doses) {
            doses.firstOrNull()?.let { Prahars.at(it.minuteOfDay) } ?: Prahars.now()
        }
        CompositionLocalProvider(LocalPrahar provides prahar) {
            Box(
                Modifier
                    .fillMaxSize()
                    .drawBehind {
                        drawRect(Brush.radialGradient(listOf(prahar.deepB, prahar.deepA), center = Offset(size.width / 2, 0f), radius = size.height * 0.95f))
                    }
            ) {
                Box(Modifier.fillMaxWidth().height(440.dp)) {
                    TurningMandala(760.dp, Color.White.copy(alpha = 0.10f), periodMs = 180_000)
                }
                AnimatedContent(
                    targetState = when {
                        isTest -> 0
                        allDone -> 2
                        else -> 1
                    },
                    transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut(tween(250)) },
                    label = "stage"
                ) { s ->
                    when (s) {
                        0 -> TestScreen(prefs, ringingTest)
                        1 -> Medicines(prefs, doses, answered, ringing.isNotEmpty(), dueNow)
                        else -> Done(prefs)
                    }
                }
            }
        }
    }

    @Composable
    private fun Header(prefs: Prefs, doses: List<Dose>, speaking: Boolean) {
        var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
        LaunchedEffect(Unit) {
            while (true) {
                now = System.currentTimeMillis()
                delay(1000)
            }
        }
        val first = doses.firstOrNull()
        val minsLeft = if (first != null) ((first.at - now + 59_999) / 60_000).toInt() else 0
        val eyebrow = when {
            first == null -> "MEDICINE REMINDER"
            minsLeft > 0 -> "IN $minsLeft MIN · AT ${minuteLabel(first.minuteOfDay)}"
            minsLeft == 0 -> "IT IS TIME NOW"
            else -> "${-minsLeft} MIN LATE · WAS DUE ${minuteLabel(first.minuteOfDay)}"
        }
        val voiceId = first?.medicine?.voiceId ?: prefs.defaultVoiceId
        val voiceName = VoiceCatalog.byId(voiceId)?.name
            ?: prefs.recordedVoices.firstOrNull { it.id == voiceId }?.label
            ?: VoiceCatalog.fallback.name
        val p = LocalPrahar.current
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(eyebrow, style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 2.sp), color = p.glow, textAlign = TextAlign.Center)
            Spacer(Modifier.height(10.dp))
            // The time sits inside a jharokha – the carved window of an Indian palace.
            Box(
                Modifier
                    .widthIn(max = 340.dp)
                    .fillMaxWidth(0.88f)
                    .drawBehind { jharokha(p.glow.copy(alpha = 0.85f)) }
                    .padding(start = 18.dp, end = 18.dp, top = 54.dp, bottom = 18.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        if (first != null) minuteLabel(first.minuteOfDay) else "",
                        style = MaterialTheme.typography.displayMedium,
                        color = Color.White, maxLines = 1, softWrap = false
                    )
                    Text(
                        (first?.let { Slots.at(it.minuteOfDay, Slots.build(Slots.decode(prefs.slots))).name } ?: "").uppercase(),
                        style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.5.sp),
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
            if (speaking) {
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SoundBars(p.glow, small = true)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "$voiceName is speaking · ${TuneSynth.byId(prefs.tuneId).title}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.75f)
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    private fun Medicines(
        prefs: Prefs,
        doses: List<Dose>,
        answered: MutableMap<String, DoseStatus>,
        speaking: Boolean,
        dueNow: Boolean
    ) {
        val total = doses.size
        val done = doses.count { answered.containsKey(it.key) }
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Header(prefs, doses, speaking)
            if (total == 0) {
                Text("Nothing to answer right now.", style = MaterialTheme.typography.titleMedium, color = Color.White)
                BigButton("Close", onClick = { finish() }, modifier = Modifier.fillMaxWidth())
                return@Column
            }

            // Progress: one segment per medicine.
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    if (total == 1) "Your medicine" else "$total medicines – take them one by one",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White, textAlign = TextAlign.Center
                )
                // One bead per medicine, filling as each is answered.
                FlowRow(horizontalArrangement = Arrangement.spacedBy(9.dp, Alignment.CenterHorizontally), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    doses.forEach { d ->
                        val st = answered[d.key]
                        val c = when (st) {
                            DoseStatus.TAKEN -> StatusColors.TakenDark
                            DoseStatus.NOT_TAKEN -> StatusColors.NotTakenDark
                            else -> null
                        }
                        Box(
                            Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(c ?: Color.Transparent)
                                .border(2.dp, c ?: Color.White.copy(alpha = 0.55f), CircleShape)
                        )
                    }
                }
                Text(
                    when {
                        !dueNow -> "Medicine time is coming. Already taken it? Tap Taken."
                        done == 0 -> "Take each medicine, then tap Taken. The music stops when all are answered."
                        done < total -> "$done of $total done. ${total - done} still to go."
                        else -> "All done!"
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center
                )
            }

            doses.forEachIndexed { i, d ->
                AnswerCard(i + 1, total, d, answered[d.key]) { status, reason ->
                    Answers.record(this@ReminderActivity, d.key, status, reason)
                    answered[d.key] = status
                }
            }

            if (!dueNow && speaking) {
                BigOutlinedButton(
                    text = "Quiet until the next reminder",
                    onClick = { AlarmService.stop(this@ReminderActivity) },
                    icon = Icons.Rounded.NotificationsOff,
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (!dueNow && !speaking) {
                TextButton(onClick = { finish() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Close", style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.75f))
                }
            }
            if (prefs.caregiverPhone.isNotBlank()) {
                BigOutlinedButton(
                    text = "Call ${prefs.caregiverName.ifBlank { "family" }}",
                    onClick = { CaregiverAlert.callIntent(this@ReminderActivity)?.let { startActivity(it) } },
                    icon = Icons.Rounded.Call,
                    color = LocalPrahar.current.glow,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    private fun AnswerCard(number: Int, total: Int, d: Dose, current: DoseStatus?, onAnswer: (DoseStatus, String) -> Unit) {
        var askingReason by remember { mutableStateOf(false) }
        var picked by remember { mutableStateOf("") }
        var other by remember { mutableStateOf("") }
        val takenC = StatusColors.TakenDark
        val notC = StatusColors.NotTakenDark
        val m = d.medicine
        val shape = RoundedCornerShape(28.dp)
        val ringColor = when (current) {
            DoseStatus.TAKEN -> takenC
            DoseStatus.NOT_TAKEN -> notC
            else -> MedColors.of(m.colorIndex)
        }

        Column(
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(Color.White.copy(alpha = if (current == null) 0.08f else 0.04f))
                .border(1.dp, if (current == null) ringColor.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.14f), shape)
                .drawBehind {
                    // the colour rail on the left edge
                    val inset = 22.dp.toPx()
                    drawRoundRect(ringColor, topLeft = Offset(0f, inset), size = Size(4.dp.toPx(), size.height - inset * 2), cornerRadius = CornerRadius(4.dp.toPx()))
                }
                .padding(start = 20.dp, end = 16.dp, top = 16.dp, bottom = 16.dp)
                .animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(48.dp).border(4.dp, Color.White.copy(alpha = 0.12f), CircleShape).padding(4.dp).clip(CircleShape).background(ringColor),
                    contentAlignment = Alignment.Center
                ) {
                    if (current == null) {
                        Text("$number", style = MaterialTheme.typography.titleLarge, color = Color(0xFF14122E))
                    } else {
                        Icon(if (current == DoseStatus.TAKEN) Icons.Rounded.Check else Icons.Rounded.Close, null,
                            tint = Color(0xFF14122E), modifier = Modifier.size(24.dp))
                    }
                }
                Spacer(Modifier.width(12.dp))
                PillIcon(m.colorIndex, m.shape, size = 52.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    if (total > 1) Text("MEDICINE $number OF $total", style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.5.sp), color = Color.White.copy(alpha = 0.62f))
                    Text(m.name, style = MaterialTheme.typography.headlineMedium, color = Color.White)
                    val sub = listOf(m.dosage, m.food.takeIf { it != FoodRule.NONE }?.label ?: "").filter { it.isNotBlank() }.joinToString(" · ")
                    if (sub.isNotBlank()) Text(sub, style = MaterialTheme.typography.titleMedium, color = Color.White.copy(alpha = 0.75f))
                }
            }

            if (current != null) {
                Text(
                    if (current == DoseStatus.TAKEN) "Taken. Thank you!" else "Not taken – reason saved.",
                    style = MaterialTheme.typography.titleMedium,
                    color = ringColor
                )
                return@Column
            }
            BigButton(
                text = "Taken",
                onClick = { onAnswer(DoseStatus.TAKEN, "Taken") },
                icon = Icons.Rounded.Check,
                container = takenC,
                content = Color(0xFF00241B),
                height = 76.dp,
                modifier = Modifier.fillMaxWidth()
            )
            TextButton(onClick = { askingReason = !askingReason }, modifier = Modifier.fillMaxWidth()) {
                Text(
                    if (askingReason) "Hide" else "I can't take this one",
                    style = MaterialTheme.typography.labelLarge,
                    color = notC
                )
            }
            AnimatedVisibility(askingReason, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Why not? (needed)", style = MaterialTheme.typography.titleMedium, color = Color.White)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Reasons.notTaken.forEach { r ->
                            val on = picked == r
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(if (on) notC else Color.White.copy(alpha = 0.1f))
                                    .border(1.dp, if (on) notC else Color.White.copy(alpha = 0.2f), RoundedCornerShape(50))
                                    .clickable { picked = if (on) "" else r }
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                Text(r, style = MaterialTheme.typography.labelMedium, color = if (on) Color(0xFF2E1A00) else Color.White)
                            }
                        }
                    }
                    OutlinedTextField(
                        value = other,
                        onValueChange = { other = it },
                        label = { Text("Or write your own reason") },
                        minLines = 2,
                        textStyle = MaterialTheme.typography.bodyLarge,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = notC,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                            focusedLabelColor = notC,
                            unfocusedLabelColor = Color.White.copy(alpha = 0.6f),
                            cursorColor = notC
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    val reason = listOf(picked, other.trim()).filter { it.isNotBlank() }.joinToString(" – ")
                    BigButton(
                        text = "Save: not taken",
                        onClick = { onAnswer(DoseStatus.NOT_TAKEN, reason) },
                        enabled = reason.isNotBlank(),
                        container = notC,
                        content = Color(0xFF2E1A00),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    @Composable
    private fun TestScreen(prefs: Prefs, speaking: Boolean) {
        var heard by remember { mutableStateOf(false) }
        LaunchedEffect(speaking) { if (speaking) heard = true }
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
        ) {
            val p = LocalPrahar.current
            Text("TEST REMINDER", style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 2.sp), color = p.glow)
            Box(Modifier.size(260.dp), contentAlignment = Alignment.Center) {
                Pulse(p.glow)
                SoundBars(p.glow)
            }
            Text("Can you hear me clearly?", style = MaterialTheme.typography.headlineLarge, color = Color.White, textAlign = TextAlign.Center)
            Text(TuneSynth.byId(prefs.tuneId).title, style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.7f))
            BigButton(
                if (speaking || !heard) "Stop test" else "Close",
                onClick = { AlarmService.stop(this@ReminderActivity); finish() },
                container = LocalPrahar.current.glow, content = LocalPrahar.current.deepA,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    @Composable
    private fun Done(prefs: Prefs) {
        val streak = remember { Answers.streak(this) }
        LaunchedEffect(Unit) {
            delay(4500)
            finish()
        }
        val t = rememberInfiniteTransition(label = "done")
        val glow by t.animateFloat(0.85f, 1.05f, infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "glow")
        Column(
            Modifier.fillMaxSize().padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                Modifier
                    .size(150.dp * glow)
                    .clip(CircleShape)
                    .background(StatusColors.TakenDark.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.CheckCircle, null, tint = StatusColors.TakenDark, modifier = Modifier.size(84.dp))
            }
            Spacer(Modifier.height(28.dp))
            val name = prefs.userName
            Text(
                if (name.isBlank()) "Thank you!" else "Thank you, $name!",
                style = MaterialTheme.typography.displaySmall,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(12.dp))
            Text(
                if (streak >= 2) "All medicines taken for $streak days in a row. Wonderful!" else "All answered. Stay well – I will remind you of the next one.",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.75f),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(32.dp))
            BigOutlinedButton("Close", onClick = { finish() }, color = Color.White, modifier = Modifier.fillMaxWidth(0.6f))
        }
    }
}

/** Expanding rings – visible from across the room. */
@Composable
private fun Pulse(color: Color) {
    val t = rememberInfiniteTransition(label = "pulse")
    val p by t.animateFloat(0f, 1f, infiniteRepeatable(tween(2400, easing = LinearEasing)), label = "p")
    Canvas(Modifier.size(260.dp)) {
        val maxR = size.minDimension / 2
        for (i in 0 until 3) {
            val f = (p + i / 3f) % 1f
            drawCircle(
                color = color.copy(alpha = (1f - f) * 0.45f),
                radius = maxR * (0.3f + 0.7f * f),
                style = Stroke(width = 6.dp.toPx() * (1f - f) + 1f)
            )
        }
        drawCircle(color.copy(alpha = 0.12f), radius = maxR * 0.42f)
    }
}

/** Five little bars that bounce while the voice speaks. */
@Composable
private fun SoundBars(color: Color, small: Boolean = false) {
    val t = rememberInfiniteTransition(label = "bars")
    val phase by t.animateFloat(0f, 6.2832f, infiniteRepeatable(tween(1100, easing = LinearEasing)), label = "ph")
    val w = if (small) 22.dp else 64.dp
    val h = if (small) 16.dp else 48.dp
    Canvas(Modifier.size(w, h)) {
        val n = 5
        val gap = size.width / (n * 2 - 1)
        for (i in 0 until n) {
            val amp = 0.35f + 0.65f * ((kotlin.math.sin(phase + i * 1.1f) + 1f) / 2f)
            val bh = size.height * amp
            val x = gap * (i * 2) + gap / 2
            drawLine(
                color, Offset(x, (size.height - bh) / 2), Offset(x, (size.height + bh) / 2),
                strokeWidth = gap, cap = StrokeCap.Round
            )
        }
    }
}
