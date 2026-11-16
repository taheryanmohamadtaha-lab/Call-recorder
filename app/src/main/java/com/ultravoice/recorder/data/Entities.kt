package com.ultravoice.recorder.data

import androidx.room.Entity
import androidx.room.PrimaryKey

object RecordingType {
    const val AMBIENT = "ambient"
    const val CALL_SPEAKER = "call_speaker"
    const val ENHANCED = "enhanced"
}

@Entity(tableName = "recordings")
data class RecordingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val path: String,
    val name: String,
    val createdAt: Long,
    val durationMs: Long,
    val sizeBytes: Long,
    val sampleRate: Int,
    val channels: Int,
    val type: String = RecordingType.AMBIENT,
    val favorite: Boolean = false,
    val note: String = "",
    val trashed: Boolean = false,
)

@Entity(tableName = "call_log")
data class CallLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startedAt: Long,
    val endedAt: Long,
)
