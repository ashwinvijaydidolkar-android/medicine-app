package com.ragini.medreminder.alarm

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.telephony.SmsManager
import androidx.core.content.ContextCompat
import com.ragini.medreminder.data.Db
import com.ragini.medreminder.data.Dose
import com.ragini.medreminder.data.Prefs
import com.ragini.medreminder.data.minuteLabel

/**
 * If a dose is still unanswered after all the follow-up reminders, quietly sends a text
 * message to the family member set in Settings, so someone can call and check.
 */
object CaregiverAlert {

    fun canSms(ctx: Context) =
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED

    fun send(ctx: Context, doses: List<Dose>) {
        val prefs = Prefs(ctx)
        if (!prefs.caregiverSms || prefs.caregiverPhone.isBlank() || !canSms(ctx)) return
        val db = Db.get(ctx)
        val open = doses.filter { db.response(it.key) == null }.distinctBy { it.key }
        if (open.isEmpty()) return
        val who = prefs.userName.ifBlank { "Your family member" }
        val list = open.joinToString(", ") { "${it.medicine.name} (${minuteLabel(it.minuteOfDay)})" }
        val text = "Ragini reminder: $who has not confirmed $list yet. Please call to check. – sent automatically"
        try {
            val sms: SmsManager = if (Build.VERSION.SDK_INT >= 31) {
                ctx.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }
            val parts = sms.divideMessage(text)
            sms.sendMultipartTextMessage(prefs.caregiverPhone, null, parts, null, null)
        } catch (_: Exception) {
            // No SIM / SMS not allowed – nothing else to do.
        }
    }

    /** Tells the family the phone is about to run out of battery, so reminders don't stop. */
    fun lowBattery(ctx: Context, percent: Int) {
        val prefs = Prefs(ctx)
        if (!prefs.caregiverSms || prefs.caregiverPhone.isBlank() || !canSms(ctx)) return
        val who = prefs.userName.ifBlank { "your family member" }
        sendText(ctx, prefs.caregiverPhone,
            "Ragini reminder: the phone of $who is at $percent% battery. Please make sure it is put on charge so medicine reminders keep working.")
    }

    private fun sendText(ctx: Context, phone: String, text: String) {
        try {
            val sms: SmsManager = if (Build.VERSION.SDK_INT >= 31) {
                ctx.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }
            sms.sendMultipartTextMessage(phone, null, sms.divideMessage(text), null, null)
        } catch (_: Exception) {
        }
    }

    /** Opens the phone dialer with the caregiver's number. */
    fun callIntent(ctx: Context): Intent? {
        val phone = Prefs(ctx).caregiverPhone
        if (phone.isBlank()) return null
        return Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}
