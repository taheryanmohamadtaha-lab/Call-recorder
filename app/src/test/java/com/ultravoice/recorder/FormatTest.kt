package com.ultravoice.recorder

import com.ultravoice.recorder.ui.formatDuration
import com.ultravoice.recorder.ui.formatSize
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.Locale

class FormatTest {
    @Before fun setLocale() { Locale.setDefault(Locale.US) }

    @Test fun durationUnderAnHour() {
        assertEquals("00:00", formatDuration(0))
        assertEquals("04:52", formatDuration(292_000))
    }

    @Test fun durationOverAnHour() {
        assertEquals("1:01:01", formatDuration(3_661_000))
    }

    @Test fun negativeDurationIsZero() {
        assertEquals("00:00", formatDuration(-5000))
    }

    @Test fun sizes() {
        assertEquals("512 B", formatSize(512))
        assertEquals("1.5 KB", formatSize(1536))
        assertEquals("2.0 MB", formatSize(2L * 1024 * 1024))
    }
}
