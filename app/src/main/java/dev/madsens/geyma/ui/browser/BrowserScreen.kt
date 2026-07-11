@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package dev.madsens.geyma.ui.browser

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.madsens.geyma.GeymaApp
import dev.madsens.geyma.data.FileEvent
import dev.madsens.geyma.data.SetItem
import dev.madsens.geyma.data.SortDir
import dev.madsens.geyma.data.SortKey
import dev.madsens.geyma.data.ViewMode
import dev.madsens.geyma.files.Entry
import dev.madsens.geyma.files.GhostTrail
import dev.madsens.geyma.files.PathUtils
import dev.madsens.geyma.theme.DANGER
import dev.madsens.geyma.theme.LocalTheme
import dev.madsens.geyma.theme.onAccent
import dev.madsens.geyma.ui.components.EmptyState
import dev.madsens.geyma.ui.components.EventGlyph
import dev.madsens.geyma.ui.components.EventUi
import dev.madsens.geyma.ui.components.FileGridTile
import dev.madsens.geyma.ui.components.FileRow
import dev.madsens.geyma.ui.components.KindBadge
import dev.madsens.geyma.ui.components.SectionHeader
import dev.madsens.geyma.ui.components.formatWhen
import dev.madsens.geyma.ui.components.geymaShape
import dev.madsens.geyma.ui.components.timeAgo
import kotlinx.coroutines.launch

@Composable
fun BrowserScreen(app: GeymaApp, vm: BrowserViewModel) {
    val t = LocalTheme.current
    val state by vm.state.collectAsState()
    val context = LocalContext.current
    var searchOpen by remember { mutableStateOf(false) }
    var sheetEntry by remember { mutableStateOf<Entry?>(null) }
    var renameTarget by remember { mutableStateOf<Entry?>(null) }
    var newFolderOpen by remember { mutableStateOf(false) }
    var addToSetTarget by remember { mutableStateOf<List<String>?>(null) }

    BackHandler(enabled = state.selecting || state.dir != state.rootPath) {
        if (state.selecting) vm.clearSelection() else vm.up()
    }

    Column(Modifier.fillMaxSize()) {
        BrowserTopBar(
            vm = vm,
            state = state,
            searchOpen = searchOpen,
            onToggleSearch = {
                searchOpen = !searchOpen
                if (!searchOpen) vm.setQuery("")
            },
        )
        Breadcrumbs(state) { vm.open(it) }

        state.error?.let { err ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
                    .clip(geymaShape())
                    .background(DANGER.copy(alpha = 0.15f))
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(err, color = DANGER, fontSize = 13.sp, modifier = Modifier.weight(1f))
                IconButton(onClick = { vm.dismissError() }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Filled.Close, "Dismiss", tint = DANGER, modifier = Modifier.size(16.dp))
                }
            }
        }

        Box(Modifier.weight(1f)) {
            when {
                state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = t.accent)
                }
                state.visibleEntries.isEmpty() && state.ghosts.isEmpty() ->
                    EmptyState(if (state.query.isBlank()) "Nothing here yet" else "No matches for “${state.query}”")
                else -> Listing(
                    state = state,
                    onOpen = { entry ->
                        if (state.selecting) {
                            vm.toggleSelect(entry.path)
                        } else if (entry.isDir) {
                            vm.open(entry.path)
                        } else {
                            openFile(context, entry.path)
                        }
                    },
                    onLongPress = { vm.toggleSelect(it.path) },
                    onMore = { sheetEntry = it },
                    onGhostTap = { ghost ->
                        val parent = PathUtils.parentOf(ghost.toPath)
                        if (parent != null && ghost.action != "trashed") vm.open(parent)
                    },
                )
            }

            if (!state.selecting && state.clipboard == null) {
                FloatingActionButton(
                    onClick = { newFolderOpen = true },
                    containerColor = t.accent,
                    contentColor = t.onAccent,
                    shape = geymaShape(),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp),
                ) {
                    Icon(Icons.Filled.CreateNewFolder, "New folder")
                }
            }
        }

        state.clipboard?.let { clip ->
            ClipboardBar(clip = clip, onPaste = { vm.paste() }, onCancel = { vm.cancelClipboard() })
        }
        if (state.selecting) {
            SelectionBar(
                vm = vm,
                state = state,
                onRename = { renameTarget = state.entries.firstOrNull { it.path == state.selection.first() } },
                onAddToSet = { addToSetTarget = state.selection.toList() },
            )
        }
    }

    sheetEntry?.let { entry ->
        EntrySheet(
            app = app,
            entry = entry,
            onDismiss = { sheetEntry = null },
            onOpen = {
                sheetEntry = null
                if (entry.isDir) vm.open(entry.path) else openFile(context, entry.path)
            },
            onShare = { shareFile(context, entry.path) },
            onStar = {
                vm.setStarred(entry.path, !entry.starred)
                sheetEntry = null
            },
            onRename = {
                sheetEntry = null
                renameTarget = entry
            },
            onTrash = {
                sheetEntry = null
                vm.trash(entry.path)
            },
            onAddToSet = {
                sheetEntry = null
                addToSetTarget = listOf(entry.path)
            },
        )
    }

    renameTarget?.let { entry ->
        NameDialog(
            title = "Rename",
            initial = entry.name,
            confirmLabel = "Rename",
            onConfirm = {
                vm.rename(entry.path, it)
                vm.clearSelection()
                renameTarget = null
            },
            onDismiss = { renameTarget = null },
        )
    }

    if (newFolderOpen) {
        NameDialog(
            title = "New folder",
            initial = "",
            confirmLabel = "Create",
            onConfirm = {
                vm.createFolder(it)
                newFolderOpen = false
            },
            onDismiss = { newFolderOpen = false },
        )
    }

    addToSetTarget?.let { paths ->
        AddToSetDialog(
            app = app,
            paths = paths,
            onDone = {
                addToSetTarget = null
                vm.clearSelection()
            },
        )
    }
}

