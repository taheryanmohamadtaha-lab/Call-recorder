package com.ultravoice.recorder.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import com.ultravoice.recorder.UltraApp
import com.ultravoice.recorder.data.CallLogEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Logs only the start and end TIME of phone calls (no number, no contact, no audio).
 * It needs the READ_PHONE_STATE permission, which the user grants from the Calls screen.
 */
class CallStateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return
        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE) ?: return
        val prefs = context.applicationContext.getSharedPreferences("call_state", Context.MODE_PRIVATE)
        when (state) {
            TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                if (prefs.getLong("start", 0L) == 0L) prefs.edit().putLong("start", System.currentTimeMillis()).apply()
            }
            TelephonyManager.EXTRA_STATE_IDLE -> {
                val start = prefs.getLong("start", 0L)
                if (start > 0L) {
                    prefs.edit().remove("start").apply()
                    val end = System.currentTimeMillis()
                    val pending = goAsync()
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            (context.applicationContext as UltraApp).db.callLogDao()
                                .insert(CallLogEntity(startedAt = start, endedAt = end))
                        } finally {
                            pending.finish()
                        }
                    }
                }
            }
        }
    }
}
