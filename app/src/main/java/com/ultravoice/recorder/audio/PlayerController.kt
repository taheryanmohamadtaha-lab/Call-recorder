package com.ultravoice.recorder.audio

import android.media.MediaPlayer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Small wrapper around MediaPlayer that exposes Compose-observable state. */
class PlayerController(path: String) {
    private val mp = MediaPlayer()
    private var released = false

    var isPlaying by mutableStateOf(false); private set
    var positionMs by mutableLongStateOf(0L); private set
    var durationMs by mutableLongStateOf(0L); private set
    var speed by mutableFloatStateOf(1f); private set
    var volume by mutableFloatStateOf(1f); private set
    var failed by mutableStateOf(false); private set

    init {
        try {
            mp.setDataSource(path)
            mp.prepare()
            durationMs = mp.duration.toLong()
            mp.setOnCompletionListener {
                isPlaying = false
                positionMs = durationMs
            }
        } catch (e: Exception) {
            failed = true
        }
    }

    fun toggle() {
        if (failed || released) return
        if (isPlaying) {
            mp.pause()
            isPlaying = false
        } else {
            if (positionMs >= durationMs - 50) { mp.seekTo(0); positionMs = 0 }
            mp.start()
            applySpeed()
            isPlaying = true
        }
    }

    fun seekTo(ms: Long) {
        if (failed || released) return
        val t = ms.coerceIn(0L, durationMs)
        mp.seekTo(t.toInt())
        positionMs = t
    }

    fun seekFraction(f: Float) = seekTo((durationMs * f.coerceIn(0f, 1f)).toLong())
    fun skip(deltaMs: Long) = seekTo(positionMs + deltaMs)

    fun setSpeedValue(s: Float) {
        speed = s
        if (isPlaying) applySpeed()
    }

    fun setVolumeValue(v: Float) {
        volume = v.coerceIn(0f, 1f)
        if (!failed && !released) mp.setVolume(volume, volume)
    }

    private fun applySpeed() {
        runCatching { mp.playbackParams = mp.playbackParams.setSpeed(speed) }
    }

    /** Called periodically from the UI to refresh the position. */
    fun tick() {
        if (failed || released || !isPlaying) return
        runCatching { positionMs = mp.currentPosition.toLong() }
    }

    fun pause() {
        if (failed || released) return
        if (isPlaying) { mp.pause(); isPlaying = false }
    }

    fun release() {
        if (released) return
        released = true
        isPlaying = false
        runCatching { mp.release() }
    }
}
