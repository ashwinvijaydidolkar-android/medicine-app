package com.ragini.medreminder.ui.screens

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import com.ragini.medreminder.report.Period
import com.ragini.medreminder.report.ReportCard
import com.ragini.medreminder.report.Reports
import com.ragini.medreminder.ui.AppModel
import com.ragini.medreminder.ui.components.SkyHeader
import com.ragini.medreminder.ui.components.bleed
import com.ragini.medreminder.ui.theme.LocalIsDark
import com.ragini.medreminder.ui.theme.LocalPrahar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.json.JSONTokener
import java.io.File

/**
 * Reports: the Ragini report card. It is drawn by assets/report/report.js – the same file the Windows
 * app uses – so the screen, the PDF, the spreadsheet and the text message always match.
 */
@Composable
fun ReportsScreen(model: AppModel, onSettings: () -> Unit = {}) {
    val ctx = LocalContext.current
    val dark = LocalIsDark.current
    var period by rememberSaveable { mutableStateOf(Period.WEEKLY) }
    val data by produceState<Pair<Period, String>?>(null, period, model.version, model.slots, model.userName) {
        value = withContext(Dispatchers.IO) {
            val r = Reports.build(ctx, period)
            period to ReportCard.json(r, model.userName, model.slots)
        }
    }
    var heightPx by remember { mutableIntStateOf(0) }
    var web by remember { mutableStateOf<WebView?>(null) }
    var note by remember { mutableStateOf<String?>(null) }
    val p = LocalPrahar.current

    LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 0.dp, bottom = 150.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SkyHeader(eyebrow = "Ragini medicine report", title = "Reports", onSettings = onSettings, modifier = Modifier.bleed(20.dp)) {
                Spacer(Modifier.height(14.dp))
                // Three equal ways to send the report – symmetric under the title.
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    ExportPill("PDF", "print or send", Modifier.weight(1f)) {
                        val j = data?.second ?: return@ExportPill
                        note = if (printPdf(ctx, j, ReportCard.title(period))) null else "Printing is not available on this phone."
                    }
                    ExportPill("Spreadsheet", "Excel / CSV", Modifier.weight(1f)) {
                        web?.evaluateJavascript("csv()") { v -> note = shareFile(ctx, decode(v), "Ragini ${ReportCard.title(period)}.csv", "text/csv") }
                    }
                    ExportPill("Text", "for WhatsApp", Modifier.weight(1f)) {
                        web?.evaluateJavascript("text()") { v ->
                            val send = Intent(Intent.ACTION_SEND).setType("text/plain")
                                .putExtra(Intent.EXTRA_SUBJECT, "Ragini – ${ReportCard.title(period)}")
                                .putExtra(Intent.EXTRA_TEXT, decode(v))
                            ctx.startActivity(Intent.createChooser(send, "Send report"))
                        }
                    }
                }
            }
        }
        item { PeriodGrid(period) { period = it; heightPx = 0 } }
        note?.let { n -> item { Text(n, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary) } }
        item {
            val d = data
            Box(Modifier.fillMaxWidth().heightIn(min = 240.dp)) {
                AndroidView(
                    factory = { c -> reportWebView(c) { h -> heightPx = h }.also { web = it } },
                    update = { w ->
                        val json = d?.second ?: return@AndroidView
                        val tag = "${d.first}|${json.hashCode()}|$dark"
                        if (w.tag != tag) {
                            w.tag = tag
                            w.onReady { it.evaluateJavascript("show($json, 'screen', $dark)", null) }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .bleed(20.dp)
                        .height(if (heightPx > 0) (heightPx).dp else 240.dp)
                )
                if (d == null || d.first != period || heightPx == 0) {
                    CircularProgressIndicator(Modifier.align(Alignment.Center).padding(top = 40.dp), color = p.accent)
                }
            }
        }
    }
}

