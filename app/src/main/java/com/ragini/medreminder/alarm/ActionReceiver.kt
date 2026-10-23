package com.ragini.medreminder.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ragini.medreminder.data.DoseStatus
import com.ragini.medreminder.schedule.Answers

/** Handles the "✓ Taken" button on the "Did you take it?" notification. */
class ActionReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_TAKEN = "com.ragini.medreminder.action.TAKEN"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_TAKEN) return
        val key = intent.getStringExtra("key") ?: return
        Answers.record(context, key, DoseStatus.TAKEN, "Marked from notification")
    }
}
