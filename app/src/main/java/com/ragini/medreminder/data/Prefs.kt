package com.ragini.medreminder.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/** All user settings, stored in SharedPreferences. */
class Prefs(context: Context) {
    private val sp: SharedPreferences =
        context.applicationContext.getSharedPreferences("ragini_prefs", Context.MODE_PRIVATE)

    var onboarded: Boolean
        get() = sp.getBoolean("onboarded", false)
        set(v) = sp.edit().putBoolean("onboarded", v).apply()

    /** How the voice addresses the user, e.g. "Ragini", "Aai", "Maa". */
    var userName: String
        get() = sp.getString("userName", "") ?: ""
        set(v) = sp.edit().putString("userName", v.trim()).apply()

    var defaultVoiceId: String
        get() = sp.getString("defaultVoiceId", "en-IN-gentle") ?: "en-IN-gentle"
        set(v) = sp.edit().putString("defaultVoiceId", v).apply()

    /** Reminders start this many minutes before the dose (default 19). */
    var leadMinutes: Int
        get() = sp.getInt("leadMinutes", 19)
        set(v) = sp.edit().putInt("leadMinutes", v.coerceIn(0, 120)).apply()

    /** Gap between reminders, in minutes (default 2). */
    var gapMinutes: Int
        get() = sp.getInt("gapMinutes", 2)
        set(v) = sp.edit().putInt("gapMinutes", v.coerceIn(1, 30)).apply()

    /** Extra reminders after the dose time if there is still no answer. */
    var followUps: Int
        get() = sp.getInt("followUps", 2)
        set(v) = sp.edit().putInt("followUps", v.coerceIn(0, 6)).apply()

    var followGapMinutes: Int
        get() = sp.getInt("followGap", 5)
        set(v) = sp.edit().putInt("followGap", v.coerceIn(1, 30)).apply()

    /** How many times each reminder message is spoken. */
    var repeats: Int
        get() = sp.getInt("repeats", 2)
        set(v) = sp.edit().putInt("repeats", v.coerceIn(1, 5)).apply()

    /** How long the main (due-time) alarm keeps speaking if nobody closes it. */
    var dueRingSeconds: Int
        get() = sp.getInt("dueRingSeconds", 60)
        set(v) = sp.edit().putInt("dueRingSeconds", v.coerceIn(15, 300)).apply()

    var forceMaxVolume: Boolean
        get() = sp.getBoolean("forceMaxVolume", true)
        set(v) = sp.edit().putBoolean("forceMaxVolume", v).apply()

    var vibrate: Boolean
        get() = sp.getBoolean("vibrate", true)
        set(v) = sp.edit().putBoolean("vibrate", v).apply()

    var chimeOn: Boolean
        get() = sp.getBoolean("chimeOn", true)
        set(v) = sp.edit().putBoolean("chimeOn", v).apply()

    /** Which of the 200 tunes plays before the voice (see TuneSynth). */
    var tuneId: Int
        get() = sp.getInt("tuneId", 0)
        set(v) = sp.edit().putInt("tuneId", v.coerceIn(0, 199)).apply()

    /** Makes the spoken words louder than normal text-to-speech (with a built-in limiter). */
    var loudBoost: Boolean
        get() = sp.getBoolean("loudBoost", true)
        set(v) = sp.edit().putBoolean("loudBoost", v).apply()

    var lowBatteryAlertDay: String
        get() = sp.getString("lowBatteryAlertDay", "") ?: ""
        set(v) = sp.edit().putString("lowBatteryAlertDay", v).apply()

    /** Blink the camera torch while the reminder speaks – helps when hearing is weak. */
    var flashOn: Boolean
        get() = sp.getBoolean("flashOn", false)
        set(v) = sp.edit().putBoolean("flashOn", v).apply()

    /** When the day's attendance is closed (minutes after midnight, default 11:59 PM). */
    var dayCloseMinute: Int
        get() = sp.getInt("dayCloseMinute", 23 * 60 + 59)
        set(v) = sp.edit().putInt("dayCloseMinute", v.coerceIn(20 * 60, 23 * 60 + 59)).apply()

    var caregiverName: String
        get() = sp.getString("caregiverName", "") ?: ""
        set(v) = sp.edit().putString("caregiverName", v.trim()).apply()

    var caregiverPhone: String
        get() = sp.getString("caregiverPhone", "") ?: ""
        set(v) = sp.edit().putString("caregiverPhone", v.trim()).apply()

    var caregiverSms: Boolean
        get() = sp.getBoolean("caregiverSms", false)
        set(v) = sp.edit().putBoolean("caregiverSms", v).apply()

    /** Extra text size on top of the phone's own setting (1.0 – 1.5). */
    var textScale: Float
        get() = sp.getFloat("textScale", 1.12f)
        set(v) = sp.edit().putFloat("textScale", v.coerceIn(0.9f, 1.6f)).apply()

    /** 0 = follow phone, 1 = light, 2 = dark. */
    var themeMode: Int
        get() = sp.getInt("themeMode", 0)
        set(v) = sp.edit().putInt("themeMode", v).apply()

    /** The user's own times of day ("name|startMinute" per line). Empty = the standard list. */
    var slots: String
        get() = sp.getString("slots", "") ?: ""
        set(v) = sp.edit().putString("slots", v).apply()

    /** "live" = colours follow the sky (the current prahar); or a prahar key to keep one look. */
    var sky: String
        get() = sp.getString("sky", "live") ?: "live"
        set(v) = sp.edit().putString("sky", v).apply()

    var lastFiredAt: Long
        get() = sp.getLong("lastFiredAt", 0L)
        set(v) = sp.edit().putLong("lastFiredAt", v).apply()

    var firstDay: LocalDate
        get() {
            val s = sp.getString("firstDay", null)
            if (s == null) {
                val today = LocalDate.now()
                sp.edit().putString("firstDay", today.toString()).apply()
                return today
            }
            return LocalDate.parse(s)
        }
        set(v) = sp.edit().putString("firstDay", v.toString()).apply()

    var lowStockSpokenDay: String
        get() = sp.getString("lowStockDay", "") ?: ""
        set(v) = sp.edit().putString("lowStockDay", v).apply()

    var recordedVoices: List<RecordedVoice>
        get() {
            val raw = sp.getString("recordedVoices", "[]") ?: "[]"
            return try {
                val arr = JSONArray(raw)
                (0 until arr.length()).map {
                    val o = arr.getJSONObject(it)
                    RecordedVoice(o.getString("id"), o.getString("label"), o.getString("path"))
                }
            } catch (e: Exception) {
                emptyList()
            }
        }
        set(list) {
            val arr = JSONArray()
            list.forEach { arr.put(JSONObject().put("id", it.id).put("label", it.label).put("path", it.path)) }
            sp.edit().putString("recordedVoices", arr.toString()).apply()
        }

    fun clear() = sp.edit().clear().apply()
}
