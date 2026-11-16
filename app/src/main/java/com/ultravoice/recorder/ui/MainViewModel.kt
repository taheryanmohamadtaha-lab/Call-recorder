package com.ultravoice.recorder.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ultravoice.recorder.R
import com.ultravoice.recorder.UltraApp
import com.ultravoice.recorder.data.QualityProfile
import com.ultravoice.recorder.data.RecordingEntity
import com.ultravoice.recorder.data.Settings
import com.ultravoice.recorder.data.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val ua = app as UltraApp
    private val repo = ua.repo
    private val store = ua.settingsStore

    val settings: StateFlow<Settings> = store.flow.stateIn(viewModelScope, SharingStarted.Eagerly, Settings())
    val recordings = repo.observe(false).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val trash = repo.observe(true).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val calls = ua.db.callLogDao().observe().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    var enhancing by mutableStateOf(false); private set
    var enhanceProgress by mutableFloatStateOf(0f); private set
    var message by mutableStateOf<Int?>(null); private set

    fun clearMessage() { message = null }
    fun showMessage(res: Int) { message = res }

    val recordingsDir: String get() = repo.dir().absolutePath

    // settings
    fun applyProfile(p: QualityProfile) = viewModelScope.launch { store.setQuality(p.sampleRate, p.channels) }
    fun setSampleRate(v: Int) = viewModelScope.launch { store.setSampleRate(v) }
    fun setChannels(v: Int) = viewModelScope.launch { store.setChannels(v) }
    fun setTheme(v: ThemeMode) = viewModelScope.launch { store.setTheme(v) }
    fun setMaxMinutes(v: Int) = viewModelScope.launch { store.setMaxMinutes(v) }

    // library
    fun rename(r: RecordingEntity, name: String) = viewModelScope.launch { repo.rename(r, name) }
    fun setNote(r: RecordingEntity, note: String) = viewModelScope.launch { repo.setNote(r, note) }
    fun toggleFavorite(r: RecordingEntity) = viewModelScope.launch { repo.setFavorite(r, !r.favorite) }
    fun trash(r: RecordingEntity) = viewModelScope.launch { repo.trash(r); message = R.string.moved_to_trash }
    fun restore(r: RecordingEntity) = viewModelScope.launch { repo.restore(r) }
    fun deleteForever(r: RecordingEntity) = viewModelScope.launch { repo.deleteForever(r) }
    fun emptyTrash() = viewModelScope.launch { repo.emptyTrash() }
    fun clearCallLog() = viewModelScope.launch { ua.db.callLogDao().clear() }

    fun trashMany(list: List<RecordingEntity>) = viewModelScope.launch { list.forEach { repo.trash(it) } }
    fun restoreMany(list: List<RecordingEntity>) = viewModelScope.launch { list.forEach { repo.restore(it) } }
    fun deleteMany(list: List<RecordingEntity>) = viewModelScope.launch { list.forEach { repo.deleteForever(it) } }

    fun enhance(r: RecordingEntity) {
        if (enhancing) return
        enhancing = true
        enhanceProgress = 0f
        viewModelScope.launch {
            val ok = repo.enhance(r) { enhanceProgress = it }
            enhancing = false
            message = if (ok) R.string.enhance_done else R.string.enhance_fail
        }
    }
}
