package com.ragini.medreminder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Medication
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.ragini.medreminder.ui.AppModel
import com.ragini.medreminder.ui.screens.EditorScreen
import com.ragini.medreminder.ui.screens.ImportDialogs
import com.ragini.medreminder.ui.screens.MedicinesScreen
import com.ragini.medreminder.ui.screens.OnboardingScreen
import com.ragini.medreminder.ui.screens.ReportsScreen
import com.ragini.medreminder.ui.screens.SettingsScreen
import com.ragini.medreminder.ui.screens.TodayScreen
import com.ragini.medreminder.ui.screens.VoicesScreen
import com.ragini.medreminder.ui.components.DockTab
import com.ragini.medreminder.ui.components.PraharDock
import com.ragini.medreminder.ui.theme.LocalSlots
import com.ragini.medreminder.ui.theme.Prahars
import com.ragini.medreminder.ui.theme.Slots
import com.ragini.medreminder.ui.theme.RaginiTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    private lateinit var model: AppModel

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        model = AppModel(applicationContext)
        model.refresh()
        handleOpenedFile(intent)
        setContent {
            // The living sky: colours follow the current prahar (re-checked with every refresh).
            val prahar = androidx.compose.runtime.remember(model.now / 60_000, model.sky) { Prahars.current(model.sky) }
            val slots = androidx.compose.runtime.remember(model.slots) { Slots.build(model.slots) }
            RaginiTheme(themeMode = model.themeMode, textScale = model.textScale, prahar = prahar) {
              androidx.compose.runtime.CompositionLocalProvider(LocalSlots provides slots) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    if (model.onboarded) AppShell(model) else OnboardingScreen(model)
                    ImportDialogs(model)
                }
              }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleOpenedFile(intent)
    }

    /** A Ragini medicine list opened from WhatsApp, e-mail, Files or Drive. */
    private fun handleOpenedFile(i: android.content.Intent?) {
        val uri: android.net.Uri? = when (i?.action) {
            android.content.Intent.ACTION_VIEW -> i.data
            android.content.Intent.ACTION_SEND ->
                @Suppress("DEPRECATION") (i.getParcelableExtra(android.content.Intent.EXTRA_STREAM) as? android.net.Uri)
            else -> null
        }
        if (uri != null) {
            model.readImport(uri)
            i?.action = null   // do not import again on rotation
        }
    }

    override fun onResume() {
        super.onResume()
        model.refresh()
    }

    override fun onDestroy() {
        model.release()
        super.onDestroy()
    }
}

private enum class Tab(val label: String, val icon: ImageVector) {
    TODAY("Today", Icons.Rounded.WbSunny),
    MEDICINES("Medicines", Icons.Rounded.Medication),
    VOICES("Voices", Icons.Rounded.RecordVoiceOver),
    REPORTS("Reports", Icons.Rounded.BarChart)
}

private val dockTabs = Tab.entries.map { DockTab(it.label, it.icon) }

/** Full-screen pages that sit on top of the tabs. editId: 0 = new medicine. */
private sealed interface Overlay {
    data object None : Overlay
    data object Settings : Overlay
    data class Editor(val editId: Long) : Overlay
}

@Composable
private fun AppShell(model: AppModel) {
    var tabIndex by rememberSaveable { mutableStateOf(0) }
    var overlay by androidx.compose.runtime.remember { mutableStateOf<Overlay>(Overlay.None) }
    val tab = Tab.entries[tabIndex]

    // Keeps the clock, countdowns and statuses fresh.
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            model.refresh()
        }
    }

    BackHandler(enabled = overlay != Overlay.None) { overlay = Overlay.None }
    BackHandler(enabled = overlay == Overlay.None && tab != Tab.TODAY) { tabIndex = 0 }

    AnimatedContent(
        targetState = overlay,
        transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(160)) },
        label = "overlay"
    ) { o ->
        when (o) {
            is Overlay.Editor -> EditorScreen(model, o.editId, onClose = { overlay = Overlay.None })
            Overlay.Settings -> SettingsScreen(model, onClose = { overlay = Overlay.None })
            Overlay.None -> Box(Modifier.fillMaxSize()) {
                val onSettings = { overlay = Overlay.Settings }
                when (tab) {
                    Tab.TODAY -> TodayScreen(
                        model,
                        onAdd = { overlay = Overlay.Editor(0L) },
                        onSettings = onSettings,
                        onEdit = { overlay = Overlay.Editor(it) }
                    )
                    Tab.MEDICINES -> MedicinesScreen(
                        model,
                        onAdd = { overlay = Overlay.Editor(0L) },
                        onEdit = { overlay = Overlay.Editor(it) },
                        onSettings = onSettings
                    )
                    Tab.VOICES -> VoicesScreen(model, onSettings = onSettings)
                    Tab.REPORTS -> ReportsScreen(model, onSettings = onSettings)
                }
                PraharDock(
                    tabs = dockTabs,
                    selected = tabIndex,
                    onSelect = { tabIndex = it },
                    onAdd = { overlay = Overlay.Editor(0L) },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}
