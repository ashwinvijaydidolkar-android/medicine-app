package com.ragini.medreminder.alarm

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.ragini.medreminder.MainActivity
import com.ragini.medreminder.R
import com.ragini.medreminder.data.Db
import com.ragini.medreminder.data.Prefs
import com.ragini.medreminder.data.minuteLabel
import com.ragini.medreminder.schedule.RingKind
import java.time.LocalDate
import kotlin.math.abs

object Notifs {
    const val CH_REMIND = "reminders"
    const val CH_AWAIT = "awaiting"
    const val CH_INFO = "info"
    const val ID_RINGING = 1001
    private const val ID_STOCK = 1002

    fun createChannels(ctx: Context) {
        if (Build.VERSION.SDK_INT < 26) return
        val nm = ctx.getSystemService(NotificationManager::class.java)
        val remind = NotificationChannel(CH_REMIND, "Medicine reminders", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Spoken reminders before and at medicine time"
            setSound(null, null) // the app speaks and plays its own chime on the alarm channel
            enableVibration(false) // the app vibrates by itself
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            setBypassDnd(true)
        }
        val await = NotificationChannel(CH_AWAIT, "Waiting for your answer", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Asks whether a dose was taken"
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        val info = NotificationChannel(CH_INFO, "Refills and tips", NotificationManager.IMPORTANCE_DEFAULT)
        nm.createNotificationChannels(listOf(remind, await, info))
    }

    fun enabled(ctx: Context) = NotificationManagerCompat.from(ctx).areNotificationsEnabled()

    fun canFullScreen(ctx: Context): Boolean =
        if (Build.VERSION.SDK_INT >= 34) {
            ctx.getSystemService(NotificationManager::class.java).canUseFullScreenIntent()
        } else true

    private fun permitted(ctx: Context) =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun awaitId(key: String) = 3000 + abs(key.hashCode() % 100_000)

    /** The notification shown while a reminder is speaking. It opens the full-screen reminder. */
    fun ringing(ctx: Context, items: List<RingItem>, isTest: Boolean, kick: Boolean = false): Notification {
        val keys = items.map { it.dose.key }.toTypedArray()
        val full = PendingIntent.getActivity(
            ctx, 200, ReminderActivity.intent(ctx, keys, isTest, kick = kick),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stop = PendingIntent.getService(
            ctx, 201, Intent(ctx, AlarmService::class.java).setAction(AlarmService.ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val names = if (items.size > 2) "${items.size} medicines" else items.joinToString(", ") { it.dose.medicine.name }
        val first = items.firstOrNull()
        val title = when {
            isTest -> "Test reminder"
            first == null -> "Medicine reminder"
            first.kind == RingKind.DUE -> "Time for $names"
            first.kind == RingKind.PRE -> "$names in ${first.minutes} min"
            else -> "Please take $names"
        }
        val text = if (first != null) "Due at ${minuteLabel(first.dose.minuteOfDay)} · tap to answer" else "Ragini is speaking"
        val b = NotificationCompat.Builder(ctx, CH_REMIND)
            .setSmallIcon(R.drawable.ic_stat_ragini)
            .setContentTitle(title)
            .setContentText(text)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setSilent(true)
            .setColor(0xFF2E2A6B.toInt())
            .setFullScreenIntent(full, true)
            .setContentIntent(full)
        // A medicine that is due can only be silenced by answering it on the reminder screen.
        val urgent = items.any { it.kind != RingKind.PRE }
        if (isTest || !urgent) b.addAction(0, "Quiet until next reminder", stop)
        else b.addAction(0, "Open – tap Taken", full)
        return b.build()
    }

    /** After the voice stops: a notification asking "Did you take it?" until it is answered. */
    @SuppressLint("MissingPermission")
    fun awaiting(ctx: Context, items: List<RingItem>) {
        if (!permitted(ctx)) return
        val nm = NotificationManagerCompat.from(ctx)
        val db = Db.get(ctx)
        for (it in items) {
            val key = it.dose.key
            if (db.response(key) != null) continue
            val id = awaitId(key)
            val open = PendingIntent.getActivity(
                ctx, id, ReminderActivity.intent(ctx, arrayOf(key), false, askFirst = true),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val taken = PendingIntent.getBroadcast(
                ctx, id,
                Intent(ctx, ActionReceiver::class.java).setAction(ActionReceiver.ACTION_TAKEN).putExtra("key", key),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val n = NotificationCompat.Builder(ctx, CH_AWAIT)
                .setSmallIcon(R.drawable.ic_stat_ragini)
                .setContentTitle("Did you take ${it.dose.medicine.name}?")
                .setContentText("Due at ${minuteLabel(it.dose.minuteOfDay)} · tap to answer")
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setColor(0xFF2E2A6B.toInt())
                .setContentIntent(open)
                .setAutoCancel(true)
                .addAction(0, "✓ Taken", taken)
                .addAction(0, "Not taken…", open)
                .build()
            nm.notify(id, n)
        }
    }

    fun cancelAwaiting(ctx: Context, keys: List<String>) {
        val nm = NotificationManagerCompat.from(ctx)
        keys.forEach { nm.cancel(awaitId(it)) }
    }

    @SuppressLint("MissingPermission")
    fun lowStockCheck(ctx: Context) {
        if (!permitted(ctx)) return
        val prefs = Prefs(ctx)
        val today = LocalDate.now().toString()
        if (prefs.lowStockSpokenDay == today) return
        val low = Db.get(ctx).medicines().filter { it.active && it.lowStock }
        if (low.isEmpty()) return
        prefs.lowStockSpokenDay = today
        val open = PendingIntent.getActivity(
            ctx, 300, Intent(ctx, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val n = NotificationCompat.Builder(ctx, CH_INFO)
            .setSmallIcon(R.drawable.ic_stat_ragini)
            .setContentTitle("Time to refill")
            .setContentText(low.joinToString(", ") { "${it.name} (${it.stock} left)" })
            .setColor(0xFFF2A93B.toInt())
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(ctx).notify(ID_STOCK, n)
    }
}
