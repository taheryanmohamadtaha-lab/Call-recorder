package com.ultravoice.recorder.audio

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class RecStatus { IDLE, RECORDING, PAUSED }

/** Process-wide recording state shared between the foreground service and the UI. */
object RecorderState {
    private val _status = MutableStateFlow(RecStatus.IDLE)
    private val _elapsedMs = MutableStateFlow(0L)
    private val _sizeBytes = MutableStateFlow(0L)
    private val _levels = MutableStateFlow<List<Float>>(emptyList())
    private val _channels = MutableStateFlow(1)
    private val _sampleRate = MutableStateFlow(0)
    private val _error = MutableStateFlow<String?>(null)
    private val _saved = MutableStateFlow(0)

    val status: StateFlow<RecStatus> = _status
    val elapsedMs: StateFlow<Long> = _elapsedMs
    val sizeBytes: StateFlow<Long> = _sizeBytes
    val levels: StateFlow<List<Float>> = _levels
    val channels: StateFlow<Int> = _channels
    val sampleRate: StateFlow<Int> = _sampleRate
    val error: StateFlow<String?> = _error
    /** Incremented every time a recording has been saved successfully. */
    val saved: StateFlow<Int> = _saved

    const val MAX_LEVELS = 80

    fun begin(sampleRate: Int, channels: Int) {
        _levels.value = emptyList()
        _elapsedMs.value = 0
        _sizeBytes.value = 0
        _sampleRate.value = sampleRate
        _channels.value = channels
        _error.value = null
        _status.value = RecStatus.RECORDING
    }

    fun setStatus(s: RecStatus) { _status.value = s }
    fun progress(ms: Long, bytes: Long) { _elapsedMs.value = ms; _sizeBytes.value = bytes }
    fun level(v: Float) { _levels.value = (_levels.value + v).takeLast(MAX_LEVELS) }
    fun fail(msg: String) { _error.value = msg; _status.value = RecStatus.IDLE }
    fun notify(msg: String) { _error.value = msg }
    fun markSaved() { _saved.value = _saved.value + 1 }
    fun clearError() { _error.value = null }
    fun finish() { _status.value = RecStatus.IDLE; _levels.value = emptyList() }
}
