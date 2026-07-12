@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package dev.madsens.geyma.ui.finder

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
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
import dev.madsens.geyma.files.PathUtils
import dev.madsens.geyma.files.SearchHit
import dev.madsens.geyma.theme.LocalTheme
import dev.madsens.geyma.ui.components.EmptyState
import dev.madsens.geyma.ui.components.EventGlyph
import dev.madsens.geyma.ui.components.EventUi
import dev.madsens.geyma.ui.components.geymaShape
import dev.madsens.geyma.ui.components.timeAgo
import kotlinx.coroutines.delay
import java.io.File

/**
 * Search across everything Geyma remembers — not just the current folder. A file
 * you downloaded last week still turns up here even after it was moved, renamed,
 * or trashed, and the result tells you where it went.
 */
@Composable
fun FinderScreen(app: GeymaApp, onBack: () -> Unit, onView: (String) -> Unit, onBrowseTo: (String) -> Unit) {
    val t = LocalTheme.current
    var query by remember { mutableStateOf("") }
    var hits by remember { mutableStateOf<List<SearchHit>>(emptyList()) }

    LaunchedEffect(query) {
        if (query.isBlank()) {
            hits = emptyList()
        } else {
            delay(200) // debounce keystrokes
            hits = app.repo.searchJournal(query)
        }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = t.inkSoft)
            }
            Column {
                Text("Find", color = t.ink, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                Text("Search the past, not just the tree", color = t.inkFaint, fontSize = 13.sp)
            }
        }
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Name of a file you had…") },
            leadingIcon = { Icon(Icons.Filled.Search, null, tint = t.inkSoft) },
            singleLine = true,
            shape = geymaShape(),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))

        when {
            query.isBlank() -> EmptyState("Type a name. Geyma looks through moved, renamed and trashed files too.")
            hits.isEmpty() -> EmptyState("Nothing in the journal matches “$query”.")
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                items(hits, key = { it.path + it.lastWhenMs }) { hit ->
                    HitRow(
                        hit = hit,
                        onClick = {
                            when {
                                hit.exists && File(hit.path).isDirectory -> onBrowseTo(hit.path)
                                hit.exists -> onView(hit.path)
                                else -> PathUtils.parentOf(hit.path)?.let { parent ->
                                    if (File(parent).isDirectory) onBrowseTo(parent)
                                }
                            }
                        },
                    )
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun HitRow(hit: SearchHit, onClick: () -> Unit) {
    val t = LocalTheme.current
    val faded = !hit.exists
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(geymaShape(0.6f))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
    ) {
        EventGlyph(hit.lastAction, if (faded) t.inkFaint else t.accent)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                hit.name,
                color = if (faded) t.inkSoft else t.ink,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val where = when {
                hit.trashed -> "in trash"
                !hit.exists -> "last seen ${EventUi.label(hit.lastAction).lowercase()}"
                else -> hit.path.removePrefix("/storage/emulated/0/")
            }
            Text(
                "$where · ${timeAgo(hit.lastWhenMs)}",
                color = t.inkFaint,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
