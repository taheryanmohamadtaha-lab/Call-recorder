package com.ultravoice.recorder.audio

import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max

/**
 * Offline, non-destructive clean-up of a 16-bit PCM WAV:
 *  1. one-pole high-pass at 100 Hz (removes wind rumble / handling noise),
 *  2. a gentle noise gate (lowers steady background hiss between phrases),
 *  3. peak normalisation to about -1 dBFS.
 * This is classic DSP, not machine learning. It never modifies the input file.
 */
object Enhancer {
    data class Options(
        val highPass: Boolean = true,
        val gate: Boolean = true,
        val normalize: Boolean = true,
    )

    private const val GATE_THRESHOLD = 0.006f // about -44 dBFS
    private const val GATE_FLOOR = 0.25f
    private const val TARGET_PEAK = 0.89f // about -1 dBFS
    private const val MAX_BOOST = 10f
    private const val FRAMES_PER_CHUNK = 4096

    fun process(input: File, output: File, opt: Options = Options(), onProgress: (Float) -> Unit = {}): Boolean {
        val head = ByteArray(WavHeader.SIZE)
        val inLen = input.length()
        if (inLen <= WavHeader.SIZE) return false
        RandomAccessFile(input, "r").use { it.readFully(head) }
        val info = WavHeader.parse(head) ?: return false
        val ch = info.channels
        val frameBytes = ch * 2
        val totalFrames = (inLen - WavHeader.SIZE) / frameBytes
        if (totalFrames <= 0) return false
        val dataBytes = totalFrames * frameBytes

        val a = 1f / (1f + (2.0 * PI * 100.0 / info.sampleRate).toFloat())
        val px = FloatArray(ch)
        val py = FloatArray(ch)
        val tmp = FloatArray(ch)
        var env = 0f
        var gain = 1f
        var peak = 0f

        // ---- pass 1: filter + gate ----
        RandomAccessFile(input, "r").use { src ->
            src.seek(WavHeader.SIZE.toLong())
            BufferedOutputStream(FileOutputStream(output), 1 shl 16).use { dst ->
                dst.write(WavHeader.build(dataBytes, info.sampleRate, ch))
                val inBuf = ByteArray(FRAMES_PER_CHUNK * frameBytes)
                var done = 0L
                while (done < totalFrames) {
                    val frames = minOf(FRAMES_PER_CHUNK.toLong(), totalFrames - done).toInt()
                    src.readFully(inBuf, 0, frames * frameBytes)
                    var p = 0
                    for (f in 0 until frames) {
                        var lvl = 0f
                        for (c in 0 until ch) {
                            val s = ((inBuf[p + 1].toInt() shl 8) or (inBuf[p].toInt() and 0xFF)).toShort().toInt()
                            p += 2
                            val x = s / 32768f
                            val y = if (opt.highPass) a * (py[c] + x - px[c]) else x
                            px[c] = x
                            py[c] = y
                            tmp[c] = y
                            lvl = max(lvl, abs(y))
                        }
                        env = if (lvl > env) lvl else env * 0.9995f
                        val target = if (opt.gate && env < GATE_THRESHOLD) GATE_FLOOR else 1f
                        gain += (target - gain) * 0.002f
                        var q = p - frameBytes
                        for (c in 0 until ch) {
                            val v = tmp[c] * gain
                            peak = max(peak, abs(v))
                            val o = (v * 32767f).toInt().coerceIn(-32768, 32767)
                            inBuf[q] = (o and 0xFF).toByte()
                            inBuf[q + 1] = ((o shr 8) and 0xFF).toByte()
                            q += 2
                        }
                    }
                    dst.write(inBuf, 0, frames * frameBytes)
                    done += frames
                    onProgress(0.7f * done / totalFrames)
                }
            }
        }

        // ---- pass 2: normalise in place ----
        if (opt.normalize && peak > 0.001f) {
            val scale = (TARGET_PEAK / peak).coerceAtMost(MAX_BOOST)
            if (abs(scale - 1f) > 0.02f) {
                RandomAccessFile(output, "rw").use { f ->
                    val buf = ByteArray(FRAMES_PER_CHUNK * frameBytes)
                    var pos = WavHeader.SIZE.toLong()
                    val end = WavHeader.SIZE + dataBytes
                    while (pos < end) {
                        val n = minOf(buf.size.toLong(), end - pos).toInt()
                        f.seek(pos)
                        f.readFully(buf, 0, n)
                        var i = 0
                        while (i + 1 < n) {
                            val s = ((buf[i + 1].toInt() shl 8) or (buf[i].toInt() and 0xFF)).toShort().toInt()
                            val o = (s * scale).toInt().coerceIn(-32768, 32767)
                            buf[i] = (o and 0xFF).toByte()
                            buf[i + 1] = ((o shr 8) and 0xFF).toByte()
                            i += 2
                        }
                        f.seek(pos)
                        f.write(buf, 0, n)
                        pos += n
                        onProgress(0.7f + 0.3f * (pos - WavHeader.SIZE) / dataBytes)
                    }
                }
            }
        }
        onProgress(1f)
        return true
    }
}
