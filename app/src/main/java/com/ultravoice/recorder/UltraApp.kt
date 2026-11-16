package com.ultravoice.recorder

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.ultravoice.recorder.data.AppDatabase
import com.ultravoice.recorder.data.RecordingRepository
import com.ultravoice.recorder.data.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class UltraApp : Application() {
    val db: AppDatabase by lazy { AppDatabase.get(this) }
    val settingsStore: SettingsStore by lazy { SettingsStore(this) }
    val repo: RecordingRepository by lazy { RecordingRepository(this, db) }

    override fun onCreate() {
        super.onCreate()
        createChannel()
        // A fresh process never has an active recording, so unreferenced WAV files are leftovers of a crash.
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            runCatching { repo.recoverOrphans() }
        }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val ch = NotificationChannel(CHANNEL_ID, getString(R.string.notif_channel), NotificationManager.IMPORTANCE_LOW)
            ch.setShowBadge(false)
            getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        }
    }

    companion object {
        const val CHANNEL_ID = "recording"
    }
}
