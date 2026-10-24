@file:OptIn(ExperimentalLayoutApi::class)

package com.ragini.medreminder.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ragini.medreminder.data.Dose
import com.ragini.medreminder.data.DoseStatus
import com.ragini.medreminder.data.FoodRule
import com.ragini.medreminder.data.Reasons
import com.ragini.medreminder.data.minuteLabel
import com.ragini.medreminder.ui.AppModel
import com.ragini.medreminder.ui.components.BigButton
import com.ragini.medreminder.ui.components.BigOutlinedButton
import com.ragini.medreminder.ui.components.PillIcon
import com.ragini.medreminder.ui.theme.StatusColors

/**
 * Right after medicines are added (new install, imported list or typed in), Ragini asks about the doses
 * that were already due earlier today: "Did you take these?" Each answer counts in attendance exactly
 * like an answer given to a reminder. Unanswered ones are simply left out of today's register.
 */
@Composable
fun EarlierDosesDialog(model: AppModel) {
    val doses = model.earlierDoses
    if (doses.isEmpty() || model.pendingImport != null || model.showWhatsNew) return
    val answers = remember(doses) { mutableStateMapOf<String, Pair<DoseStatus, String>>() }
    var asking by remember(doses) { mutableStateOf<String?>(null) }
    val open = doses.filter { it.key !in answers }

    Dialog(onDismissRequest = { model.closeEarlier() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(shape = RoundedCornerShape(32.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.padding(14.dp)) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                model.earlierIntro?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                }
                Text("Earlier today", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                Text(
                    if (doses.size == 1) "This medicine was due before it was added to Ragini. Did you take it?"
                    else "These ${doses.size} medicines were due before they were added to Ragini. Did you take them?",
                    style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
                )
                Column(
                    Modifier.fillMaxWidth().heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    doses.forEach { d ->
                        EarlierRow(
                            d = d,
                            answer = answers[d.key],
                            asking = asking == d.key,
                            onTaken = { model.answerEarlier(d, DoseStatus.TAKEN, "Taken"); answers[d.key] = DoseStatus.TAKEN to "Taken"; asking = null },
                            onNotTaken = { asking = if (asking == d.key) null else d.key },
                            onReason = { r -> model.answerEarlier(d, DoseStatus.NOT_TAKEN, r); answers[d.key] = DoseStatus.NOT_TAKEN to r; asking = null }
                        )
                    }
                }
                if (open.size > 1) {
                    BigButton("I took all of them", onClick = {
                        open.forEach { d -> model.answerEarlier(d, DoseStatus.TAKEN, "Taken"); answers[d.key] = DoseStatus.TAKEN to "Taken" }
                    }, icon = Icons.Rounded.Check, container = StatusColors.of(DoseStatus.TAKEN), modifier = Modifier.fillMaxWidth())
                }
                if (open.isEmpty()) {
                    BigButton("Done", onClick = { model.closeEarlier() }, modifier = Modifier.fillMaxWidth())
                } else {
                    BigOutlinedButton("I don't remember – skip", onClick = { model.closeEarlier() }, modifier = Modifier.fillMaxWidth())
                    Text(
                        "Skipped medicines are not counted in today's attendance.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun EarlierRow(
    d: Dose, answer: Pair<DoseStatus, String>?, asking: Boolean,
    onTaken: () -> Unit, onNotTaken: () -> Unit, onReason: (String) -> Unit
) {
    val m = d.medicine
    val taken = StatusColors.of(DoseStatus.TAKEN)
    val not = StatusColors.of(DoseStatus.NOT_TAKEN)
    val shape = RoundedCornerShape(22.dp)
    Column(
        Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PillIcon(m.colorIndex, m.shape, size = 44.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(m.name, style = MaterialTheme.typography.titleMedium)
                val sub = listOf(minuteLabel(d.minuteOfDay), m.dosage, m.food.takeIf { it != FoodRule.NONE }?.label ?: "").filter { it.isNotBlank() }.joinToString(" · ")
                Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        when {
            answer != null -> Text(
                if (answer.first == DoseStatus.TAKEN) "✓ Taken – saved" else "Not taken – ${answer.second}",
                style = MaterialTheme.typography.titleSmall, color = if (answer.first == DoseStatus.TAKEN) taken else not
            )
            else -> {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    BigButton("Taken", onClick = onTaken, icon = Icons.Rounded.Check, container = taken, height = 56.dp, modifier = Modifier.weight(1f))
                    BigOutlinedButton("Not taken", onClick = onNotTaken, icon = Icons.Rounded.Close, color = not, height = 56.dp, modifier = Modifier.weight(1f))
                }
                if (asking) {
                    Text("Why not? Choose a reason", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Reasons.notTaken.forEach { r ->
                            Text(
                                r,
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier
                                    .border(1.dp, not.copy(alpha = 0.6f), RoundedCornerShape(50))
                                    .background(not.copy(alpha = 0.08f), RoundedCornerShape(50))
                                    .clickable { onReason(r) }
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
