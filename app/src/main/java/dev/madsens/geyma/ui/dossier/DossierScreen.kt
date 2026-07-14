package dev.madsens.geyma.ui.dossier

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.madsens.geyma.GeymaApp
import dev.madsens.geyma.data.EventActions
import dev.madsens.geyma.data.FileEvent
import dev.madsens.geyma.data.Note
import dev.madsens.geyma.data.Revisit
import dev.madsens.geyma.insights.Dossier
import dev.madsens.geyma.insights.DossierSummary
import dev.madsens.geyma.insights.RevisitWhen
import dev.madsens.geyma.files.FileFacts
import dev.madsens.geyma.files.FileKind
import dev.madsens.geyma.files.PathUtils
import dev.madsens.geyma.theme.LocalTheme
import dev.madsens.geyma.theme.itemColors
import dev.madsens.geyma.ui.components.EmptyState
import dev.madsens.geyma.ui.components.EventGlyph
import dev.madsens.geyma.ui.components.EventUi
import dev.madsens.geyma.ui.components.GeymaCard
import dev.madsens.geyma.ui.components.SectionHeader
import dev.madsens.geyma.ui.components.formatWhen
import dev.madsens.geyma.ui.components.geymaShape
import dev.madsens.geyma.ui.components.kindIcon
import dev.madsens.geyma.ui.components.timeAgo
import kotlinx.coroutines.launch
import java.io.File

/**
 * A file's whole life as Geyma remembers it: where it came from, everywhere it
 * has been, and everything that happened to it — plus the one action no plain
 * file manager offers, "bring this back to me later."
 */
