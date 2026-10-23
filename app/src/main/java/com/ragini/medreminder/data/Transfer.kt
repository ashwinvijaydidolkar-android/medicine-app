package com.ragini.medreminder.data

import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.OffsetDateTime

/**
 * Moving the medicine list to another device.
 *
 * The file is plain JSON and is the SAME format in the Android and Windows apps, so a list can go
 * phone → phone, phone → PC or PC → phone. Only medicines and the user's times of day travel;
 * the attendance history stays on the device where it happened.
 *
 * {
 *   "app": "Ragini", "kind": "medicine-list", "version": 1,
 *   "exportedAt": "2026-10-23T22:45:00+05:30", "from": "Android",
 *   "medicines": [ { "name", "sayAs", "dosage", "notes", "times": [480, 1260],
 *                    "schedule": "daily" | "weekdays" | "everyN", "weekdays": [1..7, Monday = 1], "everyN",
 *                    "startDate": "2026-10-01", "endDate": null, "food": "none" | "before" | "after" | "with" | "empty" | "bedtime",
 *                    "color", "shape", "lead", "stock", "perDose", "refillAt", "active", "voiceId" } ],
 *   "timesOfDay": [ { "name": "After breakfast", "start": 510 } ]
 * }
 */
object Transfer {
    const val VERSION = 1
    val openTypes = arrayOf("application/json", "text/plain", "application/octet-stream", "*/*")

    fun fileName(): String = "Ragini-medicines-${LocalDate.now()}.json"

    fun export(meds: List<Medicine>, slots: List<Pair<String, Int>>): String {
        val o = JSONObject()
        o.put("app", "Ragini"); o.put("kind", "medicine-list"); o.put("version", VERSION)
        o.put("exportedAt", OffsetDateTime.now().withNano(0).toString()); o.put("from", "Android")
        o.put("medicines", JSONArray().apply { meds.forEach { put(medToJson(it)) } })
        o.put("timesOfDay", JSONArray().apply { slots.forEach { (n, s) -> put(JSONObject().put("name", n).put("start", s)) } })
        return o.toString(2)
    }

    /** What was found in a file, before anything is changed. */
    data class Parsed(
        val meds: List<Medicine>,
        val slots: List<Pair<String, Int>>?,
        val from: String,
        val exportedOn: String,
        val unreadable: Int
    )

    class BadFile(message: String) : Exception(message)

    fun parse(text: String): Parsed {
        val o = try { JSONObject(text.trim().removePrefix("﻿")) } catch (e: Exception) {
            throw BadFile("This file is not a Ragini medicine list.")
        }
        if (o.optString("app") != "Ragini" || o.optString("kind") != "medicine-list") throw BadFile("This file is not a Ragini medicine list.")
        if (o.optInt("version", 1) > VERSION) throw BadFile("This list was made by a newer Ragini. Please update the app on this device first.")
        val arr = o.optJSONArray("medicines") ?: JSONArray()
        val meds = ArrayList<Medicine>()
        var bad = 0
        for (i in 0 until arr.length()) {
            val m = arr.optJSONObject(i)?.let { medFromJson(it) }
            if (m == null) bad++ else meds.add(m)
        }
        val slots = o.optJSONArray("timesOfDay")?.let { a ->
            (0 until a.length()).mapNotNull { k -> a.optJSONObject(k)?.let { it.optString("name") to it.optInt("start", -1) } }
                .filter { it.first.isNotBlank() && it.second in 0..1439 }
        }?.takeIf { it.isNotEmpty() }
        if (meds.isEmpty() && bad == 0) throw BadFile("This list has no medicines in it.")
        val on = o.optString("exportedAt").take(10).let { d -> runCatching { LocalDate.parse(d) }.getOrNull() }
        return Parsed(meds, slots, o.optString("from", "another device"), on?.let { Format.day(it) } ?: "", bad)
    }

    data class Result(val added: Int, val alreadyThere: Int, val removed: Int)

    /** Adds the medicines (skipping ones already on this device), or replaces the whole list. */
    fun apply(db: Db, p: Parsed, replace: Boolean): Result {
        var removed = 0
        if (replace) db.medicines().forEach { db.deleteMedicine(it.id); removed++ }
        val existing = if (replace) emptyList() else db.medicines()
        var added = 0; var dup = 0
        for (m in p.meds) {
            if (existing.any { it.name.equals(m.name, ignoreCase = true) && it.times.sorted() == m.times.sorted() }) { dup++; continue }
            db.saveMedicine(m.copy(id = 0, createdAt = System.currentTimeMillis())); added++
        }
        return Result(added, dup, removed)
    }

