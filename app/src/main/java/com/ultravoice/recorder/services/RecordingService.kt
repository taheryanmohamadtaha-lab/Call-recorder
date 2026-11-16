package com.ultravoice.recorder.services

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.ultravoice.recorder.MainActivity
import com.ultravoice.recorder.R
import com.ultravoice.recorder.UltraApp
import com.ultravoice.recorder.audio.RecStatus
import com.ultravoice.recorder.audio.RecorderState
import com.ultravoice.recorder.audio.WavHeader
import com.ultravoice.recorder.audio.WavRecorder
import com.ultravoice.recorder.data.RecordingEntity
import com.ultravoice.recorder.data.RecordingType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Foreground service that owns the recording. It keeps running with the screen off,
 * always shows a visible notification, and saves the file when stopped.
 */
class RecordingService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val active = AtomicBoolean(false)
    private val finishing = AtomicBoolean(false)

    @Volatile private var recorder: WavRecorder? = null
    @Volatile private var currentFile: File? = null
    private var currentType = RecordingType.AMBIENT
    private var startedWall = 0L
    private var startedElapsed = 0L
    private var maxMs = 0L
    private var lastSpaceCheck = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startRecording(intent.getStringExtra(EXTRA_TYPE) ?: RecordingType.AMBIENT)
            ACTION_PAUSE -> setPaused(true)
            ACTION_RESUME -> setPaused(false)
            ACTION_SAVE -> scope.launch { finish(save = true) }
            ACTION_DISCARD -> scope.launch { finish(save = false) }
            else -> if (recorder == null) stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun startRecording(type: String) {
        // startForegroundService() requires startForeground() promptly, so do it first.
        goForeground()
        if (!active.compareAndSet(false, true)) return
        finishing.set(false)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            RecorderState.fail(getString(R.string.perm_mic_needed))
            active.set(false)
            shutdown()
            return
        }
        currentType = type
        scope.launch {
            val app = application as UltraApp
            val s = app.settingsStore.current()
            val dir = app.repo.dir()
            if (dir.usableSpace < LOW_SPACE_BYTES) {
                RecorderState.fail(getString(R.string.error_storage))
                active.set(false)
                shutdown()
                return@launch
            }
            val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val file = File(dir, "REC_$stamp.wav")
            maxMs = s.maxMinutes * 60_000L
            val rec = WavRecorder(
                file = file,
                sampleRate = s.sampleRate,
                requestedChannels = s.channels,
                onLevel = { RecorderState.level(it) },
                onProgress = { ms, bytes -> onProgress(ms, bytes) },
            )
            val err = rec.start()
            if (err != null) {
                file.delete()
                RecorderState.fail(getString(R.string.error_start) + ": " + err)
                active.set(false)
                shutdown()
                return@launch
            }
            recorder = rec
            currentFile = file
            startedWall = System.currentTimeMillis()
            startedElapsed = SystemClock.elapsedRealtime()
            RecorderState.begin(s.sampleRate, rec.channels)
            updateNotification(paused = false)
        }
    }

    // Called on the recorder thread.
    private fun onProgress(ms: Long, bytes: Long) {
        RecorderState.progress(ms, bytes)
        if (maxMs > 0 && ms >= maxMs) {
            autoStop(getString(R.string.auto_stopped_max))
            return
        }
        val now = SystemClock.elapsedRealtime()
        if (now - lastSpaceCheck > 3000) {
            lastSpaceCheck = now
            val free = currentFile?.parentFile?.usableSpace ?: Long.MAX_VALUE
            if (free < LOW_SPACE_BYTES) autoStop(getString(R.string.error_storage))
        }
    }

    private fun autoStop(message: String) {
        if (finishing.get()) return
        scope.launch { finish(save = true, message = message) }
    }

    private fun setPaused(paused: Boolean) {
        val r = recorder ?: return
        r.paused = paused
        RecorderState.setStatus(if (paused) RecStatus.PAUSED else RecStatus.RECORDING)
        updateNotification(paused)
    }

    private suspend fun finish(save: Boolean, message: String? = null) {
        if (!finishing.compareAndSet(false, true)) return
        finalizeRecording(save)
        RecorderState.finish()
        if (message != null) RecorderState.notify(message)
        shutdown()
    }

    private suspend fun finalizeRecording(save: Boolean) {
        val rec = recorder
        val file = currentFile
        recorder = null
        currentFile = null
        if (rec == null || file == null) return
        val result = rec.stop()
        if (save && result.sizeBytes > WavHeader.SIZE) {
            val name = (if (currentType == RecordingType.CALL_SPEAKER) "Call " else "Recording ") +
                SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(startedWall))
            (application as UltraApp).repo.insert(
                RecordingEntity(
                    path = file.absolutePath,
                    name = name,
                    createdAt = startedWall,
                    durationMs = result.durationMs,
                    sizeBytes = result.sizeBytes,
                    sampleRate = result.sampleRate,
                    channels = result.channels,
                    type = currentType,
                )
            )
            RecorderState.markSaved()
        } else {
            file.delete()
        }
    }

    private fun shutdown() {
        active.set(false)
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        // If the system stops the service, still close the WAV properly and keep the audio.
        if (recorder != null && finishing.compareAndSet(false, true)) {
            runBlocking(Dispatchers.IO) { finalizeRecording(save = true) }
            RecorderState.finish()
        }
        scope.cancel()
        super.onDestroy()
    }

    // ---------------- notification ----------------

    private fun goForeground() {
        val type = if (android.os.Build.VERSION.SDK_INT >= 30) ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE else 0
        ServiceCompat.startForeground(this, NOTIF_ID, buildNotification(paused = false), type)
    }

    private fun updateNotification(paused: Boolean) {
        val nm = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        nm.notify(NOTIF_ID, buildNotification(paused))
    }

    private fun buildNotification(paused: Boolean): Notification {
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val b = NotificationCompat.Builder(this, UltraApp.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_mic)
            .setContentTitle(getString(R.string.notif_title))
            .setContentText(getString(if (paused) R.string.paused else R.string.recording))
            .setContentIntent(open)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        if (!paused && startedElapsed > 0) {
            b.setUsesChronometer(true)
            b.setWhen(System.currentTimeMillis() - (SystemClock.elapsedRealtime() - startedElapsed))
        } else {
            b.setShowWhen(false)
        }
        if (paused) {
            b.addAction(0, getString(R.string.resume), servicePending(ACTION_RESUME))
        } else {
            b.addAction(0, getString(R.string.pause), servicePending(ACTION_PAUSE))
        }
        b.addAction(0, getString(R.string.notif_stop), servicePending(ACTION_SAVE))
        return b.build()
    }

    private fun servicePending(action: String): PendingIntent =
        PendingIntent.getService(
            this, action.hashCode(),
            Intent(this, RecordingService::class.java).setAction(action),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    companion object {
        const val ACTION_START = "com.ultravoice.recorder.START"
        const val ACTION_PAUSE = "com.ultravoice.recorder.PAUSE"
        const val ACTION_RESUME = "com.ultravoice.recorder.RESUME"
        const val ACTION_SAVE = "com.ultravoice.recorder.SAVE"
        const val ACTION_DISCARD = "com.ultravoice.recorder.DISCARD"
        const val EXTRA_TYPE = "type"
        private const val NOTIF_ID = 1001
        private const val LOW_SPACE_BYTES = 20L * 1024 * 1024

        fun start(context: Context, type: String) {
            val i = Intent(context, RecordingService::class.java).setAction(ACTION_START).putExtra(EXTRA_TYPE, type)
            ContextCompat.startForegroundService(context, i)
        }

        fun send(context: Context, action: String) {
            context.startService(Intent(context, RecordingService::class.java).setAction(action))
        }
    }
}
