package dev.madsens.geyma

import dev.madsens.geyma.files.Entry
import dev.madsens.geyma.files.FileKind
import dev.madsens.geyma.files.FolderInsight
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FolderInsightTest {

    private val now = 1_700_000_000_000L
    private val day = 24 * 60 * 60 * 1000L

    private fun file(name: String, kind: String, size: Long, ageDays: Long = 1) = Entry(
        name = name,
        path = "/s/$name",
        isDir = false,
        size = size,
        modifiedMs = now - ageDays * day,
        kind = kind,
    )

    private fun folder(name: String) = Entry(
        name = name,
        path = "/s/$name",
        isDir = true,
        size = 0,
        modifiedMs = now - day,
        kind = FileKind.FOLDER,
    )

    @Test
    fun emptyFolderIsReportedPlainly() {
        val d = FolderInsight.describe(emptyList(), now)
        assertEquals("This folder is empty.", d.sentence)
        assertEquals(0, d.totalItems)
    }

    @Test
    fun dominantKindLeadsWithMostly() {
        val entries = (1..5).map { file("p$it.jpg", FileKind.IMAGE, 1_000_000) } +
            file("notes.pdf", FileKind.DOCUMENT, 2_000_000)
        val d = FolderInsight.describe(entries, now)
        assertTrue(d.sentence, d.sentence.startsWith("Mostly 5 photos and 1 document"))
        assertEquals(6, d.fileCount)
        assertEquals(7_000_000L, d.totalBytes)
        assertTrue(d.sentence.endsWith("."))
    }

    @Test
    fun mixedFolderCountsSubfoldersAndSize() {
        val entries = listOf(
            file("a.mp4", FileKind.VIDEO, 10_000_000),
            file("b.mp4", FileKind.VIDEO, 10_000_000),
            folder("clips"),
            folder("raw"),
        )
        val d = FolderInsight.describe(entries, now)
        assertEquals(2, d.folderCount)
        assertTrue(d.sentence, d.sentence.contains("2 videos"))
        assertTrue(d.sentence, d.sentence.contains("2 subfolders"))
        assertTrue(d.sentence, d.sentence.contains("in all"))
    }

    @Test
    fun onlySubfoldersReadsNaturally() {
        val d = FolderInsight.describe(listOf(folder("one"), folder("two")), now)
        assertTrue(d.sentence, d.sentence.startsWith("2 subfolders and no loose files"))
    }
}
