package com.ultravoice.recorder.data

import android.content.Context
import com.ultravoice.recorder.audio.Enhancer
import com.ultravoice.recorder.audio.WavHeader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile

class RecordingRepository(private val context: Context, db: AppDatabase) {
    private val dao = db.recordingDao()

    fun dir(): File =
        (context.getExternalFilesDir("recordings") ?: File(context.filesDir, "recordings")).apply { mkdirs() }

    fun observe(trashed: Boolean): Flow<List<RecordingEntity>> = dao.observe(trashed)

    suspend fun insert(item: RecordingEntity): Long = dao.insert(item)

    suspend fun rename(r: RecordingEntity, name: String) {
        val clean = name.trim()
        if (clean.isNotEmpty()) dao.update(r.copy(name = clean))
    }

    suspend fun setNote(r: RecordingEntity, note: String) = dao.update(r.copy(note = note.trim()))
    suspend fun setFavorite(r: RecordingEntity, fav: Boolean) = dao.update(r.copy(favorite = fav))
    suspend fun trash(r: RecordingEntity) = dao.update(r.copy(trashed = true))
    suspend fun restore(r: RecordingEntity) = dao.update(r.copy(trashed = false))

    suspend fun deleteForever(r: RecordingEntity) = withContext(Dispatchers.IO) {
        File(r.path).delete()
        dao.deleteById(r.id)
    }

    suspend fun emptyTrash() = withContext(Dispatchers.IO) {
        dao.trashedNow().forEach { File(it.path).delete(); dao.deleteById(it.id) }
    }

    /**
     * Finds WAV files in the recordings folder that are not in the database
     * (for example after the process was killed mid-recording) and adds them back.
     * The WAV header is refreshed every second while recording, and is repaired here from the file length.
     */
    suspend fun recoverOrphans() = withContext(Dispatchers.IO) {
        val known = dao.paths().toSet()
        val files = dir().listFiles { f -> f.isFile && f.extension.equals("wav", ignoreCase = true) } ?: return@withContext
        for (f in files) {
            if (f.absolutePath in known || f.length() <= WavHeader.SIZE) continue
            val head = ByteArray(WavHeader.SIZE)
            val info = runCatching {
                RandomAccessFile(f, "r").use { it.readFully(head) }
                WavHeader.parse(head)
            }.getOrNull() ?: continue
            val frame = info.channels * 2
            val data = (f.length() - WavHeader.SIZE) / frame * frame
            runCatching {
                RandomAccessFile(f, "rw").use {
                    it.write(WavHeader.build(data, info.sampleRate, info.channels))
                    it.setLength(WavHeader.SIZE + data)
                }
            }
            dao.insert(
                RecordingEntity(
                    path = f.absolutePath,
                    name = f.nameWithoutExtension,
                    createdAt = f.lastModified(),
                    durationMs = data * 1000L / (info.sampleRate.toLong() * frame),
                    sizeBytes = f.length(),
                    sampleRate = info.sampleRate,
                    channels = info.channels,
                )
            )
        }
    }

    /** Non-destructive: the original stays untouched; a new "_enhanced" file and DB row are created. */
    suspend fun enhance(r: RecordingEntity, onProgress: (Float) -> Unit = {}): Boolean = withContext(Dispatchers.IO) {
        val src = File(r.path)
        if (!src.exists()) return@withContext false
        var out = File(dir(), src.nameWithoutExtension + "_enhanced.wav")
        var n = 2
        while (out.exists()) { out = File(dir(), src.nameWithoutExtension + "_enhanced$n.wav"); n++ }
        val ok = runCatching { Enhancer.process(src, out, onProgress = onProgress) }.getOrDefault(false)
        if (!ok) { out.delete(); return@withContext false }
        dao.insert(
            r.copy(
                id = 0,
                path = out.absolutePath,
                name = r.name + " (enhanced)",
                createdAt = System.currentTimeMillis(),
                sizeBytes = out.length(),
                type = RecordingType.ENHANCED,
                favorite = false,
                note = "",
                trashed = false,
            )
        )
        true
    }
}
