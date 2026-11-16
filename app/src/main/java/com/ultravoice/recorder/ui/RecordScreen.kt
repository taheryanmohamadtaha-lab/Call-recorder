package com.ultravoice.recorder.ui

import android.widget.Toast
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ultravoice.recorder.R
import com.ultravoice.recorder.audio.RecStatus
import com.ultravoice.recorder.audio.RecorderState
import com.ultravoice.recorder.data.RecordingType
import com.ultravoice.recorder.services.RecordingService

@Composable
fun RecordScreen(vm: MainViewModel) {
    val ctx = LocalContext.current
    val status by RecorderState.status.collectAsStateWithLifecycle()
    val saved by RecorderState.saved.collectAsStateWithLifecycle()
    var seenSaved by remember { mutableIntStateOf(saved) }
    LaunchedEffect(saved) {
        if (saved != seenSaved) {
            seenSaved = saved
            Toast.makeText(ctx, R.string.saved_toast, Toast.LENGTH_SHORT).show()
        }
    }
    if (status == RecStatus.IDLE) IdleRecord(vm) else ActiveRecord(status)
}

@Composable
private fun IdleRecord(vm: MainViewModel) {
    val p = Uv.palette
    val start = rememberRecordStarter()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val error by RecorderState.error.collectAsStateWithLifecycle()
    val pulse = rememberInfiniteTransition(label = "pulse")
    val scale by pulse.animateFloat(
        initialValue = 1f, targetValue = 1.07f,
        animationSpec = infiniteRepeatable(tween(1600), RepeatMode.Reverse), label = "scale",
    )
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        val label = stringResource(R.string.cd_record)
        Box(
            Modifier.size(170.dp).scale(scale).clip(CircleShape).background(p.surface),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .size(98.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .clickable(role = Role.Button, onClickLabel = label) { start(RecordingType.AMBIENT) },
                contentAlignment = Alignment.Center,
            ) {
                MicIcon(Color(0xFF111A2E), Modifier.size(44.dp))
            }
        }
        Spacer(Modifier.height(28.dp))
        Text(stringResource(R.string.tap_to_record), color = p.onBg, fontSize = 18.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(6.dp))
        val ch = stringResource(if (settings.channels == 2) R.string.stereo else R.string.mono)
        Text(stringResource(R.string.format_info, settings.sampleRate, ch), color = p.muted, fontSize = 13.sp)
        error?.let {
            Spacer(Modifier.height(20.dp))
            Text(it, color = p.danger, textAlign = TextAlign.Center)
            TextButton(onClick = { RecorderState.clearError() }) { Text(stringResource(R.string.ok)) }
        }
    }
}

@Composable
private fun ActiveRecord(status: RecStatus) {
    val p = Uv.palette
    val elapsed by RecorderState.elapsedMs.collectAsStateWithLifecycle()
    val size by RecorderState.sizeBytes.collectAsStateWithLifecycle()
    val levels by RecorderState.levels.collectAsStateWithLifecycle()
    val rate by RecorderState.sampleRate.collectAsStateWithLifecycle()
    val channels by RecorderState.channels.collectAsStateWithLifecycle()
    val error by RecorderState.error.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    var confirmDiscard by remember { mutableStateOf(false) }
    val paused = status == RecStatus.PAUSED

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).fillMaxWidth().padding(top = 32.dp), verticalArrangement = Arrangement.Center) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                LiveWaveform(levels, Modifier.fillMaxWidth().height(220.dp).padding(horizontal = 12.dp))
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    for (k in 0..4) {
                        Text(formatDuration(elapsed - (4 - k) * 1000L), color = p.muted, fontSize = 10.sp)
                    }
                }
            }
        }
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)).background(p.surface).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                stringResource(if (paused) R.string.paused else R.string.recording),
                color = if (paused) p.muted else p.danger, fontSize = 13.sp,
            )
            Text(formatDuration(elapsed), fontSize = 46.sp, fontWeight = FontWeight.Light, color = p.onBg)
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { if (paused) 0f else (levels.lastOrNull() ?: 0f) },
                modifier = Modifier.fillMaxWidth(0.7f).height(4.dp).clip(CircleShape),
                color = p.accent, trackColor = p.surfaceHi,
            )
            Spacer(Modifier.height(20.dp))
            val toggleLabel = stringResource(if (paused) R.string.resume else R.string.pause)
            Box(
                Modifier
                    .size(72.dp).clip(CircleShape).background(p.surfaceHi)
                    .clickable(role = Role.Button, onClickLabel = toggleLabel) {
                        RecordingService.send(ctx, if (paused) RecordingService.ACTION_RESUME else RecordingService.ACTION_PAUSE)
                    },
                contentAlignment = Alignment.Center,
            ) {
                if (paused) PlayIcon(p.onBg, Modifier.size(32.dp)) else PauseIcon(p.onBg, Modifier.size(32.dp))
            }
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoChip(formatSize(size))
                InfoChip(stringResource(R.string.khz_fmt, (rate / 1000.0).toString().removeSuffix(".0")))
                InfoChip(stringResource(if (channels == 2) R.string.stereo else R.string.mono))
            }
            error?.let {
                Spacer(Modifier.height(10.dp))
                Text(it, color = p.danger, fontSize = 12.sp, textAlign = TextAlign.Center)
            }
            Spacer(Modifier.height(22.dp))
            Button(
                onClick = { RecordingService.send(ctx, RecordingService.ACTION_SAVE) },
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = p.buttonLight, contentColor = p.onButtonLight),
                modifier = Modifier.width(180.dp).height(46.dp),
            ) { Text(stringResource(R.string.save), fontWeight = FontWeight.SemiBold) }
            TextButton(onClick = { confirmDiscard = true }) {
                Text(stringResource(R.string.delete), color = p.danger)
            }
        }
    }

    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text(stringResource(R.string.discard_title)) },
            text = { Text(stringResource(R.string.discard_msg)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDiscard = false
                    RecordingService.send(ctx, RecordingService.ACTION_DISCARD)
                }) { Text(stringResource(R.string.delete), color = p.danger) }
            },
            dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
private fun InfoChip(text: String) {
    val p = Uv.palette
    Box(Modifier.clip(RoundedCornerShape(50)).background(p.surfaceHi).padding(horizontal = 12.dp, vertical = 6.dp)) {
        Text(text, color = p.onBg, fontSize = 12.sp)
    }
}
