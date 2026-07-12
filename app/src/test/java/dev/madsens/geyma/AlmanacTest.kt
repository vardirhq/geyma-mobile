package dev.madsens.geyma

import dev.madsens.geyma.data.EventActions
import dev.madsens.geyma.data.FileEvent
import dev.madsens.geyma.insights.Almanac
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class AlmanacTest {

    private val now = 1_700_000_000_000L
    private val day = TimeUnit.DAYS.toMillis(1)

    private fun ev(action: String, path: String, whenMs: Long, prevPath: String? = null) =
        FileEvent(path = path, action = action, prevPath = prevPath, whenMs = whenMs)

    private val events = listOf(
        ev(EventActions.ARRIVED, "/s/Download/a.pdf", now - TimeUnit.HOURS.toMillis(1)),
        ev(EventActions.OPENED, "/s/Download/a.pdf", now - TimeUnit.HOURS.toMillis(2)),
        ev(EventActions.ARRIVED, "/s/Pictures/b.jpg", now - 3 * day - 1),
        ev(EventActions.RENAMED, "/s/Pictures/b.jpg", now - 3 * day - 1, prevPath = "/s/Pictures/old.jpg"),
        // Outside the 14-day sparkline window but still part of the totals.
        ev(EventActions.CREATED, "/s/Download/c.txt", now - 20 * day),
    )

    @Test
    fun headlineCountsTallyByAction() {
        val s = Almanac.compute(events, neglected = 5, nowMs = now, days = 14)
        assertEquals(5, s.totalEvents)
        assertEquals(2, s.arrivals)
        assertEquals(1, s.opens)
        assertEquals(1, s.moves)
        assertEquals(5, s.neglected)
        assertTrue(s.hasHistory)
    }

    @Test
    fun perDayBucketsAreOldestFirstAndWindowed() {
        val s = Almanac.compute(events, neglected = 0, nowMs = now, days = 14)
        assertEquals(14, s.perDay.size)
        // Oldest bucket first, last bucket is today.
        assertEquals(13, s.perDay.first().daysAgo)
        assertEquals(0, s.perDay.last().daysAgo)
        assertEquals(2, s.perDay.last().count) // arrived + opened today
        assertEquals(2, s.perDay.first { it.daysAgo == 3 }.count) // arrived + renamed
        assertEquals(2, s.peakDayCount)
        // The 20-days-ago event falls outside the window and is not bucketed.
        assertEquals(4, s.perDay.sumOf { it.count })
    }

    @Test
    fun busiestFilesNeedMoreThanOneEvent() {
        val s = Almanac.compute(events, neglected = 0, nowMs = now, days = 14)
        assertEquals(1, s.busiestFiles.size)
        assertEquals("a.pdf", s.busiestFiles.first().label)
        assertEquals(2, s.busiestFiles.first().count)
    }

    @Test
    fun topFoldersRankByActivity() {
        val s = Almanac.compute(events, neglected = 0, nowMs = now, days = 14)
        // Download saw arrived+opened+created = 3; Pictures saw 2.
        assertEquals("Download", s.topFolders.first().label)
        assertEquals(3, s.topFolders.first().count)
    }

    @Test
    fun emptyJournalHasNoHistory() {
        val s = Almanac.compute(emptyList(), neglected = 0, nowMs = now, days = 7)
        assertFalse(s.hasHistory)
        assertNull(s.busiestDay)
        assertEquals(0, s.peakDayCount)
    }
}
