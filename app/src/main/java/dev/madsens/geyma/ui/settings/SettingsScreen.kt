package dev.madsens.geyma.ui.settings

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import dev.madsens.geyma.BuildConfig
import dev.madsens.geyma.GeymaApp
import dev.madsens.geyma.ui.browser.shareFile
import dev.madsens.geyma.theme.ACCENTS
import dev.madsens.geyma.theme.BackgroundPattern
import dev.madsens.geyma.theme.FontKey
import dev.madsens.geyma.theme.LocalTheme
import dev.madsens.geyma.theme.SKINS
import dev.madsens.geyma.theme.SKIN_ORDER
import dev.madsens.geyma.theme.Skin
import dev.madsens.geyma.theme.TileStyle
import dev.madsens.geyma.theme.itemColors
import dev.madsens.geyma.theme.onAccent
import dev.madsens.geyma.ui.components.GeymaCard
import dev.madsens.geyma.ui.components.SectionHeader
import dev.madsens.geyma.ui.components.geymaShape
import dev.madsens.geyma.ui.components.kindIcon
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(app: GeymaApp, onBack: () -> Unit, onOpenTrash: () -> Unit) {
    val t = LocalTheme.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = app.prefs
    val trashEntries by app.db.trash().all().collectAsState(initial = emptyList())

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.use { app.continuity.import(it) }
                }.onSuccess { summary ->
                    val msg = summary?.let {
                        "Merged ${it.events} events, ${it.stars} stars, ${it.sets} sets"
                    } ?: "Could not read that file"
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                }.onFailure {
                    Toast.makeText(context, "Not a Geyma bundle", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        item {
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.padding(end = 4.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = t.inkSoft)
                }
                Column {
                    Text("Settings", color = t.ink, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                    Text("Make Geyma yours", color = t.inkFaint, fontSize = 13.sp)
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        // Live sample of the current tokens, so every tweak below is
        // immediately legible without leaving the page.
        item {
            GeymaCard(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(t.name, color = t.ink, fontWeight = FontWeight.SemiBold)
                        Text(t.tag, color = t.inkFaint, fontSize = 12.sp)
                    }
                    Box(Modifier.size(18.dp).clip(CircleShape).background(t.accent))
                }
                Spacer(Modifier.height(10.dp))
                PreviewRow(name = "Photos", kind = "folder", starred = true, meta = "128 items")
                PreviewRow(name = "notes.md", kind = "text", starred = false, meta = "4.2 KB")
            }
        }

        item { SectionHeader("Skin") }
        item {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().height(((SKIN_ORDER.size + 1) / 2 * 96).dp),
                userScrollEnabled = false,
            ) {
                items(SKIN_ORDER.size) { i ->
                    val skin = SKINS.getValue(SKIN_ORDER[i])
                    SkinCard(
                        skin = skin,
                        selected = t.id == skin.id,
                        onClick = { scope.launch { prefs.setSkin(skin.id) } },
                    )
                }
            }
        }

        item { SectionHeader("Accent") }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(ACCENTS.size + 1) { i ->
                    if (i == 0) {
                        Box(
                            Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .border(BorderStroke(1.dp, t.border), CircleShape)
                                .clickable { scope.launch { prefs.setAccent(null) } },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("A", color = t.inkSoft, fontSize = 13.sp)
                        }
                    } else {
                        val accent = ACCENTS[i - 1]
                        Box(
                            Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(accent)
                                .clickable { scope.launch { prefs.setAccent(accent) } },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (accent == t.accent) {
                                Icon(Icons.Filled.Check, null, tint = t.onAccent, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }

        item { SectionHeader("Corner radius — ${t.radius}") }
        item {
            Slider(
                value = t.radius.toFloat(),
                onValueChange = { scope.launch { prefs.setRadius(it.toInt()) } },
                valueRange = 0f..20f,
                steps = 19,
            )
        }

        item { SectionHeader("Tiles") }
        item {
            ChoiceRow(
                options = listOf("Flat" to (t.tile == TileStyle.FLAT), "Card" to (t.tile == TileStyle.CARD)),
                onSelect = { i -> scope.launch { prefs.setTile(if (i == 0) TileStyle.FLAT else TileStyle.CARD) } },
            )
        }

        item { SectionHeader("Icons") }
        item {
            ChoiceRow(
                options = listOf("Colorful" to !t.iconMono, "Monochrome" to t.iconMono),
                onSelect = { i -> scope.launch { prefs.setIconMono(i == 1) } },
            )
        }

        item { SectionHeader("Backdrop") }
        item {
            ChoiceRow(
                options = BackgroundPattern.entries.map { p ->
                    p.name.lowercase().replaceFirstChar { it.uppercase() } to (t.backdrop == p)
                },
                onSelect = { i -> scope.launch { prefs.setPattern(BackgroundPattern.entries[i]) } },
            )
        }

        item { SectionHeader("Font") }
        item {
            ChoiceRow(
                options = FontKey.entries.map { f ->
                    f.name.lowercase().replaceFirstChar { it.uppercase() } to (t.fontKey == f)
                },
                onSelect = { i -> scope.launch { prefs.setFont(FontKey.entries[i]) } },
            )
        }

        item {
            Spacer(Modifier.height(10.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(geymaShape(0.5f))
                    .clickable { scope.launch { prefs.resetOverrides() } }
                    .padding(8.dp),
            ) {
                Icon(Icons.Filled.RestartAlt, null, tint = t.accent, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Reset customizations to the skin's defaults", color = t.accent, fontSize = 14.sp)
            }
        }

        item { SectionHeader("Storage") }
        item {
            GeymaCard(modifier = Modifier.fillMaxWidth(), padding = 4.dp) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(geymaShape(0.7f))
                        .clickable { onOpenTrash() }
                        .padding(vertical = 10.dp, horizontal = 10.dp),
                ) {
                    Icon(Icons.Filled.Delete, null, tint = t.inkSoft, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Trash", color = t.ink, modifier = Modifier.weight(1f))
                    if (trashEntries.isNotEmpty()) {
                        Text("${trashEntries.size}", color = t.inkFaint, fontSize = 12.sp)
                        Spacer(Modifier.width(4.dp))
                    }
                    Icon(Icons.AutoMirrored.Filled.NavigateNext, null, tint = t.inkFaint, modifier = Modifier.size(18.dp))
                }
            }
        }

        item { SectionHeader("Continuity") }
        item {
            GeymaCard(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Sync, null, tint = t.accent, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Carry your memory between devices", color = t.ink, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "Export your stars, sets and timeline as a portable .geyma bundle to hand off " +
                        "to the desktop app or another phone — no account, nothing leaves your device " +
                        "except the file you choose to share.",
                    color = t.inkFaint,
                    fontSize = 12.sp,
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .weight(1f)
                            .clip(geymaShape(0.7f))
                            .border(BorderStroke(1.dp, t.border), geymaShape(0.7f))
                            .clickable {
                                scope.launch {
                                    runCatching { app.continuity.export() }
                                        .onSuccess { file -> shareFile(context, file.absolutePath) }
                                        .onFailure { Toast.makeText(context, "Export failed", Toast.LENGTH_SHORT).show() }
                                }
                            }
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(Icons.Filled.FileUpload, null, tint = t.accent, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Export & share", color = t.ink, fontSize = 13.sp)
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .weight(1f)
                            .clip(geymaShape(0.7f))
                            .border(BorderStroke(1.dp, t.border), geymaShape(0.7f))
                            .clickable { importLauncher.launch(arrayOf("*/*")) }
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(Icons.Filled.FileDownload, null, tint = t.accent, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Import bundle", color = t.ink, fontSize = 13.sp)
                    }
                }
            }
        }

        item {
            SectionHeader("About")
            GeymaCard(modifier = Modifier.fillMaxWidth()) {
                // Read straight from BuildConfig so the version shown here always
                // matches the installed APK — this is how on-device testing tells
                // a fresh build apart from a stale one.
                Text(
                    "Geyma Mobile ${BuildConfig.VERSION_NAME} (build ${BuildConfig.VERSION_CODE})",
                    color = t.ink,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Old Norse — “to keep, to guard.” A file manager that remembers. " +
                        "Companion to the Geyma desktop app; everything stays on this device.",
                    color = t.inkFaint,
                    fontSize = 12.sp,
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PreviewRow(name: String, kind: String, starred: Boolean, meta: String) {
    val t = LocalTheme.current
    val colors = itemColors(kind, t)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    ) {
        Box(
            Modifier.size(30.dp).clip(geymaShape(0.6f)).background(colors.bg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(kindIcon(kind), null, tint = colors.tint, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(10.dp))
        Text(name, color = t.ink, fontSize = 14.sp)
        if (starred) {
            Spacer(Modifier.width(6.dp))
            Icon(Icons.Filled.Star, null, tint = t.accent, modifier = Modifier.size(13.dp))
        }
        Spacer(Modifier.weight(1f))
        Text(meta, color = t.inkFaint, fontSize = 12.sp)
    }
}

@Composable
private fun SkinCard(skin: Skin, selected: Boolean, onClick: () -> Unit) {
    val t = LocalTheme.current
    val shape = RoundedCornerShape(12.dp)
    Column(
        Modifier
            .clip(shape)
            .background(skin.bg)
            .border(BorderStroke(if (selected) 2.dp else 1.dp, if (selected) t.accent else t.border), shape)
            .clickable(onClick = onClick)
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(14.dp).clip(CircleShape).background(skin.accent))
            Spacer(Modifier.width(8.dp))
            Text(skin.name, color = skin.ink, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        }
        Spacer(Modifier.height(6.dp))
        Text(skin.tag, color = skin.inkFaint, fontSize = 11.sp, maxLines = 1)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(Modifier.weight(1f).height(10.dp).clip(RoundedCornerShape(3.dp)).background(skin.card))
            Box(Modifier.weight(1f).height(10.dp).clip(RoundedCornerShape(3.dp)).background(skin.surface))
            Box(Modifier.weight(1f).height(10.dp).clip(RoundedCornerShape(3.dp)).background(skin.accent))
        }
    }
}

@Composable
private fun ChoiceRow(options: List<Pair<String, Boolean>>, onSelect: (Int) -> Unit) {
    val t = LocalTheme.current
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEachIndexed { i, (label, active) ->
            val shape = geymaShape(0.7f)
            Text(
                label,
                color = if (active) t.onAccent else t.inkSoft,
                fontSize = 13.sp,
                modifier = Modifier
                    .clip(shape)
                    .background(if (active) t.accent else t.card)
                    .border(BorderStroke(1.dp, if (active) t.accent else t.border), shape)
                    .clickable { onSelect(i) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            )
        }
    }
}
