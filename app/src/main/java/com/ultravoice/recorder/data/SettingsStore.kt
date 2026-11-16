package com.ultravoice.recorder.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

/** Presets that set sample rate + channel count together. All output is 16-bit PCM WAV. */
enum class QualityProfile(val sampleRate: Int, val channels: Int) {
    STANDARD(16000, 1),
    HIGH(44100, 1),
    STUDIO(48000, 2),
    LOSSLESS(48000, 1),
}

enum class ThemeMode { SYSTEM, DARK, LIGHT }

val SAMPLE_RATES = listOf(16000, 22050, 44100, 48000)
val MAX_MINUTES_OPTIONS = listOf(0, 30, 60, 120, 240)

data class Settings(
    val sampleRate: Int = 44100,
    val channels: Int = 1,
    val theme: ThemeMode = ThemeMode.DARK,
    val maxMinutes: Int = 0, // 0 = unlimited
) {
    /** The matching preset, or null when the user customised the values. */
    val profile: QualityProfile?
        get() = QualityProfile.entries.firstOrNull { it.sampleRate == sampleRate && it.channels == channels }
}

class SettingsStore(private val context: Context) {
    private val kRate = intPreferencesKey("sample_rate")
    private val kChannels = intPreferencesKey("channels")
    private val kTheme = stringPreferencesKey("theme")
    private val kMax = intPreferencesKey("max_minutes")

    val flow: Flow<Settings> = context.dataStore.data.map { p ->
        Settings(
            sampleRate = (p[kRate] ?: 44100).let { if (it in SAMPLE_RATES) it else 44100 },
            channels = if (p[kChannels] == 2) 2 else 1,
            theme = p[kTheme]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.DARK,
            maxMinutes = p[kMax] ?: 0,
        )
    }

    suspend fun current(): Settings = flow.first()

    suspend fun setQuality(sampleRate: Int, channels: Int) = context.dataStore.edit {
        it[kRate] = sampleRate
        it[kChannels] = channels
    }
    suspend fun setSampleRate(v: Int) = context.dataStore.edit { it[kRate] = v }
    suspend fun setChannels(v: Int) = context.dataStore.edit { it[kChannels] = v }
    suspend fun setTheme(v: ThemeMode) = context.dataStore.edit { it[kTheme] = v.name }
    suspend fun setMaxMinutes(v: Int) = context.dataStore.edit { it[kMax] = v }
}
