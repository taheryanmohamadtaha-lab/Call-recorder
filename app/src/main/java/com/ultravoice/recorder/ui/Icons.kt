package com.ultravoice.recorder.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke

/** Hand-drawn icons so the app has no dependency on the extended Material icon set. */

@Composable
fun MicIcon(color: Color, modifier: Modifier = Modifier) = Canvas(modifier) {
    val w = size.width
    val h = size.height
    val sw = w * 0.085f
    drawRoundRect(
        color, topLeft = Offset(w * 0.36f, h * 0.10f), size = Size(w * 0.28f, h * 0.46f),
        cornerRadius = CornerRadius(w * 0.14f),
    )
    drawArc(
        color, startAngle = 0f, sweepAngle = 180f, useCenter = false,
        topLeft = Offset(w * 0.24f, h * 0.26f), size = Size(w * 0.52f, h * 0.46f),
        style = Stroke(sw, cap = StrokeCap.Round),
    )
    drawLine(color, Offset(w * 0.5f, h * 0.72f), Offset(w * 0.5f, h * 0.88f), sw, StrokeCap.Round)
    drawLine(color, Offset(w * 0.37f, h * 0.88f), Offset(w * 0.63f, h * 0.88f), sw, StrokeCap.Round)
}

@Composable
fun PauseIcon(color: Color, modifier: Modifier = Modifier) = Canvas(modifier) {
    val w = size.width
    val h = size.height
    drawRoundRect(color, Offset(w * 0.28f, h * 0.2f), Size(w * 0.16f, h * 0.6f), CornerRadius(w * 0.04f))
    drawRoundRect(color, Offset(w * 0.56f, h * 0.2f), Size(w * 0.16f, h * 0.6f), CornerRadius(w * 0.04f))
}

@Composable
fun PlayIcon(color: Color, modifier: Modifier = Modifier) = Canvas(modifier) {
    val w = size.width
    val h = size.height
    val path = Path().apply {
        moveTo(w * 0.32f, h * 0.2f)
        lineTo(w * 0.8f, h * 0.5f)
        lineTo(w * 0.32f, h * 0.8f)
        close()
    }
    drawPath(path, color, style = Fill)
}

@Composable
fun StopIcon(color: Color, modifier: Modifier = Modifier) = Canvas(modifier) {
    val w = size.width
    val h = size.height
    drawRoundRect(color, Offset(w * 0.25f, h * 0.25f), Size(w * 0.5f, h * 0.5f), CornerRadius(w * 0.06f))
}

@Composable
fun FolderIcon(color: Color, modifier: Modifier = Modifier) = Canvas(modifier) {
    val w = size.width
    val h = size.height
    val path = Path().apply {
        moveTo(w * 0.12f, h * 0.28f)
        lineTo(w * 0.40f, h * 0.28f)
        lineTo(w * 0.48f, h * 0.38f)
        lineTo(w * 0.88f, h * 0.38f)
        lineTo(w * 0.88f, h * 0.76f)
        lineTo(w * 0.12f, h * 0.76f)
        close()
    }
    drawPath(path, color, style = Stroke(w * 0.07f, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

@Composable
fun PhoneIcon(color: Color, modifier: Modifier = Modifier) = Canvas(modifier) {
    val w = size.width
    val h = size.height
    val path = Path().apply {
        moveTo(w * 0.28f, h * 0.18f)
        lineTo(w * 0.40f, h * 0.18f)
        lineTo(w * 0.46f, h * 0.36f)
        lineTo(w * 0.38f, h * 0.44f)
        cubicTo(w * 0.44f, h * 0.56f, w * 0.50f, h * 0.62f, w * 0.60f, h * 0.68f)
        lineTo(w * 0.68f, h * 0.60f)
        lineTo(w * 0.84f, h * 0.68f)
        lineTo(w * 0.84f, h * 0.80f)
        cubicTo(w * 0.50f, h * 0.90f, w * 0.12f, h * 0.52f, w * 0.28f, h * 0.18f)
        close()
    }
    drawPath(path, color, style = Stroke(w * 0.07f, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

@Composable
fun GearIcon(color: Color, modifier: Modifier = Modifier) = Canvas(modifier) {
    val w = size.width
    val sw = w * 0.07f
    val c = Offset(w / 2, size.height / 2)
    drawCircle(color, radius = w * 0.16f, center = c, style = Stroke(sw))
    drawCircle(color, radius = w * 0.31f, center = c, style = Stroke(sw))
    for (i in 0 until 8) {
        val a = Math.toRadians(i * 45.0)
        val dx = Math.cos(a).toFloat()
        val dy = Math.sin(a).toFloat()
        drawLine(
            color, Offset(c.x + dx * w * 0.31f, c.y + dy * w * 0.31f),
            Offset(c.x + dx * w * 0.42f, c.y + dy * w * 0.42f), sw * 1.4f, StrokeCap.Round,
        )
    }
}
