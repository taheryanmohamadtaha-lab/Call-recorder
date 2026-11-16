package com.ultravoice.recorder.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordingDao {
    @Query("SELECT * FROM recordings WHERE trashed = :trashed ORDER BY createdAt DESC")
    fun observe(trashed: Boolean): Flow<List<RecordingEntity>>

    @Query("SELECT * FROM recordings WHERE trashed = 1")
    suspend fun trashedNow(): List<RecordingEntity>

    @Query("SELECT path FROM recordings")
    suspend fun paths(): List<String>

    @Insert
    suspend fun insert(item: RecordingEntity): Long

    @Update
    suspend fun update(item: RecordingEntity)

    @Query("DELETE FROM recordings WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface CallLogDao {
    @Query("SELECT * FROM call_log ORDER BY startedAt DESC LIMIT 100")
    fun observe(): Flow<List<CallLogEntity>>

    @Insert
    suspend fun insert(item: CallLogEntity)

    @Query("DELETE FROM call_log")
    suspend fun clear()
}
