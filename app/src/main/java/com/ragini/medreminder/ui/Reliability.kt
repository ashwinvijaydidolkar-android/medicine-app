package com.ragini.medreminder.ui

import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.speech.tts.TextToSpeech
import com.ragini.medreminder.alarm.Notifs
import com.ragini.medreminder.data.Prefs
import com.ragini.medreminder.schedule.AlarmScheduler

/** One item in the "Will it ring on standby?" checklist. */
data class Check(
    val id: String,
    val title: String,
    val why: String,
    val ok: Boolean,
    val fixLabel: String,
    val fix: ((Context) -> Unit)?
)

/**
 * Android phones – especially Xiaomi, Vivo, Oppo, Realme and Samsung – can silence apps to save
 * battery. This checklist finds every setting that could stop a reminder and opens the right page.
 */
object Reliability {

    fun checks(ctx: Context): List<Check> {
        val pkg = ctx.packageName
        val out = ArrayList<Check>()

        val channelOk = if (Build.VERSION.SDK_INT >= 26) {
            val ch = ctx.getSystemService(NotificationManager::class.java).getNotificationChannel(Notifs.CH_REMIND)
            ch == null || ch.importance >= NotificationManager.IMPORTANCE_HIGH
        } else true
        out += Check(
            "notif", "Notifications allowed",
            "Needed to show the reminder on the lock screen.",
            Notifs.enabled(ctx) && channelOk, "Allow",
            { c -> open(c, notificationSettings(c)) }
        )

        out += Check(
            "exact", "Alarms & reminders",
            "Lets every reminder ring at the exact minute, even on standby.",
            AlarmScheduler.canExact(ctx), "Allow",
            exactFix.takeIf { Build.VERSION.SDK_INT >= 31 }
        )

        out += Check(
            "full", "Full-screen reminder",
            "Turns the screen on and shows the big reminder over the lock screen.",
            Notifs.canFullScreen(ctx), "Allow",
            fullScreenFix.takeIf { Build.VERSION.SDK_INT >= 34 }
        )

        val pm = ctx.getSystemService(Context.POWER_SERVICE) as PowerManager
        out += Check(
            "battery", "No battery restriction",
            "Stops the phone from putting Ragini to sleep.",
            pm.isIgnoringBatteryOptimizations(pkg), "Remove limit",
            { c ->
                val asked = open(c, Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$pkg")))
                if (!asked) open(c, Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            }
        )

        val am = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val loud = Prefs(ctx).forceMaxVolume ||
            am.getStreamVolume(AudioManager.STREAM_ALARM) * 2 >= am.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        out += Check(
            "volume", "Loud alarm volume",
            "Reminders play on the alarm channel – heard even on silent.",
            loud, "Turn up",
            { c ->
                try {
                    val m = c.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                    m.setStreamVolume(AudioManager.STREAM_ALARM, m.getStreamMaxVolume(AudioManager.STREAM_ALARM), AudioManager.FLAG_SHOW_UI)
                } catch (_: Exception) {
                }
            }
        )

        val hasTts = ctx.packageManager.queryIntentServices(Intent(TextToSpeech.Engine.INTENT_ACTION_TTS_SERVICE), 0).isNotEmpty()
        out += Check(
            "tts", "Speech engine installed",
            "The voices come from the phone's text-to-speech engine.",
            hasTts, "Install",
            { c -> open(c, Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.google.android.tts"))) }
        )

        if (needsAutostart()) {
            out += Check(
                "autostart", "Auto-start allowed (${Build.MANUFACTURER})",
                "This phone brand blocks apps after a restart unless auto-start is on. Please switch it on once.",
                false, "Open",
                { c -> openAutostart(c) }
            )
        }
        return out
    }

    private val exactFix: (Context) -> Unit = { c ->
        if (Build.VERSION.SDK_INT >= 31) {
            open(c, Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${c.packageName}")))
        }
    }

    private val fullScreenFix: (Context) -> Unit = { c ->
        if (Build.VERSION.SDK_INT >= 34) {
            open(c, Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, Uri.parse("package:${c.packageName}")))
        }
    }

    fun allOk(ctx: Context) = checks(ctx).filter { it.id != "autostart" }.all { it.ok }

    private fun needsAutostart(): Boolean {
        val m = Build.MANUFACTURER.lowercase()
        return listOf("xiaomi", "redmi", "poco", "oppo", "realme", "vivo", "iqoo", "huawei", "honor", "asus", "letv", "oneplus")
            .any { m.contains(it) }
    }

    fun notificationSettings(ctx: Context): Intent =
        if (Build.VERSION.SDK_INT >= 26) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName)
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${ctx.packageName}"))
        }

    fun openTtsSettings(ctx: Context) {
        if (!open(ctx, Intent("com.android.settings.TTS_SETTINGS"))) open(ctx, Intent(Settings.ACTION_SETTINGS))
    }

    private fun openAutostart(ctx: Context) {
        val candidates = listOf(
            ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"),
            ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity"),
            ComponentName("com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity"),
            ComponentName("com.oplus.safecenter", "com.oplus.safecenter.permission.startup.StartupAppListActivity"),
            ComponentName("com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity"),
            ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"),
            ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity"),
            ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"),
            ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity"),
            ComponentName("com.asus.mobilemanager", "com.asus.mobilemanager.entry.FunctionActivity"),
            ComponentName("com.oneplus.security", "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity")
        )
        for (cn in candidates) {
            if (open(ctx, Intent().setComponent(cn))) return
        }
        open(ctx, Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${ctx.packageName}")))
    }

    /** Starts [i] and returns false if this phone has no such screen. */
    fun open(ctx: Context, i: Intent): Boolean = try {
        ctx.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (e: Exception) {
        false
    }
}