@Composable
private fun ExportPill(title: String, sub: String, modifier: Modifier, onClick: () -> Unit) {
    val p = LocalPrahar.current
    Column(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(p.skyInk.copy(alpha = 0.16f))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, style = MaterialTheme.typography.labelLarge, color = p.skyInk, maxLines = 1)
        Text(sub, style = MaterialTheme.typography.labelSmall, color = p.skyInk.copy(alpha = 0.8f), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

// ------------------------------------------------------------------ the web view that draws the card

private class Bridge(val onHeight: (Int) -> Unit) {
    @JavascriptInterface fun height(cssPx: Int) { onHeight(cssPx) }
}

@SuppressLint("SetJavaScriptEnabled")
private fun reportWebView(c: Context, onHeight: (Int) -> Unit): WebView = WebView(c).apply {
    settings.javaScriptEnabled = true
    settings.allowFileAccess = true
    setBackgroundColor(android.graphics.Color.TRANSPARENT)
    isVerticalScrollBarEnabled = false
    val main = android.os.Handler(android.os.Looper.getMainLooper())
    addJavascriptInterface(Bridge { h -> main.post { onHeight(h) } }, "Host")
    webViewClient = object : WebViewClient() {
        override fun onPageFinished(view: WebView, url: String?) {
            ready = true
            pending.forEach { it(view) }; pending.clear()
        }
    }
    loadUrl("file:///android_asset/report/report.html")
}

// Calls made before the page has loaded wait until it is ready.
private val readyFlags = java.util.WeakHashMap<WebView, Boolean>()
private val queues = java.util.WeakHashMap<WebView, MutableList<(WebView) -> Unit>>()
private var WebView.ready: Boolean
    get() = readyFlags[this] == true
    set(v) { readyFlags[this] = v }
private val WebView.pending: MutableList<(WebView) -> Unit> get() = queues.getOrPut(this) { ArrayList() }
private fun WebView.onReady(block: (WebView) -> Unit) { if (ready) block(this) else pending += block }

/** evaluateJavascript returns a JSON-encoded string; this turns it back into plain text. */
private fun decode(v: String?): String = try { JSONTokener(v ?: "\"\"").nextValue() as? String ?: "" } catch (e: Exception) { "" }

// Keeps the print web view alive until Android has finished with it.
private var printView: WebView? = null

/** The PDF: the card in its A4 print layout, through Android's print screen ("Save as PDF" or a printer). */
@SuppressLint("SetJavaScriptEnabled")
private fun printPdf(ctx: Context, json: String, title: String): Boolean {
    val activity = ctx.findActivity() ?: return false
    val pm = activity.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return false
    val w = WebView(activity)
    w.settings.javaScriptEnabled = true
    w.settings.allowFileAccess = true
    w.webViewClient = object : WebViewClient() {
        override fun onPageFinished(view: WebView, url: String?) {
            view.evaluateJavascript("show($json, 'print', false)") {
                // give the fonts a moment, then hand the page to Android's print screen
                view.postDelayed({
                    val name = "Ragini $title"
                    pm.print(name, view.createPrintDocumentAdapter(name),
                        PrintAttributes.Builder().setMediaSize(PrintAttributes.MediaSize.ISO_A4).setColorMode(PrintAttributes.COLOR_MODE_COLOR).build())
                }, 400)
            }
        }
    }
    w.loadUrl("file:///android_asset/report/report.html")
    printView = w
    return true
}

/** Writes the file to the share folder and opens the share sheet (WhatsApp, e-mail, Drive, Files …). */
private fun shareFile(ctx: Context, text: String, name: String, mime: String): String? = try {
    val dir = File(ctx.cacheDir, "share").apply { mkdirs() }
    val f = File(dir, name.replace(Regex("[\\\\/:*?\"<>|']"), ""))
    f.writeText(text, Charsets.UTF_8)
    val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.files", f)
    val send = Intent(Intent.ACTION_SEND).setType(mime).putExtra(Intent.EXTRA_STREAM, uri)
        .putExtra(Intent.EXTRA_SUBJECT, name.removeSuffix(".csv")).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    ctx.startActivity(Intent.createChooser(send, "Save or send the spreadsheet"))
    null
} catch (e: Exception) { "The spreadsheet could not be made. Please try again." }

private fun Context.findActivity(): Activity? {
    var c: Context? = this
    while (c is ContextWrapper) { if (c is Activity) return c; c = c.baseContext }
    return null
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
