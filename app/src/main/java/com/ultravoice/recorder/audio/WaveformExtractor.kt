package com.ultravoice.recorder.audio

import java.io.File
import java.io.RandomAccessFile
import kotlin.math.abs

object WaveformExtractor {
    /** Returns [count] peak values in 0..1 (normalised to the loudest bucket) for a 16-bit PCM WAV. */
    fun peaks(file: File, count: Int): FloatArray {
        val out = FloatArray(count)
        val total = (file.length() - WavHeader.SIZE) / 2
        if (total <= 0 || count <= 0) return out
        val per = (total / count).coerceAtLeast(1)
        RandomAccessFile(file, "r").use { raf ->
            raf.seek(WavHeader.SIZE.toLong())
            val buf = ByteArray(16384)
            var remaining = total * 2
            var index = 0L
            while (remaining > 0) {
                val want = minOf(buf.size.toLong(), remaining).toInt()
                raf.readFully(buf, 0, want)
                remaining -= want
                var i = 0
                while (i + 1 < want) {
                    val s = ((buf[i + 1].toInt() shl 8) or (buf[i].toInt() and 0xFF)).toShort().toInt()
                    val b = (index / per).toInt().coerceAtMost(count - 1)
                    val v = abs(s) / 32768f
                    if (v > out[b]) out[b] = v
                    index++
                    i += 2
                }
            }
        }
        val max = out.maxOrNull() ?: 0f
        if (max > 0f) for (i in out.indices) out[i] = out[i] / max
        return out
    }
}