@Composable
private fun BrowserTopBar(
    vm: BrowserViewModel,
    state: BrowserState,
    searchOpen: Boolean,
    onToggleSearch: () -> Unit,
) {
    val t = LocalTheme.current
    var sortOpen by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().background(t.surface)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Files",
                color = t.ink,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                modifier = Modifier.padding(start = 8.dp).weight(1f),
            )
            IconButton(onClick = onToggleSearch) {
                Icon(Icons.Filled.Search, "Search", tint = if (searchOpen) t.accent else t.inkSoft)
            }
            IconButton(onClick = {
                vm.setViewMode(if (state.prefs.viewMode == ViewMode.LIST) ViewMode.GRID else ViewMode.LIST)
            }) {
                Icon(
                    if (state.prefs.viewMode == ViewMode.LIST) Icons.Filled.GridView else Icons.Filled.ViewList,
                    "Toggle view",
                    tint = t.inkSoft,
                )
            }
            Box {
                IconButton(onClick = { sortOpen = true }) {
                    Icon(Icons.Filled.Sort, "Sort", tint = t.inkSoft)
                }
                DropdownMenu(expanded = sortOpen, onDismissRequest = { sortOpen = false }) {
                    for (key in SortKey.entries) {
                        val active = state.prefs.sortKey == key
                        DropdownMenuItem(
                            text = { Text(key.name.lowercase().replaceFirstChar { it.uppercase() }) },
                            leadingIcon = {
                                if (active) Icon(Icons.Filled.Check, null, modifier = Modifier.size(18.dp))
                            },
                            onClick = {
                                val dir = if (active) {
                                    if (state.prefs.sortDir == SortDir.ASC) SortDir.DESC else SortDir.ASC
                                } else {
                                    SortDir.ASC
                                }
                                vm.setSort(key, dir)
                                sortOpen = false
                            },
                        )
                    }
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text(if (state.prefs.showHidden) "Hide hidden files" else "Show hidden files") },
                        leadingIcon = {
                            Icon(
                                if (state.prefs.showHidden) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                null,
                                modifier = Modifier.size(18.dp),
                            )
                        },
                        onClick = {
                            vm.setShowHidden(!state.prefs.showHidden)
                            sortOpen = false
                        },
                    )
                }
            }
        }
        if (searchOpen) {
            OutlinedTextField(
                value = state.query,
                onValueChange = { vm.setQuery(it) },
                placeholder = { Text("Filter this folder…") },
                singleLine = true,
                keyboardOptions = KeyboardOptions.Default,
                shape = geymaShape(),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun Breadcrumbs(state: BrowserState, onNavigate: (String) -> Unit) {
    val t = LocalTheme.current
    LazyRow(
        modifier = Modifier.fillMaxWidth().background(t.surface).padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val crumbs = state.breadcrumbs
        items(crumbs.size) { i ->
            val (label, path) = crumbs[i]
            val last = i == crumbs.lastIndex
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    label,
                    color = if (last) t.ink else t.inkSoft,
                    fontWeight = if (last) FontWeight.SemiBold else FontWeight.Normal,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onNavigate(path) }
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                )
                if (!last) {
                    Icon(
                        Icons.AutoMirrored.Filled.NavigateNext,
                        null,
                        tint = t.inkFaint,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun Listing(
    state: BrowserState,
    onOpen: (Entry) -> Unit,
    onLongPress: (Entry) -> Unit,
    onMore: (Entry) -> Unit,
    onGhostTap: (GhostTrail) -> Unit,
) {
    if (state.prefs.viewMode == ViewMode.GRID) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(110.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
        ) {
            items(state.visibleEntries, key = { it.path }) { entry ->
                FileGridTile(
                    entry = entry,
                    selected = entry.path in state.selection,
                    onClick = { onOpen(entry) },
                    onLongClick = { onLongPress(entry) },
                )
            }
        }
    } else {
        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 8.dp)) {
            items(state.visibleEntries, key = { it.path }) { entry ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) {
                        FileRow(
                            entry = entry,
                            selected = entry.path in state.selection,
                            onClick = { onOpen(entry) },
                            onLongClick = { onLongPress(entry) },
                        )
                    }
                    IconButton(onClick = { onMore(entry) }) {
                        Icon(Icons.Filled.MoreVert, "More", tint = LocalTheme.current.inkFaint)
                    }
                }
            }
            if (state.ghosts.isNotEmpty() && state.query.isBlank()) {
                item { SectionHeader("Recently departed", Modifier.padding(horizontal = 10.dp)) }
                items(state.ghosts, key = { "ghost:" + it.fromPath }) { ghost ->
                    GhostRow(ghost) { onGhostTap(ghost) }
                }
            }
        }
    }
}

