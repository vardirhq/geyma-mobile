package dev.madsens.geyma

import dev.madsens.geyma.files.PathUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PathUtilsTest {

    @Test
    fun nameOf_extractsLastSegment() {
        assertEquals("file.txt", PathUtils.nameOf("/storage/emulated/0/Download/file.txt"))
        assertEquals("Download", PathUtils.nameOf("/storage/emulated/0/Download/"))
    }

    @Test
    fun parentOf_walksUp() {
        assertEquals("/storage/emulated/0", PathUtils.parentOf("/storage/emulated/0/Download"))
        assertEquals("/", PathUtils.parentOf("/storage"))
        assertNull(PathUtils.parentOf("/"))
    }

    @Test
    fun join_normalizesSlashes() {
        assertEquals("/a/b/c.txt", PathUtils.join("/a/b/", "c.txt"))
        assertEquals("/a/b/c.txt", PathUtils.join("/a/b", "c.txt"))
    }

    @Test
    fun isAncestorOf_matchesSelfAndChildren() {
        assertTrue(PathUtils.isAncestorOf("/a/b", "/a/b"))
        assertTrue(PathUtils.isAncestorOf("/a/b", "/a/b/c/d"))
        assertFalse(PathUtils.isAncestorOf("/a/b", "/a/bc"))
    }

    @Test
    fun rebase_rewritesMovedPaths() {
        assertEquals("/new/dir", PathUtils.rebase("/old/dir", "/old/dir", "/new/dir"))
        assertEquals("/new/dir/x/y.txt", PathUtils.rebase("/old/dir/x/y.txt", "/old/dir", "/new/dir"))
        assertNull(PathUtils.rebase("/other/z.txt", "/old/dir", "/new/dir"))
    }

    @Test
    fun breadcrumbs_startAtRootLabel() {
        val crumbs = PathUtils.breadcrumbs("/storage/emulated/0/Download/sub", "/storage/emulated/0", "Internal")
        assertEquals(
            listOf(
                "Internal" to "/storage/emulated/0",
                "Download" to "/storage/emulated/0/Download",
                "sub" to "/storage/emulated/0/Download/sub",
            ),
            crumbs,
        )
    }

    @Test
    fun breadcrumbs_atRootIsSingleSegment() {
        val crumbs = PathUtils.breadcrumbs("/storage/emulated/0", "/storage/emulated/0", "Internal")
        assertEquals(listOf("Internal" to "/storage/emulated/0"), crumbs)
    }

    @Test
    fun uniqueChildName_countsUpLikeDesktop() {
        val existing = setOf("report.pdf", "report (2).pdf")
        assertEquals("report (3).pdf", PathUtils.uniqueChildName(existing, "report.pdf"))
        assertEquals("fresh.pdf", PathUtils.uniqueChildName(existing, "fresh.pdf"))
        assertEquals("folder (2)", PathUtils.uniqueChildName(setOf("folder"), "folder"))
        assertEquals(".config (2)", PathUtils.uniqueChildName(setOf(".config"), ".config"))
    }

    @Test
    fun humanSize_formatsAcrossUnits() {
        assertEquals("0 B", PathUtils.humanSize(0))
        assertEquals("512 B", PathUtils.humanSize(512))
        assertEquals("1.0 KB", PathUtils.humanSize(1024))
        assertEquals("1.5 KB", PathUtils.humanSize(1536))
        assertEquals("1.0 MB", PathUtils.humanSize(1024L * 1024))
        assertEquals("2.0 GB", PathUtils.humanSize(2L * 1024 * 1024 * 1024))
        assertEquals("—", PathUtils.humanSize(-1))
    }
}
