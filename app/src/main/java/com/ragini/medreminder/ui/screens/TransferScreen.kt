package com.ragini.medreminder.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ragini.medreminder.data.Transfer
import com.ragini.medreminder.data.minuteLabel
import com.ragini.medreminder.ui.AppModel
import com.ragini.medreminder.ui.components.BigButton
import com.ragini.medreminder.ui.components.BigOutlinedButton
import com.ragini.medreminder.ui.components.SoftCard

/**
 * "Move medicines to another device": send or save the whole medicine list as one small file,
 * and load such a file on another phone or on the Windows app.
 */
@Composable
fun MoveListCard(model: AppModel) {
    val ctx = LocalContext.current
    var note by remember { mutableStateOf<String?>(null) }
    val save = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) note = if (model.exportTo(uri)) "Saved. Open this file in Ragini on the other device." else "The file could not be saved. Please try another place."
    }
    val open = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) model.readImport(uri)
    }
    SoftCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Move medicines to another device", style = MaterialTheme.typography.titleLarge)
            Text(
                "Put all your medicines, their times and your times of day into one small file. Open it in Ragini on a new phone, on a family member's phone or on the Windows app. Your attendance history stays on this phone.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            BigButton("Send to another phone", onClick = {
                runCatching {
                    val uri = model.exportForSharing()
                    val send = Intent(Intent.ACTION_SEND)
                        .setType("application/json")
                        .putExtra(Intent.EXTRA_STREAM, uri)
                        .putExtra(Intent.EXTRA_SUBJECT, "My Ragini medicine list")
                        .putExtra(Intent.EXTRA_TEXT, "My Ragini medicine list. On the other phone, open this file with Ragini.")
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    ctx.startActivity(Intent.createChooser(send, "Send medicine list"))
                }.onFailure { note = "Sending did not work. Use \"Save as a file\" instead." }
            }, icon = Icons.Rounded.Send, modifier = Modifier.fillMaxWidth())
            BigOutlinedButton("Save as a file", onClick = { save.launch(Transfer.fileName()) }, icon = Icons.Rounded.Save, modifier = Modifier.fillMaxWidth())
            BigOutlinedButton("Load a list from a file", onClick = { open.launch(Transfer.openTypes) }, icon = Icons.Rounded.FileOpen, modifier = Modifier.fillMaxWidth())
            note?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary) }
        }
    }
}

/** Shown over any screen when a list file was opened: the user chooses Add or Replace. */
@Composable
fun ImportDialogs(model: AppModel) {
    var result by remember { mutableStateOf<Transfer.Result?>(null) }
    var confirmReplace by remember { mutableStateOf(false) }
    var useTimes by remember { mutableStateOf(true) }

    model.importProblem?.let { msg ->
        Sheet(onDismiss = { model.importProblem = null }) {
            Text("Could not open the list", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
            Text(msg, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
            BigButton("OK", onClick = { model.importProblem = null }, modifier = Modifier.fillMaxWidth())
        }
    }

    val p = model.pendingImport
    if (p != null) {
        Sheet(onDismiss = { model.pendingImport = null; confirmReplace = false }) {
            Text("Medicine list found", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
            Text(
                "${p.meds.size} ${if (p.meds.size == 1) "medicine" else "medicines"} from ${p.from}" + if (p.exportedOn.isNotBlank()) ", saved on ${p.exportedOn}." else ".",
                style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center
            )
            Column(
                Modifier.fillMaxWidth().heightIn(max = 240.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                p.meds.forEach { m ->
                    Text(
                        "• ${m.name}" + (m.dosage.takeIf { it.isNotBlank() }?.let { " · $it" } ?: "") + " – " + m.times.joinToString(", ") { minuteLabel(it) },
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            if (p.unreadable > 0) Text(
                "${p.unreadable} entries had no name or time and will be left out.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (p.slots != null) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Checkbox(checked = useTimes, onCheckedChange = { useTimes = it })
                    Spacer(Modifier.width(4.dp))
                    Text("Also use their times of day (" + p.slots.joinToString(", ") { it.first } + ")", style = MaterialTheme.typography.bodyMedium)
                }
            }
            BigButton("Add to my medicines", onClick = { result = model.applyImport(replace = false, useTimesOfDay = useTimes) }, modifier = Modifier.fillMaxWidth())
            BigOutlinedButton(
                if (confirmReplace) "Tap again: remove my list and use this one" else "Replace my whole list",
                onClick = {
                    if (!confirmReplace) confirmReplace = true
                    else { result = model.applyImport(replace = true, useTimesOfDay = useTimes); confirmReplace = false }
                },
                color = MaterialTheme.colorScheme.error, modifier = Modifier.fillMaxWidth()
            )
            BigOutlinedButton("Cancel", onClick = { model.pendingImport = null; confirmReplace = false }, modifier = Modifier.fillMaxWidth())
        }
    }

    result?.let { r ->
        Sheet(onDismiss = { result = null }) {
            Text("Done", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
            Text(
                buildString {
                    append("${r.added} ${if (r.added == 1) "medicine was" else "medicines were"} added.")
                    if (r.alreadyThere > 0) append(" ${r.alreadyThere} ${if (r.alreadyThere == 1) "was" else "were"} already on this phone.")
                    if (r.removed > 0) append(" The old list of ${r.removed} was removed.")
                    append(" Reminders are set.")
                },
                style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center
            )
            BigButton("OK", onClick = { result = null }, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun Sheet(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(shape = RoundedCornerShape(32.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.padding(16.dp)) {
            Column(
                Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) { content() }
        }
    }
}
