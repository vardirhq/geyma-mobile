@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package dev.madsens.geyma.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.RowScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import dev.madsens.geyma.files.Entry
import dev.madsens.geyma.files.FileKind
import dev.madsens.geyma.files.PathUtils
import dev.madsens.geyma.theme.LocalTheme
import dev.madsens.geyma.theme.itemColors
import java.io.File

fun kindIcon(kind: String): ImageVector = when (kind) {
    FileKind.FOLDER -> Icons.Filled.Folder
    FileKind.DOCUMENT -> Icons.Filled.Description
    FileKind.TEXT -> Icons.AutoMirrored.Filled.Notes
    FileKind.CODE -> Icons.Filled.Code
    FileKind.IMAGE -> Icons.Filled.Image
    FileKind.VIDEO -> Icons.Filled.Movie
    FileKind.AUDIO -> Icons.Filled.MusicNote
    FileKind.ARCHIVE -> Icons.Filled.FolderZip
    FileKind.APP -> Icons.Filled.Android
    else -> Icons.AutoMirrored.Filled.InsertDriveFile
}

@Composable
fun KindBadge(entry: Entry, size: androidx.compose.ui.unit.Dp, thumbnails: Boolean = true) {
    val t = LocalTheme.current
    val colors = itemColors(entry.kind, t)
    val shape = geymaShape(0.6f)
    Box(
        modifier = Modifier.size(size).clip(shape).background(colors.bg),
        contentAlignment = Alignment.Center,
    ) {
        if (thumbnails && !entry.isDir && entry.kind == FileKind.IMAGE) {
            AsyncImage(
                model = File(entry.path),
                contentDescription = entry.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(
                kindIcon(entry.kind),
                contentDescription = FileKind.LABELS[entry.kind],
                tint = colors.tint,
                modifier = Modifier.size(size * 0.55f),
            )
        }
    }
}

/**
 * The little cluster of state markers a tile carries — sealed (guarded),
 * noted (has a sticky note), starred — shown in that order of consequence.
 */
@Composable
fun RowScope.EntryBadges(entry: Entry, size: Dp) {
    val t = LocalTheme.current
    if (entry.sealed) {
        Spacer(Modifier.width(6.dp))
        Icon(Icons.Filled.Lock, contentDescription = "Sealed", tint = t.inkSoft, modifier = Modifier.size(size))
    }
    if (entry.noted) {
        Spacer(Modifier.width(6.dp))
        Icon(Icons.Filled.EditNote, contentDescription = "Has a note", tint = t.accent, modifier = Modifier.size(size))
    }
    if (entry.starred) {
        Spacer(Modifier.width(6.dp))
        Icon(Icons.Filled.Star, contentDescription = "Starred", tint = t.accent, modifier = Modifier.size(size))
    }
}

@Composable
fun FileRow(
    entry: Entry,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val t = LocalTheme.current
    val shape = geymaShape()
    val fill = when {
        selected -> t.accent.copy(alpha = 0.16f)
        else -> androidx.compose.ui.graphics.Color.Transparent
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(fill)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        KindBadge(entry, 40.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    entry.name,
                    color = if (entry.hidden) t.inkSoft else t.ink,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                EntryBadges(entry, 14.dp)
            }
            Text(
                text = if (entry.isDir) {
                    entry.childCount?.let { "$it item" + if (it == 1) "" else "s" } ?: "Folder"
                } else {
                    PathUtils.humanSize(entry.size) + "  ·  " + timeAgo(entry.modifiedMs)
                },
                color = t.inkFaint,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun FileGridTile(
    entry: Entry,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val t = LocalTheme.current
    val shape = geymaShape()
    val border = if (selected) BorderStroke(1.5.dp, t.accent) else BorderStroke(1.dp, t.border)
    val fill = when {
        selected -> t.accent.copy(alpha = 0.14f)
        t.tile == dev.madsens.geyma.theme.TileStyle.CARD -> t.card
        else -> androidx.compose.ui.graphics.Color.Transparent
    }
    Column(
        modifier = Modifier
            .clip(shape)
            .background(fill)
            .border(border, shape)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(10.dp),
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(1.3f), contentAlignment = Alignment.Center) {
            KindBadge(entry, 64.dp)
        }
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                entry.name,
                color = if (entry.hidden) t.inkSoft else t.ink,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            EntryBadges(entry, 12.dp)
        }
        Text(
            text = if (entry.isDir) entry.childCount?.let { "$it items" } ?: "Folder" else PathUtils.humanSize(entry.size),
            color = t.inkFaint,
            fontSize = 11.sp,
            maxLines = 1,
        )
    }
}
