package dev.madsens.geyma

import dev.madsens.geyma.files.InAppViewer
import dev.madsens.geyma.files.ViewerKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ViewableTest {

    @Test
    fun mediaAndImagesRouteToTheirViewers() {
        assertEquals(ViewerKind.IMAGE, InAppViewer.kindFor("photo.JPG"))
        assertEquals(ViewerKind.IMAGE, InAppViewer.kindFor("frame.heic"))
        assertEquals(ViewerKind.IMAGE, InAppViewer.kindFor("loop.gif"))
        assertEquals(ViewerKind.IMAGE, InAppViewer.kindFor("icon.svg"))
        assertEquals(ViewerKind.VIDEO, InAppViewer.kindFor("clip.mkv"))
        assertEquals(ViewerKind.AUDIO, InAppViewer.kindFor("song.flac"))
    }

    @Test
    fun pdfHasItsOwnViewerEvenThoughItIsADocument() {
        assertEquals(ViewerKind.PDF, InAppViewer.kindFor("thesis.pdf"))
    }

    @Test
    fun textCodeAndBareNamesReadAsText() {
        assertEquals(ViewerKind.TEXT, InAppViewer.kindFor("notes.txt"))
        assertEquals(ViewerKind.TEXT, InAppViewer.kindFor("Main.kt"))
        assertEquals(ViewerKind.TEXT, InAppViewer.kindFor("README"))
    }

    @Test
    fun htmlAndMarkdownRenderAsWeb() {
        assertEquals(ViewerKind.WEB, InAppViewer.kindFor("index.html"))
        assertEquals(ViewerKind.WEB, InAppViewer.kindFor("page.htm"))
        assertEquals(ViewerKind.WEB, InAppViewer.kindFor("notes.md", 2048))
        assertEquals(ViewerKind.WEB, InAppViewer.kindFor("doc.markdown"))
    }

    @Test
    fun largeKnownTextStaysInAppButTruncated() {
        // A recognized text/code extension is still worth showing when large — the
        // text viewer truncates it — so it no longer bounces to another app.
        val big = InAppViewer.MAX_TEXT_BYTES + 1
        assertEquals(ViewerKind.TEXT, InAppViewer.kindFor("huge.log", big))
        assertEquals(ViewerKind.TEXT, InAppViewer.kindFor("export.csv", big))
        assertTrue(InAppViewer.canView("huge.log", big))
    }

    @Test
    fun largeUnknownFileFallsBackToExternal() {
        // Unknown extensions (which FileKind defaults to TEXT) are only shown when
        // small, so a big binary blob isn't poured into the text pane.
        val big = InAppViewer.MAX_TEXT_BYTES + 1
        assertEquals(ViewerKind.NONE, InAppViewer.kindFor("blob.xyz123", big))
        assertFalse(InAppViewer.canView("mystery.bin", big))
        // …but a small one still opens in-app as text.
        assertTrue(InAppViewer.canView("blob.xyz123", 1024))
    }

    @Test
    fun oversizedHtmlDropsToTruncatedSource() {
        val big = InAppViewer.MAX_TEXT_BYTES + 1
        assertEquals(ViewerKind.TEXT, InAppViewer.kindFor("index.html", big))
    }

    @Test
    fun formatsWithoutABuiltInViewerStayExternal() {
        assertEquals(ViewerKind.NONE, InAppViewer.kindFor("sheet.docx"))
        assertEquals(ViewerKind.NONE, InAppViewer.kindFor("archive.zip"))
        assertEquals(ViewerKind.NONE, InAppViewer.kindFor("app.apk"))
        assertFalse(InAppViewer.canView("archive.zip"))
    }

    @Test
    fun unknownExtensionsAreTreatedAsViewableText() {
        // FileKind treats unknown extensions as text; small ones open in-app.
        assertTrue(InAppViewer.canView("data.xyz123", 1024))
    }
}
