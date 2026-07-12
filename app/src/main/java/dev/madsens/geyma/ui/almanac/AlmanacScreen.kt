package dev.madsens.geyma.ui.almanac

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.madsens.geyma.GeymaApp
import dev.madsens.geyma.insights.AlmanacStats
import dev.madsens.geyma.insights.Tally
import dev.madsens.geyma.theme.LocalTheme
import dev.madsens.geyma.ui.components.EmptyState
import dev.madsens.geyma.ui.components.GeymaCard
import dev.madsens.geyma.ui.components.SectionHeader
import dev.madsens.geyma.ui.components.geymaShape

private const val WINDOW_DAYS = 14

/**
 * A read-only almanac of the journal: how much Geyma has remembered lately, when
 * it was busiest, and which folders and files see the most life. Nothing here
 * changes a file — it's the app looking back at itself.
 */
@Composable
fun AlmanacScreen(
    app: GeymaApp,
    onBack: () -> Unit,
    onBrowse: (String) -> Unit,
    onOpenDossier: (String) -> Unit,
) {
    val t = LocalTheme.current
    var stats by remember { mutableStateOf<AlmanacStats?>(null) }

    LaunchedEffect(Unit) { stats = app.repo.almanac(WINDOW_DAYS) }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = t.inkSoft)
            }
            Column(Modifier.weight(1f)) {
                Text("Almanac", color = t.ink, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                Text("The last $WINDOW_DAYS days of memory", color = t.inkFaint, fontSize = 13.sp)
            }
        }
        Spacer(Modifier.height(8.dp))

        val s = stats
        when {
            s == null -> EmptyState("Leafing through the journal…")
            !s.hasHistory -> EmptyState("Nothing recorded in the last $WINDOW_DAYS days yet.")
            else -> LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item { HeadlineRow(s) }
                item { SectionHeader("Activity") }
                item { Sparkline(s) }

                if (s.topFolders.isNotEmpty()) {
                    item { SectionHeader("Busiest folders") }
                    item {
                        GeymaCard(modifier = Modifier.fillMaxWidth()) {
                            for (folder in s.topFolders) {
                                TallyRow(folder, Icons.Filled.Folder, s.topFolders.first().count) {
                                    onBrowse(folder.key)
                                }
                            }
                        }
                    }
                }

                if (s.busiestFiles.isNotEmpty()) {
                    item { SectionHeader("Most-handled files") }
                    item {
                        GeymaCard(modifier = Modifier.fillMaxWidth()) {
                            for (file in s.busiestFiles) {
                                TallyRow(file, Icons.AutoMirrored.Filled.InsertDriveFile, s.busiestFiles.first().count) {
                                    onOpenDossier(file.key)
                                }
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun HeadlineRow(s: AlmanacStats) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatTile("Arrived", s.arrivals, Modifier.weight(1f))
        StatTile("Opened", s.opens, Modifier.weight(1f))
        StatTile("Neglected", s.neglected, Modifier.weight(1f))
    }
}

@Composable
private fun StatTile(label: String, value: Int, modifier: Modifier = Modifier) {
    val t = LocalTheme.current
    GeymaCard(modifier = modifier) {
        Text("$value", color = t.ink, fontWeight = FontWeight.Bold, fontSize = 24.sp)
        Text(label, color = t.inkFaint, fontSize = 12.sp)
    }
}

@Composable
private fun Sparkline(s: AlmanacStats) {
    val t = LocalTheme.current
    val peak = s.peakDayCount.coerceAtLeast(1)
    GeymaCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().height(72.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            for (bucket in s.perDay) {
                val frac = bucket.count.toFloat() / peak
                Column(
                    Modifier.weight(1f).fillMaxHeight(),
                    verticalArrangement = Arrangement.Bottom,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(frac.coerceIn(0.02f, 1f))
                            .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                            .background(if (bucket.count > 0) t.accent else t.ink.copy(alpha = 0.08f)),
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth()) {
            Text("$WINDOW_DAYS days ago", color = t.inkFaint, fontSize = 10.sp, modifier = Modifier.weight(1f))
            val busiest = s.busiestDay
            if (busiest != null) {
                Text(
                    "peak ${busiest.count} in a day",
                    color = t.inkFaint,
                    fontSize = 10.sp,
                )
            }
            Spacer(Modifier.weight(1f))
            Text("today", color = t.inkFaint, fontSize = 10.sp)
        }
    }
}

@Composable
private fun TallyRow(
    tally: Tally,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    peak: Int,
    onClick: () -> Unit,
) {
    val t = LocalTheme.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(geymaShape(0.5f))
            .clickable(onClick = onClick)
            .padding(vertical = 7.dp),
    ) {
        Icon(icon, null, tint = t.accent, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                tally.label,
                color = t.ink,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(t.ink.copy(alpha = 0.07f)),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth((tally.count.toFloat() / peak.coerceAtLeast(1)).coerceIn(0.05f, 1f))
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(t.accent),
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        Text("${tally.count}", color = t.inkFaint, fontSize = 12.sp)
    }
}
