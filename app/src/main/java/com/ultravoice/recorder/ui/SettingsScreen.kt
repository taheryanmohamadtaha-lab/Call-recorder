package com.ultravoice.recorder.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ultravoice.recorder.BuildConfig
import com.ultravoice.recorder.R
import com.ultravoice.recorder.data.MAX_MINUTES_OPTIONS
import com.ultravoice.recorder.data.QualityProfile
import com.ultravoice.recorder.data.SAMPLE_RATES
import com.ultravoice.recorder.data.ThemeMode

@Composable
fun SettingsScreen(vm: MainViewModel) {
    val p = Uv.palette
    val s by vm.settings.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(stringResource(R.string.tab_settings), fontSize = 24.sp, fontWeight = FontWeight.SemiBold)

        Section(stringResource(R.string.settings_quality)) {
            Text(stringResource(R.string.settings_presets), color = p.muted, fontSize = 12.sp)
            val labels = mapOf(
                QualityProfile.STANDARD to R.string.profile_standard,
                QualityProfile.HIGH to R.string.profile_high,
                QualityProfile.STUDIO to R.string.profile_studio,
                QualityProfile.LOSSLESS to R.string.profile_lossless,
            )
            QualityProfile.entries.forEach { prof ->
                Row(
                    Modifier.fillMaxWidth().selectable(selected = s.profile == prof, role = Role.RadioButton) { vm.applyProfile(prof) },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = s.profile == prof, onClick = null)
                    Text(stringResource(labels.getValue(prof)), Modifier.padding(start = 10.dp, top = 8.dp, bottom = 8.dp), fontSize = 14.sp)
                }
            }
            Text(stringResource(R.string.settings_custom), color = p.muted, fontSize = 12.sp)
            Text(stringResource(R.string.settings_sample_rate), fontSize = 13.sp)
            ChipRow {
                SAMPLE_RATES.forEach { r ->
                    FilterChip(
                        selected = s.sampleRate == r, onClick = { vm.setSampleRate(r) },
                        label = { Text(stringResource(R.string.khz_fmt, (r / 1000.0).toString().removeSuffix(".0"))) },
                    )
                }
            }
            Text(stringResource(R.string.settings_channels), fontSize = 13.sp)
            ChipRow {
                FilterChip(selected = s.channels == 1, onClick = { vm.setChannels(1) }, label = { Text(stringResource(R.string.mono)) })
                FilterChip(selected = s.channels == 2, onClick = { vm.setChannels(2) }, label = { Text(stringResource(R.string.stereo)) })
            }
        }

        Section(stringResource(R.string.settings_max_duration)) {
            ChipRow {
                MAX_MINUTES_OPTIONS.forEach { m ->
                    FilterChip(
                        selected = s.maxMinutes == m, onClick = { vm.setMaxMinutes(m) },
                        label = { Text(if (m == 0) stringResource(R.string.unlimited) else stringResource(R.string.minutes_fmt, m)) },
                    )
                }
            }
        }

        Section(stringResource(R.string.settings_theme)) {
            ChipRow {
                listOf(ThemeMode.SYSTEM to R.string.theme_system, ThemeMode.DARK to R.string.theme_dark, ThemeMode.LIGHT to R.string.theme_light)
                    .forEach { (mode, res) ->
                        FilterChip(selected = s.theme == mode, onClick = { vm.setTheme(mode) }, label = { Text(stringResource(res)) })
                    }
            }
        }

        Section(stringResource(R.string.storage_location)) {
            Text(vm.recordingsDir, color = p.muted, fontSize = 12.sp)
        }

        Section(stringResource(R.string.settings_about)) {
            Text(stringResource(R.string.about_text), fontSize = 13.sp, color = p.muted)
            Text(stringResource(R.string.version_fmt, BuildConfig.VERSION_NAME), fontSize = 12.sp, color = p.muted)
            Text(stringResource(R.string.developer, "محمدطاها طاهریان"), fontSize = 12.sp, color = p.muted)
        }
    }
}

@Composable
private fun ChipRow(content: @Composable () -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) { content() }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    val p = Uv.palette
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(p.surface).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(title, fontWeight = FontWeight.SemiBold)
        content()
    }
}
