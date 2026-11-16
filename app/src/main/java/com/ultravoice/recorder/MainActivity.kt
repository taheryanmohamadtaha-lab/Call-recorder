package com.ultravoice.recorder

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ultravoice.recorder.audio.RecStatus
import com.ultravoice.recorder.audio.RecorderState
import com.ultravoice.recorder.ui.ArchiveScreen
import com.ultravoice.recorder.ui.BottomNav
import com.ultravoice.recorder.ui.CallsScreen
import com.ultravoice.recorder.ui.MainViewModel
import com.ultravoice.recorder.ui.PlayerScreen
import com.ultravoice.recorder.ui.RecordScreen
import com.ultravoice.recorder.ui.SettingsScreen
import com.ultravoice.recorder.ui.UltraTheme
import com.ultravoice.recorder.ui.Uv
import com.ultravoice.recorder.ui.isDarkFor

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { UltraRoot() }
    }
}

@Composable
fun UltraRoot(vm: MainViewModel = viewModel()) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    UltraTheme(dark = isDarkFor(settings.theme)) {
        val p = Uv.palette
        val ctx = LocalContext.current
        val status by RecorderState.status.collectAsStateWithLifecycle()
        var tab by rememberSaveable { mutableIntStateOf(0) }
        var openId by rememberSaveable { mutableStateOf<Long?>(null) }

        LaunchedEffect(vm.message) {
            vm.message?.let {
                Toast.makeText(ctx, it, Toast.LENGTH_SHORT).show()
                vm.clearMessage()
            }
        }
        // Calls screen starts a recording: jump to the recording screen so it is always visible.
        LaunchedEffect(status) { if (status != RecStatus.IDLE) { tab = 0; openId = null } }

        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(p.bgTop, p.bgBottom)))
        ) {
            val open = openId
            if (open != null) {
                BackHandler { openId = null }
                PlayerScreen(vm = vm, id = open, onClose = { openId = null })
            } else {
                Column(Modifier.fillMaxSize().systemBarsPadding()) {
                    Box(Modifier.weight(1f)) {
                        when (tab) {
                            0 -> RecordScreen(vm)
                            1 -> ArchiveScreen(vm, onOpen = { openId = it })
                            2 -> CallsScreen(vm)
                            else -> SettingsScreen(vm)
                        }
                    }
                    if (status == RecStatus.IDLE) BottomNav(selected = tab, onSelect = { tab = it })
                }
            }
        }
    }
}
