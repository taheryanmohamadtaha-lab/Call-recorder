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
 */
class WavRecorder(
    private val file: File,
    private val sampleRate: Int,
    private val requestedChannels: Int,
    private val onLevel: (Float) -> Unit,
    private val onProgress: (elapsedMs: Long, sizeBytes: Long) -> Unit,
) {
    @Volatile private var running = false
    @Volatile var paused = false
    private var thread: Thread? = null
    private var record: AudioRecord? = null
    private var raf: RandomAccessFile? = null

    var channels: Int = requestedChannels
        private set
    @Volatile private var dataBytes = 0L

    /** Returns null on success or an error message. */
    @SuppressLint("MissingPermission") // permission is checked by the caller
    fun start(): String? {
        var ch = requestedChannels
        var rec: AudioRecord? = null
        while (rec == null) {
            val mask = if (ch == 2) AudioFormat.CHANNEL_IN_STEREO else AudioFormat.CHANNEL_IN_MONO
            val min = AudioRecord.getMinBufferSize(sampleRate, mask, AudioFormat.ENCODING_PCM_16BIT)
            if (min > 0) {
                val candidate = AudioRecord(
                    MediaRecorder.AudioSource.MIC, sampleRate, mask,
                    AudioFormat.ENCODING_PCM_16BIT, min * 4,
                )
                if (candidate.state == AudioRecord.STATE_INITIALIZED) rec = candidate else candidate.release()
            }
            if (rec == null) {
                if (ch == 2) ch = 1 else return "AudioRecord init failed"
            }
        }
        channels = ch
        record = rec
        val out = RandomAccessFile(file, "rw")
        out.setLength(0)
        out.write(WavHeader.build(0, sampleRate, channels))
        raf = out

        running = true
        rec.startRecording()
        if (rec.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
            running = false
            rec.release()
            out.close()
            return "Microphone is busy or unavailable"
        }
        thread = Thread(::loop, "wav-recorder").also { it.start() }
        return null
    }

    private fun loop() {
        val rec = record ?: return
        val out = raf ?: return
        val buffer = ShortArray(sampleRate / 10 * channels) // ~100 ms
        val bytes = ByteArray(buffer.size * 2)
        var lastHeader = 0L
        while (running) {
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
}
