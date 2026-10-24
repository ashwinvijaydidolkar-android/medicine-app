package com.ragini.medreminder.report

import com.ragini.medreminder.data.DoseStatus
import org.json.JSONArray
import org.json.JSONObject

/**
 * The data for the Ragini report card. The card itself is drawn by assets/report/report.js – the very
 * same file the Windows app uses – so the report on screen, the PDF, the spreadsheet and the text
 * message look and read the same on every device.
 */
object ReportCard {
    fun key(p: Period): String = when (p) {
        Period.YESTERDAY -> "yesterday"; Period.DAILY -> "daily"; Period.OVERNIGHT -> "overnight"; Period.WEEKLY -> "weekly"
        Period.MONTHLY -> "monthly"; Period.QUARTERLY -> "quarterly"; Period.HALF_YEARLY -> "halfyearly"; Period.YEARLY -> "yearly"
    }

    fun title(p: Period): String = when (p) {
        Period.YESTERDAY -> "Yesterday's report"; Period.DAILY -> "Today's report"; Period.OVERNIGHT -> "Overnight report"
        Period.WEEKLY -> "Weekly report"; Period.MONTHLY -> "Monthly report"; Period.QUARTERLY -> "Quarterly report"
        Period.HALF_YEARLY -> "Six-monthly report"; Period.YEARLY -> "Yearly report"
    }

    private fun status(s: DoseStatus) = when (s) {
        DoseStatus.TAKEN -> "taken"; DoseStatus.NOT_TAKEN -> "not"; DoseStatus.MISSED -> "missed"; DoseStatus.PENDING -> "pending"
    }

    fun json(r: Report, person: String, slots: List<Pair<String, Int>>, madeAt: Long = System.currentTimeMillis()): String {
        val o = JSONObject()
        o.put("period", key(r.period)); o.put("title", title(r.period))
        o.put("from", r.from.toString()); o.put("to", r.to.toString())
        o.put("person", person); o.put("madeAt", madeAt); o.put("closeLabel", r.closeTimeLabel)
        o.put("streak", JSONObject().put("current", r.currentStreak).put("best", r.bestStreak))
        o.put("slots", JSONArray().apply { slots.forEach { (n, s) -> put(JSONObject().put("name", n).put("start", s)) } })
        o.put("rows", JSONArray().apply {
            r.rows.forEach { x ->
                put(JSONObject().apply {
                    put("day", x.day.toString()); put("minute", x.minute); put("name", x.medName); put("color", x.colorIndex)
                    put("status", status(x.status)); put("reason", x.reason)
                    put("answeredAt", x.respondedAt ?: JSONObject.NULL); put("scheduledAt", x.scheduledAt); put("final", x.final)
                })
            }
        })
        return o.toString()
    }
}
