package com.ultravoice.recorder.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ultravoice.recorder.R
import com.ultravoice.recorder.data.RecordingType

/**
 * Honest call-recording page: shows what this device/OS can and cannot do.
 * Android (since 10) does not let normal apps capture the voice-call audio stream, and this app
 * does not try to bypass that. It offers (1) call start/end time logging and
 * (2) consent-based speaker-mode recording through the microphone.
 */
@Composable
fun CallsScreen(vm: MainViewModel) {
    val p = Uv.palette
    val ctx = LocalContext.current
    val calls by vm.calls.collectAsStateWithLifecycle()
    var phoneGranted by remember { mutableStateOf(hasPermission(ctx, Manifest.permission.READ_PHONE_STATE)) }
    val phoneLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { phoneGranted = it }
    var consent by rememberSaveable { mutableStateOf(false) }
    val start = rememberRecordStarter()

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.calls_title), fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
        Text(stringResource(R.string.calls_device, Build.MANUFACTURER + " " + Build.MODEL, Build.VERSION.RELEASE), color = p.muted, fontSize = 12.sp)

        Card {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, null, tint = p.danger, modifier = Modifier.size(20.dp))
                Spacer(Modifier.size(8.dp))
                Text(stringResource(R.string.calls_support_title), fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(if (Build.VERSION.SDK_INT >= 29) R.string.calls_unsupported else R.string.calls_supported_note),
                color = p.muted, fontSize = 13.sp,
            )
        }

        Card {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, null, tint = p.accent, modifier = Modifier.size(20.dp))
                Spacer(Modifier.size(8.dp))
                Text(stringResource(R.string.calls_start_speaker), fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(6.dp))
            Text(stringResource(R.string.calls_alternative), color = p.muted, fontSize = 13.sp)
            Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = consent, onCheckedChange = { consent = it })
                Text(stringResource(R.string.calls_consent), fontSize = 13.sp)
            }
            Button(onClick = { start(RecordingType.CALL_SPEAKER) }, enabled = consent) {
                Text(stringResource(R.string.calls_start_speaker))
            }
        }

        Card {
            Text(stringResource(R.string.calls_log), fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            if (!phoneGranted) {
                Text(stringResource(R.string.calls_grant_phone), color = p.muted, fontSize = 13.sp)
                OutlinedButton(onClick = { phoneLauncher.launch(Manifest.permission.READ_PHONE_STATE) }, modifier = Modifier.padding(top = 8.dp)) {
                    Text(stringResource(R.string.ok))
                }
            } else {
                Text(stringResource(R.string.calls_perm_granted), color = p.accent, fontSize = 12.sp)
            }
            Spacer(Modifier.height(8.dp))
            if (calls.isEmpty()) {
                Text(stringResource(R.string.calls_log_empty), color = p.muted, fontSize = 13.sp)
            } else {
                calls.take(30).forEach { c ->
                    Text(
                        formatDate(c.startedAt) + "  ·  " + formatDuration(c.endedAt - c.startedAt),
                        fontSize = 13.sp, modifier = Modifier.padding(vertical = 2.dp),
                    )
                }
                TextButton(onClick = { vm.clearCallLog() }) { Text(stringResource(R.string.calls_clear_log), color = p.danger) }
            }
        }
    }
}

@Composable
private fun Card(content: @Composable () -> Unit) {
    val p = Uv.palette
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(p.surface).padding(14.dp)) { content() }
}
