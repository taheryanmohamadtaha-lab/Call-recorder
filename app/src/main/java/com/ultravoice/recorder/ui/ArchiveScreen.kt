package com.ultravoice.recorder.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ultravoice.recorder.R
import com.ultravoice.recorder.data.RecordingEntity
import com.ultravoice.recorder.data.RecordingType

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ArchiveScreen(vm: MainViewModel, onOpen: (Long) -> Unit) {
    val p = Uv.palette
    val active by vm.recordings.collectAsStateWithLifecycle()
    val trash by vm.trash.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    var sort by rememberSaveable { mutableIntStateOf(0) }
    var filter by rememberSaveable { mutableIntStateOf(0) } // 0 all, 1 favorites, 2 trash
    var selected by remember { mutableStateOf(setOf<Long>()) }

    val base = when (filter) {
        2 -> trash
        1 -> active.filter { it.favorite }
        else -> active
    }
    val list = remember(base, query, sort) {
        val q = query.trim()
        val filtered = if (q.isEmpty()) base else base.filter { it.name.contains(q, ignoreCase = true) || it.note.contains(q, ignoreCase = true) }
        when (sort) {
            1 -> filtered.sortedByDescending { it.sizeBytes }
            2 -> filtered.sortedByDescending { it.durationMs }
            else -> filtered.sortedByDescending { it.createdAt }
        }
    }
    val chosen = list.filter { it.id in selected }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(12.dp))
        if (selected.isNotEmpty()) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { selected = emptySet() }) { Icon(Icons.Default.Close, stringResource(R.string.cancel)) }
                Text(stringResource(R.string.selected_count, selected.size), Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                if (filter == 2) {
                    IconButton(onClick = { vm.restoreMany(chosen); selected = emptySet() }) {
                        Icon(Icons.Default.Refresh, stringResource(R.string.restore))
                    }
                    IconButton(onClick = { vm.deleteMany(chosen); selected = emptySet() }) {
                        Icon(Icons.Default.Delete, stringResource(R.string.delete_forever), tint = p.danger)
                    }
                } else {
                    IconButton(onClick = { vm.trashMany(chosen); selected = emptySet() }) {
                        Icon(Icons.Default.Delete, stringResource(R.string.delete), tint = p.danger)
                    }
                }
            }
        } else {
            Text(stringResource(R.string.tab_archive), fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = query, onValueChange = { query = it },
            placeholder = { Text(stringResource(R.string.search)) },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            singleLine = true, shape = RoundedCornerShape(50),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(R.string.filter_all, R.string.filter_fav, R.string.trash).forEachIndexed { i, r ->
                FilterChip(selected = filter == i, onClick = { filter = i; selected = emptySet() }, label = { Text(stringResource(r)) })
            }
            Spacer(Modifier.size(8.dp))
            listOf(R.string.sort_date, R.string.sort_size, R.string.sort_duration).forEachIndexed { i, r ->
                FilterChip(selected = sort == i, onClick = { sort = i }, label = { Text(stringResource(r)) })
            }
        }
        if (filter == 2 && trash.isNotEmpty()) {
            TextButton(onClick = { vm.emptyTrash() }) { Text(stringResource(R.string.empty_trash), color = p.danger) }
        }
        if (list.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.empty_archive), color = p.muted)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(list, key = { _, r -> r.id }) { index, r ->
                    if (sort == 0 && (index == 0 || dayKey(list[index - 1].createdAt) != dayKey(r.createdAt))) {
                        Text(formatDay(r.createdAt), color = p.muted, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp, start = 4.dp))
                    }
                    RecordingRow(
                        r = r,
                        isSelected = r.id in selected,
                        modifier = Modifier.combinedClickable(
                            onClick = {
                                if (selected.isNotEmpty()) selected = if (r.id in selected) selected - r.id else selected + r.id
                                else onOpen(r.id)
                            },
                            onLongClick = { selected = if (r.id in selected) selected - r.id else selected + r.id },
                        ),
                    )
                }
                item { Spacer(Modifier.height(12.dp)) }
            }
        }
    }
}

@Composable
private fun RecordingRow(r: RecordingEntity, isSelected: Boolean, modifier: Modifier) {
    val p = Uv.palette
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) p.accent.copy(alpha = 0.25f) else p.surface)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(42.dp).clip(CircleShape).background(p.surfaceHi), contentAlignment = Alignment.Center) {
            if (r.type == RecordingType.CALL_SPEAKER) Icon(Icons.Default.Call, null, Modifier.size(20.dp), tint = p.onBg)
            else MicIcon(p.onBg, Modifier.size(22.dp))
        }
        Spacer(Modifier.size(12.dp))
        Column(Modifier.weight(1f)) {
            Text(r.name, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
            Text(
                formatTime(r.createdAt) + " · " + formatDuration(r.durationMs) + " · " + formatSize(r.sizeBytes),
                color = p.muted, fontSize = 12.sp,
            )
            if (r.note.isNotBlank()) Text(r.note, color = p.muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (r.favorite) Icon(Icons.Default.Favorite, null, tint = p.danger, modifier = Modifier.size(18.dp))
    }
}
