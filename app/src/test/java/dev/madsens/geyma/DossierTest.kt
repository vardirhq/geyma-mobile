package dev.madsens.geyma

import dev.madsens.geyma.data.EventActions
import dev.madsens.geyma.data.FileEvent
import dev.madsens.geyma.data.SeenFile
import dev.madsens.geyma.insights.Dossier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DossierTest {

    private val path = "/s/Documents/report.pdf"

    private fun ev(action: String, whenMs: Long, detail: String? = null, prevPath: String? = null, p: String = path) =
        FileEvent(path = p, action = action, detail = detail, prevPath = prevPath, whenMs = whenMs)

    // Per-path history comes back newest-first from the DAO; mirror that here.
    private val history = listOf(
        ev(EventActions.OPENED, 500),
        ev(EventActions.RENAMED, 400, detail = "from draft.pdf", prevPath = "/s/Documents/draft.pdf"),
        ev(EventActions.OPENED, 300),
        ev(EventActions.ARRIVED, 100, detail = "in Downloads", p = "/s/Downloads/draft.pdf"),
    )

    @Test
    fun summarizeDerivesProvenanceAndTallies() {
        val s = Dossier.summarize(
            path = path,
            events = history,
            starred = true,
            setNames = listOf("Tax 2026"),
            seen = SeenFile(path = path, firstSeenMs = 100, lastOpenedMs = 500),
            exists = true,
        )
        assertEquals(EventActions.ARRIVED, s.originAction)
        assertEquals("in Downloads", s.originDetail)
        assertEquals(100L, s.bornMs)
        assertEquals(2, s.timesOpened)
        assertEquals(1, s.timesMoved)
        assertTrue(s.starred)
        assertEquals(listOf("Tax 2026"), s.setNames)
        assertEquals(4, s.eventCount)
        assertTrue(s.exists)
        assertFalse(s.trashed)
        assertTrue(s.hasProvenance)
    }

    @Test
    fun latestTrashEventMarksFileTrashed() {
        val trashed = listOf(
            ev(EventActions.TRASHED, 600, detail = "to trash"),
        ) + history
        val s = Dossier.summarize(path, trashed, starred = false, setNames = emptyList(), seen = null, exists = true)
        assertTrue(s.trashed)
        assertFalse(s.exists) // trashed overrides a stale exists=true
    }

    @Test
    fun fileWithoutJournalOriginStillReportsSeenLedger() {
        val s = Dossier.summarize(
            path = path,
            events = emptyList(),
            starred = false,
            setNames = emptyList(),
            seen = SeenFile(path = path, firstSeenMs = 42, lastOpenedMs = null),
            exists = true,
        )
        assertEquals(null, s.originAction)
        assertEquals(42L, s.firstSeenMs)
        assertTrue(s.hasProvenance)
        assertEquals(0, s.eventCount)
    }
}