/** Faint dashed marker where a file used to live — the ghost trail. */
@Composable
private fun GhostRow(ghost: GhostTrail, onTap: () -> Unit) {
    val t = LocalTheme.current
    val shape = geymaShape()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .clip(shape)
            .clickable(onClick = onTap)
            .dashedBorder(t.inkFaint.copy(alpha = 0.5f), t.radius.dp)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EventGlyph(ghost.action, t.inkFaint)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(ghost.name, color = t.inkFaint, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val dest = when (ghost.action) {
                "trashed" -> "moved to trash"
                "renamed" -> "renamed to ${PathUtils.nameOf(ghost.toPath)}"
                else -> "moved to ${PathUtils.nameOf(PathUtils.parentOf(ghost.toPath) ?: "")}"
            }
            Text("$dest · ${timeAgo(ghost.whenMs)}", color = t.inkFaint.copy(alpha = 0.8f), fontSize = 12.sp, maxLines = 1)
        }
    }
}

private fun Modifier.dashedBorder(color: Color, cornerRadius: androidx.compose.ui.unit.Dp): Modifier =
    drawBehind {
        drawRoundRect(
            color = color,
            style = Stroke(
                width = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)),
            ),
            cornerRadius = CornerRadius(cornerRadius.toPx()),
        )
    }

