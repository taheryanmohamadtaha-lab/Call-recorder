package com.ultravoice.recorder

import com.ultravoice.recorder.audio.Enhancer
import com.ultravoice.recorder.audio.WaveformExtractor
import com.ultravoice.recorder.audio.WavHeader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

class EnhancerTest {
    private fun writeSine(f: File, amp: Double, seconds: Int = 1, rate: Int = 16000) {
        val n = rate * seconds
        val bb = ByteBuffer.allocate(n * 2).order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until n) bb.putShort((amp * 32767 * sin(2 * PI * 440 * i / rate)).toInt().toShort())
        f.outputStream().use { it.write(WavHeader.build(n * 2L, rate, 1)); it.write(bb.array()) }
    }

    @Test
    fun quietToneIsNormalizedAndOriginalIsUntouched() {
        val dir = java.nio.file.Files.createTempDirectory("uvr").toFile()
        val src = File(dir, "a.wav")
        val out = File(dir, "a_enhanced.wav")
        writeSine(src, amp = 0.1)
        val before = src.readBytes()
        assertTrue(Enhancer.process(src, out))
        assertTrue(src.readBytes().contentEquals(before))
        assertEquals(src.length(), out.length())
        val peak = WaveformExtractor.peaks(out, 10).maxOrNull() ?: 0f
        assertTrue(peak > 0.9f) // normalised to full scale for display
        // actual sample peak should be near -1 dBFS (0.89)
        val bytes = out.readBytes()
        var max = 0
        var i = 44
        while (i + 1 < bytes.size) {
            val s = ((bytes[i + 1].toInt() shl 8) or (bytes[i].toInt() and 0xFF)).toShort().toInt()
            max = maxOf(max, abs(s))
            i += 2
        }
        assertTrue("peak=$max", max in 27000..30500)
        dir.deleteRecursively()
    }

    @Test
    fun rejectsNonWav() {
        val dir = java.nio.file.Files.createTempDirectory("uvr").toFile()
        val f = File(dir, "x.wav").apply { writeBytes(ByteArray(100)) }
        assertEquals(false, Enhancer.process(f, File(dir, "o.wav")))
        dir.deleteRecursively()
    }
}
