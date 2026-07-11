package dev.madsens.geyma.ui.sets

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.madsens.geyma.GeymaApp
import dev.madsens.geyma.data.WorkingSet
import dev.madsens.geyma.files.Entry
import dev.madsens.geyma.files.FileKind
import dev.madsens.geyma.files.PathUtils
import dev.madsens.geyma.theme.DANGER
import dev.madsens.geyma.theme.LocalTheme
import dev.madsens.geyma.theme.onAccent
import dev.madsens.geyma.ui.browser.NameDialog
import dev.madsens.geyma.ui.browser.openFile
import dev.madsens.geyma.ui.components.EmptyState
import dev.madsens.geyma.ui.components.GeymaCard
import dev.madsens.geyma.ui.components.KindBadge
import dev.madsens.geyma.ui.components.geymaShape
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

@Composable
fun SetsScreen(app: GeymaApp, onBrowseTo: (String) -> Unit) {
    val t = LocalTheme.current
    val sets by app.db.sets().all().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var openSet by remember { mutableStateOf<WorkingSet?>(null) }
    var createOpen by remember { mutableStateOf(false) }

    val current = openSet
    if (current != null) {
        BackHandler { openSet = null }
        SetDetail(app, current, onClose = { openSet = null }, onBrowseTo = onBrowseTo)
        return
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            Spacer(Modifier.height(10.dp))
            Text("Working sets", color = t.ink, fontWeight = FontWeight.Bold, fontSize = 24.sp)
            Text("Collections that follow your files around", color = t.inkFaint, fontSize = 13.sp)
            Spacer(Modifier.height(12.dp))

            if (sets.isEmpty()) {
                EmptyState("No sets yet. Select files in the browser and choose “To set”, or create one here.")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(sets, key = { it.id }) { set ->
                        val count by app.db.sets().itemCount(set.id).collectAsState(initial = 0)
                        GeymaCard(modifier = Modifier.fillMaxWidth().clickable { openSet = set }) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.AutoMirrored.Filled.PlaylistPlay, null, tint = t.accent)
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(set.name, color = t.ink, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        "$count item" + (if (count == 1) "" else "s"),
                                        color = t.inkFaint,
                                        fontSize = 12.sp,
                                    )
                                }
                                IconButton(onClick = {
                                    scope.launch {
                                        app.db.sets().clearItems(set.id)
                                        app.db.sets().removeSet(set.id)
                                    }
                                }) {
                                    Icon(Icons.Filled.Delete, "Delete set", tint = DANGER, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
        FloatingActionButton(
            onClick = { createOpen = true },
            containerColor = t.accent,
            contentColor = t.onAccent,
            shape = geymaShape(),
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        ) {
            Icon(Icons.Filled.Add, "New set")
        }
    }

    if (createOpen) {
        NameDialog(
            title = "New set",
            initial = "",
            confirmLabel = "Create",
            onConfirm = { name ->
                scope.launch { app.db.sets().addSet(WorkingSet(id = UUID.randomUUID().toString(), name = name)) }
                createOpen = false
            },
            onDismiss = { createOpen = false },
        )
    }
}

@Composable
private fun SetDetail(app: GeymaApp, set: WorkingSet, onClose: () -> Unit, onBrowseTo: (String) -> Unit) {
    val t = LocalTheme.current
    val context = LocalContext.current
    val items by app.db.sets().items(set.id).collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = t.inkSoft)
            }
            Column(Modifier.weight(1f)) {
                Text(set.name, color = t.ink, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text(
                    "${items.size} item" + (if (items.size == 1) "" else "s") + " · references, not copies",
                    color = t.inkFaint,
                    fontSize = 12.sp,
                )
            }
        }
        Spacer(Modifier.height(8.dp))

        if (items.isEmpty()) {
            EmptyState("This set is empty. Add files from the browser's “To set” action.")
            return
        }

        LazyColumn {
            items(items, key = { it.path }) { item ->
                val file = File(item.path)
                val missing = !file.exists()
                val entry = Entry(
                    name = PathUtils.nameOf(item.path),
                    path = item.path,
                    isDir = file.isDirectory,
                    size = if (file.isFile) file.length() else 0,
                    modifiedMs = file.lastModified(),
                    kind = FileKind.ofName(PathUtils.nameOf(item.path), file.isDirectory),
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(geymaShape())
                        .clickable(enabled = !missing) {
                            if (entry.isDir) onBrowseTo(entry.path) else openFile(context, entry.path)
                        }
                        .padding(horizontal = 6.dp, vertical = 6.dp),
                ) {
                    KindBadge(entry, 38.dp, thumbnails = !missing)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            entry.name,
                            color = if (missing) t.inkFaint else t.ink,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            if (missing) "Missing — ${item.path}" else item.path.removePrefix("/storage/emulated/0/"),
                            color = t.inkFaint,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    IconButton(onClick = { scope.launch { app.db.sets().removeItem(set.id, item.path) } }) {
                        Icon(Icons.Filled.Close, "Remove from set", tint = t.inkFaint, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}