@Composable
fun DossierScreen(app: GeymaApp, path: String, onBack: () -> Unit, onBrowse: (String) -> Unit) {
    val t = LocalTheme.current
    val scope = rememberCoroutineScope()
    var summary by remember(path) { mutableStateOf<DossierSummary?>(null) }
    var facts by remember(path) { mutableStateOf<FileFacts?>(null) }
    var events by remember(path) { mutableStateOf<List<FileEvent>>(emptyList()) }
    var revisit by remember(path) { mutableStateOf<Revisit?>(null) }
    var note by remember(path) { mutableStateOf<Note?>(null) }
    var sealed by remember(path) { mutableStateOf(false) }
    var reloads by remember(path) { mutableStateOf(0) }

    LaunchedEffect(path, reloads) {
        summary = app.repo.dossier(path)
        facts = app.repo.fileFacts(path)
        events = app.repo.historyFor(path)
        revisit = app.repo.revisitFor(path)
        note = app.repo.noteFor(path)
        sealed = app.repo.isSealed(path)
    }

    val kind = remember(path) { FileKind.ofName(PathUtils.nameOf(path), File(path).isDirectory) }
    val colors = itemColors(kind, t)

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = t.inkSoft)
            }
            Box(
                Modifier.size(40.dp).clip(geymaShape(0.6f)).background(colors.bg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(kindIcon(kind), null, tint = colors.tint, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    PathUtils.nameOf(path),
                    color = t.ink,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text("Dossier", color = t.inkFaint, fontSize = 13.sp)
            }
            IconButton(onClick = {
                scope.launch {
                    app.repo.setSealed(path, !sealed)
                    reloads++
                }
            }) {
                Icon(
                    if (sealed) Icons.Filled.Lock else Icons.Filled.LockOpen,
                    if (sealed) "Unseal" else "Seal",
                    tint = if (sealed) t.accent else t.inkSoft,
                )
            }
            summary?.let { s ->
                IconButton(onClick = {
                    scope.launch {
                        app.repo.setStarred(path, !s.starred)
                        reloads++
                    }
                }) {
                    Icon(
                        if (s.starred) Icons.Filled.Star else Icons.Filled.StarBorder,
                        if (s.starred) "Unstar" else "Star",
                        tint = if (s.starred) t.accent else t.inkSoft,
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))

        val s = summary
        if (s == null) {
            EmptyState("Reading the file's history…")
            return
        }

        LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            facts?.let { f ->
                item { SectionHeader("File details") }
                item { FileDetailsCard(f) }
            }
            item { SectionHeader("What Geyma remembers") }
            item { ProvenanceCard(s) }

            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val exists = remember(path, reloads) { File(path).exists() }
                    ActionChip(
                        icon = Icons.Filled.FolderOpen,
                        label = if (s.trashed) "In trash" else "Show in files",
                        enabled = exists && !s.trashed,
                        onClick = { PathUtils.parentOf(path)?.let(onBrowse) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            if (sealed) {
                item { SealedBanner() }
            }

            item { SectionHeader("Note") }
            item {
                NotesCard(
                    note = note,
                    onSave = { text ->
                        scope.launch {
                            app.repo.setNote(path, text)
                            reloads++
                        }
                    },
                    onClear = {
                        scope.launch {
                            app.repo.clearNote(path)
                            reloads++
                        }
                    },
                )
            }

            item { RevisitCard(revisit, onSchedule = { whenChoice ->
                scope.launch {
                    app.repo.scheduleRevisit(path, System.currentTimeMillis() + whenChoice.deltaMs)
                    reloads++
                }
            }, onClear = {
                scope.launch {
                    app.repo.clearRevisit(path)
                    reloads++
                }
            }) }

            item { SectionHeader("Life so far") }
            if (events.isEmpty()) {
                item { Text("Nothing recorded for this file yet.", color = t.inkFaint, fontSize = 13.sp) }
            } else {
                items(events, key = { it.id }) { event -> DossierEventRow(event) }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun ProvenanceCard(s: DossierSummary) {
    val t = LocalTheme.current
    GeymaCard(modifier = Modifier.fillMaxWidth()) {
        // The headline answer no plain file manager offers: why is this here?
        val why = remember(s) { Dossier.whyHere(s) }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.HelpOutline, null, tint = t.accent, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Why is this here?", color = t.inkFaint, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            why.headline + (why.whenMs?.let { " · ${timeAgo(it)}" } ?: ""),
            color = t.ink,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.height(10.dp))

        if (s.lastOpenedMs != null) {
            StatLine("Last opened", timeAgo(s.lastOpenedMs))
        }
        if (s.timesOpened > 0) StatLine("Opened", "${s.timesOpened} time${plural(s.timesOpened)} through Geyma")
        if (s.timesMoved > 0) StatLine("Moved / renamed", "${s.timesMoved} time${plural(s.timesMoved)}")
        if (s.timesCopied > 0) StatLine("Copied", "${s.timesCopied} time${plural(s.timesCopied)}")
        if (s.setNames.isNotEmpty()) StatLine("In sets", s.setNames.joinToString(", "))
        StatLine("Journal entries", "${s.eventCount}")
        if (s.trashed) {
            Spacer(Modifier.height(4.dp))
            Text("This file is currently in the trash.", color = t.inkSoft, fontSize = 12.sp)
        }
    }
}

@Composable
private fun FileDetailsCard(f: FileFacts) {
    val t = LocalTheme.current
    GeymaCard(modifier = Modifier.fillMaxWidth()) {
        StatLine("Location", f.path)
        if (f.isDir) {
            StatLine("Type", "Folder")
            f.childCount?.let { StatLine("Contains", "$it item${if (it == 1) "" else "s"}") }
        } else {
            StatLine("Size", "${PathUtils.humanSize(f.size)} (${"%,d".format(f.size)} bytes)")
            val typeLabel = FileKind.LABELS[f.kind] ?: f.kind.replaceFirstChar { it.uppercase() }
            StatLine("Type", "$typeLabel · ${f.mime}")
        }
        StatLine("Modified", formatWhen(f.modifiedMs))
        if (!f.exists) {
            Spacer(Modifier.height(4.dp))
            Text("Not on disk right now — it may have been moved or removed.", color = t.inkSoft, fontSize = 12.sp)
        }
    }
}

@Composable
private fun StatLine(label: String, value: String) {
    val t = LocalTheme.current
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(label, color = t.inkFaint, fontSize = 13.sp, modifier = Modifier.width(120.dp))
        Text(value, color = t.ink, fontSize = 13.sp, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun SealedBanner() {
    val t = LocalTheme.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(geymaShape())
            .background(t.accent.copy(alpha = 0.12f))
            .padding(12.dp),
    ) {
        Icon(Icons.Filled.Lock, null, tint = t.accent, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(
            "Sealed — guarded against rename, move and delete until you unseal it.",
            color = t.ink,
            fontSize = 13.sp,
        )
    }
}

/**
 * A sticky note pinned to this file. Shows the note when there is one, an
 * invitation when there isn't, and an inline editor either way — the whole
 * "leave a memory for a file" affordance in one card.
 */
@Composable
private fun NotesCard(note: Note?, onSave: (String) -> Unit, onClear: () -> Unit) {
    val t = LocalTheme.current
    var editing by remember(note) { mutableStateOf(false) }
    var draft by remember(note, editing) { mutableStateOf(note?.text ?: "") }

    GeymaCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.EditNote, null, tint = t.accent, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Pinned note", color = t.ink, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Spacer(Modifier.weight(1f))
            note?.let { Text(timeAgo(it.updatedMs), color = t.inkFaint, fontSize = 11.sp) }
        }
        Spacer(Modifier.height(8.dp))
        if (editing) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(geymaShape(0.6f))
                    .border(BorderStroke(1.dp, t.border), geymaShape(0.6f))
                    .padding(10.dp),
            ) {
                BasicTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    textStyle = TextStyle(color = t.ink, fontSize = 14.sp),
                    cursorBrush = SolidColor(t.accent),
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { inner ->
                        Box {
                            if (draft.isEmpty()) {
                                Text("Leave a note for future you…", color = t.inkFaint, fontSize = 14.sp)
                            }
                            inner()
                        }
                    },
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NoteAction("Save", Icons.Filled.Check, filled = true) {
                    onSave(draft)
                    editing = false
                }
                NoteAction("Cancel", Icons.Filled.Close, filled = false) {
                    draft = note?.text ?: ""
                    editing = false
                }
                if (note != null) {
                    NoteAction("Remove", Icons.Filled.Close, filled = false) {
                        onClear()
                        editing = false
                    }
                }
            }
        } else if (note != null) {
            Text(note.text, color = t.ink, fontSize = 14.sp)
            Spacer(Modifier.height(8.dp))
            NoteAction("Edit note", Icons.Filled.EditNote, filled = false) { editing = true }
        } else {
            Text("No note yet — pin a reminder to this file.", color = t.inkFaint, fontSize = 12.sp)
            Spacer(Modifier.height(8.dp))
            NoteAction("Add a note", Icons.Filled.EditNote, filled = true) { editing = true }
        }
    }
}

@Composable
private fun NoteAction(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    filled: Boolean,
    onClick: () -> Unit,
) {
    val t = LocalTheme.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(geymaShape(0.5f))
            .background(if (filled) t.accent.copy(alpha = 0.12f) else androidx.compose.ui.graphics.Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Icon(icon, null, tint = t.accent, modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, color = t.accent, fontSize = 13.sp)
    }
}

@Composable
private fun RevisitCard(revisit: Revisit?, onSchedule: (RevisitWhen) -> Unit, onClear: () -> Unit) {
    val t = LocalTheme.current
    GeymaCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.NotificationsActive, null, tint = t.accent, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Resurface this later", color = t.ink, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        }
        Spacer(Modifier.height(4.dp))
        if (revisit != null) {
            val due = revisit.dueMs
            val whenText = if (due > System.currentTimeMillis()) "on ${formatWhen(due)}" else "now — it's on Home"
            Text(
                "Scheduled to reappear $whenText.",
                color = t.inkSoft,
                fontSize = 12.sp,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Cancel reminder",
                color = t.accent,
                fontSize = 13.sp,
                modifier = Modifier
                    .clip(geymaShape(0.5f))
                    .clickable(onClick = onClear)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
            )
        } else {
            Text("Bring it back to Home when the time comes.", color = t.inkFaint, fontSize = 12.sp)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (choice in RevisitWhen.entries) {
                    Text(
                        choice.label,
                        color = t.accent,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .clip(geymaShape(0.5f))
                            .background(t.accent.copy(alpha = 0.12f))
                            .clickable { onSchedule(choice) }
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val t = LocalTheme.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .clip(geymaShape())
            .background(t.card)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 12.dp),
    ) {
        Icon(icon, null, tint = if (enabled) t.accent else t.inkFaint, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, color = if (enabled) t.ink else t.inkFaint, fontSize = 14.sp)
    }
}

@Composable
private fun DossierEventRow(event: FileEvent) {
    val t = LocalTheme.current
    val gone = event.action == EventActions.DELETED || event.action == EventActions.TRASHED
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 7.dp),
    ) {
        EventGlyph(event.action, if (gone) t.inkFaint else t.accent)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                EventUi.label(event.action) + (event.detail?.let { " · $it" } ?: ""),
                color = t.ink,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(formatWhen(event.whenMs), color = t.inkFaint, fontSize = 11.sp)
        }
    }
}

private fun plural(n: Int) = if (n == 1) "" else "s"
