package com.ultravoice.recorder.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.ultravoice.recorder.audio.RecorderState

/** Live bars while recording; newest level is on the right next to the white playhead. */
@Composable
fun LiveWaveform(levels: List<Float>, modifier: Modifier = Modifier) {
    val p = Uv.palette
    Canvas(modifier) {
        val slots = RecorderState.MAX_LEVELS
        val step = size.width / slots
        val midY = size.height / 2
        val barW = step * 0.45f
        drawLine(p.muted.copy(alpha = 0.35f), Offset(0f, midY), Offset(size.width, midY), 1.dp.toPx())
        val offset = slots - levels.size
        levels.forEachIndexed { i, v ->
            val x = (offset + i) * step + step / 2
            val half = (v.coerceIn(0f, 1f) * (size.height / 2 - 6.dp.toPx())).coerceAtLeast(2.dp.toPx())
            drawLine(p.accent, Offset(x, midY - half), Offset(x, midY + half), barW, StrokeCap.Round)
        }
        drawLine(Color.White, Offset(size.width - 2.dp.toPx(), 0f), Offset(size.width - 2.dp.toPx(), size.height), 1.5.dp.toPx())
    }
}

/** Static waveform with a played portion and tap/drag-to-seek. */
@Composable
fun SeekWaveform(
    peaks: FloatArray,
    progress: Float,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val p = Uv.palette
    Canvas(
        modifier
            .pointerInput(Unit) { detectTapGestures { onSeek((it.x / size.width).coerceIn(0f, 1f)) } }
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, _ -> onSeek((change.position.x / size.width).coerceIn(0f, 1f)) }
            }
    ) {
        val n = peaks.size.coerceAtLeast(1)
        val step = size.width / n
        val midY = size.height / 2
        val barW = step * 0.55f
        for (i in 0 until n) {
            val v = if (peaks.isEmpty()) 0f else peaks[i]
            val half = (v * (size.height / 2 - 4.dp.toPx())).coerceAtLeast(2.dp.toPx())
            val x = i * step + step / 2
            val played = (i.toFloat() / n) < progress
            drawLine(if (played) p.accent else p.muted.copy(alpha = 0.5f), Offset(x, midY - half), Offset(x, midY + half), barW, StrokeCap.Round)
        }
        val px = size.width * progress.coerceIn(0f, 1f)
        drawLine(Color.White, Offset(px, 0f), Offset(px, size.height), 2.dp.toPx())
    }
}
