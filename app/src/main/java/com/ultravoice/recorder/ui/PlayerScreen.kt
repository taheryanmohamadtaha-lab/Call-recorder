package com.ultravoice.recorder.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ultravoice.recorder.R
import com.ultravoice.recorder.audio.PlayerController
import com.ultravoice.recorder.audio.WaveformExtractor
import com.ultravoice.recorder.data.RecordingType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun PlayerScreen(vm: MainViewModel, id: Long, onClose: () -> Unit) {
    val active by vm.recordings.collectAsStateWithLifecycle()
    val trashed by vm.trash.collectAsStateWithLifecycle()
    val r = (active + trashed).firstOrNull { it.id == id }
    if (r == null) {
        // Removed (e.g. deleted forever) – leave the screen.
        LaunchedEffect(Unit) { onClose() }
        return
    }
    val p = Uv.palette
    val ctx = LocalContext.current
    val controller = remember(r.path) { PlayerController(r.path) }
    DisposableEffect(controller) { onDispose { controller.release() } }
    LaunchedEffect(controller) {
        while (true) { controller.tick(); delay(100) }
    }
    var peaks by remember(r.path) { mutableStateOf(FloatArray(120)) }
    LaunchedEffect(r.path) {
        peaks = withContext(Dispatchers.IO) { runCatching { WaveformExtractor.peaks(File(r.path), 120) }.getOrDefault(FloatArray(120)) }
    }
    var showRename by remember { mutableStateOf(false) }
    var showNote by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().systemBarsPadding().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) { Icon(Icons.Default.Close, stringResource(R.string.close)) }
            Text(r.name, Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
            IconButton(onClick = { showRename = true }) { Icon(Icons.Default.Edit, stringResource(R.string.rename)) }
        }
        val typeRes = when (r.type) {
            RecordingType.CALL_SPEAKER -> R.string.type_call
            RecordingType.ENHANCED -> R.string.type_enhanced
            else -> R.string.type_ambient
        }
        val ch = stringResource(if (r.channels == 2) R.string.stereo else R.string.mono)
        Text(
            formatDate(r.createdAt) + " · " + formatSize(r.sizeBytes) + " · " + stringResource(typeRes),
            color = p.muted, fontSize = 12.sp, modifier = Modifier.padding(start = 12.dp),
        )
        Text(
            stringResource(R.string.info_format, r.sampleRate, ch, "WAV 16-bit"),
            color = p.muted, fontSize = 12.sp, modifier = Modifier.padding(start = 12.dp),
        )
        Spacer(Modifier.height(20.dp))

        if (controller.failed) {
            Text(stringResource(R.string.unavailable_file), color = p.danger, modifier = Modifier.padding(12.dp))
        } else {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                SeekWaveform(
                    peaks = peaks,
                    progress = if (controller.durationMs > 0) controller.positionMs.toFloat() / controller.durationMs else 0f,
                    onSeek = { controller.seekFraction(it) },
                    modifier = Modifier.fillMaxWidth().height(130.dp).clip(RoundedCornerShape(16.dp)).background(p.surface),
                )
                Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(formatDuration(controller.positionMs), color = p.onBg, fontSize = 13.sp)
                    Text(stringResource(R.string.remaining, formatDuration(controller.durationMs - controller.positionMs)), color = p.muted, fontSize = 13.sp)
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                SkipButton("−30") { controller.skip(-30_000) }
                SkipButton("−10") { controller.skip(-10_000) }
                SkipButton("−5") { controller.skip(-5_000) }
                val label = stringResource(R.string.play)
                Box(
                    Modifier.size(64.dp).clip(CircleShape).background(Color.White)
                        .clickable(role = Role.Button, onClickLabel = label) { controller.toggle() },
                    contentAlignment = Alignment.Center,
                ) {
                    if (controller.isPlaying) PauseIcon(Color(0xFF111A2E), Modifier.size(30.dp)) else PlayIcon(Color(0xFF111A2E), Modifier.size(30.dp))
                }
                SkipButton("+5") { controller.skip(5_000) }
                SkipButton("+10") { controller.skip(10_000) }
                SkipButton("+30") { controller.skip(30_000) }
            }
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.speed), color = p.muted, fontSize = 12.sp)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f).forEach { s ->
                    FilterChip(
                        selected = controller.speed == s, onClick = { controller.setSpeedValue(s) },
                        label = { Text(stringResource(R.string.speed_fmt, s.toString().removeSuffix(".0"))) },
                    )
                }
            }
            Text(stringResource(R.string.volume), color = p.muted, fontSize = 12.sp)
            Slider(value = controller.volume, onValueChange = { controller.setVolumeValue(it) })
        }

        Spacer(Modifier.height(8.dp))
        if (r.note.isNotBlank()) {
            Text(r.note, Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(p.surface).padding(12.dp))
            Spacer(Modifier.height(8.dp))
        }
        if (r.trashed) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                OutlinedButton(onClick = { vm.restore(r) }) {
                    Icon(Icons.Default.Refresh, null, Modifier.size(18.dp)); Spacer(Modifier.size(6.dp)); Text(stringResource(R.string.restore))
                }
                OutlinedButton(onClick = { confirmDelete = true }) {
                    Icon(Icons.Default.Delete, null, Modifier.size(18.dp), tint = p.danger); Spacer(Modifier.size(6.dp))
                    Text(stringResource(R.string.delete_forever), color = p.danger)
                }
            }
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                IconButton(onClick = { vm.toggleFavorite(r) }) {
                    Icon(
                        if (r.favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        stringResource(R.string.favorite), tint = if (r.favorite) p.danger else p.onBg,
                    )
                }
                IconButton(onClick = { showNote = true }) { Icon(Icons.Default.Edit, stringResource(R.string.note)) }
                IconButton(onClick = { runCatching { shareRecording(ctx, r.path) } }) { Icon(Icons.Default.Share, stringResource(R.string.share)) }
                IconButton(onClick = { vm.trash(r); onClose() }) { Icon(Icons.Default.Delete, stringResource(R.string.delete), tint = p.danger) }
            }
            Spacer(Modifier.height(12.dp))
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(p.surface).padding(14.dp)) {
                Text(stringResource(R.string.enhance), fontWeight = FontWeight.SemiBold)
                Text(stringResource(R.string.enhance_desc), color = p.muted, fontSize = 12.sp, modifier = Modifier.padding(vertical = 6.dp))
                if (vm.enhancing) {
                    Text(stringResource(R.string.enhancing), fontSize = 12.sp)
                    LinearProgressIndicator(progress = { vm.enhanceProgress }, Modifier.fillMaxWidth().padding(top = 6.dp))
                } else {
                    OutlinedButton(onClick = { vm.enhance(r) }) { Text(stringResource(R.string.enhance)) }
                }
            }
        }
    }

    if (showRename) TextDialog(stringResource(R.string.rename), r.name, singleLine = true, onDismiss = { showRename = false }) {
        vm.rename(r, it); showRename = false
    }
    if (showNote) TextDialog(stringResource(R.string.note), r.note, singleLine = false, hint = stringResource(R.string.note_hint), onDismiss = { showNote = false }) {
        vm.setNote(r, it); showNote = false
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.delete_forever)) },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; vm.deleteForever(r) }) { Text(stringResource(R.string.delete_forever), color = p.danger) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
private fun SkipButton(label: String, onClick: () -> Unit) {
    val p = Uv.palette
    Box(
        Modifier.size(width = 44.dp, height = 36.dp).clip(RoundedCornerShape(50)).background(p.surfaceHi).clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(label, fontSize = 12.sp) }
}

@Composable
fun TextDialog(
    title: String,
    initial: String,
    singleLine: Boolean,
    hint: String = "",
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text, onValueChange = { text = it }, singleLine = singleLine,
                placeholder = { if (hint.isNotEmpty()) Text(hint) },
            )
        },
        confirmButton = { TextButton(onClick = { onConfirm(text) }) { Text(stringResource(R.string.ok)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
