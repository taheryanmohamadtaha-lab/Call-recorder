package com.ultravoice.recorder.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.ultravoice.recorder.R
import com.ultravoice.recorder.data.RecordingType
import com.ultravoice.recorder.services.RecordingService
import java.io.File

fun hasPermission(ctx: Context, perm: String): Boolean =
    ContextCompat.checkSelfPermission(ctx, perm) == PackageManager.PERMISSION_GRANTED

/** Returns a function that asks for the needed permissions and then starts a recording of the given type. */
@Composable
fun rememberRecordStarter(): (String) -> Unit {
    val ctx = LocalContext.current
    var pending by remember { mutableStateOf(RecordingType.AMBIENT) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        if (hasPermission(ctx, Manifest.permission.RECORD_AUDIO)) {
            RecordingService.start(ctx, pending)
        } else {
            Toast.makeText(ctx, R.string.perm_mic_needed, Toast.LENGTH_LONG).show()
        }
    }
    return { type ->
        pending = type
        val needed = buildList {
            add(Manifest.permission.RECORD_AUDIO)
            if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
        }.filter { !hasPermission(ctx, it) }
        if (needed.isEmpty()) RecordingService.start(ctx, type) else launcher.launch(needed.toTypedArray())
    }
}

fun shareRecording(ctx: Context, path: String) {
    val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".fileprovider", File(path))
    val send = Intent(Intent.ACTION_SEND)
        .setType("audio/wav")
        .putExtra(Intent.EXTRA_STREAM, uri)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    ctx.startActivity(Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

@Composable
fun BottomNav(selected: Int, onSelect: (Int) -> Unit) {
    val p = Uv.palette
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val labels = listOf(R.string.tab_record, R.string.tab_archive, R.string.tab_calls, R.string.tab_settings)
        labels.forEachIndexed { i, res ->
            val on = i == selected
            val tint = if (on) p.accent else p.muted
            val desc = stringResource(res)
            Box(
                Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (on) p.accent.copy(alpha = 0.18f) else androidx.compose.ui.graphics.Color.Transparent)
                    .clickable(role = Role.Tab, onClickLabel = desc) { onSelect(i) },
                contentAlignment = Alignment.Center,
            ) {
                val m = Modifier.size(24.dp)
                when (i) {
                    0 -> MicIcon(tint, m)
                    1 -> FolderIcon(tint, m)
                    2 -> PhoneIcon(tint, m)
                    else -> Icon(Icons.Default.Settings, contentDescription = desc, tint = tint, modifier = m)
                }
            }
        }
    }
}
