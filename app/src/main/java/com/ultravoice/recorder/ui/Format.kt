package com.ultravoice.recorder.ui

import java.text.DateFormat
import java.util.Date

/** 04:52 or 1:04:52 */
fun formatDuration(ms: Long): String {
    val total = (ms.coerceAtLeast(0L)) / 1000
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) String.format("%d:%02d:%02d", h, m, s) else String.format("%02d:%02d", m, s)
}

fun formatSize(bytes: Long): String = when {
    bytes < 1024L -> "$bytes B"
    bytes < 1024L * 1024 -> String.format("%.1f KB", bytes / 1024.0)
    bytes < 1024L * 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024))
    else -> String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024))
}

fun formatDate(ms: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(ms))

fun formatDay(ms: Long): String = DateFormat.getDateInstance(DateFormat.LONG).format(Date(ms))

fun formatTime(ms: Long): String = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(ms))

fun dayKey(ms: Long): Int {
    val c = java.util.Calendar.getInstance()
    c.timeInMillis = ms
    return c.get(java.util.Calendar.YEAR) * 1000 + c.get(java.util.Calendar.DAY_OF_YEAR)
}
