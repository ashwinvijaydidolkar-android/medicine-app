package com.ragini.medreminder.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/** Plain SQLite storage – no extra libraries, works on every Android version. */
class Db private constructor(context: Context) :
    SQLiteOpenHelper(context.applicationContext, "ragini.db", null, 1) {

    companion object {
        @Volatile private var instance: Db? = null
        fun get(context: Context): Db =
            instance ?: synchronized(this) { instance ?: Db(context).also { instance = it } }
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE medicines (id INTEGER PRIMARY KEY AUTOINCREMENT, json TEXT NOT NULL)")
        db.execSQL(
            "CREATE TABLE responses (dose_key TEXT PRIMARY KEY, med_id INTEGER, scheduled_at INTEGER, " +
                "status TEXT, reason TEXT, responded_at INTEGER)"
        )
        db.execSQL("CREATE INDEX idx_resp_time ON responses(scheduled_at)")
        db.execSQL(
            "CREATE TABLE final_doses (dose_key TEXT PRIMARY KEY, med_id INTEGER, med_name TEXT, day TEXT, " +
                "minute INTEGER, scheduled_at INTEGER, status TEXT, reason TEXT, responded_at INTEGER)"
        )
        db.execSQL("CREATE INDEX idx_final_day ON final_doses(day)")
        db.execSQL("CREATE TABLE closed_days (day TEXT PRIMARY KEY, closed_at INTEGER)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}

    // ---------- Medicines ----------

    fun medicines(): List<Medicine> {
        val out = ArrayList<Medicine>()
        readableDatabase.rawQuery("SELECT id, json FROM medicines ORDER BY id", null).use { c ->
            while (c.moveToNext()) {
                fromJson(c.getLong(0), c.getString(1))?.let { out.add(it) }
            }
        }
        return out
    }

    fun medicine(id: Long): Medicine? = medicines().firstOrNull { it.id == id }

    fun saveMedicine(m: Medicine): Long {
        val cv = ContentValues().apply { put("json", toJson(m).toString()) }
        return if (m.id == 0L) {
            writableDatabase.insert("medicines", null, cv)
        } else {
            writableDatabase.update("medicines", cv, "id = ?", arrayOf(m.id.toString()))
            m.id
        }
    }

    fun deleteMedicine(id: Long) {
        writableDatabase.delete("medicines", "id = ?", arrayOf(id.toString()))
    }

    private fun toJson(m: Medicine): JSONObject = JSONObject().apply {
        put("name", m.name)
        put("spokenName", m.spokenName)
        put("dosage", m.dosage)
        put("notes", m.notes)
        put("times", JSONArray(m.times))
        put("scheduleType", m.scheduleType.name)
        put("weekdays", JSONArray(m.weekdays.toList()))
        put("everyNDays", m.everyNDays)
        put("startDate", m.startDate.toString())
        put("endDate", m.endDate?.toString() ?: "")
        put("food", m.food.name)
        put("colorIndex", m.colorIndex)
        put("shape", m.shape.name)
        put("voiceId", m.voiceId ?: "")
        put("leadMinutes", m.leadMinutes ?: -1)
        put("stock", m.stock ?: -1)
        put("perDose", m.perDose)
        put("refillAt", m.refillAt)
        put("active", m.active)
        put("createdAt", m.createdAt)
    }

    private fun fromJson(id: Long, raw: String): Medicine? = try {
        val o = JSONObject(raw)
        val times = o.getJSONArray("times").let { a -> (0 until a.length()).map { a.getInt(it) } }
        val days = o.getJSONArray("weekdays").let { a -> (0 until a.length()).map { a.getInt(it) }.toSet() }
        Medicine(
            id = id,
            name = o.getString("name"),
            spokenName = o.optString("spokenName", ""),
            dosage = o.optString("dosage", ""),
            notes = o.optString("notes", ""),
            times = times.sorted(),
            scheduleType = ScheduleType.valueOf(o.optString("scheduleType", "DAILY")),
            weekdays = days,
            everyNDays = o.optInt("everyNDays", 1),
            startDate = LocalDate.parse(o.getString("startDate")),
            endDate = o.optString("endDate", "").let { if (it.isBlank()) null else LocalDate.parse(it) },
            food = FoodRule.valueOf(o.optString("food", "NONE")),
            colorIndex = o.optInt("colorIndex", 0),
            shape = PillShape.valueOf(o.optString("shape", "CAPSULE")),
            voiceId = o.optString("voiceId", "").ifBlank { null },
            leadMinutes = o.optInt("leadMinutes", -1).let { if (it < 0) null else it },
            stock = o.optInt("stock", -1).let { if (it < 0) null else it },
            perDose = o.optInt("perDose", 1),
            refillAt = o.optInt("refillAt", 5),
            active = o.optBoolean("active", true),
            createdAt = o.optLong("createdAt", 0L)
        )
    } catch (e: Exception) {
        null
    }

    // ---------- Responses (answers given after closing a reminder) ----------

    fun response(doseKey: String): DoseResponse? =
        readableDatabase.rawQuery(
            "SELECT dose_key, med_id, scheduled_at, status, reason, responded_at FROM responses WHERE dose_key = ?",
            arrayOf(doseKey)
        ).use { c -> if (c.moveToFirst()) readResponse(c) else null }

    fun responsesBetween(fromMs: Long, toMs: Long): Map<String, DoseResponse> {
        val out = HashMap<String, DoseResponse>()
        readableDatabase.rawQuery(
            "SELECT dose_key, med_id, scheduled_at, status, reason, responded_at FROM responses " +
                "WHERE scheduled_at >= ? AND scheduled_at < ?",
            arrayOf(fromMs.toString(), toMs.toString())
        ).use { c -> while (c.moveToNext()) readResponse(c).let { out[it.doseKey] = it } }
        return out
    }

    private fun readResponse(c: android.database.Cursor) = DoseResponse(
        doseKey = c.getString(0),
        medId = c.getLong(1),
        scheduledAt = c.getLong(2),
        status = DoseStatus.valueOf(c.getString(3)),
        reason = c.getString(4) ?: "",
        respondedAt = c.getLong(5)
    )

    fun saveResponse(r: DoseResponse) {
        val cv = ContentValues().apply {
            put("dose_key", r.doseKey)
            put("med_id", r.medId)
            put("scheduled_at", r.scheduledAt)
            put("status", r.status.name)
            put("reason", r.reason)
            put("responded_at", r.respondedAt)
        }
        writableDatabase.insertWithOnConflict("responses", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
        // A late answer for a day that is already closed still updates the register.
        val upd = ContentValues().apply {
            put("status", r.status.name)
            put("reason", r.reason)
            put("responded_at", r.respondedAt)
        }
        writableDatabase.update("final_doses", upd, "dose_key = ?", arrayOf(r.doseKey))
    }

    // ---------- Attendance register ----------

    fun isClosed(day: LocalDate): Boolean =
        readableDatabase.rawQuery("SELECT 1 FROM closed_days WHERE day = ?", arrayOf(day.toString()))
            .use { it.moveToFirst() }

    fun closeDay(day: LocalDate, finals: List<FinalDose>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            finals.forEach { f ->
                val cv = ContentValues().apply {
                    put("dose_key", f.doseKey)
                    put("med_id", f.medId)
                    put("med_name", f.medName)
                    put("day", f.day.toString())
                    put("minute", f.minuteOfDay)
                    put("scheduled_at", f.scheduledAt)
                    put("status", f.status.name)
                    put("reason", f.reason)
                    if (f.respondedAt != null) put("responded_at", f.respondedAt) else putNull("responded_at")
                }
                db.insertWithOnConflict("final_doses", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
            }
            val cv = ContentValues().apply {
                put("day", day.toString())
                put("closed_at", System.currentTimeMillis())
            }
            db.insertWithOnConflict("closed_days", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    /** Closed-day records between two days, inclusive. ISO dates sort correctly as text. */
    fun finals(from: LocalDate, to: LocalDate): List<FinalDose> {
        val out = ArrayList<FinalDose>()
        readableDatabase.rawQuery(
            "SELECT dose_key, med_id, med_name, day, minute, scheduled_at, status, reason, responded_at " +
                "FROM final_doses WHERE day >= ? AND day <= ? ORDER BY scheduled_at",
            arrayOf(from.toString(), to.toString())
        ).use { c ->
            while (c.moveToNext()) {
                out.add(
                    FinalDose(
                        doseKey = c.getString(0),
                        medId = c.getLong(1),
                        medName = c.getString(2) ?: "",
                        day = LocalDate.parse(c.getString(3)),
                        minuteOfDay = c.getInt(4),
                        scheduledAt = c.getLong(5),
                        status = DoseStatus.valueOf(c.getString(6)),
                        reason = c.getString(7) ?: "",
                        respondedAt = if (c.isNull(8)) null else c.getLong(8)
                    )
                )
            }
        }
        return out
    }

    fun clearAll() {
        val db = writableDatabase
        db.delete("medicines", null, null)
        db.delete("responses", null, null)
        db.delete("final_doses", null, null)
        db.delete("closed_days", null, null)
    }
}