    // ------------------------------------------------------------------ mapping

    private fun medToJson(m: Medicine) = JSONObject().apply {
        put("name", m.name); put("sayAs", m.spokenName); put("dosage", m.dosage); put("notes", m.notes)
        put("times", JSONArray(m.times.sorted()))
        put("schedule", when (m.scheduleType) { ScheduleType.DAILY -> "daily"; ScheduleType.WEEKDAYS -> "weekdays"; ScheduleType.EVERY_N_DAYS -> "everyN" })
        put("weekdays", JSONArray(m.weekdays.sorted())); put("everyN", m.everyNDays)
        put("startDate", m.startDate.toString()); put("endDate", m.endDate?.toString() ?: JSONObject.NULL)
        put("food", m.food.name.lowercase()); put("color", m.colorIndex); put("shape", m.shape.name.lowercase())
        put("lead", m.leadMinutes ?: JSONObject.NULL); put("stock", m.stock ?: JSONObject.NULL)
        put("perDose", m.perDose); put("refillAt", m.refillAt); put("active", m.active)
        put("voiceId", m.voiceId ?: JSONObject.NULL)
    }

    private fun JSONObject.optIntOrNull(k: String): Int? = if (!has(k) || isNull(k)) null else optInt(k, Int.MIN_VALUE).takeIf { it != Int.MIN_VALUE }

    private fun medFromJson(o: JSONObject): Medicine? {
        val name = o.optString("name").replace(Regex("\\s+"), " ").trim()
        val times = o.optJSONArray("times")?.let { a -> (0 until a.length()).map { a.optInt(it, -1) } }.orEmpty()
            .filter { it in 0..1439 }.distinct().sorted()
        if (name.isEmpty() || times.isEmpty()) return null
        val days = o.optJSONArray("weekdays")?.let { a -> (0 until a.length()).map { a.optInt(it) }.filter { it in 1..7 }.toSet() }
            ?.takeIf { it.isNotEmpty() } ?: (1..7).toSet()
        fun date(k: String) = o.optString(k).takeIf { !o.isNull(k) && it.isNotBlank() }?.let { runCatching { LocalDate.parse(it.take(10)) }.getOrNull() }
        return Medicine(
            name = name.take(120),
            spokenName = o.optString("sayAs", o.optString("spokenName", "")).trim(),
            dosage = o.optString("dosage").trim(),
            notes = o.optString("notes").trim(),
            times = times,
            scheduleType = when (o.optString("schedule")) { "weekdays" -> ScheduleType.WEEKDAYS; "everyN" -> ScheduleType.EVERY_N_DAYS; else -> ScheduleType.DAILY },
            weekdays = days,
            everyNDays = o.optInt("everyN", 1).coerceIn(1, 365),
            startDate = date("startDate") ?: LocalDate.now(),
            endDate = date("endDate"),
            food = FoodRule.entries.firstOrNull { it.name.equals(o.optString("food"), ignoreCase = true) } ?: FoodRule.NONE,
            colorIndex = o.optInt("color", 0).coerceIn(0, 7),
            shape = PillShape.entries.firstOrNull { it.name.equals(o.optString("shape"), ignoreCase = true) } ?: PillShape.CAPSULE,
            // A voice only travels if this phone has the same one (Windows voices do not exist on Android).
            voiceId = o.optString("voiceId").takeIf { !o.isNull("voiceId") && it.isNotBlank() && com.ragini.medreminder.voice.VoiceCatalog.byId(it) != null },
            leadMinutes = o.optIntOrNull("lead")?.coerceIn(0, 120),
            stock = o.optIntOrNull("stock")?.coerceAtLeast(0),
            perDose = o.optInt("perDose", 1).coerceIn(1, 50),
            refillAt = o.optInt("refillAt", 5).coerceAtLeast(0),
            active = o.optBoolean("active", true)
        )
    }

    private object Format {
        private val f = java.time.format.DateTimeFormatter.ofPattern("d MMMM yyyy", java.util.Locale.ENGLISH)
        fun day(d: LocalDate): String = d.format(f)
    }
}
