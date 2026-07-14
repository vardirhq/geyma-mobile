package dev.madsens.geyma.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.madsens.geyma.data.EventActions
import dev.madsens.geyma.theme.LocalTheme
import dev.madsens.geyma.theme.TileStyle
import java.text.DateFormat
import java.util.Date
import java.util.concurrent.TimeUnit

@Composable
fun geymaShape(scale: Float = 1f): RoundedCornerShape {
    val t = LocalTheme.current
    return RoundedCornerShape((t.radius * scale).coerceAtLeast(0f).dp)
}

/** Tile container honoring the skin's card-vs-flat token, radius and border. */
@Composable
fun GeymaCard(
    modifier: Modifier = Modifier,
    padding: Dp = 14.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val t = LocalTheme.current
    val shape = geymaShape()
    val fill = if (t.tile == TileStyle.CARD) t.card else Color.Transparent
    Column(
        modifier = modifier
            .clip(shape)
            .background(fill)
            .border(BorderStroke(1.dp, t.border), shape)
            .padding(padding),
        content = content,
    )
}

@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    val t = LocalTheme.current
    Text(
        text = text.uppercase(),
        color = t.inkFaint,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.2.sp,
        modifier = modifier.padding(vertical = 8.dp),
    )
}

@Composable
fun EmptyState(text: String, modifier: Modifier = Modifier) {
    val t = LocalTheme.current
    Box(modifier = modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(text, color = t.inkFaint)
    }
}

object EventUi {
    fun label(action: String): String = when (action) {
        EventActions.CREATED -> "Created"
        EventActions.RENAMED -> "Renamed"
        EventActions.MOVED -> "Moved here"
        EventActions.COPIED -> "Copied here"
        EventActions.TRASHED -> "Deleted"
        EventActions.RESTORED -> "Restored"
        EventActions.DELETED -> "Permanently deleted"
        EventActions.STARRED -> "Starred"
        EventActions.UNSTARRED -> "Unstarred"
        EventActions.NOTED -> "Note"
        EventActions.SEALED -> "Sealed"
        EventActions.UNSEALED -> "Unsealed"
        EventActions.PACKED -> "Packed"
        else -> action.replaceFirstChar { it.uppercase() }
    }

    fun icon(action: String): ImageVector = when (action) {
        EventActions.CREATED -> Icons.Filled.Add
        EventActions.RENAMED -> Icons.Filled.DriveFileRenameOutline
        EventActions.MOVED -> Icons.AutoMirrored.Filled.DriveFileMove
        EventActions.COPIED -> Icons.Filled.ContentCopy
        EventActions.TRASHED -> Icons.Filled.Delete
        EventActions.RESTORED -> Icons.Filled.RestoreFromTrash
        EventActions.DELETED -> Icons.Filled.DeleteForever
        EventActions.STARRED -> Icons.Filled.Star
        EventActions.UNSTARRED -> Icons.Filled.StarBorder
        EventActions.NOTED -> Icons.Filled.EditNote
        EventActions.SEALED -> Icons.Filled.Lock
        EventActions.UNSEALED -> Icons.Filled.LockOpen
        EventActions.PACKED -> Icons.Filled.Inventory2
        else -> Icons.Filled.Add
    }
}

fun timeAgo(whenMs: Long, nowMs: Long = System.currentTimeMillis()): String {
    val delta = nowMs - whenMs
    if (delta < TimeUnit.MINUTES.toMillis(1)) return "just now"
    if (delta < TimeUnit.HOURS.toMillis(1)) return "${TimeUnit.MILLISECONDS.toMinutes(delta)}m ago"
    if (delta < TimeUnit.DAYS.toMillis(1)) return "${TimeUnit.MILLISECONDS.toHours(delta)}h ago"
    if (delta < TimeUnit.DAYS.toMillis(7)) return "${TimeUnit.MILLISECONDS.toDays(delta)}d ago"
    return DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(whenMs))
}

fun formatWhen(whenMs: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(whenMs))

@Composable
fun KindDot(color: Color, size: Dp = 8.dp) {
    Box(
        Modifier
            .size(size)
            .clip(RoundedCornerShape(50))
            .background(color),
    )
}

@Composable
fun EventGlyph(action: String, tint: Color) {
    Icon(EventUi.icon(action), contentDescription = EventUi.label(action), tint = tint, modifier = Modifier.size(16.dp))
}
