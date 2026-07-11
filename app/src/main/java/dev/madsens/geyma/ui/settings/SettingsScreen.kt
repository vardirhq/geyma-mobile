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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.madsens.geyma.GeymaApp
import dev.madsens.geyma.theme.ACCENTS
import dev.madsens.geyma.theme.BackgroundPattern
import dev.madsens.geyma.theme.FontKey
import dev.madsens.geyma.theme.LocalTheme
import dev.madsens.geyma.theme.SKINS
import dev.madsens.geyma.theme.SKIN_ORDER
import dev.madsens.geyma.theme.Skin
import dev.madsens.geyma.theme.TileStyle
import dev.madsens.geyma.theme.onAccent
import dev.madsens.geyma.ui.components.SectionHeader
import dev.madsens.geyma.ui.components.geymaShape
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(app: GeymaApp, onOpenTrash: () -> Unit) {
    val t = LocalTheme.current
    val scope = rememberCoroutineScope()
    val prefs = app.prefs

    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        item {
            Spacer(Modifier.height(10.dp))
            Text("Appearance", color = t.ink, fontWeight = FontWeight.Bold, fontSize = 24.sp)
            Text("Skins and tokens, straight from the desktop app", color = t.inkFaint, fontSize = 13.sp)
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
            Spacer(Modifier.height(12.dp))
            Text(
                "Reset customizations",
                color = t.accent,
                fontSize = 14.sp,
                modifier = Modifier
                    .clip(geymaShape(0.5f))
                    .clickable { scope.launch { prefs.resetOverrides() } }
                    .padding(8.dp),
            )
        }

        item { SectionHeader("Storage") }
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(geymaShape())
                    .clickable { onOpenTrash() }
                    .padding(vertical = 10.dp, horizontal = 6.dp),
            ) {
                Icon(Icons.Filled.Delete, null, tint = t.inkSoft, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                Text("Trash", color = t.ink)
            }
        }

        item {
            SectionHeader("About")
            Text("Geyma Mobile 0.1.0", color = t.inkSoft, fontSize = 13.sp)
            Text(
                "Geyma — Old Norse, “to keep, to guard.” A file manager that remembers.",
                color = t.inkFaint,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp),
            )
            Spacer(Modifier.height(24.dp))
        }
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
