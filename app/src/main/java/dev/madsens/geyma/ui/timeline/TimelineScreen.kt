package dev.madsens.geyma.ui.timeline

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import dev.madsens.geyma.files.PathUtils
import dev.madsens.geyma.theme.LocalTheme
import dev.madsens.geyma.ui.components.EmptyState
import dev.madsens.geyma.ui.components.EventGlyph
import dev.madsens.geyma.ui.components.EventUi
import dev.madsens.geyma.ui.components.SectionHeader
import dev.madsens.geyma.ui.components.geymaShape
import java.text.DateFormat
import java.util.Calendar
import java.util.Date

private fun dayLabel(whenMs: Long, now: Calendar): String {
    val cal = Calendar.getInstance().apply { timeInMillis = whenMs }
    val sameDay = { a: Calendar, b: Calendar ->
        a.get(Calendar.YEAR) == b.get(Calendar.YEAR) && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
    }
    if (sameDay(cal, now)) return "Today"
    val yesterday = Calendar.getInstance().apply {
        timeInMillis = now.timeInMillis
        add(Calendar.DAY_OF_YEAR, -1)
    }
    if (sameDay(cal, yesterday)) return "Yesterday"
    return DateFormat.getDateInstance(DateFormat.FULL).format(Date(whenMs))
}

@Composable
fun TimelineScreen(app: GeymaApp, onJumpTo: (String) -> Unit) {
    val t = LocalTheme.current
    val events by app.db.events().recent(400).collectAsState(initial = emptyList())

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(10.dp))
        Text("Timeline", color = t.ink, fontWeight = FontWeight.Bold, fontSize = 24.sp)
        Text("Everything Geyma has remembered", color = t.inkFaint, fontSize = 13.sp)
        Spacer(Modifier.height(4.dp))

        if (events.isEmpty()) {
            EmptyState("Nothing recorded yet.")
            return
        }

        val now = Calendar.getInstance()
        val grouped = events.groupBy { dayLabel(it.whenMs, now) }

        LazyColumn(Modifier.fillMaxSize()) {
            grouped.forEach { (label, dayEvents) ->
                item(key = "header:$label") { SectionHeader(label) }
                items(dayEvents.size, key = { i -> dayEvents[i].id }) { i ->
                    TimelineRow(dayEvents[i], onJumpTo)
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun TimelineRow(event: FileEvent, onJumpTo: (String) -> Unit) {
    val t = LocalTheme.current
    val gone = event.action == EventActions.DELETED || event.action == EventActions.TRASHED
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(geymaShape(0.5f))
            .clickable(enabled = !gone) {
                PathUtils.parentOf(event.path)?.let(onJumpTo)
            }
            .padding(horizontal = 4.dp, vertical = 7.dp),
    ) {
        EventGlyph(event.action, if (gone) t.inkFaint else t.accent)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                PathUtils.nameOf(if (gone) event.prevPath ?: event.path else event.path),
                color = if (gone) t.inkSoft else t.ink,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                EventUi.label(event.action) + (event.detail?.let { " · $it" } ?: ""),
                color = t.inkFaint,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(event.whenMs)),
            color = t.inkFaint,
            fontSize = 11.sp,
        )
    }
}
