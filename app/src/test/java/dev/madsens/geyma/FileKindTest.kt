package dev.madsens.geyma

import dev.madsens.geyma.files.FileKind
import org.junit.Assert.assertEquals
import org.junit.Test

class FileKindTest {

    @Test
    fun directoriesAreFolders() {
        assertEquals(FileKind.FOLDER, FileKind.ofName("anything.zip", isDir = true))
    }

    @Test
    fun classifiesCommonExtensions() {
        assertEquals(FileKind.DOCUMENT, FileKind.ofName("thesis.pdf", isDir = false))
        assertEquals(FileKind.IMAGE, FileKind.ofName("photo.JPG", isDir = false))
        assertEquals(FileKind.VIDEO, FileKind.ofName("clip.mkv", isDir = false))
        assertEquals(FileKind.AUDIO, FileKind.ofName("song.flac", isDir = false))
        assertEquals(FileKind.ARCHIVE, FileKind.ofName("backup.tar", isDir = false))
        assertEquals(FileKind.CODE, FileKind.ofName("Main.kt", isDir = false))
        assertEquals(FileKind.APP, FileKind.ofName("geyma.apk", isDir = false))
        assertEquals(FileKind.ARCHIVE, FileKind.ofName("shared.gyset", isDir = false))
    }

    @Test
    fun unknownAndBareNamesFallBackToText() {
        assertEquals(FileKind.TEXT, FileKind.ofName("README", isDir = false))
        assertEquals(FileKind.TEXT, FileKind.ofName("weird.xyz123", isDir = false))
    }

    @Test
    fun mimeTypesForOpenIntent() {
        assertEquals("application/pdf", FileKind.mimeOf("doc.pdf"))
        assertEquals("image/png", FileKind.mimeOf("shot.png"))
        assertEquals("video/*", FileKind.mimeOf("clip.mp4"))
        assertEquals("text/plain", FileKind.mimeOf("notes.md"))
        assertEquals("*/*", FileKind.mimeOf("mystery.bin"))
    }
}
