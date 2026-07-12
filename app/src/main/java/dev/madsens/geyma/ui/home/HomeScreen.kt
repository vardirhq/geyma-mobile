package dev.madsens.geyma.ui.home

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.MoveToInbox
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.SdCard
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.madsens.geyma.GeymaApp
import dev.madsens.geyma.files.PathUtils
import dev.madsens.geyma.files.StorageRoots
import dev.madsens.geyma.insights.Revisits
import dev.madsens.geyma.theme.LocalTheme
import kotlinx.coroutines.launch
import dev.madsens.geyma.ui.components.EventGlyph
import dev.madsens.geyma.ui.components.EventUi
import dev.madsens.geyma.ui.components.GeymaCard
import dev.madsens.geyma.ui.components.SectionHeader
import dev.madsens.geyma.ui.components.geymaShape
import dev.madsens.geyma.ui.components.timeAgo
import java.io.File

@Composable
fun HomeScreen(
    app: GeymaApp,
    onBrowse: (String) -> Unit,
    onOpenTimeline: () -> Unit,
    onOpenTrash: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenFinder: () -> Unit,
    onOpenSweep: () -> Unit,
    onOpenAlmanac: () -> Unit,
    onOpenEchoes: () -> Unit,
    onOpenDossier: (String) -> Unit,
) {
    val t = LocalTheme.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val roots = remember { StorageRoots.list(context) }
    val recent by app.db.events().recent(6).collectAsState(initial = emptyList())
    val arrivals by app.db.events().recentArrivals(5).collectAsState(initial = emptyList())
    val stars by app.db.stars().all().collectAsState(initial = emptyList())
    val trashCount by app.db.trash().all().collectAsState(initial = emptyList())
    val revisits by app.db.revisits().all().collectAsState(initial = emptyList())
    val dueRevisits = remember(revisits) {
        Revisits.partition(revisits, System.currentTimeMillis()) { it.dueMs }.first
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Column {
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Geyma", color = t.ink, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                        Text("to keep, to guard", color = t.inkFaint, fontSize = 13.sp)
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, "Settings", tint = t.inkSoft)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(geymaShape())
                        .background(t.card)
                        .clickable { onOpenFinder() }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                ) {
                    Icon(Icons.Filled.Search, null, tint = t.inkSoft, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Find a file you had…", color = t.inkFaint, fontSize = 14.sp)
                }
                Spacer(Modifier.height(4.dp))
            }
        }

        if (dueRevisits.isNotEmpty()) {
            item { SectionHeader("Back for you") }
            item {
                GeymaCard(modifier = Modifier.fillMaxWidth()) {
                    for (revisit in dueRevisits) {
                        val f = remember(revisit.path) { File(revisit.path) }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { onOpenDossier(revisit.path) }
                                .padding(vertical = 6.dp),
                        ) {
                            Icon(Icons.Filled.NotificationsActive, null, tint = t.accent, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    PathUtils.nameOf(revisit.path),
                                    color = t.ink,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    if (f.exists()) "you asked to revisit this" else "no longer where it was",
                                    color = t.inkFaint,
                                    fontSize = 11.sp,
                                )
                            }
                            Text(
                                "Done",
                                color = t.accent,
                                fontSize = 12.sp,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { scope.launch { app.repo.clearRevisit(revisit.path) } }
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                            )
                        }
                    }
                }
            }
        }

        if (arrivals.isNotEmpty()) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionHeader("Recently arrived", Modifier.weight(1f))
                    Text(
                        "Sweep",
                        color = t.accent,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onOpenSweep() }
                            .padding(4.dp),
                    )
                }
            }
            item {
                GeymaCard(modifier = Modifier.fillMaxWidth()) {
                    for (event in arrivals) {
                        val f = remember(event.path) { File(event.path) }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .clickable(enabled = f.exists()) {
                                    onBrowse(f.parent ?: event.path)
                                }
                                .padding(vertical = 6.dp),
                        ) {
                            Icon(Icons.Filled.MoveToInbox, null, tint = t.accent, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    PathUtils.nameOf(event.path),
                                    color = t.ink,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    (event.detail ?: "arrived") + " · " + timeAgo(event.whenMs),
                                    color = t.inkFaint,
                                    fontSize = 11.sp,
                                )
                            }
                        }
                    }
                }
            }
        }

        item { SectionHeader("Storage") }
        items(roots.size) { i ->
            val root = roots[i]
            GeymaCard(modifier = Modifier.fillMaxWidth().clickable { onBrowse(root.path) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (root.removable) Icons.Filled.SdCard else Icons.Filled.Smartphone,
                        null,
                        tint = t.accent,
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(root.label, color = t.ink, fontWeight = FontWeight.SemiBold)
                        val used = root.totalBytes - root.freeBytes
                        Text(
                            "${PathUtils.humanSize(used)} used of ${PathUtils.humanSize(root.totalBytes)}",
                            color = t.inkFaint,
                            fontSize = 12.sp,
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                StorageGauge(fraction = if (root.totalBytes > 0) (root.totalBytes - root.freeBytes).toFloat() / root.totalBytes else 0f)
            }
        }

        item { SectionHeader("Quick access") }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                for ((label, path) in StorageRoots.quickAccess()) {
                    val exists = remember(path) { File(path).exists() }
                    if (!exists) continue
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(geymaShape())
                            .clickable { onBrowse(path) }
                            .padding(horizontal = 10.dp, vertical = 10.dp),
                    ) {
                        Icon(Icons.Filled.Folder, null, tint = t.accent, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(label, color = t.ink)
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(geymaShape())
                        .clickable { onOpenSweep() }
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                ) {
                    Icon(Icons.Filled.CleaningServices, null, tint = t.inkSoft, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Sweep forgotten files", color = t.ink, modifier = Modifier.weight(1f))
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(geymaShape())
                        .clickable { onOpenEchoes() }
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                ) {
                    Icon(Icons.Filled.ContentCopy, null, tint = t.inkSoft, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Echoes — find duplicates", color = t.ink, modifier = Modifier.weight(1f))
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(geymaShape())
                        .clickable { onOpenAlmanac() }
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                ) {
                    Icon(Icons.Filled.Insights, null, tint = t.inkSoft, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Almanac — journal insights", color = t.ink, modifier = Modifier.weight(1f))
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(geymaShape())
                        .clickable { onOpenTrash() }
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                ) {
                    Icon(Icons.Filled.Delete, null, tint = t.inkSoft, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Trash", color = t.ink, modifier = Modifier.weight(1f))
                    if (trashCount.isNotEmpty()) {
                        Text("${trashCount.size}", color = t.inkFaint, fontSize = 12.sp)
                    }
                }
            }
        }

        if (stars.isNotEmpty()) {
            item { SectionHeader("Starred") }
            item {
                GeymaCard(modifier = Modifier.fillMaxWidth()) {
                    for (star in stars.take(5)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .clickable {
                                    val f = File(star.path)
                                    onBrowse(if (f.isDirectory) star.path else f.parent ?: star.path)
                                }
                                .padding(vertical = 6.dp),
                        ) {
                            Icon(Icons.Filled.Star, null, tint = t.accent, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(
                                PathUtils.nameOf(star.path),
                                color = t.ink,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }

        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionHeader("Recent activity", Modifier.weight(1f))
                Text(
                    "See all",
                    color = t.accent,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onOpenTimeline() }
                        .padding(4.dp),
                )
            }
        }
        item {
            if (recent.isEmpty()) {
                Text("Nothing recorded yet.", color = t.inkFaint, fontSize = 13.sp)
            } else {
                GeymaCard(modifier = Modifier.fillMaxWidth()) {
                    for (event in recent) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { onOpenDossier(event.path) }
                                .padding(vertical = 5.dp),
                        ) {
                            EventGlyph(event.action, t.inkSoft)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    PathUtils.nameOf(event.path),
                                    color = t.ink,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    EventUi.label(event.action) + " · " + timeAgo(event.whenMs),
                                    color = t.inkFaint,
                                    fontSize = 11.sp,
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun StorageGauge(fraction: Float) {
    val t = LocalTheme.current
    Box(
        Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(t.ink.copy(alpha = 0.08f)),
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(t.accent),
        )
    }
}
