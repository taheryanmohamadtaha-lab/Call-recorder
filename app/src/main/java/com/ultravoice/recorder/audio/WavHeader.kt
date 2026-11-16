package com.ultravoice.recorder.audio

import java.nio.ByteBuffer
import java.nio.ByteOrder

data class WavInfo(val sampleRate: Int, val channels: Int, val bitsPerSample: Int)

/** Builds a canonical 44-byte PCM WAV header. */
object WavHeader {
    const val SIZE = 44

    fun build(dataBytes: Long, sampleRate: Int, channels: Int, bitsPerSample: Int = 16): ByteArray {
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val blockAlign = channels * bitsPerSample / 8
        val buf = ByteBuffer.allocate(SIZE).order(ByteOrder.LITTLE_ENDIAN)
        buf.put("RIFF".toByteArray(Charsets.US_ASCII))
        buf.putInt((36 + dataBytes).coerceAtMost(0xFFFFFFFFL).toInt())
        buf.put("WAVE".toByteArray(Charsets.US_ASCII))
        buf.put("fmt ".toByteArray(Charsets.US_ASCII))
        buf.putInt(16)
        buf.putShort(1) // PCM
        buf.putShort(channels.toShort())
        buf.putInt(sampleRate)
        buf.putInt(byteRate)
        buf.putShort(blockAlign.toShort())
        buf.putShort(bitsPerSample.toShort())
        buf.put("data".toByteArray(Charsets.US_ASCII))
        buf.putInt(dataBytes.coerceAtMost(0xFFFFFFFFL).toInt())
        return buf.array()
    }

    /** Reads format info from the first 44 bytes of a canonical PCM WAV file. */
    fun parse(header: ByteArray): WavInfo? {
        if (header.size < SIZE) return null
        val buf = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)
        val riff = String(header, 0, 4, Charsets.US_ASCII)
        val wave = String(header, 8, 4, Charsets.US_ASCII)
        if (riff != "RIFF" || wave != "WAVE") return null
        val channels = buf.getShort(22).toInt()
        val rate = buf.getInt(24)
        val bits = buf.getShort(34).toInt()
        if (channels !in 1..2 || rate <= 0 || bits != 16) return null
        return WavInfo(rate, channels, bits)
    }
}
