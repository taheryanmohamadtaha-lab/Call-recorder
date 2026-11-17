package com.ultravoice.recorder.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import java.io.File
import java.io.RandomAccessFile
import kotlin.math.abs
import kotlin.math.min

data class RecorderResult(
    val file: File,
    val durationMs: Long,
    val sizeBytes: Long,
    val sampleRate: Int,
    val channels: Int,
)

/**
 * Records 16-bit PCM from the microphone into a WAV file.
 * The WAV header is rewritten about once per second, so the file stays playable
 * even if the process is killed unexpectedly.
 *
 * [sources] are official Android microphone sources, tried in order. During a phone call some
 * devices deliver pure digital silence for some sources. If that happens, the recorder moves
 * on to the next source (same file) and calls [onSilence] only after every source stayed silent.
 */
class WavRecorder(
    private val file: File,
    private val sampleRate: Int,
    private val requestedChannels: Int,
    private val sources: List<Int> = listOf(MediaRecorder.AudioSource.MIC),
    private val onLevel: (Float) -> Unit,
    private val onProgress: (elapsedMs: Long, sizeBytes: Long) -> Unit,
    private val onSilence: () -> Unit = {},
) {
    @Volatile private var running = false
    @Volatile var paused = false
    private var thread: Thread? = null
    @Volatile private var record: AudioRecord? = null
    private var raf: RandomAccessFile? = null
    private var sourceIndex = 0

    var channels: Int = requestedChannels
        private set
    @Volatile private var dataBytes = 0L

    private fun open(source: Int, ch: Int): AudioRecord? {
        val mask = if (ch == 2) AudioFormat.CHANNEL_IN_STEREO else AudioFormat.CHANNEL_IN_MONO
        val minBuf = AudioRecord.getMinBufferSize(sampleRate, mask, AudioFormat.ENCODING_PCM_16BIT)
        if (minBuf <= 0) return null
        return try {
            @SuppressLint("MissingPermission")
            val r = AudioRecord(source, sampleRate, mask, AudioFormat.ENCODING_PCM_16BIT, minBuf * 4)
            if (r.state == AudioRecord.STATE_INITIALIZED) r else { r.release(); null }
        } catch (e: Exception) {
            null
        }
    }

    /** Opens and starts the first source (from [from]) that works. */
    private fun openAndStart(from: Int, ch: Int): Pair<AudioRecord, Int>? {
        for (i in from until sources.size) {
            val r = open(sources[i], ch) ?: continue
            try {
                r.startRecording()
            } catch (e: Exception) {
                r.release(); continue
            }
            if (r.recordingState == AudioRecord.RECORDSTATE_RECORDING) return r to i
            r.release()
        }
        return null
    }

    /** Returns null on success or an error message. */
    fun start(): String? {
        var ch = requestedChannels
        var found = openAndStart(0, ch)
        if (found == null && ch == 2) { ch = 1; found = openAndStart(0, ch) }
        if (found == null) return "Microphone is busy or unavailable"
        channels = ch
        record = found.first
        sourceIndex = found.second
        val out = RandomAccessFile(file, "rw")
        out.setLength(0)
        out.write(WavHeader.build(0, sampleRate, channels))
        raf = out
        running = true
        thread = Thread(::loop, "wav-recorder").also { it.start() }
        return null
    }

    private fun loop() {
        val out = raf ?: return
        val buffer = ShortArray(sampleRate / 10 * channels) // ~100 ms
        val bytes = ByteArray(buffer.size * 2)
        var lastHeader = 0L
        var silentSamples = 0L
        var warned = false
        while (running) {
            val rec = record ?: break
            val n = rec.read(buffer, 0, buffer.size)
            if (n <= 0) continue
            if (paused) {
                onLevel(0f)
                continue
            }
            var peak = 0
            for (i in 0 until n) {
                val s = buffer[i].toInt()
                bytes[i * 2] = (s and 0xFF).toByte()
                bytes[i * 2 + 1] = ((s shr 8) and 0xFF).toByte()
                peak = maxOf(peak, abs(s))
            }
            out.write(bytes, 0, n * 2)
            dataBytes += n * 2
            onLevel(min(1f, peak / 32768f * 1.6f))
            val elapsed = durationMs()
            onProgress(elapsed, dataBytes + WavHeader.SIZE)
            if (elapsed - lastHeader >= 1000) {
                lastHeader = elapsed
                writeHeader(out)
            }

            // Digital silence = the system is blocking this source (typical during phone calls).
            if (peak == 0) silentSamples += n / channels else silentSamples = 0
            if (silentSamples >= sampleRate * SILENCE_SECONDS) {
                silentSamples = 0
                val next = openAndStart(sourceIndex + 1, channels)
                if (next != null) {
                    val old = rec
                    record = next.first
                    sourceIndex = next.second
                    runCatching { old.stop() }
                    old.release()
                } else if (!warned) {
                    warned = true
                    onSilence()
                }
            }
        }
    }

    private fun writeHeader(out: RandomAccessFile) {
        val pos = out.filePointer
        out.seek(0)
        out.write(WavHeader.build(dataBytes, sampleRate, channels))
        out.seek(pos)
    }

    private fun durationMs(): Long = dataBytes * 1000L / (sampleRate * channels * 2L)

    fun stop(): RecorderResult {
        running = false
        thread?.join(2000)
        runCatching { record?.stop() }
        record?.release()
        raf?.let {
            writeHeader(it)
            it.close()
        }
        return RecorderResult(file, durationMs(), file.length(), sampleRate, channels)
    }

    companion object {
        private const val SILENCE_SECONDS = 2

        /** Sources for speaker-mode call recording, most promising first. All are official APIs. */
        fun callSources(): List<Int> = buildList {
            add(MediaRecorder.AudioSource.VOICE_RECOGNITION)
            add(MediaRecorder.AudioSource.CAMCORDER)
            add(MediaRecorder.AudioSource.VOICE_COMMUNICATION)
            add(MediaRecorder.AudioSource.UNPROCESSED)
            add(MediaRecorder.AudioSource.MIC)
        }
    }
}
