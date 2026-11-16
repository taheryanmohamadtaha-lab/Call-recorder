package com.ultravoice.recorder

import com.ultravoice.recorder.audio.WavHeader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class WavHeaderTest {
    @Test
    fun headerHasCorrectSizeAndFields() {
        val h = WavHeader.build(dataBytes = 88200, sampleRate = 44100, channels = 1)
        assertEquals(44, h.size)
        val b = ByteBuffer.wrap(h).order(ByteOrder.LITTLE_ENDIAN)
        assertEquals("RIFF", String(h, 0, 4))
        assertEquals("WAVE", String(h, 8, 4))
        assertEquals(36 + 88200, b.getInt(4))
        assertEquals(44100, b.getInt(24))
        assertEquals(88200, b.getInt(28)) // byte rate for 16-bit mono
        assertEquals(88200, b.getInt(40))
    }

    @Test
    fun parseRoundTrips() {
        val info = WavHeader.parse(WavHeader.build(1000, 48000, 2))
        assertNotNull(info)
        assertEquals(48000, info!!.sampleRate)
        assertEquals(2, info.channels)
        assertEquals(16, info.bitsPerSample)
    }

    @Test
    fun parseRejectsGarbage() {
        assertNull(WavHeader.parse(ByteArray(44)))
        assertNull(WavHeader.parse(ByteArray(10)))
    }
}