@Composable
private fun ClipboardBar(clip: Clipboard, onPaste: () -> Unit, onCancel: () -> Unit) {
    val t = LocalTheme.current
    Row(
        Modifier
            .fillMaxWidth()
            .background(t.surface)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (clip.mode == ClipMode.COPY) Icons.Filled.ContentCopy else Icons.AutoMirrored.Filled.DriveFileMove,
            null,
            tint = t.accent,
        )
        Spacer(Modifier.width(10.dp))
        Text(
            "${clip.paths.size} item${if (clip.paths.size == 1) "" else "s"} to ${if (clip.mode == ClipMode.COPY) "copy" else "move"} here",
            color = t.ink,
            fontSize = 14.sp,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onCancel) { Text("Cancel", color = t.inkSoft) }
        TextButton(onClick = onPaste) {
            Icon(Icons.Filled.ContentPaste, null, tint = t.accent, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Paste", color = t.accent, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun SelectionBar(
    vm: BrowserViewModel,
    state: BrowserState,
    onRename: () -> Unit,
    onAddToSet: () -> Unit,
) {
    val t = LocalTheme.current
    val single = state.selection.size == 1
    val anyUnstarred = state.entries.any { it.path in state.selection && !it.starred }
    Column(Modifier.fillMaxWidth().background(t.surface)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { vm.clearSelection() }) {
                Icon(Icons.Filled.Close, "Clear selection", tint = t.inkSoft)
            }
            Text("${state.selection.size} selected", color = t.ink, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            IconButton(onClick = { vm.selectAll() }) {
                Icon(Icons.Filled.SelectAll, "Select all", tint = t.inkSoft)
            }
            IconButton(onClick = { vm.starSelection(anyUnstarred) }) {
                Icon(
                    if (anyUnstarred) Icons.Filled.StarBorder else Icons.Filled.Star,
                    "Star",
                    tint = t.accent,
                )
            }
            if (single) {
                IconButton(onClick = onRename) {
                    Icon(Icons.Filled.DriveFileRenameOutline, "Rename", tint = t.inkSoft)
                }
            }
            IconButton(onClick = onAddToSet) {
                Icon(Icons.Filled.PlaylistAdd, "Add to set", tint = t.inkSoft)
            }
            IconButton(onClick = { vm.stageClipboard(ClipMode.COPY) }) {
                Icon(Icons.Filled.ContentCopy, "Copy", tint = t.inkSoft)
            }
            IconButton(onClick = { vm.stageClipboard(ClipMode.MOVE) }) {
                Icon(Icons.AutoMirrored.Filled.DriveFileMove, "Move", tint = t.inkSoft)
            }
            IconButton(onClick = { vm.trashSelection() }) {
                Icon(Icons.Filled.Delete, "Delete", tint = DANGER)
            }
        }
    }
}

@Composable
private fun EntrySheet(
    app: GeymaApp,
    entry: Entry,
    onDismiss: () -> Unit,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onStar: () -> Unit,
    onRename: () -> Unit,
    onTrash: () -> Unit,
    onAddToSet: () -> Unit,
) {
    val t = LocalTheme.current
    var history by remember(entry.path) { mutableStateOf<List<FileEvent>>(emptyList()) }
    LaunchedEffect(entry.path) {
        history = app.repo.historyFor(entry.path)
    }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = t.card) {
        Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                KindBadge(entry, 44.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(entry.name, color = t.ink, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(
                        if (entry.isDir) "Folder" else PathUtils.humanSize(entry.size) + " · " + timeAgo(entry.modifiedMs),
                        color = t.inkFaint,
                        fontSize = 12.sp,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                SheetAction(Icons.Filled.OpenInNew, "Open", onOpen)
                if (!entry.isDir) SheetAction(Icons.Filled.Share, "Share", onShare)
                SheetAction(
                    if (entry.starred) Icons.Filled.Star else Icons.Filled.StarBorder,
                    if (entry.starred) "Unstar" else "Star",
                    onStar,
                )
                SheetAction(Icons.Filled.DriveFileRenameOutline, "Rename", onRename)
                SheetAction(Icons.Filled.PlaylistAdd, "To set", onAddToSet)
                SheetAction(Icons.Filled.Delete, "Delete", onTrash, tint = DANGER)
            }
            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = t.border)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                Icon(Icons.Filled.History, null, tint = t.inkFaint, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                SectionHeader("Activity")
            }
            if (history.isEmpty()) {
                Text("Nothing recorded yet.", color = t.inkFaint, fontSize = 13.sp, modifier = Modifier.padding(vertical = 8.dp))
            } else {
                for (event in history.take(12)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                    ) {
                        EventGlyph(event.action, t.inkSoft)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                EventUi.label(event.action) + (event.detail?.let { " · $it" } ?: ""),
                                color = t.ink,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(formatWhen(event.whenMs), color = t.inkFaint, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SheetAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    tint: Color? = null,
) {
    val t = LocalTheme.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(geymaShape(0.6f))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
    ) {
        Icon(icon, label, tint = tint ?: t.inkSoft)
        Spacer(Modifier.height(4.dp))
        Text(label, color = tint ?: t.inkSoft, fontSize = 11.sp)
    }
}

@Composable
fun NameDialog(
    title: String,
    initial: String,
    confirmLabel: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var value by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { if (value.isNotBlank()) onConfirm(value.trim()) }) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun AddToSetDialog(app: GeymaApp, paths: List<String>, onDone: () -> Unit) {
    val t = LocalTheme.current
    val sets by app.db.sets().all().collectAsState(initial = emptyList())
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var newName by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDone,
        title = { Text("Add to set") },
        text = {
            Column {
                if (sets.isEmpty()) {
                    Text("No sets yet — create one below.", color = t.inkFaint, fontSize = 13.sp)
                }
                for (set in sets) {
                    Text(
                        set.name,
                        color = t.ink,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(geymaShape(0.5f))
                            .clickable {
                                scope.launch {
                                    paths.forEach { app.db.sets().addItem(SetItem(setId = set.id, path = it)) }
                                    onDone()
                                }
                            }
                            .padding(vertical = 10.dp, horizontal = 6.dp),
                    )
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    placeholder = { Text("New set name…") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (newName.isBlank()) return@TextButton
                    scope.launch {
                        val set = dev.madsens.geyma.data.WorkingSet(
                            id = java.util.UUID.randomUUID().toString(),
                            name = newName.trim(),
                        )
                        app.db.sets().addSet(set)
                        paths.forEach { app.db.sets().addItem(SetItem(setId = set.id, path = it)) }
                        onDone()
                    }
                },
            ) { Text("Create & add") }
        },
        dismissButton = {
            TextButton(onClick = onDone) { Text("Cancel") }
        },
    )
}
