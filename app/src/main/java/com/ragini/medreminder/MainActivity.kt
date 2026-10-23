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
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.ragini.medreminder.ui.AppModel
import com.ragini.medreminder.ui.screens.EditorScreen
import com.ragini.medreminder.ui.screens.MedicinesScreen
import com.ragini.medreminder.ui.screens.OnboardingScreen
import com.ragini.medreminder.ui.screens.ReportsScreen
import com.ragini.medreminder.ui.screens.SettingsScreen
import com.ragini.medreminder.ui.screens.TodayScreen
import com.ragini.medreminder.ui.screens.VoicesScreen
import com.ragini.medreminder.ui.theme.RaginiTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    private lateinit var model: AppModel

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        model = AppModel(applicationContext)
        model.refresh()
        setContent {
            RaginiTheme(themeMode = model.themeMode, textScale = model.textScale) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    if (model.onboarded) AppShell(model) else OnboardingScreen(model)
                }
            }
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
            Overlay.None -> Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                bottomBar = {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 0.dp
                    ) {
                        Tab.entries.forEachIndexed { i, t ->
                            NavigationBarItem(
                                selected = i == tabIndex,
                                onClick = { tabIndex = i },
                                icon = { Icon(t.icon, null, Modifier.size(28.dp)) },
                                label = { Text(t.label, style = MaterialTheme.typography.labelMedium) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.onSecondary,
                                    indicatorColor = MaterialTheme.colorScheme.secondary,
                                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
            ) { pad ->
                Box(Modifier.padding(pad)) {
                    when (tab) {
                        Tab.TODAY -> TodayScreen(
                            model,
                            onAdd = { overlay = Overlay.Editor(0L) },
                            onSettings = { overlay = Overlay.Settings },
                            onEdit = { overlay = Overlay.Editor(it) }
                        )
                        Tab.MEDICINES -> MedicinesScreen(
                            model,
                            onAdd = { overlay = Overlay.Editor(0L) },
                            onEdit = { overlay = Overlay.Editor(it) }
                        )
                        Tab.VOICES -> VoicesScreen(model)
                        Tab.REPORTS -> ReportsScreen(model)
                    }
                }
            }
        }
    }
}
