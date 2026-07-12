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
        assertEquals(ViewerKind.TEXT, InAppViewer.kindFor("notes.md"))
        assertEquals(ViewerKind.TEXT, InAppViewer.kindFor("Main.kt"))
        assertEquals(ViewerKind.TEXT, InAppViewer.kindFor("README"))
    }

    @Test
    fun oversizedTextFallsBackToExternal() {
        val big = InAppViewer.MAX_TEXT_BYTES + 1
        assertEquals(ViewerKind.NONE, InAppViewer.kindFor("huge.log", big))
        assertFalse(InAppViewer.canView("huge.log", big))
        // Within the limit it stays viewable.
        assertEquals(ViewerKind.TEXT, InAppViewer.kindFor("huge.log", InAppViewer.MAX_TEXT_BYTES))
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
