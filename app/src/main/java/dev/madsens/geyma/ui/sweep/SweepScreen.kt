package dev.madsens.geyma.ui.sweep

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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.madsens.geyma.GeymaApp
import dev.madsens.geyma.files.PathUtils
import dev.madsens.geyma.files.SweepItem
import dev.madsens.geyma.theme.LocalTheme
import dev.madsens.geyma.theme.itemColors
import dev.madsens.geyma.theme.onAccent
import dev.madsens.geyma.ui.components.EmptyState
import dev.madsens.geyma.ui.components.GeymaCard
import dev.madsens.geyma.ui.components.geymaShape
import dev.madsens.geyma.ui.components.kindIcon
import kotlinx.coroutines.launch

/**
 * Cleanup ranked by neglect, not size: files that arrived from outside Geyma and
 * were never opened. Because trash remembers origins, a sweep is fully
 * reversible — the reassurance no other cleaner can give.
 */
@Composable
fun SweepScreen(app: GeymaApp, onBack: () -> Unit, onOpenTrash: () -> Unit) {
    val t = LocalTheme.current
    val scope = rememberCoroutineScope()
    var items by remember { mutableStateOf<List<SweepItem>?>(null) }
    var selection by remember { mutableStateOf<Set<String>>(emptySet()) }
    var justSwept by remember { mutableStateOf(0) }

    suspend fun reload() {
        items = app.repo.sweepCandidates()
    }
    androidx.compose.runtime.LaunchedEffect(Unit) { reload() }

    val list = items
    val selectedSize = list.orEmpty().filter { it.path in selection }.sumOf { it.size }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = t.inkSoft)
            }
            Column(Modifier.weight(1f)) {
                Text("Sweep", color = t.ink, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                Text("Forgotten files, oldest first", color = t.inkFaint, fontSize = 13.sp)
            }
        }
        Spacer(Modifier.height(8.dp))

        when {
            list == null -> EmptyState("Looking through what arrived…")
            list.isEmpty() -> {
                if (justSwept > 0) {
                    SweptCard(justSwept, onOpenTrash)
                } else {
                    EmptyState("Nothing neglected. Everything that arrived has been opened or filed.")
                }
            }
            else -> {
                if (justSwept > 0) {
                    SweptCard(justSwept, onOpenTrash)
                    Spacer(Modifier.height(8.dp))
                }
                GeymaCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "${list.size} file${if (list.size == 1) "" else "s"} arrived and were never opened in Geyma.",
                        color = t.ink,
                        fontSize = 13.sp,
                    )
                    Text(
                        "Sweeping moves them to trash — one tap restores any of them.",
                        color = t.inkFaint,
                        fontSize = 12.sp,
                    )
                }
                Spacer(Modifier.height(8.dp))
                Box(Modifier.weight(1f)) {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        item {
                            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                Text(
                                    if (selection.isEmpty()) "Tap files to select" else "${selection.size} selected · ${PathUtils.humanSize(selectedSize)}",
                                    color = t.inkFaint,
                                    fontSize = 12.sp,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    if (selection.size == list.size) "Clear all" else "Select all",
                                    color = t.accent,
                                    fontSize = 12.sp,
                                    modifier = Modifier
                                        .clip(geymaShape(0.5f))
                                        .clickable {
                                            selection = if (selection.size == list.size) emptySet() else list.map { it.path }.toSet()
                                        }
                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                )
                            }
                        }
                        items(list, key = { it.path }) { item ->
                            SweepRow(
                                item = item,
                                selected = item.path in selection,
                                onToggle = {
                                    selection = if (item.path in selection) selection - item.path else selection + item.path
                                },
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
                                        justSwept = paths.size
                                        selection = emptySet()
                                        reload()
                                    }
                                }
                                .padding(vertical = 14.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Filled.Delete, null, tint = t.onAccent, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Sweep ${selection.size} to trash · ${PathUtils.humanSize(selectedSize)}",
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
private fun SweptCard(count: Int, onOpenTrash: () -> Unit) {
    val t = LocalTheme.current
    GeymaCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.CheckCircle, null, tint = t.accent, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text(
                "Swept $count file${if (count == 1) "" else "s"} to trash.",
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

@Composable
private fun SweepRow(item: SweepItem, selected: Boolean, onToggle: () -> Unit) {
    val t = LocalTheme.current
    val colors = itemColors(item.kind, t)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(geymaShape(0.6f))
            .clickable(onClick = onToggle)
            .padding(horizontal = 6.dp, vertical = 8.dp),
    ) {
        Box(
            Modifier.size(38.dp).clip(geymaShape(0.6f)).background(colors.bg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(kindIcon(item.kind), null, tint = colors.tint, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(item.name, color = t.ink, fontWeight = FontWeight.Medium, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val age = when {
                item.ageDays <= 0 -> "arrived today"
                item.ageDays == 1 -> "arrived yesterday"
                item.ageDays < 30 -> "arrived ${item.ageDays} days ago"
                else -> "arrived ${item.ageDays / 30} month${if (item.ageDays / 30 == 1) "" else "s"} ago"
            }
            Text("$age · ${PathUtils.humanSize(item.size)}", color = t.inkFaint, fontSize = 11.sp)
        }
        Icon(
            if (selected) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
            if (selected) "Selected" else "Not selected",
            tint = if (selected) t.accent else t.inkFaint,
            modifier = Modifier.size(22.dp),
        )
    }
}
