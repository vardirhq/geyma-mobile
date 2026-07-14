package dev.madsens.geyma

import dev.madsens.geyma.files.Ocr
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OcrTest {

    @Test
    fun onlyDecodableRasterImagesWithinSizeAreIndexable() {
        assertTrue(Ocr.canIndex("receipt.jpg", 500_000))
        assertTrue(Ocr.canIndex("SHOT.PNG", 500_000))
        assertTrue(Ocr.canIndex("scan.heic", 500_000))
        assertFalse(Ocr.canIndex("logo.svg", 10_000)) // vector — no bitmap of text
        assertFalse(Ocr.canIndex("clip.gif", 10_000)) // animated
        assertFalse(Ocr.canIndex("notes.txt", 10_000)) // not an image
        assertFalse(Ocr.canIndex("huge.jpg", Ocr.MAX_BYTES + 1))
        assertFalse(Ocr.canIndex("empty.jpg", 0))
    }

    @Test
    fun snippetCentersOnTheMatchWithEllipses() {
        val text = "Total due   \n  $42.50 payable to Acme Corp on the fifteenth of the month"
        val snip = Ocr.snippet(text, "42.50", radius = 10)
        assertTrue(snip, snip.contains("42.50"))
        assertTrue(snip, snip.startsWith("…"))
        assertTrue(snip, snip.endsWith("…"))
        assertFalse(snip, snip.contains("\n"))
    }

    @Test
    fun snippetWithoutMatchTruncatesFromStart() {
        val text = "a".repeat(500)
        val snip = Ocr.snippet(text, "zzz", radius = 20)
        assertEquals(41, snip.length) // radius*2 chars + trailing ellipsis
        assertTrue(snip.endsWith("…"))
    }

    @Test
    fun snippetHandlesEmptyText() {
        assertEquals("", Ocr.snippet("   ", "anything"))
    }
}
