package dev.madsens.geyma.ui.trash

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.madsens.geyma.GeymaApp
import dev.madsens.geyma.data.TrashEntry
import dev.madsens.geyma.files.Entry
import dev.madsens.geyma.files.FileKind
import dev.madsens.geyma.files.PathUtils
import dev.madsens.geyma.theme.DANGER
import dev.madsens.geyma.theme.LocalTheme
import dev.madsens.geyma.ui.components.EmptyState
import dev.madsens.geyma.ui.components.KindBadge
import dev.madsens.geyma.ui.components.timeAgo
import kotlinx.coroutines.launch

@Composable
fun TrashScreen(app: GeymaApp) {
    val t = LocalTheme.current
    val entries by app.db.trash().all().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var confirmDelete by remember { mutableStateOf<TrashEntry?>(null) }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(10.dp))
        Text("Trash", color = t.ink, fontWeight = FontWeight.Bold, fontSize = 24.sp)
        Text("Recoverable — Geyma remembers where things came from", color = t.inkFaint, fontSize = 13.sp)
        Spacer(Modifier.height(12.dp))

        if (entries.isEmpty()) {
            EmptyState("Trash is empty.")
            return
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(entries, key = { it.id }) { entry ->
                val display = Entry(
                    name = entry.name,
                    path = entry.trashedPath,
                    isDir = entry.isDir,
                    size = entry.size,
                    modifiedMs = entry.whenMs,
                    kind = FileKind.ofName(entry.name, entry.isDir),
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                ) {
                    KindBadge(display, 40.dp, thumbnails = false)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(entry.name, color = t.ink, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            "from ${PathUtils.parentOf(entry.originalPath) ?: "/"} · ${timeAgo(entry.whenMs)}",
                            color = t.inkFaint,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    IconButton(onClick = { scope.launch { app.repo.restore(entry.id) } }) {
                        Icon(Icons.Filled.RestoreFromTrash, "Restore", tint = t.accent)
                    }
                    IconButton(onClick = { confirmDelete = entry }) {
                        Icon(Icons.Filled.DeleteForever, "Delete forever", tint = DANGER)
                    }
                }
            }
            item { Spacer(Modifier.height(12.dp)) }
        }
    }

    confirmDelete?.let { entry ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("Delete forever?") },
            text = { Text("“${entry.name}” will be permanently deleted. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { app.repo.deleteForever(entry.id) }
                    confirmDelete = null
                }) { Text("Delete", color = DANGER) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = null }) { Text("Cancel") }
            },
        )
    }
}
