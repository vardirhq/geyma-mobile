@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package dev.madsens.geyma.ui.echoes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.madsens.geyma.GeymaApp
import dev.madsens.geyma.insights.DuplicateGroup
import dev.madsens.geyma.insights.Echoes
import dev.madsens.geyma.insights.FileFingerprint
import dev.madsens.geyma.files.PathUtils
import dev.madsens.geyma.files.ScanPhase
import dev.madsens.geyma.files.ScanProgress
import dev.madsens.geyma.files.StorageRoots
import dev.madsens.geyma.theme.LocalTheme
import dev.madsens.geyma.theme.itemColors
import dev.madsens.geyma.theme.onAccent
import dev.madsens.geyma.ui.components.EmptyState
import dev.madsens.geyma.ui.components.GeymaCard
import dev.madsens.geyma.ui.components.geymaShape
import dev.madsens.geyma.ui.components.kindIcon
import kotlinx.coroutines.launch

/**
 * Byte-for-byte duplicate finder, framed as the journal noticing the same file
 * twice. The oldest copy in each set is the original and is left untouched; the
 * echoes can be swept to trash — and because trash remembers origins, undone.
 */
@Composable
fun EchoesScreen(
    app: GeymaApp,
    onBack: () -> Unit,
    onOpenTrash: () -> Unit,
    onOpenDossier: (String) -> Unit,
) {
    val t = LocalTheme.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // Seed from the cached scan so returning from a dossier is instant, not a rescan.
    var groups by remember { mutableStateOf(app.echoesCache) }
    var selection by remember {
        mutableStateOf(app.echoesCache?.flatMap { g -> g.echoes.map { it.path } }?.toSet() ?: emptySet())
    }
    var justCleared by remember { mutableStateOf(0) }
    var progress by remember { mutableStateOf<ScanProgress?>(null) }

    suspend fun scan() {
        groups = null
        selection = emptySet()
        progress = null
        val roots = StorageRoots.list(context).map { it.path }
        val found = app.repo.findDuplicates(roots) { progress = it }
        // Pre-select every echo (never the original) so one tap clears redundancy.
        selection = found.flatMap { g -> g.echoes.map { it.path } }.toSet()
        groups = found
        app.echoesCache = found
    }

    // Only scan when there's no cached result to reuse.
    LaunchedEffect(Unit) { if (groups == null) scan() }

    val list = groups
    val selectedFiles = list.orEmpty().flatMap { it.files }.filter { it.path in selection }
    val reclaim = selectedFiles.sumOf { it.size }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = t.inkSoft)
            }
            Column(Modifier.weight(1f)) {
                Text("Echoes", color = t.ink, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                Text("The same file, kept more than once", color = t.inkFaint, fontSize = 13.sp)
            }
            if (list != null) {
                IconButton(onClick = { scope.launch { scan() } }) {
                    Icon(Icons.Filled.Refresh, "Rescan", tint = t.inkSoft)
                }
            }
        }
        Spacer(Modifier.height(8.dp))

        when {
            list == null -> ScanningState(progress)
            list.isEmpty() -> {
                if (justCleared > 0) {
                    ClearedCard(justCleared, onOpenTrash)
                } else {
                    EmptyState("No duplicates found. Every file here is one of a kind.")
                }
            }
            else -> {
                if (justCleared > 0) {
                    ClearedCard(justCleared, onOpenTrash)
                    Spacer(Modifier.height(8.dp))
                }
                GeymaCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "${list.size} set${if (list.size == 1) "" else "s"} of duplicates · " +
                            "${PathUtils.humanSize(Echoes.totalReclaimable(list))} reclaimable.",
                        color = t.ink,
                        fontSize = 13.sp,
                    )
                    Text(
                        "The oldest copy in each set is kept; echoes go to trash and can be restored.",
                        color = t.inkFaint,
                        fontSize = 12.sp,
                    )
                    Text(
                        "Hold any copy to see its full details.",
                        color = t.inkFaint,
                        fontSize = 12.sp,
                    )
                }
                Spacer(Modifier.height(8.dp))
                Box(Modifier.weight(1f)) {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(list, key = { it.original.path }) { group ->
                            DuplicateGroupCard(
                                group = group,
                                selection = selection,
                                onToggle = { path ->
                                    selection = if (path in selection) selection - path else selection + path
                                },
                                onOpenDossier = onOpenDossier,
                            )
                        }
                        item { Spacer(Modifier.height(80.dp)) }
                    }

                    if (selection.isNotEmpty()) {
                        Row(
                            Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)
                                .clip(geymaShape())
                                .background(t.accent)
                                .clickable {
                                    val paths = selection.toList()
                                    scope.launch {
                                        app.repo.moveToTrash(paths)
                                        justCleared = paths.size
                                        scan()
                                    }
                                }
                                .padding(vertical = 14.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Filled.Delete, null, tint = t.onAccent, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Clear ${selection.size} echo${if (selection.size == 1) "" else "es"} · ${PathUtils.humanSize(reclaim)}",
                                color = t.onAccent,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DuplicateGroupCard(
    group: DuplicateGroup,
    selection: Set<String>,
    onToggle: (String) -> Unit,
    onOpenDossier: (String) -> Unit,
) {
    val t = LocalTheme.current
    val colors = itemColors(group.kind, t)
    GeymaCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(34.dp).clip(geymaShape(0.6f)).background(colors.bg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(kindIcon(group.kind), null, tint = colors.tint, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    group.original.name,
                    color = t.ink,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${group.files.size} copies · ${PathUtils.humanSize(group.size)} each · ${PathUtils.humanSize(group.reclaimable)} reclaimable",
                    color = t.inkFaint,
                    fontSize = 11.sp,
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        CopyRow(group.original, kept = true, selected = false, onToggle = {}, onOpenDossier = onOpenDossier)
        for (echo in group.echoes) {
            CopyRow(
                echo,
                kept = false,
                selected = echo.path in selection,
                onToggle = { onToggle(echo.path) },
                onOpenDossier = onOpenDossier,
            )
        }
    }
}

@Composable
private fun CopyRow(
    file: FileFingerprint,
    kept: Boolean,
    selected: Boolean,
    onToggle: () -> Unit,
    onOpenDossier: (String) -> Unit,
) {
    val t = LocalTheme.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(geymaShape(0.5f))
            .combinedClickable(
                onClick = { if (!kept) onToggle() },
                onLongClick = { onOpenDossier(file.path) },
            )
            .padding(horizontal = 4.dp, vertical = 6.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                PathUtils.parentOf(file.path)?.let { PathUtils.nameOf(it) } ?: file.path,
                color = t.inkSoft,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                file.path,
                color = t.inkFaint,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        if (kept) {
            Icon(Icons.Filled.Lock, "Kept", tint = t.inkFaint, modifier = Modifier.size(18.dp))
        } else {
            Icon(
                if (selected) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                if (selected) "Will be cleared" else "Kept",
                tint = if (selected) t.accent else t.inkFaint,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun ScanningState(progress: ScanProgress?) {
    val t = LocalTheme.current
    Column(
        Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(color = t.accent, strokeWidth = 3.dp, modifier = Modifier.size(28.dp))
        Spacer(Modifier.height(16.dp))
        val text = when (progress?.phase) {
            null, ScanPhase.WALKING -> {
                val n = progress?.done ?: 0
                if (n == 0) "Scanning your storage…" else "Scanning your storage — $n files so far…"
            }
            ScanPhase.COMPARING ->
                "Comparing ${progress.done} of ${progress.total} look-alike${if (progress.total == 1) "" else "s"}…"
        }
        Text(text, color = t.inkSoft, fontSize = 14.sp, textAlign = TextAlign.Center)
        if (progress?.phase == ScanPhase.COMPARING && progress.total > 0) {
            Spacer(Modifier.height(14.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(t.ink.copy(alpha = 0.08f)),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth((progress.done.toFloat() / progress.total).coerceIn(0f, 1f))
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(t.accent),
                )
            }
        }
    }
}

@Composable
private fun ClearedCard(count: Int, onOpenTrash: () -> Unit) {
    val t = LocalTheme.current
    GeymaCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.CheckCircle, null, tint = t.accent, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text(
                "Cleared $count echo${if (count == 1) "" else "es"} to trash.",
                color = t.ink,
                fontSize = 14.sp,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onOpenTrash) {
                Icon(Icons.Filled.RestoreFromTrash, null, tint = t.accent, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Trash", color = t.accent)
            }
        }
    }
}
